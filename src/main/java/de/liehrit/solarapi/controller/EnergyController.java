package de.liehrit.solarapi.controller;

import com.influxdb.query.FluxTable;
import de.liehrit.solarapi.components.InfluxClient;
import de.liehrit.solarapi.model.Pair;
import de.liehrit.solarapi.model.TotalResponse;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping(path = "/energy", produces = "application/json")
public class EnergyController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final InfluxClient influxClient;

    public EnergyController(InfluxClient influxClient) {
        this.influxClient = influxClient;
    }

    @GetMapping("/total/fields")
    public Set<String> getTotalFields() {
        val result = influxClient.readTotalFields();

        if(result != null) {
            val fieldNames = new HashSet<String>();

            for(val record:result.getRecords()) {
                val values = record.getValues();

                val fieldName = (String)values.get("_value");

                fieldNames.add(fieldName);
            }

            return fieldNames;
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find any field names.");
    }

    @GetMapping("/total")
    public TotalResponse getAllTotalRecords(@RequestParam Optional<Integer> hours,
                                            @RequestParam Optional<String> fieldFilter,
                                            @RequestParam Optional<Integer> aggregateMinutes) {

        val hoursValue = Math.max(1, Math.abs(hours.orElseGet(() -> 24)));
        val hoursStringValue = String.valueOf(hoursValue);

        try {
            val result = influxClient.readTotals(hoursStringValue, fieldFilter, aggregateMinutes);

            return buildResponse(result).requestedHours(hoursValue).build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e);
        }
    }

    @GetMapping("/total/range")
    public TotalResponse getRangeTotalRecords(@RequestParam long start,
                                            @RequestParam long end,
                                            @RequestParam Optional<String> fieldFilter,
                                            @RequestParam Optional<Integer> aggregateMinutes) {
        try {
            val result = influxClient.readTotalsRange(start, end, fieldFilter, aggregateMinutes);

            return buildResponse(result).requestedHours(null).build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e);
        }
    }

    private TotalResponse.TotalResponseBuilder buildResponse(List<FluxTable> result) throws ResponseStatusException {
        if(result == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Influxdb result is null");

        val data = new HashMap<String, List<Pair>>();
        var rowCount = 0;

        for(val table:result) {
            for(val record:table.getRecords()) {
                val values = record.getValues();

                val field = (String)values.get("_field");
                val value = (Double)values.get("_value");
                val time = (java.time.Instant)values.get("_time");

                val timestamp = (new DateTime(time.toEpochMilli(), DateTimeZone.UTC)).getMillis();

                val list = data.computeIfAbsent(field, k -> new ArrayList<>());

                list.add(new Pair(timestamp, value));

                rowCount++;
            }
        }

        return TotalResponse.builder()
                .keys(data.keySet())
                .rowCount(rowCount)
                .data(data);
    }
}