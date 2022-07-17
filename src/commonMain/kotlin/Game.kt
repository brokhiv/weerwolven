import roles.ActionLimitReached
import roles.PlayerProperty
import roles.civiliansWolves
import kotlin.io.readlnOrNull

/**
 * A Player of Weerwolven.
 * @param name Name of the Player.
 * @param role Role of the Player, NullRole by default.
 */
data class Player(val name: String, var role: Role = NullRole) {
    /**
     * Whether this Player is alive.
     */
    private var isAlive = true

    /**
     * Whether this Player can vote.
     */
    private var canVote = true

    /**
     * Keeps the state of a Player.
     */
    val properties = mutableListOf<PlayerProperty>()

    /**
     * Returns true if the Player is alive.
     */
    fun isAlive() = isAlive

    /**
     * Sets isAlive to false because the Player dies.
     */
    fun dies() {
        isAlive = false
    }

    fun canAct(daypart: Game.Daypart, date: Int) = role.canAct(daypart, date) && ActionLimitReached !in properties
}

/**
 * Alignment that a Player can have.
 * @instance GOOD Wins with civilians.
 * @instance NEUTRAL Has own win condition, does not prevent anyone from winning.
 * @instance EVIL Wins with wolves.
 * @instance ALONE Has own win condition, prevents everyone from winning.
 */
enum class Alignment {
    GOOD,    // Good roles (winning with civilians)
    NEUTRAL, // Neutral roles (do not prevent anyone from winning)
    EVIL,    // Evil Roles (winning with wolves)
    ALONE;   // Roles that only win by themselves (prevent others from winning)

    fun isGood() = this == GOOD || this == NEUTRAL
    fun isEvil() = this == EVIL || this == NEUTRAL
}

/**
 * Role that a Player can have.
 * @param actionPriority Determines the order in which this role acts at night, null if the Role does not act at night.
 */
abstract class Role(val actionPriority: Int? = null) {
    /**
     * Alignment of the Role (Civilians, Werewolves, Neutral, Alone).
     */
    abstract val alignment: Alignment

    /**
     * Returns true if the Player can perform their action at the current time in their current state.
     */
    abstract fun canAct(daypart: Game.Daypart, date: Int): Boolean

    /**
     * Can be called when the Player is successfully eaten by the werewolves.
     */
    fun whenEaten() {}

    /**
     * Can be called when the Player is lynched by the town.
     */
    fun whenLynched() {}
}

/**
 * Default Role of every Player, will throw a NotImplementedError on any method.
 */
object NullRole: Role() {
    override val alignment = throw NotImplementedError("Null Role")
    override fun canAct(daypart: Game.Daypart, date: Int) = throw NotImplementedError("Null Role")
    override fun toString() = "Null Role"
}

class Game(val players: List<Player>, val mode: GameMode = GameMode.SEQUENTIAL) {
    /**
     * Ways that the Game can be played.
     * @instance SEQUENTIAL All actions are called one-by-one and fed back, anytime-roles are not possible.
     * @instance CONCURRENT Actions are picked over time, feedback comes at once, anytime-roles are possible.
     */
    enum class GameMode {
        SEQUENTIAL, // All actions are called one-by-one and fed back, anytime-roles are not possible.
        CONCURRENT, // Actions are picked over time, feedback comes at once, anytime-roles are possible.
    }

    /**
     * Mayor of Wakkerdam, is elected on Day 0.
     */
    var mayor: Player = throw NullPointerException()

    enum class Daypart {DAY, NIGHT}


    /**
     * Time of the game, split in Daypart (Day or Night) and Date (number of the Day/Night).
     */
    private var time = Daypart.DAY to 0

    /**
     * Returns the in-game time.
     */
    fun time() = time

    /**
     * Advances the time from the current Day to the next Night or from the current Night to the next Day.
     */
    fun advanceTime() {
        time = time.let { (daypart, date) ->
            if (daypart == Daypart.DAY) Daypart.NIGHT to date + 1 else Daypart.DAY to date
        }
    }

    /**
     * Returns true if the Game is over, i.e. some party has won.
     */
    fun isOver() = listOf(
        winCivilians(),
        winWolves(),
    ).any()

    // --- --- Begin Win Conditions --- --- \\

    fun winCivilians() = alivePlayers().all { it.role.alignment.isGood() }

    fun winWolves() = alivePlayers().all { it.role.alignment.isEvil() }

    // --- ---  End Win Conditions  --- --- \\

    /**
     * Returns all Players that are alive.
     */
    fun alivePlayers() = players.filter { it.isAlive() }

    /**
     * Returns all Players that have died.
     */
    fun deadPlayers() = players.filterNot { it.isAlive() }

    /**
     * Runs the Game.
     */
    fun main() {
        // Create players with only names and generate their role later
        // FUTURE: add option to also specify role here
        val players = buildList {
            var line = readlnOrNull()
            while (line != null) {
                add(Player(line))
                line = readlnOrNull()
            }
        }

        civiliansWolves(players.size).let { roles -> players.forEachIndexed { i, p -> p.role = roles[i] } }

        val game = Game(players)

        game.players.forEach { println(it.toString()) }
    }
}
