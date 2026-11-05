// server/modules/shopping/shopping.routes.js
const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const db = require('../../db');
const { requireAuth, idempotency } = require('../../middleware/auth');
const { toPaisa, fromPaisaNumber, splitEqual } = require('../../engine/money');

// GET /api/shopping?status=pending|bought
router.get('/', requireAuth, async (req, res) => {
  const { status } = req.query;
  try {
    let sql = 'SELECT * FROM shopping_items ORDER BY created_at DESC';
    let params = [];
    if (status) {
      sql = 'SELECT * FROM shopping_items WHERE status = $1 ORDER BY created_at DESC';
      params = [status];
    }
    const { rows } = await db.query(sql, params);
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch shopping items' } });
  }
});

// POST /api/shopping
router.post('/', requireAuth, async (req, res) => {
  const { title, unit, requested_qty } = req.body;
  if (!title || title.trim().length === 0 || title.trim().length > 80) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Title 1-80 chars required' } });
  }
  const validUnits = ['pack', 'dozen', 'bottle', 'bag'];
  if (!unit || !validUnits.includes(unit.toLowerCase())) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: `Unit must be one of: ${validUnits.join(', ')}` } });
  }
  const qty = parseInt(requested_qty, 10);
  if (isNaN(qty) || qty < 1 || qty > 999) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Quantity must be 1-999' } });
  }

  try {
    // S5.2: Duplicate pending title check (case-insensitive)
    const dupRes = await db.query(
      'SELECT id FROM shopping_items WHERE LOWER(title) = LOWER($1) AND status = \'pending\'',
      [title.trim()]
    );
    if (dupRes.rows.length > 0) {
      return res.status(409).json({ error: { code: 'CONFLICT', message: `'${title.trim()}' is already on the shopping list` } });
    }

    const id = `SHOP-${crypto.randomUUID().slice(0, 6).toUpperCase()}`;
    const insertRes = await db.query(
      'INSERT INTO shopping_items (id, title, unit, requested_qty, status, created_by) VALUES ($1, $2, $3, $4, \'pending\', $5) RETURNING *',
      [id, title.trim(), unit.toLowerCase(), qty, req.user.id]
    );

    res.status(201).json(insertRes.rows[0]);
  } catch (err) {
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to add item' } });
  }
});

// POST /api/shopping/checkout (S5.3: Transaction + Advisory Lock + Equal Split)
router.post('/checkout', requireAuth, idempotency, async (req, res) => {
  const { item_ids, total_paid, payer_id, split_members, tax_percent, discount_amount } = req.body;

  if (!item_ids || !Array.isArray(item_ids) || item_ids.length === 0) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'item_ids array required' } });
  }
  if (!split_members || !Array.isArray(split_members) || split_members.length === 0) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'split_members array required' } });
  }

  let totalPaidPaisa;
  try {
    totalPaidPaisa = toPaisa(total_paid);
  } catch (e) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: e.message } });
  }

  const client = await db.getClient();
  try {
    await client.query('BEGIN');
    // pg_advisory_xact_lock to prevent double-checkout race conditions
    await client.query('SELECT pg_advisory_xact_lock(42)');

    // SELECT items FOR UPDATE
    const itemsRes = await client.query(
      'SELECT id, title, status FROM shopping_items WHERE id = ANY($1) FOR UPDATE',
      [item_ids]
    );

    if (itemsRes.rows.length !== item_ids.length) {
      await client.query('ROLLBACK');
      return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'One or more items do not exist' } });
    }

    const alreadyBought = itemsRes.rows.filter(it => it.status === 'bought');
    if (alreadyBought.length > 0) {
      await client.query('ROLLBACK');
      return res.status(409).json({
        error: {
          code: 'CONFLICT',
          message: `Some items were already bought: ${alreadyBought.map(b => b.title).join(', ')}`
        }
      });
    }

    // Split equal
    const sortedMembers = [...new Set(split_members)].sort();
    const shares = splitEqual(totalPaidPaisa, sortedMembers);

    const expenseId = `EXP-${crypto.randomUUID().slice(0, 6).toUpperCase()}`;
    const titles = itemsRes.rows.map(it => it.title).slice(0, 3).join(', ');
    const expenseTitle = (`Groceries: ${titles}`).slice(0, 80);
    const today = new Date().toISOString().slice(0, 10);

    const subtotalPaisa = totalPaidPaisa;
    const taxNum = tax_percent ? parseFloat(tax_percent) : 0;
    const discountPaisa = discount_amount ? toPaisa(discount_amount, false) : 0;

    await client.query(
      `INSERT INTO expenses
        (id, title, category, amount_paisa, payer_id, split_type, expense_date, source, created_by, subtotal_paisa, tax_percent, discount_paisa)
       VALUES ($1, $2, 'Groceries', $3, $4, 'EQUAL', $5, 'checkout', $6, $7, $8, $9)`,
      [expenseId, expenseTitle, totalPaidPaisa, payer_id || req.user.id, today, req.user.id, subtotalPaisa, taxNum, discountPaisa]
    );

    for (const [memberId, sharePaisa] of Object.entries(shares)) {
      await client.query(
        'INSERT INTO expense_splits (expense_id, member_id, share_paisa) VALUES ($1, $2, $3)',
        [expenseId, memberId, sharePaisa]
      );
    }

    // Mark items bought
    await client.query(
      'UPDATE shopping_items SET status = \'bought\', expense_id = $1, bought_at = NOW() WHERE id = ANY($2)',
      [expenseId, item_ids]
    );

    await client.query('COMMIT');

    const splitDtos = Object.entries(shares).map(([mId, sh]) => ({
      member_id: mId,
      amount: fromPaisaNumber(sh)
    }));

    res.status(201).json({
      expense: {
        id: expenseId,
        title: expenseTitle,
        category: 'Groceries',
        amount: fromPaisaNumber(totalPaidPaisa),
        payer_id: payer_id || req.user.id,
        split_type: 'EQUAL',
        splits: splitDtos
      },
      items: item_ids
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Checkout error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Checkout transaction failed' } });
  } finally {
    client.release();
  }
});

module.exports = router;
