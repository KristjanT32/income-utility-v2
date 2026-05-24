package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class LegacyData {
    private final HashMap<UUID, Transaction> transactions;
    private final HashMap<UUID, Account> accounts;
    private final ArrayList<String> customTransactionCategories;
    private final String lastActiveAccountId;

    public LegacyData() {
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
