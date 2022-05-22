package com.willfp.libreforge.effects.effects

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.effects.effects.particles.DirectionVector
import com.willfp.libreforge.effects.effects.particles.ParticleAnimations
import com.willfp.libreforge.effects.effects.particles.copy
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.joml.Vector3f

class EffectParticleAnimation : Effect(
    "particle_animation",
    applicableTriggers = Triggers.withParameters(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION
    )
) {
    override fun handle(data: TriggerData, config: Config) {
        val location = data.location ?: return
        val player = data.player ?: return

        val entity = when (config.getString("entity").lowercase()) {
            "victim" -> data.victim
            "projectile" -> data.projectile
            else -> data.player
        } ?: return

        val world = location.world ?: return

        val animation = ParticleAnimations.getByID(config.getString("animation")) ?: return
        val particle = Particle.valueOf(config.getString("particle").uppercase())

        var tick = 0

        val args = config.getSubsection("particle_args")

        plugin.runnableFactory.create {
            val entityVector = Vector3f(
                entity.location.x.toFloat(),
                if (animation.useEyeLocation && entity is LivingEntity) entity.eyeLocation.y.toFloat() else entity.location.y.toFloat(),
                entity.location.z.toFloat()
            )

            val entityDirectionVector = DirectionVector(
                entity.location.yaw,
                entity.location.pitch
            )

            val locationVector = Vector3f(
                location.x.toFloat(),
                location.y.toFloat(),
                location.z.toFloat()
            )

            val vectors = if (args.has("tick-multiplier")) {
                val mult = args.getIntFromExpression("tick-multiplier", player)
                val vectors = mutableSetOf<Vector3f>()

                for (t in (tick * mult until (tick * mult) + mult)) {
                    vectors.addAll(
                        animation.getParticleLocations(
                            t,
                            entityVector.copy(),
                            entityDirectionVector.copy(),
                            locationVector.copy(),
                            args,
                            player
                        )
                    )
                }

                vectors
            } else {
                animation.getParticleLocations(
                    tick,
                    entityVector.copy(),
                    entityDirectionVector.copy(),
                    locationVector.copy(),
                    args,
                    player
                )
            }

            for (vector in vectors) {
                world.spawnParticle(
                    particle,
                    Location(
                        world,
                        vector.x.toDouble(),
                        vector.y.toDouble(),
                        vector.z.toDouble()
                    ),
                    config.getIntFromExpression("particle-amount", player),
                    0.0, 0.0, 0.0, 0.0, null
                )
            }

            if (vectors.any { v ->
                    animation.shouldStopTicking(
                        tick,
                        entityVector.copy(),
                        entityDirectionVector.copy(),
                        locationVector.copy(),
                        v,
                        args,
                        player
                    )
                }) {
                it.cancel()
            }

            tick++
        }.runTaskTimerAsynchronously(1, 1)
    }

    override fun validateConfig(config: Config): List<ConfigViolation> {
        val violations = mutableListOf<ConfigViolation>()

        if (!config.has("particle")) violations.add(
            ConfigViolation(
                "particle",
                "You must specify the particle!"
            )
        )

        if (!config.has("animation")) violations.add(
            ConfigViolation(
                "animation",
                "You must specify the animation!"
            )
        )

        val animation = ParticleAnimations.getByID(config.getString("animation"))

        if (animation == null) {
            violations.add(
                ConfigViolation(
                    "animation",
                    "Invalid animation!"
                )
            )
        } else {
            violations.addAll(animation.validateConfig(config.getSubsection("particle_args")).map {
                it.copy(param = "particle_args.${it.param}")
            })
        }

        return violations
    }
}
