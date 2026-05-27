package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.exceptions.TransactionNotPermittedException;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.RejectionReason;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import org.jetbrains.annotations.Nullable;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class TransactionService {

    private static final DataManager data = DataManager.getInstance();
    private static final FiscalService fiscal = FiscalService.getInstance();
    private static TransactionService instance;

    private TransactionService() {

    }

    public static TransactionService getInstance() {
        if (instance == null) {
            instance = new TransactionService();
        }
        return instance;
    }

    /**
     * Retrieves the local account with the supplied ID.
     *
     * @param id The ID of the local account to retrieve
     * @return An Optional with the requested {@link Account}, or an empty Optional, if no such account exists.
     */
    private Optional<Account> getAccountById(UUID id) {
        return data.getAccount(id);
    }

    /**
     * Retrieves the transaction with the supplied ID.
     * @param id The ID of the transaction to retrieve
     * @return An Optional with the requested {@link Transaction}, or an empty Optional, if no such transaction exists.
     */
    private Optional<Transaction> getTransactionById(UUID id) {
        return data.getTransaction(id);
    }

    /**
     * Deposit <code>amount</code> to the account with the supplied ID.
     *
     * @param amount    The amount to deposit
     * @param accountId The ID of the account to deposit the money to
     * @param time      The time at which the deposit takes place. Set to null to use current time and date.
     * @param category  The category of the deposit
     * @param comment   Transaction comment
     */
    public void deposit(double amount, UUID accountId, LocalDateTime time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> a = getAccountById(accountId);

        if (a.isEmpty()) {
            throw new TransactionNotPermittedException(TransactionType.DEPOSIT, RejectionReason.SOURCE_NOT_FOUND, null, String.format("Cannot deposit to %s - account does not exist.", accountId));
        }

        if (amount <= 0) {
            throw new TransactionNotPermittedException(TransactionType.DEPOSIT, RejectionReason.INVALID_AMOUNT, null, String.format("Cannot deposit to %s - transacted amount has to be greater than 0.", accountId));
        }

        Account account = a.get();
        data.addTransaction(new Transaction(TransactionType.DEPOSIT, amount, accountId, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment, null));
        data.updateAccount(accountId, account);
        log("New transaction registered: DEPOSIT of %s to %s".formatted(amount, account.getName()));
    }

    /**
     * Withdraw <code>amount</code> from the account with the supplied ID.
     *
     * @param amount    The amount to deposit
     * @param accountId The ID of the account to deposit the money to
     * @param time      The time at which the withdrawal takes place. Set to null to use current time and date.
     * @param category  The category of the withdrawal
     * @param comment   Transaction comment
     */
    public void withdraw(double amount, UUID accountId, LocalDateTime time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> a = getAccountById(accountId);

        if (a.isEmpty()) {
            throw new TransactionNotPermittedException(TransactionType.WITHDRAWAL, RejectionReason.SOURCE_NOT_FOUND, null, String.format("Cannot withdraw from %s - account does not exist.", accountId));
        }

        if (amount <= 0) {
            throw new TransactionNotPermittedException(TransactionType.WITHDRAWAL, RejectionReason.INVALID_AMOUNT, null, String.format("Cannot withdraw from %s - transacted amount has to be greater than 0.", accountId));
        }

        if (amount > fiscal.getCurrentBalance(a.get())) {
            throw new TransactionNotPermittedException(TransactionType.WITHDRAWAL, RejectionReason.INSUFFICIENT_BALANCE, a.get(), String.format("Cannot withdraw from %s - insufficient balance to complete transaction.", accountId));
        }

        Account account = a.get();

        data.addTransaction(new Transaction(TransactionType.WITHDRAWAL, amount, accountId, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment, null));
        data.updateAccount(accountId, account);
        log("New transaction registered: WITHDRAWAL of %s from %s".formatted(amount, account.getName()));
    }

    /**
     * Transfer <code>amount</code> from account with the id <code>from</code> to the account with the id <code>to</code>.
     *
     * @param amount   The amount to transfer
     * @param from     The ID of the account to withdraw the money from
     * @param to       The ID of the account to deposit the money to
     * @param time     The time at which the transfer takes place. Set to null to use current time and date.
     * @param category The category of the transfer
     * @param comment  Transaction comment
     */
    public void transfer(double amount, UUID from, UUID to, LocalDateTime time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> acc1 = getAccountById(from);
        Optional<Account> acc2 = getAccountById(to);

        if (acc1.isEmpty()) {
            throw new TransactionNotPermittedException(TransactionType.TRANSFER, RejectionReason.SOURCE_NOT_FOUND, null, String.format("Cannot transfer from %s - account does not exist.", from));
        }

        if (acc2.isEmpty()) {
            throw new TransactionNotPermittedException(TransactionType.TRANSFER, RejectionReason.TARGET_NOT_FOUND, null, String.format("Cannot transfer to %s - account does not exist.", to));
        }

        if (amount <= 0) {
            throw new TransactionNotPermittedException(TransactionType.TRANSFER, RejectionReason.INVALID_AMOUNT, null, "Cannot complete transfer - transacted amount has to be greater than 0.");
        }

        if (amount > fiscal.getCurrentBalance(acc1.get())) {
            throw new TransactionNotPermittedException(TransactionType.TRANSFER, RejectionReason.INSUFFICIENT_BALANCE, acc1.get(), String.format("Cannot transfer from %s - insufficient balance to complete transaction.", acc1.get().getId()));
        }

        Account fromAccount = acc1.get();
        Account toAccount = acc2.get();

        data.updateAccount(fromAccount.getId(), fromAccount);
        data.updateAccount(toAccount.getId(), toAccount);
        data.addTransaction(new Transaction(TransactionType.TRANSFER, amount, from, to, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment, null));
        log("New transaction registered: TRANSFER of %s from %s to %s".formatted(amount, fromAccount.getName(), toAccount.getName()));
    }

    /**
     * Performs the supplied transaction and records it as a new transaction.
     *
     * @param transaction The transaction to perform
     */
    public void perform(Transaction transaction) throws TransactionNotPermittedException {
        switch (transaction.getType()) {
            case DEPOSIT -> {
                deposit(transaction.getAmount(), transaction.getTargetAccountId(), transaction.getTimestamp(), transaction.getCategory(), transaction.getCustomCategory(), transaction.getComment());
            }
            case WITHDRAWAL -> {
                withdraw(transaction.getAmount(), transaction.getTargetAccountId(), transaction.getTimestamp(), transaction.getCategory(), transaction.getCustomCategory(), transaction.getComment());
            }
            case TRANSFER -> {
                transfer(transaction.getAmount(), transaction.getSourceAccountId(), transaction.getTargetAccountId(), transaction.getTimestamp(), transaction.getCategory(), transaction.getCustomCategory(), transaction.getComment());
            }
        }
    }

    /**
     * Pushes the supplied transactions to the supplied account's transaction registry.
     * @param account The account with which the transactions should be associated.
     * @param transactions A list of transactions to push.
     * @return The number of transactions pushed to the account.
     */
    public int pushTransactionsTo(Account account, List<? extends Transaction> transactions) {
        int count = 0;

        for (Transaction t : transactions) {
            if (Transaction.isImported(t)) {
                CashewTransaction importedTransaction = (CashewTransaction) t;
                if (importedTransactionExists(account, importedTransaction)) {
                    log("Skipping already imported transaction: #%s (%s)".formatted(importedTransaction.getCashewTransactionId(), importedTransaction.getId()));
                    continue;
                }

                data.addTransaction(t);
                count++;
            } else {
                if (!transactionExists(account, t)) {
                    data.addTransaction(t);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Deletes all transactions for the supplied account.
     * All transfers will be converted to either deposits or withdrawals.
     * @param account The account whose transactions to delete.
     */
    public void deleteTransactionsFor(Account account) {
        // FIXME: This should be refactored
        if (account == null) {
            throw new InvalidParameterException("Account cannot be null!");
        }

        Collection<Transaction> transactions = new ArrayList<>(data.getAllTransactions().values().stream().toList());
        for (Transaction t: transactions) {
            if (t.isRelated(account.getId())) {
                if (t.getType().equals(TransactionType.TRANSFER)) {
                    log("Converting transfer #" + t.getId());
                    Transaction converted = convertTransfer(account, t);

                    data.updateTransaction(t.getId(), converted);
                } else {
                    log("Deleting transaction #" + t.getId());
                    data.deleteTransaction(t.getId());
                }
            }
        }
    }

    /**
     * Converts the supplied transfer transaction to either a deposit or withdrawal, based on the supplied account.
     * If the transfer originates from the supplied account, it will be converted to a deposit to the target account.
     * If the target of the transfer is the supplied account, it will be converted to a withdrawal from the source account.
     * @param origin The account which is either the source or the target of the supplied transfer.
     * @param t The transfer transaction.
     * @return The converted transaction.
     */
    public Transaction convertTransfer(Account origin, Transaction t) {
        if (origin == null) {
            throw new InvalidParameterException("Account cannot be null");
        }

        if (t == null) {
            throw new InvalidParameterException("Transaction cannot be null");
        }

        if (!t.getType().equals(TransactionType.TRANSFER)) {
            throw new InvalidParameterException("The supplied transaction id does not point to a transfer!");
        }

        if (t.getSourceAccountId().equals(origin.getId())) {
            // If the transfer originates from the supplied account,
            // convert it to a deposit

            t.setType(TransactionType.DEPOSIT);
            t.setSourceAccountId(null);
        } else if (t.getTargetAccountId().equals(origin.getId())) {
            // If the target of the transfer is the supplied account,
            // convert it to a withdrawal from the source
            t.setType(TransactionType.WITHDRAWAL);
            t.setSourceAccountId(null);
        }

        return t;
    }

    /**
     * Checks if the supplied transaction exists.
     * @param account The account whose transaction is supplied
     * @param transaction The transaction to check
     * @return <code>true</code> if the supplied transaction exists, <code>false</code> otherwise.
     */
    public boolean transactionExists(Account account, Transaction transaction) {
        return data.transactionExists(account, transaction);
    }

    /**
     * Checks if the supplied imported transaction exists.
     * @param account The account whose transaction is supplied
     * @param transaction The imported transaction to check
     * @return <code>true</code> if the supplied imported transaction exists, <code>false</code> otherwise.
     */
    public boolean importedTransactionExists(Account account, CashewTransaction transaction) {
        return data.importedTransactionExists(account, transaction);
    }

    private void log(String msg) {
        DataManager.log("[Transactions] " + msg);
    }
}
