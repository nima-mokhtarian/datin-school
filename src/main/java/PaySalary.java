import model.Account;
import model.Payment;
import model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class PaySalary implements Runnable {
    int index;
    SharedSource source;
    private static final Logger logger = LogManager.getLogger(PaySalary.class.getName());

    public PaySalary(SharedSource source, int index) {
        this.index = index;
        this.source = source;
    }

    @Override
    public void run() {
        logger.info("starting");
        List<Transaction> transactions = new ArrayList<>();
        List<Payment> payments = source.getPayments(index);
        for (Payment payment : payments) {
            if (doTransaction(payment)) {
                transactions.add(new Transaction(source.getDebtorAccount().getAccountNumber(), payment.getAccountNumber(), payment.getAmount()));
            }
        }
        logger.info("ending");
    }

    private synchronized boolean doTransaction(Payment payment) {
        logger.info("doing payment " + payment);
        logger.info("status of debtor account before transaction : " + source.getDebtorAccount());

        if (payment.getType().equals("debtor")) {
            return false;
        }
        Account account = source.getAccounts().stream().filter(e -> e.getAccountNumber().equals(payment.getAccountNumber())).findFirst().orElse(null);
        source.getDebtorAccount().setInventory(source.getDebtorAccount().getInventory() - payment.getAmount());
        if (account != null) {
            logger.info("status of creditor account before transaction : " + account);
            account.setInventory(account.getInventory() + payment.getAmount());
            logger.info("status of creditor account after transaction : " + account);
            logger.info("status of debtor account after transaction : " + source.getDebtorAccount());
            return true;
        }
        source.getAccounts().add(new Account(payment.getAccountNumber(), payment.getAmount()));
        return true;
    }
}