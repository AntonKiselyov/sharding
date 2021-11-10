package ru.akiselev.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "sharding")
@Getter
@Setter
public class ShardingProperties {

    private ShardingDataSourceProperties datasource1;
    private ShardingDataSourceProperties datasource2;
    private ShardingDataSourceProperties datasource3;

    @Getter
    @Setter
    public static class ShardingDataSourceProperties {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
    }
}
