/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.scara;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.vecmath.Vector3f;
import java.util.Timer;
import java.util.TimerTask;
import javax.media.j3d.DirectionalLight;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.vecmath.Point3d;
import javax.vecmath.Color3f;
import javax.media.j3d.Material;

/**
 *
 * @author jskrz
 */


public class ProjectSCARA extends JFrame implements ActionListener, KeyListener{

    private SimpleUniverse universe;
    private Canvas3D canvas;
    
    public enum Robot_Arms
    {
        ARM_3_UP(0),
        ARM_3_DOWN(1),
        ARM_1_LEFT(2),
        ARM_1_RIGHT(3),
        ARM_2_LEFT(4),
        ARM_2_RIGHT(5),
        ARM_GRASPER_GRIP(6),
        ARM_GRASPER_DROP(7);
        
        private final int ID;
        
        Robot_Arms(int ID)
        {
            this.ID = ID;
        }
    }
    private boolean Keys[] = new boolean[6];
    
    private TransformGroup tg_main, tg_floor, tg_base, tg_rotor1, tg_rotor2, tg_arm3, tg_grasper, tg_object, tg_object_gripped;
    private Transform3D rotor1_pos, rotor2_pos, arm3_pos, rotor1_ref, rotor2_ref, object_pos, object_gripped_pos, viewer;

    // MOVEMENT OF ARMS
    private float arm3_position = -0.1f;                                                                                            // Y-AXIS VALUE IN VECTOR, CURRENT HEIGHT OF 3RD ARM
    private static float arm3_step = 0.004f;
    private float arm2_angle = 0.0f;
    private static final float arm2_step = 0.1f;
    private boolean arm2_rot_left_stop = false;
    private boolean arm2_rot_right_stop = false;
    private float arm1_angle = 0.0f;
    private static final float arm1_step = 0.05f;
    private boolean arm1_rot_left_stop = false;
    private boolean arm1_rot_right_stop = false;
    private static float Arm1_block = (float)(Math.PI-0.87f);                                                                       // 130.0 deg
    private static float Arm2_block = (float)(Math.PI-0.58f);                                                                       // 146.6 deg
    private static float Arm3_block = 0.16f;
    
    // RECORDING TRAJECTORY
    private List<Save_Moves> Move = new ArrayList<>();
    private Timer timer;
    private int list_iterator = 0;
    private int ARM;
    private float START;
    private float STOP;
    private final static boolean start_move = true;
    private final static boolean stop_move = false;
    private float start_ang_rotor1;
    private float start_ang_rotor2;
    private boolean record = false;
    private boolean is_playing = false;
    private boolean record_when_gripped = false;
    private boolean got_data = false;
    private boolean ready_to_be_set = false;

    // INTERFACE
    ButtonGroup button_group_arms = new ButtonGroup();
    JButton reset = new JButton("RESET");
    JRadioButton save_tra = new JRadioButton("SAVE TRAJECTORY");
    JButton play_tra = new JButton("PLAY TRAJECTORY");
    JButton set_view = new JButton("SET DEFAULT VIEW");
    JTextField text = new JTextField(3);
    JRadioButton arm1_rb = new JRadioButton("ARM 1");
    JRadioButton arm2_rb = new JRadioButton("ARM 2");
    JRadioButton arm3_rb = new JRadioButton("ARM 3");
    
    // COLLISIONS
    Box object;
    Box object_gripped;
    Cylinder grasper;
    Collision_Detection collision;
    private int last_move;
    private boolean is_in = false;
    private boolean is_gripped = false;
    private float height_when_dropped;
    
    // POSITIONING
    private Vector3f arm1_default_vector = new Vector3f(0.0f,0.25f,0.0f);
    private Vector3f arm2_default_vector = new Vector3f(0.0f,0.10f,0.0f);
    private Vector3f arm3_default_vector = new Vector3f(0.0f,-0.1f,0.0f);
    private Vector3f object_default_vector = new Vector3f(0.7f,0.08f,0.3f);
    private Vector3f object_temporary_vector = new Vector3f(-100.0f,0.0f,0.0f);
    private Vector3f object_gripped_default_vector = new Vector3f(100.0f,0.0f,0.0f);
    
    private Vector3f start_rotor1 = new Vector3f();
    private Vector3f start_rotor2 = new Vector3f();
    private Vector3f start_arm3 = new Vector3f();
    
