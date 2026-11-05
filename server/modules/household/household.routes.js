// server/modules/household/household.routes.js
const express = require('express');
const crypto = require('crypto');
const router = express.Router();
const db = require('../../db');
const { requireAuth, requireAdmin } = require('../../middleware/auth');
const { fromPaisaNumber } = require('../../engine/money');

// GET /api/household
router.get('/', requireAuth, async (req, res) => {
  try {
    const hhRes = await db.query('SELECT * FROM household WHERE id = 1');
    if (hhRes.rows.length === 0) {
      return res.status(404).json({ error: { code: 'NOT_FOUND', message: 'Household not found' } });
    }
    const hh = hhRes.rows[0];

    const countRes = await db.query('SELECT COUNT(*) FROM members');
    const memberCount = parseInt(countRes.rows[0].count, 10);

    const budgetSetting = await db.query("SELECT value FROM settings WHERE key = 'monthly_budget_paisa'");
    const budgetPaisa = budgetSetting.rows.length > 0 ? parseInt(budgetSetting.rows[0].value, 10) : 500000;

    const data = {
      id: hh.id,
      name: hh.name,
      currency: 'PKR',
      monthly_budget: fromPaisaNumber(budgetPaisa),
      member_count: memberCount
    };

    // S5.1: Only admin sees household invite code
    if (req.user.role === 'admin') {
      data.invite_code = hh.invite_code;
    }

    res.json(data);
  } catch (err) {
    console.error('Fetch household error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch household profile' } });
  }
});

// POST /api/household/invite/rotate
router.post('/invite/rotate', requireAuth, requireAdmin, async (req, res) => {
  try {
    const newCode = `LHR-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;
    await db.query('UPDATE household SET invite_code = $1 WHERE id = 1', [newCode]);

    res.json({
      invite_code: newCode,
      message: 'Household invite code rotated successfully'
    });
  } catch (err) {
    console.error('Rotate invite code error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to rotate invite code' } });
  }
});

module.exports = router;
