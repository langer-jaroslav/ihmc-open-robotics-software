package us.ihmc.gdx.ui;

import imgui.ImGui;
import imgui.type.ImBoolean;
import us.ihmc.gdx.imgui.ImBooleanWrapper;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.tools.property.BooleanStoredPropertyKey;
import us.ihmc.tools.property.StoredPropertySetBasics;

import java.util.function.Consumer;

public class ImGuiStoredPropertySetBooleanWidget
{
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final String label;
   private final Runnable onParametersUpdatedCallback;
   private final ImBooleanWrapper imBooleanWrapper;
   private final Consumer<ImBoolean> accessImBoolean;

   public ImGuiStoredPropertySetBooleanWidget(StoredPropertySetBasics storedPropertySet,
                                              BooleanStoredPropertyKey key,
                                              Runnable onParametersUpdatedCallback)
   {
      this.onParametersUpdatedCallback = onParametersUpdatedCallback;
      imBooleanWrapper = new ImBooleanWrapper(storedPropertySet, key);
      label = labels.get(key.getTitleCasedName());
      accessImBoolean = this::renderCheckbox;
   }


   public void render()
   {
      imBooleanWrapper.accessImBoolean(accessImBoolean);
   }

   private void renderCheckbox(ImBoolean imBoolean)
   {
      if (ImGui.checkbox(label, imBoolean))
      {
         onParametersUpdatedCallback.run();
      }
   }
}
