package app.simplecloud.controller.runtime.reconciler.config

import app.simplecloud.controller.runtime.reconciler.type.ReconcilerType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

/**
 * allowStart: true
 * allowStop: true
 * allowStopNonEmptyServer: true
 * inactiveTimeUntilStop: 5s 5m 1h
 *
 *
 * min (available) - max (alles alles):
 * - wenn ingame dann starte neuen server
 *
 * type: PLAYER_RATIO, SLOTS_TO_SLOTS, SERVER_TO_SERVER
 * PLAYER_RATIO
 * - checkt die player ratio ab
 * SLOTS_TO_SLOTS
 * - checkt ob spieler auf slots passen
 * SERVER_TO_SERVER
 * - checkt wie viele wirklich available sein sollen
 *
 * playerRatio: 50 //required when PLAYER_RATIO
 * function: "log(x+1)+3" //required when SLOTS_TO_SLOTS or SERVER_TO_SERVER
 */

@ConfigSerializable
data class ReconcilerConfig(
    val name: String = "",
    val allowStart: Boolean = true,
    val allowStop: Boolean = true,
    val allowStopNonEmptyServer: Boolean = false,
    val inactiveTimeUntilStop: String = "5m",
    val type: ReconcilerType = ReconcilerType.NONE,
    val function: String? = null,
    val playerRatio: Int? = null
)