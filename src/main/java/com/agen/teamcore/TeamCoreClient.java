package com.agen.teamcore;

import com.agen.teamcore.client.gui.TeamConfigScreen;
import com.agen.teamcore.network.Networking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public class TeamCoreClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册客户端网络接收器
        Networking.registerClientReceivers();

        // 检测右键书本打开GUI (通过客户端tick检测)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand = client.player.getOffhandItem();

            if (isTeamBook(mainHand) || isTeamBook(offHand)) {
                // 检测右键按下
                if (client.options.keyUse.isDown() && client.screen == null) {
                    client.setScreen(new TeamConfigScreen());
                }
            }
        });

        TeamCore.LOGGER.info("TeamCore Client initialized!");
    }

    private boolean isTeamBook(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == TeamCore.TEAM_BOOK;
    }
}
