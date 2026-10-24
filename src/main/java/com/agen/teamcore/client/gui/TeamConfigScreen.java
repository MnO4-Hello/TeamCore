package com.agen.teamcore.client.gui;

import com.agen.teamcore.data.Team;
import com.agen.teamcore.data.TeamData;
import com.agen.teamcore.manager.TeamManager;
import com.agen.teamcore.network.Networking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 队伍配置主界面 - 包含设置、队伍管理、排行榜三个标签页
 */
public class TeamConfigScreen extends Screen {
    private static final int GUI_W = 280;
    private static final int GUI_H = 210;

    private int currentTab = 0;
    private int selectedTeam = 0;
    private int selectedPlayerIdx = 0;
    private int addPlayerIdx = 0;
    private int leaderboardMode = 0;
    private int teamScroll = 0;
    private int playerScroll = 0;
    private int lbScroll = 0;
    private boolean firstInit = true;

    public TeamConfigScreen() {
        super(Component.literal("分组队伍配置"));
    }

    @Override
    protected void init() {
        super.init();
        if (firstInit) {
            ClientPlayNetworking.send(new Networking.RequestDataC2SPacket());
            firstInit = false;
        }
        buildAllWidgets();
    }

    private void buildAllWidgets() {
        clearWidgets();
        int x0 = (width - GUI_W) / 2;
        int y0 = (height - GUI_H) / 2;

        // 标签页按钮
        addRenderableWidget(Button.builder(Component.literal("设置"), b -> { currentTab = 0; this.init(); })
                .bounds(x0 + 4, y0 + 4, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("队伍管理"), b -> { currentTab = 1; this.init(); })
                .bounds(x0 + 94, y0 + 4, 86, 20).build());
        addRenderableWidget(Button.builder(Component.literal("排行榜"), b -> { currentTab = 2; this.init(); })
                .bounds(x0 + 184, y0 + 4, 86, 20).build());

        boolean admin = TeamManager.getInstance().isClientAdmin();
        switch (currentTab) {
            case 0 -> buildSettingsTab(x0, y0, admin);
            case 1 -> buildTeamTab(x0, y0, admin);
            case 2 -> buildLeaderboardTab(x0, y0);
        }
    }

