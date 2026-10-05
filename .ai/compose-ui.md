# Règles Compose Multiplatform & UI Design System

Ce document définit les standards d'implémentation de l'interface utilisateur pour le module `:app:shared` (Compose Multiplatform ciblant Web, Android, iOS, Desktop).

---

## 1. Respect Strict du Design System "Emerald Estate" (`DESIGN.md`)

L'application utilise un thème **Dark Mode par défaut**, professionnel et épuré.

### Palette de Couleurs & Tokens (`ResidTheme`)
- **Fond de l'application (Base)** : `#031427` / `#0F172A` (Slate 900 très profond).
- **Surfaces & Cards (Level 1)** : `#102034` / `#1E293B` avec bordure subtile de 1px `#1b2b3f` / `#334155`.
- **Surfaces surélevées / Overlays (Level 2/3)** : `#26364a` / `#2a3a4f`.
- **Primary (Accent Brand)** : Emerald 600 (`#059669`) ou Emerald Tint (`#68dba9`). Utilisé pour les actions clés (CTA, boutons principaux, statuts actifs, focus).
- **Texte** :
  - Sur fond sombre : `#d3e4fe` / `#F8FAFC` (haut contraste).
  - Texte secondaire / labels : `#87948b` / `#94A3B8`.
- **Statuts Sémantiques** :
  - Succès / Actif / Payé : Vert émeraude (`#059669` / `#68dba9`).
  - Attention / En attente : Ambre / Jaune (`#F59E0B`).
  - Erreur / Impayé / Rejeté : Rouge sombre / Coral (`#EF4444` / `#ffb4ab`).
  - Neutre / Brouillon : Gris / Ardoise (`#64748B`).

> **Règle absolue :** Ne jamais coder de couleurs en dur dans les Composables (ex. `Color(0xFF123456)` ou `Color.Black`). Toujours utiliser les tokens du thème : `MaterialTheme.colorScheme.*` ou les couleurs définies dans `com.resid.manager.ui.theme.ResidTheme`.

### Typographie & Espacement
- **Police** : **Inter** uniquement.
- **Grille & Rythme 8px** :
  - Marges & espacements : `4.dp`, `8.dp`, `16.dp`, `24.dp`, `32.dp`.
  - Padding interne des Cards standard : `24.dp`.
  - Corner Radius :
    - Éléments standards (Cards, Inputs, Boutons) : `8.dp` (0.5rem).
    - Conteneurs larges / Widgets du Dashboard : `16.dp` (1rem).
    - Status Chips / Badges : `full` (`RoundedCornerShape(percent = 50)` ou `9999.dp`).

---

## 2. Structure Canonique d'une Feature MVI

Chaque feature (ex: `features/residences/`, `features/leases/`, etc.) doit respecter le découpage suivant :

### A. Définition du Contrat MVI (`features/<feature>/mvi/<Feature>Contract.kt`)
```kotlin
// 1. État complet de l'écran (Immutable data class)
data class FeatureUiState(
    val isLoading: Boolean = false,
    val items: List<ItemDto> = emptyList(),
    val errorMessage: String? = null,
    val isCreateDialogOpen: Boolean = false
)

// 2. Intentions de l'utilisateur (User Actions)
sealed interface FeatureIntent {
    data object Refresh : FeatureIntent
    data class OnItemClicked(val id: String) : FeatureIntent
    data class SubmitForm(val payload: CreateItemRequest) : FeatureIntent
    data class SetDialogOpen(val isOpen: Boolean) : FeatureIntent
}

// 3. Événements uniques (One-off side effects : navigation, snackbar, toast)
sealed interface FeatureEffect {
    data class NavigateToDetails(val id: String) : FeatureEffect
    data class ShowToast(val message: String) : FeatureEffect
}
```

### B. Le ViewModel (`features/<feature>/<Feature>ViewModel.kt`)
```kotlin
class FeatureViewModel(
    private val getItemsUseCase: GetItemsUseCase,
    private val createItemUseCase: CreateItemUseCase
) : MviViewModel<FeatureUiState, FeatureIntent, FeatureEffect>(FeatureUiState()) {

    override fun onIntent(intent: FeatureIntent) {
        when (intent) {
            is FeatureIntent.Refresh -> loadItems()
            is FeatureIntent.OnItemClicked -> emitEffect(FeatureEffect.NavigateToDetails(intent.id))
            is FeatureIntent.SubmitForm -> handleCreate(intent.payload)
            is FeatureIntent.SetDialogOpen -> updateState { it.copy(isCreateDialogOpen = intent.isOpen) }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            getItemsUseCase()
                .onSuccess { data -> updateState { it.copy(isLoading = false, items = data) } }
                .onFailure { err -> updateState { it.copy(isLoading = false, errorMessage = err.message) } }
        }
    }
}
```

