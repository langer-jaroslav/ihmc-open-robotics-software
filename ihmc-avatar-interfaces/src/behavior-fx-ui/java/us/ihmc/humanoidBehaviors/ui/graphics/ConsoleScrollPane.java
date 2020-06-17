package us.ihmc.humanoidBehaviors.ui.graphics;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.apache.commons.lang3.tuple.Pair;
import us.ihmc.humanoidBehaviors.BehaviorModule;
import us.ihmc.javafx.JavaFXMissingTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;

import java.util.List;

public class ConsoleScrollPane extends ScrollPane
{
   /** Feel free to add font names here. */
   private static final String[] FONT_PREFERENCE = {"Courier New", "Liberation Mono"};

   private final Messager behaviorMessager;
   private final TextFlow textFlow;
   private final String fontName;

   private boolean customScrollSeen = false;

   public ConsoleScrollPane(Messager behaviorMessager)
   {
      this.behaviorMessager = behaviorMessager;
      this.fontName = selectFontName();

      setVmin(0.0);
      setVmax(1.0);

      textFlow = new TextFlow();
      textFlow.setTextAlignment(TextAlignment.LEFT);

      setContent(textFlow);
      setVvalue(1.0);

      setOnScroll(event -> {
         customScrollSeen = true;
      });
   }

   private String selectFontName()
   {
      List<String> fontNames = Font.getFontNames();

      for (String fontNameCandidate : FONT_PREFERENCE)
      {
         if (fontNames.contains(fontNameCandidate))
         {
            return fontNameCandidate;
         }
      }

      return Font.getDefault().getName();
   }

   public void setupAtEnd()
   {
      behaviorMessager.registerTopicListener(BehaviorModule.API.StatusLog, logEntry -> Platform.runLater(() -> receivedMessageForTopic(logEntry)));
   }

   private void receivedMessageForTopic(Pair<Integer, String> logEntry)
   {
      Text text = new Text(logEntry.getRight() + "\n");
      text.setFont(new Font(fontName, -1));
      text.setFontSmoothingType(FontSmoothingType.LCD);
      switch (logEntry.getLeft())
      {
         case 100:
         case 200:
            text.setFill(Color.RED.brighter());
            break;
         case 300:
            text.setFill(Color.YELLOW.darker());
            break;
         case 400:
            text.setFill(Color.BLACK);
            break;
         case 500:
            text.setFill(Color.CYAN);
            break;
         case 600:
            text.setFill(Color.GREEN);
            break;
      }

      boolean autoScroll = getVvalue() >= 0.97;

      textFlow.getChildren().add(text);

      if (autoScroll || !customScrollSeen)
      {
         JavaFXMissingTools.runNFramesLater(1,() -> setVvalue(1.0));
      }
   }
}
