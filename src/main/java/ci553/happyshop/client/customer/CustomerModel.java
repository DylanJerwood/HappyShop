package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
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
            trolley.add(selectedProduct);
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
        if(!trolley.isEmpty()){
            // Group the products in the trolley by productId to optimize stock checking
            // Check the database for sufficient stock for all products in the trolley.
            // If any products are insufficient, the update will be rolled back.
            // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
            // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
            ArrayList<Product> groupedTrolley = groupProductsById(trolley);
            ArrayList<Product> insufficientProducts = databaseRW.purchaseStocks(groupedTrolley);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                displayTaTrolley ="";
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
            }
            else {  // Some products have insufficient stock— build an error message to inform the customer
                // --- Step 1: Prepare notification message and update trolley accordingly ---
                StringBuilder errorMsg = new StringBuilder("The following products have insufficient stock:\n");

                // Use an iterator to safely modify the trolley
                Iterator<Product> trolleyIterator = trolley.iterator();
                while (trolleyIterator.hasNext()) {
                    Product trolleyProduct = trolleyIterator.next();

                    // Check if this product has insufficient stock
                    for (Product p : insufficientProducts) {
                        if (trolleyProduct.getProductId().equals(p.getProductId())) {
                            int available = p.getStockQuantity();
                            int requested = trolleyProduct.getOrderedQuantity();
                            // Append info to the customer message
                            errorMsg.append(String.format("• %s, %s (Only %d available, %d requested)\n",
                                    trolleyProduct.getProductId(),
                                    trolleyProduct.getProductDescription(),
                                    available,
                                    requested
                            ));
                            // Adjust trolley quantities based on stock
                            if (available > 0) {
                                trolleyProduct.setOrderedQuantity(available);
                            } else {
                                // Remove product if none left
                                trolleyIterator.remove();
                            }
                            break; // Stop searching insufficientProducts once matched
                        }
                    }
                }
                theProduct = null;
                // --- Step 2: Notify customer using a popup window ---
                RemoveProductNotifier removeProductNotifier = new RemoveProductNotifier();
                removeProductNotifier.setCusView(cusView);
                removeProductNotifier.showRemovalMsg(errorMsg.toString());

                // schedule notifier to close automatically after a delay
                removeProductNotifier.closeNotifierWindowAfterDelay(30000);

                // --- Step 3: Refresh UI display ---
                displayTaTrolley = ProductListFormatter.buildString(trolley);
                displayLaSearchResult = "Checkout failed due to insufficient stock:\n" + errorMsg;
                System.out.println("Stock insufficient — customer notified.");
            }
        }
        else{

            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
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
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
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
     * */
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
}
