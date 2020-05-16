package gameSceneManager;

import gameLogicManager.gameControllerManager.GameEngine;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static gameSceneManager.App.loadFXML;
import static gameSceneManager.BoardGenerator.terrainColorMap;


/**
 * Controls the local game play UI of the application
 * @author Zeynep Cankara
 * @version 12.05.2020
 */

public class LocalGameController extends SceneController {
    // Properties: UI Related
    ImageView goBackImg;
    Button transformTerrainBtn;

    // Contains Buttons of the Game UI
    Polygon[] terrainMapHexagons;
    // Holds the information about game state
    HashMap<String, Integer> gameStateLocal;



    static HashMap<Integer, Polygon> dwellingMap = new HashMap<Integer, Polygon>();
    // Define the dwelling polygons
    //Polygon polygon = new Polygon();



    // Constructor
    public LocalGameController(Stage stage) throws IOException {
        // initialize the map buttons
        terrainMapHexagons = new Polygon[113];

        // Hold the game state in a HashMap
        gameStateLocal = new HashMap<String, Integer>();
        //TODO: Initialize to the current action round
        gameStateLocal.put("action",  -1);
        //TODO: Initialize to the user's home terrain
        gameStateLocal.put("terrainId",  -1);
        // The terrain tile selected by mouse press
        gameStateLocal.put("terrainSelected",  -1);
        // transformAndBuild --option buildDwelling: 1 (yes), 0 (no), -1  (not init)
        gameStateLocal.put("isBuildDwelling",  -1);


        // Load the FXML file
        super.root = loadFXML("localGame");
        // scene = stage.getScene(); // NOTE: This causes error in exec
        super.scene = new Scene(super.root);

        // initialize the controller
        initController(stage);
        //scene = BoardGenerator.generateDefaultTerrainMap(scene);
        HashMap<String, Boolean> gameState = GameSetupController.getInitParameters();
        if( gameState.get("isDefaultMap")){
           super.scene = BoardGenerator.generateDefaultTerrainMap(super.scene);
        } else {
            super.scene = BoardGenerator.generateRandomTerrainMap(super.scene, (int) (Math.random() * 6 + 1));
        }

        //popup for action round
        //displayActionRoundPopup();
        // buttonmain.setOnAction(e -> displayActionPopup());

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
        super.scene.getStylesheets().clear();
        super.scene.getStylesheets().add(getClass().getResource("localGame.css").toExternalForm());
        stage.setScene(super.scene);
        stage.show();

        // fetch UI elements from the FXML file
        goBackImg = (ImageView) super.scene.lookup("#goBackImg");
        transformTerrainBtn = (Button) super.scene.lookup("#transformTerrainBtn");

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
            changeTerrainSelected(gameStateLocal.get("terrainSelected"), gameStateLocal.get("terrainId"));
            buildDwellingOnSelected(gameStateLocal.get("terrainSelected"));
        });

        // Action round popup initialization
        displayActionRoundPopup();
    }

    // methods for Terrain Manipulation
    /**
     * Transforms the selected terrain space with specified terrain type.
     * @param polygonId reference of the UI hexagon
     * @param terrainId, terrain type
     */
    public void changeTerrainSelected(int polygonId, int terrainId) {
        Polygon hexagon = (Polygon) super.scene.lookup("#" + polygonId);
        hexagon.setFill(terrainColorMap.get(terrainId));
    }

    /**
     * Place dwelling on board
     * @param polygonId reference to the UI hexagon
     */
    public void buildDwellingOnSelected(int polygonId) {
        // TODO: add a rectangle to the scene selected terrain tile
        // TODO: Bug fix required
        System.out.println("Dwelling built: " + gameStateLocal.get("isBuildDwelling"));
        Rectangle rectangle = new Rectangle(200, 200);
        rectangle.setFill(Color.AZURE);
        rectangle.setLayoutX(terrainMapHexagons[polygonId].getLayoutX());
        rectangle.setLayoutY(terrainMapHexagons[polygonId].getLayoutY());
        rectangle.setVisible(true);
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
            actionRoundStage.close();
        });
        lowerExchangeRateSpadesActionBtn.setOnMouseClicked(event -> {
            System.out.println("lowerExchangeRateSpadesAction...");
            gameStateLocal.put("action", 3);
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
            actionRoundStage.close();
        });
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
