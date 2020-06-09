package us.ihmc.robotEnvironmentAwareness.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.javaFXToolkit.scenes.View3DFactory;
import us.ihmc.messager.Messager;
import us.ihmc.robotEnvironmentAwareness.communication.*;
import us.ihmc.robotEnvironmentAwareness.ui.controller.*;
import us.ihmc.robotEnvironmentAwareness.ui.io.PlanarRegionDataExporter;
import us.ihmc.robotEnvironmentAwareness.ui.io.PlanarRegionSegmentationDataExporter;
import us.ihmc.robotEnvironmentAwareness.ui.viewer.SegmentationMeshViewer;

import java.io.File;
import java.io.IOException;

public class PlanarSegmentationUI
{
   private static final String UI_CONFIGURATION_FILE_NAME = "./Configurations/defaultPlanarSegmentationUIConfiguration.txt";

   private final BorderPane mainPane;
   private final REAUIMessager uiMessager;
   private final SegmentationMeshViewer meshViewer;

   @FXML
   private OcTreeEssentialsAnchorPaneController ocTreeEssentialsAnchorPaneController;
   @FXML
   private RegionSegmentationAnchorPaneController regionSegmentationAnchorPaneController;
   @FXML
   private CustomRegionMergeAnchorPaneController customRegionMergeAnchorPaneController;
   @FXML
   private PolygonizerAnchorPaneController polygonizerAnchorPaneController;

   private final Stage primaryStage;

   private final UIConnectionHandler uiConnectionHandler;

   private PlanarSegmentationUI(REAUIMessager uiMessager, Stage primaryStage) throws Exception
   {
      this.primaryStage = primaryStage;
      FXMLLoader loader = new FXMLLoader();
      loader.setController(this);
      loader.setLocation(getClass().getResource(getClass().getSimpleName() + ".fxml"));
      mainPane = loader.load();

      // Client
      this.uiMessager = uiMessager;
      uiMessager.startMessager();

      meshViewer = new SegmentationMeshViewer(uiMessager);

      new PlanarRegionSegmentationDataExporter(uiMessager); // No need to anything with it beside instantiating it.
      new PlanarRegionDataExporter(uiMessager); // No need to anything with it beside instantiating it.

      initializeControllers(uiMessager);

      View3DFactory view3dFactory = View3DFactory.createSubscene();
      view3dFactory.addCameraController(true);
      view3dFactory.addWorldCoordinateSystem(0.3);
      mainPane.setCenter(view3dFactory.getSubSceneWrappedInsidePane());

      view3dFactory.addNodeToView(meshViewer.getRoot());

      uiConnectionHandler = new UIConnectionHandler(primaryStage, uiMessager, SegmentationModuleAPI.RequestEntireModuleState);
      uiConnectionHandler.start();

      uiMessager.notifyModuleMessagerStateListeners();

      primaryStage.setTitle(getClass().getSimpleName());
      primaryStage.setMaximized(true);
      Scene mainScene = new Scene(mainPane, 600, 400);

      mainScene.setOnKeyPressed(event ->
                                {
                                   if (event.getCode() == KeyCode.F5)
                                      refreshModuleState();
                                });

      primaryStage.setScene(mainScene);
      primaryStage.setOnCloseRequest(event -> stop());
   }

   private void refreshModuleState()
   {
      uiMessager.submitStateRequestToModule(SegmentationModuleAPI.RequestEntireModuleState);
   }

