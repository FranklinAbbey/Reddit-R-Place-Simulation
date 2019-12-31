package place.client.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;
import place.network.NetworkClient;
import place.network.PlaceRequest;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static java.lang.Thread.sleep;

/**
 * PlaceGUI uses JavaFX graphics to visualize Place
 *
 * @author Frank Abbey @fra1489
 * @author Will Andrews @wta9748
 */
public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {

    // Place data members
    private ClientModel clientModel;
    private NetworkClient networkClient;
    private String userName;
    private int boardDim;
    private String currentOwner;
    private PlaceColor fillColor;

    // JavaFX data members
    private BorderPane borderPane;
    private GridPane gridPane;
    private FlowPane flowPane;
    private HBox hBox;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private Tooltip toolTip;
    private double mouseEventX;
    private double mouseEventY;

    // offset used for time and date calculation
    private final int offset = 18000000;
    // scaling used for Rectangle size
    private final int scaling = 550;

    /**
     * Called after init() to establish the core structure of
     * the interface. Creates a GridPane of Rectangle objects to
     * represent pixels, and a ToggleGroup of Toggle buttons along
     * the bottom of the window to allow choices for the user.
     *
     * @param primaryStage - a JavaFX Stage object to be used as the
     *                       main stage
     */
    @Override
    public void start(Stage primaryStage) {

        boardDim = clientModel.getBoard().DIM;

        borderPane = new BorderPane();
        gridPane = new GridPane();
        hBox = new HBox();
        flowPane = new FlowPane();

        Rectangle box;

        gridPane.setHgap(1);
        gridPane.setVgap(1);

        // creating Rectangle objects used to display "pixels"
        for(int i = 0; i < boardDim; i++) {
            for(int k = 0; k < boardDim; k++) {
                //get Color of Tile at this position from Model
                fillColor = clientModel.getBoard().getTile(i, k).getColor();

                box = new Rectangle(scaling / (boardDim * 1.1),scaling / (boardDim *1.1),
                        Paint.valueOf(fillColor.getName()));
                box.prefHeight(primaryStage.getWidth());
                box.prefWidth(primaryStage.getWidth());

                // temporary values used as a work around for lambda expression
                int tempK = k;
                int tempI = i;
                // once a 'box' is clicked, send out the information to the Network Client
                box.setOnMouseClicked(e -> {
                    networkClient.changeTile(
                            new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE,
                                    new PlaceTile(tempI, tempK, userName, fillColor, new Date().getTime())));
                    try {
                        sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                });

                gridPane.add(box, k, i);

                PlaceTile currentTile = clientModel.getBoard().getTile(k, i);
                currentOwner = currentTile.getOwner();

            }
        }

        // create ToggleButtons to indicate which color the user will place
        for(int i = 0; i < PlaceColor.TOTAL_COLORS; i++) {
            PlaceColor currentColor = PlaceColor.values()[i];
            ToggleButton tButton = new ToggleButton(Integer.toString(currentColor.getNumber()));
            tButton.setToggleGroup(toggleGroup);
            tButton.setStyle("-fx-base: " + currentColor.getName());

            tButton.setOnAction(e -> {
                // change fill color, Rectangle will then send this color if clicked
                fillColor = currentColor;
            });

            hBox.getChildren().add(tButton);
        }

        // centering everything within Stage
        hBox.setAlignment(Pos.CENTER);
        gridPane.setAlignment(Pos.CENTER);

        borderPane.setBottom(hBox);
        borderPane.setCenter(gridPane);
        Label infoText = new Label("(scroll to zoom/drag to navigate/'ESC' or 'H' to exit zoom)");
        borderPane.setTop(infoText);

        Scene scene = new Scene(borderPane);

        // zooming event feature
        primaryStage.addEventHandler(ScrollEvent.SCROLL, event -> {
            // 'scroll' will be positive or negative depending on
            // scrolling forward or backward
            double scroll = event.getDeltaY();
            // increase the size of all rectangles
            for(Node currentNode : gridPane.getChildren()) {
                if (currentNode instanceof Rectangle) {
                    Rectangle currentBox = (Rectangle) currentNode;
                    currentBox.setHeight(currentBox.getHeight() + scroll);
                    currentBox.setWidth(currentBox.getWidth() + scroll);
                }
            }

            borderPane.setLayoutX(-(event.getSceneX()));
            borderPane.setLayoutY(-(event.getSceneY()));

        });

        // retrieve initial mouse position to properly drag pane
        borderPane.setOnMousePressed(mouseEvent -> {
            mouseEventX = mouseEvent.getX();
            mouseEventY = mouseEvent.getY();
        });

        // ability to move pane around
        borderPane.setOnMouseDragged(mouseEvent -> {
            borderPane.setLayoutX(mouseEvent.getSceneX() - mouseEventX);
            borderPane.setLayoutY(mouseEvent.getSceneY() - mouseEventY);
        });

        // quickly escape zoom with 'ESC' or 'H'
        borderPane.setOnKeyPressed(keyEvent -> {
            if(keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.H) {
                // loop through all Rectangles and enlarge
                for(Node currentNode : gridPane.getChildren()) {
                    if (currentNode instanceof Rectangle) {
                        Rectangle currentBox = (Rectangle) currentNode;
                        currentBox.setHeight(scaling / (boardDim * 1.1));
                        currentBox.setWidth(scaling / (boardDim * 1.1));
                    }
                }
                // re-align Border Pane
                borderPane.setLayoutX(0);
                borderPane.setLayoutY(0);
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Place : " + userName);
        primaryStage.show();

        this.clientModel.addObserver(this);
        this.networkClient.startListener();

        this.refresh(clientModel, null);

    }

    /**
     * Update method called by ClientModel, which is observed by
     * the PLaceGUI class
     *
     * @param model - the current model
     * @param tile - the PlaceTile that has changed
     */
    @Override
    public void update(ClientModel model, PlaceTile tile) {
        javafx.application.Platform.runLater( () -> {
            this.refresh(model, tile);
        });
    }

    /**
     * refresh is used to update the graphics interface in real time, updating the tile
     * sent in
     */
    public void refresh(ClientModel model, PlaceTile tile) {
        // loop through all Nodes on GridPane (only known way to get a Node by coordinates
        for(Node currentNode : gridPane.getChildren()) {
            if(currentNode instanceof Rectangle) {

                Rectangle currentBox = (Rectangle) currentNode;
                int row = GridPane.getRowIndex(currentBox);
                int col = GridPane.getColumnIndex(currentBox);
                PlaceTile currentTile = model.getBoard().getTile(row, col);

                currentOwner = currentTile.getOwner();

                // gathering Date and Time information
                Date date = new Date(currentTile.getTime() - offset);
                DateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");
                DateFormat dateFormatter = new SimpleDateFormat("MM/dd/yy");
                timeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                String timeFormatted = timeFormatter.format(date);
                String dateFormatted = dateFormatter.format(date);

                // if this is the PlaceTile to be updated (initially the tile sent is null)
                if ((tile != null) && (row == tile.getRow() && col == tile.getCol())) {
                    currentBox.setFill(Paint.valueOf(currentTile.getColor().getName()));
                    // output Tile Changes to terminal
                    System.out.println("Tile Change: (" + col + "," + row + ") "
                            + "\n user : " + currentOwner
                            + "\n color: " + currentTile.getColor().getName()
                            + "\n time : " + dateFormatted + " - " + timeFormatted + "\n");
                }

                // toolTip is updated every refresh to keep stats up to date
                toolTip = new Tooltip("(" + col + "," + row
                        + ")\nuser : " + currentOwner + "\ncolor: " + currentTile.getColor().getName()
                        + "\nplaced: " + dateFormatted
                        + " - " + timeFormatted);

                toolTip.setGraphic(new Rectangle( 30, 30,
                        Paint.valueOf(currentTile.getColor().getName())));

                Tooltip.install(currentBox, toolTip);

            }
        }


    }

    /**
     * called first by JavaFX convention, it creates the connection
     * with the Network Client and handles the command line arguments
     */
    public void init() {
        try{
            // get host info from command line
            List<String>args =getParameters().getRaw();
            // get host info from command line
            String hostName = args.get(0);
            int port = Integer.parseInt(args.get(1));
            userName = args.get(2);

            clientModel = new ClientModel();
            networkClient = new NetworkClient(hostName, port, clientModel, userName);
            networkClient.connect();

        }
        catch(PlaceException e) {
            System.out.println(e.getMessage());
        }

    }

    /**
     * retrieve command line arguments, launch JavaFX Application
     *
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
