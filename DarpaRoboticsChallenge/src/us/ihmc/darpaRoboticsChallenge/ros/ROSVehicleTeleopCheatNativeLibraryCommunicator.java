package us.ihmc.darpaRoboticsChallenge.ros;

import us.ihmc.darpaRoboticsChallenge.networkProcessor.ros.RosTools;
import us.ihmc.darpaRoboticsChallenge.ros.messages.*;

import java.net.InetAddress;
import java.nio.*;
import java.util.ArrayList;

public class ROSVehicleTeleopCheatNativeLibraryCommunicator
{
   private static ROSVehicleTeleopCheatNativeLibraryCommunicator instance = null;
   private static String rosMasterURI;

   /*
    * Subscriber Buffers
    */
   private final DoubleBuffer vehiclePoseBuffer;
   private final ByteBuffer leftEyeImageBuffer, rightEyeImageBuffer;
   private final LongBuffer clockBuffer;

   /*
    * Subscriber Messages
    */
   private final PoseMessage vehiclePose = new PoseMessage();
   private final ClockMessage clock = new ClockMessage();
   private final Float64Message
      steeringWheelState = new Float64Message(), handBrakeState = new Float64Message(), gasPedalState = new Float64Message(),
      brakePedalState = new Float64Message();
   private final CompressedImageMessage
      leftEyeImageMessage = new CompressedImageMessage(), rightEyeImageMessage = new CompressedImageMessage();

   /*
    * Publisher Buffers
    */
   private final DoubleBuffer atlasTeleportPoseCommandBuffer;

   /*
    * Publisher Messages
    */
   private final PoseMessage atlasTeleportPoseCommand = new PoseMessage();
   private final Int8Message directionCommand = new Int8Message();
   private final Float64Message
      steeringWheelCommand = new Float64Message(), handBrakeCommand = new Float64Message(), gasPedalCommand = new Float64Message(),
      brakePedalCommand = new Float64Message();

   /*
    * Listeners
    */

   private final ArrayList<ClockListener> clockListeners = new ArrayList<ClockListener>();
   private final ArrayList<CompressedImageListener> leftEyeImageListeners = new ArrayList<CompressedImageListener>();
   private final ArrayList<CompressedImageListener> rightEyeImageListeners = new ArrayList<CompressedImageListener>();
   private final ArrayList<VehiclePoseListener> vehiclePoseListeners = new ArrayList<VehiclePoseListener>();
   private final ArrayList<HandBrakeStateListener> handBrakeStateListeners = new ArrayList<HandBrakeStateListener>();
   private final ArrayList<SteeringWheelStateListener> steeringWheelStateListeners = new ArrayList<SteeringWheelStateListener>();
   private final ArrayList<GasPedalStateListener> gasPedalStateListeners = new ArrayList<GasPedalStateListener>();
   private final ArrayList<BrakePedalStateListener> brakePedalStateListeners = new ArrayList<BrakePedalStateListener>();

   /*
    * TODO: Make classes for messages
    *
    * ImageMessage
    *
    */

   private ROSVehicleTeleopCheatNativeLibraryCommunicator(String rosMasterURI)
   {
      InetAddress myIP = RosTools.getMyIP(rosMasterURI);

      System.loadLibrary("ROSVehicleTeleopCheatNativeLibraryCommunicator");

      if (!register(rosMasterURI, myIP.getHostAddress()))
      {
         throw new RuntimeException("Cannot load native library");
      }

      vehiclePoseBuffer = setupDoubleBuffer(getVehiclePoseBuffer());
      clockBuffer = setupLongBuffer(getClockBuffer());
      atlasTeleportPoseCommandBuffer = setupDoubleBuffer(getAtlasTeleportPoseBuffer());
      leftEyeImageBuffer = setupByteBuffer(getLeftEyeImageBuffer());
      rightEyeImageBuffer = setupByteBuffer(getRightEyeImageBuffer());
   }

   public static ROSVehicleTeleopCheatNativeLibraryCommunicator getInstance(String rosMasterURI)
   {
      if (instance == null)
      {
         instance = new ROSVehicleTeleopCheatNativeLibraryCommunicator(rosMasterURI);
      }
      else if (!rosMasterURI.equals(ROSVehicleTeleopCheatNativeLibraryCommunicator.rosMasterURI))
      {
         throw new RuntimeException("Cannot get an instance of ROSVehicleTeleopCheatNativeLibraryCommunicator for " + rosMasterURI + ", already connected to "
                                    + ROSVehicleTeleopCheatNativeLibraryCommunicator.rosMasterURI);
      }

      return instance;
   }

   public void connect()
   {
      new Thread(new Runnable()
      {
         public void run()
         {
            spin();
         }
      }).start();
   }

   private static DoubleBuffer setupDoubleBuffer(ByteBuffer buffer)
   {
      buffer.order(ByteOrder.nativeOrder());

      return buffer.asDoubleBuffer();
   }

   private static LongBuffer setupLongBuffer(ByteBuffer buffer)
   {
      buffer.order(ByteOrder.nativeOrder());

      return buffer.asLongBuffer();
   }

   private static ByteBuffer setupByteBuffer(ByteBuffer buffer)
   {
      buffer.order(ByteOrder.nativeOrder());

      return buffer;
   }

