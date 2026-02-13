package com.krisapps.incomeutility_v2.types.fiscal;

import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class Transaction {
    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public Transaction(TransactionType type, double amount, UUID account, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment) {
        this.type = type;
        this.amount = amount;
        if (type == TransactionType.DEPOSIT || type == TransactionType.WITHDRAWAL) {
            this.targetAccountId = account;
        }
        this.category = category == null ? TransactionCategory.of(type) : category;
        this.customCategory = customCategory == null ? "" : customCategory;
        this.comment = comment == null ? "" : comment;

        this.id = UUID.randomUUID();
        this.timestamp = timestamp;
    }

    public Transaction(TransactionType type, double amount, UUID from, UUID to, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountId = from;
        this.targetAccountId = to;
        this.category = category == null ? TransactionCategory.of(type) : category;
        this.customCategory = customCategory == null ? "" : customCategory;
        this.comment = comment == null ? "" : comment;
        this.id = UUID.randomUUID();
        this.timestamp = timestamp;
    }

    private TransactionType type;
    private double amount;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private TransactionCategory category;
    private String customCategory;
    private String comment;

    private LocalDateTime timestamp;
    private final UUID id;

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public String getComment() {
        return comment;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRelated(Account account) {
        return isRelated(account.getId());
    }

    public boolean isRelated(UUID accountId) {
        if (sourceAccountId != null && this.sourceAccountId.equals(accountId)) {
            return true;
        } else return targetAccountId != null && this.targetAccountId.equals(accountId);
    }

    public String formatAmount(DataManager dataManager) {
        Optional<Account> source = dataManager.getAccount(this.sourceAccountId);
        if (source.isPresent()) {
            return DataManager.Formatting.formatMoney(this.amount, source.get().getCurrencyConfig().getCurrencySymbol(), source.get().getCurrencyConfig().isCurrencySymbolPrefix());
        } else {
            return DataManager.Formatting.formatMoney(this.amount, CurrencyConfig.DEFAULT.getCurrencySymbol(), CurrencyConfig.DEFAULT.isCurrencySymbolPrefix());
        }
    }
}
