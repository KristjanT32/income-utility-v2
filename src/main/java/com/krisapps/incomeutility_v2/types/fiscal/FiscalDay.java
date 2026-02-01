package com.krisapps.incomeutility_v2.types.fiscal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class FiscalDay {
    private Date date;
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private HashMap<Account, Double> startingBalances = new HashMap<>();

    public double getStartingBalance(Account account) {
        return startingBalances.getOrDefault(account, 0.0d);
    }

    public double getEndingBalance(Account account) {
        for (Transaction transaction: transactions) {
            if (transaction.getSource() == account || transaction.getTarget() == account) {
                account.apply(transaction);
            }
        }
    }
}
