package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class TotalResponse {
    private Set<String> keys;
    private int rowCount;
    private int requestedHours;
    private Map<String, List<Pair<Object,Object>>> data;
}