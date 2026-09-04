# GTNH 2.9.0 beta1-beta2：AE2 合成订单状态排序调研

调研日期：2026-09-04

## 调研问题与边界

本调研回答以下问题：在不修改服务端、不改变 AE2 网络协议、并同时支持 GTNH 2.9.0 beta1 与 beta2 的前提下，能否在客户端将 AE2 合成状态界面中的条目按“合成中物品 → 待合成物品 → 已合成物品 = 现有物品”稳定分组，同时保留同组条目的原有次序？

范围仅限当前这一项排序功能。配置界面、自定义优先级、其他 AE2 界面和新的服务端能力不在本次范围内。

## 结论摘要

该功能可以作为纯客户端 Mixin 实现，而且不需要新增数据包或服务端逻辑。

- GTNH 2.9.0 beta1 使用 AE2 `rv3-beta-977-GTNH`，beta2 使用 `rv3-beta-1000-GTNH`。两版目标代码的状态数据结构、同步路径和建议注入方法一致。
- 客户端收到的每个 `CraftingCpuEntry` 已经包含 `storedAmount`、`activeAmount` 和 `pendingAmount`。这三个值足以实现当前要求的三个优先级组。
- 最小注入点是客户端内部类 `GuiCraftingCPU$CraftingCpuVisualState` 的 `rebuildFilteredEntries(boolean, String)`：先让 AE2 完成隐藏/搜索过滤，再在方法尾部按状态等级进行稳定排序。
- 源码中没有发现“按数量”或“按使用比例”对这个列表进行二级排序的逻辑。当前实际基础次序来自服务端集合迭代及客户端 `LinkedHashMap` 的首次到达次序。因此，MVP 能准确承诺的是“同一状态组内保留 AE2 当前展示次序”。若“数量/使用比例”指另一个界面或尚未识别的排序模式，需要在形成规格前确认。
- AE2 的当前数据模型不能区分“开始合成订单时已经存在”与“本次合成过程中刚完成、现在存放在 CPU 内部”的来源；二者都会体现为 `STORAGE`。由于当前需求明确让已合成物品与现有物品同优先级，这一限制不阻碍实现。

## 目标版本

| GTNH 版本 | 发布时间 | 内置 AE2 版本 | 结论 |
| --- | --- | --- | --- |
| 2.9.0 beta1 | 2026-06-07 | `rv3-beta-977-GTNH` | 目标注入点存在 |
| 2.9.0 beta2 | 2026-07-05 | `rv3-beta-1000-GTNH` | 同一注入点存在 |

