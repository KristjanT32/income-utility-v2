package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.exceptions.InvalidTransactionException;
import com.krisapps.incomeutility_v2.exceptions.OperationNotPermittedException;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.organization.TransactionCategory;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class TransactionService {

    private static TransactionService instance;
    private static final DataManager data = DataManager.getInstance();

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
     * @param time The time at which the deposit takes place
     * @param category The category of the deposit
     * @param comment Transaction comment
     */
    public void deposit(double amount, UUID accountId, Date time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> a = getAccountById(accountId);

        if (a.isEmpty()) {
            throw new OperationNotPermittedException(String.format("Cannot deposit to %s - account does not exist.", accountId));
        }

        if (amount <= 0) {
            throw new OperationNotPermittedException(String.format("Cannot deposit to %s - transacted amount has to be greater than 0.", accountId));
        }

        Account account = a.get();

        double before = account.getBalance();
        account.setBalance(before + amount);
        data.addTransaction(new Transaction(Transaction.Type.DEPOSIT, amount, accountId, time, category, customCategory, comment));
        data.updateAccount(accountId, account);
        log("New transaction registered: DEPOSIT of %s to %s".formatted(amount, account.getName()));
    }

    /**
     * Withdraw <code>amount</code> from the account with the supplied ID.
     * @param amount The amount to deposit
     * @param accountId The ID of the account to deposit the money to
     * @param time The time at which the deposit takes place
     * @param category The category of the deposit
     * @param comment Transaction comment
     */
    public void withdraw(double amount, UUID accountId, Date time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> a = getAccountById(accountId);

        if (a.isEmpty()) {
            throw new OperationNotPermittedException(String.format("Cannot withdraw from %s - account does not exist.", accountId));
        }

        if (amount <= 0) {
            throw new OperationNotPermittedException(String.format("Cannot withdraw from %s - transacted amount has to be greater than 0.", accountId));
        }

        if (amount > a.get().getBalance()) {
            throw new OperationNotPermittedException(String.format("Cannot withdraw from %s - insufficient balance to complete transaction.", accountId));
        }

        Account account = a.get();

        account.setBalance(account.getBalance() - amount);
        data.addTransaction(new Transaction(Transaction.Type.WITHDRAWAL, amount, accountId, time, category, customCategory, comment));
        data.updateAccount(accountId, account);
        log("New transaction registered: WITHDRAWAL of %s from %s".formatted(amount, account.getName()));
    }

    /**
     * Transfer <code>amount</code> from account with the id <code>from</code> to the account with the id <code>to</code>.
     * @param amount The amount to transfer
     * @param from The ID of the account to withdraw the money from
     * @param to The ID of the account to deposit the money to
     * @param time The time at which the deposit takes place
     * @param category The category of the deposit
     * @param comment Transaction comment
     */
    public void transfer(double amount, UUID from, UUID to, Date time, @Nullable TransactionCategory category, @Nullable String customCategory, @Nullable String comment) {
        Optional<Account> acc1 = getAccountById(from);
        Optional<Account> acc2 = getAccountById(to);

        if (acc1.isEmpty()) {
            throw new OperationNotPermittedException(String.format("Cannot transfer from %s - account does not exist.", from));
        }

        if (acc2.isEmpty()) {
            throw new OperationNotPermittedException(String.format("Cannot transfer to %s - account does not exist.", to));
        }

        if (amount <= 0) {
            throw new OperationNotPermittedException("Cannot complete transfer - transacted amount has to be greater than 0.");
        }

        if (amount > acc1.get().getBalance()) {
            throw new OperationNotPermittedException(String.format("Cannot transfer from %s - insufficient balance to complete transaction.", acc1.get().getId()));
        }

        Account fromAccount = acc1.get();
        Account toAccount = acc2.get();

        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);
        data.updateAccount(fromAccount.getId(), fromAccount);
        data.updateAccount(toAccount.getId(), toAccount);
        data.addTransaction(new Transaction(Transaction.Type.TRANSFER, amount, from, to, time, category, customCategory, comment));
        log("New transaction registered: TRANSFER of %s from %s to %s".formatted(amount, fromAccount.getName(), toAccount.getName()));
    }

    /**
     * Performs the supplied transaction and records it as a new transaction.
     * @param transaction The transaction to perform
     */
    public void perform(Transaction transaction) {
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

    public void rollback(UUID transactionId) {
        Optional<Transaction> transaction = data.getTransaction(transactionId);

        if (transaction.isEmpty()) {
            throw new OperationNotPermittedException("Cannot rollback %s - transaction does not exist.".formatted(transactionId));
        }

        Transaction transactionObject = transaction.get();
        switch (transactionObject.getType()) {
            case DEPOSIT -> {
                Optional<Account> a = getAccountById(transactionObject.getTargetAccountId());
                if (a.isPresent()) {
                    Account account = a.get();
                    account.setBalance(account.getBalance() - transactionObject.getAmount());
                    data.updateAccount(account.getId(), account);
                    data.deleteTransaction(transactionId);
                } else {
                    throw new InvalidTransactionException("Cannot rollback %s - transaction target account does not exist.".formatted(transactionId));
                }
            }
            case WITHDRAWAL -> {
                Optional<Account> a = getAccountById(transactionObject.getTargetAccountId());
                if (a.isPresent()) {
                    Account account = a.get();
                    account.setBalance(account.getBalance() + transactionObject.getAmount());
                    data.updateAccount(account.getId(), account);
                    data.deleteTransaction(transactionId);
                } else {
                    throw new InvalidTransactionException("Cannot rollback %s - transaction target account does not exist.".formatted(transactionId));
                }
            }
            case TRANSFER -> {
                Optional<Account> a = getAccountById(transactionObject.getSourceAccountId());
                Optional<Account> b = getAccountById(transactionObject.getTargetAccountId());
                if (a.isPresent() && b.isPresent()) {
                    Account from = a.get();
                    Account to = b.get();

                    // From receives back, to loses the transaction amount.
                    from.setBalance(from.getBalance() + transactionObject.getAmount());
                    to.setBalance(to.getBalance() - transactionObject.getAmount());

                    data.updateAccount(from.getId(), from);
                    data.updateAccount(to.getId(), to);
                    data.deleteTransaction(transactionId);
                } else {
                    throw new InvalidTransactionException("Cannot rollback %s - transaction %s account does not exist.".formatted(transactionId, (a.isEmpty() ? "source" : "target")));
                }
            }
        }
        log("Rolled back transaction %s of type %s".formatted(transactionId, transactionObject.getType().getDisplayName()));
    }

    private void log(String msg) {
        DataManager.log(msg);
    }
}
