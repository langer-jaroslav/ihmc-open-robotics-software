package us.ihmc.robotDataLogger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.multicastLogDataProtocol.MultiClientStreamingDataTCPServer;
import us.ihmc.multicastLogDataProtocol.control.SummaryProvider;
import us.ihmc.robotDataLogger.rtps.DataProducerParticipant;
import us.ihmc.tools.compression.SnappyUtils;
import us.ihmc.util.PeriodicThreadScheduler;

public class YoVariableProducer implements Runnable
{
   public static final int KEEP_ALIVE_RATE = 1000;
   public static final int TIMESTAMP_HEADER  = 0xAFAF;
   private static final int SEND_BUFFER_LENGTH = 1024;
   
   private final PeriodicThreadScheduler scheduler;
   
   private final ConcurrentRingBuffer<FullStateBuffer> mainBuffer;
   private final ConcurrentRingBuffer<RegistryBuffer>[] buffers;

   private final byte[] compressedBackingArray;
   private final ByteBuffer byteWriteBuffer;
   private final LongBuffer writeBuffer;
   private final ByteBuffer compressedBuffer;
   private final ByteBuffer compressedBufferDirect;
   private final int jointStateOffset;

   private int keepAliveCounter = 0;

   private final DataProducerParticipant participant;
   private MultiClientStreamingDataTCPServer server;
   
   private final LogDataHeader logDataHeader = new LogDataHeader();
   private final CRC32 crc32 = new CRC32();   
   private final boolean sendKeepAlive;
   
   @SuppressWarnings("unchecked")
   public YoVariableProducer(PeriodicThreadScheduler scheduler, YoVariableHandShakeBuilder handshakeBuilder, DataProducerParticipant participant,
         ConcurrentRingBuffer<FullStateBuffer> mainBuffer, Collection<ConcurrentRingBuffer<RegistryBuffer>> buffers, boolean sendKeepAlive)
   {
      this.scheduler = scheduler;
      this.mainBuffer = mainBuffer;
      this.buffers = buffers.toArray(new ConcurrentRingBuffer[buffers.size()]);
      this.sendKeepAlive = sendKeepAlive;
      this.participant = participant;

      this.jointStateOffset = handshakeBuilder.getNumberOfVariables();
      int numberOfJointStates = handshakeBuilder.getNumberOfJointStates();
      int bufferSize = (1 + jointStateOffset + numberOfJointStates) * 8;

      byteWriteBuffer = ByteBuffer.allocate(bufferSize);
      writeBuffer = byteWriteBuffer.asLongBuffer();

      compressedBackingArray = new byte[SnappyUtils.maxCompressedLength(bufferSize) + LogDataHeader.length()];
      compressedBuffer = ByteBuffer.wrap(compressedBackingArray);
      compressedBufferDirect = ByteBuffer.allocateDirect(compressedBuffer.capacity());
   }
   
   
   public void publishTimestampRealtime(long timestamp)
   {
      try
      {
         participant.publishTimestamp(timestamp);
      }
      catch (IOException e)
      {
         System.out.println(e.getMessage());
      }
   }

   private void updateBuffers(long timestamp)
   {
      for (int i = 0; i < buffers.length; i++)
      {
         ConcurrentRingBuffer<RegistryBuffer> buffer = buffers[i];
         if (buffer.poll())
         {
            RegistryBuffer newBuffer = buffer.peek();
            if (newBuffer != null && newBuffer.getTimestamp() < timestamp)
            {
               newBuffer = buffer.read();
               newBuffer.getIntoBuffer(writeBuffer, 1);
               buffer.flush();
            }
         }
      }
   }

   public void start()
   {
      try
      {
         // Make server here, so it is open before the logger connects
         server = new MultiClientStreamingDataTCPServer(participant.getPort(), compressedBackingArray.length, SEND_BUFFER_LENGTH);
         server.start();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

      scheduler.schedule(this, 1, TimeUnit.MILLISECONDS);
   }

   public void run()
   {

      while (mainBuffer.poll())
      {
         FullStateBuffer fullStateBuffer;

         if ((fullStateBuffer = mainBuffer.read()) != null)
         {
            writeBuffer.put(0, fullStateBuffer.getTimestamp());
            fullStateBuffer.getIntoBuffer(writeBuffer, 1);
            fullStateBuffer.getJointStatesInBuffer(writeBuffer, jointStateOffset + 1);
            updateBuffers(fullStateBuffer.getTimestamp());

            byteWriteBuffer.clear();
            compressedBuffer.clear();
            compressedBuffer.position(LogDataHeader.length());
            try
            {
               SnappyUtils.compress(byteWriteBuffer, compressedBuffer);
               compressedBuffer.flip();
            }
            catch (IllegalArgumentException | IOException e)
            {
               e.printStackTrace();
               continue;
            }

            crc32.reset();
            int dataSize = compressedBuffer.remaining() - LogDataHeader.length();
            crc32.update(compressedBackingArray, LogDataHeader.length() + compressedBuffer.arrayOffset(), dataSize);
            logDataHeader.setUid(fullStateBuffer.getUid());
            logDataHeader.setTimestamp(fullStateBuffer.getTimestamp());
            logDataHeader.setType(LogDataHeader.DATA_PACKET);
            logDataHeader.setDataSize(dataSize);
            logDataHeader.setCrc32((int) crc32.getValue());
            logDataHeader.writeBuffer(0, compressedBuffer);
            compressedBufferDirect.clear();
            compressedBufferDirect.put(compressedBuffer);
            compressedBufferDirect.flip();
            server.send(compressedBufferDirect);
            
            keepAliveCounter = 0;
         }
         mainBuffer.flush();
      }
      
      if(sendKeepAlive)
      {
         if(++keepAliveCounter > KEEP_ALIVE_RATE)
         {
            logDataHeader.setUid(-1);
            logDataHeader.setTimestamp(-1);
            logDataHeader.setType(LogDataHeader.KEEP_ALIVE_PACKET);
            logDataHeader.setDataSize(0);
            logDataHeader.setCrc32(0);
            keepAliveCounter = 0;
            
            compressedBufferDirect.clear();
            logDataHeader.writeBuffer(0, compressedBufferDirect);
            compressedBufferDirect.limit(LogDataHeader.length());
            server.send(compressedBufferDirect);
            
         }
      }

   }

   public void close()
   {
      scheduler.shutdown();
      server.close();
      
   }
}