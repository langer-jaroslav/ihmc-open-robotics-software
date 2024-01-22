package us.ihmc.rdx.ui.behavior.sequence;

import com.fasterxml.jackson.databind.JsonNode;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrameLibrary;
import us.ihmc.tools.io.JSONFileTools;
import us.ihmc.tools.io.JSONTools;
import us.ihmc.tools.io.WorkspaceResourceFile;

import java.util.SortedSet;
import java.util.TreeSet;

public class RDXAvailableBehaviorTreeFile
{
   private final WorkspaceResourceFile treeFile;
   private final ReferenceFrameLibrary referenceFrameLibrary;
   private String name;
   private final SortedSet<String> referenceFrameNames = new TreeSet<>();
   private int numberOfFramesInWorld = 0;

   public RDXAvailableBehaviorTreeFile(WorkspaceResourceFile treeFile, ReferenceFrameLibrary referenceFrameLibrary)
   {
      this.treeFile = treeFile;
      this.referenceFrameLibrary = referenceFrameLibrary;
      JSONFileTools.load(treeFile.getFilesystemFile(), this::loadFromFile);
   }

   public void update()
   {
      numberOfFramesInWorld = 0;

      for (String referenceFrameName : referenceFrameNames)
      {
         ReferenceFrame frameByName = referenceFrameLibrary.findFrameByName(referenceFrameName);
         if (frameByName != null && frameByName.getRootFrame() == ReferenceFrame.getWorldFrame())
         {
            ++numberOfFramesInWorld;
         }
      }
   }

   private void loadFromFile(JsonNode jsonNode)
   {
      name = jsonNode.get("description").asText();

      loadChildrenData(jsonNode);
   }

   private void loadChildrenData(JsonNode childNode)
   {
      JsonNode parentFrameNode = childNode.get("parentFrame");

      if (parentFrameNode != null)
      {
         referenceFrameNames.add(parentFrameNode.textValue());
      }

      JSONTools.forEachArrayElement(childNode, "children", this::loadChildrenData);
   }

   public String getName()
   {
      return name;
   }

   public WorkspaceResourceFile getTreeFile()
   {
      return treeFile;
   }

   public SortedSet<String> getReferenceFrameNames()
   {
      return referenceFrameNames;
   }

   public int getNumberOfFramesInWorld()
   {
      return numberOfFramesInWorld;
   }
}
