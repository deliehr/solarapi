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
    public List<FluxTable> readTotals(String rangeStart, Optional<String> fieldFilter, Optional<Integer> aggregateMinutes) {
        String query = String.format("from(bucket: \"%s\")\n", BUCKET) +
                String.format("|> range(start: -%s)\n", rangeStart) +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"total\")\n" +
                createDefaultQueryAppendix(fieldFilter, aggregateMinutes);

        return queryResult(query);
    }

    public List<FluxTable> readTotalsRange(long start, long end, Optional<String> fieldFilter, Optional<Integer> aggregateMinutes) {
        String query = String.format("from(bucket: \"%s\")\n", BUCKET) +
                String.format("|> range(start: %d, stop: %d)\n", start, end) +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"total\")\n" +
                createDefaultQueryAppendix(fieldFilter, aggregateMinutes);

        return queryResult(query);
    }

    private String createDefaultQueryAppendix(Optional<String> fieldFilter, Optional<Integer> aggregateMinutes) {
        val queryBuilder = new StringBuilder();

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

        return queryBuilder.toString();
    }

    private List<FluxTable> queryResult(String query) {
        if(!influxDBClient.ping()) {
            // TODO: log error
            logger.error("influx client did not pong");
            return null;
        }

        val api = influxDBClient.getQueryApi();
        return api.query(query);
    }

    @PreDestroy
    private void onPreDestroy() {
        influxDBClient.close();
        logger.info("disconnected from influx");
    }
}

