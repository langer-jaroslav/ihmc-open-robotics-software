package us.ihmc.realtime.helperClasses;

/**
 * @author Doug Stephen <a href="mailto:dstephen@ihmc.us">(dstephen@ihmc.us)</a>
 */
public class TimingInformation
{
   private long previousTime = 0;
   private long avgJitter = 0;
   private long maxJitter = 0;

   private long periodInNS;
   private long iterations = 0;

   private boolean isInitialized = false;

   public TimingInformation(String name, long periodInNS)
   {
      this.periodInNS = periodInNS;

      System.out.println(name + " Period, Hz: " + 1 / (periodInNS / 1e9));
   }

   public void initialize(long currentTime)
   {
      previousTime = currentTime;
      isInitialized = true;
   }

   public void updateTimingInformation(long newTime)
   {
      long jitter = Math.abs(newTime - previousTime - periodInNS);

      if (jitter > maxJitter)
      {
         maxJitter = jitter;
      }

      previousTime = newTime;
      avgJitter += jitter;

      iterations++;
   }

   public double getFinalMaxJitterMicroseconds()
   {
      return (double) maxJitter / 1e3;
   }

   public double getFinalAvgJitterMicroseconds()
   {
      return (double) avgJitter / (double) iterations / 1e3;
   }

   public boolean isInitialized()
   {
      return isInitialized;
   }
}
