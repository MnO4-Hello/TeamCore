package com.agen.teamcore.mixin;

import com.agen.teamcore.data.Team;
import com.agen.teamcore.manager.TeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 重生点Mixin - 玩家重生时传送到队伍出生点
 */
@Mixin(PlayerList.class)
public abstract class RespawnMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    private void teamcore$onRespawn(ServerPlayer oldPlayer, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer newPlayer = cir.getReturnValue();
        if (newPlayer == null) return;

        TeamManager mgr = TeamManager.getInstance();
        String playerName = newPlayer.getName().getString();
        Team team = mgr.getData().getPlayerTeam(playerName);

        if (team != null && team.hasSpawn) {
            // 传送到队伍出生点
            newPlayer.teleportTo(team.spawnX, team.spawnY, team.spawnZ);
        }
    }
}
