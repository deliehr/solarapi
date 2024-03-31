package de.liehrit.solarapi.components;

import de.liehrit.solarapi.model.Pair;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import jakarta.annotation.PreDestroy;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@DependsOn("influxDBClientConfiguration")
public class InfluxClient {
    private final String BUCKET;
    private final InfluxDBClient influxDBClient;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InfluxClient(InfluxDBClient influxDBClient, Environment environment) {
        this.influxDBClient = influxDBClient;

        BUCKET = environment.getRequiredProperty("INFLUXDB.BUCKET");
    }

    public void savePoint(Point point) throws InfluxException {
        val api = influxDBClient.getWriteApiBlocking();
        api.writePoint(point);
    }

    @Nullable
    public Map<String, List<Pair<Object,Object>>> readTotals() throws Exception {
        if(!influxDBClient.ping()) {
            // TODO: log error
            logger.error("influx client did not pong");
            return null;
        }

        val api = influxDBClient.getQueryApi();
        val fromLine = String.format("from(bucket: \"%s\")\n", BUCKET);
        val query = fromLine +
        """
         |> range(start: -10h)
         |> filter(fn: (r) => r["_measurement"] == "total")
        """;

        val result = api.query(query);

        logger.debug("result: {}", result);

        var map = new HashMap<String, List<Pair<Object,Object>>>();

        for(val table:result) {
            for(val record:table.getRecords()) {
                val values = record.getValues();

                val field = (String)values.get("_field");
                val value = values.get("_value");
                val time = values.get("_time");

                var list = map.computeIfAbsent(field, k -> new ArrayList<>());

                list.add(Pair.builder().key(time).value(value).build());
            }
        }

        return map;
    }

    @PreDestroy
    private void onPreDestroy() {
        influxDBClient.close();
        logger.info("disconnected from influx");
    }
}

