package us.ihmc.tools.time;

import us.ihmc.commons.Conversions;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;
import us.ihmc.tools.thread.MissingThreadTools;

import java.util.UUID;

/**
 * An exponential smoothing frequency calculator with an optional logging thread to print the frequency once per second.
 * <a href="https://en.wikipedia.org/wiki/Exponential_smoothing">...</a>
 * Call {@link #ping()} on each new event.
 * Call {@link #getFrequency()} to get the frequency, which will remain constant if events stop.
 * Call {@link #getFrequencyDecaying()} to get the current frequency which trends to 0 when there are no events.
 */
public class FrequencyCalculator
{
   private double alpha = 0.3;
   private double lastEventTime = Double.NaN;
   private double smoothedPeriod = Double.NaN;

   private volatile boolean loggingThreadRunning;

   public FrequencyCalculator(boolean enableLoggingThread)
   {
      if (enableLoggingThread)
      {
         String threadID = UUID.randomUUID().toString().substring(0, 5);

         Thread loggingThread = new Thread(() ->
         {
            loggingThreadRunning = true;

            while (loggingThreadRunning)
            {
               LogTools.info("FrequencyCalculator[" + threadID + "] average rate: " + getFrequency());

               MissingThreadTools.sleep(1.0);
            }
         }, getClass().getSimpleName() + "-" + threadID);

         loggingThread.start();
      }
   }

   public FrequencyCalculator()
   {
      this(false);
   }

   private double calculateFrequency(boolean decay)
   {
      if (Double.isNaN(smoothedPeriod))
      {
         return 0.0;
      }
      else
      {
         double currentTime = Conversions.nanosecondsToSeconds(System.nanoTime());
         double ongoingPeriod = currentTime - lastEventTime;

         if (!decay || ongoingPeriod < smoothedPeriod) // Expecting an event after the current average period
         {
            return 1.0 / smoothedPeriod;
         }
         else // Events are slowing down or stopped
         {
            double psuedoSmoothedPeriod = (1.0 - alpha) * smoothedPeriod + alpha * ongoingPeriod;
            return 1.0 / psuedoSmoothedPeriod;
         }
      }
   }

   public void ping()
   {
      double currentTime = Conversions.nanosecondsToSeconds(System.nanoTime());

      if (!Double.isNaN(lastEventTime))
      {
         double period = currentTime - lastEventTime;

         if (Double.isNaN(smoothedPeriod))
         {
            smoothedPeriod = period;
         }
         else
         {
            smoothedPeriod = (1.0 - alpha) * smoothedPeriod + alpha * period;
         }
      }

      lastEventTime = currentTime;
   }

   public double getFrequency()
   {
      return calculateFrequency(false);
   }

   public double getFrequencyDecaying()
   {
      return calculateFrequency(true);
   }

   public void destroy()
   {
      loggingThreadRunning = false;
   }
}
