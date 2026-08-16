package cn.kong.app.engine.dto;

/**
 * 书源信息 - 通用 DTO
 * <p>
 * 用于 listAllSources / listNovelSources / listComicSources 返回结果。
 */
public class SourceInfo {

    /** 书源简称（如 novel_1、comic_2 等） */
    private String source;
    /** 书源名称 */
    private String name;
    /** 类型：0=小说, 2=漫画 */
    private int type;
    /** 类型描述：小说 / 漫画 */
    private String typeDesc;

    public SourceInfo() {}

    public SourceInfo(String source, String name, int type, String typeDesc) {
        this.source = source;
        this.name = name;
        this.type = type;
        this.typeDesc = typeDesc;
    }

    // Getters & Setters

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getTypeDesc() { return typeDesc; }
    public void setTypeDesc(String typeDesc) { this.typeDesc = typeDesc; }

    @Override
    public String toString() {
        return "SourceInfo{" +
                "source='" + source + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", typeDesc='" + typeDesc + '\'' +
                '}';
    }
}
