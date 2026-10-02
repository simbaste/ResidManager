# ResidManager

**ResidManager** est une solution complète de gestion immobilière multi-résidences conçue avec **Kotlin Multiplatform (KMP)** et **Compose Multiplatform**, ciblant Android, iOS, Web (Wasm/JS) et un backend **Ktor**.

L'application client suit une architecture **Clean Architecture Feature-First** combinée au patron réactif **MVI (Model-View-Intent)**, injectée via **Koin**, et respecte une politique de décomposition modulaire (fichiers Composables de moins de 250 lignes).

---

## 🏛️ Architecture Globale

```text
ResidManager/
├── core/                                # Modèles partagés, DTOs et contrats communs
├── app/
│   ├── shared/                          # Logique métier partagée & UI Compose Multiplatform
│   │   └── src/commonMain/kotlin/com/resid/manager/
│   │       ├── base/                    # MviViewModel & interfaces MVI génériques
│   │       ├── di/                      # Configuration de l'injection Koin (DiConfig.kt)
│   │       ├── network/                 # Client Ktor mutualisé & gestion des tokens JWT
│   │       ├── features/                # Modules applicatifs Feature-First
│   │       │   ├── auth/                # Authentification & Inscription
│   │       │   ├── dashboard/           # Tableau de bord KPIs & statistiques financières
│   │       │   ├── residences/          # Gestion CRUD des résidences
│   │       │   ├── units/               # Logements & unités locatives
│   │       │   ├── leases/              # Contrats de bail, paiements & wizard 4 étapes
│   │       │   ├── members/             # Membres, rôles & habilitations
│   │       │   ├── electricity/         # Relevés de compteurs & facturation électricité
│   │       │   ├── finances/            # Grand livre comptable & saisie de dépenses
│   │       │   ├── tickets/             # Maintenance, incidents & suivi des coûts
│   │       │   └── profile/             # Fiche utilisateur & paramétrage de résidence
│   │       └── ui/                      # AppShell et système de design global
│   ├── androidApp/                      # Point d'entrée spécifique Android
│   ├── iosApp/                          # Point d'entrée SwiftUI pour iOS
│   └── webApp/                          # Point d'entrée Web (Wasm & JS)
└── server/                              # Backend Ktor (PostgreSQL, Exposed, JWT, REST API)
```

---

## 🧩 Structure d'une Feature (Feature-First + MVI)

Chaque fonctionnalité est isolée dans un package dédié au sein de `app/shared/.../features/<feature>/` :

```text
features/<feature>/
├── data/                                # Accès aux données & endpoints REST
│   └── <Feature>Repository.kt           # Interface & Implémentation isolée
├── usecase/                             # Cas d'usage métier (Single Responsibility Principle)
│   ├── Fetch<Feature>UseCase.kt
│   └── Create<Feature>UseCase.kt
├── mvi/                                 # Contrat strict de l'écran
│   └── <Feature>Contract.kt             # UiState (état immuable), Intent (actions) & Effect (effets de bord)
├── <Feature>ViewModel.kt                # Hérite de MviViewModel<State, Intent, Effect>
├── di/                                  # Module d'injection de dépendances Koin
│   └── <Feature>Module.kt               # Déclaration du Repository, UseCases et ViewModel
└── ui/                                  # Couche de présentation Compose
    ├── <Feature>Screen.kt               # Écran principal Stateful / Stateless (< 250 lignes)
    └── components/                      # Composants spécialisés réutilisables (< 250 lignes)
        ├── <Feature>Header.kt
        ├── <Feature>Table.kt
        └── <Feature>Dialog.kt
```

---

## 🎨 Charte Graphique & UI (Emerald Estate)

L'interface est construite avec **Compose Material 3** :
- **Couleur Primaire** : Vert Émeraude (`#006948` / `#059669` / `#34D399`)
- **Fonds & Surfaces** : Sombre raffiné (`#031427` / `#0F172A`), cartes claires contrastées
- **Typographie** : Famille de polices Inter avec hiérarchie Material 3
- **Ergonomie** : Respect d'un rythme de grille de 8dp, réactivité multi-écrans (Mobile, Tablette, Desktop).

---

## 🐳 Bases de Données & Environnements Docker

Le projet propose un fichier `docker-compose.yml` préconfiguré avec deux instances PostgreSQL isolées (`postgres:17-alpine`) pour séparer les données selon la branche de travail.

| Service | Container Name | Port Externe | Base de données | Utilisateur / Mot de passe | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`postgres-dev`** | `residmanager_db_dev` | `5433` | `residmanager_db` | `postgres` / `postgres` | Développement, nouvelles migrations Flyway & tests |
| **`postgres-main`** | `residmanager_db_main` | `5434` | `residmanager_db` | `postgres` / `postgres` | Reproduction fidèle de l'environnement de production |

### Démarrage des conteneurs

```bash
# Démarrer les deux conteneurs en arrière-plan
docker compose up -d

# Démarrer uniquement la base de données de développement
docker compose up -d postgres-dev

# Démarrer uniquement la base de reproduction main
docker compose up -d postgres-main
```

### Arrêt et gestion

```bash
# Arrêter les conteneurs (conserve les volumes et les données)
docker compose down

# Arrêter et purger les données pour repartir de zéro
docker compose down -v
```

### Connexion et inspection avec `psql`

```bash
# Accéder au terminal psql sur l'instance Develop (port 5433)
docker exec -it residmanager_db_dev psql -U postgres -d residmanager_db

# Accéder au terminal psql sur l'instance Main (port 5434)
docker exec -it residmanager_db_main psql -U postgres -d residmanager_db
```

---

## 🚀 Lancement des Applications

### 1. Démarrer le Serveur Backend (Ktor)

Le serveur supporte plusieurs environnements d'exécution selon la base de données cible :

```bash
# Lancement standard (par défaut)
./gradlew :server:run

# Environnement de développement (Base de données locale Develop, port 5433)
./gradlew :server:runDevelop

# Environnement réplique Main (Base de données locale Main, port 5434)
./gradlew :server:runMain
```

> **Note Android Studio / IntelliJ** : Les configurations d'exécution `:server:runDevelop` et `:server:runMain` sont également disponibles directement dans le menu déroulant des configurations de lancement de l'IDE.

### 2. Démarrer le Client Android
```bash
./gradlew :app:androidApp:assembleDebug
# Ou exécutez la configuration 'androidApp' directement depuis Android Studio
```

### 3. Démarrer le Client Web
```bash
# Cible Wasm (navigateurs modernes)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# Cible JS standard
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

### 4. Démarrer le Client iOS
Ouvrez le dossier `app/iosApp` dans **Xcode** et lancez l'application sur simulateur ou appareil physique.

---

## 🧪 Exécution des Tests

- **Tests Android** : `./gradlew :app:shared:testAndroidHostTest`
- **Tests Serveur Ktor** : `./gradlew :server:test`
- **Tests Web (Wasm)** : `./gradlew :app:shared:wasmJsTest`
- **Tests Web (JS)** : `./gradlew :app:shared:jsTest`
- **Tests Simulateur iOS** : `./gradlew :app:shared:iosSimulatorArm64Test`
