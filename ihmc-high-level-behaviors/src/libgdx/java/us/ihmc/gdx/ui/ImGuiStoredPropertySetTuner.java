package us.ihmc.gdx.ui;

import imgui.ImGui;
import imgui.flag.ImGuiDataType;
import imgui.type.ImBoolean;
import imgui.type.ImDouble;
import imgui.type.ImInt;
import us.ihmc.commons.MathTools;
import us.ihmc.commons.nio.BasicPathVisitor;
import us.ihmc.commons.nio.PathTools;
import us.ihmc.gdx.imgui.ImGuiPanel;
import us.ihmc.gdx.imgui.ImGuiTools;
import us.ihmc.gdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.tools.property.*;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class ImGuiStoredPropertySetTuner extends ImGuiPanel
{
   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private StoredPropertySetBasics storedPropertySet;
   private StoredPropertyKeyList keys;
   private Runnable onParametersUpdatedCallback;
   private final TreeSet<String> versions = new TreeSet<>();

   private record ValueRange(float min, float max) { }

   private final HashMap<DoubleStoredPropertyKey, ImDouble> doubleValues = new HashMap<>();
   private final HashMap<IntegerStoredPropertyKey, ImInt> integerValues = new HashMap<>();
   private final HashMap<BooleanStoredPropertyKey, ImBoolean> booleanValues = new HashMap<>();

   public ImGuiStoredPropertySetTuner(String name)
   {
      super(name);
      setRenderMethod(this::renderImGuiWidgets);
   }

   public void create(StoredPropertySetBasics storedPropertySet, StoredPropertyKeyList keys)
   {
      create(storedPropertySet, keys, () -> { });
   }

   public void create(StoredPropertySetBasics storedPropertySet, StoredPropertyKeyList keys, Runnable onParametersUpdatedCallback)
   {
      this.storedPropertySet = storedPropertySet;
      this.keys = keys;
      this.onParametersUpdatedCallback = onParametersUpdatedCallback;

      Path saveFileDirectory = storedPropertySet.findSaveFileDirectory();
      PathTools.walkFlat(saveFileDirectory, (path, pathType) ->
      {
         String fileName = path.getFileName().toString();
         String prefix = storedPropertySet.getUncapitalizedClassName();
         String extension = ".ini";
         if (pathType == BasicPathVisitor.PathType.FILE && fileName.startsWith(prefix) && fileName.endsWith(extension))
         {
            versions.add(fileName.replaceAll(extension, "").substring(prefix.length()));
         }
         return FileVisitResult.CONTINUE;
      });
      String currentWorkingVersion = versions.first();
      if (!storedPropertySet.getCurrentVersionSuffix().equals(currentWorkingVersion))
      {
         storedPropertySet.updateBackingSaveFile(currentWorkingVersion);
         storedPropertySet.load();
      }

      for (StoredPropertyKey<?> propertyKey : keys.keys())
      {
         // Add supported types here
         if (propertyKey.getType().equals(Double.class))
         {
            DoubleStoredPropertyKey key = (DoubleStoredPropertyKey) propertyKey;
            doubleValues.put(key, new ImDouble(storedPropertySet.get(key)));
         }
         else if (propertyKey.getType().equals(Integer.class))
         {
            IntegerStoredPropertyKey key = (IntegerStoredPropertyKey) propertyKey;
            integerValues.put(key, new ImInt(storedPropertySet.get(key)));
         }
         else if (propertyKey.getType().equals(Boolean.class))
         {
            BooleanStoredPropertyKey key = (BooleanStoredPropertyKey) propertyKey;
            booleanValues.put(key, new ImBoolean(storedPropertySet.get(key)));
         }
         else
         {
            throw new RuntimeException("Please implement spinner for type: " + propertyKey.getType());
         }
      }
   }

   public void renderImGuiWidgets()
   {
      ImGui.text("Version:");
      if (versions.size() > 1)
      {
         for (String version : versions)
         {
            if (ImGui.radioButton(version.isEmpty() ? "Primary" : version, storedPropertySet.getCurrentVersionSuffix().equals(version)))
            {
               storedPropertySet.updateBackingSaveFile(version);
               load();
            }
         }
      }
      else
      {
         ImGui.sameLine();
         ImGui.text(storedPropertySet.getCurrentVersionSuffix());
      }

      //      ImGuiInputTextFlags. // TODO: Mess with various flags
      ImGui.pushItemWidth(150.0f);
      for (StoredPropertyKey<?> propertyKey : keys.keys())
      {
         renderAPropertyTuner(propertyKey);
      }
      if (ImGui.button("Load"))
      {
         load();
      }
      ImGui.sameLine();
      if (ImGui.button("Save"))
      {
         storedPropertySet.save();
      }
   }

   public void renderAPropertyTuner(StoredPropertyKey<?> propertyKey)
   {
      if (propertyKey instanceof DoubleStoredPropertyKey doubleKey)
      {
         renderADoublePropertyTuner(doubleKey, 0.01, 0.5);
      }
      else if (propertyKey instanceof IntegerStoredPropertyKey integerKey)
      {
         renderAnIntegerPropertyTuner(integerKey, 1);
      }
      else if (propertyKey instanceof BooleanStoredPropertyKey)
      {
         if (ImGui.checkbox(propertyKey.getTitleCasedName(), booleanValues.get(propertyKey)))
         {
            BooleanStoredPropertyKey key = (BooleanStoredPropertyKey) propertyKey;
            storedPropertySet.set(key, booleanValues.get(key).get());
            onParametersUpdatedCallback.run();
         }
      }
   }

   public void renderADoublePropertyTuner(DoubleStoredPropertyKey doubleKey, double step, double stepFast)
   {
      renderADoublePropertyTuner(doubleKey, step, stepFast, Double.NaN, Double.NaN, false, null, "%.6f");
   }

   public void renderADoublePropertyTuner(DoubleStoredPropertyKey doubleKey,
                                          double step,
                                          double stepFast,
                                          double min,
                                          double max,
                                          boolean fancyLabel,
                                          String unitString,
                                          String format)
   {
      String label = fancyLabel ? labels.get(unitString, doubleKey.getTitleCasedName()) : doubleKey.getTitleCasedName();
      if (fancyLabel)
      {
         ImGui.text(doubleKey.getTitleCasedName() + ":");
         ImGui.sameLine();
         ImGui.pushItemWidth(100.0f);
      }

      boolean changed;
      if (doubleKey.hasLowerBound() && doubleKey.hasUpperBound())
      {
         changed = ImGui.sliderScalar(label,
                                      ImGuiDataType.Double,
                                      doubleValues.get(doubleKey),
                                      doubleKey.getLowerBound(),
                                      doubleKey.getUpperBound(),
                                      format);
      }
      else
      {
         changed = ImGuiTools.volatileInputDouble(label, doubleValues.get(doubleKey), step, stepFast, format);
      }

      if (changed)
      {
         if (!Double.isNaN(min))
            doubleValues.get(doubleKey).set(MathTools.clamp(doubleValues.get(doubleKey).get(), min, max));
         storedPropertySet.set(doubleKey, doubleValues.get(doubleKey).get());
         onParametersUpdatedCallback.run();
      }
      if (fancyLabel)
      {
         ImGui.popItemWidth();
      }
   }

   private void renderAnIntegerPropertyTuner(IntegerStoredPropertyKey integerKey, int step)
   {
      if (ImGuiTools.volatileInputInt(integerKey.getTitleCasedName(), integerValues.get(integerKey), step))
      {
         storedPropertySet.set(integerKey, integerValues.get(integerKey).get());
         onParametersUpdatedCallback.run();
      }
   }

   public ImGuiStoredPropertySetDoubleSlider createDoubleSlider(DoubleStoredPropertyKey key, double min, double max)
   {
      return new ImGuiStoredPropertySetDoubleSlider(key, storedPropertySet, min, max, onParametersUpdatedCallback);
   }

   private void load()
   {
      storedPropertySet.load();
      for (Map.Entry<DoubleStoredPropertyKey, ImDouble> entry : doubleValues.entrySet())
      {
         entry.getValue().set(storedPropertySet.get(entry.getKey()));
      }
      for (Map.Entry<IntegerStoredPropertyKey, ImInt> entry : integerValues.entrySet())
      {
         entry.getValue().set(storedPropertySet.get(entry.getKey()));
      }
      for (Map.Entry<BooleanStoredPropertyKey, ImBoolean> entry : booleanValues.entrySet())
      {
         entry.getValue().set(storedPropertySet.get(entry.getKey()));
      }

      onParametersUpdatedCallback.run();
   }

   public <T> void changeParameter(StoredPropertyKey<T> key, T value)
   {
      storedPropertySet.set(key, value);
      // TODO: experimental . . .
      doubleValues.get((DoubleStoredPropertyKey) key).set((double)value);
      onParametersUpdatedCallback.run();
   }
}
