// server/modules/members/members.routes.js
const express = require('express');
const router = express.Router();
const db = require('../../db');
const { requireAuth } = require('../../middleware/auth');

// GET /api/members
router.get('/', requireAuth, async (req, res) => {
  try {
    const { rows } = await db.query('SELECT id, name, email, role, created_at FROM members ORDER BY id ASC');
    res.json(rows.map(r => ({
      id: r.id,
      name: r.name,
      email: r.email,
      role: r.role,
      created_at: r.created_at.toISOString()
    })));
  } catch (err) {
    console.error('Fetch members error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch household members' } });
  }
});

module.exports = router;
