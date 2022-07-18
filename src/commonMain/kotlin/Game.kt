import roles.*

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
    private val currentProperties = mutableListOf<PlayerProperty>()

    fun addProperty(property: PlayerProperty) {
        currentProperties.add(property)
    }

    fun removeProperty(property: PlayerProperty) {
        currentProperties.remove(property)
    }

    /**
     * Returns true if the Player is alive.
     */
    fun isAlive() = isAlive

    /**
     * Sets isAlive to false because the Player dies, and may trigger onDeath events.
     */
    fun dies(cause: DeathCause) {

        when (cause) {
            DeathCause.NATURAL_DEATH -> {
                // No onDeath events can trigger with a Natural Death
                println("$name is plotseling doodgegaan.")
                println("$name was $role.")
                isAlive = false
                return
            }
            DeathCause.LYNCHED -> {
                println("$name wordt op het vuur gegooid!")
            }
            DeathCause.WEREWOLF_ATTACK -> {
            }
            DeathCause.LOVER_DIED -> {}
        }

        role.onDeath(cause)

        // Kill lover if present
        (currentProperties.firstOrNull { it is InLoveWith } as InLoveWith?)?.lover?.dies(DeathCause.LOVER_DIED)

        println("$name was $role.")
        isAlive = false
    }

    fun canAct(daypart: Game.Daypart, date: Int) = role.canAct(daypart, date, currentProperties)

    override fun toString() = "$name de $role"
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

enum class DeathCause {
    NATURAL_DEATH, // Player is killed by the Game Leader, no onDeath events are triggered
    LYNCHED, // Player is voted to be lynched
    WEREWOLF_ATTACK, // Player is eaten by the Werewolves
    LOVER_DIED;
}

/**
 * Role that a Player can have.
 * @param actionPriority Determines the order in which this role acts at night, null if the Role does not act at night. A higher priority means that it can override an action with a lower priority.
 */
abstract class Role(val actionPriority: Int? = null) {
    /**
     * Alignment of the Role (Civilians, Werewolves, Neutral, Alone).
     */
    abstract val alignment: Alignment

    open val properties: List<PlayerProperty> = emptyList()

    /**
     * Returns true if the Player can perform their action at the current time in their current state.
     */
    abstract fun canAct(daypart: Game.Daypart, date: Int, currentProperties: MutableList<PlayerProperty>): Boolean

    /**
     * Can be called when the Player is successfully eaten by the werewolves.
     */
    open fun onDeath(cause: DeathCause) {}
}

/**
 * Default Role of every Player, should not exist once the game starts.
 */
object NullRole: Role() {
    override val alignment = Alignment.NEUTRAL
    override fun canAct(daypart: Game.Daypart, date: Int, currentProperties: MutableList<PlayerProperty>) = false
    override fun toString() = "Null Role"
}

/**
 * Action of a Player
 */
abstract class Action(val type: ActionType) {
    enum class ActionType(val isVisit: Boolean) {
        BASIC_ATTACK(true),
        BASIC_PROTECTION(true),
        VISIT(true);

        companion object {
            val attacks = listOf(BASIC_ATTACK)
            val protections = listOf<ActionType>()
        }
    }

    /**
     * Player who carries out the Action.
     */
    abstract val performer: Player

    /**
     * Target(s) of the Action.
     */
    abstract val targets: List<Player>

    /**
     * Priority of the Action, higher priority may override a lower priority Action.
     */
    abstract val priority: Int

    abstract fun execute(game: Game)

    abstract fun transform(performer: Player = this.performer, targets: List<Player> = this.targets): Action
}

class Game(private val players: List<Player>, val mode: GameMode = GameMode.SEQUENTIAL) {
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
    lateinit var mayor: Player

    val winners = mutableListOf<Player>()

