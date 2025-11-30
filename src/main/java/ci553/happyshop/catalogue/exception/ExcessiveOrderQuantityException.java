package ci553.happyshop.catalogue.exception;

public class ExcessiveOrderQuantityException extends Exception {
    public ExcessiveOrderQuantityException(String productId, int qty, int maxQty) {
        super("Product " + productId + " cannot exceed quantity of " + maxQty + " You attempted: " + qty);
    }
}