    private Vector3f object_starting_position = new Vector3f();
    
    Appearance app_objects = new Appearance();

    ProjectSCARA(BranchGroup scene)
    {
        super("SCARA Robot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // CREATING VIRTUAL ENVIRONMENT
        GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
        
        canvas = new Canvas3D(config);
        canvas.setPreferredSize(new Dimension(1000,800));
        
        universe = new SimpleUniverse(canvas);
        
        viewer = new Transform3D();
        viewer.set(new Vector3f(0.0f,0.5f,4.0f));
        
        universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewer);
        
        //BoundingSphere bounds = new BoundingSphere();
        //BranchGroup scene = Create_scene(bounds);
        
        scene.setCapability(BranchGroup.ALLOW_DETACH);
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
        scene.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        scene.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        scene.setCapability(BranchGroup.ALLOW_BOUNDS_WRITE);
        
        scene.compile();
        
        add(canvas);
        pack();
        setVisible(true);
        
        // UPPER INTERFACE
        reset.addActionListener(this);
        save_tra.addActionListener(this);
        play_tra.addActionListener(this);
        set_view.addActionListener(this);
        text.addActionListener(this);
        
        arm1_rb.addActionListener(this);
        arm2_rb.addActionListener(this);
        arm3_rb.addActionListener(this);

        button_group_arms.add(arm1_rb);
        button_group_arms.add(arm2_rb);
        button_group_arms.add(arm3_rb);
       
        Panel panel = new Panel();
     
        panel.add(arm1_rb);
        panel.add(arm2_rb);
        panel.add(arm3_rb);

        panel.add(text);
        panel.add(save_tra);
        panel.add(play_tra);
        panel.add(set_view);
        panel.add(reset);

        add("North", panel);
        play_tra.setEnabled(false);
        text.setHorizontalAlignment(JTextField.RIGHT);
 
        // MOUSE MOVING
        OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ROTATE);
        orbit.setSchedulingBounds(new BoundingSphere());
        
        
        //orbit.setRotYFactor(0);
        orbit.setRotXFactor(0.5f);

        universe.getViewingPlatform().setViewPlatformBehavior(orbit);       
        universe.getCanvas().addKeyListener(this);

