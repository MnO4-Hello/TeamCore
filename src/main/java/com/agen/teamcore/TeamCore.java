package com.agen.teamcore;

import com.agen.teamcore.item.TeamBookItem;
import com.agen.teamcore.item.TeamDrawItem;
import com.agen.teamcore.manager.TeamManager;
import com.agen.teamcore.network.Networking;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamCore implements ModInitializer {

    public static final String MOD_ID = "teamcore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** 队伍配置书本物品 */
    public static final ResourceKey<Item> TEAM_BOOK_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "team_book"));
    public static final Item TEAM_BOOK = new TeamBookItem(
            new Item.Properties().setId(TEAM_BOOK_KEY).stacksTo(1));

    /** 抽取队伍物品 */
    public static final ResourceKey<Item> TEAM_DRAW_KEY =
            ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "team_draw"));
    public static final Item TEAM_DRAW = new TeamDrawItem(
            new Item.Properties().setId(TEAM_DRAW_KEY).stacksTo(1));

    @Override
    public void onInitialize() {
        // 注册物品
        Registry.register(BuiltInRegistries.ITEM, TEAM_BOOK_KEY, TEAM_BOOK);
        Registry.register(BuiltInRegistries.ITEM, TEAM_DRAW_KEY, TEAM_DRAW);

        // 注册网络包
        Networking.registerC2SPackets();
        Networking.registerS2CPackets();
        Networking.registerServerReceivers();

        // 服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TeamManager.getInstance().onServerStart(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            TeamManager.getInstance().onServerStop(server);
        });

        // 每tick检查旗帜定时移除
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TeamManager.getInstance().tickBannerRemovals(server);
        });

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });

        LOGGER.info("TeamCore Mod initialized! Author: AGen");
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("teamcore")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                // 给予书本
                .then(Commands.literal("give")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            giveBook(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("§a已获得分组队伍配置书本"), false);
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    giveBook(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已给予 " + target.getName().getString() + " 分组队伍配置书本"), false);
                                    return 1;
                                })
                        )
                )
                // 给予抽取队伍物品
                .then(Commands.literal("givedraw")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            giveDrawItem(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("§a已获得抽取队伍物品"), false);
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    giveDrawItem(target);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a已给予 " + target.getName().getString() + " 抽取队伍物品"), false);
                                    return 1;
                                })
                        )
                )
                // 重新加载数据
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            TeamManager.getInstance().load();
                            Networking.broadcastData();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a队伍数据已重新加载"), false);
                            return 1;
                        })
                )
                // 保存数据
                .then(Commands.literal("save")
                        .executes(ctx -> {
                            TeamManager.getInstance().save();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a队伍数据已保存"), false);
                            return 1;
                        })
                )
        );
    }

    private void giveBook(ServerPlayer player) {
        ItemStack stack = new ItemStack(TEAM_BOOK);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l分组队伍配置"));
        player.getInventory().add(stack);
    }

    private void giveDrawItem(ServerPlayer player) {
        ItemStack stack = new ItemStack(TEAM_DRAW);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§b§l抽取队伍"));
        player.getInventory().add(stack);
    }
}
