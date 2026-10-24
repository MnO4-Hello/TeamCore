package com.agen.teamcore.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;

import java.util.function.Consumer;

/**
 * 分组队伍配置书本物品
 */
public class TeamBookItem extends Item {

    public TeamBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag) {
        tooltipAdder.accept(Component.literal("右键打开队伍配置界面").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.literal("管理员可编辑，普通玩家仅查看").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
