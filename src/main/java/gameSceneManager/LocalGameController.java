package gameSceneManager;

import gameLogicManager.gameControllerManager.FlowManager;
import gameLogicManager.gameControllerManager.GameEngine;
import gameLogicManager.gameModel.gameBoard.Structure;
import gameLogicManager.gameModel.gameBoard.StructureType;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static gameSceneManager.App.loadFXML;
import static gameSceneManager.BoardGenerator.terrainColorMap;
import static gameSceneManager.GameSetupController.*;


/**
 * Controls the local game play UI of the application
 * @author Zeynep Cankara
 * @version 17.05.2020
 */

public class LocalGameController extends SceneController {
    // Properties: UI Related
    ImageView goBackImg;
    AnchorPane anchorPane;
    Button transformTerrainBtn;
    Button fireCultBtn;
    Button earthCultBtn;
    Button waterCultBtn;
    Button airCultBtn;
    Button endTurnBtn;
    Button actionPopupBtn;
    // Contains Buttons of the Game UI
    Polygon[] terrainMapHexagons;
    // Holds the information about game state
    static HashMap<String, Integer> gameStateLocal;
    GameEngine gameEngine;
    Label workerLabel;
    Label coinLabel;
    Label priestLabel;
    Label victoryPointLabel;
    Label currentPlayerBar;
    Label statusBar;

    // image reading
    String currentFactionName;
    FileInputStream inputStream;
    Image currentFactionImg;
    ImageView currentFactionImgView;



    // Constructor
    public LocalGameController(Stage stage) throws IOException {

        int randomness = GameSetupController.gameState.get("isDefaultMap");
        gameEngine = randomness == 0 ? GameEngine.getInstance(true) : GameEngine.getInstance(false);

        for(int i = 0; i < 4; i++){
            gameEngine.getGame().getPlayers()[i].setFaction(factions.get(i));
        }
        System.out.println(factions.toString());

        // initialize the map buttons
        terrainMapHexagons = new Polygon[113];
        // Holds the game state in a HashMap
        gameStateLocal = new HashMap<String, Integer>();
        //TODO: Initialize to the current action round
        gameStateLocal.put("action",  -1);
        // Color id associated with faction
        gameStateLocal.put("factionColorId",  GameSetupController.gameState.get("factionColorId"));
        // Id associated with faction
        gameStateLocal.put("factionId",  GameSetupController.gameState.get("factionId"));
        // Action (1) terrain selection when no change keeps home terrain or previously selected
        gameStateLocal.put("terrainId",  GameSetupController.gameState.get("factionColorId"));
        // The terrain tile selected by mouse press (131 possible Hexagon Terrain Tile)
        gameStateLocal.put("terrainSelected",  -1);
        // transformAndBuild --option buildDwelling: 1 (yes), 0 (no), -1  (not init)
        gameStateLocal.put("isBuildDwelling",  0);
        // sendPriestToCult --option sendCult: 0 (fire), 1 (water),  2 (earth), 3 (air), -1 (not init)
        gameStateLocal.put("cultId",  -1);
        // sendPriestToCult --option priestInitPos: from GameEngine Init pries location to one of 4 corner
        /*
         0: Not enough priest.
         1: You need key to proceed
         2: Move 3 steps, occupy top left slot 
         3: Move 2 steps, occupy top right slot 
         4: Move 2 steps, occupy bottom left slot 
         5: Move 2 steps, occupy bottom right slot 
         6: All slots are occupied 
         */
        gameStateLocal.put("priestInitPos",  2);


        // Load the FXML file
        super.root = loadFXML("localGame");

        // scene = stage.getScene(); // NOTE: This causes error in exec
        super.scene = new Scene(super.root);

        // initialize the controller
        initController(stage);



        //scene = BoardGenerator.generateDefaultTerrainMap(scene);
        HashMap<String, Integer> gameState = GameSetupController.getInitParameters();
        if( gameState.get("isDefaultMap") == 1){
            super.scene = BoardGenerator.generateDefaultTerrainMap(super.scene);
        } else {
            super.scene = BoardGenerator.generateRandomTerrainMap(super.scene, (int) (Math.random() * 6 + 1));
        }

        // init Hexagons on Terrain Map
        for (int i = 0; i  < 113; i++){
            terrainMapHexagons[i] = (Polygon) scene.lookup("#" + i);
            Integer terrainTileId = i;
            // Use opacity to show selection
            terrainMapHexagons[i].setOnMouseEntered(event -> {
                terrainMapHexagons[terrainTileId].setStyle("-fx-opacity: 0.5;");
            });
            terrainMapHexagons[i].setOnMouseExited(event -> {
                terrainMapHexagons[terrainTileId].setStyle("-fx-opacity: 1");
            });
            // save the selected hexagon in game state
            terrainMapHexagons[i].setOnMouseClicked(event -> {
                System.out.println("select: " + terrainTileId);
                gameStateLocal.put("terrainSelected", terrainTileId);
            });
        }
    }


