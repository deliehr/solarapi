package de.liehrit.solarapi.components;

import com.google.gson.Gson;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@DependsOn("influxDBClientConfiguration")
public class InfluxClient {
    private final InfluxDBClient influxDBClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InfluxClient(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    public void savePoint(Point point) throws InfluxException {
        val api = influxDBClient.getWriteApiBlocking();
        api.writePoint(point);
    }

    @PreDestroy
    private void onPreDestroy() {
        influxDBClient.close();
        logger.info("disconnected from influx");
    }
}

