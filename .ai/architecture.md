# Architecture Globale & Conventions de Développement

## 1. Vue d'ensemble des Modules Gradle

Le projet **ResidManager** est structuré en multi-modules Kotlin Multiplatform (KMP) :

```text
ResidManager/
├── app/
│   ├── shared/         # Code commun UI & Business (Compose Multiplatform, KMP)
│   ├── androidApp/     # Application native Android (point d'entrée Android)
│   ├── webApp/         # Application Web (Kotlin/Wasm ou JS)
│   └── iosApp/         # Application iOS (Swift / Compose XCFramework)
├── core/               # Utilitaires et modèles multiplateformes agnostiques
└── server/             # Backend API REST (Ktor Server, JVM, PostgreSQL, Exposed)
```

### Règles strictes d'isolation des modules
1. **`:core`** :
   - Totalement agnostique de toute UI et de tout framework serveur.
   - Ne dépend ni d'Android, ni de Compose, ni de Ktor Server.
2. **`:app:shared`** :
   - Code commun UI (Compose Multiplatform) et logique applicative cliente.
   - **Interdiction formelle** d'utiliser des APIs spécifiques JVM/Android (ex. `java.util.*`, `android.util.Log`, `Context`) dans `commonMain`. Utiliser le pattern `expect`/`actual` si une fonctionnalité système est indispensable.
   - Dépend de `:core`.
3. **`:server`** :
   - Application Ktor autonome (JVM).
   - Dépend de `:core` (et des DTOs communs). Ne doit **JAMAIS** dépendre de `:app:shared` ou d'une quelconque bibliothèque Compose/UI.
4. **`:app:androidApp`, `:app:webApp`, `:app:iosApp`** :
   - Points d'entrée légers qui délèguent l'exécution au composable principal d'`app:shared` (`App()`).

---

## 2. Principes Clean Architecture & MVI (Côté Client `:app:shared`)

Tout nouveau composant ou refactorisation d'écran dans `:app:shared` **DOIT** suivre rigoureusement le pattern **MVI (Model-View-Intent) + Clean Architecture** :

```text
[ Composable Screen / UI Components ]
             ▲ │
(UiState /   │ │ (User Intents / Actions)
UiEffect)    │ ▼
      [ MviViewModel ]
             │
             ▼
        [ UseCase ]  (Logique métier pure, validation, opérateur invoke)
             │
             ▼
       [ Repository ] (Interface & Implémentation, gestion du cache & network)
             │
             ▼
        [ ApiClient ] (Appels HTTP Ktor Client)
```

### Contrats et Rôles :
1. **UI (`Screen` & Composables)** :
   - Purement déclarative, passive.
   - Collecte le `uiState` (via `collectAsState()`) et observe les `effects` (via `LaunchedEffect`).
   - Émet des `Intent` au ViewModel. Ne déclenche jamais directement d'appels API ou de modifications de repository.
2. **ViewModel (`MviViewModel<State, Intent, Effect>`)** :
   - Hérite de `com.resid.manager.base.MviViewModel`.
   - Reçoit les intentions via `onIntent(intent: Intent)`.
   - Modifie l'état uniquement via `updateState { ... }`.
   - Déclenche les effets à usage unique (navigation, SnackBar, toast, modal) via `emitEffect(...)`.
   - Dépend exclusivement de **UseCases**, jamais directement d'un `ApiClient` ou de la base de données.
3. **UseCases (`usecase/`)** :
   - Une seule responsabilité par UseCase.
   - Nommage : `<Verbe><Entité>UseCase` (ex. `GetLeaseDetailsUseCase`, `CreateInspectionUseCase`).
   - Expose une fonction `suspend operator fun invoke(...) : Result<T>`.
   - Effectue les validations métier préalables avant de déléguer au Repository.
4. **Repositories (`repository/`)** :
   - Interface + classe concrète.
   - Isole la source de données (`ApiClient`, `SessionStorage`).
   - Retourne toujours des types sécurisés (`Result<T>` ou modèles DTO).

---

## 3. Organisation du Code Client par Feature (`feature-first`)

