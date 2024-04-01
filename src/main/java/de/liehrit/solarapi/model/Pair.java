package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;

@Data
public class Pair <T,G> {
    private T key;
    private G value;

    public Pair(T key, G value) {
        this.key = key;
        this.value = value;
    }
}
