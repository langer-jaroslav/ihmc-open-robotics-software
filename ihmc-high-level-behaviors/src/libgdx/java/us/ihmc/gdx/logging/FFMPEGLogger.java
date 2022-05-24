package us.ihmc.gdx.logging;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.global.swresample;
import org.bytedeco.ffmpeg.global.swscale;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoImage;

import java.io.*;

/**
 * Doxygen:
 * https://ffmpeg.org/doxygen/trunk/index.html
 * Wiki:
 * https://trac.ffmpeg.org/
 */
public class FFMPEGLogger
{
   private final String fileName;
   private boolean isInitialized = false;
   private boolean isClosed = false;
   private final AVDictionary avDictionary;
   private final AVFormatContext avFormatContext;

   private AVPacket tempPacket;
   private AVStream stream;
   private AVCodecContext encoder;
   private AVFrame frame;
   private AVFrame tempFrame;
   private SwsContext swsContext;
   private SwrContext swrContext;
   private int nextPts;

   private AVFrame rgbTempFrame;
   private SwsContext rgbSwsContext;

   public FFMPEGLogger(int width, int height, boolean lossless, int framerate, String fileName)
   {
      this.fileName = fileName;

      int avDictFlags = 0;
      avDictionary = new AVDictionary();
      avutil.av_dict_set(avDictionary, "lossless", lossless ? "1" : "0", avDictFlags); // TODO this is maybe wrong

      LogTools.info("Building FFMPEG contexts");

      // Output context
      avFormatContext = new AVFormatContext();
      int returnCode = avformat.avformat_alloc_output_context2(avFormatContext, null, "webm", fileName);
      if (returnCode != 0 || avFormatContext.isNull())
      {
         LogTools.error("{}: Failed to find output format webm (does this computer support webm?). The logger will not begin.",
                        FFMPEGTools.getErrorCodeString(returnCode));
         isClosed = true;
         return;
      }

      AVOutputFormat outputFormat = avFormatContext.oformat();

      LogTools.debug("Codec ID: " + outputFormat.video_codec());

      // Add video stream
      AVCodec codec = avcodec.avcodec_find_encoder(outputFormat.video_codec());
      if (codec == null)
      {
         LogTools.error("Webm codec is null (does this computer support webm?). The logger will not begin.");
         isClosed = true;
         return;
      }
      AVCodecContext avCodecContext = avcodec.avcodec_alloc_context3(codec);

      tempPacket = avcodec.av_packet_alloc();
      stream = avformat.avformat_new_stream(avFormatContext, null);
      stream.id(avFormatContext.nb_streams() - 1); // I don't know what this does at all, but it's in the example
      encoder = avCodecContext;

      avCodecContext.codec_id(avFormatContext.video_codec_id());
      avCodecContext.bit_rate(400000); // This is what they've used in all the examples but is arbitrary other than that
      avCodecContext.width(width);
      avCodecContext.height(height);

      AVRational framePeriod = new AVRational();
      framePeriod.num(1);
      framePeriod.den(framerate);
      stream.time_base(framePeriod);
      avCodecContext.time_base(framePeriod);

      avCodecContext.gop_size(12); // Some or all of these settings may be unnecessary with lossless
      avCodecContext.pix_fmt(avutil.AV_PIX_FMT_YUV420P);

      if ((outputFormat.flags() & avformat.AVFMT_GLOBALHEADER) != 0)
         avCodecContext.flags(avCodecContext.flags() | avcodec.AV_CODEC_FLAG_GLOBAL_HEADER);
   }

