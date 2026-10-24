package com.agen.teamcore.item;

import com.agen.teamcore.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 抽取队伍物品 - 右键随机/均分分配到队伍，获得对应颜色旗帜(5秒后消失)
 */
public class TeamDrawItem extends Item {

    public TeamDrawItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ClientPlayNetworking.send(new Networking.DrawTeamC2SPacket());
        }
        return InteractionResult.SUCCESS;
    }
}
