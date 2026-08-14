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

### 1. 引入依赖

将 `reader-engine-1.0.0.jar` 安装到本地 Maven 仓库：

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

> **注意**：PowerShell 不支持 `\` 换行，且含 `.` 的参数值需要用引号包裹，否则会被截断。

然后在项目的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>cn.kong</groupId>
    <artifactId>reader-engine</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 使用 BookSourceManager（推荐）

`BookSourceManager` 是高层管理 API，内置 4 个小说源 + 4 个漫画源，开箱即用：

```java
import cn.kong.app.engine.BookSourceManager;
import io.legado.app.data.entities.*;

// 获取管理器（自动加载 8 个内置源）
BookSourceManager manager = BookSourceManager.getInstance();

// 搜索小说
List<SearchBook> results = manager.searchNovel("斗破苍穹");

// 获取详情
Book book = manager.getBookInfo(results.get(0));

// 获取目录
List<BookChapter> chapters = manager.getChapterList(book);

// 获取正文
String content = manager.getBookContent(book, chapters.get(0));

// 批量下载前 10 章
List<String> contents = manager.batchDownloadContent(book, chapters.subList(0, 10));
```

### 3. 使用 ReaderEngine（底层 API）

如果只需要操作单个书源，可以直接使用 `ReaderEngine`：

```java
import cn.kong.app.engine.ReaderEngine;
import io.legado.app.data.entities.*;

// 从 JSON 创建书源
BookSource source = ReaderEngine.parseBookSource(jsonString);

// 初始化书源（漫画源必须）
ReaderEngine.initSource(source);

// 搜索
List<SearchBook> results = ReaderEngine.search(source, "斗破苍穹", 1);

// 获取详情
Book book = ReaderEngine.getBookInfo(source, results.get(0).getBookUrl());

// 获取目录
List<BookChapter> chapters = ReaderEngine.getChapterList(source, book);

// 获取正文
String content = ReaderEngine.getBookContent(source, book, chapters.get(0));
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

> 漫画源在加载时自动执行 `initSource()` 初始化 variable（URL、cookie 等），无需手动调用。

## API 文档

### BookSourceManager（推荐使用）

单例管理器，提供源管理 + 聚合搜索 + 批量下载等高层 API。

#### 源管理

| 方法 | 说明 |
|------|------|
| `getInstance()` | 获取单例（自动加载内置源） |
| `importSources(String json)` | 从 JSON 导入书源（支持单个或数组） |
| `importSources(InputStream is)` | 从输入流导入书源 |
| `importSource(BookSource source)` | 导入单个书源对象 |
| `removeSource(String bookSourceUrl)` | 移除指定书源 |
| `clearAllSources()` | 清除所有书源 |
| `reloadBuiltinSources()` | 重新加载内置源 |

#### 列出源

| 方法 | 说明 |
|------|------|
| `listAllSources()` | 所有已注册的书源 |
| `listNovelSources()` | 小说源（type=0） |
| `listComicSources()` | 漫画源（type=2） |
| `getSource(String url)` | 按URL获取书源 |
| `getSourceCount()` | 书源总数 |

#### 搜索

| 方法 | 说明 |
|------|------|
| `searchNovel(String key)` | 跨所有小说源聚合搜索 |
| `searchNovel(String key, int page)` | 聚合搜索指定页 |
| `searchComic(String key)` | 跨所有漫画源聚合搜索 |
| `searchComic(String key, int page)` | 漫画聚合搜索指定页 |
| `searchAll(String key)` | 跨所有源搜索（小说+漫画） |
| `searchAll(String key, int page)` | 全源搜索指定页 |
| `search(BookSource source, String key)` | 在指定源中搜索 |

#### 按作者搜索

| 方法 | 说明 |
|------|------|
| `searchNovelByAuthor(String author)` | 按作者搜索小说 |
| `searchComicByAuthor(String author)` | 按作者搜索漫画 |
| `searchAllByAuthor(String author)` | 按作者搜索全部源 |

#### 详情 / 目录 / 正文

| 方法 | 说明 |
|------|------|
| `getBookInfo(SearchBook sb)` | 从搜索结果获取详情 |
| `getBookInfo(Book book)` | 刷新已有 Book 详情 |
| `getBookInfo(String sourceUrl, String bookUrl)` | 指定源获取详情 |
| `getChapterList(Book book)` | 获取章节目录 |
| `getBookContent(Book book, BookChapter chapter)` | 获取章节正文 |

#### 批量下载

| 方法 | 说明 |
|------|------|
| `batchDownloadContent(Book, List<BookChapter>)` | 批量下载，返回 `List<String>` |
| `batchDownloadContent(Book, List<BookChapter>, long delayMs)` | 带间隔延迟 |
| `batchDownloadContentAsMap(Book, List<BookChapter>)` | 返回 `Map<标题, 正文>` |
| `batchDownloadContentAsString(Book, List<BookChapter>, String sep)` | 拼接为完整文本 |

