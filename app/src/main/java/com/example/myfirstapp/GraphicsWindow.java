package com.example.myfirstapp;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

public class GraphicsWindow extends Activity
{
    /** Hold a reference to our GLSurfaceView */
    private MyGLSurfaceView mGLSurfaceView;

    private Family myFamily;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2)
        {
            // Get family object

            MyApplication myApp = (MyApplication) getApplication();
            myFamily = myApp.getFamily();

            // Create a GLSurfaceView

            mGLSurfaceView = new MyGLSurfaceView(this, myFamily);

            // Set the layout

            setContentView(R.layout.graphics_window);

            // Add the GLSurfaceView to the layout

            FrameLayout frame = (FrameLayout)findViewById(R.id.myframelayout);
            if(frame != null) frame.addView(mGLSurfaceView, 0);

        }
        else
        {
            // This is where you could create an OpenGL ES 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            return;
        }

    }


    @Override
    protected void onResume()
    {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause()
    {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLSurfaceView.onPause();
    }



    /** Toggles the animation on/off when the button is clicked */
    public void toggleAnimation(View view)
    {
        if(mGLSurfaceView.getRenderer().toggleAnimation()) {
            ((Button) view).setText(R.string.pause_animation);
        }
        else {
            ((Button) view).setText(R.string.start_animation);
        }
    }



    public void doSIPlot(View view) {
        mGLSurfaceView.getRenderer().setSI(true);
    }

    public void doSHPlot(View view) {
        mGLSurfaceView.getRenderer().setSI(false);
    }


    /** Previous state */
    public void prevState(View view)
    {
        mGLSurfaceView.getRenderer().goToPrevState();
    }

    /** Next state */
    public void nextState(View view)
    {
        mGLSurfaceView.getRenderer().goToNextState();
    }

}



class MyGLSurfaceView extends GLSurfaceView
{

    private final MyRenderer mRenderer;


    /** Constructor */
    public MyGLSurfaceView(Context context, Family fam) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView

        mRenderer = new MyRenderer(this, fam, context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }




    // Want to detect - ROTATE    (single finger drag)
    //                - ZOOM      (two finger pinch)
    //                - TRANSLATE (three finger drag)


    static final int NONE = 0;
    static final int ROT  = 1;
    static final int ZOOM = 2;
    static final int TRAN = 3;

    int mode = NONE;

    // Remember some things

    PointF start  = new PointF();
    PointF mid    = new PointF();
    float  oldDist = 1f;


    private final float TOUCH_T_SCALE_FACTOR = 0.005f;
    private final float TOUCH_R_SCALE_FACTOR = 1.6f;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        // Reset factors

        //mRenderer.setScale(1.0f);

        mRenderer.setRotX(0.0f);
        mRenderer.setRotY(0.0f);

        mRenderer.setTranX(0.0f);
        mRenderer.setTranY(0.0f);

        /** Motion events on GLSurfaceView */

        switch (e.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:

                // 1 finger down - store location and set mode to ROT

                start.set(e.getX(), e.getY());
                mode = ROT;

                break;

            case MotionEvent.ACTION_POINTER_DOWN:

                // Get number of fingers down

                int numberFingers = e.getPointerCount();

                // 2 fingers down  - store distance between two fingers and if big enough
                //                   (10pixels) store the mid-point and set mode to ZOOM

                if(numberFingers == 2) {

                    oldDist = spacing(e);

                    if(oldDist > 10.0f) {
                        midPoint(mid, e);
                        mode = ZOOM;
                    }
                }

                // 3 fingers down  - set mode to TRAN
                //

                else if (numberFingers == 3) {
                    mode = TRAN;
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:

                // 1st, 2nd or 3rd finger up - finish gesture

                mode = NONE;

                break;

            case MotionEvent.ACTION_MOVE:

                // Movement - action depends on current mode

                if(mode == ROT) {

                    mRenderer.setRotX((e.getX() - start.x) * TOUCH_R_SCALE_FACTOR);
                    mRenderer.setRotY((e.getY() - start.y) * TOUCH_R_SCALE_FACTOR);

                    start.set(e.getX(), e.getY());  // Reset location for next movement
                }
                else if(mode == ZOOM) {

                    float newDist = spacing(e);

                    if(newDist > 10.0f) {

                        float s = mRenderer.getScale();

                        mRenderer.setScale(s * (newDist / oldDist));

                        oldDist = newDist;	// Reset for next movement
                    }
                }
                else if(mode == TRAN) {

                    float scale = mRenderer.getScale();
                    float diag  = mRenderer.getModelDiag();

                    float tx = (e.getX() - start.x) * diag * TOUCH_T_SCALE_FACTOR / scale;
                    float ty = (e.getY() - start.y) * diag * TOUCH_T_SCALE_FACTOR / scale;

                    mRenderer.setTranX(tx);
                    mRenderer.setTranY(ty);

                    start.set(e.getX(), e.getY());  // Reset location for next movement
                }

                // Explicit call to render. No need if we're animating as
                // render mode will have been set to RENDERMODE_CONTINUOUSLY
                if(!mRenderer.isAnimating()) requestRender();

                break;

        }

        return true;
    }


    /** Determine the space between the first two fingers */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }


    /** Calculate the mid point of the first two fingers */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }


    /**
     * @return the Renderer
     */
    public MyRenderer getRenderer() {
        return mRenderer;
    }

}
