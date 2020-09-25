import Exceptions.PaymentException;
import Exceptions.PaymentExceptionMessages;
import model.Account;
import model.Payment;
import model.Transaction;

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

public class Main {
    public static void main(String[] args) {
        createPaymentFile();
        createInventoryFile();
        ArrayList<Payment> p = readPaymentFile();
        ArrayList<Account> ac = readInventoryFile();
        ArrayList<Transaction> transactions = null;
        try {
            transactions = doTransactions(p, ac);
        } catch (PaymentException e) {
            e.printStackTrace();
        }
        updateTransactionFile(transactions);
    }

    private static void updateTransactionFile(ArrayList<Transaction> transactions) {
        if (transactions == null) return;
        StringBuilder sb = new StringBuilder();
        for (Transaction t : transactions)
            sb.append(t.getDebtorDepositNumber()).append('\t').append(t.getCreditorDepositNumber()).append('\t').append(t.getNumber()).append('\n');
        Path filePathObj = Paths.get("transaction.out");
        boolean fileExists = Files.exists(filePathObj);
        if(fileExists) {
            try {
                // Appending The New Data To The Existing File
                Files.write(filePathObj, sb.toString().getBytes(), StandardOpenOption.APPEND);
                System.out.println("! Data Successfully Appended !");
            } catch (IOException ioExceptionObj) {
                System.out.println("Problem Occured While Writing To The File= " + ioExceptionObj.getMessage());
            }
        } else {
            writeFile("transaction.out", sb.toString());
            System.out.println("File Not Present! Please Check!");
        }
    }

    private static ArrayList<Transaction> doTransactions(ArrayList<Payment> p, ArrayList<Account> ac) throws PaymentException {
        ArrayList<Transaction> transactions = new ArrayList<>();
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
        }else{
            throw new PaymentException(PaymentExceptionMessages.InsufficientInventory);
        }
        return transactions;
    }

    private static void createPaymentFile() {

        if (Files.exists(Paths.get("payment.in"))) {
            try {
                Files.delete(Paths.get("payment.in"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String an = "1.10.100.1";
        int a = randBetween(1000, 9000);
        StringBuilder s = new StringBuilder("debtor" + '\t' + an + '\t' + a + '\n');

        int sum = 0;
        int num = randBetween(1, 10);
        for (int i = 1; i <= num; i++) {
            String accountNumber = createRandomAccountNumber();
            int amount = randBetween(1, a - sum);
            if (i == num) {
                amount = a - sum;
            }
            sum += amount;
            s.append("creditor" + '\t').append(accountNumber).append('\t').append(amount).append('\n');
        }
        writeFile("payment.in", s.toString());
    }

    private static void createInventoryFile() {
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
                sb.append("1.10.100.1").append("\t").append(10000);
            }
        }
        writeFile("inventory.in", sb.toString());
    }

    private static ArrayList<Payment> readPaymentFile() {
        ArrayList<Payment> res = new ArrayList<>();
        RandomAccessFile aFile;
        try {
            aFile = new RandomAccessFile("payment.in", "r");
            FileChannel inChannel = aFile.getChannel();

            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (inChannel.read(buffer) > 0) {
                buffer.flip();
                StringBuffer line = new StringBuffer();
                for (int i = 0; i < buffer.limit(); i++) {
                    char c = (char) buffer.get();
                    if (c == '\n' || c == '\r') {
                        String[] l = line.toString().split(String.valueOf('\t'));
                        res.add(new Payment(l[0], l[1], Integer.parseInt(l[2])));
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
            e.printStackTrace();
        }
        return res;
    }

    private static ArrayList<Account> readInventoryFile() {
        ArrayList<Account> res = new ArrayList<>();
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
            e.printStackTrace();
        }
        return res;
    }

    private static void writeFile(String name, String text) {
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
            e.printStackTrace();
        }
    }

    private static int randBetween(int lower, int upper) {
        return (int) (Math.random() * (upper - lower)) + lower;
    }

    private static String createRandomAccountNumber() {
        int p1 = randBetween(1, 1);
        int p2 = randBetween(10, 10);
        int p3 = randBetween(100, 100);
        int p4 = randBetween(2, 9);
        return p1 + "." + p2 + "." + p3 + "." + p4;
    }

}
