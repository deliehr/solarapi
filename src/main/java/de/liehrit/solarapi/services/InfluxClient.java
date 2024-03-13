package de.liehrit.solarapi.services;

import com.google.gson.Gson;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import de.liehrit.solarapi.model.LogMessage;
import de.liehrit.solarapi.model.WattMessage;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@DependsOn("influxDBClientConfiguration")
public class InfluxClient {
    public static final Gson gson = new Gson();
    private final InfluxDBClient influxDBClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InfluxClient(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    public void saveInInflux(String jsonMessageContent) {
        if(influxDBClient == null) return;

        if(!influxDBClient.ping()) return;

        try {
            WattMessage logMessage = gson.fromJson(jsonMessageContent, WattMessage.class);

            val point = getPointForTemperatureLog(logMessage);

            if(point != null) {
                var api = influxDBClient.getWriteApiBlocking();

                api.writePoint(point);
                logger.debug("point written to influx");
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }
    }

    @Nullable
    private Point getPointForTemperatureLog(WattMessage message) {
        Optional<String> measurementName = Optional.empty();

        /*if(message.getTemperature() != null) {
            measurementName = Optional.of("temperature");
        } else if(message.getAltitude() != null) {
            measurementName = Optional.of("altitude");
        } else if(message.getHumidity() != null) {
            measurementName = Optional.of("humidity");
        } else if(message.getPressure() != null) {
            measurementName = Optional.of("pressure");
        }

        if(measurementName.isPresent()) {
            var point = Point.measurement(measurementName.get())
                    .time(message.getTimestamp(), WritePrecision.S);

            var fieldName = message.getLocation().concat("-").concat(message.getSensor());

            if(message.getTemperature() != null) {
                point.addField(fieldName, message.getTemperature());
            } else if(message.getAltitude() != null) {
                point.addField(fieldName, message.getAltitude());
            } else if(message.getHumidity() != null) {
                point.addField(fieldName, message.getHumidity());
            } else if(message.getPressure() != null) {
                point.addField(fieldName, message.getPressure());
            }

            return point;
        }*/
        return null;
    }

    @PreDestroy
    private void onPreDestroy() {
        influxDBClient.close();
        logger.info("disconnected from influx");
    }
}

