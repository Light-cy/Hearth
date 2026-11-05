// server/modules/expenses/expenses.routes.js
const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const db = require('../../db');
const { requireAuth, idempotency } = require('../../middleware/auth');
const { toPaisa, fromPaisaNumber, splitEqual, validateCustomSplit } = require('../../engine/money');

// GET /api/expenses?month=YYYY-MM
router.get('/', requireAuth, async (req, res) => {
  const { month } = req.query;
  try {
    let sql = `
      SELECT e.*,
        json_agg(json_build_object('member_id', s.member_id, 'share_paisa', s.share_paisa)) as splits
      FROM expenses e
      LEFT JOIN expense_splits s ON e.id = s.expense_id
    `;
    const params = [];
    if (month) {
      sql += ' WHERE to_char(e.expense_date, \'YYYY-MM\') = $1';
      params.push(month);
    }
    sql += ' GROUP BY e.id ORDER BY e.expense_date DESC, e.created_at DESC';

    const { rows } = await db.query(sql, params);
    const result = rows.map(r => ({
      id: r.id,
      title: r.title,
      category: r.category,
      amount: fromPaisaNumber(r.amount_paisa),
      amount_paisa: Number(r.amount_paisa),
      payer_id: r.payer_id,
      split_type: r.split_type,
      expense_date: r.expense_date.toISOString().slice(0, 10),
      source: r.source,
      is_recurring: r.is_recurring,
      splits: (r.splits || []).filter(s => s && s.member_id).map(s => ({
        member_id: s.member_id,
        amount: fromPaisaNumber(s.share_paisa)
      }))
    }));

    res.json(result);
  } catch (err) {
    console.error('Fetch expenses error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch expenses' } });
  }
});

// POST /api/expenses (S5.4: Manual Expense)
router.post('/', requireAuth, idempotency, async (req, res) => {
  const { title, category, amount, payer_id, split_type, splits, expense_date, is_recurring } = req.body;

  if (!title || title.trim().length === 0 || title.trim().length > 80) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Title 1-80 characters required' } });
  }
  const validCategories = ['Groceries', 'Utilities', 'Cleaning', 'Entertainment', 'Dining', 'Other'];
  if (!category || !validCategories.includes(category)) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: `Category must be one of: ${validCategories.join(', ')}` } });
  }

  let amountPaisa;
  try {
    amountPaisa = toPaisa(amount);
  } catch (e) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: e.message } });
  }

  const client = await db.getClient();
  try {
    await client.query('BEGIN');
    await client.query('SELECT pg_advisory_xact_lock(43)');

    let shareMap = {};
    if (split_type === 'EQUAL') {
      const { rows: memberRows } = await client.query('SELECT id FROM members ORDER BY id ASC');
      const targetMemberIds = memberRows.map(m => m.id);
      shareMap = splitEqual(amountPaisa, targetMemberIds);
    } else if (split_type === 'CUSTOM') {
      if (!splits || !Array.isArray(splits) || splits.length === 0) {
        await client.query('ROLLBACK');
        return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Custom splits array required' } });
      }
      for (const sp of splits) {
        shareMap[sp.member_id] = toPaisa(sp.amount, false);
      }
      validateCustomSplit(amountPaisa, shareMap);
    } else {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'split_type must be EQUAL or CUSTOM' } });
    }

    const expenseId = `EXP-${crypto.randomUUID().slice(0, 6).toUpperCase()}`;
    const dateVal = expense_date || new Date().toISOString().slice(0, 10);

    await client.query(
      `INSERT INTO expenses
        (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by, is_recurring)
       VALUES ($1, $2, $3, $4, $5, $6, $7, 'manual', $8, $9)`,
      [expenseId, title.trim(), category, amountPaisa, payer_id || req.user.id, split_type, dateVal, req.user.id, !!is_recurring]
    );

    for (const [memberId, sharePaisa] of Object.entries(shareMap)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [expenseId, memberId, sharePaisa]
      );
    }

    await client.query('COMMIT');

    const splitDtos = Object.entries(shareMap).map(([mId, sh]) => ({
      member_id: mId,
      amount: fromPaisaNumber(sh)
    }));

    res.status(201).json({
      id: expenseId,
      title: title.trim(),
      category,
      amount: fromPaisaNumber(amountPaisa),
      payer_id: payer_id || req.user.id,
      split_type,
      expense_date: dateVal,
      splits: splitDtos,
      is_recurring: !!is_recurring
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Add expense error:', err);
    res.status(400).json({ error: { code: 'BUSINESS_RULE', message: err.message || 'Failed to add expense' } });
  } finally {
    client.release();
  }
});

// POST /api/expenses/recurring/apply (Recurring Monthly Bills Generator)
router.post('/recurring/apply', requireAuth, async (req, res) => {
  const currentMonth = new Date().toISOString().slice(0, 7);
  const today = new Date().toISOString().slice(0, 10);
  const client = await db.getClient();

  try {
    await client.query('BEGIN');
    const { rows: templates } = await client.query('SELECT * FROM expenses WHERE is_recurring = true');

    let created = 0;
    let skipped = 0;

    for (const t of templates) {
      const dup = await client.query(
        'SELECT id FROM expenses WHERE title = $1 AND to_char(expense_date, \'YYYY-MM\') = $2 AND source = \'recurring\'',
        [t.title, currentMonth]
      );
      if (dup.rows.length > 0) {
        skipped++;
        continue;
      }

      const newId = `EXP-REC-${crypto.randomUUID().slice(0, 6).toUpperCase()}`;
      await client.query(
        `INSERT INTO expenses
          (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by, is_recurring)
         VALUES ($1, $2, $3, $4, $5, $6, $7, 'recurring', $8, true)`,
        [newId, t.title, t.category, t.amount_paisa, t.payer_id, t.split_type, today, req.user.id]
      );

      // Copy splits
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) SELECT $1, member_id, share_paisa FROM expense_splits WHERE expense_id = $2',
        [newId, t.id]
      );
      created++;
    }

    await client.query('COMMIT');
    res.json({ created, skipped });
  } catch (err) {
    await client.query('ROLLBACK');
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to apply recurring bills' } });
  } finally {
    client.release();
  }
});

module.exports = router;
