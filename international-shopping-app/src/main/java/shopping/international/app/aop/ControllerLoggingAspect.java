package shopping.international.app.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLoggingAspect {

    /**
     * <p>对象映射器, 用于序列化和反序列化 Java 对象到 JSON 格式以及从 JSON 格式转换回 Java 对象</p>
     */
    private final ObjectMapper objectMapper;

    /**
     * 匹配 trigger.controller 包及其子包内的所有公共方法
     */
    @Pointcut("execution(public * shopping.international.trigger.controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * 在匹配的控制器方法执行前后记录日志信息, 包括方法名, 输入参数和输出结果
     *
     * @param joinPoint 当前正在被通知的方法调用上下文
     * @return 被通知方法的返回值
     * @throws Throwable 如果被通知方法抛出异常, 则该异常会被重新抛出
     */
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        List<String> methodClassNamePartList = Arrays.asList(joinPoint.getSignature().getDeclaringType().getName().split("\\."));
        String methodClassSimpledName = String.join(".", methodClassNamePartList.subList(2, methodClassNamePartList.size()));
        Method method = methodSignature.getMethod();
        String methodName = method.getName();

        String[] paramNames = methodSignature.getParameterNames();  // 需要 -parameters 才能拿到实际参数名
        Object[] args = joinPoint.getArgs();

        // 入参日志
        String inKv = buildInKv(paramNames, args);
        if (log.isInfoEnabled())
            log.debug("正在执行 [{} # {}] 方法, 参数为: {}", methodClassSimpledName, methodName, inKv.isEmpty() ? "-" : inKv);

        long start = System.currentTimeMillis();
        Object ret = joinPoint.proceed();
        long end = System.currentTimeMillis();

        // 出参日志（统一以 result=... 打印）
        String outKv = "result=" + toStringSafe(sanitize(ret));
        if (log.isInfoEnabled()) {
            log.debug("方法结束 [{} # {}] 耗时 {} ms, 输出参数为: {}", methodClassSimpledName, methodName, end - start, outKv);
        }
        return ret;
    }

    /**
     * 构建键值对格式的字符串, 用于日志记录中的参数展示。此方法会根据给定的参数名称数组和参数值数组生成形如 "name=value" 的键值对列表,
     * 并将这些键值对以逗号分隔的形式返回。对于敏感信息(例如密码、令牌等), 其值会被替换为占位符, 以避免泄露。
     *
     * @param names 参数名数组, 如果为空或长度不足, 则使用默认命名方式 "arg0", "arg1" 等等
     * @param args  参数值数组, 每个元素代表一个参数的实际值
     * @return 由所有参数组成的键值对字符串, 形如 "name1=value1, name2=value2, ...", 若无参数则返回空字符串
     */
    private String buildInKv(String[] names, Object[] args) {
        if (args == null || args.length == 0)
            return "";
        List<String> pairs = new ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String name;
            if (names != null && i < names.length && names[i] != null && !names[i].isBlank())
                name = names[i];
            else
                name = "arg" + i;

            // 简单脱敏: 参数名命中关键词则隐藏值
            String lower = name.toLowerCase();
            boolean sensitive = lower.contains("password") || lower.contains("pwd")
                    || lower.contains("token") || lower.contains("secret") || lower.contains("code");

            Object value = sensitive ? "**** Sensitive Value ****" : sanitize(args[i]);
            pairs.add(name + "=" + toStringSafe(value));
        }
        return String.join(", ", pairs);
    }

    /**
     * 对给定的对象进行处理, 以返回一个更安全或更适合日志记录的表示形式
     *
     * @param value 需要被处理的对象, 可能是 <code>null</code>, {@link HttpServletRequest}, {@link HttpServletResponse}, {@link MultipartFile} 或其他类型
     * @return 根据对象类型不同返回不同的结果:
     * <ul>
     *     <li>如果 <code>value</code> 是 <code>null</code>, 则直接返回 <code>null</code></li>
     *     <li>如果 <code>value</code> 是 {@link HttpServletRequest} 的实例, 返回一个包含请求方法和请求URI的映射</li>
     *     <li>如果 <code>value</code> 是 {@link HttpServletResponse} 的实例, 返回 "{HttpServletResponse}" 字符串</li>
     *     <li>如果 <code>value</code> 是 {@link MultipartFile} 的实例, 返回一个包含文件原始名称和大小的映射</li>
     *     <li>对于其他类型的 <code>value</code>, 直接返回该对象本身</li>
     * </ul>
     */
    private Object sanitize(Object value) {
        if (value == null)
            return null;
        if (value instanceof HttpServletRequest req)
            return Map.of("request", "{" + req.getMethod() + " " + req.getRequestURI() + "}");
        if (value instanceof HttpServletResponse)
            return "{HttpServletResponse}";
        if (value instanceof MultipartFile file)
            return Map.of("file", Objects.requireNonNull(file.getOriginalFilename()), "size", file.getSize());
        return value;
    }

    /**
     * 将给定的对象转换为字符串表示, 以安全的方式处理可能的异常和大数据量问题
     *
     * @param value 要转换成字符串的对象, 可能是 <code>null</code>, 字符串, 数字, 布尔值, 或者其他类型
     * @return 对象的安全字符串表示。如果对象是 <code>null</code>, 返回 "null"。对于特定类型(如字符串, 数字, 布尔值)直接返回其字符串形式。
     * 如果对象可以被序列化为 JSON, 则返回该 JSON 字符串, 并且如果生成的 JSON 长度超过 2000 个字符, 则会被截断并在末尾添加长度信息。
     * 如果在尝试转换过程中发生任何异常, 将返回对象的默认 <code>toString()</code> 方法的结果
     */
    private String toStringSafe(Object value) {
        try {
            if (value == null)
                return "null";
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean)
                return String.valueOf(value);

            String json = objectMapper.writeValueAsString(value);
            int max = 2000;
            return json.length() > max ? json.substring(0, max) + "...(" + json.length() + " chars)" : json;
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
