// shopping.international.app.config.MailAsyncConfig
package shopping.international.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 邮件发送异步线程池配置
 */
@Configuration
public class MailAsyncConfig {

    /**
     * 邮件发送任务线程池
     *
     * @return 用于执行邮件异步任务的 {@link Executor}
     */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(2);
        exec.setMaxPoolSize(4);
        exec.setQueueCapacity(1000);
        exec.setThreadNamePrefix("mail-async-");
        exec.initialize();
        return exec;
    }
}
