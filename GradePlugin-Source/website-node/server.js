require('dotenv').config();
const express = require('express');
const cors = require('cors');

const shopRoutes = require('./routes/shop');
const webhookRoutes = require('./routes/webhook');
const adminRoutes = require('./routes/admin');

const app = express();

// Autorise ton site (index.html) a appeler cette API depuis un autre
// port/domaine. Restreins ORIGIN dans .env en production
// (ex: ORIGIN=https://tonsite.com) plutot que de laisser "*".
app.use(cors({ origin: process.env.ORIGIN || '*' }));

// IMPORTANT : le webhook Stripe doit etre monte AVANT tout express.json()
// global, car il a besoin du body brut pour verifier la signature.
app.use('/webhook', webhookRoutes);

app.use('/shop', shopRoutes);
app.use('/admin', adminRoutes);

// Pages de retour apres paiement Stripe : on renvoie simplement le visiteur
// vers la section boutique de ton site, avec un indicateur dans l'URL que
// le front (index.html) affiche sous forme de toast.
app.get('/shop/success', (req, res) => {
  res.redirect(`${process.env.SITE_URL || 'http://localhost:8080'}/#boutique?achat=succes`);
});
app.get('/shop/cancel', (req, res) => {
  res.redirect(`${process.env.SITE_URL || 'http://localhost:8080'}/#boutique?achat=annule`);
});

const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`Serveur d'integration GradePlugin demarre sur le port ${port}`);
});
