import Exceptions.PaymentException;
import Exceptions.PaymentExceptionMessages;
import model.Account;
import model.Payment;
import model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;


public class SalaryPayment {
    private static final Logger logger = LogManager.getLogger(SalaryPayment.class.getName());

    public static void updateTransactionFile(List<Transaction> transactions) {
        if (transactions == null) return;
        StringBuilder sb = new StringBuilder();
        for (Transaction t : transactions)
            sb.append(t.getDebtorDepositNumber()).append('\t').append(t.getCreditorDepositNumber()).append('\t').append(t.getNumber()).append('\n');
        Path filePathObj = Paths.get("transaction.out");
        boolean fileExists = Files.exists(filePathObj);
        if (fileExists) {
            try {
                Files.write(filePathObj, sb.toString().getBytes(), StandardOpenOption.APPEND);
                logger.info("! Data Successfully Appended !");
            } catch (IOException ioExceptionObj) {
                logger.info("Problem occurred while writing To The File= " + ioExceptionObj.getMessage());
            }
        } else {
            writeFile("transaction.out", sb.toString());
            logger.info("File Not Present! Please Check!");
        }
    }

    public static List<Transaction> doTransactions(List<Payment> p, List<Account> ac) throws PaymentException {
        List<Transaction> transactions = new ArrayList<>();
        Payment debtorPayment = p.stream().filter(e -> e.getType().equals("debtor")).findFirst().orElse(null);
        if (debtorPayment == null) {
            throw new PaymentException(PaymentExceptionMessages.NoDebtorInPaymentFile);
        }
        Account debtorAccount = ac.stream().filter(e -> e.getAccountNumber().equals(debtorPayment.getAccountNumber())).findFirst().orElse(null);
        if (debtorAccount == null) {
            throw new PaymentException(PaymentExceptionMessages.NoMatchingForDebtorInAccountFile);
        }
        if (debtorPayment.getAmount() <= debtorAccount.getInventory()) {
            debtorAccount.setInventory(debtorAccount.getInventory() - debtorPayment.getAmount());
            for (Payment pp : p) {
                Account a = ac.stream().filter(e -> e.getAccountNumber().equals(pp.getAccountNumber())).findFirst().orElse(null);
                if (a != null) {
                    a.setInventory(a.getInventory() + pp.getAmount());
                } else {
                    ac.add(new Account(pp.getAccountNumber(), pp.getAmount()));
                }
                transactions.add(new Transaction(debtorAccount.getAccountNumber(), pp.getAccountNumber(), pp.getAmount()));
            }
        } else {
            throw new PaymentException(PaymentExceptionMessages.InsufficientInventory);
        }
        return transactions;
    }

    public static void enhancedDoTransaction() {
        List<Payment> allPaymentFileContext = readPaymentFile();
        List<Account> allAccountFileContext = readInventoryFile();
        //shared sources between threads
        SharedSource source = new SharedSource(allPaymentFileContext, allAccountFileContext);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Payment> payments = SalaryPayment.readPaymentFile();
        int numberOfNeededThreads = (int) Math.ceil(payments.size() / 100.00);
        for (int i = 0; i < numberOfNeededThreads; i++) {
            try {
                executor.submit(new PaySalary(source, i)).get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e.getMessage());
            }
        }
        logger.info(source.getDebtorAccount());
    }

    public static void createPaymentFile() {
        if (Files.exists(Paths.get("payment.in"))) {
            try {
                Files.delete(Paths.get("payment.in"));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        String an = "1.10.100.1";
        int debtorAmount = randBetween(25000000, 26000000);
        StringBuilder s = new StringBuilder("debtor" + '\t' + an + '\t' + debtorAmount + '\n');

        int num = 2500;
        for (int i = 1; i <= num; i++) {
            String accountNumber = createRandomAccountNumber();
            int amount = randBetween(1, 10);
            s.append("creditor" + '\t').append(accountNumber).append('\t').append(amount).append('\n');
            if (i % 25 == 0 || i == num) {
                Path filePathObj = Paths.get("payment.in");
                boolean fileExists = Files.exists(filePathObj);
                if (fileExists) {
                    try {
                        Files.write(filePathObj, s.toString().getBytes(), StandardOpenOption.APPEND);
                        logger.info("Data Successfully Appended !");
                    } catch (IOException ioExceptionObj) {
                        logger.info("Problem occurred while writing To The File= " + ioExceptionObj.getMessage());
                    }
                } else {
                    writeFile("payment.in", s.toString());
                    logger.info("File Not Present! Please Check!");
                }
                s = new StringBuilder();
            }
        }
    }

    public static void createInventoryFile() {
        File f = new File("inventory.in");
        if (f.exists()) return;
        StringBuilder sb = new StringBuilder();
        int num = randBetween(1, 10);
        for (int i = 0; i < num; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            if (i != 0) {
                String s = createRandomAccountNumber();
                int a = randBetween(1, 9000);
                sb.append(s).append("\t").append(a);
            } else {
                sb.append("1.10.100.1").append("\t").append(100000000);
            }
        }
        writeFile("inventory.in", sb.toString());
    }

    public static List<Payment> readPaymentFile() {
        List<Payment> res = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get("payment.in"))) {
            stream.forEach(line -> {
                        String[] l = line.split(String.valueOf('\t'));
                        res.add(new Payment(l[0], l[1], Integer.parseInt(l[2])));
                    }
            );
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return res;
    }

    public static List<Account> readInventoryFile() {
        List<Account> res = new ArrayList<>();
        RandomAccessFile aFile;
        try {
            aFile = new RandomAccessFile("inventory.in", "r");
            FileChannel inChannel = aFile.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (inChannel.read(buffer) > 0) {
                buffer.flip();
                StringBuffer line = new StringBuffer();
                for (int i = 0; i < buffer.limit(); i++) {
                    char c = (char) buffer.get();
                    if (c == '\n' || c == '\r') {
                        String[] l = line.toString().split(String.valueOf('\t'));
                        res.add(new Account(l[0], Integer.parseInt(l[1])));
                        line = new StringBuffer();
                    } else {
                        line.append(c);
                    }
                }
                buffer.clear(); // do something with the data and clear/compact it.
            }
            inChannel.close();
            aFile.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return res;
    }

    public static void writeFile(String name, String text) {
        RandomAccessFile aFile;
        try {
            aFile = new RandomAccessFile(name, "rw");
            FileChannel inChannel = aFile.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(1024);
            buf.clear();
            buf.put(text.getBytes());
            buf.flip();
            while (buf.hasRemaining()) {
                inChannel.write(buf);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static int randBetween(int lower, int upper) {
        return (int) (Math.random() * (upper - lower)) + lower;
    }

    public static String createRandomAccountNumber() {
        int p1 = randBetween(1, 1);
        int p2 = randBetween(10, 10);
        int p3 = randBetween(100, 100);
        int p4 = randBetween(2, 9);
        return p1 + "." + p2 + "." + p3 + "." + p4;
    }
}