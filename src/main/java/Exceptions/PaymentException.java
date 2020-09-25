package Exceptions;

public class PaymentException extends Exception {
    public PaymentException(String pem) {
        super(pem);
    }
}
