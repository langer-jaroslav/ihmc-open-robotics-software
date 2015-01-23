package us.ihmc.simulationconstructionset.util.simulationRunner;

import java.util.ArrayList;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.utilities.ThreadTools;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class BlockingSimulationRunner
{
   private static final long CLOSING_SLEEP_TIME = 1000;
   private final SimulationConstructionSet scs;

   private final double maximumClockRunTimeInSeconds;
   private final boolean destroySimulationIfOverrunMaxTime;

   public BlockingSimulationRunner(SimulationConstructionSet scs, double maximumClockRunTimeInSeconds)
   {
      this(scs, maximumClockRunTimeInSeconds, true);
   }

   public BlockingSimulationRunner(SimulationConstructionSet scs, double maximumClockRunTimeInSeconds, boolean destroySimulationaIfOverrunMaxTime)
   {
      this.scs = scs;

      this.maximumClockRunTimeInSeconds = maximumClockRunTimeInSeconds;
      this.destroySimulationIfOverrunMaxTime = destroySimulationaIfOverrunMaxTime;
   }

   public void simulateNTicksAndBlock(int numberOfTicks) throws SimulationExceededMaximumTimeException
   {
      // TODO: Sometimes you need to sleep before simulating and blocking. Need to fix up the threading stuff in SCS to make more reliable.
//    if (!hasSleptOnce)
//    {
//       hasSleptOnce = true;
//       sleep(3000);
//    }

      scs.simulate(numberOfTicks);

//    waitForSimulationToStart();
      waitForSimulationToFinish(scs, maximumClockRunTimeInSeconds, destroySimulationIfOverrunMaxTime);
   }

   public void simulateAndBlock(double simulateTime) throws SimulationExceededMaximumTimeException
   {
//    System.out.println("Starting Simulation for " + simulateTime);

      // TODO: Sometimes you need to sleep before simulating and blocking. Need to fix up the threading stuff in SCS to make more reliable.

//    if (!hasSleptOnce)
//    {
//       hasSleptOnce = true;
//       sleep(3000);
//    }

      double startTime = scs.getTime();
      scs.simulate(simulateTime);

//    waitForSimulationToStart();
      waitForSimulationToFinish(scs, maximumClockRunTimeInSeconds, destroySimulationIfOverrunMaxTime);

      double endTime = scs.getTime();
      double elapsedTime = endTime - startTime;
      
      if (Math.abs(elapsedTime - simulateTime) > 0.01)
      {
         throw new SimulationExceededMaximumTimeException("Elapsed time didn't equal requested. Sim probably crashed");
      }
      
//    System.out.println("Done Simulation for " + simulateTime);

   }

   public boolean doOneShotRewindTest(double t0, double t1, double t2) throws SimulationExceededMaximumTimeException
   {
      boolean passed = true;

      // Run to t0 and put a setpoint there.
      sleep(1000);
      simulateAndBlock(t0);
      sleep(1000);

      scs.setInPoint();
      sleep(1000);

      // Run to t1 and save the state.
      simulateAndBlock(t1 - t0);
      sleep(1000);

      String filenameOne = "Tests/test_" + getTimeString(scs.getTime()) + ".state";
      scs.writeState(filenameOne);
      sleep(1000);

      // Run to t2
      simulateAndBlock(t2 - t1);
      sleep(1000);

      // Rewind to t0 and simulate to t1 again.
      scs.gotoInPointNow();
      sleep(1000);

      simulateAndBlock(t1 - t0);
      sleep(1000);

      // Save the state again.
      String filenameTwo = "Tests/test_" + getTimeString(scs.getTime()) + "_Rewind.state";
      scs.writeState(filenameTwo);
      sleep(1000);

      // Now compare the two:

      double maxPercentDiff = 0.001;
      ArrayList<VariableDifference> changedVariables = StateFileComparer.percentualCompareStateFiles(filenameOne, filenameTwo, maxPercentDiff, null);

      if (changedVariables.size() > 0)
      {
         System.err.println("Difference between " + filenameOne + " and " + filenameTwo);
         passed = false;
      }

      return passed;
   }

   private static void sleep(long sleepTimeMillis)
   {
      try
      {
         Thread.sleep(sleepTimeMillis);
      }
      catch (InterruptedException ex)
      {
         ex.printStackTrace();
      }
   }

   private static String getTimeString(double time)
   {
      String ret = Double.toString(time);

      ret = ret.substring(0, Math.min(8, ret.length() - 1));

      return ret;
   }

   public void destroySimulation()
   {
      destroySimulation(scs);
   }

   private static void destroySimulation(SimulationConstructionSet scs)
   {
      ThreadTools.sleep(CLOSING_SLEEP_TIME);
      scs.closeAndDispose();
      scs = null;
   }

   public static void waitForSimulationToFinish(SimulationConstructionSet scs, double maximumClockRunTimeInSeconds, boolean destroySimulationaIfOverrunMaxTime)
           throws SimulationExceededMaximumTimeException
   {
      long startTime = System.currentTimeMillis();

      while (scs.isSimulating())
      {
         sleep(100);

         long currentTime = System.currentTimeMillis();
         double elapsedTime = ((double) (currentTime - startTime)) * 0.001;

         if (elapsedTime > maximumClockRunTimeInSeconds)
         {
            scs.stop();
            if (destroySimulationaIfOverrunMaxTime)
               destroySimulation(scs);

            throw new SimulationExceededMaximumTimeException("Simulation Exceeded maximumClockRunTimeInSeconds");
         }
      }
   }

   public static class SimulationExceededMaximumTimeException extends Exception
   {
      private static final long serialVersionUID = 9041559998947724357L;

      public SimulationExceededMaximumTimeException(String description)
      {
         super(description);
      }
   }
}
