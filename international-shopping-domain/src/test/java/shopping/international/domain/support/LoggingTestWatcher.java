package shopping.international.domain.support;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * 简单的测试结果记录扩展, 确保每个用例都会输出执行结果。
 */
public class LoggingTestWatcher implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.println("[TEST PASS] " + context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.out.println("[TEST FAIL] " + context.getDisplayName() + " -> " + cause.getMessage());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        System.out.println("[TEST ABORTED] " + context.getDisplayName() + " -> " + (cause == null ? "" : cause.getMessage()));
    }
}
