package us.ihmc.robotDataCommunication;

import us.ihmc.commons.Conversions;
import us.ihmc.robotDataLogger.YoVariableServer;
import us.ihmc.robotDataLogger.logger.LogSettings;
import us.ihmc.robotDataLogger.util.JVMStatisticsGenerator;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.yoVariables.variable.IntegerYoVariable;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.util.PeriodicNonRealtimeThreadScheduler;

public class TestYoVariableConnection
{
   enum TestEnum
   {
      A, B, C, D
   }
   
   private final YoVariableRegistry registry = new YoVariableRegistry("tester");
   private final YoDouble var1 = new YoDouble("var1", registry);
   private final YoDouble var2 = new YoDouble("var2", registry);
   private final IntegerYoVariable var4 = new IntegerYoVariable("var4", registry);
   private final IntegerYoVariable var5 = new IntegerYoVariable("var5", registry);
   private final EnumYoVariable<TestEnum> var3 = new EnumYoVariable<TestEnum>("var3", "", registry, TestEnum.class, true);
   
   private final IntegerYoVariable echoIn = new IntegerYoVariable("echoIn", registry);
   private final IntegerYoVariable echoOut = new IntegerYoVariable("echoOut", registry);
   
   private final IntegerYoVariable timeout = new IntegerYoVariable("timeout", registry);
   
   private final YoBoolean startVariableSummary = new YoBoolean("startVariableSummary", registry);
   private final YoBoolean gc = new YoBoolean("gc", registry);
   
   
   
   private final YoVariableServer server = new YoVariableServer(getClass(), new PeriodicNonRealtimeThreadScheduler("TestYoVariableConnection"), null, LogSettings.TEST_LOGGER, 0.001);
   private final JVMStatisticsGenerator jvmStatisticsGenerator = new JVMStatisticsGenerator(server);
   
   
   private volatile long timestamp = 0;
   
   public TestYoVariableConnection()
   {
      server.setSendKeepAlive(true);
      server.setMainRegistry(registry, null, null);
      
      server.createSummary("tester.startVariableSummary");
      server.addSummarizedVariable("tester.var1");
      server.addSummarizedVariable("tester.var2");
      server.addSummarizedVariable(var4);
      
      jvmStatisticsGenerator.addVariablesToStatisticsGenerator(server);
      
      startVariableSummary.set(false);
      
      jvmStatisticsGenerator.start();
      
      new ThreadTester(server).start();
      server.start();
      var4.set(5000);
      
      timeout.set(1);
      
      int i = 0;
      TestEnum[] values = { TestEnum.A, TestEnum.B, TestEnum.C, TestEnum.D };
      while(true)
      {
         var1.add(1.0);
         var2.sub(1.0);
         var4.subtract(1);
         
         if(++i >= values.length)
         {
            i = 0;
         }
         var3.set(values[i]);
         
//         var5.set(new Random().nextInt());
         
         echoOut.set(echoIn.getIntegerValue());
         
         if(gc.getBooleanValue())
         {
            System.gc();
            gc.set(false);
         }
         
         timestamp += Conversions.millisecondsToNanoseconds(1);
         server.update(timestamp);
         ThreadTools.sleep(timeout.getIntegerValue());
      }
   }
   
   private class ThreadTester extends Thread
   {
      private final YoVariableRegistry registry = new YoVariableRegistry("Thread");
      private final YoDouble A = new YoDouble("A", registry);
      private final YoDouble B = new YoDouble("B", registry);
      private final YoDouble C = new YoDouble("C", registry);
      
      
      private final EnumYoVariable<TestEnum> echoThreadIn = new EnumYoVariable<TestEnum>("echoThreadIn", registry, TestEnum.class, false);
      private final EnumYoVariable<TestEnum> echoThreadOut = new EnumYoVariable<TestEnum>("echoThreadOut", registry, TestEnum.class, false);

      
      public ThreadTester(YoVariableServer server)
      {
         server.addRegistry(registry, null);
      }
      
      public void run()
      {
         while(true)
         {
            A.set(A.getDoubleValue() + 0.5);
            B.set(B.getDoubleValue() - 0.5);
            C.set(C.getDoubleValue() * 2.0);
            
            echoThreadOut.set(echoThreadIn.getEnumValue());
            
            server.update(timestamp, registry);
            
            ThreadTools.sleep(10);
         }
      }
      
      
   }
   
   public static void main(String[] args)
   {
      new TestYoVariableConnection();
   }
}
