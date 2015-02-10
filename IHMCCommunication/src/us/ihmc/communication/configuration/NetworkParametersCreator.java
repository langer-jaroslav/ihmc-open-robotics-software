package us.ihmc.communication.configuration;

import java.awt.Container;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

public class NetworkParametersCreator
{
   private final JFrame frame = new JFrame();

   private final EnumMap<NetworkParameterKeys, JTextField> entryBoxes = new EnumMap<>(NetworkParameterKeys.class);

   private final JTextField exportName;
   private final JComboBox<NetworkParameterKeys> destination;

   public NetworkParametersCreator()
   {
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      Container content = frame.getContentPane();
      content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

      for (NetworkParameterKeys key : NetworkParameterKeys.values())
      {
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
         panel.setBorder(BorderFactory.createTitledBorder(key.toString() + (key.isRequired() ? "*" : "")));
         JLabel description = new JLabel(key.getDescription());
         JTextField host = new JTextField(64);
         panel.add(description);
         panel.add(host);

         entryBoxes.put(key, host);

         content.add(panel);
      }

      JPanel requiredPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      JLabel required = new JLabel("* required");
      requiredPanel.add(required);
      content.add(requiredPanel);

      JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      savePanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

      JButton save = new JButton("Save");
      save.addActionListener(new SaveActionListener());
      savePanel.add(save);

      content.add(savePanel);

      JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      exportPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

      JButton export = new JButton("Export to: ");
      export.addActionListener(new ExportActionListener());
      destination = new JComboBox<>(NetworkParameterKeys.values());
      JLabel nameLabel = new JLabel(" Path:");
      exportName = new JTextField(NetworkParameters.defaultParameterFile, 32);

      exportPanel.add(export);
      exportPanel.add(destination);
      exportPanel.add(nameLabel);
      exportPanel.add(exportName);

      content.add(exportPanel);

      frame.pack();
      frame.setLocationByPlatform(true);
      frame.setVisible(true);
   }

   private boolean isValid()
   {
      for (NetworkParameterKeys key : NetworkParameterKeys.values())
      {
         if (key.isRequired() && entryBoxes.get(key).getText().length() == 0)
         {
            JOptionPane.showMessageDialog(frame, key.toString() + " is required.", "Missing required fields", JOptionPane.ERROR_MESSAGE);
            return false;
         }
      }
      return true;
   }

   private void save(File file)
   {
      try
      {
         FileOutputStream out = new FileOutputStream(file);
         Properties properties = new Properties();
         for (NetworkParameterKeys key : NetworkParameterKeys.values())
         {
            if (entryBoxes.get(key).getText().length() != 0)
            {
               properties.setProperty(key.toString(), entryBoxes.get(key).getText());
            }
         }
         properties.store(out, "Generated by " + getClass().getCanonicalName());
         out.close();
      }
      catch (IOException e)
      {
         JOptionPane.showMessageDialog(frame, "Cannot write to file " + file, "Write error", JOptionPane.ERROR_MESSAGE);
      }
   }

   private void export(String host, String path)
   {
      JOptionPane.showMessageDialog(frame, "TODO: Implement exporting to " + host + ":" + path, "Implement me", JOptionPane.ERROR_MESSAGE);

   }

   private class ExportActionListener implements ActionListener
   {

      @Override
      public void actionPerformed(ActionEvent e)
      {
         if (isValid())
         {
            NetworkParameterKeys key = (NetworkParameterKeys) destination.getSelectedItem();
            if (entryBoxes.get(key).getText().length() == 0)
            {
               JOptionPane.showMessageDialog(frame, key + " is not set.", "Invalid host", JOptionPane.ERROR_MESSAGE);
               return;
            }

            if (exportName.getText().length() == 0)
            {
               JOptionPane.showMessageDialog(frame, "No path given", "Invalid entry", JOptionPane.ERROR_MESSAGE);
               return;
            }

            export(entryBoxes.get(key).getText(), exportName.getText());
         }

      }

   }

   private class SaveActionListener implements ActionListener
   {

      @Override
      public void actionPerformed(ActionEvent e)
      {
         if (isValid())
         {
            FileDialog dialog = new FileDialog(frame, "Choose file", FileDialog.SAVE);
            dialog.setFilenameFilter(new FilenameFilter()
            {

               @Override
               public boolean accept(File dir, String name)
               {
                  if (name.endsWith(".ini"))
                  {
                     return true;
                  }
                  else
                  {
                     return false;
                  }
               }
            });
            dialog.setFile(NetworkParameters.defaultParameterFile);
            dialog.setVisible(true);

            String filename = dialog.getFile();
            if (filename == null)
            {
               return;
            }
            else
            {
               save(new File(dialog.getDirectory(), dialog.getFile()));
            }
         }
      }

   }

   public static void main(String[] args)
   {
      new NetworkParametersCreator();
   }

}
