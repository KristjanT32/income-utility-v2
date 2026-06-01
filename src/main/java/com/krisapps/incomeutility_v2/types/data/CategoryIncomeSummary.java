package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

import java.util.List;

public record CategoryIncomeSummary(String categoryName, List<Transaction> transactions) {

    public Double totalIncome() {
        return transactions.stream().mapToDouble(Transaction::getAbsoluteAmount).sum();
    }

    public int transactionCount() {
        return transactions.size();
    }
}
