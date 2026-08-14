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

| 组件 | 技术 |
|------|------|
| 语言 | Java 11 + Kotlin 1.8 |
| 构建 | Maven |
| HTTP | OkHttp 4.12 |
| HTML 解析 | Jsoup 1.17 + JsoupXpath 2.5 |
| JSON 解析 | Gson 2.10 + Jackson + JsonPath 2.9 |
| JS 引擎 | Mozilla Rhino 1.7.13 (定制版) |
| 协程 | Kotlin Coroutines 1.7 |
| 加密 | Hutool + BouncyCastle |
| 框架 | Spring Boot 2.7 (自动配置) |

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
BookSourceManager manager = BookSourceManager.getInstance();

// 搜索小说
List<SearchResult> results = manager.searchNovel("斗破苍穹");

// 获取详情
SearchResult r = results.get(0);
BookDetail detail = manager.getBookDetail(r.getBookUrl(), r.getSourceUrl());

// 获取目录
List<ChapterInfo> chapters = manager.getChapterList(r.getBookUrl(), r.getSourceUrl());

// 获取第 1 章正文
String content = manager.getContent(r.getBookUrl(), r.getSourceUrl(), 0);

// 批量下载前 10 章
List<String> contents = manager.batchDownload(r.getBookUrl(), r.getSourceUrl(), 0, 10);
```

## 内置书源

### 小说源（4 个）

| 源名称 | URL | 类型 |
|--------|-----|------|
| 八零小说 | `http://www.80ge.info` | novel |
| 独步小说 | `https://www.dbxsd.com` | novel |
| 猫眼看书 | `http://api.lemiyigou.com` | novel |
| 七猫小说 | `https://api-bc.wtzw.com` | novel |

### 漫画源（4 个）

| 源名称 | URL | 类型 |
|--------|-----|------|
| G站漫画 | `https://godamanga.com` | comic |
| 漫画台 | `https://m.manhuatai.com` | comic |
| 如漫画 | `https://www.rumanhua.com` | comic |
| 再漫画 | `https://www.zaimanhua.com` | comic |

## API 文档

### BookSourceManager（推荐）

单例管理器，用户只需传入 `String` 参数和简单 DTO，无需接触内部实体对象。

#### 获取实例

```java
BookSourceManager manager = BookSourceManager.getInstance();
```

#### 1. 导入书源

| 方法 | 参数 | 说明 |
|------|------|------|
| `importSources(String json)` | 书源 JSON 字符串 | 导入单个或数组格式 |
| `importSources(InputStream is)` | 输入流 | 从文件导入 |
| `removeSource(String sourceUrl)` | 书源 URL | 移除指定源 |
| `clearAllSources()` | — | 清除所有源 |
| `reloadBuiltinSources()` | — | 重新加载内置源 |

