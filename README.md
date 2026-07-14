# GradePlugin

Plugin Paper 1.21 (multi-serveurs derrière BungeeCord) pour gérer des grades personnalisables, achetables sur votre site web ou obtenus via des competitions. Système de permissions autonome (pas besoin de LuckPerms).

## Description

GradePlugin est un plugin Minecraft permettant la gestion de grades personnalisés sur votre serveur. Les joueurs peuvent acheter des grades via votre site web ou les obtenir via des competitions.

### Caractéristiques principales

- **Base de données MySQL** : Une seule base partagée entre votre site web et tous vos serveurs Paper
- **Multi-serveurs** : Partagez les grades entre plusieurs serveurs Paper (survie, skyblock, etc.)
- **Achat via site web** : Intégration possible avec votre site pour l'achat de grades
- **Competitions** : Attribution de grades suite à des competitions
- **Prefixes personnalisables** : Chaque grade dispose d'un préfixe visible dans le chat
- **Expiration automatique** : Les grades temporaires expirent automatiquement
- **Synchronisation** : Le plugin lit `pending_sync` toutes les 10 secondes et applique les changements sans redémarrage
- **HikariCP** : Pool de connexions optimisé pour MySQL

### Architecture

Une seule base MySQL partagée entre votre site et tous vos serveurs Paper. Le plugin lit `pending_sync` toutes les 10 secondes et applique les changements en jeu.

```
[ Site web (PHP) ]  --écrit-->  [ MySQL partagé ]  <--lit--  [ Paper #1 : survie   ]
                                                    <--lit--  [ Paper #2 : skyblock ]
                                                    <--lit--  [ Paper #3 : ...      ]
```

### Commandes

- `/grade` - Affiche vos grades ou la liste des grades disponibles
- `/grade <joueur>` - Affiche les grades d'un joueur
- `/grade liste` - Liste tous les grades configurés et leur prix
- `/gradeadmin` - Commandes d'administration des grades
  - `/gradeadmin give <joueur> <grade> [duree_jours]` - Attribuer un grade (sans durée = permanent)
  - `/gradeadmin remove <joueur> <grade>` - Retirer un grade
  - `/gradeadmin reload` - Recharger la configuration
  - `/gradeadmin create <id> <nom> <couleur> <priorite> <prefixe> [permissions] [prix]` - Créer un grade
  - `/gradeadmin list` - Lister tous les grades

### Permissions

- `gradeplugin.admin` - Acces aux commandes d'administration (par defaut: op)

### Installation

1. Créez la base de données MySQL et exécutez `schema.sql` (optionnel - les tables sont créées automatiquement)
2. Placez le fichier `GradePlugin.jar` dans le dossier `plugins` de chaque serveur Paper
3. Configurez les identifiants MySQL dans `plugins/GradePlugin/config.yml`
4. Redémarrez les serveurs

### Intégration avec le site web

- **`website-node/`** - Intégration Node.js/Express prête à l'emploi : boutique avec paiement Stripe + panel admin pour les competitions

Le principe : une fonction `grantGrade()` insere le grade dans la base MySQL et previent le plugin via `pending_sync`, qui l'applique en jeu en quelques secondes sans redemarrage.

### Configuration

Le fichier `config.yml` permet de configurer :
- Les identifiants de connexion MySQL (host, port, database, user, password)
- La taille du pool de connexions
- Le format du chat
- L'activation du tab-list coloré
- Les intervalles de synchronisation
- Les grades par défaut

### Requirements

- Minecraft Paper 1.21+
- Java 21
- MySQL 8.0+

### Version

**Version actuelle : 1.0.0**

### License

Plugin développé par Tututte
