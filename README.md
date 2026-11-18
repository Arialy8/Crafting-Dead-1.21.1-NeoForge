# Crafting Dead（行尸走肉 · 僵尸末日生存）

> Minecraft `1.21.1` · NeoForge `21.1.x` · 模组版本 `3.0`

本仓库包含基于 **1.18.2 开源代码**移植而来的 **Minecraft 1.21.1 NeoForge** 版本的源代码与资源，属于非商业移植整合工程。在末日世界中收集物资、寻找枪械、抵御尸潮、治疗伤病，与队友建立基地并活下去。

This repository contains the source code and resources of a **Minecraft 1.21.1 NeoForge** port based on the **open-source 1.18.2 codebase**. It is a non-commercial port and integration project. Scavenge for supplies, find firearms, survive zombie hordes, treat injuries, and build a base with your team to stay alive in a zombie apocalypse.

仓库内容仅为主源码目录 `src/main`，包含：
- `src/main/java` — Java 源代码（448 个文件）
- `src/main/resources` — 模组资源、配置与数据（2039 个文件）

This repository contains only the main source directory `src/main`:
- `src/main/java` — Java source code (448 files)
- `src/main/resources` — mod assets, configs and data (2039 files)

---

## 介绍视频 / Video

- 哔哩哔哩（Bilibili）：https://www.bilibili.com/video/BV1e8HL6WENX/?spm_id_from=333.1387.homepage.video_card.click
- 详细指令网站 / Detailed command reference：https://craftingdead.cc.cd/

---

## 功能特色 / Features

- **枪械战斗**：手枪、冲锋枪、霰弹枪、步枪、狙击枪、机枪、加特林、RPG 等完整武器体系，支持第一/第三人称 3D 模型、瞄准、换弹动画与弹药同步。
- **生存状态**：口渴、骨折、感染等状态系统，以及医疗包、绷带、肾上腺素等医疗物品。
- **职业僵尸与夜间尸潮**：多种职业僵尸、差异化射击行为与夜间尸潮事件。
- **空投与战利品**：医疗/军用空投、自动生成空投、可配置区域的战利品生成器。
- **基地、小队与尸体**：基地蓝图与权限系统、最多 5 人小队、死亡掉落尸体与回收机制。
- **战斗模式 HUD**：`B` 键切换独立战斗栏，显示生命、护甲、饱食、口渴与击杀统计。

- **Firearms**: pistols, SMGs, shotguns, rifles, snipers, machine guns, miniguns and RPGs, with first/third-person 3D models, aiming, reload animations and ammo sync.
- **Survival systems**: thirst, fractures, infection, and medical items such as medkits, bandages and adrenaline.
- **Professional zombies & night hordes**: various zombie classes with distinct ranged behavior and nighttime horde events.
- **Airdrops & loot**: medical/military airdrops, automatic drops, and area-based configurable loot generators.
- **Bases, squads & corpses**: base blueprints with permissions, squads of up to 5, and corpse recovery on death.
- **Combat HUD**: press `B` to toggle a dedicated combat bar with health, armor, hunger, thirst and kill stats.

---

## 环境要求 / Requirements

| 项目 / Item | 版本 / Version |
|---|---|
| Minecraft | 1.21.1 |
| Mod 加载器 / Mod loader | NeoForge 21.1.x |
| Java | 21 |

客户端与服务端需使用相同版本模组，请勿混用不同版本。
Client and server must use the same mod version; do not mix versions.

---

## 指令介绍 / Commands

### 生存规则 / Survival rules

| 指令 / Command | 说明 / Description |
|---|---|
| `/hydrate [玩家]` | 恢复自己或指定玩家的口渴值 / Restore thirst of yourself or a player |
| `/craftingdead explosion terrain [true/false]` | 查看或设置爆炸是否破坏地形 / View or set whether explosions break terrain |
| `/craftingdead zombies natural [true/false]` | 查看或设置特殊僵尸自然生成 / View or set natural spawning of special zombies |
| `/craftingdead zombies rate [0-100]` | 查看或设置特殊僵尸生成率 / View or set special zombie spawn rate |
| `/craftingdead zombies rare [true/false]` | 查看或设置稀有职业僵尸 / View or set rare professional zombies |
| `/craftingdead horde enabled [true/false]` | 查看或设置夜间尸潮 / View or set night hordes |
| `/craftingdead horde chance [0-100]` | 查看或设置尸潮触发概率 / View or set horde trigger chance |

