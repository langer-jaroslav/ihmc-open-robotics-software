package us.ihmc.atlas.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.code.unitTesting.runner.BambooTestSuiteRunner;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseRocksTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseStandingYawedTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseSteppingStonesTest.class,
   us.ihmc.atlas.ObstacleCourseTests.AtlasObstacleCourseTrialsTerrainTest.class
})

public class AtlasNFastTestSuite
{
   public static void main(String[] args)
   {
      new BambooTestSuiteRunner(AtlasNFastTestSuite.class);
   }
}

