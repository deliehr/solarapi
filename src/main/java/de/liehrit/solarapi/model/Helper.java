package de.liehrit.solarapi.model;

import lombok.val;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
    public static boolean isTimeRangeStringValid(Optional<String> input) {
        if(!(input.isPresent() && input.get() != null && !input.get().isEmpty())) return false;

        val rangeStartInput = input.get().trim();

        String regex = "^[0-9]{1,2}[dhm]$";       // ^[0-9]{1,2}[dhm]$
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rangeStartInput);

        return matcher.matches();
    }
}
