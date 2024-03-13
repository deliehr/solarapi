package de.liehrit.solarapi.configurations;

import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class InfluxDBClientConfiguration {
    @Bean
    InfluxDBClient influxDBClient(Environment environment) throws Exception {
        String influxDbHost = environment.getRequiredProperty("INFLUXDB.HOST");
        String influxDbToken = environment.getRequiredProperty("INFLUXDB.TOKEN");
        String influxDbOrga = environment.getRequiredProperty("INFLUXDB.ORGA");
        String influxDbBucket = environment.getRequiredProperty("INFLUXDB.BUCKET");

        return InfluxDBClientFactory.create(influxDbHost, influxDbToken.toCharArray(), influxDbOrga, influxDbBucket).setLogLevel(LogLevel.BASIC);
    }
}
