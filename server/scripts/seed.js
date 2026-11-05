// server/scripts/seed.js
require('dotenv').config();
const bcrypt = require('bcryptjs');
const db = require('../db');
const { toPaisa } = require('../engine/money');

async function seed() {
  const client = await db.getClient();
  try {
    console.log('--- Seeding Hearth Database ---');
    await client.query('BEGIN');

    // Clean existing tables in reverse dependency order
    await client.query(`
      TRUNCATE
        idempotency_keys,
        recurring_runs,
        recurring_templates,
        settlements,
        expense_splits,
        expenses,
        shopping_items,
        refresh_tokens,
        members,
        settings,
        household
      CASCADE
    `);

    // 1. Household & Settings
    await client.query(`
      INSERT INTO household (id, name, invite_code)
      VALUES (1, 'Flat 4B, 3 Gulberg Estate', 'LHR-4B-9872')
    `);

    await client.query(`
      INSERT INTO settings (key, value)
      VALUES ('monthly_budget_paisa', '500000')
    `);

    // 2. Members (Ali M01 Admin, Ayesha M02, Bilal M03)
    const passwordHash = await bcrypt.hash('hearth2026', 10);
    const membersData = [
      { id: 'M01', name: 'Ali', email: 'ali@hearth.pk', role: 'admin' },
      { id: 'M02', name: 'Ayesha', email: 'ayesha@hearth.pk', role: 'member' },
      { id: 'M03', name: 'Bilal', email: 'bilal@hearth.pk', role: 'member' }
    ];

    for (const m of membersData) {
      await client.query(
        'INSERT INTO members (id, name, email, password_hash, role) VALUES ($1, $2, $3, $4, $5)',
        [m.id, m.name, m.email, passwordHash, m.role]
      );
    }

    // 3. Initial Household Expenses:
    // Spend: 4,200.00 / 5,000.00 -> 84% NEAR_LIMIT; Net: Ali +250, Ayesha -450, Bilal +200
    // Exp 1: Grocery Trip (Milk & Rice) — 780.00 paid by Ali (M01), Equal across M01, M02, M03 (260.00 each)
    const exp1Id = 'EXP-0001';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Grocery Trip (Milk & Rice)', 'Groceries', 78000, 'M01', 'EQUAL', CURRENT_DATE, 'checkout', 'M01')
    `, [exp1Id]);
    const exp1Splits = { M01: 26000, M02: 26000, M03: 26000 };
    for (const [mId, share] of Object.entries(exp1Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp1Id, mId, share]
      );
    }

    // Exp 2: Cleaning Supplies — 900.00 paid by Ayesha (M02), Equal between M01 & M02 (450.00 each)
    const exp2Id = 'EXP-0002';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Cleaning Supplies', 'Cleaning', 90000, 'M02', 'EQUAL', CURRENT_DATE, 'manual', 'M02')
    `, [exp2Id]);
    const exp2Splits = { M01: 45000, M02: 45000 };
    for (const [mId, share] of Object.entries(exp2Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp2Id, mId, share]
      );
    }

    // Exp 3: Internet Bill — 1,500.00 paid by Bilal (M03), Custom: Ali 500, Ayesha 300, Bilal 700
    const exp3Id = 'EXP-0003';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by, is_recurring)
      VALUES ($1, 'Internet Bill', 'Utilities', 150000, 'M03', 'CUSTOM', CURRENT_DATE, 'manual', 'M03', true)
    `, [exp3Id]);
    const exp3Splits = { M01: 50000, M02: 30000, M03: 70000 };
    for (const [mId, share] of Object.entries(exp3Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp3Id, mId, share]
      );
    }

    // Exp 4: Electricity Bill — 1,020.00 paid by Ali (M01), Equal across M01, M02, M03 (340.00 each)
    const exp4Id = 'EXP-0004';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Electricity Bill', 'Utilities', 102000, 'M01', 'EQUAL', CURRENT_DATE, 'manual', 'M01')
    `, [exp4Id]);
    const exp4Splits = { M01: 34000, M02: 34000, M03: 34000 };
    for (const [mId, share] of Object.entries(exp4Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp4Id, mId, share]
      );
    }

    // 4. Seed Shopping List
    const shoppingItems = [
      { id: 'SHOP-0001', title: 'Basmati Rice', unit: 'bag', requested_qty: 5, created_by: 'M01' },
      { id: 'SHOP-0002', title: 'Tapal Danedar Tea', unit: 'pack', requested_qty: 1, created_by: 'M02' },
      { id: 'SHOP-0003', title: 'Mineral Water 19L', unit: 'bottle', requested_qty: 2, created_by: 'M03' },
      { id: 'SHOP-0004', title: 'Farm Fresh Eggs', unit: 'dozen', requested_qty: 2, created_by: 'M01' }
    ];

    for (const item of shoppingItems) {
      await client.query(
        'INSERT INTO shopping_items (id, title, unit, requested_qty, status, created_by) VALUES ($1, $2, $3, $4, \'pending\', $5)',
        [item.id, item.title, item.unit, item.requested_qty, item.created_by]
      );
    }

    // 5. Seed Recurring Template (Internet Bill)
    await client.query(`
      INSERT INTO recurring_templates (id, title, category, amount_paisa, payer_id, split_type, splits_json, active)
      VALUES ('REC-0001', 'Nayatel Fiber Internet 50Mbps', 'Utilities', 300000, 'M01', 'EQUAL', '{"M01": 100000, "M02": 100000, "M03": 100000}', true)
    `);

    await client.query('COMMIT');
    console.log('✅ Hearth Database Seeded successfully!');
    console.log('   Household: Flat 4B, 3 Gulberg Estate (Code: LHR-4B-9872)');
    console.log('   Total Spend: PKR 4,200.00 | Remaining: PKR 800.00 (84% NEAR_LIMIT)');
    console.log('   Net Balances: Ali +250.00 | Ayesha -450.00 | Bilal +200.00 | Sum 0.00');
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Seeding failed:', err);
    process.exit(1);
  } finally {
    client.release();
    await db.pool.end();
  }
}

if (require.main === module) {
  seed();
}

module.exports = seed;
