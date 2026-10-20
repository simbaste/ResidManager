# Guide de Débogage Web (Kotlin/Wasm & Kotlin/JS)

Ce guide décrit comment lancer et déboguer interactivement l'application Web de ResidManager avec des points d'arrêt (breakpoints) dans IntelliJ IDEA / Android Studio ou directement dans Google Chrome DevTools.

---

## 1. Caractéristiques de l'environnement Web

- **Framework :** Compose Multiplatform for Web (`Canvas / Skiko / ComposeViewport`)
- **Cibles Web configurées :**
  - **`wasmJs` (Kotlin/Wasm) :** Cible moderne haute performance.
  - **`js` (Kotlin/JS) :** Cible compatible avec source maps complètes.
- **Port de développement local :** `http://localhost:8080/`
- **Serveur API Backend local :** `http://localhost:8081` (Ktor)

---

## 2. Commandes Gradle pour lancer le serveur de dev

### Mode WebAssembly (`wasmJs`) :
```bash
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun --continuous
```

### Mode JavaScript (`js`) :
```bash
./gradlew :app:webApp:jsBrowserDevelopmentRun --continuous
```

> **Note :** Le flag `--continuous` active le rechargement automatique à chaud lors de la modification des fichiers sources Kotlin.

---

## 3. Débogage dans IntelliJ IDEA / Android Studio

Des configurations d'exécution prêtes à l'emploi ont été créées dans le dossier partagé `.run/` :

1. **`Web: Wasm Dev Server (:app:webApp)`** : Démarre le serveur Webpack de développement Wasm sur le port 8080 en mode continu.
2. **`Web: JS Dev Server (:app:webApp)`** : Démarre le serveur Webpack de développement JS sur le port 8080 en mode continu.
3. **`Web: Debug in Chrome (http://localhost:8080)`** : Démarre une session de débogage Chrome attachée à `http://localhost:8080/`.
4. **`Web: Debug Wasm (Compound)`** ou **`Web: Debug JS (Compound)`** : Lance le serveur puis attache la session de débogage Chrome en une seule action.

### Poser des points d'arrêt dans l'IDE :
1. Lancez la configuration **`Web: Debug JS (Compound)`** (ou lancez le serveur puis la configuration JavaScript Debug).
2. Ouvrez un fichier Kotlin partagé ou web (ex: `RegisterScreen.kt`, `LoginScreen.kt`, `App.kt`).
3. Cliquez dans la marge pour poser un point d'arrêt (rond rouge).
4. Effectuez l'action dans le navigateur : l'exécution s'interrompt directement sur votre ligne Kotlin dans l'IDE.

---

## 4. Débogage directement dans Google Chrome DevTools

### Débogage Kotlin/JS :
1. Démarrez `./gradlew :app:webApp:jsBrowserDevelopmentRun`.
2. Ouvrez Google Chrome à l'adresse `http://localhost:8080/`.
3. Ouvrez les DevTools (`F12` ou `Cmd+Option+I` / `Ctrl+Shift+I`).
4. Allez dans l'onglet **Sources**.
5. Appuyez sur `Ctrl+P` (Windows/Linux) ou `Cmd+P` (macOS) et tapez le nom d'un fichier Kotlin (ex: `RegisterScreen.kt`, `AuthViewModel.kt`).
6. Posez vos points d'arrêt directement sur le code source Kotlin grâce aux Source Maps.
7. Vous pouvez également insérer l'instruction `js("debugger;")` dans votre code Kotlin pour provoquer une interruption immédiate.

### Débogage Kotlin/Wasm (`wasmJs` avec DWARF) :
1. Démarrez `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`.
2. Installez l'extension Chrome officielle : [C/C++ DevTools Support (DWARF)](https://chromewebstore.google.com/detail/cc++-devtools-support-dwa/pdcpmagijalflfakpmioapggnglhhbal).
3. Ouvrez les DevTools de Chrome (`F12`), cliquez sur la roue crantée **Settings** (en haut à droite des DevTools) > **Experiments**.
4. Cochez l'option **"WebAssembly Debugging: Enable DWARF support"**.
5. Rechargez la page `http://localhost:8080/`.
6. Dans l'onglet **Sources**, les fichiers `.kt` apparaîtront dans l'arborescence des sources WebAssembly et vous pourrez inspecter les variables locales et la pile d'appels.

---

## 5. Gestion des exceptions et erreurs globales

Le point d'entrée `main.kt` intègre un intercepteur global d'erreurs :
- Les exceptions non interceptées sont journalisées avec le préfixe `[ResidManager Web Error]`.
- Les rejets de promesses asynchrones sont journalisés avec `[ResidManager Unhandled Promise Rejection]`.
- Un bandeau discret s'affiche en bas de page pour signaler l'erreur sans masquer la vue silencieusement si Compose rencontre une anomalie au démarrage.