   public void sendDirectionCommand(long timestamp, int delay, Int8Message directionCommand)
   {
      sendDirectionCommand((byte) directionCommand.getValue(), timestamp, delay);
   }

   public void sendSteeringWheelCommand(long timestamp, int delay, Float64Message steeringWheelCommand)
   {
      sendSteeringWheelCommand(steeringWheelCommand.getValue(), timestamp, delay);
   }

   public void sendHandBrakeCommand(long timestamp, int delay, Float64Message handBrakeCommand)
   {
      sendHandBrakeCommand(handBrakeCommand.getValue(), timestamp, delay);
   }

   public void sendGasPedalCommand(long timestamp, int delay, Float64Message gasPedalCommand)
   {
      sendGasPedalCommand(gasPedalCommand.getValue(), timestamp, delay);
   }

   public void sendBrakePedalCommand(long timestamp, int delay, Float64Message brakePedalCommand)
   {
      sendBrakePedalCommand(brakePedalCommand.getValue(), timestamp, delay);
   }

   public void sendAtlasTeleportIntoVehicleCommand(long timestamp, int delay, PoseMessage atlasTeleportPoseCommand)
   {
      atlasTeleportPoseCommand.copyToBuffer(atlasTeleportPoseCommandBuffer);
      sendAtlasTeleportIntoVehicleCommand(timestamp, delay);
   }

   public void sendAtlasTeleportOutOfVehicleCommand(long timestamp, int delay, PoseMessage atlasTeleportPoseCommand)
   {
      atlasTeleportPoseCommand.copyToBuffer(atlasTeleportPoseCommandBuffer);
      sendAtlasTeleportOutOfVehicleCommand(timestamp, delay);
   }

   public void enableOutput()
   {
      enableOutputNative();
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedClockMessage()
   {
      clock.setFromBuffer(clockBuffer);

      for (int i = 0; i < clockListeners.size(); i++)
      {
         clockListeners.get(i).receivedClockMessage(clock);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedVehiclePose()
   {
      vehiclePose.setFromBuffer(vehiclePoseBuffer);

      for (int i = 0; i < vehiclePoseListeners.size(); i++)
      {
         vehiclePoseListeners.get(i).receivedVehiclePose(vehiclePose);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedHandBrakeState(long timestamp)
   {
      handBrakeState.setValue(getHandBrakeState());

      for (int i = 0; i < handBrakeStateListeners.size(); i++)
      {
         handBrakeStateListeners.get(i).receivedHandBrakeState(handBrakeState, timestamp);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedSteeringWheelState(long timestamp)
   {
      steeringWheelState.setValue(getSteeringWheelState());

      for (int i = 0; i < steeringWheelStateListeners.size(); i++)
      {
         steeringWheelStateListeners.get(i).receivedSteeringWheelState(steeringWheelState, timestamp);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedGasPedalState(long timestamp)
   {
      gasPedalState.setValue(getGasPedalState());

      for (int i = 0; i < gasPedalStateListeners.size(); i++)
      {
         gasPedalStateListeners.get(i).receivedGasPedalState(gasPedalState, timestamp);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedBrakePedalState(long timestamp)
   {
      brakePedalState.setValue(getBrakePedalState());

      for (int i = 0; i < brakePedalStateListeners.size(); i++)
      {
         brakePedalStateListeners.get(i).receivedBrakePedalState(brakePedalState, timestamp);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedLeftEyeImage(long timestamp, int size)
   {
      leftEyeImageMessage.setFromBuffer(leftEyeImageBuffer, size);

      for(int i = 0 ; i < leftEyeImageListeners.size(); i++)
      {
         leftEyeImageListeners.get(i).receivedImage(leftEyeImageMessage, timestamp);
      }
   }

   /*
    * Do not remove due to non-use! Invoked by native library!
    */
   @SuppressWarnings("UnusedDeclaration")
   private void receivedRightEyeImage(long timestamp, int size)
   {
      rightEyeImageMessage.setFromBuffer(rightEyeImageBuffer, size);

      for(int i = 0 ; i < rightEyeImageListeners.size(); i++)
      {
         rightEyeImageListeners.get(i).receivedImage(rightEyeImageMessage, timestamp);
      }
   }


   private native boolean register(String rosMasterURI, String myIP);

   private native void spin();

   public native double getSteeringWheelState();

   public native double getHandBrakeState();

   public native double getGasPedalState();

   public native double getBrakePedalState();

   private native ByteBuffer getVehiclePoseBuffer();

   private native ByteBuffer getAtlasTeleportPoseBuffer();

   private native ByteBuffer getClockBuffer();

   private native ByteBuffer getLeftEyeImageBuffer();

   private native ByteBuffer getRightEyeImageBuffer();

   private native void sendDirectionCommand(byte directionCommand, long timestamp, int delay);

   private native void sendSteeringWheelCommand(double steeringWheelCommand, long timestamp, int delay);

   private native void sendHandBrakeCommand(double handBrakeCommand, long timestamp, int delay);

   private native void sendGasPedalCommand(double gasPedalCommand, long timestamp, int delay);

   private native void sendBrakePedalCommand(double brakePedalCommand, long timestamp, int delay);

   private native void sendAtlasTeleportIntoVehicleCommand(long timestamp, int delay);

   private native void sendAtlasTeleportOutOfVehicleCommand(long timestamp, int delay);

   private native void enableOutputNative();
}
