package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

import java.util.List;
import java.util.Objects;

public class CategorySummary {
    private final String categoryName;
    private final List<Transaction> transactions;
    private final SummaryType type;

    public enum SummaryType {
        EXPENSES,
        INCOME,
        OTHER,
    }

    public CategorySummary(String categoryName, List<Transaction> transactions, SummaryType type) {
        this.categoryName = categoryName;
        this.transactions = transactions;
        this.type = type;
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double sumTransactions() {
        return transactions.stream().mapToDouble(Transaction::getAbsoluteAmount).sum();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CategorySummary) obj;
        return Objects.equals(this.categoryName, that.categoryName) &&
                Objects.equals(this.transactions, that.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName, transactions);
    }

    @Override
    public String toString() {
        return "CategorySummary[" +
                "categoryName=" + categoryName + ", " +
                "transactions=" + transactions + ']';
    }

}
