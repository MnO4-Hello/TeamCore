package com.agen.teamcore.data;

import java.util.*;
import java.util.Collections;

/**
 * 队伍数据模型 - 存储所有队伍配置信息
 */
public class TeamData {
    /** 分组数量 */
    public int teamCount = 2;
    /** 分配模式: 0=均分, 1=完全随机 */
    public int distributionMode = 0;
    /** 是否开启队伍PvP (true=队友可互相攻击) */
    public boolean pvpEnabled = false;
    /** 队伍列表 */
    public List<Team> teams = new ArrayList<>();
    /** 玩家死亡次数统计 (玩家名 -> 死亡数) */
    public Map<String, Integer> deathStats = new HashMap<>();

    public TeamData() {
        initTeams();
    }

    /** 根据队伍数量初始化队伍 */
    public void initTeams() {
        // 保留已有队伍的玩家数据
        List<Team> oldTeams = new ArrayList<>(teams);
        teams.clear();

        String[] defaultColors = {
            "§c", "§9", "§a", "§e", "§d", "§b", "§6", "§f",
            "§4", "§1", "§2", "§5", "§3", "§7", "§8", "§0"
        };
        String[] defaultNames = {
            "红队", "蓝队", "绿队", "黄队", "粉队", "青队", "橙队", "白队",
            "暗红队", "深蓝队", "暗绿队", "紫队", "湖蓝队", "灰队", "深灰队", "黑队"
        };

        for (int i = 0; i < teamCount; i++) {
            Team team = new Team();
            team.id = i;
            team.name = i < defaultNames.length ? defaultNames[i] : "队伍" + (i + 1);
            team.colorCode = i < defaultColors.length ? defaultColors[i] : "§f";
            // 尝试保留旧队伍的玩家
            if (i < oldTeams.size()) {
                team.players = new ArrayList<>(oldTeams.get(i).players);
                team.spawnX = oldTeams.get(i).spawnX;
                team.spawnY = oldTeams.get(i).spawnY;
                team.spawnZ = oldTeams.get(i).spawnZ;
                team.hasSpawn = oldTeams.get(i).hasSpawn;
            }
            teams.add(team);
        }
    }

    /** 获取玩家所在队伍，未分配返回null */
    public Team getPlayerTeam(String playerName) {
        for (Team team : teams) {
            if (team.players.contains(playerName)) {
                return team;
            }
        }
        return null;
    }

    /** 获取玩家所在队伍ID，未分配返回-1 */
    public int getPlayerTeamId(String playerName) {
        Team team = getPlayerTeam(playerName);
        return team != null ? team.id : -1;
    }

    /** 增加死亡统计 */
    public void addDeath(String playerName) {
        deathStats.put(playerName, deathStats.getOrDefault(playerName, 0) + 1);
    }

    /** 获取玩家死亡数 */
    public int getDeaths(String playerName) {
        return deathStats.getOrDefault(playerName, 0);
    }

    /** 获取指定索引的队伍 */
    public Team getTeam(int index) {
        if (index >= 0 && index < teams.size()) return teams.get(index);
        return null;
    }

    /** 添加玩家到指定队伍 */
    public void addPlayerToTeam(String playerName, int teamIndex) {
        // 先从其他队伍移除
        for (Team t : teams) t.players.remove(playerName);
        Team team = getTeam(teamIndex);
        if (team != null && !team.players.contains(playerName)) {
            team.players.add(playerName);
        }
    }

    /** 从指定队伍移除玩家 */
    public void removePlayerFromTeam(String playerName, int teamIndex) {
        Team team = getTeam(teamIndex);
        if (team != null) {
            team.players.remove(playerName);
        }
    }

    /** 记录死亡 (别名) */
    public void recordDeath(String playerName) {
        addDeath(playerName);
    }

    /** 重新分配队伍 */
    public void redistribute(List<String> allPlayers) {
        // 清空所有队伍
        for (Team t : teams) t.players.clear();
        // 确保队伍数量正确
        if (teams.size() != teamCount) initTeams();

        if (distributionMode == 0) {
            // 均分模式: 轮流分配
            for (int i = 0; i < allPlayers.size(); i++) {
                teams.get(i % teamCount).players.add(allPlayers.get(i));
            }
        } else {
            // 随机模式: 打乱后轮流分配
            List<String> shuffled = new ArrayList<>(allPlayers);
            Collections.shuffle(shuffled);
            for (int i = 0; i < shuffled.size(); i++) {
                teams.get(i % teamCount).players.add(shuffled.get(i));
            }
        }
    }

    /** 获取队伍总死亡数 */
    public int getTeamDeaths(Team team) {
        int total = 0;
        for (String player : team.players) {
            total += getDeaths(player);
        }
        return total;
    }
}
