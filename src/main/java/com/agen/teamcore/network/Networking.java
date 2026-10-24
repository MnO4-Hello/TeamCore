package com.agen.teamcore.network;

import com.agen.teamcore.TeamCore;
import com.agen.teamcore.data.Team;
import com.agen.teamcore.data.TeamData;
import com.agen.teamcore.manager.TeamManager;
import com.agen.teamcore.client.gui.TeamConfigScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

/**
 * 网络通信管理 - 处理客户端与服务端之间的数据同步
 */
public class Networking {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ============ C2S 数据包 (客户端 -> 服务端) ============

    /** 更新分组数量 */
    public record UpdateTeamCountC2SPacket(int teamCount) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdateTeamCountC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "update_team_count"));
        public static final StreamCodec<FriendlyByteBuf, UpdateTeamCountC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> buf.writeInt(packet.teamCount),
                        buf -> new UpdateTeamCountC2SPacket(buf.readInt()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 更新分配模式 */
    public record UpdateModeC2SPacket(int mode) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdateModeC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "update_mode"));
        public static final StreamCodec<FriendlyByteBuf, UpdateModeC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> buf.writeInt(packet.mode),
                        buf -> new UpdateModeC2SPacket(buf.readInt()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 更新PvP开关 */
    public record UpdatePvPC2SPacket(boolean enabled) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdatePvPC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "update_pvp"));
        public static final StreamCodec<FriendlyByteBuf, UpdatePvPC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> buf.writeBoolean(packet.enabled),
                        buf -> new UpdatePvPC2SPacket(buf.readBoolean()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 执行重新分配 */
    public record RedistributeC2SPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RedistributeC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "redistribute"));
        public static final StreamCodec<FriendlyByteBuf, RedistributeC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> {}, buf -> new RedistributeC2SPacket());
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 添加玩家到队伍 */
    public record AddPlayerC2SPacket(String playerName, int teamIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AddPlayerC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "add_player"));
        public static final StreamCodec<FriendlyByteBuf, AddPlayerC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> { buf.writeUtf(packet.playerName); buf.writeInt(packet.teamIndex); },
                        buf -> new AddPlayerC2SPacket(buf.readUtf(), buf.readInt()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 从队伍移除玩家 */
    public record RemovePlayerC2SPacket(String playerName, int teamIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemovePlayerC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "remove_player"));
        public static final StreamCodec<FriendlyByteBuf, RemovePlayerC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> { buf.writeUtf(packet.playerName); buf.writeInt(packet.teamIndex); },
                        buf -> new RemovePlayerC2SPacket(buf.readUtf(), buf.readInt()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 更新队伍颜色 */
    public record UpdateColorC2SPacket(int teamIndex, String color) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdateColorC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "update_color"));
        public static final StreamCodec<FriendlyByteBuf, UpdateColorC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> { buf.writeInt(packet.teamIndex); buf.writeUtf(packet.color); },
                        buf -> new UpdateColorC2SPacket(buf.readInt(), buf.readUtf()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 设置队伍出生点 */
    public record SetSpawnC2SPacket(int teamIndex, double x, double y, double z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetSpawnC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "set_spawn"));
        public static final StreamCodec<FriendlyByteBuf, SetSpawnC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> { buf.writeInt(packet.teamIndex); buf.writeDouble(packet.x); buf.writeDouble(packet.y); buf.writeDouble(packet.z); },
                        buf -> new SetSpawnC2SPacket(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 请求数据同步 */
    public record RequestDataC2SPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestDataC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "request_data"));
        public static final StreamCodec<FriendlyByteBuf, RequestDataC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> {}, buf -> new RequestDataC2SPacket());
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 抽取队伍 */
    public record DrawTeamC2SPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DrawTeamC2SPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "draw_team"));
        public static final StreamCodec<FriendlyByteBuf, DrawTeamC2SPacket> CODEC =
                StreamCodec.of((buf, packet) -> {}, buf -> new DrawTeamC2SPacket());
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ============ S2C 数据包 (服务端 -> 客户端) ============

    /** 同步完整队伍数据 */
    public record SyncDataS2CPacket(String jsonData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncDataS2CPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "sync_data"));
        public static final StreamCodec<FriendlyByteBuf, SyncDataS2CPacket> CODEC =
                StreamCodec.of((buf, packet) -> buf.writeUtf(packet.jsonData),
                        buf -> new SyncDataS2CPacket(buf.readUtf()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** 同步权限信息 */
    public record SyncPermissionS2CPacket(boolean isAdmin) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPermissionS2CPacket> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TeamCore.MOD_ID, "sync_permission"));
        public static final StreamCodec<FriendlyByteBuf, SyncPermissionS2CPacket> CODEC =
                StreamCodec.of((buf, packet) -> buf.writeBoolean(packet.isAdmin),
                        buf -> new SyncPermissionS2CPacket(buf.readBoolean()));
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ============ 注册方法 ============

    /** 注册C2S数据包类型 */
    public static void registerC2SPackets() {
        PayloadTypeRegistry.serverboundPlay().register(UpdateTeamCountC2SPacket.TYPE, UpdateTeamCountC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateModeC2SPacket.TYPE, UpdateModeC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdatePvPC2SPacket.TYPE, UpdatePvPC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RedistributeC2SPacket.TYPE, RedistributeC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AddPlayerC2SPacket.TYPE, AddPlayerC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RemovePlayerC2SPacket.TYPE, RemovePlayerC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateColorC2SPacket.TYPE, UpdateColorC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetSpawnC2SPacket.TYPE, SetSpawnC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestDataC2SPacket.TYPE, RequestDataC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DrawTeamC2SPacket.TYPE, DrawTeamC2SPacket.CODEC);
    }

    /** 注册S2C数据包类型 */
    public static void registerS2CPackets() {
        PayloadTypeRegistry.clientboundPlay().register(SyncDataS2CPacket.TYPE, SyncDataS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncPermissionS2CPacket.TYPE, SyncPermissionS2CPacket.CODEC);
    }

    /** 注册服务端接收器 */
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(UpdateTeamCountC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.getData().teamCount = payload.teamCount();
                mgr.save();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdateModeC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.getData().distributionMode = payload.mode();
                mgr.save();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdatePvPC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.getData().pvpEnabled = payload.enabled();
                mgr.save();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RedistributeC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.redistributeTeams();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(AddPlayerC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.getData().addPlayerToTeam(payload.playerName(), payload.teamIndex());
                mgr.save();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RemovePlayerC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                mgr.getData().removePlayerFromTeam(payload.playerName(), payload.teamIndex());
                mgr.save();
                broadcastData();
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(UpdateColorC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                Team team = mgr.getData().getTeam(payload.teamIndex());
                if (team != null) {
                    team.colorCode = payload.color();
                    mgr.save();
                    broadcastData();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetSpawnC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            if (!isAdmin(player)) return;
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                Team team = mgr.getData().getTeam(payload.teamIndex());
                if (team != null) {
                    team.setSpawn(payload.x(), payload.y(), payload.z());
                    mgr.save();
                    broadcastData();
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestDataC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                // 发送数据给请求的玩家
                TeamManager mgr = TeamManager.getInstance();
                String json = GSON.toJson(mgr.getData());
                ServerPlayNetworking.send(player, new SyncDataS2CPacket(json));
                // 发送权限信息
                boolean admin = isAdmin(player);
                ServerPlayNetworking.send(player, new SyncPermissionS2CPacket(admin));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DrawTeamC2SPacket.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                TeamManager mgr = TeamManager.getInstance();
                TeamData data = mgr.getData();
                if (data.teams.isEmpty()) return;

                String playerName = player.getName().getString();

                // 先从所有队伍移除该玩家
                for (Team t : data.teams) t.players.remove(playerName);

                Team assignedTeam;
                if (data.distributionMode == 0) {
                    // 均分模式: 分配到人数最少的队伍
                    assignedTeam = data.teams.stream()
                            .min((a, b) -> Integer.compare(a.players.size(), b.players.size()))
                            .orElse(data.teams.get(0));
                } else {
                    // 随机模式: 随机选择一个队伍
                    assignedTeam = data.teams.get(player.getRandom().nextInt(data.teams.size()));
                }

                // 添加玩家到队伍
                assignedTeam.players.add(playerName);
                mgr.save();
                broadcastData();

                // 移除玩家手中的抽取队伍物品
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() == TeamCore.TEAM_DRAW) {
                    mainHand.shrink(1);
                } else {
                    ItemStack offHand = player.getOffhandItem();
                    if (offHand.getItem() == TeamCore.TEAM_DRAW) {
                        offHand.shrink(1);
                    }
                }

                // 给予对应颜色的旗帜
                DyeColor dyeColor = getDyeColor(assignedTeam.colorCode);
                Item bannerItem = getBannerItem(dyeColor);
                ItemStack banner = new ItemStack(bannerItem);
                banner.set(DataComponents.CUSTOM_NAME,
                        Component.literal(assignedTeam.colorCode + assignedTeam.name));
                player.getInventory().add(banner);

                // 发送提示消息
                player.sendSystemMessage(Component.literal(
                        assignedTeam.colorCode + "你被分配到了 " + assignedTeam.name + "!"));

                // 5秒后自动移除旗帜 (100 ticks)
                final String teamName = assignedTeam.name;
                mgr.scheduleBannerRemoval(player.getUUID(), teamName,
                        ctx.server().getTickCount() + 100);
            });
        });
    }

    /** 注册客户端接收器 */
    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SyncDataS2CPacket.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                TeamData data = GSON.fromJson(payload.jsonData(), TeamData.class);
                if (data != null) {
                    TeamManager.getInstance().setClientData(data);
                }
                // 如果当前打开了队伍配置界面，刷新它
                if (ctx.client().screen instanceof TeamConfigScreen teamScreen) {
                    teamScreen.refresh();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncPermissionS2CPacket.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> {
                TeamManager.getInstance().setClientAdmin(payload.isAdmin());
            });
        });
    }

    /** 广播数据给所有在线玩家 */
    public static void broadcastData() {
        TeamManager mgr = TeamManager.getInstance();
        if (mgr.getServer() == null) return;
        String json = GSON.toJson(mgr.getData());
        for (ServerPlayer player : mgr.getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, new SyncDataS2CPacket(json));
            ServerPlayNetworking.send(player, new SyncPermissionS2CPacket(isAdmin(player)));
        }
    }

    /** 检查玩家是否为管理员 */
    private static boolean isAdmin(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }

    /** 将队伍颜色代码转换为DyeColor */
    private static DyeColor getDyeColor(String colorCode) {
        return switch (colorCode) {
            case "§c" -> DyeColor.RED;
            case "§9" -> DyeColor.BLUE;
            case "§a" -> DyeColor.LIME;
            case "§e" -> DyeColor.YELLOW;
            case "§d" -> DyeColor.PINK;
            case "§b" -> DyeColor.LIGHT_BLUE;
            case "§6" -> DyeColor.ORANGE;
            case "§f" -> DyeColor.WHITE;
            case "§4" -> DyeColor.RED;
            case "§1" -> DyeColor.BLUE;
            case "§2" -> DyeColor.GREEN;
            case "§5" -> DyeColor.PURPLE;
            case "§3" -> DyeColor.CYAN;
            case "§7" -> DyeColor.LIGHT_GRAY;
            case "§8" -> DyeColor.GRAY;
            case "§0" -> DyeColor.BLACK;
            default -> DyeColor.WHITE;
        };
    }

    /** 根据DyeColor获取对应旗帜物品 */
    private static Item getBannerItem(DyeColor color) {
        return switch (color) {
            case RED -> Items.RED_BANNER;
            case BLUE -> Items.BLUE_BANNER;
            case LIME -> Items.LIME_BANNER;
            case YELLOW -> Items.YELLOW_BANNER;
            case PINK -> Items.PINK_BANNER;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_BANNER;
            case ORANGE -> Items.ORANGE_BANNER;
            case WHITE -> Items.WHITE_BANNER;
            case GREEN -> Items.GREEN_BANNER;
            case PURPLE -> Items.PURPLE_BANNER;
            case CYAN -> Items.CYAN_BANNER;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_BANNER;
            case GRAY -> Items.GRAY_BANNER;
            case BLACK -> Items.BLACK_BANNER;
            case MAGENTA -> Items.MAGENTA_BANNER;
            case BROWN -> Items.BROWN_BANNER;
        };
    }
}
