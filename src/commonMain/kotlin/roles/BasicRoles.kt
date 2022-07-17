package roles

import Action
import Alignment
import Player
import Role

class Civilian: Role() {
    override val alignment = Alignment.GOOD
    override fun canAct(daypart: Game.Daypart, date: Int) = false

    override fun toString() = "Burger"
}

open class Werewolf: Role(actionPriority = 1) {
    override val alignment = Alignment.EVIL
    override fun canAct(daypart: Game.Daypart, date: Int) = daypart == Game.Daypart.NIGHT

    override fun toString() = "Weerwolf"

    /**
     * Represents an attack by the Werewolves.
     * @param performer The Werewolf carrying the attack out.
     * @param targets The player(s) attacked, should only be 1.
     */
    data class Attack(override val performer: Player, override val targets: List<Player>): Action(ActionType.BASIC_ATTACK) {
        override val priority = Werewolf().actionPriority!!
        override val isVisit = true
    }
}