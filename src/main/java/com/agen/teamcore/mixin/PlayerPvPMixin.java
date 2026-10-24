package com.agen.teamcore.mixin;

import com.agen.teamcore.manager.TeamManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PvP控制Mixin - 当队伍PvP关闭时，阻止同队玩家互相伤害
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerPvPMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void teamcore$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        TeamManager mgr = TeamManager.getInstance();
        // 如果PvP已开启，不做限制
        if (mgr.getData().pvpEnabled) return;

        // 检查伤害来源是否为其他玩家
        if (source.getEntity() instanceof ServerPlayer attacker) {
            ServerPlayer victim = (ServerPlayer) (Object) this;
            String attackerName = attacker.getName().getString();
            String victimName = victim.getName().getString();

            // 如果攻击者和受害者在同一队伍，取消伤害
            if (mgr.areInSameTeam(attackerName, victimName)) {
                cir.setReturnValue(false);
            }
        }
    }
}
