package com.krisapps.incomeutility_v2.util;

import org.jetbrains.annotations.Nullable;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public class Logging {
    private static Logging instance;

    private Path logFilePath;
    private boolean DEBUG = false;
    private boolean initialized = false;

    // Stores log messages that were queued to be logged after logger initialization.
    private final List<String> preInitQueue = new ArrayList<>();

    // Holds currently open log sections not yet written to the log file.
    private final HashMap<String, LinkedList<String>> sectionQueue = new HashMap<>();

    private final String MSG_FORMAT_FULL = "[%s Income Utility/%s] [%s]: %s%n";
    private final String MSG_FORMAT_CONCISE = "[%s Income Utility/%s]: %s%n";
    private final String MSG_FORMAT_DEBUG = "[%s Income Utility/DBG]: %s%n";


    private Logging() {
    }

    public static Logging getInstance() {
        if (instance == null) {
            instance = new Logging();
        }

        return instance;
    }

    public void initialize(Path logFilePath) {
        if (initialized) {
            logToConsole("Will not re-initialize Logging");
            return;
        }

        if (logFilePath.toFile().isDirectory()) {
            throw new InvalidParameterException("The specified path is a directory.");
        }

        this.logFilePath = logFilePath;
        if (!logFilePath.toFile().exists()) {
            try {
                logFilePath.toFile().createNewFile();
                logToConsole("Created a new log file at: " + logFilePath);
            } catch (IOException e) {
                logToConsole("Failed to create a new log file. Error: " + e.getMessage());
            }
        }

        initialized = true;

        // Flush queued messages
        synchronized (preInitQueue) {
            for (String msg : preInitQueue) {
                writeToLogFile(msg);
            }
            preInitQueue.clear();
        }
    }

    public void debug(String msg) {
        if (!DEBUG) return;

        String message = String.format(MSG_FORMAT_DEBUG, Formatting.formatDate(Date.from(Instant.now()), true), msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    private void logToConsole(String msg) {
        String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), "Logger", msg);
        System.out.print(message);
    }

    /**
     * Logs the supplied message as-is, with no formatting.
     *
     * @param msg The message to log.
     */
    public void print(String msg) {
        System.out.println(msg);
        writeToLogFile(msg);
    }

    public void log(String msg, Level level) {
        String message = String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    public void log(String msg, String modulePrefix) {
        String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), modulePrefix, msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    public void log(String msg, String modulePrefix, Level level) {
        String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), modulePrefix, msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    public void logToSection(String msg, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            log(msg, Level.INFO);
        }
    }

    public void logToSection(String msg, String modulePrefix, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), modulePrefix, msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            log(msg, modulePrefix, Level.INFO);
        }
    }

    public void logToSection(String msg, String modulePrefix, Level level, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), modulePrefix, msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            log(msg, modulePrefix, level);
        }
    }

    public void logStackTrace(Exception e) {
        String section = "__stacktrace_" + e.getClass().getName() + "_" + System.currentTimeMillis();
        startSection(section, "--- Begin stack trace for " + e.getClass().getSimpleName() + " at " + Formatting.formatDate(Date.from(Instant.now()), true) + " ---");
        logToSection("", section);
        logToSection(e.getMessage(), section);
        logToSection("", section);
        for (StackTraceElement el : e.getStackTrace()) {
            logToSection(el.toString(), section);
        }
        endSection(section, "--- End of stack trace ---");
    }

    public void startSection(String name, String header) {
        if (!sectionQueue.containsKey(name)) {
            sectionQueue.put(name, new LinkedList<>());
            writeToLogFile("\n\n" + String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO, header));
        } else {
            throw new KeyAlreadyExistsException("A section with this name has already been started!");
        }
    }

    public void endSection(String section, @Nullable String footer) {
        if (sectionQueue.containsKey(section)) {
            for (String msg : sectionQueue.get(section)) {
                writeToLogFile(msg);
            }
            sectionQueue.remove(section);
            logToConsole("Logging section '" + section + "' closed.");
            if (footer != null)
                writeToLogFile("\n" + String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO, footer) + "\n\n");
        }
    }

    public void truncateLogFile() {
        if (!initialized) {
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(logFilePath.toFile())) {
            FileChannel channel = outputStream.getChannel();
            channel.truncate(0);
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void writeToLogFile(String msg) {
        if (!initialized) {
            synchronized (preInitQueue) {
                preInitQueue.add(msg);
            }
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFilePath.toFile(), true))) {
            bw.write(msg);
            bw.flush();
        } catch (IOException e) {
            logToConsole("Failed to append log message to file. Error: " + e.getMessage());
        }
    }

    public void setEnableDebug(boolean enableDebug) {
        this.DEBUG = enableDebug;
    }


    public boolean isDebugEnabled() {
        return DEBUG;
    }
}
