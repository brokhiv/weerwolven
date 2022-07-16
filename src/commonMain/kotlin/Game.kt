/**
 * A Player of Weerwolven.
 * @param name Name of the Player.
 * @param role Role of the Player, NullRole by default.
 */
class Player(val name: String, var role: Role = NullRole) {
    var isAlive = true
    var canVote = true
}

/**
 * Role that a Player can have.
 */
abstract class Role {
}

/**
 * Default Role of every Player, will throw a NotImplementedException on any method.
 */
object NullRole: Role() {}