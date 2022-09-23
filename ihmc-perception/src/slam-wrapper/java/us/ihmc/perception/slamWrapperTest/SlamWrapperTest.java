package us.ihmc.perception.slamWrapperTest;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.slamWrapper.SlamWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlamWrapperTest
{
   static
   {
      // We need to disable javacpp from trying to automatically load libraries.
      // Otherwise, it will try to load them by name when they aren't in the library path
      // (LD_LIBRARY_PATH on Linux).
      //
      // The approach taken here is to use System.load to load each library by explicit
      // absolute path on disk.
      System.setProperty("org.bytedeco.javacpp.loadlibraries", "false");
   }

   public static void main(String[] args)
   {
      List<String> libraryFiles = new ArrayList<>();
      libraryFiles.add("libmetis-gtsam.so");
      libraryFiles.add("libgtsam.so");
      libraryFiles.add("libjniSlamWrapper.so");

      for (String libraryFile : libraryFiles)
      {
         try
         {
            Loader.cacheResource(libraryFile);
            File cacheDir = Loader.getCacheDir();
            System.load(cacheDir.getAbsolutePath() + "/main/" + libraryFile);
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }

      SlamWrapper.FactorGraphExternal factorGraphExternal = new SlamWrapper.FactorGraphExternal();

      factorGraphExternal.helloWorldTest();
   }
}
