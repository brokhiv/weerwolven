package roles

import Action
import Alignment
import Game
import Player
import Role

class Civilian: Role() {
    override val alignment = Alignment.GOOD
    override fun canAct(daypart: Game.Daypart, date: Int, currentProperties: MutableList<PlayerProperty>) = false

    override fun toString() = "Burger"
}

class Werewolf: Role() {
    override val alignment = Alignment.EVIL
    override fun canAct(daypart: Game.Daypart, date: Int, currentProperties: MutableList<PlayerProperty>) =
        daypart == Game.Daypart.NIGHT

    override fun toString() = "Weerwolf"

    /**
     * Represents an attack by the Werewolves.
     * @param performer The Werewolf carrying the attack out.
     * @param targets The player(s) attacked, should only be 1.
     */
    data class Attack(override val performer: Player, override val targets: List<Player>): Action(ActionType.BASIC_ATTACK) {
        override val priority = 1

        override fun execute(game: Game) {
            targets.map { it.dies(DeathCause.WEREWOLF_ATTACK) }
        }

        override fun transform(performer: Player, targets: List<Player>) = Attack(performer, targets)
    }
}

object Cupid: Role() {
    override val alignment = Alignment.GOOD

    override val properties = listOf(ActionCount(1))

    override fun canAct(daypart: Game.Daypart, date: Int, currentProperties: MutableList<PlayerProperty>) =
        daypart == Game.Daypart.NIGHT && (currentProperties.first { it is ActionCount } as ActionCount).count > 0

    override fun toString() = "Cupido"

    data class Couple(override val performer: Player, val lovers: Pair<Player, Player>): Action(ActionType.VISIT) {
        override val priority: Int = 2
        override val targets: List<Player>
            get() = lovers.toList()

        override fun execute(game: Game) = with(lovers) {
            first.addProperty(InLoveWith(second))
        }

        override fun transform(performer: Player, targets: List<Player>) =
            Couple(performer, targets.let { (p, q) -> p to q })
    }
}