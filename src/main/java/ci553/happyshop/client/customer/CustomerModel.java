package ci553.happyshop.client.customer;

import ci553.happyshop.businessRules.OrderRules;
import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.exception.ExcessiveOrderQuantityException;
import ci553.happyshop.catalogue.exception.UnderMinimumPaymentException;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */

public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.

    private Product theProduct = null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley
    private ArrayList<Product> searchList = new ArrayList<>(); // search results fetched from the database

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)


    /**
     * Searches using text entered by the user.
     * <p>
     * Behaviour:
     * <ul>
     *     <li>If a keyword is present, attempts to access the database.</li>
     *     <li>If no products were found, displays a message.</li>
     *     <li>If no keyword is present, clears previous results and prompts for input.</li>
     *     <li>Any SQL errors are caught and shown to the user.</li>
     * </ul>
     * <p>
     *
     * @throws SQLException if the database layer reports an unrecoverable error.
     */
    void search() throws SQLException {
        final String keyword = cusView.tfId.getText().trim();
        // --- validation: for no empty searches ---
        if (keyword.isEmpty()) {
            handleEmptySearch();
            updateView();
            return;
        }

        try {
            searchList = databaseRW.searchProduct(keyword);
            if (searchList.isEmpty()) {
                displayLaSearchResult = "No products found.";
            } else {
                displayLaSearchResult = searchList.size() + " product(s) found.";
            }

        } catch (SQLException ex) {
            // Robust DB error handling (can be logged or rethrown based on design)
            displayLaSearchResult = "Database error during search.";
            System.err.println("Search failed: " + ex.getMessage());
            throw ex;  // or log+return depending on design
        }
        updateView();
          /* *PLACEHOLDER NO LONGER NEEDED, ABOVE CODE SERVES SIMILAR PURPOSE HOWEVER BELOW CODE IS LEFT IN FOR COMPARISON***
        String productId = cusView.tfId.getText().trim();
        if(!productId.isEmpty()){
            theProduct = databaseRW.searchByProductId(productId); //search database
            if(theProduct != null && theProduct.getStockQuantity()>0){
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();
                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            }else{
                theProduct=null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            } }else{
            theProduct=null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");}
        updateView(); */
    }

    /**
     * Adds currently selected product from the views listview product list to the trolley.
     * <p>
     * If product is already in trolley, increments amount ordered.
     * Otherwise, adds the product to the trolley.
     * Trolley is then sorted by product ID.
     * Updates the display string for the trolley.
     * <p>
     * If no product is selected, displays a message to the user.
     */
    void addToTrolley() {
        // Finds which item is currently selected in the listview
        Product selectedProduct = cusView.obrLvProducts.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            displayLaSearchResult = "Please select an available product before adding it to the trolley.";
            System.out.println("No product selected before adding to trolley.");
            updateView();
            return;
        }

        boolean found = false;
        for (Product p : trolley) {
            if (selectedProduct.getProductId().equals(p.getProductId())) {
                p.setOrderedQuantity(p.getOrderedQuantity() + 1);
                found = true;
                break;
            }
        }
        if (!found) {
            trolley.add(new Product(selectedProduct));
        }

        // Sort trolley by Product ID can use other comparator
        trolley = OrganizeTrolley(trolley, Comparator.comparing(Product::getProductId));
        // Build the display string
        displayTaTrolley = ProductListFormatter.buildString(trolley);
        displayTaReceipt = "";

        updateView();
        /* ***PLACEHOLDER NO LONGER NEEDED, ABOVE CODE SERVES SIMILAR PURPOSE HOWEVER BELOW CODE IS LEFT IN FOR COMPARISON***
        if(theProduct!= null){
            boolean found = false;      // Bool to track in product is already in trolley
            for (Product p : trolley){  // Checks if product is already in trolley
                if(theProduct.getProductId().equals(p.getProductId())){
                    p.setOrderedQuantity(p.getOrderedQuantity() + 1);
                    found = true;
                    break; }}
            if (!found){
                // trolley.add(theProduct) — Product is appended to the end of the trolley.
                trolley.add(theProduct); }
            //  CAN USE OrganizeTrolley() WITHOUT COMPARATOR, COMPARATOR VERSION IS USED FOR FLEXIBILITY
            trolley = OrganizeTrolley(trolley, Comparator.comparing(Product::getProductId));
            displayTaTrolley = ProductListFormatter.buildString(trolley); // Build a String for trolley so that we can show it }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");}
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();*/
    }

    void checkOut() throws IOException, SQLException {
        if (trolley.isEmpty()) {
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
            updateView();
            return;
        }
        try {
            valiadteTrolley(trolley);

            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedTrolley = groupProductsById(trolley);
            ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(groupedTrolley);

            if (insufficientProducts.isEmpty()) {
                handleSuccessfulCheckout();
            }
            else  {
                handleStockFailure(insufficientProducts);
            }
        }
        catch (UnderMinimumPaymentException | ExcessiveOrderQuantityException ex) { // If the trolley breaks either exception
            handleCheckoutException(ex);    // Notifier is made and told to the user
        }
        updateView();
    }

    /**
     * Groups products by productId to better display to the user.
     * Grouping products, we can check stock for a `productId` once, rather than multiple times
     *
     * @param proList The list of products.
     * @return A new productsArrayList, with unique product ids.
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                // Add the current products ordered quantity to the existing product
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Create a copy of the product to preserve orderedQuantity
                Product copy = new Product(
                        p.getProductId(),
                        p.getProductDescription(),
                        p.getProductImageName(),
                        p.getUnitPrice(),
                        p.getStockQuantity()
                );
                copy.setOrderedQuantity(p.getOrderedQuantity()); //PRESERVE QUANTITY
                grouped.put(id, copy);
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if(theProduct != null){
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
//            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.updateObservableProductList(searchList);
        cusView.update(imageName, displayLaSearchResult, trolley, displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }

    /**
     * Sorts products in trolley by product ID in ascending order.
     * <p>
     * This method uses a Comparator with a method reference,
     * which is functionally equivalent to a lambda expression such as:
     * <pre>{@code
     * prodList.sort((p1, p2) -> p1.getProductId().compareTo(p2.getProductId()));
     * }</pre>
     * Using the method references makes the intent clearer.
     *
     * @param prodList The list of products in the customer's trolley.
     * @ret
     **/
    public ArrayList<Product> OrganizeTrolley(ArrayList<Product> prodList) {
        if (prodList == null || prodList.isEmpty()) {  // validation check against empty lists
            return prodList;
        }

        prodList.sort(Comparator.comparing(Product::getProductId));
        // Alternate example: lambda
        // prodList.sort((p1, p2) -> p1.getProductId().compareTo(p2.getProductId()));
        return prodList;
    }

    /**
     * Sorts trolley with a custom comparator.
     * <p>
     * This allows sorting by multiple types, like id price or name.
     * <p>
     * Example usage:
     * <pre>{@code
     * sortTrolley(Comparator.comparing(Product::getUnitPrice)); // Sort by price
     * sortTrolley(Comparator.comparing(Product::getProductDescription)); // Sort by name
     * }</pre>
     *
     * @param comparator The comparator defining the sort order.
     */
    public ArrayList<Product> OrganizeTrolley(ArrayList<Product> prodList, Comparator<Product> comparator) {
        if (prodList == null || prodList.isEmpty()) {  // validation check against empty lists
            return prodList;
        }

        prodList.sort(comparator);
        return prodList;
    }

    /**
     * Handles scenario when user didn't enter any search keyword.
     * Clears state and prepares message.
     */
    private void handleEmptySearch() {
        searchList.clear();
        theProduct = null;
        displayLaSearchResult = "Please enter a Product ID or Name.";
    }

    /**
     * Updates quantity of a product in the trolley, or removes it if quantity <= 0.
     *
     * @param product The product to update or remove.
     * @param newQuantity The new desired quantity. If <= 0, the product is removed.
     */
    public void updateTrolleyProductQuantity(Product product, int newQuantity) {
        if (product == null) return;

        if (newQuantity <= 0) {
            trolley.remove(product);
        } else {
            product.setOrderedQuantity(newQuantity);
            if (!trolley.contains(product)) {
                trolley.add(product); // add back if it was removed
            }
        }

        updateView(); // refresh the view
    }

    /**
     * Validates trolley before checkout to ensure that:
     * <ul>
     *     <li> Product quantities do not exceed their maximum.</li>
     *     <li> Total payment meets the minimum threshold.</li>
     * </ul>
     *
     * @param tr the list of products representing the customer's trolley
     *
     * @throws ExcessiveOrderQuantityException if any product exceeds its maximum allowed quantity
     * @throws UnderMinimumPaymentException    if the trolley total is below the minimum payment threshold
     */
    private void valiadteTrolley(List<Product> tr) throws UnderMinimumPaymentException, ExcessiveOrderQuantityException{
        if (tr == null || tr.isEmpty()) {   // Validation check fro robustness
            throw new IllegalArgumentException("Trolley cant be null or empty");
        }

        validateQuantities(trolley);
        validatePayment(trolley);
    }

    /**
     * Validates no product exceeds trolley/product maximum quantity.
     *
     * <p>If a violation is detected, an {@link ExcessiveOrderQuantityException} is thrown
     * with details about the specified product </p>
     *
     * @param tr product list to validate
     * @throws ExcessiveOrderQuantityException Exception for when Products maxQuantity is exceeded
     */
    private void validateQuantities(List<Product> tr) throws ExcessiveOrderQuantityException{
        for (Product p : tr) {
            if (p.getOrderedQuantity() > p.getMaxQuantity()) {
                throw new ExcessiveOrderQuantityException(
                        p.getProductId(), p.getOrderedQuantity(), p.getMaxQuantity()
                );
            }
        }
    }

    /**
     * Validates trolley meets minimum value. Calculates total current trolley cost,
     * and compares it to OrderRules MINIMUM_PAYMENT
     *
     * <p>If a violation is detected, an {@link UnderMinimumPaymentException} is thrown
     * with details about the specified product </p>
     *
     * @param tr product list to validate
     * @throws UnderMinimumPaymentException Exception for when Trolley doesn't meet OrderRules MINIMUM_PAYMENT
     */
    private void validatePayment(List<Product> tr) throws UnderMinimumPaymentException{
        double total = 0;
        for (Product p : tr) {
            total += p.getOrderedQuantity() * p.getUnitPrice();
        }
        if (total < OrderRules.MINIMUM_PAYMENT) {
            throw new UnderMinimumPaymentException(total, OrderRules.MINIMUM_PAYMENT);
        }
    }

    /**
     * Handles exceptions during the checkout process, displays the appropriate
     * messages depending on the exception type and perform corrective actions.
     *
     * @param ex The exception called during the trolley checkout.
     */
    private void handleCheckoutException(Exception ex) {
        RemoveProductNotifier notifier = new RemoveProductNotifier();
        notifier.setCusView(cusView);
        notifier.showRemovalMsg(ex.getMessage());

        //Customize the customer action label in the notifier depending on exception
        if(ex instanceof ExcessiveOrderQuantityException){
            StringBuilder actions = new StringBuilder(" \u26A1 You can now: \n");
            actions.append("\u2022 Checkout your trolley as it is with the maximum available quantity\n");
            actions.append("\u2022 Or cancel your trolley if you no longer wish to proceed.\n");
            actions.append("Thank you for understanding! \n");
            notifier.setCustomerActionMessage(actions.toString());
            // Apply quantity correction AFTER notifying the user
            adjustQuantatiesToMax(trolley);
        }
        else if (ex instanceof UnderMinimumPaymentException){
            StringBuilder actions = new StringBuilder(" \u26A1 You can now: \n");
            actions.append("\u2022 Increase your trolley value to the value minimum\n");
            actions.append("\u2022 Or cancel your trolley if you no longer wish to proceed.\n");
            actions.append("Thank you for understanding! \n");
            notifier.setCustomerActionMessage(actions.toString());
        }

        notifier.closeNotifierWindowAfterDelay(30000);
        displayLaSearchResult = "Checkout failed:\n" + ex.getMessage();
    }

    /**
     * Handles and completes the checkout after trolley has been validated. Creates a new order from OrderHub.
     * Then clears the trolley and builds a receipt text for the user.
     *
     * @throws IOException
     * @throws SQLException
     */
    private void handleSuccessfulCheckout() throws IOException, SQLException{
        // Create new order
        OrderHub orderHub = OrderHub.getOrderHub();
        Order order = orderHub.newOrder(trolley);
        // Clear trolley after successfully order
        trolley.clear();
        displayTaTrolley = "";
        // Build text for receipt UI
        displayTaReceipt = String.format(
                "order_ID: %s\nOrdered_Date_Time: %s\n%s",
                order.getOrderId(),
                order.getOrderedDateTime(),
                ProductListFormatter.buildString(order.getProductList())
        );
        System.out.println(displayTaReceipt);
    }

    /**
     * Handles when products in the trolley don't have enough stock.
     *
     * <p> This method:
     * <ul>
     *     <li>Generates a message displaying the products with insufficient stock</li>
     *     <li>Adjusts quantities of products to max available amount</li>
     *     <li>Removes products with 0 stock from trolley</li>
     *     <li>Displays notification via {@link RemoveProductNotifier}/li>
     * </ul></p>
     *
     * @param insufficientProducts product list from trolley with insufficient stock
     */
    private void handleStockFailure(ArrayList<Product> insufficientProducts) {
        StringBuilder msg = new StringBuilder("The following products have insufficient stock:\n");

        Iterator<Product> iter = trolley.iterator();
        while (iter.hasNext()) {
            Product t = iter.next();
            for (Product insufficient : insufficientProducts) {
                if (t.getProductId().equals(insufficient.getProductId())) {
                    int available = insufficient.getStockQuantity();
                    int requested = t.getOrderedQuantity();
                    msg.append(String.format(
                            "• %s, %s (Only %d available, %d requested)\n",
                            t.getProductId(),
                            t.getProductDescription(),
                            available,
                            requested
                    ));
                    if (available > 0) {
                        t.setOrderedQuantity(available);
                    } else {
                        iter.remove();
                    }
                }
            }
        }
        RemoveProductNotifier notifier = new RemoveProductNotifier();
        notifier.setCusView(cusView);
        notifier.showRemovalMsg(msg.toString());
        notifier.closeNotifierWindowAfterDelay(30000);
        displayTaTrolley = ProductListFormatter.buildString(trolley);
        displayLaSearchResult = "Checkout failed due to insufficient stock:\n" + msg;
    }

    /**
     * Reduces the ordered quantities of products in the trolley to their maximum
     * if desired quantity is exceeded.
     *
     * @param tr List of products to validate max available isn't exceeded
     */
    private void adjustQuantatiesToMax(List<Product> tr) {
        for (Product p : tr) {
            if (p.getOrderedQuantity() > p.getMaxQuantity()) {
                p.setOrderedQuantity(p.getMaxQuantity());
            }
            else if (p.getOrderedQuantity() > p.getStockQuantity()) {
                p.setOrderedQuantity(p.getStockQuantity());
            }
        }
        updateView();
    }
}
