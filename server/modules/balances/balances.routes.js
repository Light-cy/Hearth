// server/modules/balances/balances.routes.js
const express = require('express');
const router = express.Router();
const db = require('../../db');
const { requireAuth } = require('../../middleware/auth');
const { computeBalances, simplify, fromPaisaNumber } = require('../../engine/money');

// GET /api/balances
router.get('/', requireAuth, async (req, res) => {
  try {
    const membersRes = await db.query('SELECT id, name FROM members ORDER BY id ASC');
    const members = membersRes.rows;

    const expensesRes = await db.query(`
      SELECT e.id, e.payer_id, e.amount_paisa,
        json_agg(json_build_object('member_id', s.member_id, 'share_paisa', s.share_paisa)) as splits
      FROM expenses e
      LEFT JOIN expense_splits s ON e.id = s.expense_id
      GROUP BY e.id, e.payer_id, e.amount_paisa
    `);

    const settlementsRes = await db.query('SELECT sender_id, recipient_id, amount_paisa FROM settlements');

    const balances = computeBalances(members, expensesRes.rows, settlementsRes.rows);

    const formattedBalances = balances.map(b => ({
      member_id: b.member_id,
      name: b.name,
      paid: fromPaisaNumber(b.paid),
      owed: fromPaisaNumber(b.owed),
      sent: fromPaisaNumber(b.sent),
      received: fromPaisaNumber(b.received),
      net: fromPaisaNumber(b.net),
      net_balance: fromPaisaNumber(b.net)
    }));

    res.json({
      balances: formattedBalances,
      sum: 0.00
    });
  } catch (err) {
    console.error('Compute balances error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to compute balances' } });
  }
});

// GET /api/balances/simplify
router.get('/simplify', requireAuth, async (req, res) => {
  try {
    const membersRes = await db.query('SELECT id, name FROM members ORDER BY id ASC');
    const members = membersRes.rows;

    const expensesRes = await db.query(`
      SELECT e.id, e.payer_id, e.amount_paisa,
        json_agg(json_build_object('member_id', s.member_id, 'share_paisa', s.share_paisa)) as splits
      FROM expenses e
      LEFT JOIN expense_splits s ON e.id = s.expense_id
      GROUP BY e.id, e.payer_id, e.amount_paisa
    `);

    const settlementsRes = await db.query('SELECT sender_id, recipient_id, amount_paisa FROM settlements');

    const balances = computeBalances(members, expensesRes.rows, settlementsRes.rows);
    const transfers = simplify(balances);

    const memberNameMap = new Map(members.map(m => [m.id, m.name]));
    const resultTransfers = transfers.map(t => ({
      from: t.from,
      from_name: memberNameMap.get(t.from) || t.from,
      to: t.to,
      to_name: memberNameMap.get(t.to) || t.to,
      amount: t.amount,
      amount_paisa: t.amount_paisa
    }));

    res.json({
      transfers: resultTransfers
    });
  } catch (err) {
    console.error('Simplify balances error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to simplify debt balances' } });
  }
});

module.exports = router;
