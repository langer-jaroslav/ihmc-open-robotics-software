package us.ihmc.sensorProcessing.sensors;

public class RawJointSensorDataHolder
{
   private final String name;
   
   private boolean useOutputEncoder = false;
   private double q_out_offset;
   
   private double q_raw;
   private double q_out_raw;
   private double qd_out_raw;
   private double qd_raw;
   private double f_raw;
   

   public RawJointSensorDataHolder(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name;
   }
   
   public void set(RawJointSensorDataHolder source)
   {
      if(!source.getName().equals(getName()))
      {
         throw new RuntimeException("Source name does not equal name");
      }
      
      setQ_raw(source.getQ_raw());
      setQ_out_raw(source.getQ_out_raw());
      setQd_out_raw(source.getQd_out_raw());
      setQd_raw(source.getQd_raw());
      setF_raw(source.getF_raw());
   }

   
   
   public boolean isUseOutputEncoder()
   {
      return useOutputEncoder;
   }

   
   public void setUsesOutputEncoder(boolean useOutputEncoder)
   {
      this.useOutputEncoder = useOutputEncoder;
   }

   
   public double getQ_raw()
   {
      return q_raw;
   }

   
   public double getQd_raw()
   {
      return qd_raw;
   }

   
   public double getF_raw()
   {
      return f_raw;
   }

   
   public void setQ_raw(double q_raw)
   {
      this.q_raw = q_raw;
   }

   
   public void setQd_raw(double qd_raw)
   {
      this.qd_raw = qd_raw;
   }

   
   public void setF_raw(double f_raw)
   {
      this.f_raw = f_raw;
   }

   
   public double getQ_out_raw()
   {
      return q_out_raw;
   }

   
   public void setQ_out_raw(double q_out_raw)
   {
      this.q_out_raw = q_out_raw;
   }

   
   public double getQd_out_raw()
   {
      return qd_out_raw;
   }

   
   public void setQd_out_raw(double qd_out_raw)
   {
      this.qd_out_raw = qd_out_raw;
   }

   public double getQ_out_offset()
   {
      return q_out_offset;
   }

   public void setQ_out_offset(double q_out_offset)
   {
      this.q_out_offset = q_out_offset;
   }

}
