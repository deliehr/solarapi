package de.liehrit.solarapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/test", produces = "application/json")
public class TestController {
    @GetMapping("")
    public String getTest() {
        return "Teststring";
    }
}
