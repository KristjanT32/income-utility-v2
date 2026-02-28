package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.exceptions.InvalidTransactionException;
import com.krisapps.incomeutility_v2.exceptions.OperationNotPermittedException;
import com.krisapps.incomeutility_v2.exceptions.TransactionNotPermittedException;
import com.krisapps.incomeutility_v2.types.OperationType;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewAccount;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.RejectionReason;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionService {

    private static TransactionService instance;
    private static final DataManager data = DataManager.getInstance();
    private static final FiscalService fiscal = FiscalService.getInstance();

    private TransactionService() {

    }

    public static TransactionService getInstance() {
        if (instance == null) {
            instance = new TransactionService();
        }
        return instance;
    }

    private Optional<Account> getAccountById(UUID id) {
        return data.getAccount(id);
    }

    private Optional<Transaction> getTransactionById(UUID id) {
        return data.getTransaction(id);
    }

    /**
     * Deposit <code>amount</code> to the account with the supplied ID.
     * @param amount The amount to deposit
     * @param accountId The ID of the account to deposit the money to
     * @param time The time at which the deposit takes place. Set to null to use current time and date.
     * @param category The category of the deposit
     * @param comment Transaction comment
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
        data.addTransaction(new Transaction(TransactionType.DEPOSIT, amount, accountId, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment));
        data.updateAccount(accountId, account);
        log("New transaction registered: DEPOSIT of %s to %s".formatted(amount, account.getName()));
    }

    /**
     * Withdraw <code>amount</code> from the account with the supplied ID.
     * @param amount The amount to deposit
     * @param accountId The ID of the account to deposit the money to
     * @param time The time at which the withdrawal takes place. Set to null to use current time and date.
     * @param category The category of the withdrawal
     * @param comment Transaction comment
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

        data.addTransaction(new Transaction(TransactionType.WITHDRAWAL, amount, accountId, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment));
        data.updateAccount(accountId, account);
        log("New transaction registered: WITHDRAWAL of %s from %s".formatted(amount, account.getName()));
    }

    /**
     * Transfer <code>amount</code> from account with the id <code>from</code> to the account with the id <code>to</code>.
     * @param amount The amount to transfer
     * @param from The ID of the account to withdraw the money from
     * @param to The ID of the account to deposit the money to
     * @param time The time at which the transfer takes place. Set to null to use current time and date.
     * @param category The category of the transfer
     * @param comment Transaction comment
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
        data.addTransaction(new Transaction(TransactionType.TRANSFER, amount, from, to, time != null ? time : LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()), category, customCategory, comment));
        log("New transaction registered: TRANSFER of %s from %s to %s".formatted(amount, fromAccount.getName(), toAccount.getName()));
    }

    /**
     * Performs the supplied transaction and records it as a new transaction.
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

    public void pushTransactionsTo(Account account, Transaction... transactions) {
        for (Transaction t: transactions) {
            if (Transaction.isImported(t)) {
                CashewTransaction importedTransaction = (CashewTransaction) t;
                if (importedTransactionExists(account, importedTransaction)) {
                    continue;
                }

                data.addTransaction(t);
            }
        }
    }

    public void pushTransactionsTo(Account account, List<? extends Transaction> transactions) {
        for (Transaction t: transactions) {
            if (Transaction.isImported(t)) {
                CashewTransaction importedTransaction = (CashewTransaction) t;
                if (importedTransactionExists(account, importedTransaction)) {
                    System.out.println("Skipping existing Cashew transaction #" + importedTransaction.getCashewTransactionId());
                    continue;
                }

                data.addTransaction(t);
            } else {
                if (!transactionExists(account, t)) {
                    data.addTransaction(t);
                }
            }
        }
    }

    public boolean transactionExists(Account account, Transaction transaction) {
        return data.getTransactions(account).stream().anyMatch(t -> t.getId().equals(transaction.getId()));
    }

    public boolean importedTransactionExists(Account account, CashewTransaction transaction) {
        return data.getTransactions(account).stream().filter(Transaction::isImported).anyMatch(imported -> ((CashewTransaction)imported).getCashewTransactionId().equals(transaction.getCashewTransactionId()));
    }

    private void log(String msg) {
        DataManager.log("[Transactions] " + msg);
    }
}
