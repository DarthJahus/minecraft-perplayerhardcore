package net.jahus.perplayerhardcore;

import net.jahus.perplayerhardcore.command.HardcoreCommand;
import net.jahus.perplayerhardcore.config.ModConfig;
import net.jahus.perplayerhardcore.data.HardcoreAttachments;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerPlayerHardcore implements ModInitializer {
    public static final String MOD_ID = "perplayerhardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("PerPlayerHardcore initializing...");

        HardcoreAttachments.register();
        ModConfig.load();
        CommandRegistrationCallback.EVENT.register(HardcoreCommand::register);

        LOGGER.info("PerPlayerHardcore loaded successfully.");
    }
}
