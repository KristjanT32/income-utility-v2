package com.krisapps.incomeutility_v2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTimeTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.TransactionDeserializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public class DataManager {

    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapter(Transaction.class, new TransactionDeserializer())
            .create();
    private static DataManager instance;
    private final File dataFile = new File(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "data.json");
    private boolean isSaving = false;

    private Data currentData;

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
            System.out.println(String.format("[%s IncomeUtility/ERROR]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        } else {
            System.out.println(String.format("[%s IncomeUtility/INFO]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        }
    }

    public static void log(String msg, Level level) {
        System.out.println(String.format("[%s IncomeUtility/%s]: ", Formatting.formatDate(Date.from(Instant.now()), true), level.getName()) + msg);
    }

    public void initialize() {
        loadData();
    }

    private void firstTimeFileSetup() {
        log("No files found, initializing first-time setup.");

        try {
            log("Creating a data directory at: " + Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2"));
            Files.createDirectory(Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2"));
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
        isSaving = true;

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
        isSaving = false;
    }

    public boolean isSaving() {
        return isSaving;
    }

    public void saveCurrentData() {
        if (currentData == null) {
            return;
        }
        saveData(currentData);
    }

    /**
     * Loads the saved data from disk.
     *
     * @return The data
     */
    private Data getData() {

        if (currentData != null) {
            return currentData;
        } else {
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
    }

    private void loadData() {
        log("Caching current data...");
        currentData = getData();
        log("Done.");
    }

    //<editor-fold desc="Data access">

    public HashSet<Account> getAccounts() {
        Data d = getData();
        return new HashSet<>(d.accounts.values());
    }

    public Optional<Account> getAccount(UUID accountId) {
        Data d = getData();
        return Optional.ofNullable(d.accounts.get(accountId));
    }

    public HashMap<UUID, Transaction> getAllTransactions() {
        Data d = getData();
        return d.getTransactions();
    }

    public List<Transaction> getTransactions(Account account) {
        return getTransactions(account.getId());
    }

    public List<Transaction> getTransactions(UUID accountId) {
        Data d = getData();
        return d.transactions.values().stream().filter(t -> t.isRelated(accountId)).toList();
    }

    public Optional<Transaction> getTransaction(UUID transactionId) {
        Data d = getData();
        return Optional.ofNullable(d.transactions.get(transactionId));
    }

    public ArrayList<String> getCustomTransactionCategories() {
        Data d = getData();
        return d.customTransactionCategories;
    }

    public Optional<UUID> getLastActiveAccount() {
        Data d = getData();
        return d.getLastActiveAccountId();
    }

    //</editor-fold>

    //<editor-fold desc="Data modification">
    public void addAccount(Account account) {
        if (currentData == null) {
            initialize();
        }
        currentData.accounts.putIfAbsent(account.getId(), account);
    }

    public void updateAccount(UUID accountId, Account data) {
        if (currentData == null) {
            initialize();
        }
        currentData.accounts.replace(accountId, data);
    }

    public void deleteAccount(UUID accountId) {
        if (currentData == null) {
            initialize();
        }
        currentData.accounts.remove(accountId);
    }


    public void addTransaction(Transaction transaction) {
        if (currentData == null) {
            initialize();
        }
        currentData.transactions.putIfAbsent(transaction.getId(), transaction);

        if (!getCustomTransactionCategories().contains(transaction.getCustomCategory())) {
            currentData.customTransactionCategories.add(transaction.getCustomCategory());
        }
    }

    public void updateTransaction(UUID transactionId, Transaction data) {
        if (currentData == null) {
            initialize();
        }
        currentData.transactions.replace(transactionId, data);
        log("Updated transaction #" + transactionId);
    }

    public void deleteTransaction(UUID transactionId) {
        if (currentData == null) {
            initialize();
        }
        currentData.transactions.remove(transactionId);
    }


    /**
     * Updates the saved ID of the last open account.
     *
     * @param account The account whose ID to set as the new last open account ID.
     */
    public void updateLastOpenAccount(Account account) {
        if (account == null) return;
        if (currentData == null) {
            initialize();
        }
        currentData.lastActiveAccountId = account.getId().toString();
    }
    //</editor-fold>

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

    private static class Data {
        private final HashMap<UUID, Transaction> transactions;
        private final HashMap<UUID, Account> accounts;
        private final ArrayList<String> customTransactionCategories;
        private String lastActiveAccountId;

        public Data() {
            this.transactions = new HashMap<>();
            this.accounts = new HashMap<>();
            this.customTransactionCategories = new ArrayList<>();
            this.lastActiveAccountId = "";
        }

        public HashMap<UUID, Transaction> getTransactions() {
            return transactions;
        }

        public HashMap<UUID, Account> getAccounts() {
            return accounts;
        }

        public ArrayList<String> getCustomTransactionCategories() {
            return customTransactionCategories;
        }

        public Optional<UUID> getLastActiveAccountId() {
            return lastActiveAccountId.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(lastActiveAccountId));
        }
    }
}
