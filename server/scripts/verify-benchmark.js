/**
 * verify-benchmark.js
 * Validates the pure arithmetic and invariant properties of the Hearth household ledger engine.
 * Can be run standalone with `node server/scripts/verify-benchmark.js`.
 */
const {
  toPaisa,
  fromPaisa,
  fromPaisaNumber,
  splitEqual,
  validateCustomSplit,
  computeBalances,
  validateSettlement,
  simplify
} = require('../engine/money');

function assert(condition, message) {
  if (!condition) {
    console.error(`❌ ASSERTION FAILED: ${message}`);
    process.exit(1);
  }
}

console.log('--- Verifying Hearth Household Ledger Math & Invariants ---');

// 1. Members
const members = [
  { id: 'M01', name: 'Ali' },
  { id: 'M02', name: 'Ayesha' },
  { id: 'M03', name: 'Bilal' }
];

// 2. Budget: 5,000.00
const budgetPaisa = toPaisa(5000);
assert(budgetPaisa === 500000, `Budget paisa should be 500,000, got ${budgetPaisa}`);

// 3. Initial Household Expenses
// Exp 1: Grocery Trip (Milk & Rice) — 780.00 paid by Ali (M01), Equal across M01, M02, M03 (260.00 each)
const exp1Paisa = toPaisa(780);
const exp1Shares = splitEqual(exp1Paisa, ['M01', 'M02', 'M03']);
const exp1Splits = Object.entries(exp1Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 2: Cleaning Supplies — 900.00 paid by Ayesha (M02), Equal between M01 & M02 (450.00 each)
const exp2Paisa = toPaisa(900);
const exp2Shares = splitEqual(exp2Paisa, ['M01', 'M02']);
const exp2Splits = Object.entries(exp2Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 3: Internet Bill — 1,500.00 paid by Bilal (M03), Custom: Ali 500, Ayesha 300, Bilal 700
const exp3Paisa = toPaisa(1500);
const exp3Shares = { M01: 50000, M02: 30000, M03: 70000 };
validateCustomSplit(exp3Paisa, exp3Shares);
const exp3Splits = Object.entries(exp3Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 4: Electricity Bill — 1,020.00 paid by Ali (M01), Equal across M01, M02, M03 (340.00 each)
const exp4Paisa = toPaisa(1020);
const exp4Shares = splitEqual(exp4Paisa, ['M01', 'M02', 'M03']);
const exp4Splits = Object.entries(exp4Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Total spend check
const totalSpendPaisa = exp1Paisa + exp2Paisa + exp3Paisa + exp4Paisa;
assert(totalSpendPaisa === 420000, 'Total spend should be PKR 4,200.00');
const remainingPaisa = budgetPaisa - totalSpendPaisa;
assert(remainingPaisa === 80000, 'Remaining should be PKR 800.00');
const percentUsed = (totalSpendPaisa / budgetPaisa) * 100;
assert(percentUsed === 84, 'Percent used should be exactly 84%');

console.log(`✅ Total Spend: PKR ${fromPaisa(totalSpendPaisa)} | Remaining: PKR ${fromPaisa(remainingPaisa)} | Usage: ${percentUsed}% (NEAR_LIMIT)`);

// 4. Balances Check
const expenses = [
  { id: 'EXP-1', payer_id: 'M01', amount_paisa: exp1Paisa, splits: exp1Splits },
  { id: 'EXP-2', payer_id: 'M02', amount_paisa: exp2Paisa, splits: exp2Splits },
  { id: 'EXP-3', payer_id: 'M03', amount_paisa: exp3Paisa, splits: exp3Splits },
  { id: 'EXP-4', payer_id: 'M01', amount_paisa: exp4Paisa, splits: exp4Splits }
];

const balances = computeBalances(members, expenses, []);

const aliBal = balances.find(b => b.member_id === 'M01');
const ayeshaBal = balances.find(b => b.member_id === 'M02');
const bilalBal = balances.find(b => b.member_id === 'M03');

assert(aliBal.paid === 180000 && aliBal.owed === 155000 && aliBal.net === 25000, `Ali should be Paid 1800, Owed 1550, Net +250`);
assert(ayeshaBal.paid === 90000 && ayeshaBal.owed === 135000 && ayeshaBal.net === -45000, `Ayesha should be Paid 900, Owed 1350, Net -450`);
assert(bilalBal.paid === 150000 && bilalBal.owed === 130000 && bilalBal.net === 20000, `Bilal should be Paid 1500, Owed 1300, Net +200`);

const aliNet = aliBal.net;
const ayeshaNet = ayeshaBal.net;
const bilalNet = bilalBal.net;
assert(aliNet + ayeshaNet + bilalNet === 0, 'Sum of all net balances MUST strictly equal 0');

console.log(`✅ Net Balances: Ali +${fromPaisa(aliNet)} | Ayesha ${fromPaisa(ayeshaNet)} | Bilal +${fromPaisa(bilalNet)} | Sum = 0.00`);

// 5. Smart Settle Debt Simplification Check
const transfers = simplify(balances);
assert(transfers.length === 2, `Expected 2 transfers, got ${transfers.length}`);

const toAli = transfers.find(t => t.to === 'M01');
const toBilal = transfers.find(t => t.to === 'M03');
assert(toAli && toAli.from === 'M02' && toAli.amount_paisa === 25000, 'Ayesha must pay Ali 250.00');
assert(toBilal && toBilal.from === 'M02' && toBilal.amount_paisa === 20000, 'Ayesha must pay Bilal 200.00');

console.log('✅ Smart Settle Transfers:');
for (const t of transfers) {
  console.log(`   ${t.from} pays ${t.to}: PKR ${fromPaisa(t.amount_paisa)}`);
}

// 6. Settle Validation & Zero-Sum Post-Settlement Invariant
validateSettlement('M02', 'M01', 25000, balances);
console.log('✅ Settlement of 250.00 from M02 to M01 is valid');

const postSettlements = [
  { sender_id: 'M02', recipient_id: 'M01', amount_paisa: 25000 },
  { sender_id: 'M02', recipient_id: 'M03', amount_paisa: 20000 }
];
const postBalances = computeBalances(members, expenses, postSettlements);
const aliPost = postBalances.find(b => b.member_id === 'M01').net;
const ayeshaPost = postBalances.find(b => b.member_id === 'M02').net;
const bilalPost = postBalances.find(b => b.member_id === 'M03').net;

assert(aliPost === 0, 'Ali should have 0.00 post settlements');
assert(ayeshaPost === 0, 'Ayesha should have 0.00 post settlements');
assert(bilalPost === 0, 'Bilal should have 0.00 post settlements');

console.log('✅ Post Full Settlement Balances: Ali 0.00 | Ayesha 0.00 | Bilal 0.00');
console.log('🎉 ALL HACKATHON BENCHMARK INVARIANTS VERIFIED SUCCESSFULLY!');
