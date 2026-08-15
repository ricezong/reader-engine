# Reader Engine

> 书源 / 漫画源解析引擎 —— 基于 [reader-dev](https://github.com/changshengyu/reader-dev) 改造的 Java/Kotlin 混合引擎

## 简介

`reader-engine` 是一个独立的书源解析引擎，将阅读 App 中书源/漫画源的解析逻辑提取为可复用的 Java 库。

核心能力：
- 解析 JSON 格式的书源规则（兼容阅读 App 书源格式）
- 通过 HTTP 请求获取网页内容
- 使用 Jsoup / XPath / JsonPath / CSS 选择器解析网页和 JSON 数据
- 通过 Rhino JS 引擎执行书源中的自定义 JavaScript 规则
- 支持小说源（`bookSourceType=0`）和漫画源（`bookSourceType=2`）
- 内置 4 个小说源 + 4 个漫画源，开箱即用

### 技术栈

| 组件 | 技术                                     |
|------|----------------------------------------|
| 语言 | Java 1.8 + Kotlin 1.8                  |
| 构建 | Maven + maven-shade-plugin (fat jar)   |
| HTTP | OkHttp 4.12                            |
| HTML 解析 | Jsoup 1.17 + JsoupXpath 2.5            |
| JSON 解析 | Gson 2.10 + JsonPath 2.9               |
| JS 引擎 | Mozilla Rhino 1.7.13 (定制版)             |
| 协程 | Kotlin Coroutines 1.7                  |
| 加密 | Hutool + BouncyCastle                  |
| 日志 | kotlin-logging 3.0.5 (slf4j-api 由宿主提供) |

## 快速开始

### 1. 安装 jar 到本地仓库

**Linux / macOS (bash):**

```bash
mvn install:install-file \
  -Dfile=target/reader-engine-1.0.0.jar \
  -DgroupId=cn.kong \
  -DartifactId=reader-engine \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

**Windows (PowerShell):**

```powershell
mvn install:install-file "-Dfile=target/reader-engine-1.0.0.jar" "-DgroupId=cn.kong" "-DartifactId=reader-engine" "-Dversion=1.0.0" "-Dpackaging=jar"
```

### 2. 引入依赖

```xml
<dependency>
    <groupId>cn.kong</groupId>
    <artifactId>reader-engine</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. 使用

```java
ReaderService service = ReaderService.getInstance();

// 搜索小说
List<SearchResult> results = service.searchNovel("斗破苍穹");

// 获取详情
SearchResult r = results.get(0);
BookDetail detail = service.getBookDetail(r.getBookUrl(), r.getSource());

// 获取目录
List<ChapterInfo> chapters = service.getChapterList(r.getBookUrl(), r.getSource());

// 获取第 1 章正文
String content = service.getContent(r.getBookUrl(), r.getSource(), 0);

// 批量下载前 10 章
List<ChapterContent> contents = service.batchDownload(r.getBookUrl(), r.getSource(), 0, 10);
```

## 内置书源

### 小说源（4 个）

| 源名称 | 简称(source) | URL |
|--------|-------------|-----|
| 八零小说 | `80` | `http://www.80ge.info` |
| 独步小说 | `dubu` | `https://www.dbxsd.com` |
| 猫眼看书 | `maoyan` | `http://api.lemiyigou.com` |
| 七猫小说 | `qimao` | `https://api-bc.wtzw.com` |

### 漫画源（4 个）

| 源名称 | 简称(source) | URL |
|--------|-------------|-----|
| G站漫画 | `godamanga` | `https://godamanga.com` |
| 漫画台 | `manhuatai` | `https://m.manhuatai.com` |
| 如漫画 | `rumanhua` | `https://www.rumanhua.com` |
| 再漫画 | `zaimanhua` | `https://www.zaimanhua.com` |

> 获取详情/目录/正文时传入 `source` 简称即可，无需传完整 URL。

## API 文档

### ReaderService（推荐）

单例服务入口，用户只需传入 `String` 参数和简单 DTO，无需接触内部实体对象。

#### 获取实例

```java
ReaderService service = ReaderService.getInstance();
```

#### 1. 列出书源

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `listAllSources()` | `List<SourceInfo>` | 所有源 (source, name, type, typeDesc) |
| `listNovelSources()` | `List<SourceInfo>` | 小说源 |
| `listComicSources()` | `List<SourceInfo>` | 漫画源 |

`SourceInfo` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `source` | String | 书源简称（如 80、dubu、godamanga） |
| `name` | String | 书源名称 |
| `type` | int | 0=小说, 2=漫画 |
| `typeDesc` | String | 类型描述（小说 / 漫画） |

#### 2. 搜索

| 方法 | 参数 | 说明 |
|------|------|------|
| `search(String keyword)` | 关键词 | 跨所有源搜索 |
| `search(String keyword, int page)` | 关键词, 页码 | 指定页码 |
| `search(String keyword, String source)` | 关键词, 书源简称 | 在指定书源中搜索 |
| `search(String keyword, String source, int page)` | 关键词, 书源简称, 页码 | 指定书源 + 页码 |
| `searchNovel(String keyword)` | 关键词 | 仅搜索小说源 |
| `searchNovel(String keyword, int page)` | 关键词, 页码 | 小说源指定页 |
| `searchComic(String keyword)` | 关键词 | 仅搜索漫画源 |
| `searchComic(String keyword, int page)` | 关键词, 页码 | 漫画源指定页 |

返回 `List<SearchResult>`，每个结果包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | String | 书名 |
| `author` | String | 作者 |
| `bookUrl` | String | 书籍 URL（后续操作需要） |
| `source` | String | 书源简称（后续操作需要，如 80/dubu/godamanga） |
| `sourceName` | String | 书源名称 |
| `coverUrl` | String | 封面 URL |
| `intro` | String | 简介 |
| `kind` | String | 分类 |
| `wordCount` | String | 字数 |
| `type` | int | 0=小说, 2=漫画 |

#### 3. 按作者搜索

| 方法 | 参数 | 说明 |
|------|------|------|
| `searchByAuthor(String author)` | 作者名 | 跨所有源 |
| `searchNovelByAuthor(String author)` | 作者名 | 仅小说源 |
| `searchComicByAuthor(String author)` | 作者名 | 仅漫画源 |

返回 `List<SearchResult>`，自动过滤匹配作者的结果。

#### 4. 获取详情

| 方法 | 参数 | 说明 |
|------|------|------|
| `getBookDetail(String bookUrl, String source)` | 书籍URL, 书源简称 | 获取书籍详情 |

返回 `BookDetail`，包含：name, author, bookUrl, tocUrl, source, sourceName, coverUrl, intro, kind, wordCount, latestChapterTitle, type。

#### 5. 获取目录

| 方法 | 参数 | 说明 |
|------|------|------|
| `getChapterList(String bookUrl, String source)` | 书籍URL, 书源简称 | 获取章节列表 |

返回 `List<ChapterInfo>`，每个章节包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 章节标题 |
| `url` | String | 章节 URL |
| `index` | int | 章节序号（从 0 开始） |

#### 6. 获取正文

| 方法 | 参数 | 说明 |
|------|------|------|
| `getContent(bookUrl, source, chapterIndex)` | 书籍URL, 书源简称, 章节序号 | 按序号获取正文 |
| `getContentByUrl(bookUrl, source, chapterUrl)` | 书籍URL, 书源简称, 章节URL | 按URL获取正文 |

返回 `String` 正文内容（漫画为图片 HTML）。

#### 7. 批量下载

| 方法 | 参数 | 说明 |
|------|------|------|
| `batchDownload(bookUrl, source, start, end)` | 书籍URL, 书源简称, 起始序号, 结束序号(不含) | 下载范围 |
| `batchDownload(bookUrl, source, start, end, delayMs)` | 同上+间隔毫秒 | 带延迟 |
| `batchDownloadAll(bookUrl, source)` | 书籍URL, 书源简称 | 下载全部 |
| `batchDownloadAsString(bookUrl, source, start, end, sep)` | 同上+分隔符 | 拼接为完整文本 |

返回 `List<ChapterContent>`，每个结果包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 章节标题 |
| `url` | String | 章节 URL |
| `index` | int | 章节序号 |
| `content` | String | 正文内容 |

（`batchDownloadAsString` 返回 `String`）

### ReaderEngine（底层 API）

如果需要直接操作书源对象，可使用底层 API：

| 方法 | 说明 |
|------|------|
| `parseBookSource(String json)` | 解析单个书源 |
| `parseBookSources(String json)` | 解析书源列表 |
| `initSource(BookSource source)` | 初始化书源（执行 loginUrl JS，设置 variable） |
| `setVariable(BookSource, String variable)` | 手动设置书源 variable |
| `getVariable(BookSource)` | 获取书源 variable |
| `evalJS(BookSource, String js)` | 执行书源 JS 代码 |
| `search(BookSource, String key, Integer page)` | 搜索 |
| `explore(BookSource, String url, Integer page)` | 发现页 |
| `getBookInfo(BookSource, String bookUrl)` | 获取详情 |
| `getChapterList(BookSource, Book book)` | 获取目录 |
| `getBookContent(BookSource, Book, BookChapter)` | 获取正文 |

## 使用示例

### 完整小说流程

```java
ReaderService service = ReaderService.getInstance();

// 1. 搜索
List<SearchResult> results = service.searchNovel("斗破苍穹");
System.out.println("搜索结果: " + results.size() + " 本");

// 2. 取第一本
SearchResult r = results.get(0);
System.out.println("书名: " + r.getName() + " | 作者: " + r.getAuthor());

// 3. 获取详情
BookDetail detail = service.getBookDetail(r.getBookUrl(), r.getSource());
System.out.println("简介: " + detail.getIntro());

// 4. 获取目录
List<ChapterInfo> chapters = service.getChapterList(r.getBookUrl(), r.getSource());
System.out.println("章节数: " + chapters.size());

// 5. 获取第 1 章正文
String content = service.getContent(r.getBookUrl(), r.getSource(), 0);
System.out.println("正文长度: " + content.length());

// 6. 批量下载前 10 章
List<ChapterContent> contents = service.batchDownload(r.getBookUrl(), r.getSource(), 0, 10);
for (ChapterContent ch : contents) {
    System.out.println("  " + ch.getTitle() + " : " + (ch.getContent() == null ? 0 : ch.getContent().length()));
}

// 7. 下载为完整文本
String fullText = service.batchDownloadAsString(
    r.getBookUrl(), r.getSource(), 0, 10, "\n\n"
);
```

### 漫画流程

```java
ReaderService service = ReaderService.getInstance();

// 搜索漫画
List<SearchResult> results = service.searchComic("哑舍");

SearchResult r = results.get(0);
System.out.println("漫画: " + r.getName());

// 获取目录
List<ChapterInfo> chapters = service.getChapterList(r.getBookUrl(), r.getSource());

// 获取第 1 章正文（图片 HTML）
String content = service.getContent(r.getBookUrl(), r.getSource(), 0);
int imgCount = content.split("<img").length - 1;
System.out.println("图片数: " + imgCount);
```

### 按作者搜索

```java
ReaderService service = ReaderService.getInstance();

List<SearchResult> results = service.searchNovelByAuthor("天蚕土豆");
for (SearchResult r : results) {
    System.out.println(r.getName() + " - " + r.getAuthor());
}
```

### Spring Boot 集成

引入依赖后直接使用即可，`ReaderService` 首次调用 `getInstance()` 时会自动预热 JS 引擎和 HTTP 客户端。

```java
@Service
public class BookService {

    private final ReaderService service = ReaderService.getInstance();

    public List<SearchResult> search(String keyword) {
        return service.searchNovel(keyword);
    }

    public String downloadChapter(String bookUrl, String source, int chapterIndex) {
        return service.getContent(bookUrl, source, chapterIndex);
    }
}
```

## 项目结构

```
reader-engine/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── cn/kong/app/engine/
│   │   │   │   ├── ReaderEngine.java                # 底层 API (直接操作书源对象)
│   │   │   │   ├── ReaderService.java               # 高层 API (通用方法，推荐使用)
│   │   │   │   └── dto/                             # 通用 DTO
│   │   │   │       ├── SourceInfo.java              #   书源信息
│   │   │   │       ├── SearchResult.java            #   搜索结果
│   │   │   │       ├── BookDetail.java              #   书籍详情
│   │   │   │       ├── ChapterInfo.java             #   章节信息
│   │   │   │       └── ChapterContent.java          #   章节正文
│   │   │   └── io/legado/app/                       # Java 工具类
│   │   │       ├── model/analyzeRule/QueryTTF.java #   TTF 字体解析
│   │   │       └── utils/                           #   Base64, TextUtils 等
│   │   ├── kotlin/io/legado/app/
│   │   │   ├── constant/                            # 常量定义
│   │   │   ├── data/entities/                       # 实体类 (Book, BookSource, ...)
│   │   │   │   └── rule/                            #   解析规则实体
│   │   │   ├── engine/ReaderEngineBridge.kt         # Kotlin→Java 桥接
│   │   │   ├── exception/                           # 异常定义
│   │   │   ├── help/                               # 工具 (JsExtensions, CacheManager, HTTP)
│   │   │   ├── init/                                # 初始化
│   │   │   ├── model/
│   │   │   │   ├── analyzeRule/                     # 解析规则 (Jsoup/XPath/JsonPath)
│   │   │   │   └── webBook/                         # WebBook 核心逻辑
│   │   │   └── utils/                               # 工具类
│   │   └── resources/
│   │       └── builtin/                             # 内置书源 JSON (8 个)
│   └── test/
│       └── java/cn/kong/app/
│           ├── engine/ReaderServiceTest.java         # 服务测试 (10 个)
│           └── model/
│               ├── NovelSourceTest.java              # 小说源测试
│               └── ComicSourceTest.java              # 漫画源测试
```

## 架构说明

### 调用链

```
Java 调用方
    ↓  (String 参数 + 通用 DTO)
ReaderService.java (单例，源管理 + 聚合搜索)
    ↓
ReaderEngine.java (静态方法)
    ↓
ReaderEngineBridge.kt (runBlocking 同步桥接)
    ↓
WebBook.kt (suspend 协程函数)
    ↓
AnalyzeUrl / AnalyzeRule (HTTP + 解析)
    ↓
Rhino JS 引擎 (执行书源 JS 规则)
```

### 关键设计

1. **分层架构**：`ReaderService`（高层服务）封装 `ReaderEngine`（底层引擎），高层使用 DTO + 简单参数，底层操作内部实体对象。

2. **通用 DTO**：用户不接触 `SearchBook`/`Book`/`BookChapter` 等内部对象，统一使用 `SourceInfo`/`SearchResult`/`BookDetail`/`ChapterInfo`/`ChapterContent`。

3. **Book 缓存**：`ReaderService` 内部缓存 Book 对象（`source + bookUrl` 为 key），避免获取目录和正文时重复请求详情页。

4. **Kotlin 协程桥接**：Kotlin 的 `WebBook.kt` 使用 `suspend` 函数，通过 `ReaderEngineBridge.kt` 的 `runBlocking` 桥接为同步方法。

5. **jsLib 预加载**：书源可定义 `jsLib`，在 `BaseSource.evalJS` 中自动拼接到用户 JS 前执行。

6. **漫画源 variable 初始化**：漫画源依赖 `variable` 存储 URL、cookie 等，加载时自动执行 `initSource()` 初始化。

7. **无 Spring Boot 依赖**：引擎本身不依赖 Spring Boot，`ReaderService` 首次调用 `getInstance()` 时通过 `static` 初始化块预热 JS 引擎和 HTTP 客户端，可在任何 Java 项目中使用。

## 构建

### 环境要求

- JDK 11+
- Maven 3.6+

### 编译打包

```bash
# 打包（跳过测试）— 生成 fat jar，包含所有运行时依赖
mvn package -DskipTests

# 运行测试
mvn test

# 运行特定测试
mvn test -Dtest="cn.kong.app.engine.ReaderServiceTest"
```

构建产物：`target/reader-engine-1.0.0.jar`（fat jar，包含 kotlin-stdlib、okhttp、jsoup、rhino、gson、hutool、kotlin-logging 等所有运行时依赖，不包含 slf4j-api）

## 测试

| 测试项 | 结果 |
|--------|------|
| 内置源加载 | 4 小说 + 4 漫画 = 8 个 |
| 搜索小说（斗破苍穹） | 128 本（4 源聚合） |
| 搜索漫画（哑舍） | 35 本（4 源聚合） |
| 搜索全部（斗破苍穹） | 186 本（8 源聚合） |
| 按作者搜索（天蚕土豆） | 通过 |
| 获取详情 | 通过 |
| 获取目录 | 通过 |
| 获取正文 | 通过 |
| 批量下载（3 章） | 通过 |
| 漫画完整流程 | 通过 |

## FAQ

### Q: 漫画源搜索报错？

漫画源在 `ReaderService` 加载时自动调用 `initSource()` 初始化 variable。如果使用底层 `ReaderEngine`，需手动调用：

```java
ReaderEngine.initSource(source);
```

### Q: 打包时 rhino 依赖找不到？

```bash
mvn install:install-file "-Dfile=lib/rhino-1.7.13-1.jar" "-DgroupId=com.github.gedoor" "-DartifactId=rhino-android" "-Dversion=1.7.13-1" "-Dpackaging=jar"
```

### Q: 报 NoClassDefFoundError: mu/KotlinLogging？

确保使用的是 fat jar（`mvn package` 打出的 `reader-engine-1.0.0.jar`），而不是普通 jar。项目使用 `maven-shade-plugin` 打包，所有运行时依赖都已包含在 jar 中。

## License

基于 [reader-dev](https://github.com/changshengyu/reader-dev) 和 [legado](https://github.com/gedoor/legado) 改造。
