package com.krisapps.incomeutility_v2.types.fiscal;

import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

public class Transaction {
    private final UUID id;
    private TransactionType type;
    private double amount;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private TransactionCategory category;
    private String customCategory;
    private String comment;
    private LocalDateTime timestamp;

    public Transaction(TransactionType type, double amount, UUID account, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, @Nullable UUID id) {
        this.type = type;
        this.amount = amount;
        if (type == TransactionType.DEPOSIT || type == TransactionType.WITHDRAWAL) {
            this.targetAccountId = account;
        }
        this.category = category == null ? TransactionCategory.of(type) : category;
        this.customCategory = customCategory == null ? "" : customCategory;
        this.comment = comment == null ? "" : comment;

        this.id = (id == null ? UUID.randomUUID() : id);
        this.timestamp = timestamp;
    }
    public Transaction(TransactionType type, double amount, UUID from, UUID to, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, @Nullable UUID id) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountId = from;
        this.targetAccountId = to;
        this.category = category == null ? TransactionCategory.of(type) : category;
        this.customCategory = customCategory == null ? "" : customCategory;
        this.comment = comment == null ? "" : comment;
        this.id = (id == null ? UUID.randomUUID() : id);
        this.timestamp = timestamp;
    }
    public Transaction() {
        this.type = TransactionType.WITHDRAWAL;
        this.amount = 0;
        this.sourceAccountId = null;
        this.targetAccountId = null;
        this.category = TransactionCategory.UNKNOWN;
        this.customCategory = "";
        this.comment = "";
        this.id = UUID.randomUUID();
        this.timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    }

    public static boolean isImported(Transaction t) {
        return t instanceof CashewTransaction;
    }

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAbsoluteAmount() {
        return Math.abs(amount);
    }

    /**
     * Returns the source of the outgoing transfer of the transaction.
     * If the transaction is not a transfer, use {@link Transaction#getTargetAccountId()} to retrieve the affected account ID.
     *
     * @return The account ID.
     */
    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    /**
     * Returns the target of the incoming transfer of the transaction.
     * If the transaction is not a transfer, returns the ID of the affected account.
     *
     * @return The account ID.
     */
    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setTime(LocalTime time) {
        LocalDate date = timestamp.toLocalDate();
        setTimestamp(LocalDateTime.of(date, time));
    }

    public void setDate(LocalDate date) {
        LocalTime time = timestamp.toLocalTime();
        setTimestamp(LocalDateTime.of(date, time));
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isRelated(Account account) {
        return isRelated(account.getId());
    }

    public boolean isRelated(UUID accountId) {
        if (sourceAccountId != null && this.sourceAccountId.equals(accountId)) {
            return true;
        } else return targetAccountId != null && this.targetAccountId.equals(accountId);
    }

    public String formatAmount(DataManager dataManager, boolean absolute) {
        Optional<Account> source = dataManager.getAccount(this.sourceAccountId);
        if (source.isPresent()) {
            return Formatting.formatMoney(absolute ? Math.abs(this.amount) : this.amount, source.get().getCurrencyConfig().getCurrencySymbol(), source.get().getCurrencyConfig().isCurrencySymbolPrefix());
        } else {
            return Formatting.formatMoney(absolute ? Math.abs(this.amount) : this.amount, CurrencyConfig.DEFAULT.getCurrencySymbol(), CurrencyConfig.DEFAULT.isCurrencySymbolPrefix());
        }
    }

    public Transaction copy() {
        return new Transaction(
                this.type,
                this.amount,
                this.sourceAccountId,
                this.targetAccountId,
                this.timestamp,
                this.category,
                this.customCategory,
                this.comment,
                this.id
        );
    }
}
