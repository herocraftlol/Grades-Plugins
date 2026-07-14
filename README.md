# GradePlugin

Plugin Minecraft Paper 1.21 - Système de grades personnalisables

## Description

GradePlugin est un plugin Minecraft permettant la gestion de grades personnalisés sur votre serveur. Les joueurs peuvent acheter des grades via votre site web ou les obtenir via des competitions.

### Fonctionnalités

- **Gestion des grades** : Création, suppression et modification de grades
- **Achat via site web** : Integration possible avec votre site pour l'achat de grades
- **Competitions** : Attribution de grades suite à des competitions
- **Prefixes personnalisables** : Chaque grade dispose d'un prefixe visible dans le chat
- **Expiration automatique** : Les grades temporaires expirent automatiquement
- **Synchronisation** : Synchronisation des grades entre le serveur et la base de données
- **Base de données MySQL** : Stockage persistant de tous les grades

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

### Configuration

Le plugin nécessite une base de données MySQL. Configurez les informations de connexion dans `config.yml`.

### Installation

1. Telechargez la dernière version depuis la page des releases
2. Placez le fichier `GradePlugin.jar` dans le dossier `plugins` de votre serveur
3. Redemarrez le serveur
4. Configurez la connexion MySQL dans `plugins/GradePlugin/config.yml`

### Version

**Version actuelle : 1.0.0**

### Requirements

- Minecraft Paper 1.21+
- Java 21
- MySQL 8.0+

### License

Plugin developpé par Tututte