### ReaderEngine（底层 API）

提供书源解析、初始化、搜索、详情、目录、正文的静态方法。

| 方法 | 说明 |
|------|------|
| `parseBookSource(String json)` | 解析单个书源 |
| `parseBookSources(String json)` | 解析书源列表 |
| `parseBookSources(InputStream is)` | 从流解析书源列表 |
| `initSource(BookSource source)` | 初始化书源（漫画源必须） |
| `search(BookSource, String key, Integer page)` | 搜索 |
| `explore(BookSource, String url, Integer page)` | 发现 |
| `getBookInfo(BookSource, String bookUrl)` | 获取详情 |
| `getChapterList(BookSource, Book book)` | 获取目录 |
| `getBookContent(BookSource, Book, BookChapter)` | 获取正文 |
| `setVariable(BookSource, String variable)` | 设置 variable |
| `getVariable(BookSource)` | 获取 variable |
| `evalJS(BookSource, String jsStr)` | 执行 JS 代码 |

## 使用示例

### 完整小说流程

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 1. 搜索
List<SearchBook> results = manager.searchNovel("斗破苍穹");
System.out.println("搜索结果: " + results.size() + " 本");

// 2. 详情
Book book = manager.getBookInfo(results.get(0));
System.out.println("书名: " + book.getName());
System.out.println("简介: " + book.getIntro());

// 3. 目录
List<BookChapter> chapters = manager.getChapterList(book);
System.out.println("章节数: " + chapters.size());

// 4. 批量下载前 5 章
List<String> contents = manager.batchDownloadContent(book, chapters.subList(0, 5));
for (int i = 0; i < contents.size(); i++) {
    System.out.println("第" + (i+1) + "章: " + contents.get(i).length() + " 字");
}

// 5. 下载为完整文本
String fullText = manager.batchDownloadContentAsString(
    book, chapters.subList(0, 10), "\n\n"
);
```

### 漫画流程

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 搜索漫画
List<SearchBook> results = manager.searchComic("哑舍");

// 获取详情
Book book = manager.getBookInfo(results.get(0));

// 获取目录
List<BookChapter> chapters = manager.getChapterList(book);

// 获取正文（漫画正文为图片 HTML）
String content = manager.getBookContent(book, chapters.get(0));
int imgCount = content.split("<img").length - 1;
System.out.println("图片数: " + imgCount);
```

### 按作者搜索

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 按作者搜索小说
List<SearchBook> results = manager.searchNovelByAuthor("天蚕土豆");
for (SearchBook sb : results) {
    System.out.println(sb.getName() + " - " + sb.getAuthor());
}
```

### 导入外部书源

```java
BookSourceManager manager = BookSourceManager.getInstance();

// 从 JSON 字符串导入
String json = "[{\"bookSourceName\":\"新源\", \"bookSourceUrl\":\"https://...\", ...}]";
List<BookSource> imported = manager.importSources(json);

// 从文件导入
try (InputStream is = new FileInputStream("my_source.json")) {
    manager.importSources(is);
}

// 列出所有源（内置 + 导入的）
List<BookSource> all = manager.listAllSources();
```

### Spring Boot 集成

引入依赖后，Spring Boot 会自动加载 `ReaderEngineAutoConfiguration`，预热 JS 引擎和 HTTP 客户端：

```yaml
# application.yml（可选配置）
logging:
  level:
    io.legado.app: DEBUG
    cn.kong.app: INFO
```

```java
@Service
public class BookService {

    private final BookSourceManager manager = BookSourceManager.getInstance();

    public List<SearchBook> search(String keyword) {
        return manager.searchNovel(keyword);
    }