    /**
     * Can be either DAY or NIGHT.
     */
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
    ).any { it }

    // --- --- Begin Win Conditions --- --- \\

    fun winCivilians() = alivePlayers().all { it.role.alignment.isGood() }

    fun winWolves() = alivePlayers().all { it.role.alignment.isEvil() }

    // --- ---  End Win Conditions  --- --- \\

    /**
     * Returns the Player with the given name, or throws a NullPointerException if not found.
     * @param name Name of the Player looked for.
     * @throws NullPointerException if the given Player is not in the game
     */
    private fun findPlayer(name: String) = players.find { it.name == name }!!

    /**
     *  Selects a living Player based on the name typed in IO.
     *  @param filter Optional, can filter players for eligibility.
     */
    fun selectPlayer(filter: (Player) -> Boolean = { true }): Player {
        var name = readln()
        while (name !in alivePlayers().filter(filter).map { it.name }) {
            println(if (name !in alivePlayers().map { it.name }) "Player not found, please try again" else "This player cannot be picked now, please enter another")
            name = readln()
        }
        return findPlayer(name)
    }

    /**
     * Returns all Players that are alive.
     */
    fun alivePlayers() = players.filter { it.isAlive() }

    /**
     * Returns all Players that have died.
     */
    fun deadPlayers() = players.filterNot { it.isAlive() }

    companion object {
        /**
         * Runs the Game.
         */
        fun run() {
            val game = run {
                // Create players with only names and generate their role later
                // FUTURE: add option to also specify role here
                println("Wie wonen er in Wakkerdam?")

                val players = buildList {
                    var line = readln()
                    while (line != "") {
                        add(Player(line))
                        line = readln()
                    }
                }

                civiliansWolves(players.size).let { roles -> players.forEachIndexed { i, p -> p.role = roles[i] } }
                Game(players)
            }

            println("Wie wordt de burgemeester van Wakkerdam?")
            game.mayor = game.selectPlayer().also { println("${it.name} is de burgemeester!") }

            while (!game.isOver()) {
                println("Heel Wakkerdam gaat slapen...")
                game.advanceTime()

                // -- Night section -- \\
                val actions = mutableListOf<Action>()

                if (game.alivePlayers().any { it.role is Werewolf }) {
                    println("De weerwolven worden wakker. Wie kiezen ze als prooi?")
                    val target = game.selectPlayer { it.role.alignment != Alignment.EVIL }

                    actions.add(Werewolf.Attack(
                        game.alivePlayers()
                            .filter { it.role is Werewolf }
                            .maxByOrNull { it.hashCode() }!!,
                        listOf(target)
                    ))
                }

                // Check who is protected, and scrap all attacks with lower priority
                run {
                    val protected = actions
                        .filter { it.type in Action.ActionType.protections }
                        .flatMap { a -> a.targets.map { t -> t to a.priority } }

                    actions
                        .map {a ->
                            if (a.type in Action.ActionType.attacks
                                && a.targets.intersect(protected.map { it.first }.toSet()).isNotEmpty()
                            ) {
                                // NOTE This may create attacks targeted at nobody
                                a.transform(targets = a.targets.filter { t ->
                                    (protected.firstOrNull { it.first == t }?.second ?: 0) > a.priority
                                })
                            }
                        }
                }

                // Check who will die to an attack, minus who is successfully protected
                val willDie = actions
                    .filter { it.type in Action.ActionType.attacks }
                    .flatMap { a -> a.targets }

                if (willDie.isEmpty()) println("Heel Wakkerdam wordt wakker!")
                else {
                    // TODO swap for executing all remaining attacks
                    willDie
                        .also { println("Heel Wakkerdam wordt wakker... behalve: ${it.joinToString() { it.name }}") }
                        .forEach { p -> p.dies(
                            when (actions
                                .filter { p in it.targets && it.type in Action.ActionType.attacks }
                                .maxByOrNull { it.priority }
                            ) {
                                is Werewolf.Attack -> DeathCause.WEREWOLF_ATTACK
                                else -> error("Unknown Attack Type")
                            }
                        ) }
                }

                if (game.isOver()) { break }

                game.advanceTime()
                // -- Day section -- \\

                if (!game.mayor.isAlive()) {
                    println("Wie wijst ${game.mayor.name} aan als opvolger?")
                    game.mayor = game.selectPlayer().also { println("$it wordt de nieuwe burgemeester!") }
                }

                println("Wie wordt er vandaag op het vuur gegooid?")
                game.selectPlayer().dies(DeathCause.LYNCHED)

                if (!game.mayor.isAlive()) {
                    println("Wie wijst ${game.mayor.name} aan als opvolger?")
                    game.mayor = game.selectPlayer().also { println("$it wordt de nieuwe burgemeester!") }
                }
            }

            // The game is over, who won?
            if (game.winCivilians()) {
                game.winners.addAll(game.players.filter { it.role.alignment == Alignment.GOOD })
            }
            if (game.winWolves()) {
                game.winners.addAll(game.players.filter { it.role.alignment == Alignment.EVIL })
            }

            println("${game.winners.joinToString { it.name }} ${if (game.winners.size == 1) "wint" else "winnen" }!")
        }
    }
}
