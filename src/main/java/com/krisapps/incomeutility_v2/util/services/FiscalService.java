package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FiscalService {

    private static FiscalService instance;
    private final DataManager dataManager = DataManager.getInstance();

    private FiscalService() {

    }

    public static FiscalService getInstance() {
        if (instance == null) {
            instance = new FiscalService();
        }
        return instance;
    }

    /**
     * Returns the adjustment to the supplied account based on the transaction
     *
     * @param account     The account whose balance adjustment will be returned.
     * @param transaction The transaction the adjustment will be derived from.
     * @return The adjustment value to be added to the balance of the account.
     */
    public double getBalanceAdjustment(Account account, Transaction transaction) {
        if (transaction.getType().equals(TransactionType.TRANSFER)) {
            // If the supplied account is the source of the transfer, the adjustment has to be negative.
            if (transaction.getSourceAccountId().equals(account.getId())) {
                return -transaction.getAbsoluteAmount();
            } else if (transaction.getTargetAccountId().equals(account.getId())) {
                return transaction.getAbsoluteAmount();
            } else {
                log("Transaction #" + transaction.getId() + " is unrelated to account #" + account.getId());
                return 0.0d;
            }
        } else {
            return (transaction.getType().equals(TransactionType.DEPOSIT)
                    ? transaction.getAbsoluteAmount() : transaction.getType().equals(TransactionType.WITHDRAWAL) ? -transaction.getAbsoluteAmount() : 0.0d);
        }
    }

    public boolean isExpense(Account account, Transaction transaction) {
        return getBalanceAdjustment(account, transaction) < 0;
    }

    public boolean isIncome(Account account, Transaction transaction) {
        return getBalanceAdjustment(account, transaction) > 0;
    }

    public List<Transaction> getTransactionsBefore(Account account, LocalDate date) {
        return dataManager.getTransactions(account).stream().filter(transaction -> transaction.getTimestamp().toLocalDate().isBefore(date)).toList();
    }

    public List<Transaction> getTransactionsBetween(Account account, LocalDate startInclusive, LocalDate endInclusive) {
        return dataManager.getTransactions(account).stream().filter(transaction ->
                (transaction.getTimestamp().toLocalDate().isEqual(startInclusive) || transaction.getTimestamp().toLocalDate().isAfter(startInclusive))
                && (transaction.getTimestamp().toLocalDate().isEqual(endInclusive) || transaction.getTimestamp().toLocalDate().isBefore(endInclusive))
        ).toList();
    }

    public List<Transaction> getTransactionsOn(Account account, LocalDate date) {
        return dataManager.getTransactions(account).stream().filter(transaction -> transaction.getTimestamp().toLocalDate().isEqual(date)).toList();
    }

    public List<Transaction> getTransactions(Account account) {
        return dataManager.getTransactions(account);
    }

    /**
     * Gets the starting balance of the supplied account for the supplied date.
     *
     * @param account The account whose starting balance to calculate.
     * @param date    The date for which the starting balance will be calculated.
     * @return The starting balance.
     */
    public double getStartingBalance(Account account, LocalDate date) {
        double adjustment = 0.0d;
        for (Transaction t : getTransactionsBefore(account, date)) {
            adjustment += getBalanceAdjustment(account, t);
        }
        return account.getInitialBalance() + adjustment;
    }

    /**
     * Gets the supplied account's balance as of the current date.
     * @param account The account whose balance to return
     * @return The balance as of the current date.
     */
    public double getCurrentBalance(Account account) {
//        double adjustment = 0.0d;
//        for (Transaction t : dataManager.getTransactions(account)) {
//            adjustment += getBalanceAdjustment(account, t);
//        }
//        return account.getInitialBalance() + adjustment;
        return getBalance(account, LocalDate.now());
    }

    /**
     * Gets the supplied account's balance as of the supplied date.
     * @param account The account whose balance to return
     * @param date The date as of which to calculate the balance.
     * @return The balance as of the supplied date.
     */
    public double getBalance(Account account, LocalDate date) {
        double adjustment = 0.0d;
        for (Transaction t : getTransactionsBetween(account, LocalDate.MIN, date)) {
            adjustment += getBalanceAdjustment(account, t);
        }
        return account.getInitialBalance() + adjustment;
    }

    /**
     * Gets the total cash inflow on the supplied date for the supplied account.
     * @param account The account whose inflow to calculate.
     * @param date The date whose inflow to calculate.
     * @return The total inflow on the supplied date.
     */
    public double getInflow(Account account, LocalDate date) {
        return getTransactionsOn(account, date).stream().filter(transaction -> getBalanceAdjustment(account, transaction) > 0).mapToDouble(Transaction::getAbsoluteAmount).sum();
    }

    /**
     * Gets the total cash inflow between the supplied dates for the supplied account.
     * @param account The account whose inflow to calculate.
     * @param fromInclusive The start date (incl.) of the period
     * @param toInclusive The end date (incl.) of the period
     * @return The total inflow between the start and end dates (both inclusive).
     */
    public double getInflow(Account account, LocalDate fromInclusive, LocalDate toInclusive) {
        return getTransactionsBetween(account, fromInclusive, toInclusive).stream().filter(transaction -> getBalanceAdjustment(account, transaction) > 0).mapToDouble(Transaction::getAbsoluteAmount).sum();
    }

    /**
     * Gets the total cash outflow on the supplied date for the supplied account.
     * @param account The account whose outflow to calculate.
     * @param date The date whose outflow to calculate.
     * @return The total outflow on the supplied date.
     */
    public double getOutflow(Account account, LocalDate date) {
        return getTransactionsOn(account, date).stream().filter(transaction -> getBalanceAdjustment(account, transaction) < 0).mapToDouble(t -> Math.abs(t.getAmount())).sum();
    }

    /**
     * Gets the total cash outflow between the supplied dates for the supplied account.
     * @param account The account whose outflow to calculate.
     * @param fromInclusive The start date (incl.) of the period
     * @param toInclusive The end date (incl.) of the period
     * @return The total outflow between the start and end dates (both inclusive).
     */
    public double getOutflow(Account account, LocalDate fromInclusive, LocalDate toInclusive) {
        return getTransactionsBetween(account, fromInclusive, toInclusive).stream().filter(transaction -> getBalanceAdjustment(account, transaction) < 0).mapToDouble(t -> Math.abs(t.getAmount())).sum();
    }

    /**
     * Gets the difference between the total inflow and total outflow on the supplied date for the supplied account.
     * @param account The account whose change to calculate.
     * @param date The date whose change to calculate.
     * @return The difference between the inflow and outflow on the supplied date.
     */
    public double getChange(Account account, LocalDate date) {
        return getInflow(account, date) - getOutflow(account, date);
    }

    /**
     * Gets the difference between the total inflow and total outflow between the supplied dates for the supplied account.
     * @param account The account whose change to calculate.
     * @param fromInclusive The start date (incl.) of the period.
     * @param toInclusive The end date (incl.) of the period.
     * @return The difference between the inflow and outflow between the supplied dates.
     */
    public double getChange(Account account, LocalDate fromInclusive, LocalDate toInclusive) {
        return getInflow(account, fromInclusive, toInclusive) - getOutflow(account, fromInclusive, toInclusive);
    }

    public HashSet<Account> getAccounts() {
        return dataManager.getAccounts();
    }

    public Optional<Account> getAccountByName(String accountName) {
        return dataManager.getAccounts().stream().filter(acc -> acc.getName().equals(accountName)).findFirst();
    }

    public Optional<Account> getAccountById(UUID accountId) {
        return dataManager.getAccounts().stream().filter(acc -> acc.getId().equals(accountId)).findFirst();
    }

    private void log(String msg) {
        DataManager.log("[Fiscal] " + msg);
    }
}
