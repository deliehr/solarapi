package de.liehrit.solarapi.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WattMessage {
    private String timestamp;

}