package de.liehrit.solarapi.controller;

import de.liehrit.solarapi.components.InfluxClient;
import de.liehrit.solarapi.model.Pair;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/energy", produces = "application/json")
public class EnergyController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final InfluxClient influxClient;

    public EnergyController(InfluxClient influxClient) {
        this.influxClient = influxClient;
    }

    @GetMapping("/total")
    public Map<String, List<Pair<Object,Object>>> getAllTotalRecords() {
        try {
            val read = influxClient.readTotals();

            if(read != null) {
                return read;
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}