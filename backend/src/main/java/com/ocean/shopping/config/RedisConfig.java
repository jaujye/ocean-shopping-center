package com.ocean.shopping.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;

/**
 * Redis configuration for caching and session management
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
@Slf4j
@ConditionalOnProperty(name = "spring.session.store-type", havingValue = "redis", matchIfMissing = true)
@Profile("!test")
public class RedisConfig {

    // Standalone configuration
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    // Sentinel configuration for high availability
    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    // Connection pool configuration
    @Value("${spring.data.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    // SSL/TLS configuration for secure connections
    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = createLettuceClientConfiguration();
        
        if (StringUtils.hasText(sentinelMaster) && StringUtils.hasText(sentinelNodes)) {
            // High-availability Sentinel configuration
            RedisSentinelConfiguration sentinelConfig = createSentinelConfiguration();
            log.info("Configuring Redis Sentinel connection - master: {}, sentinels: {}", sentinelMaster, sentinelNodes);
            return new LettuceConnectionFactory(sentinelConfig, clientConfig);
        } else {
            // Standalone configuration
            RedisStandaloneConfiguration standaloneConfig = createStandaloneConfiguration();
            log.info("Configuring Redis standalone connection to {}:{}", redisHost, redisPort);
            return new LettuceConnectionFactory(standaloneConfig, clientConfig);
        }
    }

    private RedisStandaloneConfiguration createStandaloneConfiguration() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }
        
        return config;
    }

    private RedisSentinelConfiguration createSentinelConfiguration() {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.setMaster(sentinelMaster);
        
        // Parse sentinel nodes
        Arrays.stream(sentinelNodes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(node -> {
                    String[] hostPort = node.split(":");
                    if (hostPort.length == 2) {
                        config.sentinel(hostPort[0].trim(), Integer.parseInt(hostPort[1].trim()));
                    }
                });
        
        if (StringUtils.hasText(redisPassword)) {
            config.setPassword(redisPassword);
        }
        
        return config;
    }

    private LettuceClientConfiguration createLettuceClientConfiguration() {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = 
                LettuceClientConfiguration.builder();
        
        // Configure client options for better performance and reliability
        ClientOptions.Builder clientOptionsBuilder = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .keepAlive(true)
                        .build())
                .timeoutOptions(TimeoutOptions.builder()
                        .fixedTimeout(Duration.ofSeconds(10))
                        .build());
        
        // Enable SSL if configured
        if (sslEnabled) {
            log.info("Enabling SSL for Redis connection");
            clientOptionsBuilder.sslOptions(io.lettuce.core.SslOptions.builder()
                    .jdkSslProvider()
                    .build());
            builder.useSsl();
        }
        
        return builder
                .clientOptions(clientOptionsBuilder.build())
                .commandTimeout(Duration.ofSeconds(5))
                .shutdownTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

}