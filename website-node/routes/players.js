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
    const [rows] = await pool.query(
      `SELECT uuid, username, online, server, first_join, last_seen,
              playtime_minutes, kills, deaths
       FROM players
       ORDER BY last_seen DESC`
    );

    const players = rows.map(r => ({
      uuid: r.uuid,
      username: r.username,
      online: !!r.online,
      server: r.server,
      firstJoin: r.first_join,
      lastSeen: r.last_seen,
      playtimeMinutes: r.playtime_minutes,
      stats: { kills: r.kills, deaths: r.deaths }
    }));

    res.json(players);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Erreur serveur' });
  }
});

module.exports = router;
