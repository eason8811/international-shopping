package shopping.international.types.utils;

import lombok.Data;
import shopping.international.types.exceptions.URIFormatException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static shopping.international.types.utils.FieldValidateUtils.requireNotBlank;

/**
 * URI 路径类, 可以替换路径参数
 */
@Data
public final class URI {
    /**
     * URI 表达式 (可带路径参数)
     */
    private String pattern;
    /**
     * 路径参数个数
     */
    private int variableCount;

    /**
     * 私有构造函数
     *
     * @param pattern URI 表达式 (可带路径参数)
     */
    private URI(String pattern) {
        requireNotBlank(pattern, "URI 路径不能为空");
        validate(pattern);
        this.pattern = pattern;
    }

    /**
     * 创建一个 <code>Uri</code> 对象, 该对象表示由给定模式定义的 URI 路径
     *
     * @param pattern URI 表达式 (可带路径参数), 必须非空
     * @return 新创建的 {@link URI} 实例
     * @throws IllegalArgumentException 如果 <code>pattern</code> 为空
     */
    public static URI of(String pattern) {
        return new URI(pattern);
    }

    /**
     * 用给定的值替换 URI 模式中的指定路径变量
     *
     * @param pathVariableName 要被替换的路径变量名称, 必须与 URI 模式中定义的一致
     * @param value            替换路径变量的值
     * @return 替换后的完整 URI 字符串
     * @throws URIFormatException 如果 URI 中定义的路径参数个数不为 1 个
     */
    public String fill(String pathVariableName, String value) {
        requireNotBlank(pathVariableName, "变量名称不能为空");
        if (variableCount != 1)
            throw new URIFormatException("URI 路径参数个数为: " + variableCount + " 个, 但实际传输了 1 个");
        if (!pattern.contains("{%s}".formatted(pathVariableName)))
            throw new URIFormatException("URI 模版中不存在 '" + pathVariableName + "' 这个路径变流量");
        return pattern.replace("{%s}".formatted(pathVariableName), value);
    }

    /**
     * 用给定的两个值替换 URI 模式中的指定路径变量
     *
     * @param pathVariableName1 第一个要被替换的路径变量名称, 必须与 URI 模式中定义的一致
     * @param value1            替换第一个路径变量的值
     * @param pathVariableName2 第二个要被替换的路径变量名称, 必须与 URI 模式中定义的一致
     * @param value2            替换第二个路径变量的值
     * @return 替换后的完整 URI 字符串
     * @throws URIFormatException 如果 URI 中定义的路径参数个数不为 2 个
     */
    public String fill(String pathVariableName1, String value1, String pathVariableName2, String value2) {
        requireNotBlank(pathVariableName1, "变量名称不能为空");
        requireNotBlank(pathVariableName2, "变量名称不能为空");
        if (variableCount != 2)
            throw new URIFormatException("URI 路径参数个数为: " + variableCount + " 个, 但实际传输了 2 个");
        if (!pattern.contains("{%s}".formatted(pathVariableName1)))
            throw new URIFormatException("URI 模版中不存在 '" + pathVariableName1 + "' 这个路径变流量");
        if (!pattern.contains("{%s}".formatted(pathVariableName2)))
            throw new URIFormatException("URI 模版中不存在 '" + pathVariableName2 + "' 这个路径变流量");
        pattern = pattern.replace("{%s}".formatted(pathVariableName1), value1);
        pattern = pattern.replace("{%s}".formatted(pathVariableName2), value2);
        return pattern;
    }

    /**
     * 用给定的值替换 URI 模式中的所有指定路径变量
     *
     * @param nameValueMap 包含路径变量名及其对应值的映射, 键为路径变量名称, 值为要替换的具体值
     * @return 替换后的完整 URI 字符串
     * @throws URIFormatException 如果传入的 <code>nameValueMap</code> 中的键与 URI 模板中定义的路径参数不匹配,
     *                            或者 <code>nameValueMap</code> 的大小与 URI 模板中定义的路径参数数量不一致
     */
    public String fill(Map<String, String> nameValueMap) {
        if (variableCount != nameValueMap.size())
            throw new URIFormatException("URI 路径参数个数为: " + variableCount + " 个, 但实际传输了 " + nameValueMap.size() + " 个");
        for (Map.Entry<String, String> entry : nameValueMap.entrySet()) {
            requireNotBlank(entry.getKey(), "变量名称不能为空");
            if (!pattern.contains("{%s}".formatted(entry.getKey())))
                throw new URIFormatException("URI 模版中不存在 '" + entry.getValue() + "' 这个路径变流量");
            pattern = pattern.replace("{%s}".formatted(entry.getKey()), entry.getValue());
        }
        return pattern;
    }

    /**
     * 验证给定的 URI 模式是否符合规范, 主要是检查花括号 <code>{</code> 和 <code>}</code> 的配对情况
     *
     * @param pattern 要验证的 URI 模板字符串, 可能包含路径参数, 以花括号形式表示
     * @throws URIFormatException 如果 URI 模板格式错误, 即花括号不正确配对时抛出此异常
     */
    private void validate(String pattern) {
        if (!pattern.contains("{") && !pattern.contains("}"))
            return;
        List<Character> stack = new ArrayList<>();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if ('{' == c && !stack.isEmpty())
                throw new URIFormatException("URI 模版格式错误: ‘" + pattern + "'");
            if ('{' == c)
                stack.add(c);
            else if ('}' == c) {
                stack.remove(stack.size() - 1);
                variableCount++;
            }
        }
    }

    @Override
    public String toString() {
        return pattern;
    }
}
