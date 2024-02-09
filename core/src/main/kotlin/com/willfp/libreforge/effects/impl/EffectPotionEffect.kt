package com.willfp.libreforge.effects.impl

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.plugin
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import com.willfp.libreforge.triggers.run
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object EffectPotionEffect : Effect<NoCompileData>("potion_effect") {
    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "effect",
            "You must specify a valid potion effect! See here: " +
                    "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/potion/PotionEffectType.html",
            Config::getString
        ) {
            PotionEffectType.getByName(it.uppercase()) != null
        }
        require("level", "You must specify the effect level!")
        require("duration", "You must specify the duration!")
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val toApply = if (config.getBoolOrNull("apply_to_player") == true) {
            data.player ?: return false
        } else {
            data.victim ?: return false
        }

        plugin.scheduler.run(toApply) {
            toApply.addPotionEffect(
                PotionEffect(
                    PotionEffectType.getByName(config.getString("effect").uppercase())
                        ?: PotionEffectType.INCREASE_DAMAGE,
                    config.getIntFromExpression("duration", data),
                    config.getIntFromExpression("level", data) - 1,
                    true,
                    true,
                    true
                )
            )
        }

        return true
    }
}
