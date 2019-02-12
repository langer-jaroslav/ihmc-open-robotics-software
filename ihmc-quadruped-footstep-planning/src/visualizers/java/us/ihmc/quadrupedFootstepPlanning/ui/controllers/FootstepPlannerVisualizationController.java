package us.ihmc.quadrupedFootstepPlanning.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.QuadrupedFootstepPlannerNodeRejectionReason;

public class FootstepPlannerVisualizationController
{
   @FXML
   private CheckBox showAllValidNodes;
   @FXML
   private CheckBox showAllInvalidNodes;
   @FXML
   private CheckBox showValidNodesThisTick;
   @FXML
   private CheckBox showInvalidNodesThisTick;
   @FXML
   private CheckBox showNodesRejectedByReason;
   @FXML
   private ComboBox<QuadrupedFootstepPlannerNodeRejectionReason> rejectionReasonToShow;
   @FXML
   private Slider plannerPlaybackSlider;
//   @FXML
//   public void requestStatistics()
//   {
//      throw new RuntimeException("This feature is currently not implemented.");
//      if (verbose)
//         PrintTools.info(this, "Clicked request statistics...");
//
//      messager.submitMessage(FootstepPlannerMessagerAPI.RequestPlannerStatistics, true);
//   }

   private JavaFXMessager messager;

   public void attachMessager(JavaFXMessager messager)
   {
      this.messager = messager;
   }

   public void setupControls()
   {
      ObservableList<QuadrupedFootstepPlannerNodeRejectionReason> plannerTypeOptions = FXCollections.observableArrayList(QuadrupedFootstepPlannerNodeRejectionReason.values);
      rejectionReasonToShow.setItems(plannerTypeOptions);
      rejectionReasonToShow.setValue(QuadrupedFootstepPlannerNodeRejectionReason.OBSTACLE_BLOCKING_STEP);
   }
   public void bindControls()
   {
      setupControls();

      messager.bindBidirectional(FootstepPlannerMessagerAPI.ShowAllValidNodesTopic, showAllValidNodes.selectedProperty(), true);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.ShowAllInvalidNodesTopic, showAllInvalidNodes.selectedProperty(), true);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.ShowValidNodesThisTickTopic, showValidNodesThisTick.selectedProperty(), true);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.ShowInvalidNodesThisTickTopic, showInvalidNodesThisTick.selectedProperty(), true);
      messager.bindBidirectional(FootstepPlannerMessagerAPI.ShowNodesRejectedByReasonTopic, showNodesRejectedByReason.selectedProperty(), true);

      messager.bindBidirectional(FootstepPlannerMessagerAPI.RejectionReasonToShowTopic, rejectionReasonToShow.valueProperty(), true);

      messager.bindBidirectional(FootstepPlannerMessagerAPI.PlannerPlaybackFractionTopic, plannerPlaybackSlider.valueProperty(), true);
   }
}
