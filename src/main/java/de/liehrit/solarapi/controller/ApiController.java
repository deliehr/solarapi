package de.liehrit.solarapi.controller;

import de.liehrit.solarapi.SolarapiApplication;
import lombok.val;
import org.apache.maven.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping(path = "/api/v1", produces = "application/json")
public class ApiController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @GetMapping("/version")
    public HashMap<String, String> getVersion() {
        Model pomModel = null;

        try {
            pomModel = SolarapiApplication.getPomModel();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

        val map = new HashMap<String, String>();

        if(pomModel != null) {
            map.put("version", pomModel.getVersion());
            pomModel.getBuild();
        }

        return map;
    }

    @GetMapping(value = "/ping", produces="text/plain")
    public String getPong() {
        return "pong";
    }
}
