package com.example.ai.config;

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisVectorStoreConfig {

    @Bean
    public RedisVectorStore vectorStore(EmbeddingModel embeddingModel,
                                        RedisVectorStoreProperties properties,
                                        JedisConnectionFactory jedisConnectionFactory,
                                        ObjectProvider<ObservationRegistry> observationRegistryProvider,
                                        ObjectProvider<VectorStoreObservationConvention> observationConventionProvider) {
        JedisPooled jedis = createJedisPooled(jedisConnectionFactory);

        RedisVectorStore.Builder builder = RedisVectorStore.builder(jedis, embeddingModel)
                .initializeSchema(properties.isInitializeSchema())
                .indexName(properties.getIndexName())
                .prefix(properties.getPrefix())
                .batchingStrategy(new TokenCountBatchingStrategy())
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("knowledgeScope"),
                        RedisVectorStore.MetadataField.numeric("ownerUserId"),
                        RedisVectorStore.MetadataField.numeric("docId"),
                        RedisVectorStore.MetadataField.text("fileName")
                );

        ObservationRegistry observationRegistry = observationRegistryProvider.getIfUnique();
        if (observationRegistry != null) {
            builder.observationRegistry(observationRegistry);
        }

        VectorStoreObservationConvention observationConvention = observationConventionProvider.getIfAvailable();
        if (observationConvention != null) {
            builder.customObservationConvention(observationConvention);
        }

        return builder.build();
    }

    private JedisPooled createJedisPooled(JedisConnectionFactory jedisConnectionFactory) {
        RedisStandaloneConfiguration standalone = jedisConnectionFactory.getStandaloneConfiguration();
        JedisClientConfiguration springClientConfig = jedisConnectionFactory.getClientConfiguration();

        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis((int) springClientConfig.getConnectTimeout().toMillis())
                .socketTimeoutMillis((int) springClientConfig.getReadTimeout().toMillis())
                .database(standalone.getDatabase())
                .ssl(springClientConfig.isUseSsl());

        springClientConfig.getClientName().ifPresent(clientConfigBuilder::clientName);
        springClientConfig.getSslSocketFactory().ifPresent(clientConfigBuilder::sslSocketFactory);
        springClientConfig.getSslParameters().ifPresent(clientConfigBuilder::sslParameters);
        springClientConfig.getHostnameVerifier().ifPresent(clientConfigBuilder::hostnameVerifier);

        String username = standalone.getUsername();
        if (StringUtils.hasText(username)) {
            clientConfigBuilder.user(username);
        }

        RedisPassword redisPassword = standalone.getPassword();
        if (redisPassword != null && redisPassword.isPresent()) {
            clientConfigBuilder.password(String.valueOf(redisPassword.get()));
        }

        GenericObjectPoolConfig<redis.clients.jedis.Connection> poolConfig = springClientConfig.getPoolConfig()
                .orElseGet(GenericObjectPoolConfig::new);

        return new JedisPooled(
                new HostAndPort(standalone.getHostName(), standalone.getPort()),
                clientConfigBuilder.build(),
                poolConfig
        );
    }
}
