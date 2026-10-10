package io.icker.factions.mixin;

import io.icker.factions.api.persistents.User;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    // Port note: the original Fabric mod had extra branches here for the "styledchat"
    // mod (Fabric-only, no NeoForge build for 26.1), which have been removed. The
    // redirect target method is private in 26.1.2, but the descriptor is identical
    // to the one used against 26.3.
    @Redirect(
            method =
                    "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
    public void broadcastChatMessage(
            ServerPlayer player,
            OutgoingChatMessage message,
            boolean bl,
            ChatType.Bound parameters) {
        if (message instanceof OutgoingChatMessage.Disguised) {
            player.sendChatMessage(message, bl, parameters);
            return;
        }

        User sender = User.get(((OutgoingChatMessage.Player) message).message().link().sender());

        User target = User.get(player.getUUID());

        if (sender.chat == User.ChatMode.GLOBAL && target.chat != User.ChatMode.FOCUS) {
            player.sendChatMessage(message, bl, parameters);
        }

        if ((sender.chat == User.ChatMode.FACTION || sender.chat == User.ChatMode.FOCUS)
                && sender.getFaction().equals(target.getFaction())) {
            player.sendChatMessage(message, bl, parameters);
        }
    }
}
