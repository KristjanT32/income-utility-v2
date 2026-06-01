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

    /**
     * Checks whether the supplied transaction is an expense on the supplied account.
     *
     * @param account     The account whose transaction is supplied.
     * @param transaction The transaction to check.
     * @return <code>true</code> if the supplied transaction results in a negative adjustment to the supplied account's balance, <code>false</code> otherwise.
     */
    public boolean isExpense(Account account, Transaction transaction) {
        return getBalanceAdjustment(account, transaction) < 0;
    }

    /**
     * Checks whether the supplied transaction is income on the supplied account.
     * @param account The account whose transaction is supplied.
     * @param transaction The transaction to check.
     * @return <code>true</code> if the supplied transaction results in a positive adjustment to the supplied account's balance, <code>false</code> otherwise.
     */
    public boolean isIncome(Account account, Transaction transaction) {
        return getBalanceAdjustment(account, transaction) > 0;
    }

    /**
     * Retrieves a list of transactions with timestamps before the supplied date.
     * @param account The account whose transactions to retrieve.
     * @param date The date to compare to (exclusive)
     * @return All transactions of the supplied account with a date earlier than the supplied date. Transactions with the same date as the supplied one are excluded.
     */
    public List<Transaction> getTransactionsBefore(Account account, LocalDate date) {
        return dataManager.getTransactions(account).stream().filter(transaction -> transaction.getTimestamp().toLocalDate().isBefore(date)).toList();
    }

    /**
     * Retrieves a list of transactions with dates between the supplied dates (both inclusive).
     * @param account The accounts whose transactions to retrieve.
     * @param startInclusive The start date of the period.
     * @param endInclusive The end date of the period.
     * @return All transactions of the supplied account with a date between the supplied dates.
     */
    public List<Transaction> getTransactionsBetween(Account account, LocalDate startInclusive, LocalDate endInclusive) {
        return dataManager.getTransactions(account).stream().filter(transaction ->
                (transaction.getTimestamp().toLocalDate().isEqual(startInclusive) || transaction.getTimestamp().toLocalDate().isAfter(startInclusive))
                && (transaction.getTimestamp().toLocalDate().isEqual(endInclusive) || transaction.getTimestamp().toLocalDate().isBefore(endInclusive))
        ).toList();
    }


    /**
     * Retrieves a list of transactions which took place on the supplied date.
     * @param account The account whose transactions to retrieve.
     * @param date The date whose transactions to retrieve.
     * @return All transactions of the supplied account on the supplied date.
     */
    public List<Transaction> getTransactionsOn(Account account, LocalDate date) {
        return dataManager.getTransactions(account).stream().filter(transaction -> transaction.getTimestamp().toLocalDate().isEqual(date)).toList();
    }

    /**
     * Retrieves a list of transactions for the supplied account.
     * @param account The account whose transactions to retrieve.
     * @return All transactions for the supplied account.
     */
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
     * Gets the total cash inflow for the supplied account, based on the supplied transactions.
     *
     * @param account      The account associated with the supplied transactions.
     * @param transactions The transactions to use in order to calculate the inflow.
     * @return The total inflow within the supplied transactions.
     */
    public double getInflow(Account account, List<? extends Transaction> transactions) {
        return transactions.stream().filter(transaction -> getBalanceAdjustment(account, transaction) > 0).mapToDouble(Transaction::getAbsoluteAmount).sum();
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
     * Gets the total cash outflow for the supplied account, based on the supplied transactions.
     *
     * @param account      The account associated with the supplied transactions.
     * @param transactions The transactions to use in order to calculate the outflow.
     * @return The total outflow within the supplied transactions.
     */
    public double getOutflow(Account account, List<? extends Transaction> transactions) {
        return transactions.stream().filter(transaction -> getBalanceAdjustment(account, transaction) < 0).mapToDouble(Transaction::getAbsoluteAmount).sum();
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
     * Gets the difference between the total inflow and total outflow for the supplied account, based on the supplied transactions.
     *
     * @param account      The account whose change to calculate.
     * @param transactions The transactions to use in order to calculate the change.
     * @return The difference between the inflow and outflow based on the supplied transactions.
     */
    public double getChange(Account account, List<? extends Transaction> transactions) {
        return getInflow(account, transactions) - getOutflow(account, transactions);
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

    /**
     * Retrieves a set of all existing local accounts.
     * @return All existing local accounts.
     */
    public HashSet<Account> getAccounts() {
        return dataManager.getAccounts();
    }

    /**
     * Retrieves an account by the supplied display name.
     * @param accountName The display name of the account to look for.
     * @return An Optional with an account with the supplied display name, or an empty Optional, if none could be found.
     */
    public Optional<Account> getAccountByName(String accountName) {
        return dataManager.getAccounts().stream().filter(acc -> acc.getName().equals(accountName)).findFirst();
    }

    /**
     * Retrieves an account by the supplied ID.
     * @param accountId The ID of the account to look for.
     * @return An Optional with an account with the supplied ID, or an empty Optional, if none could be found.
     */
    public Optional<Account> getAccountById(UUID accountId) {
        return dataManager.getAccounts().stream().filter(acc -> acc.getId().equals(accountId)).findFirst();
    }

    private void log(String msg) {
        DataManager.log("[Fiscal] " + msg);
    }
}