版本依据来自 GTNH 的 [beta1 发布页](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/releases/tag/2.9.0-beta-1)、[beta2 发布页](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/releases/tag/2.9.0-beta-2) 以及两版官方模组清单：[beta1 使用 AE2 977](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/2.9.0-beta-1#L353-L367)，[beta2 使用 AE2 1000](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/2.9.0-beta-2#L353-L367)。

GTNH 的官方版本差异记录确认 AE2 从 977 升级到 1000，并列出了期间的 AE2 变更。[完整 GTNH 变更记录](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/master/releases/changelogs/changelog%20from%202.9.0-beta-1%20to%202.9.0-beta-2.md#L285-L331)

## AE2 状态数据链

状态数据从合成 CPU 到 GUI 的路径如下：

1. `CraftingCPUCluster` 把内部数据暴露为三类列表：
   - `ACTIVE` 来自 `waitingFor`，即已经发出、正在等待产出的内容；
   - `PENDING` 来自尚未执行完的 `tasks` 输出；
   - `STORAGE` 来自合成 CPU 的内部 `inventory`。
2. `ContainerCraftingCPU.buildVisualEntryUpdates` 对每个变化条目分别查询 `STORAGE`、`ACTIVE`、`PENDING` 数量，并构造 `CraftingCpuEntry`。
3. `CraftingCpuEntry.writeToPacket` 将三个 `long` 数量直接写入 `PacketCraftingCpuUpdate`。
4. 客户端数据包处理器调用 `GuiCraftingCPU.postVisualEntryUpdate`；GUI 更新条目后重新构建过滤列表。

主要源码：

- [`CraftingCPUCluster#getModernListOfItem` 与 `getStackAmount`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L1434-L1517)
- [`ContainerCraftingCPU#buildVisualEntryUpdates`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/container/implementations/ContainerCraftingCPU.java#L177-L193)
- [`CraftingCpuEntry` 的字段、序列化和状态判断](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/container/implementations/CraftingCpuEntry.java#L11-L101)
- [`PacketCraftingCpuUpdate` 的客户端处理](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/core/sync/packets/PacketCraftingCpuUpdate.java#L23-L80)

### 建议的状态映射

一个条目可以同时拥有多个非零数量，所以必须明确优先级，而不能把状态看成互斥枚举。

| 条件（按顺序判断） | 领域状态 | 排序等级 |
| --- | --- | --- |
| `activeAmount > 0` | 合成中物品 | 0 |
| 否则，`pendingAmount > 0` | 待合成物品 | 1 |
| 否则，`storedAmount > 0` | 已合成物品 / 现有物品共同组 | 2 |

总量不大于零的条目会被 AE2 从 GUI 状态中移除，因此排序器不需要定义“全为零”的可见组。

这里的“已合成物品 / 现有物品”是界面层面的共同解释，不是 AE2 能够识别的两个独立来源。当前 UI 是实时快照而不是完成历史；已经完成但随即被消费或移出 CPU 的内容可能不再显示。

## 当前展示次序的真实含义

`GuiCraftingCPU.CraftingCpuVisualState` 使用 `LinkedHashMap` 保存条目。每次重建列表时，它按 `entries.values()` 的次序应用“隐藏已存储”和搜索过滤，然后依次加入 `ArrayList`，没有调用任何排序器。[GUI 源码](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/client/gui/implementations/GuiCraftingCPU.java#L663-L712)

初次同步与后续变化使用 `AEApi.instance().storage().createAEStackList()` 聚合。其多类型容器使用 `IdentityHashMap`，物品和流体的默认列表使用 `ObjectOpenHashSet`；这不是按数量、名称或使用比例定义的业务顺序：

- [`IAEStackList`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/util/item/IAEStackList.java#L18-L127)
- [`ItemList`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/util/item/ItemList.java#L32-L230)
- [`FluidList`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/rv3-beta-1000-GTNH/src/main/java/appeng/util/item/FluidList.java#L27-L142)

因此建议把需求中的“保留原有排序”精确定义为：

> 在每次 AE2 完成当前过滤并产出可见列表后，只按状态做稳定分组；比较结果相同的条目保持进入排序器时的相对次序。

Java 8 的 `Collections.sort(List, Comparator)` 明确保证稳定性，即比较结果相等的元素不会被重新排列。[Oracle Java 8 文档](https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#sort-java.util.List-java.util.Comparator-)

## 推荐实现切口

推荐使用 GTNH Late Mixin，目标为：

```text
appeng.client.gui.implementations.GuiCraftingCPU$CraftingCpuVisualState
```

在以下方法的 `TAIL` 注入：

```text
rebuildFilteredEntries(boolean hideStored, String searchText)
```

注入逻辑只对私有 `filteredEntries` 调用稳定排序，比较器只返回状态等级：

```java
private static int stateRank(CraftingCpuEntry entry) {
    if (entry.hasActiveAmount()) return 0;
    if (entry.hasPendingAmount()) return 1;
    return 2;
}
```

```java
Collections.sort(filteredEntries, Comparator.comparingInt(Ordering::stateRank));
```

实现时应使用字符串形式的 Mixin 目标名，因为目标内部类是 `private`；只在客户端 Mixin 列表中注册。`CraftingCpuEntry` 本身是公开类，可以作为比较器输入。

选择这个切口的原因：

- 排序发生在搜索和隐藏过滤之后，原功能保持不变；
- 所有会触发 GUI 重建的路径都会经过该方法，不需要分别修改绘制、滚动或数据包处理；
- 不修改 `ContainerCraftingCPU`、`PacketCraftingCpuUpdate` 或 `CraftingCPUCluster`；
- 排序仅改变客户端 `ArrayList` 的展示次序，不改变合成订单本身。

GTNH 官方模板推荐对其他非 core mod 使用 Early/Late Mixin，并可在运行时检查目标 mod 是否存在。[ExampleMod Mixin 指引](https://github.com/GTNewHorizons/ExampleMod1.7.10#mixins)；[GTNHMixins Late Mixin 说明](https://github.com/GTNewHorizons/GTNHMixins#late-mixins)

### 兼容性判断

对 AE2 977 与 1000 的以下四个文件逐一比较：

- `GuiCraftingCPU.java`
- `ContainerCraftingCPU.java`
- `CraftingCpuEntry.java`
- `PacketCraftingCpuUpdate.java`

后三个文件在两个标签之间没有变化。`GuiCraftingCPU` 的相关变化只有颜色 API 替换，以及 beta2 中 CPU 界面中键下单改为仅使用活动数量；`CraftingCpuVisualState` 和 `rebuildFilteredEntries` 没有变化。官方变更记录也把该中键行为列为 `rv3-beta-982-GTNH` 的变更。[AE2 977→1000 比较](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/compare/rv3-beta-977-GTNH...rv3-beta-1000-GTNH)；[中键下单变更记录](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/master/releases/changelogs/changelog%20from%202.9.0-beta-1%20to%202.9.0-beta-2.md#L320-L324)

因此可以用同一个 Mixin 支持两个版本。但该切口属于 AE2 私有 GUI 实现细节；未来超出 977–1000 范围的 AE2 版本必须重新验证目标类、字段和方法签名。

## 项目配置建议

### 构建基线

从 GTNH 官方 `ExampleMod1.7.10` 模板开始，保留其 Minecraft `1.7.10`、Forge `10.13.4.1614`、MCP `stable_12` 基线，把 `usesMixins` 改为 `true`。当前模板的 `.java-version` 为 25，默认 Jabel 模式允许现代 Java 语法但仍生成 Java 8 字节码：

- [模板 `gradle.properties`](https://github.com/GTNewHorizons/ExampleMod1.7.10/blob/master/gradle.properties)
- [模板 `.java-version`](https://github.com/GTNewHorizons/ExampleMod1.7.10/blob/master/.java-version)

### AE2 依赖

因为 Mixin 访问的是 AE2 实现类而不只是公开 API，应使用 `dev` classifier。GTNH 自有项目使用的坐标形式如下：[官方依赖示例](https://github.com/GTNewHorizons/GTNH-Web-Map/blob/master/dependencies.gradle#L551-L560)。

建议把 AE2 版本做成 Gradle 属性，并默认以最低目标版本 977 编译：

```groovy
compileOnly("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:${ae2Version}:dev") {
    transitive = false
}
runtimeOnlyNonPublishable("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:${ae2Version}:dev") {
    transitive = false
}
```

测试矩阵分别传入：

```text
rv3-beta-977-GTNH
rv3-beta-1000-GTNH
```

不要把 AE2 打包进本 mod。FML 依赖应声明本地客户端必须有 `appliedenergistics2`，同时为纯客户端 mod 设置 `acceptableRemoteVersions = "*"`，以允许连接未安装本 mod 的服务器。FML 1.7.10 源码将 `*` 定义为接受任意远端版本。[FML 1.7.10 `@Mod` 文档](https://github.com/MinecraftForge/FML/blob/1.7.10/src/main/java/cpw/mods/fml/common/Mod.java#L979-L995)

Mixin 配置只把目标类列在 `client` 部分，避免专用服务器加载任何 GUI 类。MVP 不增加自定义网络通道，也不增加服务器端入口。

## 验证计划

### 纯 Java 单元测试

把状态等级函数放在不依赖 Minecraft 客户端类的独立类中，至少覆盖：

1. `active > 0` 时等级为 0，即使 `pending` 或 `stored` 也大于零；
2. `active == 0 && pending > 0` 时等级为 1；
3. 仅 `stored > 0` 时等级为 2；
4. 多个同等级条目稳定保留输入顺序；
5. 状态更新后，条目会在下一次重建时移动到正确组；
6. 物品与流体条目使用同一状态规则，或在规格明确排除流体后增加对应边界测试。

### 游戏内验证

分别在 AE2 977 与 1000 环境中验证：

1. 构造同时含合成中、待合成和仅存储条目的大型合成订单；
2. 确认显示顺序为合成中 → 待合成 → 仅存储；
3. 确认同组内相对顺序与关闭本 mod 时一致；
4. 状态实时变化时列表会重新分组，且无条目丢失或数量变化；
5. 搜索、隐藏已存储、滚动、悬停提示和中键下单仍正常；
6. 单人游戏正常；安装本 mod 的客户端能连接未安装本 mod 的专用服务器；
7. 若合成订单包含流体，确认其行为符合最终规格。

## 形成规格前需要确认的两点

1. **“原有排序”的定义。** 当前目标源码没有数量或使用比例排序。建议 MVP 采用“保留进入状态分组前的实际展示次序”。如果期望的是另一个界面中的具体排序选项，需要给出界面名称、截图或操作路径。
2. **流体是否参与。** 该 GUI 和数据包使用泛型 `IAEStack<?>`，会同时容纳物品与流体。技术上最一致的实现是对所有可见条目按同一状态规则分组；如果功能必须只作用于物品，需要在规格中明确流体与物品混排时的位置。

除这两点外，当前技术信息已经足够进入需求质询和规格编写阶段。MVP 不需要排序开关、自定义优先级或服务端组件。

## 主要来源

- [GTNH 2.9.0 beta1 发布与模组清单](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/2.9.0-beta-1)
- [GTNH 2.9.0 beta2 发布与模组清单](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/2.9.0-beta-2)
- [GTNH beta1→beta2 官方变更记录](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/master/releases/changelogs/changelog%20from%202.9.0-beta-1%20to%202.9.0-beta-2.md)
- [AE2 `rv3-beta-977-GTNH`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/tree/rv3-beta-977-GTNH)
- [AE2 `rv3-beta-1000-GTNH`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/tree/rv3-beta-1000-GTNH)
- [GTNH ExampleMod 1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10)
- [GTNHMixins](https://github.com/GTNewHorizons/GTNHMixins)
- [FML 1.7.10 `@Mod` 源码](https://github.com/MinecraftForge/FML/blob/1.7.10/src/main/java/cpw/mods/fml/common/Mod.java)
- [Oracle Java 8 `Collections.sort` 文档](https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#sort-java.util.List-java.util.Comparator-)
