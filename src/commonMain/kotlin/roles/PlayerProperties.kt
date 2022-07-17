package roles

/**
 * A PlayerProperty can be added to a Player's properties field to indicate a state.
 */
abstract class PlayerProperty

/**
 * Number of (successful) actions the Player has performed.
 */
data class ActionCount(var count: Int): PlayerProperty()

/**
 * Indicates that the Player can no longer perform their action.
 */
object ActionLimitReached: PlayerProperty()