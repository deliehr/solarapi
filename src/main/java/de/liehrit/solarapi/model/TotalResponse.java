package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class TotalResponse {
    private TotalResponseValuesUsed valuesUsed;
    private Set<String> keys;
    private int rowCount;
    private Map<String, List<Pair>> data;
}