    public String downloadChapter(String bookUrl, int chapterIndex) {
        // 实现逻辑...
    }
}
```

## 项目结构

```
reader-engine/
├── pom.xml                          # Maven 构建配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cn/kong/app/
│   │   │       ├── ReaderEngineAutoConfiguration.java   # Spring Boot 自动配置
│   │   │       └── engine/
│   │   │           ├── ReaderEngine.java                # Java 门面 (底层 API)
│   │   │           └── BookSourceManager.java           # 书源管理器 (高层 API)
│   │   ├── kotlin/
│   │   │   └── io/legado/app/
│   │   │       ├── data/entities/                       # 实体类 (Book, BookSource, ...)
│   │   │       ├── engine/
│   │   │       │   └── ReaderEngineBridge.kt            # Kotlin→Java 桥接
│   │   │       ├── model/
│   │   │       │   ├── analyzeRule/                    # 解析规则 (Jsoup/XPath/JsonPath)
│   │   │       │   └── webBook/                        # WebBook 核心逻辑
│   │   │       ├── help/                               # 工具 (JsExtensions, CacheManager, ...)
│   │   │       └── utils/                               # 工具类
│   │   └── resources/
│   │       ├── META-INF/spring.factories               # Spring Boot 自动配置注册
│   │       └── builtin/                                # 内置书源 JSON
│   │           ├── novel_80.json
│   │           ├── novel_dubu.json
│   │           ├── novel_maoyan.json
│   │           ├── novel_qimao.json
│   │           ├── comic_godamanga.json
│   │           ├── comic_manhuatai.json
│   │           ├── comic_rumanhua.json
│   │           └── comic_zaimanhua.json
│   └── test/
│       ├── java/cn/kong/app/
│       │   ├── engine/BookSourceManagerTest.java        # 管理器测试
│       │   └── model/
│       │       ├── NovelSourceTest.java                 # 小说源测试
│       │       └── ComicSourceTest.java                 # 漫画源测试
│       └── resources/                                   # 测试书源 JSON
```

## 架构说明

### 编译流程

```
Kotlin 编译器 (process-sources 阶段)
    ↓ 编译 src/main/kotlin → target/classes
Java 编译器 (compile 阶段)
    ↓ 编译 src/main/java → target/classes
    ↓ (依赖 Kotlin 编译产物)
最终 JAR 包
```

### 调用链

```
Java 调用方
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

1. **Kotlin 协程桥接**：Kotlin 的 `WebBook.kt` 使用 `suspend` 函数，Java 无法直接调用。`ReaderEngineBridge.kt` 通过 `runBlocking` 桥接为同步方法。

2. **jsLib 预加载**：书源可定义 `jsLib`（自定义 JS 函数库），在 `BaseSource.evalJS` 中自动拼接到用户 JS 前面执行，解决 Map 函数覆盖 ES6 Map 构造器的问题。

3. **漫画源 variable 初始化**：漫画源依赖 `variable` 存储 URL、cookie 等配置。`initSource()` 执行 `loginUrl` 中的 JS 代码自动设置 variable。

4. **AES 加密兼容**：Java 不支持 `PKCS7Padding`，引擎在 `EncoderUtils.kt` 中自动替换为 `PKCS5Padding`。

## 构建

### 环境要求

- JDK 11+
- Maven 3.6+
- Kotlin 1.8 (由 Maven 插件自动管理)

### 编译打包

```bash
# 跳过测试打包
mvn package -DskipTests

# 运行测试
mvn test

# 运行特定测试
mvn test -Dtest="cn.kong.app.engine.BookSourceManagerTest"
```

构建产物：`target/reader-engine-1.0.0.jar`（约 600KB）

## 测试

### 测试覆盖

| 测试类 | 测试内容 |
|--------|----------|
| `BookSourceManagerTest` | 管理器全流程（10 个测试） |
| `NovelSourceTest` | 4 小说源 × 5 关键词 |
| `ComicSourceTest` | 4 漫画源 × 5 关键词 |

### 测试结果

| 测试项 | 结果 |
|--------|------|
| 内置源加载 | 4 小说 + 4 漫画 = 8 个 |
| 搜索小说（斗破苍穹） | 128 本（4 源聚合） |
| 搜索漫画（哑舍） | 35 本（4 源聚合） |
| 搜索全部（斗破苍穹） | 186 本（8 源聚合） |
| 详情 → 目录 → 正文 | 全流程通过 |
| 批量下载（3 章） | 通过 |

## FAQ

### Q: 漫画源搜索报错怎么办？

漫画源必须先调用 `initSource()` 初始化 variable。使用 `BookSourceManager` 时会自动处理，使用 `ReaderEngine` 时需手动调用：

```java
ReaderEngine.initSource(source);  // 必须在搜索前调用
```

### Q: 如何添加自定义书源？

```java
// 方式 1: JSON 字符串
manager.importSources(jsonString);

// 方式 2: 文件流
manager.importSources(new FileInputStream("source.json"));
```

书源 JSON 格式参考 [阅读 App 书源格式](https://github.com/gedoor/legado)。

### Q: 打包时 rhino 依赖找不到？

`rhino-android` 是定制版，需安装到本地仓库：

```bash
mvn install:install-file -Dfile=lib/rhino-1.7.13-1.jar \
  -DgroupId=com.github.gedoor -DartifactId=rhino-android \
  -Dversion=1.7.13-1 -Dpackaging=jar
```

### Q: Spring Boot 项目中如何使用？

引入依赖后自动生效。`ReaderEngineAutoConfiguration` 会在启动时预热 JS 引擎和 HTTP 客户端。如需禁用自动配置：

```java
@SpringBootApplication(exclude = ReaderEngineAutoConfiguration.class)
```

## License

基于 [reader-dev](https://github.com/changshengyu/reader-dev) 和 [legado](https://github.com/gedoor/legado) 改造。
