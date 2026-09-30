# Règles Métier & Logique Domaine (ResidManager)

Ce document réunit les règles métier fondamentales et invariants du domaine immobilier de ResidManager. Ces règles s'appliquent aussi bien aux validations côté client (UI/UseCases) qu'aux vérifications côté serveur (`:server/service`).

---

## 1. Monnaie & Calculs Financiers

1. **Devise Pivot :**
   - La devise par défaut pour l'ensemble des résidences et transactions est le **Franc CFA (FCFA / XOF)**.
   - Les indicateurs globaux et tableaux de bord financiers sont tous agrégés en FCFA.
2. **Précision Financière :**
   - Interdiction d'utiliser des types flottants non contrôlés pour les montants critiques. Utiliser `Double` avec arrondi contrôlé ou des représentations en centimes / `BigDecimal` pour éviter les erreurs d'arrondi sur les totaux financiers.

---

## 2. Relevés d'Électricité & Compteurs

1. **Formule de Calcul :**
   $$\text{Montant Dû} = (\text{Nouvel Index} - \text{Ancien Index}) \times \text{Prix unitaire du kWh}$$
2. **Contraintes et Invariants :**
   - Le **Nouvel Index** doit obligatoirement être supérieur ou égal à l'**Ancien Index** :
     $$\text{Nouvel Index} \ge \text{Ancien Index}$$
   - L'ancien index est **strictement en lecture seule** pour l'utilisateur qui saisit le relevé (il correspond au dernier index validé du compteur de l'unité).
   - Le prix unitaire du kWh est configuré au niveau de la résidence ou de l'unité.

---

## 3. Machine à États des Baux (Leases)

Chaque contrat de bail suit un cycle de vie strict défini par les états suivants :

```text
[ PENDING_PAYMENT ] ── (Paiement partiel / avance) ──► [ DOWN_PAYMENT_PAID ]
         │                                                      │
         └───────────── (Paiement total reçu) ──────────────────┘
                                 │
                                 ▼
                       [ PENDING_SIGNATURE ]
                                 │
                   (Signature des deux parties)
                                 │
                                 ▼
                        [ SIGNED_ACTIVE ]
                                 │
                   (Résiliation / Fin de contrat)
                                 │
                                 ▼
                            [ TERMINATED ]
```

- **`PENDING_PAYMENT`** : Bail généré, en attente du versement de la caution et/ou du premier loyer.
- **`DOWN_PAYMENT_PAID`** : Paiement partiel validé.
- **`PENDING_SIGNATURE`** : Totalité des frais initiaux réglée, en attente de signature numérique ou manuscrite.
- **`SIGNED_ACTIVE`** : Bail en vigueur, l'occupant a accès aux services et les loyers récurrents s'appliquent.
- **`TERMINATED`** : Bail clôturé suite à un état des lieux de sortie.

---

## 4. Tickets d'Incidents & Maintenance

Cycle de vie d'un ticket :
$$\text{OPEN} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{CLOSED}$$

1. **Statut `OPEN`** : Créé par un locataire ou un gestionnaire.
2. **Statut `IN_PROGRESS`** : Assigné à un prestataire ou en cours d'intervention.
3. **Statut `CLOSED` (Clôture) :**
   - La clôture d'un ticket exige obligatoirement le renseignement d'un coût d'intervention (`intervention_cost` $\ge 0$).
   - **Règle financière automatique :** Dès la clôture, une transaction de dépense opérationnelle (`EXPENSE`) est automatiquement créée dans le grand livre de la résidence correspondante pour ce montant.

---

## 5. Facturation Automatique des Loyers

- Chaque **1er du mois à 00:00**, le moteur d'automatisation génère une transaction financière de type `RENT` au statut `UNPAID` pour chaque bail actif (`SIGNED_ACTIVE`).
- Lorsqu'un locataire effectue un paiement, la transaction passe à `PAID` (ou `PARTIALLY_PAID`).