   /***
    * The first time a frame is put will take longer than the others because of initialization
    */
   public boolean put(BytedecoImage image)
   {
      if (isClosed)
         return false;

      if (!isInitialized)
      {
         AVDictionary optAVDictionary = new AVDictionary();
         avutil.av_dict_copy(optAVDictionary, avDictionary, 0);

         try
         {
            if (avcodec.avcodec_open2(encoder, encoder.codec(), optAVDictionary) > 0) // TODO codec may be wrong here
            {
               LogTools.error("Could not open video codec. Logging will not begin.");
               close();
               return false;
            }
         }
         finally
         {
            avutil.av_dict_free(optAVDictionary); //Free dictionary even if we return false up there
         }

         AVFrame pic = avutil.av_frame_alloc();
         pic.format(encoder.pix_fmt());
         pic.width(encoder.width());
         pic.height(encoder.height());

         frame = pic;

         AVFrame tempPic = avutil.av_frame_alloc();
         tempPic.format(avutil.AV_PIX_FMT_YUV420P);
         tempPic.width(encoder.width());
         tempPic.height(encoder.height());

         tempFrame = tempPic;

         if (avutil.av_frame_get_buffer(pic, 0) > 0 || avutil.av_frame_get_buffer(tempPic, 0) > 0)
         {
            LogTools.error("Could not get framebuffer. Logging will not begin.");
            close();
            return false;
         }

         if (avcodec.avcodec_parameters_from_context(stream.codecpar(), encoder) > 0)
         {
            LogTools.error("Could not copy parameters to muxer. Logging will not begin.");
            close();
            return false;
         }

         avformat.av_dump_format(avFormatContext, 0, fileName, 1); //this is not freeing the memory - that's avformat_free_context (called during close())

         try
         {
            new File(fileName).getParentFile().mkdirs();
         }
         catch (Exception ignored) {}

         AVIOContext pb = new AVIOContext();
         int ret = 0;
         if ((ret = avformat.avio_open(pb, fileName, avformat.AVIO_FLAG_WRITE)) < 0)
         {
            LogTools.error("{}: Could not open file for writing. Logging will not begin.", FFMPEGTools.getErrorCodeString(ret));
            close();
            return false;
         }
         avFormatContext.pb(pb);

         if ((ret = avformat.avformat_write_header(avFormatContext, optAVDictionary)) < 0) {
            LogTools.error("{}: Could not write to file. Logging will not begin.", FFMPEGTools.getErrorCodeString(ret));
            close();
            return false;
         }

         isInitialized = true; //Initialization is now finished. Note that !isInitialized && isClosed is an error state
      }

      //Encode video
      AVFrame frame = getVideoFrame(image);
      avcodec.avcodec_send_frame(encoder, frame);

      //This while loop is weird, but copied from muxing.c
      int ret = 0;
      while (ret >= 0)
      {
         ret = avcodec.avcodec_receive_packet(encoder, tempPacket);
         if (ret < 0)
         {
            if (ret == -11)
            {
               continue; //Resource temporarily unavailable - we just try this again
            }
            else
            {
               LogTools.error("{}: Error encoding frame. Logging will stop.", FFMPEGTools.getErrorCodeString(ret));
               close();
               return false;
            }
         }

         avcodec.av_packet_rescale_ts(tempPacket, encoder.time_base(), stream.time_base());
         tempPacket.stream_index(stream.index());

         ret = avformat.av_interleaved_write_frame(avFormatContext, tempPacket);
         if (ret < 0)
         {
            LogTools.error("Error writing output packet. Logging will stop.");
            close();
            return false;
         }
      }

      avformat.avio_flush(avFormatContext.pb());

      return ret == avutil.AVERROR_EOF();
   }

