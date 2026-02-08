package com.krisapps.incomeutility_v2.types.fiscal;

import com.krisapps.incomeutility_v2.types.organization.TransactionCategory;
import com.krisapps.incomeutility_v2.util.DataManager;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class Transaction {
    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public enum Type {
        DEPOSIT("Deposit"),
        WITHDRAWAL("Withdrawal"),
        TRANSFER("Transfer"),
        ;

        private final String displayName;
        private Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Transaction(Type type, double amount, UUID account, Date timestamp, TransactionCategory category, String customCategory, String comment) {
        this.type = type;
        this.amount = amount;
        if (type == Type.DEPOSIT || type == Type.WITHDRAWAL) {
            this.targetAccountId = account;
        }
        this.category = category == null ? TransactionCategory.of(type) : category;
        this.customCategory = customCategory == null ? "" : customCategory;
        this.comment = comment == null ? "" : comment;

        this.id = UUID.randomUUID();
        this.timestamp = timestamp;
    }

    public Transaction(Type type, double amount, UUID from, UUID to, Date timestamp, TransactionCategory category, String customCategory, String comment) {
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

    private Transaction.Type type;
    private double amount;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private TransactionCategory category;
    private String customCategory;
    private String comment;

    private Date timestamp;
    private final UUID id;

    public Type getType() {
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

    public Date getTimestamp() {
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

    public void setType(Type type) {
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

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRelated(Account account) {
        return isRelated(account.getId());
    }

    public boolean isRelated(UUID accountId) {
        if (sourceAccountId != null) {
            return this.sourceAccountId.equals(accountId);
        } else if (targetAccountId != null) {
            return this.targetAccountId.equals(accountId);
        } else {
            return false;
        }
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
