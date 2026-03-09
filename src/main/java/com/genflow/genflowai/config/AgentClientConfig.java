package com.genflow.genflowai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Configures a RestTemplate bean scoped to the agent microservice and
 * a dedicated thread pool for @Async prediction jobs.
 */
@Configuration
public class AgentClientConfig {

    private final AgentProperties agentProperties;

    public AgentClientConfig(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    /**
     * RestTemplate for agent calls with extended timeout (LLM is slow).
     */
    @Bean("agentRestTemplate")
    public RestTemplate agentRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofSeconds(agentProperties.getTimeoutSeconds());
        return builder
                .rootUri(agentProperties.getBaseUrl())
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(timeout)
                .build();
    }

    /**
     * Dedicated thread pool for background prediction jobs so they don't
     * block the main request thread pool.
     */
    @Bean("predictionTaskExecutor")
    public Executor predictionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("genflow-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
