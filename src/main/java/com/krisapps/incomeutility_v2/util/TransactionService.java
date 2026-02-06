package com.krisapps.incomeutility_v2.util;

import com.krisapps.incomeutility_v2.exceptions.InvalidTransactionException;
import com.krisapps.incomeutility_v2.exceptions.OperationNotPermittedException;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

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

    public void deposit(double amount, UUID accountId, Date time) {
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
        data.addTransaction(new Transaction(Transaction.Type.MONEY_RECEIVED, amount, accountId, time));
        data.updateAccount(accountId, account);
    }

    public void withdraw(double amount, UUID accountId, Date time) {
        Optional<Account> a = getAccountById(accountId);

        if (a.isEmpty()) {
            throw new OperationNotPermittedException(String.format("Cannot withdraw from %s - account does not exist.", accountId));
        }

        if (amount <= 0) {
            throw new OperationNotPermittedException(String.format("Cannot withdraw from %s - transacted amount has to be greater than 0.", accountId));
        }

        Account account = a.get();

        account.setBalance(account.getBalance() - amount);
        data.addTransaction(new Transaction(Transaction.Type.MONEY_WITHDRAWN, amount, accountId, time));
        data.updateAccount(accountId, account);
    }

    public void transfer(double amount, UUID from, UUID to, Date time) {
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

        Account fromAccount = acc1.get();
        Account toAccount = acc2.get();

        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);
        data.updateAccount(fromAccount.getId(), fromAccount);
        data.updateAccount(toAccount.getId(), toAccount);
        data.addTransaction(new Transaction(Transaction.Type.MONEY_TRANSFERRED, amount, from, to, time));
    }

    public void rollback(UUID transactionId) {
        Optional<Transaction> transaction = data.getTransaction(transactionId);

        if (transaction.isEmpty()) {
            throw new OperationNotPermittedException("Cannot rollback %s - transaction does not exist.".formatted(transactionId));
        }

        Transaction transactionObject = transaction.get();
        switch (transactionObject.getType()) {
            case MONEY_RECEIVED -> {
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
            case MONEY_WITHDRAWN -> {
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
            case MONEY_TRANSFERRED -> {
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
    }
}