Afin de garantir une forte cohésion et un couplage faible, le code du module client (`:app:shared`) **DOIT** être organisé **par Feature**. L'ancien découpage horizontal (`ui/`, `viewmodel/`) est abandonné au profit d'une structure modulaire par domaine d'usage :

```text
app/shared/src/commonMain/kotlin/com/resid/manager/
│
├── features/
│   ├── auth/                          # Feature Authentification (Login, Register)
│   │   ├── navigation/                # Clés Navigation 3 (AuthNavKey : NavKey)
│   │   ├── mvi/                       # AuthContract.kt (UiState, Intent, Effect)
│   │   ├── AuthViewModel.kt           # MviViewModel
│   │   ├── ui/                        # Composables de la feature
│   │   │   ├── LoginScreen.kt
│   │   │   ├── RegisterScreen.kt
│   │   │   └── components/            # Sous-composants dédiés (AuthHeader, LoginForm...)
│   │   └── usecase/                   # LoginUseCase, RegisterUseCase...
│   │
│   ├── dashboard/                     # Feature Dashboard (KPIs, Résumés)
│   │   ├── mvi/                       # DashboardContract.kt
│   │   ├── DashboardViewModel.kt
│   │   └── ui/
│   │       ├── DashboardScreen.kt
│   │       └── components/            # Widgets, KpiCards, RecentActivityList...
│   │
│   ├── residences/                    # Feature Résidences
│   │   ├── mvi/                       # ResidencesContract.kt
│   │   ├── ResidencesViewModel.kt
│   │   ├── ui/
│   │   │   ├── ResidencesScreen.kt
│   │   │   └── components/            # ResidenceCard, CreateResidenceDialog...
│   │   └── usecase/                   # GetResidencesUseCase, CreateResidenceUseCase...
│   │
│   ├── units/                         # Feature Logements / Lots (Units)
│   │   ├── mvi/
│   │   ├── UnitsViewModel.kt
│   │   └── ui/
│   │       ├── UnitsScreen.kt
│   │       └── components/            # UnitCard, AddUnitModal...
│   │
│   ├── leases/                        # Feature Baux & Contrats
│   ├── electricity/                   # Feature Électricité & Compteurs
│   ├── finances/                      # Feature Finances & Transactions
│   ├── tickets/                       # Feature Tickets de Maintenance
│   ├── members/                       # Feature Membres & Rôles
│   └── profile/                       # Feature Profil & Préférences
│
└── core/                              # Socle commun transverse
    ├── base/                          # MviViewModel.kt
    ├── di/                            # DiConfig.kt (Koin)
    ├── navigation/                    # Root NavKey & PlatformBackHandler
    ├── network/                       # ApiClient.kt (Ktor client)
    ├── session/                       # SessionStorage.kt
    └── ui/                            # Design System, Layout global & I18n
        ├── theme/                     # ResidTheme, Color, Type
        ├── layout/                    # AppShell.kt, SidebarContent.kt, HeaderBar.kt
        ├── components/                # ImagePicker, AppButton, AppTextField, Badges génériques...
        └── i18n/                      # AppStrings, FrStrings, EnStrings
```

### Règles d'or du packaging Feature-First :
1. **Autonomie d'une Feature** : Tout ce qui concerne une feature (State, ViewModel, UseCases, Composables, Dialogs) réside dans son dossier `features/<nom>/`.
2. **Réutilisabilité** : Si un composant UI ou un utilitaire est utilisé par plus de deux features indépendantes, il doit être extrait vers `core/ui/components/` ou `core/`.
3. **Module Koin par Feature** : Chaque feature déclare idéalement son module Koin (ex. `val authModule = module { ... }`), assemblé ensuite dans `DiConfig.kt`.

---

## 4. Architecture Backend (`:server`)

Le backend suit une architecture en 3 couches distinctes :

```text
[ Ktor Routing ] (routes/*.kt)
       │
       ▼
 [ Service Layer ] (service/*Service.kt)
       │
       ▼
[ Exposed DAO / DSL ] (data/DatabaseSchema.kt, Tables)
       │
       ▼
  [ PostgreSQL ]
```

