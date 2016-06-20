/*
 * Copyright (c) 2013-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Project BUBO.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package us.ihmc.sensorProcessing.bubo.clouds.detect;

/**
 * List of shapes which can be searched for in {@link PointCloudShapeFinder}
 *
 * @author Peter Abeles
 */
public enum CloudShapeTypes {
	/**
	 * Plane described by {@link georegression.struct.plane.PlaneGeneral3D_F64}.
	 */
	PLANE,
	/**
	 * Sphere described by {@link georegression.struct.shapes.Sphere3D_F64}.
	 */
	SPHERE,
	/**
	 * Cylinder described by {@link georegression.struct.shapes.Cylinder3D_F64}.
	 */
	CYLINDER
}
