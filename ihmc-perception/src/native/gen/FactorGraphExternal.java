// Targeted by JavaCPP version 1.5.7: DO NOT EDIT THIS FILE

import java.nio.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.annotation.*;

public class FactorGraphExternal extends ihmc_slam_wrapper {
    static { Loader.load(); }

// Parsed from include/factor_graph_external.h

// #pragma once

// #include "factor_graph_handler.h"

public static class FactorGraphExternal extends Pointer {
    static { Loader.load(); }
    /** Default native constructor. */
    public FactorGraphExternal() { super((Pointer)null); allocate(); }
    /** Native array allocator. Access with {@link Pointer#position(long)}. */
    public FactorGraphExternal(long size) { super((Pointer)null); allocateArray(size); }
    /** Pointer cast constructor. Invokes {@link Pointer#Pointer(Pointer)}. */
    public FactorGraphExternal(Pointer p) { super(p); }
    private native void allocate();
    private native void allocateArray(long size);
    @Override public FactorGraphExternal position(long position) {
        return (FactorGraphExternal)super.position(position);
    }
    @Override public FactorGraphExternal getPointer(long i) {
        return new FactorGraphExternal((Pointer)this).offsetAddress(i);
    }

        // Expects packed Pose3
        public native void addPriorPoseFactor(int index, FloatPointer pose);
        public native void addPriorPoseFactor(int index, FloatBuffer pose);
        public native void addPriorPoseFactor(int index, float[] pose);

        // Expects packed Pose3
        public native void addOdometryFactor(FloatPointer odometry, int poseId);
        public native void addOdometryFactor(FloatBuffer odometry, int poseId);
        public native void addOdometryFactor(float[] odometry, int poseId);

        // Expects packed Vector4
        public native void addOrientedPlaneFactor(FloatPointer lmMean, int lmId, int poseIndex);
        public native void addOrientedPlaneFactor(FloatBuffer lmMean, int lmId, int poseIndex);
        public native void addOrientedPlaneFactor(float[] lmMean, int lmId, int poseIndex);

        public native void optimize();

        public native void optimizeISAM2(@Cast("uint8_t") byte numberOfUpdates);

        public native void clearISAM2();

        // Expects packed Pose3
        public native void setPoseInitialValue_Pose3(int index, FloatPointer value);
        public native void setPoseInitialValue_Pose3(int index, FloatBuffer value);
        public native void setPoseInitialValue_Pose3(int index, float[] value);

        // Expects packed OrientedPlane3
        public native void setOrientedPlaneInitialValue(int landmarkId, FloatPointer value);
        public native void setOrientedPlaneInitialValue(int landmarkId, FloatBuffer value);
        public native void setOrientedPlaneInitialValue(int landmarkId, float[] value);

        // Expects packed Vector6
        public native void createOdometryNoiseModel(FloatPointer odomVariance);
        public native void createOdometryNoiseModel(FloatBuffer odomVariance);
        public native void createOdometryNoiseModel(float[] odomVariance);

        // Expects packed Vector3
        public native void createOrientedPlaneNoiseModel(FloatPointer lmVariances);
        public native void createOrientedPlaneNoiseModel(FloatBuffer lmVariances);
        public native void createOrientedPlaneNoiseModel(float[] lmVariances);
}

}
