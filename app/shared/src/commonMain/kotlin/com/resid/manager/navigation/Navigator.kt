package com.resid.manager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration

/**
 * Custom Navigator wrapping Navigation 3 NavBackStack.
 */
@Stable
class Navigator(
    val startEntry: NavKey,
    internal val stateBackStack: NavBackStack<NavKey>
) {
    /**
     * Returns the active screen at the top of the stack, or startEntry if the stack is empty.
     */
    fun current(): NavKey = stateBackStack.lastOrNull() ?: startEntry

    /**
     * Returns an immutable copy of the back stack.
     */
    fun backStack(): List<NavKey> = stateBackStack.toList()

    /**
     * Pushes a new entry onto the stack.
     * If [unique] is true, removes any existing occurrence of the entry before pushing.
     * Ignores the operation if the top of the stack is already equal to [entry].
     */
    fun push(entry: NavKey, unique: Boolean = false) {
        if (stateBackStack.lastOrNull() == entry) return
        if (unique) {
            stateBackStack.removeAll { it == entry }
        }
        stateBackStack.add(entry)
    }

    /**
     * Removes the top entry from the stack, only if the stack size is greater than 1 (to preserve startEntry).
     * @return true if an entry was popped, false otherwise.
     */
    fun popBack(): Boolean {
        if (stateBackStack.size > 1) {
            stateBackStack.removeLastOrNull()
            return true
        }
        return false
    }
}

/**
 * Remembers a [Navigator] instance backed by Navigation 3 [rememberNavBackStack].
 */
@Composable
fun rememberNavigator(
    startEntry: NavKey,
    configuration: SavedStateConfiguration = DefaultSavedStateConfiguration
): Navigator {
    val backStack = rememberNavBackStack(configuration, startEntry)
    return remember(backStack, startEntry) {
        Navigator(startEntry = startEntry, stateBackStack = backStack)
    }
}
