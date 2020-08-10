plugins {
   id("us.ihmc.ihmc-build") version "0.20.1"
   id("us.ihmc.ihmc-ci") version "5.3"
   id("us.ihmc.ihmc-cd") version "1.14"
   id("us.ihmc.log-tools-plugin") version "0.5.0"
}

ihmc {
   loadProductProperties("../product.properties")
   
   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("org.apache.commons:commons-lang3:3.8.1")
   api("org.ejml:ejml-ddense:0.39")
   api("org.ejml:ejml-core:0.39")
   api("jakarta.xml.bind:jakarta.xml.bind-api:2.3.2")
   api("org.glassfish.jaxb:jaxb-runtime:2.3.2")

   api("us.ihmc:euclid:0.15.0")
   api("us.ihmc:euclid-geometry:0.15.0")
   api("us.ihmc:ihmc-yovariables:0.8.0")
   api("us.ihmc:ihmc-graphics-description:0.18.0")
   api("us.ihmc:ihmc-humanoid-robotics:source")
   api("us.ihmc:ihmc-common-walking-control-modules:source")
   api("us.ihmc:ihmc-sensor-processing:source")
   api("us.ihmc:ihmc-java-toolkit:source")
   api("us.ihmc:ihmc-robotics-toolkit:source")
   api("us.ihmc:ekf:0.6.0")
   api("us.ihmc:ihmc-lord-microstrain-drivers:0.0.2")
}

testDependencies {
   api("us.ihmc:euclid:0.15.0")
   api("us.ihmc:euclid-geometry:0.15.0")
   api("us.ihmc:simulation-construction-set:0.19.0")
   api("us.ihmc:simulation-construction-set-test:0.19.0")
   api("us.ihmc:ihmc-robotics-toolkit-test:source")
}
