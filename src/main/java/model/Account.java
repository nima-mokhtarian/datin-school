package model;

public class Account {
    private String accountNumber;
    private int inventory;

    public Account(String accountNumber, int inventory) {
        this.accountNumber = accountNumber;
        this.inventory = inventory;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getInventory() {
        return inventory;
    }

    public void setInventory(int inventory) {
        this.inventory = inventory;
    }

    @Override
    public String toString() {
        return "model.Account{" +
                "accountNumber='" + accountNumber + '\'' +
                ", inventory=" + inventory +
                '}';
    }
}
