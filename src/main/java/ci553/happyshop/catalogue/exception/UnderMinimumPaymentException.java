package ci553.happyshop.catalogue.exception;

public class UnderMinimumPaymentException extends Exception {
    public UnderMinimumPaymentException(double total, double minPayment) {
        super("Payment must be at least £" + minPayment + ". Your payment was: £" + total);
    }
}