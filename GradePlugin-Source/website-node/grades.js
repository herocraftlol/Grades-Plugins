const fetch = require('node-fetch');
const { pool } = require('./db');

/**
 * Recupere l'UUID (avec tirets) d'un joueur a partir de son pseudo.
 * Le joueur doit s'etre connecte au moins une fois sur un serveur Mojang
 * pour avoir un compte valide.
 */
async function getUuidFromUsername(username) {
  const res = await fetch(`https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(username)}`);
  if (!res.ok) {
    throw new Error(`Joueur introuvable: ${username}`);
  }
  const data = await res.json();
  // L'API Mojang renvoie l'UUID sans tirets, il faut le reformater
  const raw = data.id;
  return `${raw.slice(0, 8)}-${raw.slice(8, 12)}-${raw.slice(12, 16)}-${raw.slice(16, 20)}-${raw.slice(20)}`;
}

/**
 * Attribue un grade a un joueur. Insere/actualise player_grades puis
 * previent le plugin via pending_sync pour une prise en compte immediate
 * en jeu (sans redemarrage serveur).
 *
 * @param {string} uuid            UUID du joueur (avec tirets)
 * @param {string} gradeId         Identifiant du grade (ex: "vip")
 * @param {"purchase"|"competition"|"admin"} source
 * @param {number|null} durationDays  null = permanent
 */
async function grantGrade(uuid, gradeId, source, durationDays = null) {
  let expiresAt = null;
  if (durationDays !== null) {
    const d = new Date();
    d.setDate(d.getDate() + Number(durationDays));
    expiresAt = d.toISOString().slice(0, 19).replace('T', ' ');
  }

  // Vraie transaction MySQL : les deux ecritures (grade + notification au
  // plugin) doivent reussir ensemble, sinon on annule tout.
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();

    await conn.execute(
      `INSERT INTO player_grades (uuid, grade_id, source, expires_at, active)
       VALUES (?, ?, ?, ?, 1)
       ON DUPLICATE KEY UPDATE
         source = VALUES(source),
         expires_at = VALUES(expires_at),
         active = 1,
         granted_at = CURRENT_TIMESTAMP`,
      [uuid, gradeId, source, expiresAt]
    );

    await conn.execute(`INSERT INTO pending_sync (uuid) VALUES (?)`, [uuid]);

    await conn.commit();
  } catch (err) {
    await conn.rollback();
    throw err;
  } finally {
    conn.release();
  }
}

/** Liste les grades disponibles (pour affichage boutique). */
async function listGrades() {
  const [rows] = await pool.query(
    `SELECT id, display_name, prefix, color, priority, price FROM grades ORDER BY priority DESC`
  );
  return rows;
}

module.exports = { getUuidFromUsername, grantGrade, listGrades };
