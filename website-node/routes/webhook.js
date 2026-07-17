const express = require('express');
const Stripe = require('stripe');
const { grantGrade } = require('../grades');

const router = express.Router();
const stripe = Stripe(process.env.STRIPE_SECRET_KEY);

/**
 * IMPORTANT : cette route doit recevoir le BODY BRUT (pas du JSON parse),
 * c'est pourquoi on utilise express.raw() ici et pas express.json() global.
 *
 * Cote Stripe Dashboard / Stripe CLI, configure le webhook pour ecouter
 * l'evenement "checkout.session.completed" et pointe-le vers :
 *   https://tonsite.com/webhook/stripe
 *
 * Quand tu crees la session de paiement (checkout), resous d'abord le pseudo
 * en UUID (voir routes/shop.js) et ajoute dans "metadata" :
 *   { minecraft_uuid: "...", grade_id: "vip", duration_days: "30" }
 * (duration_days optionnel : omets-le pour un grade permanent)
 */
router.post('/stripe', express.raw({ type: 'application/json' }), async (req, res) => {
  let event;

  try {
    const signature = req.headers['stripe-signature'];
    event = stripe.webhooks.constructEvent(req.body, signature, process.env.STRIPE_WEBHOOK_SECRET);
  } catch (err) {
    console.error('Signature Stripe invalide:', err.message);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }

  if (event.type === 'checkout.session.completed') {
    const session = event.data.object;
    const { minecraft_uuid, grade_id, duration_days } = session.metadata || {};

    if (!minecraft_uuid || !grade_id) {
      console.error('Metadata manquante sur la session Stripe:', session.id);
      return res.status(200).send('OK (metadata manquante, ignore)');
    }

    try {
      await grantGrade(
        minecraft_uuid,
        grade_id,
        'purchase',
        duration_days ? Number(duration_days) : null
      );
      console.log(`Grade ${grade_id} attribue a ${minecraft_uuid} suite a un achat.`);
    } catch (err) {
      console.error('Erreur attribution du grade:', err);
      return res.status(500).send('Erreur interne');
    }
  }

  res.status(200).send('OK');
});

module.exports = router;
