package org.smeshikMods.macedamageui;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Macedamageui implements ModInitializer {
    public static final String MOD_ID = "macedamageui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Mace Damage UI Mod initialized");
    }
}