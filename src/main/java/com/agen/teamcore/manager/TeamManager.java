package com.agen.teamcore.manager;

import com.agen.teamcore.TeamCore;
import com.agen.teamcore.data.Team;
import com.agen.teamcore.data.TeamData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 队伍数据管理器 - 负责数据的加载、保存和业务逻辑
 */
public class TeamManager {

    private static final TeamManager INSTANCE = new TeamManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private TeamData data = new TeamData();
    private MinecraftServer server;
    private Path dataFile;

    // 客户端缓存
    private TeamData clientData = new TeamData();
    private boolean clientIsAdmin = false;

    // 旗帜定时移除队列: UUID -> [expireTick, teamName]
    private final Map<UUID, long[]> pendingBannerRemovals = new HashMap<>();
    private final Map<UUID, String> pendingBannerTeamNames = new HashMap<>();

    private TeamManager() {}

    public static TeamManager getInstance() {
        return INSTANCE;
    }

    // ============ 服务端方法 ============

    public void onServerStart(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.getWorldPath(LevelResource.ROOT).resolve("teamcore_data.json");
        load();
    }

    public void onServerStop(MinecraftServer server) {
        save();
        this.server = null;
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) {
            data = new TeamData();
            return;
        }
        try {
            String json = Files.readString(dataFile);
            TeamData loaded = GSON.fromJson(json, TeamData.class);
            if (loaded != null) {
                data = loaded;
            } else {
                data = new TeamData();
            }
        } catch (IOException e) {
            TeamCore.LOGGER.error("Failed to load team data", e);
            data = new TeamData();
        }
    }

    public void save() {
        if (dataFile == null) return;
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, GSON.toJson(data));
        } catch (IOException e) {
            TeamCore.LOGGER.error("Failed to save team data", e);
        }
    }

    public TeamData getData() {
        // 客户端没有server时使用clientData
        if (server == null) return clientData;
        return data;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /** 重新分配队伍 */
    public void redistributeTeams() {
        if (server == null) return;

        List<String> allPlayers = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            allPlayers.add(player.getName().getString());
        }

        data.redistribute(allPlayers);
        save();
    }

    /** 记录玩家死亡 */
    public void recordDeath(String playerName) {
        data.recordDeath(playerName);
        save();
    }

    /** 检查玩家是否为管理员 */
    public boolean isAdmin(String playerName) {
        if (server == null) return false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getName().getString().equals(playerName)) {
                return player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
            }
        }
        return false;
    }

    /** 检查两个玩家是否在同一队伍 */
    public boolean areInSameTeam(String player1, String player2) {
        Team team1 = data.getPlayerTeam(player1);
        Team team2 = data.getPlayerTeam(player2);
        if (team1 == null || team2 == null) return false;
        return team1.name.equals(team2.name);
    }

    /** 获取所有已知玩家名 (在线+死亡统计+队伍中) */
    public List<String> getAllKnownPlayerNames() {
        Set<String> names = new LinkedHashSet<>();
        // 在线玩家
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                names.add(p.getName().getString());
            }
        }
        // 从死亡统计中获取
        TeamData d = getData();
        names.addAll(d.deathStats.keySet());
        // 从队伍中获取
        for (com.agen.teamcore.data.Team t : d.teams) {
            names.addAll(t.players);
        }
        return new ArrayList<>(names);
    }

    // ============ 客户端方法 ============

    public void setClientData(TeamData data) {
        this.clientData = data;
    }

    public TeamData getClientData() {
        return clientData;
    }

    public void setClientAdmin(boolean isAdmin) {
        this.clientIsAdmin = isAdmin;
    }

    public boolean isClientAdmin() {
        return clientIsAdmin;
    }

    // ============ 旗帜定时移除 ============

    /** 添加一个待移除的旗帜任务 */
    public void scheduleBannerRemoval(UUID playerUuid, String teamName, int expireTick) {
        pendingBannerRemovals.put(playerUuid, new long[]{expireTick});
        pendingBannerTeamNames.put(playerUuid, teamName);
    }

    /** 每tick检查并处理过期的旗帜移除 */
    public void tickBannerRemovals(MinecraftServer server) {
        if (pendingBannerRemovals.isEmpty()) return;
        int currentTick = server.getTickCount();
        Iterator<Map.Entry<UUID, long[]>> it = pendingBannerRemovals.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, long[]> entry = it.next();
            if (currentTick >= entry.getValue()[0]) {
                UUID uuid = entry.getKey();
                String teamName = pendingBannerTeamNames.remove(uuid);
                it.remove();
                // 查找玩家并移除旗帜
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null && !player.isRemoved() && teamName != null) {
                    removeBannerFromPlayer(player, teamName);
                }
            }
        }
    }

    /** 从玩家背包移除指定队伍名称的旗帜 */
    private void removeBannerFromPlayer(ServerPlayer player, String teamName) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BannerItem && stack.has(DataComponents.CUSTOM_NAME)) {
                Component name = stack.get(DataComponents.CUSTOM_NAME);
                if (name != null && name.getString().contains(teamName)) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.sendSystemMessage(Component.literal("§7旗帜已消失"));
                    return;
                }
            }
        }
    }
}
