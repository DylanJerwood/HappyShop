package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;

import java.io.IOException;
import java.sql.SQLException;

public class CustomerController {
    public CustomerModel cusModel;

    public void doAction(String action) throws SQLException, IOException {
        switch (action) {
            case "🔍":
                cusModel.search();
                break;
            case "🛒":
                cusModel.addToTrolley();
                break;
            case "Cancel":
                cusModel.cancel();
                break;
            case "Check Out":
                cusModel.checkOut();
                break;
            case "OK & Close":
                cusModel.closeReceipt();
                break;
        }
    }

    /**
     * Changes order quantity of a product in the trolley.
     * If quantity is <= 0, the product will be removed.
     *
     * @param selectedProduct Product that's quantity is to be changed.
     * @param changeBy The amount to change the current ordered quantity by.
     */
    public void changeOrderQuantity(Product selectedProduct, Integer changeBy) {
        if (selectedProduct == null || changeBy == null) return;

        int newQuantity = selectedProduct.getOrderedQuantity() + changeBy;
        cusModel.updateTrolleyProductQuantity(selectedProduct, newQuantity); // Use the helper method
    }

    /**
     * Removes a product entirely from the trolley.
     * Delegates removal logic to model class for consistent behavior.
     *
     * @param selectedProduct Product to remove.
     */
    public void removeFromTrolley(Product selectedProduct) {
        if (selectedProduct == null) return;

        cusModel.updateTrolleyProductQuantity(selectedProduct, 0); // Removes the product
    }

}
