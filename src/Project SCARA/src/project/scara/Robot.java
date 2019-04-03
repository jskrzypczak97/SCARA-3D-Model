/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.scara;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

/**
 *
 * @author jskrz
 */
public class Robot 
{
    public BranchGroup BG_main;
    
    public TransformGroup tg_floor, tg_foot, tg_base, tg_rotor1, tg_rotor2, tg_arm3, tg_grasper, tg_object, tg_object_gripped;
    public Transform3D rotor1_pos, rotor1_ref, rotor2_pos, rotor2_ref, arm3_pos, object_pos, object_gripped_pos;
    public Cylinder grasper;
    public Box object, object_gripped;
    public Collision_Detection collision;
    
    private final Vector3f arm1_default_vector = new Vector3f(0.0f,0.25f,0.0f);
    private final Vector3f arm2_default_vector = new Vector3f(0.0f,0.10f,0.0f);
    private final Vector3f arm3_default_vector = new Vector3f(0.0f,-0.1f,0.0f);
    private final Vector3f object_default_vector = new Vector3f(0.7f,0.08f,0.3f);
    private final Vector3f object_temporary_vector = new Vector3f(-100.0f,0.0f,0.0f);
    private final Vector3f object_gripped_default_vector = new Vector3f(100.0f,0.0f,0.0f);
    
    Robot()
    {
        BG_main = new BranchGroup();
        
        TransformGroup tg_main = new TransformGroup();
        tg_main.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        //DIRECTIONAL LIGHT
        BG_main.addChild(Create_light(new Color3f(1.0f,1.0f,1.0f), new Vector3f(4.0f,-7.0f,-12.0f)));
        
        // BUILDING FLOOR
        Material mat_floor = new Material(new Color3f(0.2f, 0.8f, 0.1f), new Color3f(0.6f, 0.6f, 0.6f), new Color3f(0.3f, 0.3f,0.8f), new Color3f(0.8f, 0.8f, 0.7f), 60.0f);
        Cylinder floor = new Cylinder(1.3f,0.00001f, Make_appearance(mat_floor)); 
        Transform3D floor_pos = new Transform3D();
        floor_pos.set(new Vector3f(0.0f,0.0f,0.0f));
        
        floor.setCollidable(false);
        
        tg_floor = new TransformGroup(floor_pos);
        tg_floor.addChild(floor);
        
        // BUILDING BASE AND FOOT
        Material mat_armsandbase = new Material(new Color3f(0.1f, 0.1f, 0.1f), new Color3f(0.1f, 0.1f, 0.1f), new Color3f(0.7f, 0.7f,0.7f), new Color3f(0.1f, 0.3f, 0.3f), 100.0f);
        Cylinder base = new Cylinder(0.1f,0.4f,Make_appearance(mat_armsandbase)); 
        Transform3D base_pos = new Transform3D();
        base_pos.set(new Vector3f(0.0f,0.2f,0.0f));
        
        tg_base = new TransformGroup(base_pos);
        tg_base.addChild(base);
        
        Box foot = new Box(0.15f, 0.02f, 0.15f, Make_appearance(mat_armsandbase));
        Transform3D foot_pos = new Transform3D();
        foot_pos.set(new Vector3f(0.0f,-0.18f,0.0f));

        tg_foot = new TransformGroup(foot_pos);
        tg_foot.setCollidable(false);
        tg_foot.addChild(foot);
        
        
        tg_base.addChild(tg_foot);

        // BUILDING ARM 1
        Cylinder rotor1 = new Cylinder(0.1f,0.1f, Make_appearance(mat_armsandbase)); 
        Box arm1 = new Box(0.19f, 0.05f, 0.095f, Make_appearance(mat_armsandbase));
        Cylinder arm1_rounding = new Cylinder(0.1f,0.1f, Make_appearance(mat_armsandbase)); 
        
        TransformGroup tg_arm1_joint = new TransformGroup();
        tg_arm1_joint = Create_rotating_arm(rotor1, rotor1_pos, rotor1_ref, tg_rotor1, arm1, arm1_rounding, arm1_default_vector);

        // BUILDING ARM 2
        Cylinder rotor2 = new Cylinder(0.1f,0.1f, Make_appearance(mat_armsandbase)); 
        Box arm2 = new Box(0.19f, 0.05f, 0.095f, Make_appearance(mat_armsandbase));
        Cylinder arm2_rounding = new Cylinder(0.1f,0.1f, Make_appearance(mat_armsandbase)); 
        
        TransformGroup tg_arm2_joint = new TransformGroup();
        tg_arm2_joint = Create_rotating_arm(rotor2, rotor2_pos, rotor2_ref, tg_rotor2, arm2, arm2_rounding, arm2_default_vector);
        
        // BUILDING ARM 3
        Cylinder arm3 = new Cylinder(0.02f,0.6f, Make_appearance(mat_armsandbase)); 
        arm3_pos = new Transform3D();
        arm3_pos.set(arm3_default_vector);
        
        tg_arm3 = new TransformGroup(arm3_pos);
        tg_arm3.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_arm3.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_arm3.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_arm3.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_arm3.addChild(arm3);
        
        
        grasper = new Cylinder(0.05f,0.03f, Make_appearance(mat_armsandbase)); 
        Transform3D grasper_pos = new Transform3D();
        grasper_pos.set(new Vector3f(0.0f,-0.3f,0.0f));
        
        tg_grasper = new TransformGroup(grasper_pos);
        tg_grasper.addChild(grasper);
        
        
        tg_arm3.addChild(tg_grasper);
        
        
        // CONNECTIONS
        tg_arm2_joint.addChild(tg_arm3); // 5th
        tg_arm1_joint.addChild(tg_rotor2);// 4th
        tg_base.addChild(tg_rotor1); // 3rd
        tg_floor.addChild(tg_base); // 2nd
        tg_main.addChild(tg_floor); // 1st
     
        
        // ADDING OBJECT
        Material mat_objects = new Material(new Color3f(0.8f, 0.85f,0.9f), new Color3f(0.6f,0.0f,0.0f),new Color3f(0.6f, 0.0f, 0.0f), new Color3f(0.6f, 0.0f, 0.0f), 100.0f);
        
        object = new Box(0.08f, 0.08f, 0.08f, Make_appearance(mat_objects));  
        Create_object(object,tg_object,object_pos,tg_floor,object_default_vector);
        
        object_gripped = new Box(0.08f, 0.08f, 0.08f, Make_appearance(mat_objects));        
        Create_object(object_gripped, tg_object_gripped, object_gripped_pos, tg_grasper, object_gripped_default_vector);
        
        // SETTING COLLISION DETECTOR TO OBJECT
        collision = new Collision_Detection(object);
        collision.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        BG_main.addChild(collision);
        
        BG_main.addChild(tg_main);
    }

