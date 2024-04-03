package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class TotalResponse {
    private Set<String> keys;
    private int rowCount;

    @Nullable
    private Integer requestedHours;

    private Map<String, List<Pair>> data;
}
