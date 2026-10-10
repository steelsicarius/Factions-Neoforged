package io.icker.factions;

import io.icker.factions.config.Config;

import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FactionsMod.MODID)
public class FactionsMod {
    public static final String MODID = "factions";
    public static Logger LOGGER = LoggerFactory.getLogger("Factions");

    public static Config CONFIG = Config.load();

    public FactionsMod() {
        LOGGER.info("Initialized Factions Mod");
    }
}
