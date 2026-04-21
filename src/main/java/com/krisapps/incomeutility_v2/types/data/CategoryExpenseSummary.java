package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

import java.util.List;

public record CategoryExpenseSummary(String categoryName, List<Transaction> transactions) {

    public Double totalExpenses() {
        return transactions.stream().mapToDouble(Transaction::getAbsoluteAmount).sum();
    }

    public int transactionCount() {
        return transactions.size();
    }
}
