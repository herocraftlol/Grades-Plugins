const path = require('path');
const Database = require('better-sqlite3');
require('dotenv').config();

// Chemin du fichier SQLite. DOIT etre le MEME fichier que celui utilise
// par le plugin GradePlugin sur tes serveurs Paper (voir "sqlite.path"
// dans plugins/GradePlugin/config.yml sur chaque serveur), et donc
// forcement sur la MEME machine (ou un volume/partage reseau commun).
const dbPath = process.env.DB_PATH || path.join(__dirname, 'grades.db');

const db = new Database(dbPath);
// Permet les lectures pendant qu'un autre processus (le plugin Paper) ecrit,
// et inversement — indispensable puisque plusieurs programmes partagent
// ce meme fichier en meme temps.
db.pragma('journal_mode = WAL');
db.pragma('busy_timeout = 5000');

// Cree les tables si ce service demarre avant qu'aucun serveur Paper
// n'ait encore ete lance (sinon elles existent deja).
db.exec(`
  CREATE TABLE IF NOT EXISTS grades (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    prefix TEXT DEFAULT '',
    suffix TEXT DEFAULT '',
    color TEXT DEFAULT '&f',
    priority INTEGER DEFAULT 0,
    permissions TEXT,
    price REAL DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS player_grades (
    uuid TEXT NOT NULL,
    grade_id TEXT NOT NULL,
    source TEXT DEFAULT 'admin',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    active INTEGER DEFAULT 1,
    PRIMARY KEY (uuid, grade_id)
  );
  CREATE TABLE IF NOT EXISTS pending_sync (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT NOT NULL,
    processed INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  );
`);

module.exports = db;