   private void fillImage(AVFrame pict, BytedecoImage image, int width, int height)
   {
      if (rgbTempFrame == null)
      {
         rgbTempFrame = avutil.av_frame_alloc();
         rgbTempFrame.format(avutil.AV_PIX_FMT_RGBA);
         rgbTempFrame.width(width);
         rgbTempFrame.height(height);

         avutil.av_frame_get_buffer(rgbTempFrame, 0);
      }

      if (avutil.av_frame_make_writable(rgbTempFrame) < 0)
      {
         LogTools.error("Could not make frame writable. Logging will stop.");
         close();
      }

      rgbSwsContext = swscale.sws_getContext(width, height, avutil.AV_PIX_FMT_RGBA, width, height, avutil.AV_PIX_FMT_YUV420P, swscale.SWS_BICUBIC, null, null, (DoublePointer)null);

      if (rgbSwsContext == null || rgbSwsContext.isNull())
      {
         LogTools.error("Error creating SWS Context.");
         return;
      }

      for (int y = 0; y < height; y++)
      {
         for (int x = 0; x < width; x++)
         {
            int r = image.getBackingDirectByteBuffer().get(4 * (y * width + x));
            int g = image.getBackingDirectByteBuffer().get(4 * (y * width + x) + 1);
            int b = image.getBackingDirectByteBuffer().get(4 * (y * width + x) + 2);
            int a = image.getBackingDirectByteBuffer().get(4 * (y * width + x) + 3);
            //Note: x * 4 because 4 bytes per pixel
            Pointer data = rgbTempFrame.data().get();
            data.getPointer(y * rgbTempFrame.linesize().get() + x * 4).fill(r);
            data.getPointer(y * rgbTempFrame.linesize().get() + x * 4 + 1).fill(g);
            data.getPointer(y * rgbTempFrame.linesize().get() + x * 4 + 2).fill(b);
            data.getPointer(y * rgbTempFrame.linesize().get() + x * 4 + 3).fill(a);
         }
      }

      swscale.sws_scale(rgbSwsContext, rgbTempFrame.data(), rgbTempFrame.linesize(), 0, height, pict.data(), pict.linesize());
   }

   private AVFrame getVideoFrame(BytedecoImage image)
   {
      if (avutil.av_frame_make_writable(frame) < 0)
      {
         LogTools.error("Could not make frame writable. Logging will stop.");
         close();
         return null;
      }

      if (encoder.pix_fmt() != avutil.AV_PIX_FMT_YUV420P)
      {
         if (swsContext == null || swsContext.isNull())
         {
            swsContext = swscale.sws_getContext(encoder.width(),
                                                encoder.height(),
                                                avutil.AV_PIX_FMT_YUV420P,
                                                encoder.width(),
                                                encoder.height(),
                                                encoder.pix_fmt(),
                                                swscale.SWS_BICUBIC,
                                                null,
                                                null,
                                                (DoublePointer) null);

            if (swsContext == null || swsContext.isNull())
            {
               LogTools.error("Error creating SWS Context.");
               return null;
            }
         }

         fillImage(tempFrame, image, encoder.width(), encoder.height());

         swscale.sws_scale(swsContext, tempFrame.data(), tempFrame.linesize(), 0, encoder.height(), frame.data(), frame.linesize());
      }
      else
      {
         fillImage(frame, image, encoder.width(), encoder.height());
      }

      frame.pts(nextPts);
      nextPts = nextPts + 1;

      return frame;
   }

   public void close()
   {
      LogTools.info("Closing logger (if you did not expect this to happen, something has gone wrong, and logging will stop.)");
      isClosed = true;

      avformat.avio_close(avFormatContext.pb());

      if (encoder != null && !encoder.isNull())
         avcodec.avcodec_free_context(encoder);

      if (rgbTempFrame != null && !rgbTempFrame.isNull())
         avutil.av_frame_free(rgbTempFrame);

      if (rgbSwsContext != null && !rgbSwsContext.isNull())
         swscale.sws_freeContext(rgbSwsContext);

      if (frame != null && !frame.isNull())
         avutil.av_frame_free(frame);

      if (tempFrame != null && !tempFrame.isNull())
         avutil.av_frame_free(tempFrame);

      if (tempPacket != null && !tempPacket.isNull())
         avcodec.av_packet_free(tempPacket);

      if (swsContext != null && !swsContext.isNull())
         swscale.sws_freeContext(swsContext);

      if (swrContext != null && !swrContext.isNull())
         swresample.swr_free(swrContext);
   }

   @Override
   protected void finalize() throws Throwable
   {
      super.finalize();

      if (!isClosed)
         close(); //Ensure that file gets written properly
   }

   public boolean isClosed()
   {
      return isClosed;
   }
}
