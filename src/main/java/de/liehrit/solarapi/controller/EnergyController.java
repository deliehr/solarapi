package de.liehrit.solarapi.controller;

import com.influxdb.query.FluxTable;
import de.liehrit.solarapi.components.InfluxClient;
import de.liehrit.solarapi.model.Pair;
import de.liehrit.solarapi.model.TotalResponse;
import lombok.val;
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

    @GetMapping("/total")
    public TotalResponse getAllTotalRecords(@RequestParam Optional<Integer> hours, @RequestParam Optional<String> fieldFilter, @RequestParam Optional<Integer> aggregateMinutes) {
        List<FluxTable> result = null;

        val hoursValue = Math.max(1, Math.abs(hours.orElseGet(() -> 24)));
        val hoursStringValue = String.valueOf(hoursValue);

        try {
            result = influxClient.readTotals(hoursStringValue, fieldFilter, aggregateMinutes);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(result == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

        val data = new HashMap<String, List<Pair<Object,Object>>>();
        var rowCount = 0;

        for(val table:result) {
            for(val record:table.getRecords()) {
                val values = record.getValues();

                val field = (String)values.get("_field");
                val value = values.get("_value");
                val time = values.get("_time");

                val list = data.computeIfAbsent(field, k -> new ArrayList<>());

                list.add(new Pair<>(time, value));

                rowCount++;
            }
        }

        return TotalResponse.builder()
                .keys(data.keySet())
                .requestedHours(hoursValue)
                .rowCount(rowCount)
                .data(data)
                .build();
    }
}