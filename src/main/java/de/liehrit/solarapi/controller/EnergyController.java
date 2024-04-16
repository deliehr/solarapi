package de.liehrit.solarapi.controller;

import com.influxdb.query.FluxTable;
import de.liehrit.solarapi.components.InfluxClient;
import de.liehrit.solarapi.model.*;
import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping(path = "/api/v1/energy", produces = "application/json")
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

    @GetMapping("/total/range")
    public TotalResponse getRangeTotalRecords(@RequestParam Optional<String> rangeStart,
                                            @RequestParam Optional<String> fieldFilter,
                                            @RequestParam Optional<String> aggregation,
                                            @RequestParam Optional<AggregateFunction> aggregationMethod) {

        var rangeStartValue = "1m";

        if(Helper.isTimeRangeStringValid(rangeStart)) rangeStartValue = rangeStart.get().trim();

        try {
            val result = influxClient.readTotals(rangeStartValue, fieldFilter, aggregation, aggregationMethod);

            String method = "mean";
            if(aggregationMethod.isPresent()) {
                method = aggregationMethod.get().name();
            }

            if(aggregation.isEmpty()) method = null;

            val valuesUsed = TotalResponseValuesUsed.builder()
                    .fields(fieldFilter.orElse(null))
                    .timeRange(rangeStart.orElse(rangeStartValue))
                    .aggregation(aggregation.orElse(null))
                    .aggregationMethod(method)
                    .build();

            return buildResponse(result).valuesUsed(valuesUsed).build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e);
        }
    }

    @GetMapping("/total/period")
    public TotalResponse getPeriodTotalRecords(@RequestParam long start,
                                            @RequestParam long end,
                                            @RequestParam Optional<String> fieldFilter,
                                            @RequestParam Optional<String> aggregation,
                                            @RequestParam Optional<AggregateFunction> aggregationMethod) {
        try {
            val result = influxClient.readTotalsRange(start, end, fieldFilter, aggregation, aggregationMethod);

            String method = "mean";
            if(aggregationMethod.isPresent()) {
                method = aggregationMethod.get().name();
            }

            val valuesUsed = TotalResponseValuesUsed.builder()
                    .fields(fieldFilter.orElse(null))
                    .timeRange(null)
                    .aggregation(aggregation.orElse(null))
                    .aggregationMethod(method)
                    .build();

            return buildResponse(result).valuesUsed(valuesUsed).build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), e);
        }
    }

    @GetMapping(path="/total/lastValue")
    public TotalResponse getLastValue(@RequestParam Optional<String> fieldFilter) {
        val result = influxClient.getLastValue(fieldFilter);

        if(result != null) {
            val valuesUsed = TotalResponseValuesUsed.builder()
                    .fields(fieldFilter.orElse(null))
                    .timeRange(null)
                    .aggregation(null)
                    .aggregationMethod(null)
                    .build();

            return buildResponse(result).valuesUsed(valuesUsed).build();
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not find the first day");
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