1. **Routes (`routes/`)** :
   - Validation de la session/JWT (`call.principal<UserPrincipal>()`).
   - Parsing et validation syntaxique du body DTO.
   - Délégation immédiate au Service approprié.
   - Réponse HTTP avec status code explicite (200, 201, 400, 401, 403, 404, 500).
2. **Services (`service/`)** :
   - Contient l'intégralité de la logique métier (calculs, transitions d'états, autorisations).
   - Encapsule les transactions de base de données (`dbQuery { ... }`).
   - Lève des exceptions typées `HttpError(HttpStatusCode, "message")` en cas d'erreur métier.
3. **Données (`data/`)** :
   - Tables Exposed strictement typées.
   - Enums partagés ou synchronisés avec le domaine métier.

---

## 5. Règles de Découpage et Qualité de Code

- **Taille des fichiers UI** : Aucun fichier Composable ne doit dépasser **250 lignes**. Dès qu'un écran grossit, extraire systématiquement les sous-composants, formulaires, dialogs et tables dans le sous-dossier `features/<feature>/ui/components/`.
- **Nommage des fichiers** :
  - Écrans racines : `<Feature>Screen.kt`.
  - Sous-composants : `<Concept>Card.kt`, `<Action>Dialog.kt`, `<Feature>Table.kt`.
  - Contrats MVI : `<Feature>Contract.kt` contenant `UiState`, `Intent`, `Effect`.
  - ViewModels : `<Feature>ViewModel.kt`.
  - UseCases : `<Verbe><Entite>UseCase.kt`.
- **Gestion des Dépendances & DI (Koin)** :
  - Déclaration centralisée via Version Catalog (`gradle/libs.versions.toml`).
  - **Injection de dépendances obligatoire avec Koin** (`DiConfig.kt`).
  - Les ViewModels, UseCases, Repositories, Clients HTTP et Stockages sont déclarés dans des modules Koin dédiés (`authModule`, `residencesModule`, `repositoryModule`, `networkModule`, etc.).
  - Dans les Composables Compose Multiplatform, la récupération des ViewModels ou services se fait impérativement via Koin (`koinInject()` ou `koinViewModel()`).

---

## 6. Navigation avec Navigation 3 (`androidx.navigation3`)

La navigation dans `:app:shared` **DOIT** être implémentée à l'aide de **Jetpack Navigation 3** :

1. **Typage strict des Destinations (`NavKey`)** :
   - Chaque destination ou écran navigable est défini comme une clé typée et sérialisable implémentant `NavKey` (ex. `AppNavKey` avec `@Serializable sealed interface AppNavKey : NavKey`).
   - Déclarer des `data object` pour les écrans statiques et des `data class` sérialisables avec arguments typés pour les écrans dynamiques avec paramètres (ex. `data class LeaseDetails(val leaseId: String) : AppNavKey`).
2. **Gestion de la pile (`NavBackStack`) & Affichage (`NavDisplay`)** :
   - Utiliser `NavBackStack<NavKey>` pour maintenir l'état de la navigation.
   - Utiliser `NavDisplay(backStack = backStack, entryProvider = { key -> NavEntry(key) { ... } })` pour mapper chaque clé à son écran Composable.
3. **Support multiplateforme du bouton Retour (`PlatformBackHandler`)** :
   - Toujours interconnecter le retour arrière avec `PlatformBackHandler` pour gérer nativement l'historique de navigation du navigateur Web (événement `popstate`) ainsi que le retour système Android / geste iOS.
4. **Coordination MVI et Navigation 3 :**
   - Les transitions de navigation déclenchées par l'utilisateur transitent par les `Intent` du MviViewModel ou sont émises via des `Effect` (ex. `FeatureEffect.NavigateTo(...)`).
   - Le Composable racine ou l'hôte de navigation écoute ces effets et manipule le `NavBackStack` (`backStack.add(key)`, `backStack.removeLastOrNull()`, `backStack.clear()`).
