package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Contains various methods for formatting data.
 */
public class Formatting {

    public static DecimalFormat decimalFormatter = new DecimalFormat("#.##");

    public static String generateDurationString(Date start, Date current, boolean showZeros, boolean withWords) {
        Instant startInstant = start.toInstant();
        Instant endInstant = current.toInstant();

        Duration dur = Duration.between(startInstant, endInstant);

        long days = Math.abs(dur.toDays());
        long hours = Math.abs(dur.minusDays(days).toHours());
        long minutes = Math.abs(dur.minusDays(days).minusHours(hours).toMinutes());
        long seconds = Math.abs(dur.minusDays(days).minusHours(hours).minusMinutes(minutes).toSeconds());

        if (!showZeros) {
            if (withWords) {
                return (days > 0 ? (int) days + " days, " : "") + (hours > 0 ? (int) hours + " hours, " : "") + (minutes > 0 ? (int) minutes + " minutes, " : "") + (seconds > 0 ? (int) seconds + " seconds" : "");
            } else {
                return (days > 0 ? (int) days + ":" : "") + (hours > 0 ? (int) hours + ":" : "") + (minutes > 0 ? (int) minutes + ":" : "") + (seconds > 0 ? (int) seconds + ":" : "");
            }
        } else {
            if (withWords) {
                return String.format("%s hours, %s minutes and %s seconds", (int) hours, (int) minutes, (int) seconds);
            } else {
                return String.format("%s:%s:%s", formatTimeUnit((int) hours), formatTimeUnit((int) minutes), formatTimeUnit((int) seconds));
            }
        }
    }

    public static String formatDate(Date date, boolean withTime) {

        if (date == null) {
            return "N/A";
        }

        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateAndTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        if (withTime) {
            return dateAndTime.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        } else {
            return dateOnly.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }
    }

    public static String formatLocalDate(LocalDate date) {

        if (date == null) {
            return "N/A";
        }

        DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateOnly.format(date);
    }

    public static String formatMoney(double money, String symbol, boolean symbolIsPrefix) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        if (symbolIsPrefix) {
            return symbol + decimalFormat.format(money);
        } else {
            return decimalFormat.format(money) + symbol;
        }
    }

    public static String formatMoney(double money, CurrencyConfig config) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        if (config.isCurrencySymbolPrefix()) {
            return config.getCurrencySymbol() + decimalFormat.format(money);
        } else {
            return decimalFormat.format(money) + config.getCurrencySymbol();
        }
    }

    public static String formatMoney(double money) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return decimalFormat.format(money);
    }

    public static String formatTimeUnit(int unit) {
        return unit <= 9
                ? "0" + unit
                : String.valueOf(unit);
    }

    public static String formatTime(Date date) {
        if (date == null) {
            return "N/A";
        }
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    public static String formatLocalTime(LocalTime time) {

        if (time == null) {
            return "N/A";
        }

        return DateTimeFormatter.ofPattern("HH:mm:ss").format(time);
    }

    public static String capitalize(String str) {
        if (str.isEmpty()) {
            return str;
        }

        return Character.toString(str.charAt(0)).toUpperCase() + str.toLowerCase().substring(1);
    }

    /**
     * Replaces underscores with spaces, trims, then capitalizes the input string.
     *
     * @param str The input string
     * @return A string with underscores replaced with spaces and the first letter capitalized with no trailing spaces.
     */
    public static String humanize(String str) {
        String s = str.toLowerCase();
        s = s.trim();
        s = s.replace('_', ' ');

        return capitalize(s);
    }

    public static String getNumberSuffix(int number) {
        return switch (String.valueOf(number).charAt(String.valueOf(number).length() - 1)) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    public static Date dateFromJSON(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        try {
            return format.parse(date);
        } catch (ParseException e) {
            DataManager.log("Failed to parse a date from '" + date + "'");
            return null;
        }
    }

    public static boolean isNumber(String input) {
        if (input == null) return false;
        if (input.isEmpty()) return false;

        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Formats a number according to the following rules.
     * If the number is whole (e.g. 5.0), it is shown as an integer (in this example, 5).
     * If the number is not whole (e.g. 4.3), it is left unchanged.
     *
     * @param number The number to format.
     */
    public static String formatDouble(Double number) {
        String str = number.toString();
        if (str.split("\\.")[1].equals("0")) {
            return String.valueOf(number.intValue());
        } else {
            return String.valueOf(number);
        }
    }
}
