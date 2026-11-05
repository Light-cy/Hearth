// server/modules/auth/auth.routes.js
const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const db = require('../../db');
const { requireAuth, JWT_SECRET } = require('../../middleware/auth');

function generateTokens(member) {
  const accessToken = jwt.sign(
    { sub: member.id, role: member.role },
    JWT_SECRET,
    { algorithm: 'HS256', expiresIn: '15m' }
  );
  const refreshToken = crypto.randomBytes(32).toString('hex');
  const tokenHash = crypto.createHash('sha256').update(refreshToken).digest('hex');
  const familyId = crypto.randomUUID();
  const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 days

  return { accessToken, refreshToken, tokenHash, familyId, expiresAt };
}

// POST /api/auth/register
router.post('/register', async (req, res) => {
  const { name, email, password, invite_code } = req.body;
  if (!name || !email || !password) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Name, email, and password are required' } });
  }

  const normalizedEmail = email.trim().toLowerCase();
  if (password.length < 8 || password.length > 72 || password === email) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Password must be 8-72 characters and not equal email' } });
  }

  try {
    const memberCountRes = await db.query('SELECT COUNT(*) FROM members');
    const memberCount = parseInt(memberCountRes.rows[0].count, 10);
    const isFirstAccount = memberCount === 0;

    let role = 'member';
    let assignedId = `M${String(memberCount + 1).padStart(2, '0')}`;

    if (isFirstAccount) {
      role = 'admin';
      assignedId = 'M01';
      // Create default household if absent
      await db.query(
        "INSERT INTO household (id, name, invite_code) VALUES (1, 'Roommates Flat 4B, Lahore', 'LHR-4B-9872') ON CONFLICT DO NOTHING"
      );
      await db.query(
        "INSERT INTO settings (key, value) VALUES ('monthly_budget_paisa', '500000') ON CONFLICT DO NOTHING"
      );
    } else {
      if (!invite_code) {
        return res.status(422).json({ error: { code: 'BUSINESS_RULE', message: 'Invite code required to join household' } });
      }
      const hhRes = await db.query('SELECT invite_code FROM household WHERE id = 1');
      if (hhRes.rows.length === 0 || hhRes.rows[0].invite_code !== invite_code.trim()) {
        return res.status(422).json({ error: { code: 'BUSINESS_RULE', message: 'Invalid household invite code' } });
      }
    }

    const existing = await db.query('SELECT id FROM members WHERE email = $1', [normalizedEmail]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: { code: 'CONFLICT', message: 'Email already registered' } });
    }

    const salt = await bcrypt.genSalt(12);
    const passwordHash = await bcrypt.hash(password, salt);

    const insertRes = await db.query(
      'INSERT INTO members (id, name, email, password_hash, role) VALUES ($1, $2, $3, $4, $5) RETURNING id, name, email, role',
      [assignedId, name.trim(), normalizedEmail, passwordHash, role]
    );

    const newMember = insertRes.rows[0];
    const tokens = generateTokens(newMember);

    await db.query(
      'INSERT INTO refresh_tokens (member_id, family_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [newMember.id, tokens.familyId, tokens.tokenHash, tokens.expiresAt]
    );

    res.status(201).json({
      user: newMember,
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
      expires_in: 900
    });
  } catch (err) {
    console.error('Register error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Registration failed' } });
  }
});

// POST /api/auth/login
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Email and password required' } });
  }

  const normalizedEmail = email.trim().toLowerCase();
  try {
    const { rows } = await db.query(
      'SELECT id, name, email, role, password_hash, failed_attempts, locked_until FROM members WHERE email = $1',
      [normalizedEmail]
    );

    if (rows.length === 0) {
      // Dummy compare to avoid timing side-channel
      await bcrypt.compare(password, '$2a$12$e80yq9Vw95c73rFpM1r71e1kZk0QzGk5g3V1Qf0QzGk5g3V1Qf0Qz');
      return res.status(401).json({ error: { code: 'UNAUTHENTICATED', message: 'Invalid email or password' } });
    }

    const member = rows[0];
    if (member.locked_until && new Date(member.locked_until) > new Date()) {
      const waitMinutes = Math.ceil((new Date(member.locked_until) - new Date()) / 60000);
      return res.status(429).json({ error: { code: 'RATE_LIMITED', message: `Account locked. Try again in ${waitMinutes} minutes` } });
    }

    const match = await bcrypt.compare(password, member.password_hash);
    if (!match) {
      const newAttempts = (member.failed_attempts || 0) + 1;
      let lockSql = 'UPDATE members SET failed_attempts = $1 WHERE id = $2';
      let lockParams = [newAttempts, member.id];

      if (newAttempts >= 5) {
        lockSql = 'UPDATE members SET failed_attempts = $1, locked_until = NOW() + INTERVAL \'15 minutes\' WHERE id = $2';
      }
      await db.query(lockSql, lockParams);
      return res.status(401).json({ error: { code: 'UNAUTHENTICATED', message: 'Invalid email or password' } });
    }

    // Reset failed counter
    await db.query('UPDATE members SET failed_attempts = 0, locked_until = NULL WHERE id = $1', [member.id]);

    const tokens = generateTokens(member);
    await db.query(
      'INSERT INTO refresh_tokens (member_id, family_id, token_hash, expires_at) VALUES ($1, $2, $3, $4)',
      [member.id, tokens.familyId, tokens.tokenHash, tokens.expiresAt]
    );

    res.json({
      user: { id: member.id, name: member.name, email: member.email, role: member.role },
      access_token: tokens.accessToken,
      refresh_token: tokens.refreshToken,
      expires_in: 900
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Login failed' } });
  }
});

// GET /api/auth/me
router.get('/me', requireAuth, (req, res) => {
  res.json({ user: req.user });
});

// POST /api/auth/password
router.post('/password', requireAuth, async (req, res) => {
  const { current_password, new_password } = req.body;
  if (!current_password || !new_password || new_password.length < 8) {
    return res.status(400).json({ error: { code: 'VALIDATION_ERROR', message: 'Valid new password of at least 8 characters required' } });
  }

  try {
    const { rows } = await db.query('SELECT password_hash FROM members WHERE id = $1', [req.user.id]);
    const match = await bcrypt.compare(current_password, rows[0].password_hash);
    if (!match) {
      return res.status(401).json({ error: { code: 'UNAUTHENTICATED', message: 'Current password incorrect' } });
    }

    const salt = await bcrypt.genSalt(12);
    const newHash = await bcrypt.hash(new_password, salt);
    await db.query('UPDATE members SET password_hash = $1 WHERE id = $2', [newHash, req.user.id]);
    // Revoke all existing sessions
    await db.query('UPDATE refresh_tokens SET revoked_at = NOW() WHERE member_id = $1', [req.user.id]);

    res.status(204).send();
  } catch (err) {
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to update password' } });
  }
});

module.exports = router;