### 空投 / Airdrops

| 指令 / Command | 说明 / Description |
|---|---|
| `/craftingdead supplydrop medical <1-5>` | 在玩家周围生成医疗空投 / Spawn medical airdrops around the player |
| `/craftingdead supplydrop military <1-5>` | 在玩家周围生成军用空投 / Spawn military airdrops around the player |
| `/craftingdead supplydrop auto here / pos <x> <y> <z>` | 设置自动空投中心 / Set the automatic airdrop center |
| `/craftingdead supplydrop auto type medical/military` | 设置空投类型 / Set airdrop type |
| `/craftingdead supplydrop auto count <1-5>` | 设置每轮数量 / Set drops per round |
| `/craftingdead supplydrop auto interval <1-12000>` | 设置间隔（游戏刻）/ Set interval (ticks) |
| `/craftingdead supplydrop auto enabled true/false` | 启动或停止自动空投 / Start or stop automatic airdrops |
| `/craftingdead supplydrop auto status / delete` | 查看状态或删除设置 / View status or delete settings |

### 战利品生成器 / Loot generators

| 指令 / Command | 说明 / Description |
|---|---|
| `/craftingdead generator pos1 / pos2` | 设置区域两个角点 / Set the two area corners |
| `/craftingdead generator confirm <区域名>` | 确认区域并生成战利品生成器 / Confirm area and spawn generators |
| `/craftingdead generator delete <区域名>` | 删除区域、生成器及战利品 / Delete area, generators and loot |
| `/craftingdead generator list` | 列出所有已保存区域 / List all saved areas |

### 小队 / Squads

| 指令 / Command | 说明 / Description |
|---|---|
| `/team create <名称>` | 创建小队 / Create a squad |
| `/team invite <玩家>` | 邀请玩家 / Invite a player |
| `/team accept / decline` | 接受或拒绝邀请 / Accept or decline an invite |
| `/team kick <玩家>` | 踢出成员 / Kick a member |
| `/team leave` | 退出或解散小队 / Leave or disband the squad |

### 基地 / Bases

| 指令 / Command | 说明 / Description |
|---|---|
| `/base add <玩家>` | 添加基地成员 / Add a base member |
| `/base remove <玩家>` | 移除基地成员 / Remove a base member |
| `/base grant <玩家> <权限>` | 授予基地权限 / Grant a base permission |
| `/base revoke <玩家> <权限>` | 撤销基地权限 / Revoke a base permission |
| `/base list` | 查看基地授权列表 / View base permissions |

权限包括 `build`、`break`、`container`、`craft`。/ Permissions include `build`, `break`, `container`, `craft`.

---

## 联系与反馈 / Contact

遇到问题或有建议，可通过邮箱或 QQ 群联系 / For bugs or suggestions, contact via email or QQ group:

- 邮箱 / Email：**2078195736@qq.com**
- QQ 群聊 / QQ group：**947796302**

反馈时建议附上 `latest.log`、`debug.log` 或崩溃报告与复现步骤。
When reporting an issue, please include `latest.log`, `debug.log` or a crash report, along with steps to reproduce.

---

## 许可与致谢 / License & Credits

- 本项目基于 1.18.2 开源代码移植，采用 GPL v3.0 许可（详见 `LICENSE`，全文：https://www.gnu.org/licenses/gpl-3.0.txt）。
- Based on the open-source 1.18.2 codebase, licensed under GPL v3.0 (see `LICENSE`; full text: https://www.gnu.org/licenses/gpl-3.0.txt).
- 原作者 / Original authors: Sm0keySa1m0n, Arzio
