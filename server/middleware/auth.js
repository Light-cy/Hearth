// server/middleware/auth.js
const jwt = require('jsonwebtoken');
const db = require('../db');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_jwt_key_at_least_32_characters_long_12345';

async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      error: { code: 'UNAUTHENTICATED', message: 'Missing or malformed Authorization header' }
    });
  }

  const token = authHeader.split(' ')[1];
  try {
    const payload = jwt.verify(token, JWT_SECRET, { algorithms: ['HS256'] });
    const { rows } = await db.query(
      'SELECT id, name, email, role, locked_until FROM members WHERE id = $1',
      [payload.sub]
    );

    if (rows.length === 0) {
      return res.status(401).json({
        error: { code: 'UNAUTHENTICATED', message: 'Member not found or account removed' }
      });
    }

    const member = rows[0];
    if (member.locked_until && new Date(member.locked_until) > new Date()) {
      return res.status(401).json({
        error: { code: 'UNAUTHENTICATED', message: 'Account is temporarily locked' }
      });
    }

    req.user = member;
    next();
  } catch (err) {
    return res.status(401).json({
      error: { code: 'UNAUTHENTICATED', message: 'Invalid or expired access token' }
    });
  }
}

function requireAdmin(req, res, next) {
  if (!req.user || req.user.role !== 'admin') {
    return res.status(403).json({
      error: { code: 'FORBIDDEN', message: 'Admin privileges required for this action' }
    });
  }
  next();
}

// Idempotency Middleware (S2.6)
async function idempotency(req, res, next) {
  const key = req.headers['idempotency-key'];
  if (!key) return next();

  const endpoint = req.originalUrl || req.url;
  try {
    const { rows } = await db.query(
      'SELECT status, response FROM idempotency_keys WHERE key = $1 AND endpoint = $2',
      [key, endpoint]
    );

    if (rows.length > 0) {
      return res.status(rows[0].status).json(rows[0].response);
    }

    // Intercept res.json to store response
    const originalJson = res.json.bind(res);
    res.json = (body) => {
      if (res.statusCode >= 200 && res.statusCode < 300) {
        db.query(
          'INSERT INTO idempotency_keys (key, endpoint, member_id, status, response) VALUES ($1, $2, $3, $4, $5) ON CONFLICT DO NOTHING',
          [key, endpoint, req.user ? req.user.id : 'anon', res.statusCode, body]
        ).catch(err => console.error('Idempotency key save error:', err));
      }
      return originalJson(body);
    };
    next();
  } catch (err) {
    next();
  }
}

module.exports = {
  requireAuth,
  requireAdmin,
  idempotency,
  JWT_SECRET
};
