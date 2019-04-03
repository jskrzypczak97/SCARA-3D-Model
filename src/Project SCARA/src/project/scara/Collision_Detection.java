/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.scara;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import java.util.Enumeration;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;

import javax.media.j3d.WakeupOnCollisionEntry;
import javax.media.j3d.WakeupOnCollisionExit;
import javax.vecmath.Point3d;


/**
 *
 * @author jskrz
 */

    
import com.sun.j3d.utils.geometry.Box;
import java.util.Enumeration;
import javax.media.j3d.*;

public class Collision_Detection extends Behavior{
    public boolean iscollision = false;
    public WakeupOnCollisionEntry collisionEnter;
    public WakeupOnCollisionExit collisionExit;
    Box box;
    
    public Collision_Detection(Box shape){
        box = shape;
    }

    @Override
    public void initialize() {
        collisionEnter = new WakeupOnCollisionEntry(box, WakeupOnCollisionEntry.USE_GEOMETRY );
        collisionExit = new WakeupOnCollisionExit(box, WakeupOnCollisionEntry.USE_GEOMETRY);;
        wakeupOn(collisionEnter);
    }

    @Override
    public void processStimulus(Enumeration enmrtn) {          
        iscollision = !iscollision;
        if (iscollision){
            wakeupOn(collisionExit);
            System.out.println("IN");
            //Robot.playSound("C:\\Users\\kasia\\Documents\\NetBeansProjects\\cylindricalArm_1\\bip.wav");
        }
        else{
            wakeupOn(collisionEnter);
            System.out.println("OUT");
            //Robot.playSound("C:\\Users\\kasia\\Documents\\NetBeansProjects\\cylindricalArm_1\\hop.wav");
        }
    } 
}