### C. Le Screen Composable (`features/<feature>/ui/<Feature>Screen.kt`)
Pour assurer la testabilité, l'injection propre et la prévisualisation, séparez toujours le point d'entrée connecté au ViewModel de l'UI pure :

```kotlin
// Point d'entrée connecté (Stateful) : Injection Koin + Navigation 3
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = koinInject(),
    onNavigateToDetails: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Gestion des effets ponctuels (navigation, snackbar, etc.)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FeatureEffect.NavigateToDetails -> onNavigateToDetails(effect.id)
                is FeatureEffect.ShowToast -> { /* afficher notification */ }
            }
        }
    }

    FeatureContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}

// UI Pure (Stateless) - Idéal pour les Previews et les tests
@Composable
fun FeatureContent(
    state: FeatureUiState,
    onIntent: (FeatureIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Rendu des sous-composants situés dans features/<feature>/ui/components/
        }
    }
}
```

### D. Organisation des sous-composants (`features/<feature>/ui/components/`)
Afin de respecter la limite stricte de **250 lignes par fichier** :
- Les cartes de listes : `features/<feature>/ui/components/<Feature>Card.kt`.
- Les boîtes de dialogue / modales : `features/<feature>/ui/components/Create<Feature>Dialog.kt`.
- Les filtres et barres d'action : `features/<feature>/ui/components/<Feature>FilterBar.kt`.

### D. Enregistrement dans Navigation 3 (`NavEntry`)
Dans l'orchestrateur de navigation racine (`NavDisplay`) :

```kotlin
NavDisplay(
    backStack = backStack,
    entryProvider = { key: AppNavKey ->
        when (key) {
            is AppNavKey.Feature -> NavEntry(key) {
                FeatureScreen(
                    onNavigateToDetails = { id -> backStack.add(AppNavKey.Details(id)) }
                )
            }
            // Autres destinations...
        }
    }
)
```

---

## 3. Bonnes Pratiques & Interdictions Compose

1. **Navigation 3 stricte au lieu de variables d'état** :
   - Toujours utiliser Jetpack Navigation 3 (`androidx.navigation3.runtime.NavKey`, `NavBackStack`, `NavDisplay`) pour orchestrer la navigation entre écrans et flux.
   - Ne jamais utiliser de variables d'état locales ou globales (ex: `var currentScreen by remember { mutableStateOf(...) }`, `when (activeTab) { ... }`) pour commuter des écrans complets.
2. **Séparation des écrans par fichier distinct** :
   - Chaque écran (`Screen.kt`) doit obligatoirement être déclaré dans son propre fichier dédié.
   - Ne jamais déclarer plusieurs écrans ou sous-écrans dans le même fichier.
3. **Découpage en composants réutilisables (Limite stricte de 250 lignes)** :
   - Toujours scinder la vue en composants réutilisables et autonomes dans `features/<feature>/ui/components/`.
   - Extraire systématiquement les list items, cartes, formulaires, barres de filtres, widgets et dialogues dans des fichiers séparés.
   - Ne pas empiler tous les éléments graphiques d'une page dans un seul fichier géant.
4. **Icônes Material Design obligatoires (Zéro Emoji)** :
   - Ne jamais utiliser d'emojis pour des icônes, des indicateurs visuels ou des boutons d'action dans l'UI (ex: ❌, ✅, 🏠, ⚡, 📋).
   - Utiliser exclusivement les icônes Material (`androidx.compose.material.icons.Icons.Default.*` ou `.AutoMirrored.*`).
5. **Pas de logique métier dans l'UI** : Les validations complexes, calculs financiers ou appels réseau ne doivent jamais figurer dans un Composable. L'UI se contente d'émettre `onIntent(Intent)`.
6. **Responsive Web & Mobile** :
   - Prévoir des dispositions flexibles (`BoxWithConstraints` ou adaptive layouts) pour supporter aussi bien les écrans desktop (grille 12 colonnes, sidebar 260px) que les écrans mobiles (colonne unique, barre de navigation basse ou drawer).
7. **Accessibilité & Feedback** :
   - Chaque action asynchrone doit avoir un indicateur de chargement (`CircularProgressIndicator` ou skeleton).
   - Les formulaires doivent afficher des messages d'erreur clairs sous chaque champ invalide.
