const mysql = require('mysql2/promise');
require('dotenv').config();

// Meme base MySQL que celle configuree dans plugins/GradePlugin/config.yml
// sur CHACUN de tes serveurs Paper (voir le bloc "mysql:"). Comme MySQL est
// un vrai serveur reseau, ce micro-service peut tourner sur une machine
// differente des serveurs Minecraft tant qu'il peut joindre l'hote MySQL.
const pool = mysql.createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  port: process.env.DB_PORT ? Number(process.env.DB_PORT) : 3306,
  user: process.env.DB_USER || 'grades_user',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'grades_db',
  waitForConnections: true,
  connectionLimit: 10,
  charset: 'utf8mb4_general_ci',
});

// Cree les tables si ce service demarre avant qu'aucun serveur Paper n'ait
// encore ete lance (sinon elles existent deja - IF NOT EXISTS ne casse rien).
async function ensureTables() {
  await pool.query(`
    CREATE TABLE IF NOT EXISTS grades (
      id VARCHAR(64) PRIMARY KEY,
      display_name VARCHAR(64) NOT NULL,
      prefix VARCHAR(64) DEFAULT '',
      suffix VARCHAR(64) DEFAULT '',
      color VARCHAR(8) DEFAULT '&f',
      priority INT DEFAULT 0,
      permissions TEXT,
      price DECIMAL(10,2) DEFAULT NULL,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS player_grades (
      uuid VARCHAR(36) NOT NULL,
      grade_id VARCHAR(64) NOT NULL,
      source VARCHAR(32) DEFAULT 'admin',
      granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      expires_at TIMESTAMP NULL DEFAULT NULL,
      active TINYINT(1) DEFAULT 1,
      PRIMARY KEY (uuid, grade_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  await pool.query(`
    CREATE TABLE IF NOT EXISTS pending_sync (
      id INT AUTO_INCREMENT PRIMARY KEY,
      uuid VARCHAR(36) NOT NULL,
      processed TINYINT(1) DEFAULT 0,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
}

module.exports = { pool, ensureTables };
