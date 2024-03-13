package de.liehrit.solarapi.controller;

import de.liehrit.solarapi.model.LogMessage;
import de.liehrit.solarapi.repositories.MyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping(path = "/log", produces = "application/json")
public class LogController {
    @Autowired
    private MyRepository myRepository;

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public List<LogMessage> getAllLogs() {
        return myRepository.findAll();
    }
}
