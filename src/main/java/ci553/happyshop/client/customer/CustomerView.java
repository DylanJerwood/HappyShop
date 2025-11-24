package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The CustomerView is separated into two sections by a line :
 *
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 *    depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView  {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;
    private ListView<Product> trolleyList = new ListView<>();

    TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;

    private ObservableList<Product> obeProductList; //observable product list to display products from search
    ListView<Product> obrLvProducts; //A ListView observes the product list

    public void start(Stage window) {
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyle);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window,WIDTH,HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow=window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        /* ADD IN PLACE OF
        // data, an observable ArrayList, observed by obrLvProducts
        obeProductList = FXCollections.observableArrayList();
        obrLvProducts = new ListView<>(obeProductList);//ListView proListView observes proList
        obrLvProducts.setPrefHeight(HEIGHT - 100);
        obrLvProducts.setFixedCellSize(50);
        obrLvProducts.setStyle(UIStyle.listViewStyle);*/

        Label laPageTitle = new Label("Search by Product ID/Name");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        tfId = new TextField();
        tfId.setPromptText("Search products...");
        tfId.setStyle(UIStyle.textFiledStyle);
        tfId.setPrefWidth(200);

        Button btnSearch = new Button("🔍");
        btnSearch.setStyle(UIStyle.buttonStyle);
        btnSearch.setOnAction(this::buttonClicked);
        Button btnAddToTrolley = new Button("🛒");
        btnSearch.setMinWidth(35);
        btnSearch.setMinHeight(35);
        btnAddToTrolley.setMinWidth(35);
        btnAddToTrolley.setMinHeight(35);

        btnAddToTrolley.setStyle(UIStyle.buttonStyle);
        btnAddToTrolley.setOnAction(this::buttonClicked);

        HBox HbBtns = new HBox(10,btnSearch, btnAddToTrolley);
        HBox hbId = new HBox(10, tfId, HbBtns);
        hbId.setAlignment(Pos.CENTER);

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); // Image keeps its original shape and fits inside 60×60
        ivProduct.setSmooth(true); //make it smooth and nice-looking

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);
        lbProductInfo.setMinHeight(Region.USE_PREF_SIZE);  // maintain minimal height
        lbProductInfo.setMaxWidth(400); // set desired width before wrapping

        // Wrap label in a ScrollPane
        ScrollPane spProductInfo = new ScrollPane(lbProductInfo);
        spProductInfo.setPrefWidth(250);
        spProductInfo.setFitToWidth(true);
        spProductInfo.setPannable(false);
        spProductInfo.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spProductInfo.setPrefViewportHeight(100); // limits visible area height

        HBox hbSearchResult = new HBox(5, ivProduct, spProductInfo);
        hbSearchResult.setAlignment(Pos.CENTER_LEFT);

        // data, an observable ArrayList, observed by obrLvProducts
        obeProductList = FXCollections.observableArrayList();
        obrLvProducts = new ListView<>(obeProductList);//ListView proListView observes proList
        obrLvProducts.setPrefHeight(HEIGHT - 100);
        obrLvProducts.setFixedCellSize(50);
        obrLvProducts.setStyle(UIStyle.listViewStyle);
        HBox searchList = new HBox(5, obrLvProducts);

        /*
          When is setCellFactory() Needed?
          If you want to customize each row’s content (e.g.,images, buttons, labels, etc.).
          If you need special formatting (like colors or borders).
          <p>
          When is setCellFactory() NOT Needed?
          Each row is just plain text without images or formatting.
         */
        obrLvProducts.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                    //System.out.println("setCellFactory - empty item");
                } else {
                    String imageName = product.getProductImageName(); // Get image name (e.g. "0001.jpg")
                    String relativeImageUrl = StorageLocation.imageFolder + imageName;
                    // Get the full absolute path to the image
                    Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
                    String imageFullUri = imageFullPath.toUri().toString();// Build the full image Uri

                    ImageView ivPro;
                    try {
                        ivPro = new ImageView(new Image(imageFullUri, 50,45, true,true)); // Attempt to load the product image
                    } catch (Exception e) {
                        // If loading fails, use a default image directly from the resources folder
                        ivPro = new ImageView(new Image("imageHolder.jpg",50,45,true,true)); // Directly load from resources
                    }

                    Label laProToString = new Label(product.toString()); // Create a label for product details
                    HBox hbox = new HBox(10, ivPro, laProToString); // Put ImageView and label in a horizontal layout
                    setGraphic(hbox);  // Set the whole row content
                }
            }
        });

        VBox vbSearchPage = new VBox(15, laPageTitle, hbId, hbSearchResult,searchList);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }

    /**
     * Creates Trolley page UI.
     * Uses ListView to display products in trolley with compact layout:
     * - Product ID and description
     * - Unit price
     * - Quantity TextField with +/- buttons
     * - Remove button and total price
     *
     * @return VBox containing the trolley page
     */
    private VBox CreateTrolleyPage() {
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);
        // Configure ListView
        trolleyList.setPrefHeight(HEIGHT - 100);
        trolleyList.setStyle(UIStyle.listViewStyle);

        trolleyList.setCellFactory(listView -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setGraphic(null);
                    return;
                }

                // Product Info (ID + Description)
                Label lblID = new Label(product.getProductId());
                Label lblName = new Label(product.getProductDescription());
                lblName.setMaxWidth(150);
                lblName.setWrapText(true);
                VBox vbInfo = new VBox(2, lblID, lblName);

                // Price, Quantity, +/- buttons
                Label lblPrice = new Label(String.format("£%.2f", product.getUnitPrice()));
                TextField tfQuantity = new TextField(String.valueOf(product.getOrderedQuantity()));
                tfQuantity.setPrefWidth(40);

                // Handle input via processQuantityInput
                tfQuantity.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal) processQuantityInput(tfQuantity, product);
                });
                tfQuantity.setOnAction(e -> processQuantityInput(tfQuantity, product));

                // +/- buttons stacked vertically
                Button btnPlus = new Button("+");
                Button btnMinus = new Button("-");
                btnPlus.setPrefSize(20, 20);
                btnMinus.setPrefSize(20, 20);
                btnPlus.setOnAction(e -> cusController.changeOrderQuantity(product, +1));
                btnMinus.setOnAction(e -> cusController.changeOrderQuantity(product, -1));

                VBox vbBtns = new VBox(2, btnPlus, btnMinus);
                vbBtns.setPadding(Insets.EMPTY);
                vbBtns.setAlignment(Pos.CENTER);

                HBox hbQty = new HBox(5, lblPrice, tfQuantity, vbBtns);
                hbQty.setAlignment(Pos.CENTER);

                // Remove button + total price
                Button btnRemove = new Button("🗑");
                btnRemove.setOnAction(e -> cusController.removeFromTrolley(product));
                Label lblTotalPrice = new Label(String.format("£%.2f", product.getUnitPrice() * product.getOrderedQuantity()));
                VBox vbActions = new VBox(2, btnRemove, lblTotalPrice);
                vbActions.setAlignment(Pos.CENTER);

                // Combine all into row
                HBox row = new HBox(10, vbInfo, hbQty, vbActions);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(vbInfo, Priority.ALWAYS);

                setGraphic(row);
            }
        });
        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel,btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        // Assemble Trolley Page
        VBox vbTrolleyPage = new VBox(15, laPageTitle, trolleyList, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");

        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH/2, HEIGHT-50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);

        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }


    private void buttonClicked(ActionEvent event) {
        try{
            Button btn = (Button)event.getSource();
            String action = btn.getText();
            if(action.equals("Add to Trolley")){
                showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
            }
            if(action.equals("OK & Close")){
                showTrolleyOrReceiptPage(vbTrolleyPage);
            }
            cusController.doAction(action);
        }
        catch(SQLException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void update(String imageName, String searchResult, ArrayList<Product> trolley, String receipt) {
        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);
        updateTrolley(trolley);
        if (!receipt.isEmpty()) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                  viewWindow.getWidth(), viewWindow.getHeight());
    }

    //update the product list from search
    void updateObservableProductList( ArrayList<Product> productList ) {
        obeProductList.clear();
        obeProductList.addAll(productList);
    }

    /**
     * Updates trolley ListView with the current list of products.
     * Clears and resets the products.
     * @param trolley Current list of products in the trolley.
     */
    public void updateTrolley(ArrayList<Product> trolley) {
        if (trolley == null) {
            trolleyList.getItems().clear();
        } else {
            trolleyList.getItems().setAll(trolley);
        }
    }

    /**
     * Processes the quantity user entered and updates the model.
     * Handles invalid input and removes products if quantity < 1.
     *
     * @param tfQuantity TextField where the user enters the desired quantity.
     * @param product The product associated with this TextField.
     */
    private void processQuantityInput(TextField tfQuantity, Product product) {
        try {
            int newQty = Integer.parseInt(tfQuantity.getText());
            if (newQty < 1) {
                cusController.removeFromTrolley(product);
            } else {
                cusController.changeOrderQuantity(product, newQty - product.getOrderedQuantity());
            }
        } catch (NumberFormatException ex) {
            tfQuantity.setText(String.valueOf(product.getOrderedQuantity())); // revert invalid input
        }
    }
}
