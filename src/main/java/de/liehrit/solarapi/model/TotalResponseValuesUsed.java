package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
@Builder
public class TotalResponseValuesUsed {
    @Nullable private String fields;
    @Nullable private String timeRange;
    @Nullable private String aggregation;
    @Nullable private String aggregationMethod;
}
