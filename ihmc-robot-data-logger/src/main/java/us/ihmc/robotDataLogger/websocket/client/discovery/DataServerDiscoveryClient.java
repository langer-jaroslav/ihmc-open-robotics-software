package us.ihmc.robotDataLogger.websocket.client.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import us.ihmc.log.LogTools;
import us.ihmc.robotDataLogger.websocket.client.discovery.HTTPDataServerConnection.HTTPDataServerConnectionListener;

public class DataServerDiscoveryClient
{
   private final Object lock = new Object();
   private final DataServerDiscoveryListener listener;

   private final ScheduledExecutorService connectionExecutor = Executors.newSingleThreadScheduledExecutor();
   private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

   private final HashMap<HTTPDataServerDescription, HTTPDataServerDescription> hosts = new HashMap<HTTPDataServerDescription, HTTPDataServerDescription>();

   private boolean clientClosed = false;
   private final HashSet<HTTPDataServerConnection> connections = new HashSet<HTTPDataServerConnection>();

   public DataServerDiscoveryClient(DataServerDiscoveryListener listener)
   {
      this.listener = listener;
   }

   public void addHost(String host, int port, boolean persistant)
   {
      synchronized (lock)
      {
         HTTPDataServerDescription description = new HTTPDataServerDescription(host, port, persistant);

         if (hosts.containsKey(description))
         {
            if (description.isPersistant())
            {
               LogTools.debug("{} already in list of hosts. Marking persistant", description);
               hosts.put(description, description);
            }
            else
            {
               LogTools.debug("{} already in list of hosts", description);
            }
         }
         else
         {
            hosts.put(description, description);
            connectionExecutor.execute(() -> tryConnection(description));
         }
      }
   }

   public void close()
   {
      close(null);
   }

   public void close(HTTPDataServerConnection connectionToKeep)
   {
      synchronized (lock)
      {
         clientClosed = true;
         if (connectionToKeep != null)
         {
            connections.remove(connectionToKeep);
         }

         for (HTTPDataServerConnection connection : connections)
         {
            connection.close();
         }
      }
   }

   private void tryConnection(HTTPDataServerDescription target)
   {
      LogTools.debug("Connecting to {}.", target);
      try
      {
         new HTTPDataServerConnection(target, new ConnectionListener());
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   private class ConnectionListener implements HTTPDataServerConnectionListener
   {
      @Override
      public void connected(HTTPDataServerConnection connection)
      {
         synchronized (lock)
         {
            LogTools.debug("Connected to {}.", connection.getTarget());

            connections.add(connection);
            listenerExecutor.execute(() -> listener.connected(connection));
         }
      }

      @Override
      public void disconnected(HTTPDataServerConnection connection)
      {
         synchronized (lock)
         {
            LogTools.debug("Disconnected from {}.", connection.getTarget());
            connections.remove(connection);
            listenerExecutor.execute(() -> listener.disconnected(connection));

            if (!clientClosed && hosts.get(connection.getTarget()).isPersistant())
            {
               LogTools.debug("{} is marked persistant, reconnecting.", connection.getTarget());
               connectionExecutor.execute(() -> tryConnection(connection.getTarget()));
            }
            else
            {
               LogTools.debug("{} is volatile. Dropping.", connection.getTarget());
               hosts.remove(connection.getTarget());
            }
         }
      }

      @Override
      public void connectionRefused(HTTPDataServerDescription target)
      {
         synchronized (lock)
         {
            LogTools.debug("Connection refused to {}.", target);

            if (!clientClosed && hosts.get(target).isPersistant())
            {
               LogTools.debug("{} is marked persistant, reconnecting.", target);
               connectionExecutor.schedule(() -> tryConnection(target), 1, TimeUnit.SECONDS);
            }
            else
            {
               LogTools.debug("{} is volatile. Dropping.", target);
               hosts.remove(target);
            }

         }
      }
   }

   public static void main(String[] args)
   {
      DataServerDiscoveryClient client = new DataServerDiscoveryClient(new DataServerDiscoveryListener()
      {

         @Override
         public void disconnected(HTTPDataServerConnection connection)
         {
            System.out.println("DISCONNECTED FROM " + connection.getTarget());
         }

         @Override
         public void connected(HTTPDataServerConnection connection)
         {
            System.out.println("CONNECTED TO " + connection.getTarget());

         }
      });

      client.addHost("127.0.0.1", 8008, true);
   }
}
