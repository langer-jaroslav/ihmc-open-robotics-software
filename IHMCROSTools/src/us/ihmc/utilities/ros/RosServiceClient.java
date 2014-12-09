package us.ihmc.utilities.ros;

import org.ros.exception.RosRuntimeException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.internal.message.Message;
import org.ros.node.ConnectedNode;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;

public class RosServiceClient<T extends Message, S extends Message>
{
   private final String requestType;

   private ServiceClient<T, S> client;

   private final Object syncObject = new Object();

   private ConnectedNode connectedNode = null;
   private String attachedServiceName = null;

   public RosServiceClient(String requestType)
   {
      this.requestType = requestType;
   }

   public ServiceClient<T, S> getClient()
   {
      return client;
   }

   public String getRequestType()
   {
      return requestType;
   }

   public void setServiceClient(ServiceClient<T, S> client, ConnectedNode connectedNode, String attachedServiceName)
   {
      this.connectedNode = connectedNode;
      this.attachedServiceName = attachedServiceName;
      this.client = client;
      connected();
   }

   private void checkInitialized()
   {
      if (client == null)
      {
         throw new RuntimeException("RosServiceClient is not registered with RosMainNode");
      }
   }
   

   /**
    * @param request
    * @param response
    */
   public void call(T request, ServiceResponseListener<S> response)
   {
      checkInitialized();
  

      if (!client.isConnected() || client == null)
      {
         if (client != null)
            client.shutdown();

         //locate URI
         System.err.println("re-connecting to service " + attachedServiceName);

         try
         {
            client = connectedNode.newServiceClient(attachedServiceName, getRequestType());
         }
         catch (ServiceNotFoundException e)
         {
            System.err.println("rennection failed. Service not found");
            throw new RosRuntimeException(e);
         }
         
         System.err.println("service re-connected, making call");
      }

      client.call(request, response);

   }

   public T getMessage()
   {
      checkInitialized();

      return client.newMessage();
   }

   private void connected()
   {
      synchronized (syncObject)
      {
         syncObject.notifyAll();
      }
   }

   public void waitTillConnected()
   {
      while (client == null)
      {
         synchronized (syncObject)
         {
            try
            {
               syncObject.wait();
            }
            catch (InterruptedException e)
            {
            }
         }
      }
   }

}
