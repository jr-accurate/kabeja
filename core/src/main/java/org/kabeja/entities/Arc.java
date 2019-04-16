/*******************************************************************************
 * Copyright 2010 Simon Mieth
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
 ******************************************************************************/
/*
 * Created on Jun 28, 2004
 *
 */
package org.kabeja.entities;

import org.kabeja.common.Type;
import org.kabeja.math.*;


/**
 * @author <a href="mailto:simon.mieth@gmx.de>Simon Mieth</a>
 *
 */
public class Arc extends Entity {
	
    private Point3D center;
    private double radius;
    private double start_angle;
    private double end_angle;
    private boolean counterclockwise = false;

    public Arc() {
        center = new Point3D();
    }

    /**
     * @return Returns the end_angle.
     */
    public double getEndAngle() {
        return end_angle;
    }

    /**
     * @param end_angle
     *            The end_angle to set.
     */
    public void setEndAngle(double end_angle) {
        this.end_angle = end_angle;
    }

    /**
     * @return Returns the radius.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius
     *            The radius to set.
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * @return Returns the start_angle.
     */
    public double getStartAngle() {
        return start_angle;
    }

    /**
     * @param start_angle
     *            The start_angle to set.
     */
    public void setStartAngle(double start_angle) {
        this.start_angle = start_angle;
    }


    
    public Bounds getBounds() {
        Bounds bounds = new Bounds();
        Point3D start = this.getStartPoint();
        Point3D end = this.getEndPoint();
        bounds.addToBounds(start);
        bounds.addToBounds(end);

        ParametricPlane plane = new ParametricPlane(this.getExtrusion());
        Point3D center = plane.getPoint(this.center.getX(), this.center.getY());
        int startQ = MathUtils.getQuadrant(start, center);
        int endQ = MathUtils.getQuadrant(end, center);

        if (endQ < startQ) {
            endQ += 4;
        }

        while (endQ > startQ) {
            switch (startQ) {
            case 0:
                bounds.addToBounds(center.getX(), center.getY() + radius,
                    center.getZ());

                break;

            case 1:
                bounds.addToBounds(center.getX() - radius, center.getY(),
                    center.getZ());

                break;

            case 2:
                bounds.addToBounds(center.getX(), center.getY() - radius,
                    center.getZ());

                break;

            case 3:
                bounds.addToBounds(center.getX() + radius, center.getY(),
                    center.getZ());
                endQ -= 4;
                startQ -= 4;

                break;
            }

            startQ++;
        }

        return bounds;
    }

    public void setCenterPoint(Point3D p) {
        this.center = p;
    }

    public Point3D getCenterPoint() {
        return center;
    }

    /**
     * Calculate the start point of the arc (defined by the start parameter)
     *
     * @return the start point
     */
    public Point3D getStartPoint() {
        double angle = this.start_angle;

        // if (this.start_angle < 0) {
        // angle += 360;
        // }
        return this.getPointAt(angle);
    }

    /**
     * Calculate the end point of the arc (defined by the end parameter)
     *
     * @return the end point
     */
    public Point3D getEndPoint() {
        double angle = this.end_angle;

        // if (this.end_angle < 0) {
        // angle += 360;
        // }
        return this.getPointAt(angle);
    }

    /**
     * Calculate a point of the arc
     *
     * @param angle
     *            in degree
     * @return Point on the arc in WCS-coordinates
     */
    public Point3D getPointAt(double angle) {
        // the local part
        double x = this.radius * Math.cos(Math.toRadians(angle));
        double y = radius * Math.sin(Math.toRadians(angle));

        // the wcs part
        ParametricPlane plane = new ParametricPlane(this.center,
                this.getExtrusion().getDirectionX(),
                this.getExtrusion().getDirectionY(),
                this.getExtrusion().getNormal());

        Point3D p = plane.getPoint(x + this.center.getX(), y +
                this.center.getY());

        return p;
    }

    /**
     * @param angle of the point on the arc
     *              in degree
     * @return the point on the arc in OCS-coordinates
     */
    private Point3D getPointInOcs(double angle) {
        double x = this.radius * Math.cos(Math.toRadians(angle));
        double y = radius * Math.sin(Math.toRadians(angle));
        double z = this.center.getZ();

        return new Point3D(x,y,z);
    }

    /**
     *
     */
    public Type<Arc> getType() {
        return Type.TYPE_ARC;
    }

    public double getLength() {
        double alpha = this.getTotalAngle();

        return (alpha * Math.PI * this.radius) / 180.0;
    }

    public double getTotalAngle() {
        if (this.end_angle < this.start_angle) {
            return (360 + this.end_angle) - this.start_angle;
        } else {
            return Math.abs(this.end_angle - this.start_angle);
        }
    }

    public double getChordLength() {
        double s = 2 * this.radius * Math.sin(Math.toRadians(
                    this.getTotalAngle() / 2));

        return s;
    }

    public boolean isCounterClockwise() {
        return counterclockwise;
    }

    public void setCounterClockwise(boolean counterclockwise) {
        this.counterclockwise = counterclockwise;
    }

    
    /**
     * Not implemented yet
     */
    
    public void transform(TransformContext context) {
      this.center = context.transform(this.center);
    }

    /**
     * Transforms the arcs coordinates from OCS to WCS.
     * Does not simply translate the coordinates, but "reverts" the arbitrary
     * axis algorithm's effects.
     * Should only ever be called after the whole arc has been constructed
     * i.e. when the entity's block has ended.
     */
    public void transformToWcs() {
        Extrusion e = this.getExtrusion();
        if (getExtrusion().getNormal().equals(new Vector(0,0,1))) {
            return;
        }
        Point3D transformedStart = e.transformOcsToWcs(getPointInOcs(this.start_angle));
        Point3D transformedEnd = e.transformOcsToWcs(getPointInOcs(this.end_angle));
        this.center = e.transformOcsToWcs(this.center);

        Extrusion newE = new Extrusion();
        newE.setX(0);
        newE.setY(0);
        newE.setZ(1);

        transformedStart = newE.wcsToOcs(transformedStart);
        transformedEnd = newE.wcsToOcs(transformedEnd);
        this.start_angle = getAngleAtPoint(transformedStart);
        this.end_angle = getAngleAtPoint(transformedEnd);

        // @ToDo is this the general criterium?
        if (e.getZ() < 0) {
            counterclockwise = !counterclockwise;
        }
    }

    /**
     *
     * @param p Point in OCS, should be on the arc
     * @return angle of p in degree
     */
    public double getAngleAtPoint(Point3D p) {
        double angle = Math.toDegrees(Math.acos(p.getX() / radius));
        if (p.getY() >= 0) {
            return angle;
        }
        return -angle;
    }
}
