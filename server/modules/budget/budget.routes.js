// server/modules/budget/budget.routes.js
const express = require('express');
const router = express.Router();
const db = require('../../db');
const { requireAuth } = require('../../middleware/auth');
const { fromPaisaNumber } = require('../../engine/money');

// GET /api/budget?month=YYYY-MM
router.get('/', requireAuth, async (req, res) => {
  try {
    const now = new Date();
    const currentMonth = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, '0')}`;
    const targetMonth = req.query.month || currentMonth;

    const budgetSetting = await db.query("SELECT value FROM settings WHERE key = 'monthly_budget_paisa'");
    const budgetPaisa = budgetSetting.rows.length > 0 ? parseInt(budgetSetting.rows[0].value, 10) : 500000;

    const expensesRes = await db.query(
      "SELECT COALESCE(SUM(amount_paisa), 0) as total_spent FROM expenses WHERE to_char(expense_date, 'YYYY-MM') = $1",
      [targetMonth]
    );

    const spentPaisa = parseInt(expensesRes.rows[0].total_spent, 10);
    const remainingPaisa = budgetPaisa - spentPaisa;
    const percentUsed = budgetPaisa > 0 ? (spentPaisa / budgetPaisa) * 100 : 0;

    let status = 'HEALTHY';
    if (spentPaisa > budgetPaisa) {
      status = 'EXCEEDED';
    } else if (percentUsed >= 80) {
      status = 'NEAR_LIMIT';
    }

    res.json({
      month: targetMonth,
      monthly_budget: fromPaisaNumber(budgetPaisa),
      total_spent: fromPaisaNumber(spentPaisa),
      remaining: fromPaisaNumber(remainingPaisa),
      percent_used: Math.round(percentUsed * 100) / 100,
      status
    });
  } catch (err) {
    console.error('Fetch budget error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch budget' } });
  }
});

// GET /api/budget/categories?month=YYYY-MM (Category Budget Breakdown & Warnings)
router.get('/categories', requireAuth, async (req, res) => {
  try {
    const now = new Date();
    const currentMonth = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, '0')}`;
    const targetMonth = req.query.month || currentMonth;

    const budgetSetting = await db.query("SELECT value FROM settings WHERE key = 'monthly_budget_paisa'");
    const budgetPaisa = budgetSetting.rows.length > 0 ? parseInt(budgetSetting.rows[0].value, 10) : 500000;

    const result = await db.query(
      `SELECT category, COALESCE(SUM(amount_paisa), 0) as cat_spent
       FROM expenses
       WHERE to_char(expense_date, 'YYYY-MM') = $1
       GROUP BY category
       ORDER BY cat_spent DESC`,
      [targetMonth]
    );

    const categories = result.rows.map(r => {
      const catSpent = parseInt(r.cat_spent, 10);
      const catPercent = budgetPaisa > 0 ? (catSpent / budgetPaisa) * 100 : 0;
      return {
        category: r.category,
        spent: fromPaisaNumber(catSpent),
        percent_of_budget: Math.round(catPercent * 100) / 100,
        exceeds_threshold: catPercent > 40.0
      };
    });

    res.json({
      month: targetMonth,
      monthly_budget: fromPaisaNumber(budgetPaisa),
      threshold_percent: 40,
      categories
    });
  } catch (err) {
    console.error('Fetch category budget warnings error:', err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Failed to fetch category budget status' } });
  }
});

module.exports = router;
