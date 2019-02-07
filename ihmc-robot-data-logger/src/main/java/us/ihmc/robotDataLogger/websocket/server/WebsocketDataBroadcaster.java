package us.ihmc.robotDataLogger.websocket.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import us.ihmc.robotDataLogger.websocket.command.DataServerCommand;

/**
 * Helper class that keep track of active connections and writes data to all connections
 * 
 * @author Jesper Smith
 *
 */
class WebsocketDataBroadcaster implements ChannelFutureListener
{
   private final Object channelLock = new Object();

   private final ArrayList<WebsocketDataServerFrameHandler> channels = new ArrayList<WebsocketDataServerFrameHandler>();

   public WebsocketDataBroadcaster()
   {
   }

   public void addClient(WebsocketDataServerFrameHandler websocketLogFrameHandler)
   {

      synchronized (channelLock)
      {
         channels.add(websocketLogFrameHandler);
         websocketLogFrameHandler.addCloseFutureListener(this);
      }
   }

   public void write(int bufferID, long timestamp, ByteBuffer frame) throws IOException
   {
      synchronized (channelLock)
      {
         for (int i = 0; i < channels.size(); i++)
         {
            channels.get(i).write(bufferID, timestamp, frame);
         }
      }

   }

   @Override
   public void operationComplete(ChannelFuture future) throws Exception
   {
      synchronized (channelLock)
      {
         for (int i = 0; i < channels.size(); i++)
         {
            if (channels.get(i).channel() == future.channel())
            {
               channels.remove(i).release();
               return;
            }
         }
      }
   }

   public void writeCommand(DataServerCommand command, int argument)
   {
      synchronized (channelLock)
      {
         for (int i = 0; i < channels.size(); i++)
         {
            channels.get(i).writeCommand(command, argument);
         }
      }

   }

   public void publishTimestamp(long timestamp)
   {
      synchronized (channelLock)
      {
         for (int i = 0; i < channels.size(); i++)
         {
            channels.get(i).publishTimestamp(timestamp);
         }
      }
   }

}
