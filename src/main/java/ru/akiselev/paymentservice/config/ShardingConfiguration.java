package ru.akiselev.paymentservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.akiselev.paymentservice.persistence.shard.HashShardManager;
import ru.akiselev.paymentservice.persistence.shard.Shard;
import ru.akiselev.paymentservice.persistence.shard.ShardServer;
import ru.akiselev.paymentservice.persistence.shard.ShardManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ShardingConfiguration {

    private final ShardingProperties properties;

    @Bean
    @Qualifier("hikariConfig1")
    public HikariConfig hikariConfig1() {
        ShardingProperties.ShardingDataSourceProperties dataSourceProperties = properties.getDatasource1();
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceProperties.getJdbcUrl());
        config.setUsername(dataSourceProperties.getUsername());
        config.setPassword(dataSourceProperties.getPassword());
        config.setDriverClassName(dataSourceProperties.getDriverClassName());
        return config;
    }

    @Bean
    @Qualifier("hikariConfig2")
    public HikariConfig hikariConfig2() {
        ShardingProperties.ShardingDataSourceProperties dataSourceProperties = properties.getDatasource2();
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceProperties.getJdbcUrl());
        config.setUsername(dataSourceProperties.getUsername());
        config.setPassword(dataSourceProperties.getPassword());
        config.setDriverClassName(dataSourceProperties.getDriverClassName());
        return config;
    }

    @Bean
    @Qualifier("hikariConfig3")
    public HikariConfig hikariConfig3() {
        ShardingProperties.ShardingDataSourceProperties dataSourceProperties = properties.getDatasource3();
        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dataSourceProperties.getJdbcUrl());
        config.setUsername(dataSourceProperties.getUsername());
        config.setPassword(dataSourceProperties.getPassword());
        config.setDriverClassName(dataSourceProperties.getDriverClassName());
        return config;
    }

    @Bean
    @Qualifier("hikariDataSource1")
    public HikariDataSource hikariDataSource1() {
        return new HikariDataSource(hikariConfig1());
    }

    @Bean
    @Qualifier("hikariDataSource2")
    public HikariDataSource hikariDataSource2() {
        return new HikariDataSource(hikariConfig2());
    }

    @Bean
    @Qualifier("hikariDataSource3")
    public HikariDataSource hikariDataSource3() {
        return new HikariDataSource(hikariConfig3());
    }


    @Bean
    public ShardManager shardManager() {
        final Shard shard1 = new ShardServer(1, hikariDataSource1());
        final Shard shard2 = new ShardServer(2, hikariDataSource2());
        final Shard shard3 = new ShardServer(3, hikariDataSource3());
        return new HashShardManager(List.of(shard1, shard2, shard3));
    }
}
