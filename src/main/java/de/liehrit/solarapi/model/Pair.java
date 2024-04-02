package de.liehrit.solarapi.model;

import lombok.Data;

@Data
public class Pair {
    private Long timestamp;
    private Double value;

    public Pair(Long timestamp, Double value) {
        this.timestamp = timestamp;
        this.value = value;
    }
}
