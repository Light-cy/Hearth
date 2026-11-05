// server/engine/money.js
// Pure money engine operating on integer paisa (BIGINT / Number)

function toPaisa(amount, requirePositive = true) {
  if (amount === undefined || amount === null || amount === '') {
    throw new Error('Amount cannot be empty');
  }
  const str = String(amount).trim().replace(/,/g, '');
  if (!/^-?\d+(\.\d{1,2})?$/.test(str)) {
    throw new Error(`Invalid decimal format (at most 2 decimal places allowed): ${amount}`);
  }
  const num = parseFloat(str);
  if (isNaN(num) || !isFinite(num)) {
    throw new Error(`Amount is not a valid number: ${amount}`);
  }
  if (requirePositive && num <= 0) {
    throw new Error('Amount must be positive');
  }
  const paisa = Math.round(num * 100);
  if (paisa > 1000000000) { // 10 million PKR
    throw new Error('Amount exceeds maximum allowed ceiling of PKR 10,000,000.00');
  }
  return paisa;
}

function fromPaisa(paisa) {
  const p = Number(paisa);
  const isNegative = p < 0;
  const absP = Math.abs(p);
  const rupees = Math.floor(absP / 100);
  const remainder = absP % 100;
  const formatted = `${rupees}.${remainder.toString().padStart(2, '0')}`;
  return isNegative ? `-${formatted}` : formatted;
}

function fromPaisaNumber(paisa) {
  return parseFloat(fromPaisa(paisa));
}

// S4.2: Equal split with ascending remainder allocation
function splitEqual(amountPaisa, memberIds) {
  if (amountPaisa <= 0) throw new Error('Amount must be positive');
  if (!memberIds || memberIds.length === 0) throw new Error('Members list cannot be empty');

  const sortedIds = [...new Set(memberIds)].sort();
  const n = sortedIds.length;
  const base = Math.floor(amountPaisa / n);
  const remainder = amountPaisa % n;

  const result = {};
  for (let i = 0; i < sortedIds.length; i++) {
    result[sortedIds[i]] = base + (i < remainder ? 1 : 0);
  }

  // Integrity assertion
  const sum = Object.values(result).reduce((acc, v) => acc + v, 0);
  if (sum !== amountPaisa) {
    throw new Error(`Internal split error: sum of shares (${sum}) != total (${amountPaisa})`);
  }
  return result;
}

// S4.3: Custom split validator
function validateCustomSplit(amountPaisa, shares) {
  if (amountPaisa <= 0) throw new Error('Amount must be positive');
  let sum = 0;
  for (const [id, share] of Object.entries(shares)) {
    if (share < 0) throw new Error(`Share for member ${id} cannot be negative`);
    sum += share;
  }
  if (sum !== amountPaisa) {
    const diff = fromPaisa(Math.abs(amountPaisa - sum));
    const dir = sum < amountPaisa ? 'under' : 'over';
    throw new Error(`Custom split is ${dir} by PKR ${diff} (Total: ${fromPaisa(sum)}, Expected: ${fromPaisa(amountPaisa)})`);
  }
}

// S4.4: Net = Paid - Owed + Sent - Received. Asserts sum === 0.
function computeBalances(members, expenses, settlements) {
  const paid = {};
  const owed = {};
  const sent = {};
  const received = {};

  for (const m of members) {
    paid[m.id] = 0;
    owed[m.id] = 0;
    sent[m.id] = 0;
    received[m.id] = 0;
  }

  for (const exp of expenses) {
    const pId = exp.payer_id;
    paid[pId] = (paid[pId] || 0) + Number(exp.amount_paisa);
    if (exp.splits) {
      for (const sp of exp.splits) {
        owed[sp.member_id] = (owed[sp.member_id] || 0) + Number(sp.share_paisa);
      }
    }
  }

  for (const st of settlements) {
    sent[st.sender_id] = (sent[st.sender_id] || 0) + Number(st.amount_paisa);
    received[st.recipient_id] = (received[st.recipient_id] || 0) + Number(st.amount_paisa);
  }

  const balances = members.map(m => {
    const net = (paid[m.id] || 0) - (owed[m.id] || 0) + (sent[m.id] || 0) - (received[m.id] || 0);
    return {
      member_id: m.id,
      name: m.name,
      paid: (paid[m.id] || 0),
      owed: (owed[m.id] || 0),
      sent: (sent[m.id] || 0),
      received: (received[m.id] || 0),
      net: net
    };
  });

  const sumNet = balances.reduce((acc, b) => acc + b.net, 0);
  if (sumNet !== 0) {
    throw new Error(`INTERNAL ERROR: Net balances sum invariant violated: ${sumNet} paisa`);
  }

  return balances;
}

// S4.5: Settlement validation
function validateSettlement(senderId, recipientId, amountPaisa, balances) {
  if (amountPaisa <= 0) throw new Error('Settlement amount must be positive');
  if (senderId === recipientId) throw new Error('Cannot settle with yourself');

  const senderBal = balances.find(b => b.member_id === senderId);
  const recipientBal = balances.find(b => b.member_id === recipientId);

  if (!senderBal) throw new Error(`Sender ${senderId} not found`);
  if (!recipientBal) throw new Error(`Recipient ${recipientId} not found`);

  const senderDebt = Math.max(0, -senderBal.net);
  const recipientCredit = Math.max(0, recipientBal.net);

  if (senderDebt <= 0) throw new Error('Nothing to settle');
  if (recipientCredit <= 0) throw new Error('Recipient is not owed anything');

  if (amountPaisa > senderDebt) {
    throw new Error(`You only owe PKR ${fromPaisa(senderDebt)}`);
  }
  if (amountPaisa > recipientCredit) {
    throw new Error(`Recipient is only owed PKR ${fromPaisa(recipientCredit)}`);
  }
}

// S4.6: Greedy debt simplification
function simplify(balances) {
  const debtors = balances
    .filter(b => b.net < 0)
    .map(b => ({ id: b.member_id, debt: -b.net }))
    .sort((a, b) => b.debt - a.debt || a.id.localeCompare(b.id));

  const creditors = balances
    .filter(b => b.net > 0)
    .map(b => ({ id: b.member_id, credit: b.net }))
    .sort((a, b) => b.credit - a.credit || a.id.localeCompare(b.id));

  const transfers = [];
  let dIdx = 0;
  let cIdx = 0;

  while (dIdx < debtors.length && cIdx < creditors.length) {
    const d = debtors[dIdx];
    const c = creditors[cIdx];
    const amount = Math.min(d.debt, c.credit);

    if (amount > 0) {
      transfers.push({
        from: d.id,
        to: c.id,
        amount_paisa: amount,
        amount: fromPaisaNumber(amount)
      });
    }

    d.debt -= amount;
    c.credit -= amount;

    if (d.debt === 0) dIdx++;
    if (c.credit === 0) cIdx++;
  }

  return transfers;
}

module.exports = {
  toPaisa,
  fromPaisa,
  fromPaisaNumber,
  splitEqual,
  validateCustomSplit,
  computeBalances,
  validateSettlement,
  simplify
};
