package io.joshuasalcedo.logging.spring.autoconfigure;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.spring.actuator.LoggingHealthIndicator;
import io.joshuasalcedo.logging.spring.actuator.LoggingMetricsEndpoint;
import io.joshuasalcedo.logging.spring.properties.LoggingProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Actuator integration configuration for Joshua Salcedo Logging Framework
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnProperty(
    prefix = "joshuasalcedo.logging.metrics",
    name = "actuator-integration",
    havingValue = "true",
    matchIfMissing = true
)
public class LoggingActuatorConfiguration {
    
    /**
     * Health indicator for logging framework
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("logging")
    @ConditionalOnBean(LoggingMetrics.class)
    @ConditionalOnMissingBean
    public LoggingHealthIndicator loggingHealthIndicator(LoggingMetrics metrics) {
        return new LoggingHealthIndicator(metrics);
    }
    
    /**
     * Metrics endpoint for logging framework
     */
    @Bean
    @ConditionalOnAvailableEndpoint
    @ConditionalOnBean(LoggingMetrics.class)
    @ConditionalOnMissingBean
    public LoggingMetricsEndpoint loggingMetricsEndpoint(LoggingMetrics metrics) {
        return new LoggingMetricsEndpoint(metrics);
    }
}