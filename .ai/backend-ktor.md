# Règles Backend Ktor Server & Base de Données

Ce document décrit les règles architecturales et techniques régissant le module `:server` (Ktor 3, JetBrains Exposed, PostgreSQL).

---

## 1. Structure des Couches & Responsabilités

Le code backend est strictement découpé en 3 couches étanches :

```text
server/src/main/kotlin/com/resid/manager/
├── Application.kt             # Configuration Ktor, plugins, routing principal
├── auth/                      # Authentification JWT, Principal, hashing
├── data/                      # Tables Exposed, Enums de base, HttpError
├── routes/                    # Endpoints HTTP (Routing Ktor)
└── service/                   # Logique métier, transactions SQL, DTO mapping
```

### Règle d'or :
- **Les `routes` ne contiennent AUCUNE logique métier ni requête SQL directe.**
- **Les `services` contiennent toute la logique métier et encapsulent l'accès aux données.**

---

## 2. Couche Routes (`routes/`)

1. **Responsabilités des routes :**
   - Extraire et valider l'authentification (`call.principal<UserPrincipal>()` ou `call.getUserId()`).
   - Recevoir et valider le format du corps de requête (`call.receive<MyRequestDto>()`).
   - Déléguer immédiatement au Service métier.
   - Renvoyer un code HTTP sémantique :
     - `200 OK` : Lecture ou mise à jour réussie.
     - `201 Created` : Création de ressource réussie (ex. nouveau bail, nouvel utilisateur).
     - `204 No Content` : Suppression réussie sans corps de réponse.
2. **Gestion des exceptions dans les routes :**
   - Utiliser `StatusPages` ou capturer `HttpError` pour renvoyer le code et le message appropriés.
   - Ne jamais renvoyer une stack trace brute ou un `500 Internal Server Error` pour une erreur fonctionnelle (ex: fonds insuffisants, droits manquants, index invalide).

```kotlin
// Exemple canonique de Route Ktor
fun Route.leaseRoutes(leaseService: LeaseService) {
    route("/api/leases") {
        authenticate("auth-jwt") {
            post {
                val userId = call.getUserId()
                val request = call.receive<CreateLeaseRequest>()
                val newLease = leaseService.createLease(userId, request)
                call.respond(HttpStatusCode.Created, newLease)
            }
        }
    }
}
```

---

## 3. Couche Services & Transactions (`service/`)

1. **Transactions Exposed :**
   - Toutes les opérations de lecture et écriture en base de données doivent s'exécuter dans un bloc `dbQuery { ... }` (qui wrappe `newSuspendedTransaction(Dispatchers.IO)`).
2. **Levée d'erreurs métier (`HttpError`) :**
   - Utiliser la classe `com.resid.manager.data.HttpError` pour signaler une anomalie métier ou de validation :
     ```kotlin
     if (newIndex < previousIndex) {
         throw HttpError(HttpStatusCode.BadRequest, "Le nouvel index ($newIndex) ne peut pas être inférieur au précédent ($previousIndex)")
     }
     ```
3. **Sécurité et Permissions :**
   - Chaque service doit vérifier que l'utilisateur connecté (`userId`) dispose des droits nécessaires sur la résidence ou l'entité ciblée avant toute mutation.

---

## 4. Schémas de Base de Données & Migrations (`data/DatabaseSchema.kt`)

1. **Exposed Tables & Typage strict :**
   - Déclarer les colonnes avec leurs contraintes réelles (`varchar(length)`, `references`, `check`, `default`).
   - Utiliser les types KotlinX Datetime pour les dates et horodatages.
2. **RÈGLE CRITIQUE : ZÉRO PERTE DE DONNÉES EN PRODUCTION :**
   - **Interdiction absolue** d'utiliser `SchemaUtils.drop(...)` ou des recréations destructives de tables.
   - Toute modification de schéma (ajout de colonne, changement de type, nouvelle table) doit être accompagnée d'une stratégie de migration explicite :
     - Bloc d'exécution SQL d'altération (`exec("ALTER TABLE ...")` contrôlé).
     - Ou script de migration versionné.
   - Utiliser `SchemaUtils.createMissingTablesAndColumns(...)` avec précaution uniquement en environnement de développement local.

---

## 5. DTOs & Sérialisation

- Tous les objets échangés avec les clients doivent être des DTOs annotés avec `@Serializable` (KotlinX Serialization).
- Les entités Exposed (DAO / ResultRow) ne doivent **JAMAIS** être exposées directement aux clients HTTP. Toujours les mapper explicitement vers des DTOs partagés (`toDto()`).
