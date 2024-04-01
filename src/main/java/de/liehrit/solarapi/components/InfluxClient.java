package de.liehrit.solarapi.components;

import com.influxdb.query.FluxTable;
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
import org.springframework.web.server.ResponseStatusException;

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
    public FluxTable readTotalFields() {
        if(!influxDBClient.ping()) {
            // TODO: log error
            logger.error("influx client did not pong");
            return null;
        }

        val query = new StringBuilder();
        query.append("import \"influxdata/influxdb/schema\"\n");
        query.append(String.format("schema.fieldKeys(bucket: \"%s\", predicate: (r) => r._measurement == \"total\")", BUCKET));

        val api = influxDBClient.getQueryApi();
        val result = api.query(query.toString());

        logger.debug("result: {}", result);

        if(result.size() == 1) {
            return result.get(0);
        }

        return null;
    }

    @Nullable
    public List<FluxTable> readTotals(String hours, Optional<String> fieldFilter, Optional<Integer> aggregateMinutes) {
        if(!influxDBClient.ping()) {
            // TODO: log error
            logger.error("influx client did not pong");
            return null;
        }

        val api = influxDBClient.getQueryApi();

        val queryBuilder = new StringBuilder();

        val fromLine = String.format("from(bucket: \"%s\")\n", BUCKET);
        val rangeLine = String.format("|> range(start: -%sh)\n", hours);
        val measurementLine = "|> filter(fn: (r) => r[\"_measurement\"] == \"total\")\n";

        queryBuilder.append(fromLine);
        queryBuilder.append(rangeLine);
        queryBuilder.append(measurementLine);

        if(fieldFilter.isPresent() && !fieldFilter.get().isEmpty()) {
            val fields = fieldFilter.get().split(",");

            if(fields.length > 0) {
                val firstField = fields[0].trim();

                queryBuilder.append(String.format("|> filter(fn: (r) => r[\"_field\"] == \"%s\"", firstField));

                if(fields.length > 1) {
                    for(int i=1;i < fields.length;i++) {
                        val nextField = fields[i].trim();

                        if(nextField.isEmpty()) continue;

                        queryBuilder.append(String.format(" or r[\"_field\"] == \"%s\"", nextField));
                    }
                }

                queryBuilder.append(")\n");
            }
        }

        if(aggregateMinutes.isPresent()) {
            val minutes = Math.min(10, Math.max(1, Math.abs(aggregateMinutes.get())));

            queryBuilder.append(String.format("|> aggregateWindow(every: %dm, fn: mean, createEmpty: false)\n|> yield(name: \"mean\")", minutes));
        }

        val result = api.query(queryBuilder.toString());

        logger.debug("result: {}", result);

        return result;
    }

    @PreDestroy
    private void onPreDestroy() {
        influxDBClient.close();
        logger.info("disconnected from influx");
    }
}

