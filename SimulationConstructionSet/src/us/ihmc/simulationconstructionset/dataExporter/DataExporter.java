package us.ihmc.simulationconstructionset.dataExporter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import org.tmatesoft.svn.core.SVNException;

import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.utilities.DateTools;
import us.ihmc.utilities.ThreadTools;

public class DataExporter implements ActionListener
{
   private final SimulationConstructionSet scs;
   private final Robot robot;

   private DataExporterOptionsDialog optionsPanel;
   private final DataExporterReadmeWriter readmeWriter = new DataExporterReadmeWriter();
   private final DataExporterGraphCreator graphCreator;
   private final DataExporterExcelWorkbookCreator excelWorkbookCreator;

   public DataExporter(SimulationConstructionSet scs, Robot robot)
   {
      this.scs = scs;
      this.robot = robot;
      this.graphCreator = new DataExporterGraphCreator(robot, scs.getDataBuffer());
      this.excelWorkbookCreator = new DataExporterExcelWorkbookCreator(robot, scs.getDataBuffer());
   }

   public void actionPerformed(ActionEvent e)
   {
      // Stop the sim and disable the GUI:
      scs.stop();
      scs.disableGUIComponents();

      // Wait till done running:
      while (scs.isSimulating())
      {
         ThreadTools.sleep(1000);
      }

      // Crop the Buffer to In/Out. This is important because of how we use the DataBuffer later and we assume that in point is at index=0:
      scs.cropBuffer();
      scs.gotoInPointNow();

      // confirm directory structure is correct
      File simulationRootDirectory = DataExporterDirectoryFinder.findSimulationRootLocation(robot);
      if (simulationRootDirectory == null)
         return;
      File simulationDataAndVideoDirectory = DataExporterDirectoryFinder.findSimulationDataAndVideoRootLocation(simulationRootDirectory, robot);
      if (simulationDataAndVideoDirectory == null)
         return;

      // create label
      String timeStamp = DateTools.getDateString() + "_" + DateTools.getTimeString();
      String tagName = timeStamp + "_" + robot.getClass().getSimpleName();

      // figure out svn revsision number for project
      long revisionNumber = -1;
      try
      {
         revisionNumber = DataExporterSVNHandler.getRevisionNumber(simulationRootDirectory);
      }
      catch (SVNException e1)
      {
         e1.printStackTrace();
      }

      optionsPanel = new DataExporterOptionsDialog(tagName);

      if (!optionsPanel.isCancelled())
      {
         if (optionsPanel.saveData() || optionsPanel.createSpreadSheet() || optionsPanel.createGraphsJPG() || optionsPanel.createGraphsPDF()
                 || optionsPanel.createMovie() || optionsPanel.tagCode())
         {
            tagName = optionsPanel.tagName();
            System.out.println("Saving data using tag: " + tagName);

            // make destination directory
            File dataAndVideosTagDirectory = new File(simulationDataAndVideoDirectory, tagName);
            dataAndVideosTagDirectory.mkdir();

            // make graph directory inside destination directory
            File graphDirectory = new File(dataAndVideosTagDirectory, "graphs");
            graphDirectory.mkdir();

            if (optionsPanel.saveReadMe())
            {
               System.out.println("Saving ReadMe");
               readmeWriter.writeReadMe(dataAndVideosTagDirectory, tagName, revisionNumber);
               System.out.println("Done Saving ReadMe");
            }

            if (optionsPanel.saveData())
            {
               System.out.println("Saving data");
               saveDataFile(dataAndVideosTagDirectory, tagName);
               System.out.println("Done Saving Data");
            }

            if (optionsPanel.createSpreadSheet())
            {
               System.out.println("creating torque and speed spreadsheet");
               excelWorkbookCreator.createAndSaveTorqueAndSpeedSpreadSheet(dataAndVideosTagDirectory, tagName);
               System.out.println("done creating torque and speed spreadsheet");
            }

            if (optionsPanel.createGraphsJPG() || optionsPanel.createGraphsPDF())
            {
               System.out.println("creating torque and speed graphs");
               graphCreator.createGraphs(graphDirectory, tagName, optionsPanel.createGraphsJPG(), optionsPanel.createGraphsPDF());
               System.out.println("done creating torque and speed graphs");
            }

            if (optionsPanel.createMovie())
            {
               System.out.println("creating movie");
               createMovie(dataAndVideosTagDirectory, tagName);
               System.out.println("done creating movie");
            }

            if (optionsPanel.commitToSVN())
            {
               try
               {
                  System.out.println("committing to svn");
                  DataExporterSVNHandler.importIntoSVN(dataAndVideosTagDirectory, robot.getClass().getSimpleName(), tagName);
                  System.out.println("done committing to svn");
               }
               catch (SVNException svnException)
               {
                  svnException.printStackTrace();
               }
            }
         }
      }
      else
      {
         System.out.println("Data export cancelled.");
      }

      scs.enableGUIComponents();
   }

   private void saveDataFile(File directory, String fileHeader)
   {
      File file = new File(directory, fileHeader + ".data.gz");
      scs.writeData(file);
   }

   /**
    * Create movie from current viewport using the file path and file header
    * @param dataAndVideosTagDirectory
    * @param fileHeader
    */
   private void createMovie(File dataAndVideosTagDirectory, String fileHeader)
   {
      File movie = new File(dataAndVideosTagDirectory, fileHeader + "_Movie.mov");
      scs.getStandardSimulationGUI().getViewportPanel().getStandardGUIActions().createMovie(movie);
   }
}
