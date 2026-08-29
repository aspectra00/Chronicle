<p align="center">
  <img src="https://cdn.modrinth.com/data/cached_images/849c602a1ac00208e0371ed231540da69e4fbfb3.png" alt="Chronicle" width="600">
</p>

<p align="center">
  <a href="README.md"><img src="badges-for-readme/flags/us.png" alt="English" title="English" width="24" height="16"></a>&nbsp;&nbsp;
  <a href="README.ru.md"><img src="badges-for-readme/flags/ru.png" alt="Русский" title="Русский" width="24" height="16"></a>&nbsp;&nbsp;
  <a href="README.zh-CN.md"><img src="badges-for-readme/flags/cn.png" alt="简体中文" title="简体中文" width="24" height="16"></a>&nbsp;&nbsp;
  <a href="README.es.md"><img src="badges-for-readme/flags/es.png" alt="Español" title="Español" width="24" height="16"></a>&nbsp;&nbsp;
  <a href="README.de.md"><img src="badges-for-readme/flags/de.png" alt="Deutsch" title="Deutsch" width="24" height="16"></a>
</p>

<p align="center">
  <a href="https://github.com/aspectra00/Chronicle"><img src="https://i.imgur.com/vFmBpDq.png" alt="GitHub" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://ko-fi.com/aspectra"><img src="https://i.imgur.com/H08GkHi.png" alt="Ko-fi" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/chronicle-reminders"><img src="https://i.imgur.com/VROd79E.png" alt="Modrinth" width="64" height="64"></a>&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/chronicle-reminders"><img src="https://i.imgur.com/IDs74bZ.png" alt="CurseForge" width="64" height="64"></a>
</p>

<p align="center">
  <img src="badges-for-readme/minecraft.svg" alt="Minecraft" height="38">
  <img src="badges-for-readme/fabric.svg" alt="Fabric Loader" height="38">
  <img src="badges-for-readme/java.svg" alt="Java" height="38">
  <img src="badges-for-readme/chronicle.svg" alt="Chronicle" height="38">
</p>

Chronicle 是一款 Minecraft 客户端提醒模组。它可用于单人游戏和多人服务器，服务器端无需安装。

## 支持 Chronicle

Chronicle 完全免费，并持续适配所有受支持的 Minecraft 版本。如果它帮你节省了时间或避免错过重要事项，你可以支持后续更新、兼容性维护和测试工作。

<p align="center">
  <a href="https://ko-fi.com/aspectra"><img src="https://storage.ko-fi.com/cdn/brandasset/v2/support_me_on_kofi_blue.png" alt="在 Ko-fi 上支持 Chronicle" width="220"></a>
</p>

支持款项将用于版本兼容、发布测试和新的提醒功能。会员也可以选择在游戏内的 Community Supporters 页面中展示名字。

## 功能

### 提醒

- 每日计划
- 可选择星期的每周计划
- 自定义重复间隔
- 在游戏内启用、编辑、禁用或删除提醒
- 提醒触发后可保留、禁用或删除
- 在菜单中即时测试当前通知设置

### 触发规则

以下情况可以触发提醒：

- 生命值、饥饿值或氧气达到设定值
- 物品栏已满
- 手持物品的耐久度达到设定值
- 玩家进入某个维度
- 玩家进入设定的 X/Z 区域

规则只会在条件从不满足变为满足时触发。条件不再满足后，规则会重新就绪。

### Watch This

看向受支持的目标并按 `R` 即可开始或停止观察。Chronicle 可以在以下情况通知你：

- 作物完全成熟
- 蜂箱或蜂巢装满蜂蜜
- 炼药锅或堆肥桶就绪
- 洞穴藤蔓长出发光浆果
- 熔炉、烟熏炉或高炉停止工作
- 铜完全氧化
- 幼年生物长大

Watches 页面会列出当前世界或服务器中的观察目标。Chronicle 只检查客户端已经获取的数据，因此未加载区域中的目标会保持等待状态。

### 通知

- Modern 和 Vanilla 两种布局
- Modern 布局可选 Snooze 和 Dismiss 按钮
- 可延后 5、10、15、30 或 60 分钟
- 记录错过、完成和延后的提醒历史
- Minimal、Neon、Glass 和 Matrix 主题
- 自定义标题、图标、颜色、尺寸和动画
- Modern 通知可使用 PNG 或 JPG 自定义背景
- 自定义时实时预览
- 原版、静音或自定义通知声音

自定义音频支持 MP3、OGG、WAV、AIFF 和 AU。模组内置 JLayer 用于解码 MP3；详情请参阅[第三方声明](THIRD_PARTY_NOTICES.md)。

### 占位符

提醒文本支持：

- `{world}`
- `{coords}`
- `{biome}`
- `{dimension}`

同时支持通过 Text Placeholder API 注册的占位符。

### 语言

- 英语
- 俄语
- 简体中文
- 西班牙语
- 德语

## 按键

| 按键 | 操作 |
|---|---|
| `J` | 打开 Chronicle |
| `R` | 观察或取消观察准星指向的目标 |

两个按键都可以在 Minecraft 按键设置中更改。

## 环境要求

| 依赖 | 版本 |
|---|---:|
| Minecraft | 1.21.3 |
| Fabric Loader | 0.16.8 或更高版本（推荐 0.19.3） |
| Fabric API | 0.114.1+1.21.3 |
| Java | 21 |

Mod Menu 为可选依赖。Text Placeholder API 已包含在 Chronicle JAR 中。

## 安装

1. 为对应的 Minecraft 版本安装 Fabric Loader 和 Fabric API。
2. 将 Chronicle JAR 复制到 `mods` 文件夹。
3. 启动 Minecraft 并按 `J`。

设置保存在 `config/chronicle.json`。

## 构建

使用上方列出的 Java 版本并运行：

```powershell
.\gradlew.bat clean build
```

构建后的 JAR 位于 `build/libs`。

## 许可证

Chronicle 采用 [MIT 许可证](LICENSE)。
