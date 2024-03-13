package de.liehrit.solarapi.model;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogMessage {
    private String timestamp;
    private String topic;
    private String value;
}