        universe.addBranchGraph(scene);
    }

    private BranchGroup Create_scene(BoundingSphere bounds) 
    {
        BranchGroup bg_main = new BranchGroup();
        
        tg_main = new TransformGroup();
        tg_main.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
                 
         Material mat_armsandbase = new Material(new Color3f(0.1f, 0.1f, 0.1f), new Color3f(0.1f, 0.1f, 0.1f), new Color3f(0.7f, 0.7f,0.7f), new Color3f(0.1f, 0.3f, 0.3f), 100.0f);
         Material mat_floor = new Material(new Color3f(0.2f, 0.8f, 0.1f), new Color3f(0.6f, 0.6f, 0.6f), new Color3f(0.3f, 0.3f,0.8f), new Color3f(0.8f, 0.8f, 0.7f), 60.0f);
         Material mat_objects = new Material(new Color3f(0.8f, 0.85f,0.9f), new Color3f(0.6f,0.0f,0.0f),new Color3f(0.6f, 0.0f, 0.0f), new Color3f(0.6f, 0.0f, 0.0f), 100.0f);

         Appearance app_armsandbase = new Appearance();
         Appearance app_floor = new Appearance();
         app_objects = new Appearance();

         app_armsandbase.setMaterial(mat_armsandbase);
         app_floor.setMaterial(mat_floor);
         app_objects.setMaterial(mat_objects);

        //DIRECTIONAL LIGHT
        BoundingSphere light_bs = new BoundingSphere(new Point3d(1.0d,1.0d,1.0d), 10.0d);
        Color3f light_color = new Color3f(1.0f,1.0f,1.0f);
        Vector3f light_direction = new Vector3f(4.0f,-7.0f,-12.0f);
        DirectionalLight light = new DirectionalLight(light_color, light_direction);
        light.setInfluencingBounds(light_bs);
        bg_main.addChild(light);
        
        // BUILDING FLOOR
        Cylinder floor = new Cylinder(1.3f,0.00001f,app_floor); 
        Transform3D floor_pos = new Transform3D();
        floor_pos.set(new Vector3f(0.0f,0.0f,0.0f));
        
        floor.setCollidable(false);
        
        tg_floor = new TransformGroup(floor_pos);
        tg_floor.addChild(floor);
        
        
        // BUILDING BASE
        Cylinder base = new Cylinder(0.1f,0.4f,app_armsandbase); 
        Transform3D base_pos = new Transform3D();
        base_pos.set(new Vector3f(0.0f,0.2f,0.0f));
        
        tg_base = new TransformGroup(base_pos);
        tg_base.addChild(base);
        
        
        Box foot = new Box(0.15f, 0.02f, 0.15f, app_armsandbase);
        Transform3D foot_pos = new Transform3D();
        foot_pos.set(new Vector3f(0.0f,-0.18f,0.0f));
        
        
        TransformGroup tg_foot = new TransformGroup(foot_pos);
        tg_foot.setCollidable(false);
        tg_foot.addChild(foot);
        
        
        tg_base.addChild(tg_foot);

        
        // BUILDING ARM 1
        Cylinder rotor1 = new Cylinder(0.1f,0.1f,app_armsandbase); 
        rotor1_pos = new Transform3D();
        rotor1_ref = new Transform3D();
        rotor1_pos.set(arm1_default_vector);
        
        tg_rotor1 = new TransformGroup(rotor1_pos);
        tg_rotor1.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_rotor1.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_rotor1.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_rotor1.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_rotor1.addChild(rotor1);
        
        
        Box arm1 = new Box(0.19f, 0.05f, 0.095f, app_armsandbase);
        Transform3D arm1_pos = new Transform3D();
        arm1_pos.set(new Vector3f(0.2f,0.0f,0.0f));
        
        TransformGroup tg_arm1 = new TransformGroup(arm1_pos);
        tg_arm1.addChild(arm1);
        
        
        Cylinder arm1_rounding = new Cylinder(0.1f,0.1f,app_armsandbase); 
        Transform3D arm1_rounding_pos = new Transform3D();
        arm1_rounding_pos.set(new Vector3f(0.4f,0.0f,0.0f));
        
        TransformGroup tg_arm1_rounding = new TransformGroup(arm1_rounding_pos);
        tg_arm1_rounding.addChild(arm1_rounding);
        
        
        tg_rotor1.addChild(tg_arm1_rounding);
        tg_rotor1.addChild(tg_arm1);
        
        // BUILDING ARM 2
        Cylinder rotor2 = new Cylinder(0.1f,0.1f,app_armsandbase); 
        rotor2_pos = new Transform3D();
        rotor2_ref = new Transform3D();
        rotor2_pos.set(arm2_default_vector);
        
        tg_rotor2 = new TransformGroup(rotor2_pos);
        tg_rotor2.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_rotor2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_rotor2.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_rotor2.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_rotor2.addChild(rotor2);
        
        
        Box arm2 = new Box(0.19f, 0.05f, 0.095f, app_armsandbase);
        Transform3D arm2_pos = new Transform3D();
        arm2_pos.set(new Vector3f(0.2f,0.0f,0.0f));
        
        TransformGroup tg_arm2 = new TransformGroup(arm2_pos);
        tg_arm2.addChild(arm2);
        
        
        Cylinder arm2_rounding = new Cylinder(0.1f,0.1f,app_armsandbase); 
        Transform3D arm2_rounding_pos = new Transform3D();
        arm2_rounding_pos.set(new Vector3f(0.4f,0.0f,0.0f));
        
        TransformGroup tg_arm2_rounding = new TransformGroup(arm2_rounding_pos);
        tg_arm2_rounding.addChild(arm2_rounding);
        
        
        tg_rotor2.addChild(tg_arm2_rounding);
        tg_rotor2.addChild(tg_arm2);
        
        // BUILDING ARM 3
        Cylinder arm3 = new Cylinder(0.02f,0.6f,app_armsandbase); 
        arm3_pos = new Transform3D();
        arm3_pos.set(arm3_default_vector);
        
        tg_arm3 = new TransformGroup(arm3_pos);
        tg_arm3.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_arm3.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_arm3.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_arm3.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_arm3.addChild(arm3);
        
        
        grasper = new Cylinder(0.05f,0.03f,app_armsandbase); 
        Transform3D grasper_pos = new Transform3D();
        grasper_pos.set(new Vector3f(0.0f,-0.3f,0.0f));
        
        tg_grasper = new TransformGroup(grasper_pos);
        tg_grasper.addChild(grasper);
        
        
        tg_arm3.addChild(tg_grasper);
        
        
        // CONNECTIONS
        tg_arm2_rounding.addChild(tg_arm3); // 5th
        tg_arm1_rounding.addChild(tg_rotor2);// 4th
        tg_base.addChild(tg_rotor1); // 3rd
        tg_floor.addChild(tg_base); // 2nd
        tg_main.addChild(tg_floor); // 1st
     
        
        // ADDING STATIC OBJECT
        object = new Box(0.08f, 0.08f, 0.08f, app_objects);  
        object_pos = new Transform3D();
        object_pos.set(object_default_vector);
        
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

        
        tg_floor.addChild(tg_object);
        
        // ADDING MOVEABLE OBJECT
        object_gripped = new Box(0.08f, 0.08f, 0.08f, app_objects);
        object_gripped_pos = new Transform3D();
        object_gripped_pos.set(object_gripped_default_vector);
        
        object_gripped.setCapability(TransformGroup.ALLOW_COLLIDABLE_READ);
        object_gripped.setCapability(TransformGroup.ALLOW_COLLIDABLE_WRITE);
        object_gripped.setCapability(Box.ENABLE_APPEARANCE_MODIFY);
        
        tg_object_gripped = new TransformGroup(object_gripped_pos);
        tg_object_gripped.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        tg_object_gripped.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        tg_object_gripped.setCapability(TransformGroup.ALLOW_CHILDREN_READ);
        tg_object_gripped.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
        tg_object_gripped.addChild(object_gripped);

        
        tg_grasper.addChild(tg_object_gripped);
        
        
        // SETTING COLLISION DETECTOR TO OBJECT
        collision = new Collision_Detection(object);
        collision.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0));
        bg_main.addChild(collision);
        
        
        bg_main.addChild(tg_main);

        return bg_main;
    }
    
    @Override
        public void keyPressed(KeyEvent e)
        {
            Move(e.getKeyCode(),true);
        }
        
    @Override
        public void keyTyped(KeyEvent e) 
        {
   
        }

    @Override
        public void keyReleased(KeyEvent e) {
            Move(e.getKeyCode(), false);
            if(ready_to_be_set) play_tra.setEnabled(record);
        }
        
    private void Move(int key, boolean state)
    {
        switch (key) {
            case KeyEvent.VK_UP:
            {
                Check_Collision(Robot_Arms.ARM_3_UP.ID);
                Keys[Robot_Arms.ARM_3_UP.ID] = state;

                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_3_UP.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_3_UP.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_3(Robot_Arms.ARM_3_UP.ID);   
            }
            break;
            case KeyEvent.VK_DOWN:
            {
                Check_Collision(Robot_Arms.ARM_3_DOWN.ID);
                Keys[Robot_Arms.ARM_3_DOWN.ID] = state;
                
                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_3_DOWN.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_3_DOWN.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_3(Robot_Arms.ARM_3_DOWN.ID);
            }
            break;
            case KeyEvent.VK_LEFT:
            {
                Check_Collision(Robot_Arms.ARM_1_LEFT.ID);
                Keys[Robot_Arms.ARM_1_LEFT.ID] = state;
                
                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_1_LEFT.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_1_LEFT.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_1(Robot_Arms.ARM_1_LEFT.ID);
            }
            break;
            case KeyEvent.VK_RIGHT:
            {
                Check_Collision(Robot_Arms.ARM_1_RIGHT.ID);
                Keys[Robot_Arms.ARM_1_RIGHT.ID] = state;

                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_1_RIGHT.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_1_RIGHT.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_1(Robot_Arms.ARM_1_RIGHT.ID);
            }
            break;
            case KeyEvent.VK_Z:
            {
                Check_Collision(Robot_Arms.ARM_2_LEFT.ID);
                Keys[Robot_Arms.ARM_2_LEFT.ID] = state;

                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_2_LEFT.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_2_LEFT.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_2(Robot_Arms.ARM_2_LEFT.ID);
            }
            break;
            case KeyEvent.VK_X:
            {
                Check_Collision(Robot_Arms.ARM_2_RIGHT.ID);
                Keys[Robot_Arms.ARM_2_RIGHT.ID] = state;

                if(record && state && !got_data)
                {
                    Get_Move_Value(Robot_Arms.ARM_2_RIGHT.ID, start_move);
                    got_data = true;
                }
                else if(record && !state)
                {
                    Get_Move_Value(Robot_Arms.ARM_2_RIGHT.ID, stop_move);
                    got_data = false;
                }
                
                if(state && !is_playing) Move_ARM_2(Robot_Arms.ARM_2_RIGHT.ID);
            }
            break;
            case KeyEvent.VK_SPACE:
            {
                if(record)
                {
                    Move_Object(true);
                }
                if(last_move == Robot_Arms.ARM_3_DOWN.ID && !is_gripped && !is_playing)
                {
                    object_gripped_pos.setTranslation(new Vector3f(0.0f,-0.095f,0.0f));
                    tg_object_gripped.setTransform(object_gripped_pos);
                    
                    object_pos.setTranslation(object_temporary_vector);
                    tg_object.setTransform(object_pos);  
                    is_gripped = true;
                }
            }
            break;
            case KeyEvent.VK_CONTROL:
            {
                if(record)
                {
                    Move_Object(false);
                }
                if(is_gripped && !is_playing)
                {
                    Vector3f vector = new Vector3f();
                    object_gripped.getLocalToVworld(object_gripped_pos);
                    tg_object.setTransform(object_gripped_pos);
                    
                    object_pos = object_gripped_pos;
 
                    object_pos.get(vector);

                    object_gripped_pos.set(object_gripped_default_vector);
                    tg_object_gripped.setTransform(object_gripped_pos);
                    
                    is_gripped = false;
                    is_in = true;
                    
                    Gravity(vector);
                }
            }
            break;
        }   
    }
    
    private void Move_ARM_1(int key)
    {
        if(key == Robot_Arms.ARM_1_LEFT.ID && arm1_angle < Arm1_block && !arm1_rot_left_stop && last_move != Robot_Arms.ARM_1_LEFT.ID && last_move != Robot_Arms.ARM_2_LEFT.ID) // +130deg
        {
            if(arm1_rot_right_stop) arm1_rot_right_stop = !arm1_rot_right_stop;

            arm1_angle += arm1_step;
            Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);

            if(arm1_angle >= Arm1_block) arm1_rot_left_stop = true ;
        }
        else if(key == Robot_Arms.ARM_1_RIGHT.ID && arm1_angle > -Arm1_block && !arm1_rot_right_stop && last_move != Robot_Arms.ARM_1_RIGHT.ID && last_move != Robot_Arms.ARM_2_RIGHT.ID) // -130deg
        {
            if(arm1_rot_left_stop) arm1_rot_left_stop = !arm1_rot_left_stop;
            
            arm1_angle -= arm1_step;
            Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
            
            if(arm1_angle >= Arm1_block) arm1_rot_right_stop = true ;
        }
    }
    
    private void Move_ARM_2(int key)
    {
        if(key == Robot_Arms.ARM_2_LEFT.ID && arm2_angle < Arm2_block && !arm2_rot_left_stop && last_move != Robot_Arms.ARM_2_LEFT.ID && last_move != Robot_Arms.ARM_1_LEFT.ID) // +146.6deg
        {
            if(arm2_rot_right_stop) arm2_rot_right_stop = !arm2_rot_right_stop;
            
            arm2_angle += arm2_step;
            Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);

            if(arm2_angle >= Arm2_block) arm2_rot_left_stop = true ;
        }
        else if(key == Robot_Arms.ARM_2_RIGHT.ID && arm2_angle > -Arm2_block && !arm2_rot_right_stop && last_move != Robot_Arms.ARM_2_RIGHT.ID && last_move != Robot_Arms.ARM_1_RIGHT.ID) // -146.6deg
        {
            if(arm2_rot_left_stop) arm2_rot_left_stop = !arm2_rot_left_stop;
            
            arm2_angle -= arm2_step;
            Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);

            if(arm2_angle <= -Arm2_block) arm2_rot_right_stop = true ;
        }
    }
    
    private void Move_ARM_3(int key)
    {
        if((key == Robot_Arms.ARM_3_UP.ID && arm3_position <= Arm3_block))
        {
            arm3_position += arm3_step;
            Slide(arm3_pos,arm3_position,tg_arm3);
        }
        else if(key == Robot_Arms.ARM_3_DOWN.ID && ((arm3_position >= -Arm3_block) && !is_gripped || (arm3_position >= -0.066f) && is_gripped) && last_move != Robot_Arms.ARM_3_DOWN.ID)
        {
            arm3_position -= arm3_step;
            Slide(arm3_pos,arm3_position,tg_arm3);
        }
    }
    
    private void Move_Object(boolean lift) 
    {
        Save_Moves bufMove = new Save_Moves();
        Move.add(bufMove);
        int index = Move.size()-1;
        
        Move.get(index).arm_used = lift ? Robot_Arms.ARM_GRASPER_GRIP.ID : Robot_Arms.ARM_GRASPER_DROP.ID;
        
        Move.get(index).start_val = 0;
        Move.get(index).stop_val = 0;
    }
    
    private void Slide(Transform3D position, float value, TransformGroup tg)
    {
        position.set(new Vector3f(0.0f,value,0.0f));
        tg.setTransform(position);
    }
    
    private void Slide(Transform3D position, Vector3f vector, TransformGroup tg)
    {
        position.set(vector);
        tg.setTransform(position);
    }

    private void Spin(float angle, Transform3D position, Transform3D reference, TransformGroup tg, Vector3f vector)
    {
        position.rotY(angle);
        reference.set(vector);
        position.mul(reference);
        tg.setTransform(position);
    }
    
    // INITIALIZE MOVE LIST
    private void Get_Move_Value(int Arm, boolean start_stop) 
    {
        if(start_stop)
        {
            Save_Moves bufMove = new Save_Moves();
            Move.add(bufMove);
            
            int index = Move.size()-1;
            Move.get(index).arm_used = Arm;
            switch(Arm)
            {
                case 0:
                case 1:
                {
                    Move.get(index).start_val = arm3_position;
                }
                break;
                case 2:
                case 3:
                {
                    Move.get(index).start_val = arm1_angle;
                }
                break;
                case 4:
                case 5:
                {
                    Move.get(index).start_val = arm2_angle;
                } 
                break;
            }
        }
        else
        {
            int index = Move.size()-1;
            switch(Arm)
            {
                case 0:
                case 1:
                {
                    Move.get(index).stop_val = arm3_position;
                }
                break;
                case 2:
                case 3:
                {
                    Move.get(index).stop_val = arm1_angle;
                }
                break;
                case 4:
                case 5:
                {
                    Move.get(index).stop_val = arm2_angle;
                }
                break;
            }
        }
    }
    
    private void Play_Trajectory() 
    {       
        record = false;
        is_playing = true;
        list_iterator = 0;
        
        play_tra.setEnabled(false);
        save_tra.setEnabled(false);
        
        ARM = Move.get(list_iterator).arm_used;
        START = Move.get(list_iterator).start_val;
        STOP = Move.get(list_iterator).stop_val;
        
        rotor1_pos.set(start_rotor1);
        Spin(start_ang_rotor1,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
        arm1_angle = start_ang_rotor1;

        rotor2_pos.set(start_rotor2);
        Spin(start_ang_rotor2,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);
        arm2_angle = start_ang_rotor2;
        
        Slide(arm3_pos,start_arm3,tg_arm3);
        arm3_position = start_arm3.y;
        
        if(!record_when_gripped) 
        {
            object_pos.set(object_starting_position);
            tg_object.setTransform(object_pos);
        }
        else
        {
            object_pos.setTranslation(object_temporary_vector);
            tg_object.setTransform(object_pos);  
            
            object_gripped_pos.set(object_starting_position);
            tg_object_gripped.setTransform(object_gripped_pos);
        }
        Animation(ARM,STOP,true);  
    }
    
    private void Animation(int Arm, float stop, boolean long_animation)
    {        
        ARM = Arm;
        STOP = stop;
        
        class Task extends TimerTask
        {
            @Override
            public void run() {
                if(!long_animation) 
                {
                   if(collision.iscollision) 
                   {
                       timer.cancel();
                       Check_Collision(Robot_Arms.ARM_3_DOWN.ID);
                   }
                }
                switch(ARM)
                {
                    case 0:
                    {
                        arm3_position += arm3_step/2;
                        Slide(arm3_pos,arm3_position,tg_arm3);
                        if(arm3_position >= STOP)
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 1:
                    {
                        arm3_position -= arm3_step/2;
                        Slide(arm3_pos,arm3_position,tg_arm3);
                        if(arm3_position <= STOP) 
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 2:
                    {
                        arm1_angle += arm1_step/3;
                        Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
                        if(arm1_angle >= STOP)
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 3:
                    {
                        arm1_angle -= arm1_step/3;
                        Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
                        if(arm1_angle <= STOP)
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 4:
                    {
                        arm2_angle += arm2_step/3;
                        Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);
                        if(arm2_angle >= STOP)
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 5:
                    {
                        arm2_angle -= arm2_step/3;
                        Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);
                        if(arm2_angle <= STOP)
                        {
                            if(long_animation) Change();
                            else timer.cancel();
                        }
                    }
                    break;
                    case 6:
                    {
                        object_gripped_pos.setTranslation(new Vector3f(0.0f,-0.095f,0.0f));
                        tg_object_gripped.setTransform(object_gripped_pos);
                    
                        object_pos.setTranslation(object_temporary_vector);
                        tg_object.setTransform(object_pos);  
                        is_gripped = true; 
                        
                        list_iterator++;
                        if(long_animation) Change();
                        else timer.cancel();
                    }
                    break;
                    case 7:
                    {
                        Vector3f vector = new Vector3f();
                        
                        object_gripped.getLocalToVworld(object_gripped_pos);
                        tg_object.setTransform(object_gripped_pos);

                        object_pos = object_gripped_pos;
                        object_pos.get(vector);

                        object_gripped_pos.set(object_gripped_default_vector);
                        tg_object_gripped.setTransform(object_gripped_pos);
                    
                        is_gripped = false;
                        is_in = true;
                    
                        Gravity(vector);
                            
                        list_iterator++;
                        if(long_animation) Change();
                        else timer.cancel();
                    }
                    break;
                }
            }   
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new Task(), 500, 20);
    }
    
    private void Change()
        {
            if(list_iterator >= Move.size()-1)
            {
                timer.cancel();
                list_iterator = 0;
                is_playing = false;
                                
                play_tra.setEnabled(true);
                save_tra.setEnabled(true);
            }
            else
            {
                ARM = Move.get(list_iterator+1).arm_used;
                START = Move.get(list_iterator+1).start_val;
                STOP = Move.get(list_iterator+1).stop_val;
            
                switch(ARM)
                    {
                        case 0:
                        {
                            arm3_position = START;
                            Slide(arm3_pos,arm3_position,tg_arm3);
                        }
                        break;
                        case 1:
                        {
                            arm3_position = START;
                            Slide(arm3_pos,arm3_position,tg_arm3);
                        }
                        break;
                        case 2:
                        {
                            arm1_angle = START;
                            Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
                        }
                        break;
                        case 3:
                        {
                            arm1_angle = START;
                            Spin(arm1_angle,rotor1_pos,rotor1_ref,tg_rotor1,arm1_default_vector);
                        }
                        break;
                        case 4:
                        {
                            arm2_angle = START;
                            Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);
                        }
                        break;
                        case 5:
                        {
                            arm2_angle = START;
                            Spin(arm2_angle,rotor2_pos,rotor2_ref,tg_rotor2,arm2_default_vector);
                        }
                        break;
                    }
            }
            list_iterator++;
        }
    
    private void Check_Collision(int Arm)
    {
        if(collision.iscollision && !is_in)
        {
            is_in = true;
            last_move = Arm;
        }
        
        if(!collision.iscollision && is_in)
        {
            is_in = false;
            last_move = -1;
        }
    }
     
    private void Gravity(Vector3f vector)
    {
        Timer gravity_timer = new Timer();
        height_when_dropped = vector.y;

     class Task extends TimerTask
        {
            @Override
            public void run() 
            {
                //height_when_dropped = height_when_dropped - 0.008f;
                height_when_dropped = 0.1f;
                
                object_pos.set(new Vector3f(vector.x, height_when_dropped, vector.z));
                tg_object.setTransform(object_pos);
                
                if(height_when_dropped <= 0.1f) 
                {
                    gravity_timer.cancel();
                }
            }
        }
        gravity_timer.scheduleAtFixedRate(new Task(), 0, 10);   
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == reset) Reset();
        else if(e.getSource() == save_tra)
        {
            ready_to_be_set = true;
            if(save_tra.isSelected()) 
            {
                Move.clear();                                                                                       // DELETING LAST SAVED SEQUENCE
                start_ang_rotor1 = arm1_angle;
                start_ang_rotor2 = arm2_angle;
                arm3_pos.get(start_arm3);
                if(!is_gripped) 
                {
                    object_pos.get(object_starting_position);
                    record_when_gripped = false;
                }
                else 
                {
                    object_gripped_pos.get(object_starting_position);
                    record_when_gripped = true;
                }
                record = true;
            }
            else record = false;
        }
        else if(e.getSource() == play_tra) 
        {
            save_tra.setSelected(false);
            Play_Trajectory();
        }
        else if(e.getSource() == set_view)
        {
            viewer.set(new Vector3f(0.0f,0.5f,4.0f));
            universe.getViewingPlatform().getViewPlatformTransform().setTransform(viewer);
        }
        else if(e.getSource() == arm1_rb || e.getSource() == arm2_rb || e.getSource() == arm3_rb)
        {
            Radio_buttons_control(e.getSource());
        }
    }
    
    private void Radio_buttons_control(Object rbutton)
    {
        boolean success = false;
        String get_text = text.getText();
        float value = 0;
        try
        {
            value = Float.parseFloat(get_text);
            success = true;
        }
        catch(NumberFormatException ex)
        {
            text.setText("ERR");
        }
        if(success)
        {
            if(rbutton == arm1_rb)
            {
                value = (float)Math.toRadians((float)value);
                if(value >= Arm1_block) Animation(Robot_Arms.ARM_1_LEFT.ID,Arm1_block,false);
                else if(value <= -Arm1_block) Animation(Robot_Arms.ARM_1_RIGHT.ID,-Arm1_block,false);
                else if(arm1_angle > value) Animation(Robot_Arms.ARM_1_RIGHT.ID,value,false);
                else Animation(Robot_Arms.ARM_1_LEFT.ID,value,false);
            }
            else if(rbutton == arm2_rb)
            {
                value = (float)Math.toRadians((float)value);
                if(value >= Arm2_block) Animation(Robot_Arms.ARM_2_LEFT.ID,Arm2_block,false);
                else if(value <= -Arm2_block) Animation(Robot_Arms.ARM_2_RIGHT.ID,-Arm2_block,false);
                else if(arm1_angle > value) Animation(Robot_Arms.ARM_2_RIGHT.ID,value,false);
                else Animation(Robot_Arms.ARM_2_LEFT.ID,value,false);   
            }
            else
            {
                value = Arm1_block*value;
                if(value >= Arm1_block) Animation(Robot_Arms.ARM_3_UP.ID,Arm1_block,false);
                else if(value <= -Arm1_block) Animation(Robot_Arms.ARM_3_DOWN.ID,-Arm1_block,false);
                else if(arm3_position > value) Animation(Robot_Arms.ARM_3_DOWN.ID,value,false);
                else Animation(Robot_Arms.ARM_3_UP.ID,value,false);
            }
            text.setText(" ");
        }
        button_group_arms.clearSelection();
    }

    private void Reset() 
    {
        Move.clear();
        
        play_tra.setEnabled(false);
        save_tra.setEnabled(true);
        save_tra.setSelected(false);
        text.setText("");
        ready_to_be_set = false;

        arm1_angle = 0.0f;
        arm2_angle = 0.0f;
        arm3_position = -0.1f;
        
        record = false;
        is_playing = false;
        record_when_gripped = false;
        list_iterator = 0;

        rotor1_pos.set(arm1_default_vector);
        tg_rotor1.setTransform(rotor1_pos);

        rotor2_pos.set(arm2_default_vector);
        tg_rotor2.setTransform(rotor2_pos);

        arm3_pos.set(arm3_default_vector);
        tg_arm3.setTransform(arm3_pos);
        
        object_gripped_pos.set(object_gripped_default_vector);
        tg_object_gripped.setTransform(object_gripped_pos);
        
        object_pos.set(object_default_vector);
        tg_object.setTransform(object_pos);
        
        try
        {
            timer.cancel();
        }
        catch(NullPointerException ex)
        {
            System.err.println("");
        }
        
        object_pos.set(object_default_vector);
        tg_object.setTransform(object_pos);
        is_gripped = false;
        
    }    

    public static void main(String[] args)
    {
        Robot RobotSCARA = new Robot();
        ProjectSCARA World = new ProjectSCARA(RobotSCARA.BG_main);
    }
}