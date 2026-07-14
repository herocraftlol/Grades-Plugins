const express = require('express');
const Stripe = require('stripe');
const { listGrades, getUuidFromUsername } = require('../grades');

const router = express.Router();
const stripe = Stripe(process.env.STRIPE_SECRET_KEY);

router.use(express.json());

/** GET /shop/grades - liste des grades achetables (price non nul) */
router.get('/grades', async (req, res) => {
  try {
    const grades = await listGrades();
    res.json(grades.filter(g => g.price !== null));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Erreur serveur' });
  }
});

/**
 * POST /shop/checkout
 * body: { username: "PseudoMinecraft", gradeId: "vip" }
 * Cree une session Stripe et renvoie l'URL de paiement a rediriger cote client.
 */
router.post('/checkout', async (req, res) => {
  const { username, gradeId } = req.body;
  if (!username || !gradeId) {
    return res.status(400).json({ error: 'username et gradeId sont requis' });
  }

  try {
    const uuid = await getUuidFromUsername(username);
    const grades = await listGrades();
    const grade = grades.find(g => g.id === gradeId);

    if (!grade || grade.price === null) {
      return res.status(404).json({ error: 'Grade introuvable ou non achetable' });
    }

    const session = await stripe.checkout.sessions.create({
      mode: 'payment',
      payment_method_types: ['card'],
      line_items: [
        {
          price_data: {
            currency: 'eur',
            product_data: { name: `Grade ${grade.display_name} - ${username}` },
            unit_amount: Math.round(grade.price * 100),
          },
          quantity: 1,
        },
      ],
      metadata: {
        minecraft_uuid: uuid,
        grade_id: grade.id,
        // duration_days: "30" // decommente si le grade est temporaire
      },
      success_url: `${process.env.SITE_URL || 'http://localhost:3000'}/shop/success`,
      cancel_url: `${process.env.SITE_URL || 'http://localhost:3000'}/shop/cancel`,
    });

    res.json({ url: session.url });
  } catch (err) {
    console.error(err);
    res.status(400).json({ error: err.message });
  }
});

module.exports = router;
