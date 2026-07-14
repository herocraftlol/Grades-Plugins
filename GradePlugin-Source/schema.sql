-- ============================================================
-- Schéma de référence de la base "grades_db"
-- Ces tables sont créées AUTOMATIQUEMENT par le plugin (au premier
-- démarrage) et par le micro-service Node (au premier lancement) si
-- elles n'existent pas déjà — tu n'es donc pas obligé d'exécuter ce
-- fichier à la main. Il est fourni pour référence / création manuelle
-- si tu préfères tout créer toi-même via phpMyAdmin.
-- ============================================================

CREATE DATABASE IF NOT EXISTS grades_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE grades_db;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS player_grades (
    uuid VARCHAR(36) NOT NULL,
    grade_id VARCHAR(64) NOT NULL,
    source VARCHAR(32) DEFAULT 'admin',
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL DEFAULT NULL,
    active TINYINT(1) DEFAULT 1,
    PRIMARY KEY (uuid, grade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pending_sync (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    processed TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Compte dédié pour le plugin et le micro-service (à adapter : mot de
-- passe, hôte autorisé). Remplace CHANGE_ME par un vrai mot de passe.
-- CREATE USER IF NOT EXISTS 'grades_user'@'%' IDENTIFIED BY 'CHANGE_ME';
-- GRANT ALL PRIVILEGES ON grades_db.* TO 'grades_user'@'%';
-- FLUSH PRIVILEGES;
