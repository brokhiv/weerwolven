package roles

/**
 * Generates a game with only normal Civilians and normal Werewolves.
 * @param amount Amount of players.
 * @param wolves Determines how many of the players are werewolves.
 */
fun civiliansWolves(amount: Int, wolves: (Int) -> Int = { it.floorDiv(4) }) = buildList {
    repeat(wolves(amount)) { add(Werewolf()) }
    repeat(amount - wolves(amount)) { add(Civilian()) }
}.shuffled()