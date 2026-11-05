// server/test/money-engine.test.js
const { test, describe } = require('node:test');
const assert = require('node:assert');
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

describe('Hisaab Pure Money Engine & Invariants', () => {

  test('toPaisa converts valid decimals and throws on invalid inputs', () => {
    assert.strictEqual(toPaisa('333.34'), 33334);
    assert.strictEqual(toPaisa(4200), 420000);
    assert.strictEqual(toPaisa('5,000.00'), 500000);
    assert.strictEqual(fromPaisa(33334), '333.34');
    assert.strictEqual(fromPaisa(-45000), '-450.00');
    assert.strictEqual(fromPaisaNumber(20000), 200.00);

    assert.throws(() => toPaisa('10.005'), /at most 2 decimal places/);
    assert.throws(() => toPaisa('0.00'), /must be positive/);
    assert.throws(() => toPaisa('-50.00'), /must be positive/);
    assert.throws(() => toPaisa('NaN'), /Invalid decimal format/);
    assert.throws(() => toPaisa('1e3'), /Invalid decimal format/);
  });

  test('splitEqual: ascending member remainder allocation', () => {
    // S4.2: 100000 / 3 gives [33334, 33333, 33333]
    const split1 = splitEqual(100000, ['M03', 'M01', 'M02']);
    assert.strictEqual(split1['M01'], 33334);
    assert.strictEqual(split1['M02'], 33333);
    assert.strictEqual(split1['M03'], 33333);

    // 1 paisa across 3 members
    const split2 = splitEqual(1, ['M01', 'M02', 'M03']);
    assert.strictEqual(split2['M01'], 1);
    assert.strictEqual(split2['M02'], 0);
    assert.strictEqual(split2['M03'], 0);

    // Integrity: sum of shares equals total
    const sum = Object.values(split1).reduce((a, b) => a + b, 0);
    assert.strictEqual(sum, 100000);
  });

  test('validateCustomSplit: checks sum and non-negative shares', () => {
    const validShares = { M01: 5000, M02: 15000, M03: 10000 };
    assert.doesNotThrow(() => validateCustomSplit(30000, validShares));

    const invalidShares = { M01: 5000, M02: 14000, M03: 10000 };
    assert.throws(() => validateCustomSplit(30000, invalidShares), /under by PKR 10.00/);

    const negativeShares = { M01: -100, M02: 30100 };
    assert.throws(() => validateCustomSplit(30000, negativeShares), /cannot be negative/);
  });

  test('computeBalances: Net = Paid - Owed + Sent - Received with sum == 0', () => {
    const members = [
      { id: 'M01', name: 'Ali' },
      { id: 'M02', name: 'Ayesha' },
      { id: 'M03', name: 'Bilal' }
    ];

    const expenses = [
      {
        id: 'EXP-1',
        payer_id: 'M01',
        amount_paisa: 120000,
        splits: [
          { member_id: 'M01', share_paisa: 30000 },
          { member_id: 'M02', share_paisa: 10000 },
          { member_id: 'M03', share_paisa: 80000 }
        ]
      },
      {
        id: 'EXP-2',
        payer_id: 'M03',
        amount_paisa: 252000,
        splits: [
          { member_id: 'M01', share_paisa: 100000 },
          { member_id: 'M02', share_paisa: 20000 },
          { member_id: 'M03', share_paisa: 132000 }
        ]
      },
      {
        id: 'EXP-3',
        payer_id: 'M01',
        amount_paisa: 30000,
        splits: [
          { member_id: 'M01', share_paisa: 8000 },
          { member_id: 'M02', share_paisa: 7000 },
          { member_id: 'M03', share_paisa: 15000 }
        ]
      },
      {
        id: 'EXP-4',
        payer_id: 'M01',
        amount_paisa: 18000,
        splits: [
          { member_id: 'M01', share_paisa: 5000 },
          { member_id: 'M02', share_paisa: 8000 },
          { member_id: 'M03', share_paisa: 5000 }
        ]
      }
    ];

    const balances = computeBalances(members, expenses, []);
    const ali = balances.find(b => b.member_id === 'M01');
    const ayesha = balances.find(b => b.member_id === 'M02');
    const bilal = balances.find(b => b.member_id === 'M03');

    assert.strictEqual(ali.net, 25000);   // +250.00
    assert.strictEqual(ayesha.net, -45000); // -450.00
    assert.strictEqual(bilal.net, 20000);  // +200.00
    assert.strictEqual(ali.net + ayesha.net + bilal.net, 0);
  });

  test('validateSettlement and simplify (Smart Settle)', () => {
    const balances = [
      { member_id: 'M01', net: 25000 },
      { member_id: 'M02', net: -45000 },
      { member_id: 'M03', net: 20000 }
    ];

    assert.doesNotThrow(() => validateSettlement('M02', 'M01', 25000, balances));
    assert.throws(() => validateSettlement('M02', 'M01', 30000, balances), /Recipient is only owed PKR 250.00/);
    assert.throws(() => validateSettlement('M01', 'M02', 1000, balances), /Nothing to settle/);
    assert.throws(() => validateSettlement('M02', 'M02', 5000, balances), /Cannot settle with yourself/);

    const transfers = simplify(balances);
    assert.strictEqual(transfers.length, 2);
    assert.strictEqual(transfers[0].from, 'M02');
    assert.strictEqual(transfers[0].to, 'M01');
    assert.strictEqual(transfers[0].amount_paisa, 25000);
    assert.strictEqual(transfers[1].from, 'M02');
    assert.strictEqual(transfers[1].to, 'M03');
    assert.strictEqual(transfers[1].amount_paisa, 20000);
  });
});
