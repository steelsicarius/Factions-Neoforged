package io.icker.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

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
import io.icker.factions.core.FactionsManager;
import io.icker.factions.core.ServerManager;
import io.icker.factions.core.SoundManager;
import io.icker.factions.core.WorldManager;
import io.icker.factions.util.Command;
import io.icker.factions.util.WorldUtils;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(FactionsMod.MODID)
public class FactionsMod {
    public static final String MODID = "factions";
    public static Logger LOGGER = LoggerFactory.getLogger("Factions");

    public static Config CONFIG = Config.load();

    // TODO (Fase 5): dynmap/bluemap/squaremap wrappers + PlaceholdersWrapper init.
    // TODO (Fase 4): AdminCommand, InfoCommand, MemberCommand, ModifyCommand (GUI/sgui).

    public FactionsMod() {
        LOGGER.info("Initialized Factions Mod");

        WorldUtils.register();
        // TODO (Fase 2): ChatManager.register() — needs ServerChatEvent + PlayerListMixin.
        // TODO (Fase 2): InteractionManager.register() — needs NeoForge interaction events.
        FactionsManager.register();
        ServerManager.register();
        SoundManager.register();
        WorldManager.register();

        // Fabric original: CommandRegistrationCallback.EVENT
        NeoForge.EVENT_BUS.addListener(FactionsMod::registerCommands);
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
                    new ClaimCommand(),
                    new CreateCommand(),
                    new DeclareCommand(),
                    new DisbandCommand(),
                    new HomeCommand(),
                    new InviteCommand(),
                    new JoinCommand(),
                    new KickCommand(),
                    new LeaveCommand(),
                    new MapCommand(),
                    new PermissionCommand(),
                    new RankCommand(),
                    new SafeCommand(),
                    new SettingsCommand(),
                    // TODO (Fase 4): new AdminCommand(), new InfoCommand(),
                    //                 new MemberCommand(), new ModifyCommand() (sgui).
                    // TODO (Fase 4): new ListCommand() (sgui).
                };

        for (Command command : commands) {
            factions.addChild(command.getNode());
            alias.addChild(command.getNode());
        }
    }
}
