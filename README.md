# TeamCore · 队伍核心

> 面向 PvP 服务器/小游戏地图的分组队伍系统：一本「队伍配置书」管人、一件「抽取队伍」道具分人、一套 mixin 管队友免伤与队伍重生点。

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-informational)
![Fabric](https://img.shields.io/badge/Fabric-0.19.3-informational)
![Java](https://img.shields.io/badge/Java-25-informational)

---

## 简介

`TeamCore` 是给服务器管理员用的队伍工具，不是给单人玩家玩的模组。它做三件事：

1. **分队**：用「抽取队伍」道具右键，把玩家按均分或随机分到 N 个队伍里，并给队伍一个颜色（会以旗帜的形式短暂展示 5 秒）。
2. **管理**：拿「分组队伍配置」书本（右键打开 GUI，需管理员权限）可以改队伍数量、分配模式、PvP 开关、设置队伍出生点、增删成员、看死亡排行榜。
3. **生效**：队伍颜色、队友免伤（PvP 关闭时）、死亡统计、重生回队伍出生点，全部由服务端 mixin 落地。

队伍数据保存在存档根目录的 `teamcore_data.json`，重启不丢。

## 功能

| 功能 | 说明 |
| --- | --- |
| 队伍配置书 | `teamcore:team_book`，右键打开管理界面；不可堆叠 |
| 抽取队伍道具 | `teamcore:team_draw`，右键把自己分配到队伍，并获得 5 秒的队伍颜色旗帜 |
| 分配模式 | 均分 / 随机，一键「执行分配」把已知玩家重新分组 |
| 队伍成员管理 | 逐个 ◀ ▶ 翻队、添加/移除成员、设置队伍出生点 |
| PvP 开关 | 关闭时同队玩家互相伤害被取消（`PlayerPvPMixin`） |
| 队伍重生点 | 玩家重生后自动传送到所在队伍的出生点（`RespawnMixin`） |
| 死亡统计 | 记录每人死亡次数并排行（`PlayerDeathMixin`） |
| 排行榜 | GUI 内查看死亡榜，「全部排行」一次看全服 |
| 热重载 | `/teamcore reload` / `/teamcore save` |

## 环境要求

- Minecraft **26.1.2**（新版本号体系）
- Fabric Loader **0.19.3** + Fabric API **0.155.2+26.1.2**
- Java **25**（服务端与客户端都需要）

## 安装

1. 把 `teamcore-1.0.0.jar` 放进 `mods/`（服务端必须装，客户端装才能用 GUI）。
2. 启动服务器，日志出现 `TeamCore Mod initialized` 即为成功。
3. 管理员执行 `/teamcore give` 领书，或 `/teamcore givedraw` 领抽取道具。

## 使用

### 命令

```
/teamcore give [玩家]       # 给自己/指定玩家一本队伍配置书
/teamcore givedraw [玩家]   # 给自己/指定玩家一个抽取队伍道具
/teamcore reload            # 从磁盘重新加载队伍数据
/teamcore save              # 手动保存队伍数据
```

（命令需要管理员权限）

### 管理员流程

1. `/teamcore give` → 右键书本打开界面。
2. 「设置」页：`-`/`+` 设定队伍数量 → 选「均分 / 随机」 → 按需切换 `PvP` → 点「执行分配」。
3. 「队伍管理」页：选队伍 → 「设出生点」站在目标位置按一下即可；用 ◀ ▶ 加人/移除。
4. 玩家用「抽取队伍」道具自助分队，或在设置页直接执行分配。

## 数据

存档根目录下的 `teamcore_data.json`：

```json
{
  "teamCount": 2,
  "distributionMode": 0,
  "pvpEnabled": false,
  "teams": [
    {
      "id": 0,
      "name": "红队",
      "colorCode": "§c",
      "players": ["Steve"],
      "spawnX": 0.0, "spawnY": 64.0, "spawnZ": 0.0,
      "hasSpawn": true
    }
  ],
  "deathStats": { "Steve": 3 }
}
```

## 实现说明

| 组件 | 作用 |
| --- | --- |
| `TeamManager` | 单例，负责加载/保存、分队、查询同队、旗帜延时移除队列 |
| `TeamData` / `Team` | 纯数据对象，供 Gson 序列化 |
| `Networking` | C2S/S2C 数据包：改设置、执行分配、加人/移人、改颜色、请求同步 |
| `PlayerPvPMixin` | 注入 `ServerPlayer#hurtServer`，PvP 关闭且同队时取消伤害 |
| `PlayerDeathMixin` | 注入 `ServerPlayer#die`，累加死亡统计 |
| `RespawnMixin` | 注入 `PlayerList#respawn`，把重生后的玩家送到队伍出生点 |

客户端只负责界面与发包，任何状态变更都要经服务端 `TeamManager` 确认后再同步回来，避免两端数据打架。

## 构建

```bash
# 需要 JDK 25
./gradlew build
# 产物：build/libs/teamcore-1.0.0.jar
```

## 目录结构

```
src/main/java/com/agen/teamcore/
├── TeamCore.java               # 服务端入口：注册物品、网络包、生命周期与命令
├── TeamCoreClient.java         # 客户端入口：GUI 与按键
├── data/Team.java              # 单个队伍（名称/颜色/成员/出生点）
├── data/TeamData.java          # 全局数据（队伍数/分配模式/PvP/死亡统计）
├── manager/TeamManager.java    # 业务核心（加载保存、分队、同队判定、旗帜移除）
├── item/TeamBookItem.java      # 队伍配置书
├── item/TeamDrawItem.java      # 抽取队伍道具
├── network/Networking.java     # 全部自定义数据包
├── client/gui/TeamConfigScreen.java  # 设置 / 队伍管理 / 排行榜三页 GUI
└── mixin/                      # PvP 免伤、死亡统计、重生点
```

## 注意

- 队伍 PvP 判定基于玩家名，改名前请先更新队伍数据。
- 旗帜展示是临时的（5 秒后自动移除），不要指望它当永久装饰。

## 许可

MIT
