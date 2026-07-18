# GradePlugin

Plugin Paper 1.21 (multi-serveurs derriere BungeeCord) pour gerer des grades
personnalisables, achetables sur ton site web ou obtenables via des
competitions. Systeme de permissions autonome (pas besoin de LuckPerms).

## Architecture

- **Une seule base MySQL partagee** entre ton site et tous tes serveurs Paper.
- Le plugin **GradePlugin est installe sur chaque serveur** (survie, skyblock...).
  Comme ils lisent tous la meme base, un grade attribue est actif partout.
- Ton site web ecrit directement dans la base (table `player_grades` +
  `pending_sync`). Le plugin lit `pending_sync` toutes les
  `sync-interval-seconds` (10s par defaut) et applique le changement en jeu,
  sans avoir besoin de redemarrer le serveur.

```
[ Site web (PHP) ]  --écrit-->  [ MySQL partagé ]  <--lit--  [ Paper #1 : survie   ]
                                                    <--lit--  [ Paper #2 : skyblock ]
                                                    <--lit--  [ Paper #3 : ...      ]
```

## Compilation

Ce projet n'a pas pu etre compile dans cet environnement (acces reseau
restreint aux depots Maven/PaperMC). Pour le compiler chez toi :

```bash
mvn clean package
```

Le jar final sera dans `target/GradePlugin.jar`. Necessite Java 21+ et Maven.
Copie ce jar dans le dossier `plugins/` de **chaque** serveur Paper.

## Installation

1. Cree la base de donnees MySQL (voir `schema.sql` pour la structure de
   reference — les tables sont de toute facon creees automatiquement au
   premier demarrage du plugin ou du micro-service si elles n'existent pas).
2. Copie `GradePlugin.jar` dans `plugins/` sur chaque serveur Paper, demarre
   une fois pour generer `config.yml`.
3. Renseigne les identifiants MySQL dans `plugins/GradePlugin/config.yml`
   (memes identifiants sur chaque serveur, meme base).
4. Redemarre les serveurs. Les grades par defaut (`vip`, `legend`) sont crees
   automatiquement en base au premier lancement — modifie-les ou ajoutes-en
   via `/gradeadmin create` ou directement en SQL / depuis ton site.

## Integration avec le site web

**`website-node/`** — integration Node.js/Express prete a l'emploi :
boutique avec paiement Stripe + panel admin pour les competitions. Voir
`website-node/README.md` pour l'installation complete.

Le principe : une fonction `grantGrade()` insere le grade dans
`player_grades` et previent le plugin via `pending_sync`, qui l'applique en
jeu en quelques secondes sans redemarrage. L'UUID d'un joueur est recupere
via l'API Mojang avant l'insertion :
`https://api.mojang.com/users/profiles/minecraft/{pseudo}`.

## Section "Joueurs" du site (skins, pseudos, stats)

En plus des grades, le plugin alimente maintenant une table `players` (voir
`schema.sql`) avec, pour chaque joueur ayant déjà rejoint : pseudo, statut
en ligne, serveur actuel, date de première/dernière connexion, temps de jeu
et kills/morts. C'est `PlayerTrackingListener` qui écrit dans cette table à
chaque connexion/déconnexion/mort — directement en SQL, sans appel HTTP.

Comme tous les serveurs Paper du réseau BungeeCord écrivent dans la même
base, active `track-players: true` (par défaut) sur **chacun** d'eux, avec
un `server-name` différent à chaque fois. Le site interroge ensuite un seul
endpoint (`GET /boutique/players`, voir `website-node/routes/players.js`)
qui couvre déjà tout le réseau.

Rien à installer en plus : c'est inclus dans le même `GradePlugin.jar`.

## Commandes en jeu

- `/grade` — affiche tes grades actifs.
- `/grade <joueur>` — affiche les grades d'un joueur.
- `/grade liste` — liste tous les grades configures et leur prix.
- `/gradeadmin give <joueur> <grade> [jours]` — attribue un grade (sans
  duree = permanent).
- `/gradeadmin remove <joueur> <grade>` — retire un grade.
- `/gradeadmin create <id> <nom> <&couleur> <priorite> <prefixe> [perms,separees,virgule] [prix]`
- `/gradeadmin reload` — recharge la config et les grades.
- `/gradeadmin list` — liste les grades configures.

## Personnalisation

Tout se configure dans `config.yml` : format du chat, activation du
tab-list colore, intervalles de synchronisation, grades par defaut. La
priorite determine quel grade s'affiche/s'applique quand un joueur en a
plusieurs (le plus eleve gagne).
