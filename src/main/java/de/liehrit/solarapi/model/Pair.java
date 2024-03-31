package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pair <T,G> {
    private T key;
    private G value;
}
