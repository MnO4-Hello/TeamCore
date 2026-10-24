package com.agen.teamcore.mixin;

import com.agen.teamcore.manager.TeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡统计Mixin - 记录玩家死亡次数
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void teamcore$onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        String playerName = player.getName().getString();
        TeamManager.getInstance().recordDeath(playerName);
    }
}
