import Exceptions.PaymentException;
import Exceptions.PaymentExceptionMessages;
import model.Account;
import model.Payment;
import model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SharedSource {
    private final List<Payment> payments;
    private final List<Account> accounts;
    private final Payment debtorPayment;
    private Account debtorAccount;
    private List<Transaction> transactions;
    private static final Logger logger = LogManager.getLogger(SharedSource.class.getName());

    public SharedSource(List<Payment> payments, List<Account> accounts) {
        this.payments = payments;
        this.accounts = accounts;
        this.transactions = new ArrayList<>();
        this.debtorPayment = payments.stream().filter(e -> e.getType().equals("debtor")).findFirst().orElse(null);
        if (debtorPayment == null) {
            try {
                throw new PaymentException(PaymentExceptionMessages.NoDebtorInPaymentFile);
            } catch (PaymentException e) {
                logger.info(e.getMessage());
            }
        } else {
            this.debtorAccount = accounts.stream().filter(e -> e.getAccountNumber().equals(debtorPayment.getAccountNumber())).findFirst().orElse(null);
            if (debtorAccount == null) {
                try {
                    throw new PaymentException(PaymentExceptionMessages.NoMatchingForDebtorInAccountFile);
                } catch (PaymentException e) {
                    logger.info(e.getMessage());
                }
            }
        }
    }

    public Payment getDebtorPayment() {
        return debtorPayment;
    }

    public List<Payment> getPayments(int index) {
        return payments.subList(index * 2, Math.min((index + 1) * 2, payments.size() - 1));
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getDebtorAccount() {
        return debtorAccount;
    }

    public void addTransaction(Transaction transaction){
        this.transactions.add(transaction);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }
}