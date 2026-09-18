# Wear compile fix while preserving prior navigation/language fixes

CI exposed a compile error in WearInternalNavHost.kt: Navigation Compose 2.9.8 NavHost does not expose predictivePopEnterTransition/predictivePopExitTransition parameters.

Minimal correction:
- Removed only predictivePopEnterTransition and predictivePopExitTransition from the child NavHost call.
- Kept enterTransition/exitTransition/popEnterTransition/popExitTransition as EnterTransition.None / ExitTransition.None, preserving the single-motion-owner design.
- Preserved android:enableOnBackInvokedCallback="false".
- Preserved Android 13+ LocaleManager.applicationLocales language handling and legacy fallback.
- Preserved hiltViewModel() cleanup and existing routes/back stacks/ViewModel scopes.

The provided CI log reached :wear:compileDebugKotlin and failed specifically on the unsupported NavHost parameters. Local sandbox compilation could not proceed because the Gradle wrapper distribution is not cached and services.gradle.org cannot be resolved here.
