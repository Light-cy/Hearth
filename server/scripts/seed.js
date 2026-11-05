// server/scripts/seed.js
require('dotenv').config();
const bcrypt = require('bcryptjs');
const db = require('../db');
const { toPaisa } = require('../engine/money');

async function seed() {
  const client = await db.getClient();
  try {
    console.log('--- Seeding Hisaab Database ---');
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
      VALUES (1, 'Roommates Flat 4B, Lahore', 'LHR-4B-9872')
    `);

    await client.query(`
      INSERT INTO settings (key, value)
      VALUES ('monthly_budget_paisa', '500000')
    `);

    // 2. Members (Ali M01 Admin, Ayesha M02, Bilal M03)
    const passwordHash = await bcrypt.hash('hisaab2026', 10);
    const membersData = [
      { id: 'M01', name: 'Ali', email: 'ali@hisaab.local', role: 'admin' },
      { id: 'M02', name: 'Ayesha', email: 'ayesha@hisaab.local', role: 'member' },
      { id: 'M03', name: 'Bilal', email: 'bilal@hisaab.local', role: 'member' }
    ];

    for (const m of membersData) {
      await client.query(
        'INSERT INTO members (id, name, email, password_hash, role) VALUES ($1, $2, $3, $4, $5)',
        [m.id, m.name, m.email, passwordHash, m.role]
      );
    }

    // 3. Four Benchmark Expenses (Spend: 4,200.00 / 5,000.00 -> 84% NEAR_LIMIT; Net: Ali +250, Ayesha -450, Bilal +200)
    // Exp 1: Groceries 1,200.00 paid by Ali
    const exp1Id = 'EXP-0001';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Monthly Groceries & Pantry', 'Groceries', 120000, 'M01', 'CUSTOM', CURRENT_DATE, 'manual', 'M01')
    `, [exp1Id]);
    const exp1Splits = { M01: 30000, M02: 10000, M03: 80000 };
    for (const [mId, share] of Object.entries(exp1Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp1Id, mId, share]
      );
    }

    // Exp 2: Utilities 2,520.00 paid by Bilal
    const exp2Id = 'EXP-0002';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Electricity & Fiber Internet', 'Utilities', 252000, 'M03', 'CUSTOM', CURRENT_DATE, 'manual', 'M03')
    `, [exp2Id]);
    const exp2Splits = { M01: 100000, M02: 20000, M03: 132000 };
    for (const [mId, share] of Object.entries(exp2Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp2Id, mId, share]
      );
    }

    // Exp 3: Cleaning Supplies 300.00 paid by Ali
    const exp3Id = 'EXP-0003';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Apartment Cleaning Supplies', 'Cleaning', 30000, 'M01', 'CUSTOM', CURRENT_DATE, 'manual', 'M01')
    `, [exp3Id]);
    const exp3Splits = { M01: 8000, M02: 7000, M03: 15000 };
    for (const [mId, share] of Object.entries(exp3Splits)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [exp3Id, mId, share]
      );
    }

    // Exp 4: Dining 180.00 paid by Ali
    const exp4Id = 'EXP-0004';
    await client.query(`
      INSERT INTO expenses (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by)
      VALUES ($1, 'Weekend Dinner Takeout', 'Dining', 18000, 'M01', 'CUSTOM', CURRENT_DATE, 'manual', 'M01')
    `, [exp4Id]);
    const exp4Splits = { M01: 5000, M02: 8000, M03: 5000 };
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
    console.log('✅ Hisaab Database Seeded successfully!');
    console.log('   Household: Roommates Flat 4B, Lahore (Code: LHR-4B-9872)');
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
