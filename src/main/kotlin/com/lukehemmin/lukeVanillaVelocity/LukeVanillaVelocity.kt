package com.lukehemmin.lukeVanillaVelocity;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger

@Plugin(
    id = "lukevanilla-velocity", name = "LukeVanilla-Velocity", version = BuildConstants.VERSION
)
class LukeVanillaVelocity @Inject constructor(val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
    }
}
