package com.me4502.sexystuff;

import com.me4502.modularframework.ModuleController;
import com.me4502.modularframework.ShadedModularFramework;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "sexystuff", name = "SexyStuff")
public class SexyStuff {

    ModuleController moduleController;

    public static SexyStuff plugin;
    public static Game game;

    @Listener
    public void onInitialize(GameStartingServerEvent event) {

        this.plugin = this;
        this.game = event.getGame();

        //Initialize the sexiness
        moduleController = ShadedModularFramework.registerModuleController(this, event.getGame());

        discoverModules();

        moduleController.enableModules();
    }

    public void discoverModules() {

        moduleController.registerModule("com.me4502.sexystuff.modules.MagicCarpet");
    }
}
