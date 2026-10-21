# HMCL-Uncut

> 本仓库旨在 **恢复原版 HMCL 中被删减的功能**。

基于 [HMCL-dev/HMCL](https://github.com/HMCL-dev/HMCL) 的 **3.17.0.362** 源码，只做最小必要的还原改动，其余代码与原版保持一致。

<br/>

## 恢复的是什么

从 **3.17.0.362** 起，HMCL 在中国大陆地区禁用了下载功能，下载页会显示一条提示并把整页置灰：

> 根据相关法律法规，HMCL 在中国大陆地区暂不支持下载游戏相关内容。请前往我的世界官方网站下载我的世界中国版。

被禁用的范围是下载页的全部入口：

| 入口 | 代码位置 |
|---|---|
| 游戏版本安装 | `VersionsPage` |
| 整合包 / 模组 / 资源包 / 光影 / 世界 | `DownloadListPage`（`HMCLLocalizedDownloadListPage` 继承自它） |

本仓库把这些入口恢复成可用。

<br/>

## 恢复原理

**关键点：这次的功能不是被"编译期删掉"的，代码一行没删，只是 UI 层被禁用了。** 下载管线（`DownloadProvider`、`RemoteVersion`、`FileDownloader`、各下载源实现）完全没动。

官方实现分三层：

1. `RegionUtils.checkMachineIsChinaMainland()` —— 判断机器是否在中国大陆：时区是 `Asia/Shanghai`，或 UTC+8 且系统地区是 `CN`，或 Windows `GetUserGeoID` 返回 45（中国）
2. `RegionUtils.checkChinaMainlandNetwork()` —— 机器判定为真时，请求 `https://ipwho.is` 拿出口国家；请求失败或 `success=false` 也按"在大陆"处理（fail-closed）
3. `VersionsPage` / `DownloadListPage` 在构造皮肤时调用 `RegionUtils.isIsChinaMainlandNetwork()`，为真就套一层 `HintPane` 警告并把内容 `setDisable(true)`

所以恢复只需要让第三层的判定失效，前两层的下载能力原封不动。

### 本仓库的改动

只改 2 个文件：

**1. `HMCL/src/main/java/org/jackhuang/hmcl/util/RegionUtils.java`**

```java
public static void init() {
    // 不再做地区预检，也就不再请求 ipwho.is
}

public static boolean isIsChinaMainlandNetwork() {
    return false;   // 下载页永远不会被禁用
}
```

同时删掉了只服务于该判定的 `checkChinaMainlandNetwork()`、`IpWhoIsResponse` 和几个 import。

**保留** `isIsChinaMainlandMachine()` 不动——镜像源优选（国内优先 BMCLAPI）、Terracotta 节点排序、离线账户策略仍然依赖它，删了反而会让国内下载体验变差。

**2. `HMCL/src/main/java/org/jackhuang/hmcl/download/HMCLDownloadProvider.java`**

同一个提交在 Forge / NeoForge 的版本列表请求里顺手去掉了 BMCLAPI 回退，这里恢复：

```java
if (!RegionUtils.isIsChinaMainlandMachine()
        && (source == DownloadSource.DEFAULT || source == DownloadSource.OFFICIAL)) {
    return fetchOfficial;
}
```

这样国内用 `DEFAULT` 源时仍然是「官方源优先、BMCLAPI 兜底」。

改动合计 2 个文件、十几行。没有碰下载逻辑本身，也没有引入任何开关或后门。

<br/>

## 下载与使用

到 [Releases](../../releases/latest) 下载对应平台的文件：

| 文件 | 说明 |
|---|---|
| `HMCL-3.17.0.362-uncut.exe` | Windows 自解压版，双击即可 |
| `HMCL-3.17.0.362-uncut.jar` | 通用版，`java -jar` 启动 |
| `HMCL-3.17.0.362-uncut.sh` | Linux / macOS 自解压脚本 |
| `HMCL-3.17.0.362-uncut.deb` | Debian / Ubuntu 安装包 |

**运行要求**：Java 17 或更高（`.exe` / `.sh` 自带引导逻辑，会提示安装合适的 Java）。

<br/>

## 已知缺陷（重要）

官方构建在打包时会通过环境变量注入两个凭据：

- `MICROSOFT_AUTH_ID` → 写进 jar 的 `hmcl.microsoft.auth.id`
- `CURSEFORGE_API_KEY` → 写进 jar 的 `hmcl.curseforge.apikey`

**本仓库没有、也不会内置这两个值**，所以构建产物里它们是空字符串，由此带来两个明确缺陷：

1. **微软正版登录可能失败。** OAuth 流程需要客户端 ID，空值会导致授权请求被拒绝。使用外置登录（authlib-injector）或离线账户不受影响。
2. **CurseForge 搜索与文件下载可能受限或不可用。** 官方构建用 API Key 访问 CurseForge API，缺失时可能被限流或拒绝。Modrinth 与 BMCLAPI 镜像不受影响。

如果你有自己的凭据，可以在运行时覆盖，不需要重新构建：

```bash
java -Dhmcl.microsoft.auth.id=<你的 client id> \
     -Dhmcl.curseforge.apikey=<你的 api key> \
     -jar HMCL-3.17.0.362-uncut.jar
```

建议自己到 Azure 注册应用、到 CurseForge 申请 key，不要直接复用官方包里的值。

<br/>

## 自行构建

要求：JDK 17、Gradle（用仓库自带的 wrapper 即可）。

```bash
./gradlew build
```

产物在 `HMCL/build/libs/`：`HMCL-<版本>.jar` / `.exe` / `.sh` / `.deb`。

也可以直接用 CI：推送到 `main` 会触发 [`.github/workflows/build.yml`](.github/workflows/build.yml)，在 Ubuntu 上用 JDK 17 构建并上传四类产物。

<br/>

## 换新版本

上游每次发版都要重新核对，不要盲目套用同样的改动：

1. `git diff v<旧> v<新> -- HMCL/src/main/java/org/jackhuang/hmcl/util/RegionUtils.java HMCL/src/main/java/org/jackhuang/hmcl/ui/download/VersionsPage.java HMCL/src/main/java/org/jackhuang/hmcl/ui/instances/DownloadListPage.java`
2. 确认门禁是否仍然是 `isIsChinaMainlandNetwork()` 驱动的 UI 禁用；如果改成编译期常量、拆成多处、或挪到服务端，需要重新分析
3. 确认 `isIsChinaMainlandMachine()` 的调用点没有新增语义
4. 改版本号、构建、自检、发版

<br/>

## 声明

- 本仓库是基于 [HMCL-dev/HMCL](https://github.com/HMCL-dev/HMCL) 的第三方修改，**不是官方产物**，与原作者无关。
- 遵循上游 **GPLv3** 许可证（见 [LICENSE](LICENSE)），本仓库同样以 GPLv3 分发。
- 原版移除相关功能是作者出于法规合规考虑所做的明确表态。本仓库不代表原作者立场，也不为其背书。
- 本仓库仅供个人学习与自用，**按「现状」提供，不提供任何明示或暗示的担保**。使用本仓库产生的任何后果由使用者自行承担。
- 如果你喜欢 HMCL，请支持原作者：[HMCL 官网](https://hmcl.huangyuhui.net/) · [GitHub](https://github.com/HMCL-dev/HMCL) · [爱发电](https://afdian.com/a/huanghongxun)
