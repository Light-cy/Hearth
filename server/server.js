require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./modules/auth/auth.routes');
const shoppingRoutes = require('./modules/shopping/shopping.routes');
const expensesRoutes = require('./modules/expenses/expenses.routes');
const balancesRoutes = require('./modules/balances/balances.routes');
const settlementsRoutes = require('./modules/settlements/settlements.routes');
const budgetRoutes = require('./modules/budget/budget.routes');
const householdRoutes = require('./modules/household/household.routes');
const membersRoutes = require('./modules/members/members.routes');

const app = express();

// Security and utility middleware
app.use(helmet());
app.use(cors({ origin: true, credentials: true }));
app.use(express.json({ limit: '100kb' }));
if (process.env.NODE_ENV !== 'test') {
  app.use(morgan('dev'));
}

// Global rate limiting
const generalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  message: { error: { code: 'RATE_LIMIT_EXCEEDED', message: 'Too many requests, please try again later.' } }
});
app.use('/api/', generalLimiter);

// Health check endpoint (public, used by Android app profile screen)
app.get('/api/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    service: 'HEARTH Backend API',
    currency: 'PKR',
    version: '1.0.0'
  });
});

// Route registration
app.use('/api/auth', authRoutes);
app.use('/api/shopping', shoppingRoutes);
app.use('/api/expenses', expensesRoutes);
app.use('/api/balances', balancesRoutes);
app.use('/api/settle', settlementsRoutes);
app.use('/api/settlements', settlementsRoutes);
app.use('/api/budget', budgetRoutes);
app.use('/api/household', householdRoutes);
app.use('/api/members', membersRoutes);

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: {
      code: 'ROUTE_NOT_FOUND',
      message: `Endpoint ${req.method} ${req.originalUrl} not found`
    }
  });
});

// Centralized error handler
app.use((err, req, res, next) => {
  console.error('Unhandled Server Error:', err);
  const status = err.status || 500;
  const code = err.code || 'INTERNAL_ERROR';
  const message = err.message || 'An unexpected server error occurred';

  res.status(status).json({
    error: {
      code,
      message,
      ...(err.fields ? { fields: err.fields } : {})
    }
  });
});

const PORT = process.env.PORT || 4000;
if (process.env.NODE_ENV !== 'test') {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`HEARTH API server listening on http://0.0.0.0:${PORT}`);
  });
}

module.exports = app;
