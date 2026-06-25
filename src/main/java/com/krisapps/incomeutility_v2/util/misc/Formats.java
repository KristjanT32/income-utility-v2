package com.krisapps.incomeutility_v2.util.misc;

import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;

public class Formats {

    public static final StringConverter<LocalDate> DATE_FORMAT = new StringConverter<>() {
        final DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        @Override
        public String toString(LocalDate localDate) {
            if (localDate != null) {
                return format.format(localDate);
            } else {
                return "";
            }
        }

        @Override
        public LocalDate fromString(String s) {
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            try {
                return LocalDate.ofInstant(format.parse(s).toInstant(), ZoneId.systemDefault());
            } catch (ParseException e) {
                return null;
            }
        }
    };

    public static final String TIME_VALIDATION_EXPRESSION = "(?:0[0-9]|1[0-9]|2[0-3])(?::[0-5][0-9])*";

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public final UnaryOperator<TextFormatter.Change> NUMBERS_ONLY_FORMATTER = (change) -> {
        if (change.getControlNewText().isEmpty()) {
            return change;
        }

        try {
            Double.parseDouble(change.getControlNewText());
            return change;
        } catch (NumberFormatException ignored) {
        }

        return null;
    };
}