    @Override
    public void initController(Stage stage) throws IOException {
        // Setup scene style from CSS
        super.scene.getStylesheets().clear();
        super.scene.getStylesheets().add(getClass().getResource("localGame.css").toExternalForm());
        stage.setScene(super.scene);
        stage.show();

        // fetch UI elements from the FXML file
        anchorPane = (AnchorPane) super.scene.lookup("#AnchorPaneSinglePlayer");
        goBackImg = (ImageView) super.scene.lookup("#goBackImg");
        transformTerrainBtn = (Button) super.scene.lookup("#transformTerrainBtn");
        fireCultBtn = (Button) super.scene.lookup("#fireCultBtn");
        waterCultBtn = (Button) super.scene.lookup("#waterCultBtn");
        earthCultBtn = (Button) super.scene.lookup("#earthCultBtn");
        airCultBtn = (Button) super.scene.lookup("#airCultBtn");
        endTurnBtn = (Button) super.scene.lookup("#endTurnBtn");
        actionPopupBtn = (Button) super.scene.lookup("#actionPopupBtn");
        workerLabel = (Label) super.scene.lookup("#worker");
        coinLabel = (Label) super.scene.lookup("#coin");
        priestLabel = (Label) super.scene.lookup("#priest");
        victoryPointLabel = (Label) super.scene.lookup("#victoryPoint");
        currentPlayerBar = (Label) super.scene.lookup("#playerName");


        //TODO: Delete initialization of Image
        /*
        FlowManager flowManager = FlowManager.getInstance();
        currentFactionName = String.valueOf(flowManager.getCurrentPlayer().getFaction().getType());
        inputStream = null;
        try {
            inputStream  = new FileInputStream("src/main/resources/gameSceneManager/images/" + currentFactionName + ".png");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        currentFactionImg = new Image(inputStream);
        // setImage
        currentFactionImgView = new ImageView(currentFactionImg);
        // TODO: Logo init position change
        currentFactionImgView.setLayoutX(1020);
        currentFactionImgView.setLayoutY(100);
        currentFactionImgView.setVisible(true);
        anchorPane.getChildren().addAll(currentFactionImgView);

         */
        statusBar = (Label) super.scene.lookup("#statusLabel");


        // Add listeners to buttons
        Parent finalRoot = super.root;
        goBackImg.setOnMouseClicked(event -> {
            FadeTransition fadeAnimation = new FadeTransition(Duration.seconds(1), finalRoot);
            fadeAnimation.setOnFinished(event1 ->
            {
                try {
                    finalRoot.setVisible(false);
                    App.setController(0, stage);
                } catch (IOException e) {
                    System.out.println(e);
                }
            });
            fadeAnimation.play();
        });

        transformTerrainBtn.setOnMouseClicked(event -> {
            if(gameStateLocal.get("action")  == 1){
                changeTerrainSelected(gameStateLocal.get("terrainSelected"), gameStateLocal.get("terrainId"));
                try {
                    buildDwellingOnSelected(gameStateLocal.get("terrainSelected"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if(gameStateLocal.get("action") == 4){
                //TODO: Take upgradge structure
                Integer polygonId = gameStateLocal.get("terrainSelected");
                if(GameEngine.upgradeStructure(polygonId)){
                    try {
                        changeStructureSelected(polygonId);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    statusBar.setText("Can't upgradge structure");
                }
            }
        });
        fireCultBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("cultId", 0);
            try {
                sendPriestToCult(0, gameStateLocal.get("priestInitPos"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        waterCultBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("cultId", 1);
            try {
                sendPriestToCult(1, gameStateLocal.get("priestInitPos"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        earthCultBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("cultId", 2);
            try {
                sendPriestToCult(2, gameStateLocal.get("priestInitPos"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        airCultBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("cultId", 3);
            try {
                sendPriestToCult(3, gameStateLocal.get("priestInitPos"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
        endTurnBtn.setOnMouseClicked(event -> {
            //TODO: signal end of the turn
            FlowManager flowManager = FlowManager.getInstance();
            flowManager.getNextPlayer();
            workerLabel.setText(Integer.toString(flowManager.getCurrentPlayer().getNumOfWorkers()));
            coinLabel.setText(Integer.toString(flowManager.getCurrentPlayer().getCoins()));
            priestLabel.setText(Integer.toString(flowManager.getCurrentPlayer().getNumOfPriests()));
            victoryPointLabel.setText(Integer.toString(flowManager.getCurrentPlayer().getScore()));
            String playerName = flowManager.getCurrentPlayer().getFaction().getType().toString();
            currentPlayerBar.setText(flowManager.getCurrentPlayer().getFaction().getType().toString());
            
            gameStateLocal.put("terrainId", factionToTerrain.get(playerName));

            //  Set the image of  current faction...
            /*
            currentFactionName = String.valueOf(flowManager.getCurrentPlayer().getFaction().getType());
            inputStream =  null;
            try {
                inputStream =  new FileInputStream("src/main/resources/gameSceneManager/images/" + currentFactionName + ".png");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            currentFactionImg = new Image(String.valueOf(inputStream));
            // setImage
            currentFactionImgView.setImage(null);
            anchorPane.getChildren().removeAll(currentFactionImgView);
            currentFactionImgView = new ImageView(currentFactionImg);
            // TODO: Logo init position change
            currentFactionImgView.setLayoutX(1020);
            currentFactionImgView.setLayoutY(100);
            currentFactionImgView.setVisible(true);
            anchorPane.getChildren().addAll(currentFactionImgView);

             */
        });
        actionPopupBtn.setOnMouseClicked(event -> {
            // Action round popup initialization
            try {
                displayActionRoundPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // methods for Terrain Manipulation
    /**
     * Transforms the selected terrain space with specified terrain type.
     * @param polygonId reference of the UI hexagon
     * @param terrainId, terrain type
     */
    public void changeTerrainSelected(int polygonId, int terrainId) {
        if( GameEngine.transformTerrain(polygonId, terrainId) ) {
            Polygon hexagon = (Polygon) super.scene.lookup("#" + polygonId);
            hexagon.setFill(terrainColorMap.get(terrainId));
        }
        if(statusBar != null )
            statusBar.setText(GameEngine.getGameStatus());
    }

    public void changeStructureSelected(int polygonId) throws FileNotFoundException {
        FileInputStream inputStream;
        StructureType selectStructure = gameEngine.getStructureType(polygonId);
        inputStream = new FileInputStream("src/main/resources/gameSceneManager/images/" + selectStructure + ".png");
        Image img = new Image(inputStream);
        ImageView imgView = new ImageView(img);
        imgView.setLayoutX(terrainMapHexagons[polygonId].getLayoutX()-17);
        imgView.setLayoutY(terrainMapHexagons[polygonId].getLayoutY()-20);
        imgView.setVisible(true);
        anchorPane.getChildren().addAll(imgView);
    }


    /**
     * Place dwelling on board (dwelling symbol FactionColor rectangle)
     * @param polygonId reference to the UI hexagon
     */
    public void buildDwellingOnSelected(int polygonId) throws FileNotFoundException {
        if(gameStateLocal.get("isBuildDwelling") == 1){
            System.out.println("o");
            if( gameEngine.buildDwelling(polygonId)) {
                System.out.println("x");
                FileInputStream inputStream = new FileInputStream("src/main/resources/gameSceneManager/images/dwelling.png");
                Image imageDwelling = new Image(inputStream);
                ImageView imgView = new ImageView(imageDwelling);
                imgView.setLayoutX(terrainMapHexagons[polygonId].getLayoutX()-17);
                imgView.setLayoutY(terrainMapHexagons[polygonId].getLayoutY()-20);
                imgView.setVisible(true);
                anchorPane.getChildren().addAll(imgView);
                //Loading image from URL
                FlowManager flowManager  = FlowManager.getInstance();
                victoryPointLabel.setText(Integer.toString(flowManager.getCurrentPlayer().getScore()));
            }
            if(statusBar != null )
                statusBar.setText(GameEngine.getGameStatus());
        }

    }

    // methods for Cult Track
    /**
     * Send a priest to the cult board (priest symbol FactionColor circle)
     * @param cultId reference to the Cult track
     * @param priestInitPos priest initialization location
     */
    public void sendPriestToCult(int cultId, int priestInitPos) throws FileNotFoundException {
        //TODO: GameFlow logic to initialize priest position

        // cult board location
        double posX;
        double posY;
        double offsetX;
        double offsetY;
        switch(cultId) {
            case 0:
                posX = fireCultBtn.getLayoutX();
                posY = fireCultBtn.getLayoutY();
                break;
            case 1:
                posX = waterCultBtn.getLayoutX();
                posY = waterCultBtn.getLayoutY();
                break;
            case 2:
                posX = earthCultBtn.getLayoutX();
                posY = earthCultBtn.getLayoutY();
                break;
            case 3:
                posX = airCultBtn.getLayoutX();
                posY = airCultBtn.getLayoutY();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + cultId);
        }
        switch(priestInitPos) {
            case 0:
                // 0: Not enough priest
                offsetX = -1.0;
                offsetY = -1.0;
                break;
            case 1:
                // 1: You need key to proceed
                offsetX = -1.0;
                offsetY = -1.0;
                break;
            case 2:
                // 2: Move 3 steps, occupy top left slot
                offsetX = -18.0;
                offsetY = -18.0;
                break;
            case 3:
                // 3: Move 2 steps, occupy top right slot
                offsetX = +18.0;
                offsetY = -18.0;
                break;
            case 4:
                // 4: Move 2 steps, occupy bottom left slot
                offsetX = -18.0;
                offsetY = +18.0;
                break;
            case 5:
                // 5: Move 2 steps, occupy bottom right slot
                offsetX = +18.0;
                offsetY = +18.0;
                break;
            case 6:
                // 6: All slots are occupied
                offsetX = -1.0;
                offsetY = -1.0;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + cultId);
        }
        if(offsetX != -1 && offsetY != -1){
            Circle circle = new Circle(10);
            circle.setFill(terrainColorMap.get(gameStateLocal.get("factionColorId")));
            circle.setLayoutX(posX + 20 + offsetX);
            circle.setLayoutY(posY - 25 + offsetY);
            circle.setVisible(true);
            anchorPane.getChildren().addAll(circle);
        }
    }

    // methods for Cult Track manipulation
    /**
     * Retrieve current priest marker and advance by the number of steps
     * @param cultId reference to the Cult Track
     * @param cultPos position of the current priest marker
     * @param stepSize advancement of the current priest marker
     */
    public void updateCultBoard(int cultId, int cultPos, int stepSize) throws FileNotFoundException {
        /*
        Rectangle rectangle = new Rectangle(25, 35);
        rectangle.setFill(terrainColorMap.get(gameStateLocal.get("factionColorId")));
        // get the previou
        //rectangle.setLayoutX(terrainMapHexagons[polygonId].getLayoutX() - 20);
        //rectangle.setLayoutY(terrainMapHexagons[polygonId].getLayoutY() - 15);
        rectangle.setVisible(true);
        anchorPane.getChildren().addAll(rectangle);
         */
    }

    // Popups for game flow
    /**
     * Action Round Popup
     */
    public void  displayActionRoundPopup() throws IOException {
        // Properties
        Button transformAndBuildActionBtn;
        Button advanceShippingActionBtn;
        Button lowerExchangeRateSpadesActionBtn;
        Button upgradeStructureActionBtn;
        Button sendPriestToCultActionBtn;
        Button takePowerActionBtn;
        Button takeSpecialActionBtn;
        Button passActionBtn;


        Stage actionRoundStage = new Stage();
        actionRoundStage.initModality(Modality.APPLICATION_MODAL);
        actionRoundStage.setTitle("Action Round Popup");
        actionRoundStage.setHeight(500);
        actionRoundStage.setWidth(400);

        //load the css file
        Parent actionRoundPopupFXML  = loadFXML("actionRoundPopup");
        Scene actionRoundScene = new Scene(actionRoundPopupFXML);

        actionRoundScene.getStylesheets().clear();
        actionRoundScene.getStylesheets().add(getClass().getResource("actionRoundPopup.css").toExternalForm());
        actionRoundStage.setScene(actionRoundScene);
        actionRoundStage.show();

        // Fetch button from the FXML file
        transformAndBuildActionBtn = (Button) actionRoundScene.lookup("#action1Btn");
        advanceShippingActionBtn = (Button) actionRoundScene.lookup("#action2Btn");
        lowerExchangeRateSpadesActionBtn = (Button) actionRoundScene.lookup("#action3Btn");
        upgradeStructureActionBtn = (Button) actionRoundScene.lookup("#action4Btn");
        sendPriestToCultActionBtn = (Button) actionRoundScene.lookup("#action5Btn");
        takePowerActionBtn = (Button) actionRoundScene.lookup("#action6Btn");
        takeSpecialActionBtn = (Button) actionRoundScene.lookup("#action7Btn");
        passActionBtn = (Button) actionRoundScene.lookup("#action8Btn");

        // TODO: Connect buttons to the action logic
        transformAndBuildActionBtn.setOnMouseClicked(event -> {
            System.out.println("transformAndBuildAction...");
            gameStateLocal.put("action", 1);
            try {
                displayTransformAndBuildPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            actionRoundStage.close();
        });
        advanceShippingActionBtn.setOnMouseClicked(event -> {
            System.out.println("advanceShippingAction...");
            gameStateLocal.put("action", 2);
            GameEngine.improveShipping();
            actionRoundStage.close();
        });
        lowerExchangeRateSpadesActionBtn.setOnMouseClicked(event -> {
            System.out.println("lowerExchangeRateSpadesAction...");
            gameStateLocal.put("action", 3);
            GameEngine.improveTerraforming();
            actionRoundStage.close();
        });
        upgradeStructureActionBtn.setOnMouseClicked(event -> {
            System.out.println("upgradeStructureAction...");
            gameStateLocal.put("action", 4);
            actionRoundStage.close();
        });
        sendPriestToCultActionBtn.setOnMouseClicked(event -> {
            System.out.println("sendPriestToCultActionAction...");
            gameStateLocal.put("action", 5);
            actionRoundStage.close();
        });
        takePowerActionBtn.setOnMouseClicked(event -> {
            System.out.println("takePowerAction...");
            gameStateLocal.put("action", 6);
            actionRoundStage.close();
        });
        takeSpecialActionBtn.setOnMouseClicked(event -> {
            System.out.println("takeSpecialAction...");
            gameStateLocal.put("action", 7);
            actionRoundStage.close();
        });
        passActionBtn.setOnMouseClicked(event -> {
            System.out.println("passAction...");
            gameStateLocal.put("action", 8);
            GameEngine.pass();
            actionRoundStage.close();
        });
    }


    /**
     * Popup for Upgradge Structure
     */
    public void  displayUpgradgeStructurePopup() throws IOException {
        // Stage setup
        Stage upgradgeStructureStage = new Stage();
        upgradgeStructureStage.initModality(Modality.APPLICATION_MODAL);
        upgradgeStructureStage.setTitle("Transform and Build Action");
        upgradgeStructureStage.setHeight(250);
        upgradgeStructureStage.setWidth(850);

        //load the css file
        Parent upgradgeStructureFXML  = loadFXML("upgradgeStructurePopup");
        Scene upgradgeStructureScene = new Scene(upgradgeStructureFXML);
        upgradgeStructureScene.getStylesheets().clear();
        upgradgeStructureScene.getStylesheets().add(getClass().getResource("upgradgeStructurePopup.css").toExternalForm());
        upgradgeStructureStage.setScene(upgradgeStructureScene);
        upgradgeStructureStage.show();


    }

    /**
     * Popup for Transform and build action
     */
    public void  displayTransformAndBuildPopup() throws IOException {
        // Properties
        Button plainsBtn;
        Button swampBtn;
        Button lakesBtn;
        Button forestBtn;
        Button mountainsBtn;
        Button wastelandBtn;
        Button desertBtn;
        Button noChangeBtn;

        // Stage setup
        Stage terraformingStage = new Stage();
        terraformingStage.initModality(Modality.APPLICATION_MODAL);
        terraformingStage.setTitle("Transform and Build Action");
        terraformingStage.setHeight(250);
        terraformingStage.setWidth(850);

        //load the css file
        Parent transformAndBuildPopupFXML  = loadFXML("transformAndBuildPopup");
        Scene transformAndBuildScene = new Scene(transformAndBuildPopupFXML);
        transformAndBuildScene.getStylesheets().clear();
        transformAndBuildScene.getStylesheets().add(getClass().getResource("transformAndBuildPopup.css").toExternalForm());
        terraformingStage.setScene(transformAndBuildScene);
        terraformingStage.show();

        // Fetch button from the FXML file
        plainsBtn = (Button) transformAndBuildScene.lookup("#plains");
        swampBtn = (Button) transformAndBuildScene.lookup("#swamp");
        lakesBtn = (Button) transformAndBuildScene.lookup("#lakes");
        forestBtn = (Button) transformAndBuildScene.lookup("#forest");
        mountainsBtn = (Button) transformAndBuildScene.lookup("#mountains");
        wastelandBtn = (Button) transformAndBuildScene.lookup("#wasteland");
        desertBtn = (Button) transformAndBuildScene.lookup("#desert");
        noChangeBtn = (Button) transformAndBuildScene.lookup("#noChangeBtn");

        plainsBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 0);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        swampBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 1);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        lakesBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 2);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        forestBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 3);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        mountainsBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 4);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        wastelandBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 5);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        desertBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("terrainId", 6);
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
        noChangeBtn.setOnMouseClicked(event -> {
            System.out.println(gameStateLocal.get("terrainId"));
            if(gameStateLocal.get("terrainId") == -1){
                gameStateLocal.put("terrainId", GameSetupController.gameState.get("factionColorId"));
            }
            try {
                displayBuildDwellingPopup();
            } catch (IOException e) {
                e.printStackTrace();
            }
            terraformingStage.close();
        });
    }

    /**
     * Popup for Build Dwelling
     */
    public void  displayBuildDwellingPopup() throws IOException {
        // Properties
        Button yesBuildDwellingBtn;
        Button noBuildDwellingBtn;

        // Stage setup
        Stage buildDwellingStage = new Stage();
        buildDwellingStage.initModality(Modality.APPLICATION_MODAL);
        buildDwellingStage.setTitle("Build Dwelling");
        buildDwellingStage.setHeight(250);
        buildDwellingStage.setWidth(850);

        //load the css file
        Parent buildDwellingPopupFXML  = loadFXML("buildDwellingPopup");
        Scene buildDwellingScene = new Scene(buildDwellingPopupFXML);
        buildDwellingScene.getStylesheets().clear();
        buildDwellingScene.getStylesheets().add(getClass().getResource("buildDwellingPopup.css").toExternalForm());
        buildDwellingStage.setScene(buildDwellingScene);
        buildDwellingStage.show();

        // Fetch button from the FXML file
        yesBuildDwellingBtn = (Button) buildDwellingScene.lookup("#yesBtn");
        noBuildDwellingBtn = (Button) buildDwellingScene.lookup("#noBtn");

        // TODO: Use buttons to fetch information user want to build dwelling or not
        yesBuildDwellingBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("isBuildDwelling", 1);
            buildDwellingStage.close();
        });
        noBuildDwellingBtn.setOnMouseClicked(event -> {
            gameStateLocal.put("isBuildDwelling",0);
            buildDwellingStage.close();
        });
    }



    /**
     * Random popup example
     */
    public void displayActionRoundPopup_demo() {
        //popup for action round
        //displayActionRoundPopup();
        // buttonmain.setOnAction(e -> displayActionPopup());
        Stage actionRoundStage = new Stage();
        actionRoundStage.initModality(Modality.APPLICATION_MODAL);
        actionRoundStage.setTitle("Action Round Popup");
        actionRoundStage.setHeight(500);
        actionRoundStage.setWidth(400);

        //load the css file
        String actionRoundPopupCss = getClass().getResource("actionRoundPopup.css").toExternalForm();


        final Popup popup = new Popup();
        popup.setX(300);
        popup.setY(200);

        // this add a circle on the screen
        popup.getContent().addAll(new Circle(25, 25, 50, Color.AQUAMARINE));

        Button show = new Button("Show");
        show.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                popup.show(actionRoundStage);
            }
        });

        Button hide = new Button("Hide");
        hide.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                popup.hide();
            }
        });
        HBox layout = new HBox(10);
        layout.getChildren().addAll(show, hide);
        actionRoundStage.setScene(new Scene(layout));
        actionRoundStage.show();
    }


}
