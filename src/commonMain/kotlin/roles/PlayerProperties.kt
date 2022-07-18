package roles

import Player

/**
 * A PlayerProperty can be added to a Player's properties field to indicate a state.
 */
abstract class PlayerProperty

/**
 * Number of (successful) actions the Player can still perform.
 */
data class ActionCount(var count: Int): PlayerProperty()

data class InLoveWith(val lover: Player): PlayerProperty()

data class Protected(val against: List<Action.ActionType>, val priority: Int): PlayerProperty()