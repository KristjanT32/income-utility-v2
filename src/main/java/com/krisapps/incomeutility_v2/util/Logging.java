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
    private final String MSG_FORMAT_DEBUG_CONCISE = "[%s Income Utility/DBG]: %s%n";
    private final String MSG_FORMAT_DEBUG_FULL = "[%s Income Utility/DBG] [%s]: %s%n";


    private Logging() {
    }

    public static Logging getInstance() {
        if (instance == null) {
            instance = new Logging();
        }

        return instance;
    }

    /**
     * Initializes the logger with the specified log file location.
     *
     * @param logFilePath The path to the log file to use.
     */
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
                //noinspection ResultOfMethodCallIgnored
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

    /**
     * Logs a debug message.
     * Does nothing, if debug logging is disabled.
     *
     * @param msg The message to log.
     */
    public void debug(String msg) {
        if (!DEBUG) return;

        String message = String.format(MSG_FORMAT_DEBUG_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    /**
     * Logs a debug message.
     * Does nothing, if debug logging is disabled.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     */
    public void debug(String msg, String modulePrefix) {
        if (!DEBUG) return;

        String message = String.format(MSG_FORMAT_DEBUG_FULL, Formatting.formatDate(Date.from(Instant.now()), true), modulePrefix, msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    /**
     * Logs the message only to the standard output, leaving it out of the log file.
     *
     * @param msg The message to log.
     */
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

    /**
     * Logs an informational message.
     *
     * @param msg   The message to log.
     * @param level The type of log message to log.
     */
    public void info(String msg, Level level) {
        String message = String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    /**
     * Logs an informational message.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     */
    public void info(String msg, String modulePrefix) {
        String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), modulePrefix, msg);
        System.out.print(message);
        writeToLogFile(message);
    }


    /**
     * Logs a message.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     * @param level        The type of log message to log.
     */
    public void log(String msg, String modulePrefix, Level level) {
        String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), modulePrefix, msg);
        System.out.print(message);
        writeToLogFile(message);
    }

    /**
     * Logs a warning message.
     *
     * @param msg The message to log.
     */
    public void warning(String msg) {
        info(msg, Level.WARNING);
    }

    /**
     * Logs a warning message.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     */
    public void warning(String msg, String modulePrefix) {
        log(msg, modulePrefix, Level.WARNING);
    }

    /**
     * Logs an error message.
     *
     * @param msg The message to log.
     */
    public void error(String msg) {
        info(msg, Level.SEVERE);
    }

    /**
     * Logs an error message.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     */
    public void error(String msg, String modulePrefix) {
        log(msg, modulePrefix, Level.SEVERE);
    }

    /**
     * Logs a message to the supplied log section.
     * If the supplied section doesn't exist, this method will log an informational message instead.
     *
     * @param msg     The message to log.
     * @param section The ID of the log section to log to.
     */
    public void logToSection(String msg, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_CONCISE, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            info(msg, Level.INFO);
        }
    }


    /**
     * Logs a message to the supplied log section.
     * If the supplied section doesn't exist, this method will log an informational message instead.
     *
     * @param msg          The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     * @param section      The ID of the log section to log to.
     */
    public void logToSection(String msg, String modulePrefix, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), Level.INFO.getName(), modulePrefix, msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            log(msg, modulePrefix, Level.INFO);
        }
    }

    /**
     * Logs a message to the supplied log section.
     * If the supplied section doesn't exist, this method will log an informational message instead.
     * @param msg The message to log.
     * @param modulePrefix The prefix of the module of the program to which the message relates.
     * @param level The type of log message to log.
     * @param section The ID of the log section to log to.
     */
    public void logToSection(String msg, String modulePrefix, Level level, String section) {
        if (sectionQueue.containsKey(section)) {
            String message = String.format(MSG_FORMAT_FULL, Formatting.formatDate(Date.from(Instant.now()), true), level.getName(), modulePrefix, msg);
            sectionQueue.get(section).add(message);
        } else {
            debug("Cannot log to non-existent log section '" + section + "'");
            log(msg, modulePrefix, level);
        }
    }

    /**
     * Logs the stacktrace for the supplied exception.
     * <br>
     * More specifically, this will log the message, the stack trace, as well as the stack trace of the cause of this exception.
     * <br>
     * This method will print the stack trace to the standard output only if debug logging is enabled.
     * @param e The exception whose stack trace to log.
     */
    public void logStackTrace(Exception e) {
        if (DEBUG) {
            e.printStackTrace();
        }

        String section = "__stacktrace_" + e.getClass().getName() + "_" + System.currentTimeMillis();
        String causeSection = "__cause" + section;
        startSection(section, "--- Begin stack trace for " + e.getClass().getSimpleName() + " at " + Formatting.formatDate(Date.from(Instant.now()), true) + " ---");
        logToSection("", section);
        logToSection(e.getMessage(), section);
        logToSection("", section);
        for (StackTraceElement el : e.getStackTrace()) {
            logToSection(el.toString(), section);
        }
        if (e.getCause() != null) {
            startSection(causeSection, "--- Begin stack trace of exception cause ---");
            logToSection("", causeSection);
            logToSection(e.getCause().getMessage(), causeSection);
            for (StackTraceElement el : e.getCause().getStackTrace()) {
                logToSection(el.toString(), causeSection);
            }
            logToSection("", causeSection);
            endSection(causeSection, "--- End stack trace of exception cause ---");
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
