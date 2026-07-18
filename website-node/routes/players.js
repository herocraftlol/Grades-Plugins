const express = require('express');
const { pool } = require('../db');

const router = express.Router();

/**
 * GET /players
 * Renvoie tous les joueurs déjà connectés sur le réseau (tous les serveurs
 * Paper derrière le BungeeCord confondus, puisqu'ils écrivent tous dans la
 * même table MySQL "players" via GradePlugin). Endpoint public : c'est le
 * site web qui l'appelle pour afficher skins/pseudos/stats.
 */
router.get('/', async (req, res) => {
  try {
    // Pour chaque joueur, on récupère son grade actif le plus prioritaire
    // (pas expiré). Un joueur peut avoir plusieurs grades en base (ex: VIP +
    // Légende) : ROW_NUMBER() ne garde que celui de plus haute priorité,
    // comme c'est déjà le cas pour l'affichage en jeu.
    const [rows] = await pool.query(
      `SELECT p.uuid, p.username, p.online, p.server, p.first_join, p.last_seen,
              p.playtime_minutes, p.kills, p.deaths,
              tg.grade_id, tg.display_name, tg.prefix, tg.color
       FROM players p
       LEFT JOIN (
         SELECT ranked.* FROM (
           SELECT pg.uuid, gr.id AS grade_id, gr.display_name, gr.prefix, gr.color,
                  ROW_NUMBER() OVER (PARTITION BY pg.uuid ORDER BY gr.priority DESC) AS rn
           FROM player_grades pg
           JOIN grades gr ON gr.id = pg.grade_id
           WHERE pg.active = 1 AND (pg.expires_at IS NULL OR pg.expires_at > NOW())
         ) ranked WHERE ranked.rn = 1
       ) tg ON tg.uuid = p.uuid
       ORDER BY p.last_seen DESC`
    );

    const players = rows.map(r => ({
      uuid: r.uuid,
      username: r.username,
      online: !!r.online,
      server: r.server,
      firstJoin: r.first_join,
      lastSeen: r.last_seen,
      playtimeMinutes: r.playtime_minutes,
      stats: { kills: r.kills, deaths: r.deaths },
      grade: r.grade_id ? { id: r.grade_id, displayName: r.display_name, prefix: r.prefix, color: r.color } : null
    }));

    res.json(players);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Erreur serveur' });
  }
});

module.exports = router;
