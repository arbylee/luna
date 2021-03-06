/*
 A plugin that adds functionality for sending 'world' announcements.

 SUPPORTS:
  -> Random message selection at fixed interval.
  -> Filtering certain players from the announcements.

 AUTHOR: lare96
*/

import io.luna.game.event.impl.ServerLaunchEvent
import io.luna.game.model.mobile.Player


/* How often announcements will be sent, in ticks. */
private val TICK_INTERVAL = 1500 // 15 mins

/* Messages that will be randomly selected every 'TICK_INTERVAL'. */
private val MESSAGES = Vector(
  "Luna is a Runescape private server for the #317 protocol.",
  "Lare96's favorite bands are Tame Impala, Black Sabbath, and Bad Brains.",
  "Lare96 enjoys collecting records from various places around the world.",
  "Lare96 wants to travel to Japan, Iran, Turkey, Brazil, and Germany.",
  "Change these messages in /plugins/player/announcement.scala",
  "Any bugs found using Luna should be reported to the github page."
)

/*
 Filter that will be applied on players before the announcement is sent. This allows for
 only specific players receiving announcements.
*/
private val FILTER = (plr: Player) => plr.rights <=@ RIGHTS_ADMIN // Only players below admin rank receive announcements.


/*
 When the server turns online, schedule a task that will run forever. Every 'TICK_INTERVAL' it will randomly
 select one message from 'MESSAGES', filter all players online with 'FILTER', and send all the
 remaining players the message.
*/
intercept[ServerLaunchEvent] { (msg, plr) =>
  world.scheduleForever(TICK_INTERVAL) {
    world.getPlayers.
      filter(FILTER).
      foreach(_.sendMessage(MESSAGES.randomElement))
  }
}