    // ==================== 设置标签页 ====================
    private void buildSettingsTab(int x0, int y0, boolean admin) {
        TeamData data = TeamManager.getInstance().getData();
        int cy = y0 + 36;

        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            if (data.teamCount > 1) {
                ClientPlayNetworking.send(new Networking.UpdateTeamCountC2SPacket(data.teamCount - 1));
            }
        }).bounds(x0 + 100, cy, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            if (data.teamCount < 16) {
                ClientPlayNetworking.send(new Networking.UpdateTeamCountC2SPacket(data.teamCount + 1));
            }
        }).bounds(x0 + 160, cy, 20, 20).build());

        cy += 28;
        String modeText = data.distributionMode == 0 ? "均分" : "随机";
        addRenderableWidget(Button.builder(Component.literal("分配: " + modeText), b -> {
            ClientPlayNetworking.send(new Networking.UpdateModeC2SPacket(data.distributionMode == 0 ? 1 : 0));
        }).bounds(x0 + 100, cy, 80, 20).build());

        cy += 28;
        String pvpText = data.pvpEnabled ? "开启" : "关闭";
        addRenderableWidget(Button.builder(Component.literal("PvP: " + pvpText), b -> {
            ClientPlayNetworking.send(new Networking.UpdatePvPC2SPacket(!data.pvpEnabled));
        }).bounds(x0 + 100, cy, 80, 20).build());

        cy += 32;
        addRenderableWidget(Button.builder(Component.literal("执行分配"), b -> {
            ClientPlayNetworking.send(new Networking.RedistributeC2SPacket());
        }).bounds(x0 + 90, cy, 100, 20).build());

        if (!admin) {
            addRenderableWidget(Button.builder(Component.literal("仅管理员可修改设置"), b -> {})
                    .bounds(x0 + 70, y0 + GUI_H - 30, 140, 20).build());
        }
    }

    // ==================== 队伍管理标签页 ====================
    private void buildTeamTab(int x0, int y0, boolean admin) {
        TeamData data = TeamManager.getInstance().getData();
        if (data.teams.isEmpty()) return;
        selectedTeam = Math.min(selectedTeam, data.teams.size() - 1);
        Team team = data.teams.get(selectedTeam);

        // 队伍选择按钮
        int btnY = y0 + 28;
        int maxVisible = Math.min(data.teams.size(), 4);
        for (int i = teamScroll; i < teamScroll + maxVisible && i < data.teams.size(); i++) {
            final int idx = i;
            Team t = data.teams.get(i);
            String label = (i == selectedTeam ? "> " : "") + t.name + " (" + t.players.size() + "人)";
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                selectedTeam = idx; selectedPlayerIdx = 0; addPlayerIdx = 0; this.init();
            }).bounds(x0 + 4, btnY, 126, 18).build());
            btnY += 20;
        }
        if (data.teams.size() > 4) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                teamScroll = Math.max(0, teamScroll - 1); this.init();
            }).bounds(x0 + 4, btnY, 60, 14).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                teamScroll = Math.min(data.teams.size() - 4, teamScroll + 1); this.init();
            }).bounds(x0 + 68, btnY, 60, 14).build());
        }

        // 右侧操作按钮
        int rx = x0 + 138;
        int ry = y0 + 28;

        addRenderableWidget(Button.builder(Component.literal("设出生点"), b -> {
            if (minecraft != null && minecraft.player != null) {
                var pos = minecraft.player.position();
                ClientPlayNetworking.send(new Networking.SetSpawnC2SPacket(selectedTeam, pos.x, pos.y, pos.z));
            }
        }).bounds(rx, ry, 134, 18).build());

        ry += 22;
        // 玩家选择
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (!team.players.isEmpty()) {
                selectedPlayerIdx = (selectedPlayerIdx - 1 + team.players.size()) % team.players.size();
                this.init();
            }
        }).bounds(rx, ry, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (!team.players.isEmpty()) {
                selectedPlayerIdx = (selectedPlayerIdx + 1) % team.players.size();
                this.init();
            }
        }).bounds(rx + 114, ry, 20, 18).build());

        ry += 22;
        addRenderableWidget(Button.builder(Component.literal("移除选中"), b -> {
            if (selectedPlayerIdx >= 0 && selectedPlayerIdx < team.players.size()) {
                ClientPlayNetworking.send(new Networking.RemovePlayerC2SPacket(
                        team.players.get(selectedPlayerIdx), selectedTeam));
                selectedPlayerIdx = 0;
            }
        }).bounds(rx, ry, 66, 18).build());

        // 添加玩家
        List<String> available = getAvailablePlayers(data);
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
            if (!available.isEmpty()) { addPlayerIdx = (addPlayerIdx - 1 + available.size()) % available.size(); this.init(); }
        }).bounds(rx, ry + 22, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            if (!available.isEmpty()) { addPlayerIdx = (addPlayerIdx + 1) % available.size(); this.init(); }
        }).bounds(rx + 114, ry + 22, 20, 18).build());

        addRenderableWidget(Button.builder(Component.literal("添加"), b -> {
            List<String> avail = getAvailablePlayers(TeamManager.getInstance().getData());
            if (!avail.isEmpty() && addPlayerIdx < avail.size()) {
                ClientPlayNetworking.send(new Networking.AddPlayerC2SPacket(avail.get(addPlayerIdx), selectedTeam));
                addPlayerIdx = 0;
            }
        }).bounds(rx + 24, ry + 44, 86, 18).build());
    }

    private List<String> getAvailablePlayers(TeamData data) {
        Set<String> inTeams = new HashSet<>();
        for (Team t : data.teams) inTeams.addAll(t.players);
        return TeamManager.getInstance().getAllKnownPlayerNames().stream()
                .filter(n -> !inTeams.contains(n))
                .collect(Collectors.toList());
    }

    // ==================== 排行榜标签页 ====================
    private void buildLeaderboardTab(int x0, int y0) {
        int ty = y0 + 28;
        addRenderableWidget(Button.builder(Component.literal("全部排行"), b -> {
            leaderboardMode = 0; lbScroll = 0; this.init();
        }).bounds(x0 + 4, ty, 86, 18).build());
        addRenderableWidget(Button.builder(Component.literal("队伍排行"), b -> {
            leaderboardMode = 1; lbScroll = 0; this.init();
        }).bounds(x0 + 94, ty, 86, 18).build());
        addRenderableWidget(Button.builder(Component.literal("队内排行"), b -> {
            leaderboardMode = 2; lbScroll = 0; this.init();
        }).bounds(x0 + 184, ty, 86, 18).build());

        if (leaderboardMode == 2) {
            TeamData data = TeamManager.getInstance().getData();
            if (!data.teams.isEmpty()) {
                selectedTeam = Math.min(selectedTeam, data.teams.size() - 1);
                addRenderableWidget(Button.builder(Component.literal("◀ " + data.teams.get(selectedTeam).name + " ▶"), b -> {
                    selectedTeam = (selectedTeam + 1) % data.teams.size(); this.init();
                }).bounds(x0 + 80, ty + 22, 120, 18).build());
            }
        }
    }

    // ==================== 渲染 ====================
    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        // 标准半透明背景(变暗游戏画面)
        extractTransparentBackground(ctx);

        int x0 = (width - GUI_W) / 2;
        int y0 = (height - GUI_H) / 2;

        // 先绘制背景面板(在控件之下)
        ctx.fill(x0, y0, x0 + GUI_W, y0 + GUI_H, 0xF03A3A3A);
        ctx.fill(x0, y0, x0 + GUI_W, y0 + 1, 0xFF8888AA);
        ctx.fill(x0, y0 + GUI_H - 1, x0 + GUI_W, y0 + GUI_H, 0xFF8888AA);
        ctx.fill(x0, y0, x0 + 1, y0 + GUI_H, 0xFF8888AA);
        ctx.fill(x0 + GUI_W - 1, y0, x0 + GUI_W, y0 + GUI_H, 0xFF8888AA);

        // 标签高亮
        for (int i = 0; i < 3; i++) {
            if (i == currentTab) {
                ctx.fill(x0 + 4 + i * 90, y0 + 3, x0 + 4 + i * 90 + 86, y0 + 25, 0x44FFFFFF);
            }
        }

        // 渲染控件(按钮等) - 在背景之上
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        // 标题(最顶层)
        ctx.centeredText(font, "§6§l分组队伍配置", x0 + GUI_W / 2, y0 - 12, 0xFFFFFF);

        // 渲染标签内容文本
        switch (currentTab) {
            case 0 -> renderSettingsContent(ctx, x0, y0);
            case 1 -> renderTeamContent(ctx, x0, y0);
            case 2 -> renderLeaderboardContent(ctx, x0, y0);
        }
    }

    private void renderSettingsContent(GuiGraphicsExtractor ctx, int x0, int y0) {
        TeamData data = TeamManager.getInstance().getData();
        int lx = x0 + 14;
        int cy = y0 + 42;

        ctx.text(font, "分组数量:", lx, cy + 5, 0xFFAAAAAA);
        ctx.centeredText(font, "§e§l" + data.teamCount, x0 + 140, cy + 5, 0xFFFFFF55);

        cy += 28;
        ctx.text(font, "分配模式:", lx, cy + 5, 0xFFAAAAAA);

        cy += 28;
        ctx.text(font, "队伍PvP:", lx, cy + 5, 0xFFAAAAAA);
        String pvpInfo = data.pvpEnabled ? "§a开启 - 队友可互相攻击" : "§c关闭 - 队友无法互相攻击";
        ctx.text(font, pvpInfo, lx, cy + 28, 0xFF888888);

        cy += 50;
        ctx.text(font, "§7点击[执行分配]将在线玩家分配到各队伍", lx, cy, 0xFF666666);
        ctx.text(font, "§7均分: 每组人数尽可能相同", lx, cy + 12, 0xFF666666);
        ctx.text(font, "§7随机: 完全随机打乱分配", lx, cy + 24, 0xFF666666);
    }

    private void renderTeamContent(GuiGraphicsExtractor ctx, int x0, int y0) {
        TeamData data = TeamManager.getInstance().getData();
        if (data.teams.isEmpty()) return;
        Team team = data.teams.get(Math.min(selectedTeam, data.teams.size() - 1));

        int rx = x0 + 138;

        // 在第一组◀ ▶箭头之间显示选中的玩家名 (y0+50 区域)
        if (!team.players.isEmpty() && selectedPlayerIdx < team.players.size()) {
            ctx.centeredText(font, "§e" + team.players.get(selectedPlayerIdx), rx + 67, y0 + 55, 0xFFFFFF55);
        } else {
            ctx.centeredText(font, "§7(空)", rx + 67, y0 + 55, 0xFF666666);
        }

        // 在第二组◀ ▶箭头之间显示待添加的玩家名 (y0+94 区域)
        List<String> available = getAvailablePlayers(data);
        if (!available.isEmpty() && addPlayerIdx < available.size()) {
            ctx.centeredText(font, "§b" + available.get(addPlayerIdx), rx + 67, y0 + 99, 0xFF55FFFF);
        } else {
            ctx.centeredText(font, "§7(无可用)", rx + 67, y0 + 99, 0xFF666666);
        }

        // 队伍信息 (在按钮区域下方显示)
        int infoY = y0 + 140;
        ctx.text(font, "队伍: " + team.getColoredName(), rx, infoY, 0xFFFFFFFF);
        infoY += 12;
        String spawnInfo = team.hasSpawn
                ? String.format("出生点: %.0f, %.0f, %.0f", team.spawnX, team.spawnY, team.spawnZ)
                : "出生点: 未设置";
        ctx.text(font, spawnInfo, rx, infoY, 0xFF888888);
        infoY += 12;
        ctx.text(font, "成员 (" + team.players.size() + "人):", rx, infoY, 0xFFAAAAAA);
        infoY += 12;

        // 成员列表
        int maxShow = 4;
        for (int i = playerScroll; i < Math.min(playerScroll + maxShow, team.players.size()); i++) {
            String prefix = (i == selectedPlayerIdx) ? "§e> " : "  ";
            ctx.text(font, prefix + team.players.get(i), rx + 4, infoY, 0xFFCCCCCC);
            infoY += 11;
        }
        if (team.players.isEmpty()) {
            ctx.text(font, "  (空)", rx + 4, infoY, 0xFF666666);
        }
    }

    private void renderLeaderboardContent(GuiGraphicsExtractor ctx, int x0, int y0) {
        TeamData data = TeamManager.getInstance().getData();
        int startY = y0 + (leaderboardMode == 2 ? 74 : 52);
        int lx = x0 + 14;
        int maxEntries = 9;

        if (leaderboardMode == 0) {
            // 全部排行
            ctx.text(font, "§e§l全部玩家死亡排行", lx, startY - 12, 0xFFFFFF55);
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(data.deathStats.entrySet());
            sorted.sort((a, b) -> b.getValue() - a.getValue());

            for (int i = lbScroll; i < Math.min(lbScroll + maxEntries, sorted.size()); i++) {
                var entry = sorted.get(i);
                String line = String.format("§e%d. §f%s §7- §c%d 次死亡", i + 1, entry.getKey(), entry.getValue());
                ctx.text(font, line, lx, startY + (i - lbScroll) * 13, 0xFFFFFFFF);
            }
            if (sorted.isEmpty()) {
                ctx.text(font, "§7暂无死亡记录", lx, startY, 0xFF666666);
            }
        } else if (leaderboardMode == 1) {
            // 队伍排行
            ctx.text(font, "§e§l队伍总死亡排行", lx, startY - 12, 0xFFFFFF55);
            List<Team> sorted = new ArrayList<>(data.teams);
            sorted.sort((a, b) -> data.getTeamDeaths(b) - data.getTeamDeaths(a));

            for (int i = 0; i < Math.min(maxEntries, sorted.size()); i++) {
                Team t = sorted.get(i);
                String line = String.format("§e%d. %s §7- 总计 §c%d §7次死亡 (%d人)",
                        i + 1, t.getColoredName(), data.getTeamDeaths(t), t.players.size());
                ctx.text(font, line, lx, startY + i * 13, 0xFFFFFFFF);
            }
        } else {
            // 队内排行
            if (!data.teams.isEmpty()) {
                Team team = data.teams.get(Math.min(selectedTeam, data.teams.size() - 1));
                ctx.text(font, "§e§l" + team.getColoredName() + " §e§l队内排行", lx, startY - 12, 0xFFFFFF55);

                List<String> players = new ArrayList<>(team.players);
                players.sort((a, b) -> data.getDeaths(b) - data.getDeaths(a));

                for (int i = lbScroll; i < Math.min(lbScroll + maxEntries, players.size()); i++) {
                    String p = players.get(i);
                    String line = String.format("§e%d. §f%s §7- §c%d §7次死亡", i + 1, p, data.getDeaths(p));
                    ctx.text(font, line, lx, startY + (i - lbScroll) * 13, 0xFFFFFFFF);
                }
                if (players.isEmpty()) {
                    ctx.text(font, "§7该队伍暂无成员", lx, startY, 0xFF666666);
                }
            }
        }
    }

    // ==================== 输入处理 ====================
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int dir = vAmount > 0 ? -1 : 1;
        TeamData data = TeamManager.getInstance().getData();

        if (currentTab == 1) {
            Team team = data.teams.get(Math.min(selectedTeam, data.teams.size() - 1));
            int maxScroll = Math.max(0, team.players.size() - 5);
            playerScroll = Math.max(0, Math.min(maxScroll, playerScroll + dir));
        } else if (currentTab == 2) {
            int size = 0;
            if (leaderboardMode == 0) size = data.deathStats.size();
            else if (leaderboardMode == 2 && !data.teams.isEmpty()) {
                size = data.teams.get(Math.min(selectedTeam, data.teams.size() - 1)).players.size();
            }
            int maxScroll = Math.max(0, size - 9);
            lbScroll = Math.max(0, Math.min(maxScroll, lbScroll + dir));
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    /** 供外部(网络包回调)调用以刷新界面 */
    public void refresh() {
        this.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
