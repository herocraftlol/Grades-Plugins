const express = require('express');
const { grantGrade, getUuidFromUsername } = require('../grades');

const router = express.Router();
router.use(express.json());

/** Protection simple par cle API. Adapte selon ton systeme d'auth existant
 *  (session admin, JWT, etc.) si tu en as deja un sur ton site. */
function requireAdminKey(req, res, next) {
  const key = req.headers['x-admin-key'];
  if (!key || key !== process.env.ADMIN_API_KEY) {
    return res.status(403).json({ error: 'Non autorise' });
  }
  next();
}

/**
 * POST /admin/grant
 * body: { username: "PseudoMinecraft", gradeId: "legend", source: "competition", durationDays: 30 }
 * durationDays omis ou null => grade permanent
 */
router.post('/grant', requireAdminKey, async (req, res) => {
  const { username, gradeId, source, durationDays } = req.body;

  if (!username || !gradeId) {
    return res.status(400).json({ error: 'username et gradeId sont requis' });
  }

  try {
    const uuid = await getUuidFromUsername(username);
    await grantGrade(uuid, gradeId, source || 'competition', durationDays || null);
    res.json({ success: true, uuid, gradeId });
  } catch (err) {
    console.error(err);
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
