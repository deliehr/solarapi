package de.liehrit.solarapi.controller;

import org.joda.time.DateTimeZone;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Set;

@RestController
@RequestMapping(path = "/time", produces = "application/json")
public class TimeController {
    @GetMapping("/timezones")
    public Set<String> getTimeZones() {
        return DateTimeZone.getAvailableIDs();
    }
}