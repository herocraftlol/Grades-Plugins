# Integration Node.js pour GradePlugin

Module autonome (Express) a brancher sur ton site. Si tu utilises un
framework different (Next.js, Fastify, NestJS...), reprends la logique de
`grades.js` (aucune dependance a Express) et adapte juste les routes.

## Installation

```bash
cd website-node
npm install
cp .env.example .env
# Renseigne DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD : la MEME base
# MySQL que celle configuree dans le config.yml de chaque serveur Paper
# GradePlugin, et ta cle Stripe si tu utilises Stripe.
npm start
```

MySQL etant un vrai serveur reseau (contrairement a un fichier SQLite),
ce micro-service et tes serveurs Paper n'ont pas besoin d'etre sur la
meme machine : il suffit qu'ils puissent tous joindre le meme hote MySQL
avec les memes identifiants.

## Fichiers

- **db.js** — pool de connexions MySQL (mysql2), partage la meme base que
  le plugin.
- **grades.js** — logique metier reutilisable, independante d'Express :
  - `getUuidFromUsername(pseudo)` : resout un pseudo Minecraft en UUID via
    l'API Mojang.
  - `grantGrade(uuid, gradeId, source, durationDays)` : attribue un grade
    et notifie le plugin (table `pending_sync`) pour une prise en compte
    en jeu en quelques secondes.
  - `listGrades()` : liste les grades configures (pour affichage boutique).
- **routes/shop.js** — `/shop/grades` (liste) et `/shop/checkout` (cree une
  session de paiement Stripe pour un grade achetable).
- **routes/webhook.js** — `/webhook/stripe` : recoit la confirmation de
  paiement de Stripe et attribue le grade automatiquement.
- **routes/admin.js** — `/admin/grant` : attribution manuelle protegee par
  cle API (`x-admin-key`), a utiliser depuis ta page d'administration des
  competitions.
- **routes/players.js** — `/players` (public) : liste tous les joueurs
  connus (skin/pseudo/stats), lus depuis la table `players` que le plugin
  (`PlayerTrackingListener`) alimente sur chaque serveur du reseau. Consomme
  directement par la section "Joueurs" du site (`SHOP_API_BASE + '/players'`,
  donc `https://tonsite.com/boutique/players` une fois derriere le
  reverse-proxy — voir `deploy/nginx-example.conf`, aucune regle
  supplementaire necessaire).

## Paiement : Stripe par defaut

L'exemple utilise Stripe (le plus simple a integrer en Node). Si tu
utilises un autre prestataire (PayPal, Mollie, PaddleTonSite...), seule la
partie `routes/webhook.js` et `routes/shop.js` change : la fonction
`grantGrade()` de `grades.js` reste identique, c'est le coeur reutilisable.

Pour tester en local, utilise le CLI Stripe :
```bash
stripe listen --forward-to localhost:3000/webhook/stripe
```

## Panel competitions (attribution manuelle)

Depuis ta page d'admin, appelle simplement :

```js
await fetch('https://tonsite.com/admin/grant', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-admin-key': 'TA_CLE_ADMIN',
  },
  body: JSON.stringify({
    username: 'PseudoDuGagnant',
    gradeId: 'legend',
    source: 'competition',
    durationDays: 30, // omets ce champ pour un grade permanent
  }),
});
```

## Integration dans un framework existant (ex : Next.js)

Si ton site tourne deja en Next.js/autre, tu n'as pas besoin du serveur
Express (`server.js`). Copie juste `db.js` et `grades.js`, puis appelle
`grantGrade(...)` directement depuis tes propres API routes / server
actions (ex : `app/api/webhook/stripe/route.js`, ou une server action
appelee par ton bouton "Attribuer" dans le panel admin).
