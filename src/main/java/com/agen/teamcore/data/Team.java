package com.agen.teamcore.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个队伍的数据模型
 */
public class Team {
    /** 队伍ID */
    public int id;
    /** 队伍名称 */
    public String name = "";
    /** 颜色代码 (如 §c 表示红色) */
    public String colorCode = "§f";
    /** 队伍内玩家名列表 */
    public List<String> players = new ArrayList<>();
    /** 出生点坐标 */
    public double spawnX = 0;
    public double spawnY = 0;
    public double spawnZ = 0;
    /** 是否设置了出生点 */
    public boolean hasSpawn = false;

    public Team() {}

    public Team(int id, String name, String colorCode) {
        this.id = id;
        this.name = name;
        this.colorCode = colorCode;
    }

    /** 获取带颜色的队伍名 */
    public String getColoredName() {
        return colorCode + name;
    }

    /** 设置出生点 */
    public void setSpawn(double x, double y, double z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
        this.hasSpawn = true;
    }

    /** 获取颜色对应的RGB值 (用于GUI渲染) */
    public int getColorRgb() {
        return switch (colorCode) {
            case "§c" -> 0xFF5555; // 红
            case "§9" -> 0x5555FF; // 蓝
            case "§a" -> 0x55FF55; // 绿
            case "§e" -> 0xFFFF55; // 黄
            case "§d" -> 0xFF55FF; // 粉
            case "§b" -> 0x55FFFF; // 青
            case "§6" -> 0xFFAA00; // 橙
            case "§f" -> 0xFFFFFF; // 白
            case "§4" -> 0xAA0000; // 暗红
            case "§1" -> 0x0000AA; // 深蓝
            case "§2" -> 0x00AA00; // 暗绿
            case "§5" -> 0xAA00AA; // 紫
            case "§3" -> 0x00AAAA; // 湖蓝
            case "§7" -> 0xAAAAAA; // 灰
            case "§8" -> 0x555555; // 深灰
            case "§0" -> 0x000000; // 黑
            default -> 0xFFFFFF;
        };
    }

    /** 获取所有可用颜色代码 */
    public static String[] getAllColors() {
        return new String[]{
            "§c", "§9", "§a", "§e", "§d", "§b", "§6", "§f",
            "§4", "§1", "§2", "§5", "§3", "§7", "§8", "§0"
        };
    }
}