    private Appearance Make_appearance(Material mat) 
    {
        Appearance app = new Appearance();
        app.setMaterial(mat);
        return app;
    }

    private Node Create_light(Color3f light_color, Vector3f light_direction) 
    {
        BoundingSphere light_bs = new BoundingSphere(new Point3d(1.0d,1.0d,1.0d), 10.0d);
        DirectionalLight light = new DirectionalLight(light_color, light_direction);
        light.setInfluencingBounds(light_bs);
        
        return light;
    }

    private TransformGroup Create_rotating_arm(Cylinder rotor, Transform3D rotor_pos, Transform3D rotor_ref, TransformGroup tg_rotor, Box arm, Cylinder arm_rounding, Vector3f default_vector) 
    {
        rotor_pos = new Transform3D();
        rotor_ref = new Transform3D();
        rotor_pos.set(default_vector);
        
        tg_rotor = new TransformGroup(rotor_pos);
        tg_rotor.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_rotor.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_rotor.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_rotor.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_rotor.addChild(rotor);
        
        Transform3D arm_pos = new Transform3D();
        arm_pos.set(new Vector3f(0.2f,0.0f,0.0f)); //not sure
        
        TransformGroup tg_arm = new TransformGroup(arm_pos);
        tg_arm.addChild(arm);
        
        Transform3D arm_rounding_pos = new Transform3D();
        arm_rounding_pos.set(new Vector3f(0.4f,0.0f,0.0f));
        
        TransformGroup tg_arm_rounding = new TransformGroup(arm_rounding_pos);
        tg_arm_rounding.addChild(arm_rounding);
        
        tg_rotor.addChild(tg_arm_rounding);
        tg_rotor.addChild(tg_arm); 
        
        return tg_arm_rounding;
    }

    private void Create_object(Box object, TransformGroup tg_object, Transform3D object_pos, TransformGroup tg_parent, Vector3f default_vector) 
    {
        object_pos = new Transform3D();
        object_pos.set(default_vector);
        
        object.setCapability(TransformGroup.ALLOW_COLLIDABLE_READ);
        object.setCapability(TransformGroup.ALLOW_COLLIDABLE_WRITE);
        object.setCapability(Box.ENABLE_APPEARANCE_MODIFY);
        
        tg_object = new TransformGroup(object_pos);
        tg_object.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_object.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_object.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_object.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_object.setCapability(TransformGroup.ALLOW_COLLIDABLE_READ);
        tg_object.setCapability(TransformGroup.ALLOW_COLLIDABLE_WRITE);
        tg_object.addChild(object);

        tg_parent.addChild(tg_object);
    }
}