#### 2. 列出书源

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getSourceCount()` | `int` | 书源总数 |
| `listSourceNames()` | `List<String>` | 所有源名称 |
| `listNovelSourceNames()` | `List<String>` | 小说源名称 |
| `listComicSourceNames()` | `List<String>` | 漫画源名称 |
| `listAllSources()` | `List<Map<String, Object>>` | 所有源详情 (name, url, type, typeDesc) |

#### 3. 搜索

| 方法 | 参数 | 说明 |
|------|------|------|
| `search(String keyword)` | 关键词 | 跨所有源搜索 |
| `search(String keyword, int page)` | 关键词, 页码 | 指定页码 |
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
| `sourceUrl` | String | 书源 URL（后续操作需要） |
| `sourceName` | String | 书源名称 |
| `coverUrl` | String | 封面 URL |
| `intro` | String | 简介 |
| `kind` | String | 分类 |
| `wordCount` | String | 字数 |
| `type` | int | 0=小说, 2=漫画 |

#### 4. 按作者搜索

| 方法 | 参数 | 说明 |
|------|------|------|
| `searchByAuthor(String author)` | 作者名 | 跨所有源 |
| `searchNovelByAuthor(String author)` | 作者名 | 仅小说源 |
| `searchComicByAuthor(String author)` | 作者名 | 仅漫画源 |

返回 `List<SearchResult>`，自动过滤匹配作者的结果。

#### 5. 获取详情

| 方法 | 参数 | 说明 |
|------|------|------|
| `getBookDetail(String bookUrl, String sourceUrl)` | 书籍URL, 书源URL | 获取书籍详情 |

返回 `BookDetail`，包含：name, author, bookUrl, tocUrl, sourceUrl, sourceName, coverUrl, intro, kind, wordCount, latestChapterTitle, type。

#### 6. 获取目录

| 方法 | 参数 | 说明 |
|------|------|------|
| `getChapterList(String bookUrl, String sourceUrl)` | 书籍URL, 书源URL | 获取章节列表 |

返回 `List<ChapterInfo>`，每个章节包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | String | 章节标题 |
| `url` | String | 章节 URL |
| `index` | int | 章节序号（从 0 开始） |

#### 7. 获取正文

| 方法 | 参数 | 说明 |
|------|------|------|
| `getContent(bookUrl, sourceUrl, chapterIndex)` | 书籍URL, 书源URL, 章节序号 | 按序号获取正文 |
| `getContentByUrl(bookUrl, sourceUrl, chapterUrl)` | 书籍URL, 书源URL, 章节URL | 按URL获取正文 |

返回 `String` 正文内容（漫画为图片 HTML）。

#### 8. 批量下载

| 方法 | 参数 | 说明 |
|------|------|------|
| `batchDownload(bookUrl, sourceUrl, start, end)` | 书籍URL, 书源URL, 起始序号, 结束序号(不含) | 下载范围 |
| `batchDownload(bookUrl, sourceUrl, start, end, delayMs)` | 同上+间隔毫秒 | 带延迟 |
| `batchDownloadAll(bookUrl, sourceUrl)` | 书籍URL, 书源URL | 下载全部 |
| `batchDownloadAsMap(bookUrl, sourceUrl, start, end)` | 同上 | 返回 `Map<标题, 正文>` |
| `batchDownloadAsString(bookUrl, sourceUrl, start, end, sep)` | 同上+分隔符 | 拼接为完整文本 |

返回 `List<String>`（`batchDownloadAsMap` 返回 `Map<String,String>`，`batchDownloadAsString` 返回 `String`）。

### ReaderEngine（底层 API）

如果需要直接操作书源对象，可使用底层 API：

| 方法 | 说明 |
|------|------|
| `parseBookSource(String json)` | 解析单个书源 |
| `parseBookSources(String json)` | 解析书源列表 |
| `initSource(BookSource source)` | 初始化书源 |
| `search(BookSource, String key, Integer page)` | 搜索 |
| `getBookInfo(BookSource, String bookUrl)` | 获取详情 |
| `getChapterList(BookSource, Book book)` | 获取目录 |
| `getBookContent(BookSource, Book, BookChapter)` | 获取正文 |

## 使用示例

### 完整小说流程

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 1. 搜索
List<SearchResult> results = manager.searchNovel("斗破苍穹");
System.out.println("搜索结果: " + results.size() + " 本");

// 2. 取第一本
SearchResult r = results.get(0);
System.out.println("书名: " + r.getName() + " | 作者: " + r.getAuthor());

// 3. 获取详情
BookDetail detail = manager.getBookDetail(r.getBookUrl(), r.getSourceUrl());
System.out.println("简介: " + detail.getIntro());

// 4. 获取目录
List<ChapterInfo> chapters = manager.getChapterList(r.getBookUrl(), r.getSourceUrl());
System.out.println("章节数: " + chapters.size());

// 5. 获取第 1 章正文
String content = manager.getContent(r.getBookUrl(), r.getSourceUrl(), 0);
System.out.println("正文长度: " + content.length());

// 6. 批量下载前 10 章
List<String> contents = manager.batchDownload(r.getBookUrl(), r.getSourceUrl(), 0, 10);

// 7. 下载为完整文本
String fullText = manager.batchDownloadAsString(
    r.getBookUrl(), r.getSourceUrl(), 0, 10, "\n\n"
);
```

### 漫画流程

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 搜索漫画
List<SearchResult> results = manager.searchComic("哑舍");

SearchResult r = results.get(0);
System.out.println("漫画: " + r.getName());

// 获取目录
List<ChapterInfo> chapters = manager.getChapterList(r.getBookUrl(), r.getSourceUrl());

// 获取第 1 章正文（图片 HTML）
String content = manager.getContent(r.getBookUrl(), r.getSourceUrl(), 0);
int imgCount = content.split("<img").length - 1;
System.out.println("图片数: " + imgCount);
```

### 按作者搜索

```java
BookSourceManager manager = BookSourceManager.getInstance();

List<SearchResult> results = manager.searchNovelByAuthor("天蚕土豆");
for (SearchResult r : results) {
    System.out.println(r.getName() + " - " + r.getAuthor());
}
```

### 导入外部书源

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 从 JSON 字符串导入
String json = "[{\"bookSourceName\":\"新源\", \"bookSourceUrl\":\"https://...\", ...}]";
int count = manager.importSources(json);
System.out.println("导入了 " + count + " 个源");

// 从文件导入
try (InputStream is = new FileInputStream("my_source.json")) {
    manager.importSources(is);
}

// 列出所有源
for (Map<String, Object> source : manager.listAllSources()) {
    System.out.println(source.get("name") + " | " + source.get("typeDesc"));
}
```

### Spring Boot 集成

引入依赖后自动生效，`ReaderEngineAutoConfiguration` 会在启动时预热 JS 引擎和 HTTP 客户端。