   private void initializeControllers(REAUIMessager uiMessager)
   {
      File configurationFile = new File(UI_CONFIGURATION_FILE_NAME);
      try
      {
         configurationFile.getParentFile().mkdirs();
         configurationFile.createNewFile();
      }
      catch (IOException e)
      {
         System.out.println(configurationFile.getAbsolutePath());
         e.printStackTrace();
      }

      ocTreeEssentialsAnchorPaneController.setConfigurationFile(configurationFile);
      ocTreeEssentialsAnchorPaneController.attachREAMessager(uiMessager);
      ocTreeEssentialsAnchorPaneController.bindControls();

      regionSegmentationAnchorPaneController.setPlanarRegionsSegmentationEnableTopic(SegmentationModuleAPI.PlanarRegionsSegmentationEnable);
      regionSegmentationAnchorPaneController.setPlanarRegionsSegmentationClearTopic(SegmentationModuleAPI.PlanarRegionsSegmentationClear);
      regionSegmentationAnchorPaneController.setSaveRegionUpdaterConfigurationTopic(SegmentationModuleAPI.SaveUpdaterConfiguration);
      regionSegmentationAnchorPaneController.setPlanarRegionsSegmentationParametersTopic(SegmentationModuleAPI.PlanarRegionsSegmentationParameters);
      regionSegmentationAnchorPaneController.setConfigurationFile(configurationFile);
      regionSegmentationAnchorPaneController.attachREAMessager(uiMessager);
      regionSegmentationAnchorPaneController.bindControls();

      customRegionMergeAnchorPaneController.setCustomRegionsMergingEnableTopic(SegmentationModuleAPI.CustomRegionsMergingEnable);
      customRegionMergeAnchorPaneController.setCustomRegionsClearTopic(SegmentationModuleAPI.CustomRegionsClear);
      customRegionMergeAnchorPaneController.setSaveRegionUpdaterConfigurationTopic(SegmentationModuleAPI.SaveUpdaterConfiguration);
      customRegionMergeAnchorPaneController.setCustomRegionsMergingParametersTopic(SegmentationModuleAPI.CustomRegionsMergingParameters);
      customRegionMergeAnchorPaneController.setConfigurationFile(configurationFile);
      customRegionMergeAnchorPaneController.attachREAMessager(uiMessager);
      customRegionMergeAnchorPaneController.bindControls();

      polygonizerAnchorPaneController.setPlanarRegionsPolygonizerEnableTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerEnable);
      polygonizerAnchorPaneController.setPlanarRegionsPolygonizerClearTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerClear);
      polygonizerAnchorPaneController.setPlanarRegionsIntersectionEnableTopic(SegmentationModuleAPI.PlanarRegionsIntersectionEnable);
      polygonizerAnchorPaneController.setUiPlanarRegionHideNodesTopic(SegmentationModuleAPI.UIPlanarRegionHideNodes);
      polygonizerAnchorPaneController.setSaveRegionUpdaterConfigurationTopic(SegmentationModuleAPI.SaveUpdaterConfiguration);
      polygonizerAnchorPaneController.setPlanarRegionsConcaveHullParametersTopic(SegmentationModuleAPI.PlanarRegionsConcaveHullParameters);
      polygonizerAnchorPaneController.setPlanarRegionsPolygonizerParametersTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerParameters);
      polygonizerAnchorPaneController.setPlanarRegionsIntersectionParametersTopic(SegmentationModuleAPI.PlanarRegionsIntersectionParameters);
      polygonizerAnchorPaneController.setConfigurationFile(configurationFile);
      polygonizerAnchorPaneController.attachREAMessager(uiMessager);
      polygonizerAnchorPaneController.bindControls();
   }

   public void show() throws IOException
   {
      refreshModuleState();
      primaryStage.show();
   }

   public void stop()
   {
      try
      {
         uiConnectionHandler.stop();
         uiMessager.closeMessager();

         meshViewer.stop();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public static PlanarSegmentationUI creatIntraprocessUI(Stage primaryStage) throws Exception
   {
      Messager moduleMessager = KryoMessager.createIntraprocess(SegmentationModuleAPI.API,
                                                                NetworkPorts.REA_MODULE_UI_PORT,
                                                                REACommunicationProperties.getPrivateNetClassList());
      REAUIMessager uiMessager = new REAUIMessager(moduleMessager);
      return new PlanarSegmentationUI(uiMessager, primaryStage);
   }

   public static PlanarSegmentationUI creatRemoteUI(Stage primaryStage, String host) throws Exception
   {
      Messager moduleMessager = KryoMessager.createTCPClient(SegmentationModuleAPI.API,
                                                             host,
                                                             NetworkPorts.REA_MODULE_UI_PORT,
                                                             REACommunicationProperties.getPrivateNetClassList());
      REAUIMessager uiMessager = new REAUIMessager(moduleMessager);
      return new PlanarSegmentationUI(uiMessager, primaryStage);
   }

}
