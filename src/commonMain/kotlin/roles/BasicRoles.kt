package roles

import Alignment
import Role

class Civilian: Role() {
    override val alignment = Alignment.GOOD
    override fun canAct(daypart: Game.Daypart, date: Int) = false
}

class Werewolf: Role() {
    override val alignment = Alignment.EVIL
    override fun canAct(daypart: Game.Daypart, date: Int) = daypart == Game.Daypart.NIGHT
}