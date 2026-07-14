# GradePlugin

Plugin Paper 1.21 (compatible multi-serveurs derrière BungeeCord) pour gérer des grades personnalisables, achetables sur ton site web ou obtenus via des competitions. Système de permissions autonome (pas besoin de LuckPerms).

## Description

GradePlugin est un plugin Minecraft permettant la gestion de grades personnalisés sur votre serveur. Les joueurs peuvent acheter des grades via votre site web ou les obtenir via des competitions.

### Caractéristiques principales

- **Base de données SQLite** : Pas besoin d'installer MySQL ! SQLite stocke les données dans un fichier local
- **Multi-serveurs** : Partagez les grades entre plusieurs serveurs Paper sur la même machine
- **Achat via site web** : Intégration possible avec votre site pour l'achat de grades
- **Competitions** : Attribution de grades suite à des competitions
- **Prefixes personnalisables** : Chaque grade dispose d'un préfixe visible dans le chat
- **Expiration automatique** : Les grades temporaires expirent automatiquement
- **Synchronisation** : Synchronisation des grades entre le serveur et la base de données
- **Mode WAL** : SQLite configuré pour une utilisation multi-serveurs

### Architecture

Le plugin utilise SQLite avec le mode WAL, permettant à plusieurs serveurs Paper de lire le fichier de base de données en même temps qu'un autre écrit dessus, sans se bloquer mutuellement.

```
[ Site web ]  --écrit-->  [ Base SQLite partagée ]  <--lit--  [ Paper #1 : survie   ]
                                                     <--lit--  [ Paper #2 : skyblock ]
                                                     <--lit--  [ Paper #3 : ...      ]
```

### Commandes

- `/grade` - Affiche vos grades ou la liste des grades disponibles
- `/gradeadmin` - Commandes d'administration des grades
  - `/gradeadmin give <joueur> <grade> [duree_jours]` - Attribuer un grade
  - `/gradeadmin remove <joueur> <grade>` - Retirer un grade
  - `/gradeadmin reload` - Recharger la configuration
  - `/gradeadmin create <id> <nom> <couleur> <priorite> <prefixe>` - Creer un grade
  - `/gradeadmin list` - Lister tous les grades

### Permissions

- `gradeplugin.admin` - Acces aux commandes d'administration (par defaut: op)

### Installation

1. Téléchargez la dernière version depuis la page des releases
2. Placez le fichier `GradePlugin.jar` dans le dossier `plugins` de votre serveur
3. Redémarrez le serveur
4. Le fichier de base de données `grades.db` sera créé automatiquement dans `plugins/GradePlugin/`

Pour partager les grades entre plusieurs serveurs Paper sur la même machine, configurez le même chemin de base de données dans `config.yml` sur chaque serveur.

### Configuration

Le fichier `config.yml` permet de configurer :
- Le chemin de la base de données SQLite
- Le format du chat
- L'activation du tab-list coloré
- Les intervalles de synchronisation
- Les grades par défaut

### Requirements

- Minecraft Paper 1.21+
- Java 21

### Version

**Version actuelle : 1.0.0**

### License

Plugin développé par Tututte
