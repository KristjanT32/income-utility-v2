package com.krisapps.incomeutility_v2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTimeTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTypeAdapter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Level;

public class DataManager {

    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .create();
    private static DataManager instance;
    private final File dataFile = new File(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "data.json");
    private DataManager() {
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public static void log(String msg) {
        if (msg.toLowerCase().contains("failed") || msg.toLowerCase().contains("error") || msg.toLowerCase().contains("fail") || msg.toLowerCase().contains("couldn't") || msg.toLowerCase().contains("could not")) {
            System.out.println(String.format("[%s TripPlanner/ERROR]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        } else {
            System.out.println(String.format("[%s TripPlanner/INFO]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        }
    }

    public static void log(String msg, Level level) {
        System.out.println(String.format("[%s TripPlanner/%s]: ", Formatting.formatDate(Date.from(Instant.now()), true), level.getName()) + msg);
    }

    private void firstTimeFileSetup() {
        log("No files found, initializing first-time setup.");

        try {
            log("Creating a data directory at: " + Path.of(System.getProperty("user.home") + File.separator + "TripPlanner Data"));
            Files.createDirectory(Path.of(System.getProperty("user.home") + File.separator + "TripPlanner Data"));
        } catch (IOException e) {
            log("Failed to create data directory: " + e.getMessage());
        }

        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            log("Files successfully created.");
        } catch (IOException e) {
            log("Failed to create file: " + e.getMessage());
        }
    }

    private void createDataFile() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                Data data = new Data();
                saveData(data);
            }
        } catch (IOException e) {
            log("Could not create a new data file - " + e.getMessage());
        }
    }


    public void saveData(Data data) {

        if (!dataFile.exists()) {
            createDataFile();
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile, false), StandardCharsets.UTF_16));

            writer.write(gson.toJson(data));
            writer.close();
        } catch (IOException e) {
            log("Data saving failed - " + e.getMessage());
        }
    }

    /**
     * Loads the saved data from disk.
     *
     * @return The data
     */
    private Data getData() {

        if (!dataFile.exists()) {
            firstTimeFileSetup();
        }

        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_16);
            JsonReader reader = new JsonReader(inputStreamReader);
            Data output = gson.fromJson(reader, Data.class);
            if (output == null) {
                output = new Data();
            }
            return output;
        } catch (IOException e) {
            log("Failed to retrieve data from data file: " + e.getMessage());
            return new Data();
        }
    }

    /**
     * Contains various methods for formatting data.
     */
    public static class Formatting {

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

        public static String formatMoney(double money, String symbol, boolean symbolIsPrefix) {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            if (symbolIsPrefix) {
                return symbol + decimalFormat.format(money);
            } else {
                return decimalFormat.format(money) + symbol;
            }
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

        public static String getNumberSuffix(int number) {
            return switch (String.valueOf(number).charAt(String.valueOf(number).length() - 1)) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }

        public Date dateFromJSON(String date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                return format.parse(date);
            } catch (ParseException e) {
                log("Failed to parse a date from '" + date + "'");
                return null;
            }
        }
    }

    public class Data {


        public Data() {

        }
    }
}