```java
@Service
public class BookService {

    private final BookSourceManager manager = BookSourceManager.getInstance();

    public List<SearchResult> search(String keyword) {
        return manager.searchNovel(keyword);
    }

    public String downloadChapter(String bookUrl, String sourceUrl, int chapterIndex) {
        return manager.getContent(bookUrl, sourceUrl, chapterIndex);
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
│   │   │   ├── cn/kong/app/
│   │   │   │   ├── ReaderEngineAutoConfiguration.java   # Spring Boot 自动配置
│   │   │   │   └── engine/
│   │   │   │       ├── ReaderEngine.java                # 底层 API (直接操作书源对象)
│   │   │   │       ├── BookSourceManager.java           # 高层 API (通用方法，推荐使用)
│   │   │   │       └── dto/                             # 通用 DTO
│   │   │   │           ├── SearchResult.java            #   搜索结果
│   │   │   │           ├── BookDetail.java              #   书籍详情
│   │   │   │           └── ChapterInfo.java            #   章节信息
│   │   │   └── io/legado/app/                           # Java 工具类
│   │   │       ├── model/analyzeRule/QueryTTF.java     #   TTF 字体解析
│   │   │       └── utils/                               #   Base64, TextUtils 等
│   │   ├── kotlin/io/legado/app/
│   │   │   ├── constant/                                # 常量定义
│   │   │   ├── data/entities/                            # 实体类 (Book, BookSource, ...)
│   │   │   │   └── rule/                                 #   解析规则实体
│   │   │   ├── engine/ReaderEngineBridge.kt              # Kotlin→Java 桥接
│   │   │   ├── exception/                               # 异常定义
│   │   │   ├── help/                                    # 工具 (JsExtensions, CacheManager, HTTP)
│   │   │   ├── init/                                    # 初始化
│   │   │   ├── model/
│   │   │   │   ├── analyzeRule/                        # 解析规则 (Jsoup/XPath/JsonPath)
│   │   │   │   └── webBook/                            # WebBook 核心逻辑
│   │   │   └── utils/                                  # 工具类
│   │   └── resources/
│   │       ├── META-INF/spring.factories               # Spring Boot 自动配置注册
│   │       └── builtin/                                # 内置书源 JSON (8 个)
│   └── test/
│       └── java/cn/kong/app/
│           ├── engine/BookSourceManagerTest.java        # 管理器测试 (10 个)
│           └── model/
│               ├── NovelSourceTest.java                 # 小说源测试
│               └── ComicSourceTest.java                 # 漫画源测试
```

## 架构说明

### 调用链

```
Java 调用方
    ↓  (String 参数 + 通用 DTO)
BookSourceManager.java (单例，源管理 + 聚合搜索)
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

1. **通用 DTO**：用户不接触 `SearchBook`/`Book`/`BookChapter` 等内部对象，统一使用 `SearchResult`/`BookDetail`/`ChapterInfo` 和 `String`/`int` 参数。

2. **Book 缓存**：`BookSourceManager` 内部缓存 Book 对象（`sourceUrl + bookUrl` 为 key），避免获取目录和正文时重复请求详情页。

3. **Kotlin 协程桥接**：Kotlin 的 `WebBook.kt` 使用 `suspend` 函数，通过 `ReaderEngineBridge.kt` 的 `runBlocking` 桥接为同步方法。

4. **jsLib 预加载**：书源可定义 `jsLib`，在 `BaseSource.evalJS` 中自动拼接到用户 JS 前执行。

5. **漫画源 variable 初始化**：漫画源依赖 `variable` 存储 URL、cookie 等，加载时自动执行 `initSource()` 初始化。

## 构建

### 环境要求

- JDK 11+
- Maven 3.6+

### 编译打包

```bash
# 打包（跳过测试）
mvn package -DskipTests

# 运行测试
mvn test

# 运行特定测试
mvn test -Dtest="cn.kong.app.engine.BookSourceManagerTest"
```

构建产物：`target/reader-engine-1.0.0.jar`

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

漫画源在 `BookSourceManager` 加载时自动调用 `initSource()` 初始化 variable。如果使用底层 `ReaderEngine`，需手动调用：

```java
ReaderEngine.initSource(source);
```

### Q: 如何添加自定义书源？

```java
manager.importSources(jsonString);
// 或
manager.importSources(new FileInputStream("source.json"));
```

### Q: 打包时 rhino 依赖找不到？

```bash
mvn install:install-file "-Dfile=lib/rhino-1.7.13-1.jar" "-DgroupId=com.github.gedoor" "-DartifactId=rhino-android" "-Dversion=1.7.13-1" "-Dpackaging=jar"
```

### Q: Spring Boot 项目中如何使用？

引入依赖后自动生效。如需禁用：

```java
@SpringBootApplication(exclude = ReaderEngineAutoConfiguration.class)
```

## License

基于 [reader-dev](https://github.com/changshengyu/reader-dev) 和 [legado](https://github.com/gedoor/legado) 改造。
