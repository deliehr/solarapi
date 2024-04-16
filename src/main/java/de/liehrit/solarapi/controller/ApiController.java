package de.liehrit.solarapi.controller;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/api/v1", produces = "application/json")
public class ApiController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${git.build.time}")
    private String gitBuildTime;

    @Value("${git.build.version}")
    private String gitBuildVersion;

    @Value("${git.commit.id.abbrev}")
    private String gitCommitIdAbbrev;

    @GetMapping("/version")
    public HashMap<String, String> getVersion() {
        val map = new HashMap<String, String>();

        if(gitBuildTime != null && !gitBuildTime.isEmpty()) {
            map.put("buildTime", gitBuildTime);
        }

        if(gitBuildVersion != null && !gitBuildVersion.isEmpty()) {
            map.put("buildVersion", gitBuildVersion);
        }

        if(gitCommitIdAbbrev != null && !gitCommitIdAbbrev.isEmpty()) {
            map.put("commitId", gitCommitIdAbbrev);
        }

        return map;
    }

    @GetMapping(value = "/ping", produces="text/plain")
    public String getPong() {
        return "pong";
    }
}
