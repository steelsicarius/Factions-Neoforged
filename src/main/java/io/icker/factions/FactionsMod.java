package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.icker.factions.command.AdminCommand;
import io.icker.factions.command.InfoCommand;
import io.icker.factions.command.ListCommand;
import io.icker.factions.command.MemberCommand;
import io.icker.factions.command.ModifyCommand;
import io.icker.factions.command.ClaimCommand;
import io.icker.factions.command.CreateCommand;
import io.icker.factions.command.DeclareCommand;
import io.icker.factions.command.DisbandCommand;
import io.icker.factions.command.HomeCommand;
import io.icker.factions.command.InviteCommand;
import io.icker.factions.command.JoinCommand;
import io.icker.factions.command.KickCommand;
import io.icker.factions.command.LeaveCommand;
import io.icker.factions.command.MapCommand;
import io.icker.factions.command.PermissionCommand;
import io.icker.factions.command.RankCommand;
import io.icker.factions.command.SafeCommand;
import io.icker.factions.command.SettingsCommand;
import io.icker.factions.config.Config;
import io.icker.factions.core.ChatManager;
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.InteractionManager;
import io.icker.factions.core.ServerManager;
import io.icker.factions.core.SoundManager;
import io.icker.factions.core.WorldManager;
import io.icker.factions.util.BlueMapWrapper;
import io.icker.factions.util.Command;
import io.icker.factions.util.Permissions;
import io.icker.factions.util.SquareMapWrapper;
import io.icker.factions.util.TabWrapper;
import io.icker.factions.util.PlaceholdersWrapper;
import io.icker.factions.util.WorldUtils;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FactionsMod.MODID)
public class FactionsMod {
    public static final String MODID = "factions";
    public static Logger LOGGER = LoggerFactory.getLogger("Factions");

    public static Config CONFIG = Config.load();

    // Port note: the Fabric-only dynmap integration was removed (dynmap has no NeoForge
    // build for 26.1; FactionsMod.dynmap / DynmapWrapper / us.dynmap:DynmapCoreAPI never
    // existed in this port). Restore from the original mod if dynmap ever ships
    // a NeoForge build. BlueMap and squaremap are wired below.
    public static BlueMapWrapper bluemap;
    public static SquareMapWrapper squaremap;

    public FactionsMod() {
        LOGGER.info("Initialized Factions Mod");

        WorldUtils.register();

        // NeoForge requires permission nodes to be registered before the first query
        // (see Permissions.gatherNodes); this must be registered before server start.
        NeoForge.EVENT_BUS.addListener(Permissions::gatherNodes);

        // Fabric original: FabricLoader...isModLoaded("bluemap") / ("squaremap")
        bluemap = ModList.get().isLoaded("bluemap") ? new BlueMapWrapper() : null;
        squaremap = ModList.get().isLoaded("squaremap") ? new SquareMapWrapper() : null;

        // Fabric original: FabricLoader...isModLoaded("placeholder-api"). Multiple ports of
        // Patbox's Text Placeholder API exist for NeoForge with different mod ids:
        // "placeholder-api-neoforge" (Modrinth, OffsetMonkey538) and "placeholderapi"
        // (github.com/downloadableduck/textplaceholderapi-neoforge) — all of them expose the
        // same eu.pb4.placeholders API, so any of them works.
        if (ModList.get().isLoaded("placeholder-api")
                || ModList.get().isLoaded("placeholder-api-neoforge")
                || ModList.get().isLoaded("placeholderapi")) {
            PlaceholdersWrapper.init();
            LOGGER.info("Text Placeholder API detected; factions placeholders registered");
        }

        // TAB's NeoForge platform has no PlaceholderAPI hook (only Fabric/old Forge do),
        // so its public API is used to register the placeholders directly. TAB's API
        // instance is null during mod construction ("API was called before TAB was
        // loaded", crashes the boot), so the registration is deferred to server start.
        NeoForge.EVENT_BUS.addListener(FactionsMod::serverAboutToStart);
        ChatManager.register();
        InteractionManager.register();
        FactionsManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();

        // Fabric original: CommandRegistrationCallback.EVENT
        NeoForge.EVENT_BUS.addListener(FactionsMod::registerCommands);
    }

    private static void serverAboutToStart(ServerAboutToStartEvent event) {
        if (ModList.get().isLoaded("tab")) {
            new TabWrapper();
            LOGGER.info("TAB detected; factions placeholders registered into TAB");
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext registryAccess = event.getBuildContext();

        LiteralCommandNode<CommandSourceStack> factions = Commands.literal("factions").build();

        LiteralCommandNode<CommandSourceStack> alias = Commands.literal("f").build();

        dispatcher.getRoot().addChild(factions);
        dispatcher.getRoot().addChild(alias);

        Command[] commands =
                new Command[] {
                    new AdminCommand(),
                    new ClaimCommand(),
                    new CreateCommand(),
                    new DeclareCommand(),
                    new DisbandCommand(),
                    new HomeCommand(),
                    new InfoCommand(),
                    new InviteCommand(),
                    new JoinCommand(),
                    new KickCommand(),
                    new LeaveCommand(),
                    new ListCommand(),
                    new MapCommand(),
                    new MemberCommand(),
                    new ModifyCommand(),
                    new PermissionCommand(),
                    new RankCommand(),
                    new SafeCommand(),
                    new SettingsCommand()
                };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
