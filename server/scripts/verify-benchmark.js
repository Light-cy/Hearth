/**
 * verify-benchmark.js
 * Validates the pure arithmetic and invariant properties of the Hisaab benchmark scenario.
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

console.log('--- Verifying Hisaab Hackathon Benchmark Math ---');

// 1. Members
const members = [
  { id: 'M01', name: 'Ali' },
  { id: 'M02', name: 'Ayesha' },
  { id: 'M03', name: 'Bilal' }
];

// 2. Budget: 5,000.00
const budgetPaisa = toPaisa(5000);
assert(budgetPaisa === 500000, `Budget paisa should be 500,000, got ${budgetPaisa}`);

// 3. Four Benchmark Expenses
// Exp 1: Groceries 1,200.00 paid by Ali
const exp1Paisa = toPaisa(1200);
const exp1Shares = { M01: 30000, M02: 10000, M03: 80000 };
validateCustomSplit(exp1Paisa, exp1Shares);
const exp1Splits = Object.entries(exp1Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 2: Utilities 2,520.00 paid by Bilal
const exp2Paisa = toPaisa(2520);
const exp2Shares = { M01: 100000, M02: 20000, M03: 132000 };
validateCustomSplit(exp2Paisa, exp2Shares);
const exp2Splits = Object.entries(exp2Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 3: Cleaning Supplies 300.00 paid by Ali
const exp3Paisa = toPaisa(300);
const exp3Shares = { M01: 8000, M02: 7000, M03: 15000 };
validateCustomSplit(exp3Paisa, exp3Shares);
const exp3Splits = Object.entries(exp3Shares).map(([mId, share]) => ({ member_id: mId, share_paisa: share }));

// Exp 4: Dining 180.00 paid by Ali
const exp4Paisa = toPaisa(180);
const exp4Shares = { M01: 5000, M02: 8000, M03: 5000 };
validateCustomSplit(exp4Paisa, exp4Shares);
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
  { id: 'EXP-2', payer_id: 'M03', amount_paisa: exp2Paisa, splits: exp2Splits },
  { id: 'EXP-3', payer_id: 'M01', amount_paisa: exp3Paisa, splits: exp3Splits },
  { id: 'EXP-4', payer_id: 'M01', amount_paisa: exp4Paisa, splits: exp4Splits }
];

const balances = computeBalances(members, expenses, []);

const aliNet = balances.find(b => b.member_id === 'M01').net;
const ayeshaNet = balances.find(b => b.member_id === 'M02').net;
const bilalNet = balances.find(b => b.member_id === 'M03').net;

assert(aliNet === 25000, `Ali net should be +250.00, got ${aliNet}`);
assert(ayeshaNet === -45000, `Ayesha net should be -450.00, got ${ayeshaNet}`);
assert(bilalNet === 20000, `Bilal net should be +200.00, got ${bilalNet}`);
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
