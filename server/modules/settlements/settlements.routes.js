// server/modules/settlements/settlements.routes.js
const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const db = require('../../db');
const { requireAuth, idempotency } = require('../../middleware/auth');
const { toPaisa, fromPaisaNumber, computeBalances, validateSettlement } = require('../../engine/money');

// GET /api/settlements
router.get('/', requireAuth, async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT s.*,
        m1.name as sender_name,
        m2.name as recipient_name
      FROM settlements s
      JOIN members m1 ON s.sender_id = m1.id
      JOIN members m2 ON s.recipient_id = m2.id
      ORDER BY s.created_at DESC
    `);

    const result = rows.map(r => ({
      id: r.id,
      sender_id: r.sender_id,
      sender_name: r.sender_name,
      recipient_id: r.recipient_id,
      recipient_name: r.recipient_name,
      amount: fromPaisaNumber(r.amount_paisa),
      amount_paisa: Number(r.amount_paisa),
      recorded_by: r.recorded_by,
      created_at: r.created_at.toISOString()
    }));

    res.json(result);
  } catch (err) {
    console.error('Fetch settlements error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch settlements' } });
  }
});

// POST /api/settle
router.post('/', requireAuth, idempotency, async (req, res) => {
  const { sender_id, recipient_id, amount } = req.body;

  if (!sender_id || !recipient_id || amount === undefined || amount === null) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'sender_id, recipient_id, amount required' } });
  }

  // S4.5: Authorization check (caller must be sender, recipient, or admin)
  if (req.user.id !== sender_id && req.user.id !== recipient_id && req.user.role !== 'admin') {
    return res.status(403).json({ error: { code: 'FORBIDDEN', message: 'Caller must be sender, recipient, or admin to record settlement' } });
  }

  let amountPaisa;
  try {
    amountPaisa = toPaisa(amount);
  } catch (err) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: err.message } });
  }

  const client = await db.getClient();
  try {
    await client.query('BEGIN');
    // S5.5: Advisory transaction lock for settlement serialization
    await client.query('SELECT pg_advisory_xact_lock(44)');

    const membersRes = await client.query('SELECT id, name FROM members');
    const expensesRes = await client.query(`
      SELECT e.id, e.payer_id, e.amount_paisa,
        json_agg(json_build_object('member_id', s.member_id, 'share_paisa', s.share_paisa)) as splits
      FROM expenses e
      LEFT JOIN expense_splits s ON e.id = s.expense_id
      GROUP BY e.id, e.payer_id, e.amount_paisa
    `);
    const settlementsRes = await client.query('SELECT sender_id, recipient_id, amount_paisa FROM settlements');

    const balances = computeBalances(membersRes.rows, expensesRes.rows, settlementsRes.rows);

    // S4.5: Validate settlement debt bounds
    try {
      validateSettlement(sender_id, recipient_id, amountPaisa, balances);
    } catch (valErr) {
      await client.query('ROLLBACK');
      return res.status(422).json({ error: { code: 'BUSINESS_RULE', message: valErr.message } });
    }

    const settlementId = `SETTLE-${crypto.randomUUID().slice(0, 6).toUpperCase()}`;
    await client.query(
      'INSERT INTO settlements (id, sender_id, recipient_id, amount_paisa, recorded_by) VALUES ($1, $2, $3, $4, $5)',
      [settlementId, sender_id, recipient_id, amountPaisa, req.user.id]
    );

    // Compute updated balances
    settlementsRes.rows.push({
      sender_id,
      recipient_id,
      amount_paisa: amountPaisa
    });
    const updatedBalances = computeBalances(membersRes.rows, expensesRes.rows, settlementsRes.rows);

    await client.query('COMMIT');

    const formattedBalances = updatedBalances.map(b => ({
      member_id: b.member_id,
      name: b.name,
      paid: fromPaisaNumber(b.paid),
      owed: fromPaisaNumber(b.owed),
      sent: fromPaisaNumber(b.sent),
      received: fromPaisaNumber(b.received),
      net: fromPaisaNumber(b.net),
      net_balance: fromPaisaNumber(b.net)
    }));

    res.status(201).json({
      settlement_id: settlementId,
      sender_id,
      recipient_id,
      amount: fromPaisaNumber(amountPaisa),
      updated_balances: formattedBalances
    });
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Record settlement error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to record settlement' } });
  } finally {
    client.release();
  }
});

module.exports = router;
