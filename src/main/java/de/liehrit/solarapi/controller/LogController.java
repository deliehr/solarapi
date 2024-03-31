package de.liehrit.solarapi.controller;

import de.liehrit.solarapi.SolarapiApplication;
import de.liehrit.solarapi.model.LogMessage;
import de.liehrit.solarapi.repositories.LogRepository;
import jakarta.servlet.http.HttpSession;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping(path = "/logs", produces = "application/json")
public class LogController {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final LogRepository logRepository;

    public LogController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("")
    public List<LogMessage> getAllLogs() {
        return logRepository.findAll();
    }
}
