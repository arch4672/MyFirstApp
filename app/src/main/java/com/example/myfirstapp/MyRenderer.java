package com.example.myfirstapp;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This class implements our custom renderer. Note that the GL10 parameter passed in is unused for OpenGL ES 2.0
 * renders -- the static class GLES20 is used instead.
 */
public class MyRenderer implements GLSurfaceView.Renderer
{

    static {
        System.loadLibrary("myUtils");
    }


    /*
    ** Native function to populate Vertex Data for a part - faster than doing it in Java
    */
    private native int populatePartVertexData(int numEls,
                                             FloatBuffer undef,
                                             FloatBuffer coords,
                                             IntBuffer shTop,
                                             int[] elsInPart,
                                             FloatBuffer vertexData,
                                             boolean SI,
                                             int eswap);

    private native int updateContourLimits(FloatBuffer undef, FloatBuffer coords, int len, int eswap);



    private final static String TAG = "RENDERER";

    private GLSurfaceView mGlSurfaceView;

    /** Context (from Activity) */
    private Context context;

    /** Model matrix */
    private float[] mModelMatrix = new float[16];

    /** View Matrix */
    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** ModelView Matrix */
    private float[] mMVMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** Stores a copy of the model matrix specifically for the light position. */
    private float[] mLightModelMatrix = new float[16];

    /** Per-part vertex data */
    private FloatBuffer mVertexData;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in the light position. */
    private int mLightPosHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model color information. */
    private int mColorHandle;

    /** This will be used to pass in model normal information. */
    private int mNormalHandle;

    /** How many bytes per float. */
    private static final int BYTES_PER_FLOAT = 4;

    /** Size of the position data in elements. */
    private static final int POSITION_DATA_SIZE = 3;

    /** Size of the normal data in elements. */
    private static final int NORMAL_DATA_SIZE = 3;

    /** Size of the colour data in elements. */
    private static final int COLOUR_DATA_SIZE = 3;

    /** Vertices per shell */
    private static final int VERTICES_PER_SHELL = 6;




    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     *  we multiply this by our transformation matrices. */
    private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};

    /** Used to hold the current position of the light in world space (after transformation via model matrix). */
    private final float[] mLightPosInWorldSpace = new float[4];

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
    private final float[] mLightPosInEyeSpace = new float[4];

    /** This is a handle to our per-vertex shading program. */
    private int mPerVertexProgramHandle;

    /** VBO handle per-part */
    private int[] mVBOHandle;

    /** Model bounds */
    private float mModelMinX;
    private float mModelMaxX;

    private float mModelMinY;
    private float mModelMaxY;

    private float mModelMinZ;
    private float mModelMaxZ;

    /** Model centre */
    private float mModelCentreX;
    private float mModelCentreY;
    private float mModelCentreZ;

    /** Model diagonal */
    private float mModelDiag;

    /** Scale for projection matrix */
    private float mScaleProj;

    /** Camera eye position. */
    private float mEyeX;
    private float mEyeY;
    private float mEyeZ;

    /** Screen dimensions */
    private int mScreenWidth;
    private int mScreenHeight;

    /** Scale */

    private float mScaleFactor = 1.0f;

    /** Rotations */

    private float mRx;
    private float mRy;

    /** Translations */

    private float mTx;
    private float mTy;

    /** Number of vertex points*/
    private int[] mNPoints;

    /** Number of parts to render */
    private int mNParts;

    /** Family to render */
    private Family mFam;

    /** Coords buffer
     * Don't change to local variable - only want to allocate
     * once - reduce GC
     */
    private FloatBuffer mCoords;

	/* Set and get methods */

    public void  setTranX(float x) { mTx = x; }
    public void  setTranY(float y) { mTy = y; }
    public void  setRotX(float x)  { mRx = x; }
    public void  setRotY(float y)  { mRy = y; }

    public void  setScale(float s) { mScaleFactor = s; }
    public float getScale()        { return mScaleFactor; }

    public float getModelDiag()    { return mModelDiag; }



    /** Animating flag */
    private boolean isAnimating = false;

    /** Go to previous state flag */
    private boolean prevState = false;

    /** Go to next state flag */
    private boolean nextState = false;

    /** Current frame */
    private int currentFrame = 0;

    /** SI flag */
    private boolean doSIPlot = false;

    /** Set the SI plot flag */
    public void setSI(boolean status) {

        doSIPlot = status;

        // Update the contour limits if we're going to do an SI plot - do in native code
        if(doSIPlot) {

            //Node.getUndefCoords().rewind();
            mCoords = mFam.getStateFromID( mFam.getNumOfStates() - 1).getStateCoords();

            updateContourLimits(Node.getUndefCoords(), mCoords, Node.getNum(), mFam.getEswap());

        }

    }


    /**
     * Toggles the animation flag
     * @return the animation flag status
     */
    boolean toggleAnimation() {

        isAnimating = !isAnimating;

        if(isAnimating)
            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        else
            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        return isAnimating;
    }

    /*
    ** @return the animation flag status
     */
    public boolean isAnimating() {
        return isAnimating;
    }


    /*
    ** Go to previous state
     */
    public void goToPrevState() {
        prevState = true;
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /*
    ** Go to next return
     */
    public void goToNextState() {
        nextState = true;
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }


    /**
     * Initialize the model data.
     */
    public MyRenderer(final GLSurfaceView glSurfaceView, Family fam, Context context)
    {
        this.context = context;                         // Save Specified Context

        mGlSurfaceView = glSurfaceView;


        // Store the family in a member variable

        mFam = fam;


        mNParts = mFam.getNumParts();

        // Get extensions - could check for GL_OES_vertex_half_float (16 bit)
        //                  could be used for normals, coords(?) to reduce memory

        //String glExtensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);

        // Crashes here when changing screen orientation with null pointer exception?
        //Log.d("EXTENSIONS", glExtensions);    
    }



    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Set the background clear color to grey.
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        // Use culling to remove back faces - don't want to do for shells.
        //GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        final String vertexShader   = getVertexShader();
        final String fragmentShader = getFragmentShader();

        final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mPerVertexProgramHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[] {"a_Position",  "a_Color", "a_Normal"});

        // Initialise data here, rather than in constructor so that it's not done on
        // the UI thread

        // Initialise vertex data
        initialiseVertexData();

        // Initialise VBOs
        initialiseVBOs();


        // Get model bounds

        float[] modelBounds = mFam.getModelBounds();

        mModelMinX =  modelBounds[0];
        mModelMinY =  modelBounds[1];
        mModelMinZ =  modelBounds[2];

        mModelMaxX =  modelBounds[3];
        mModelMaxY =  modelBounds[4];
        mModelMaxZ =  modelBounds[5];
		
		/* Calculate centre position of model */

        mModelCentreX = 0.5f * (mModelMinX + mModelMaxX);
        mModelCentreY = 0.5f * (mModelMinY + mModelMaxY);
        mModelCentreZ = 0.5f * (mModelMinZ + mModelMaxZ);

		/* Calculate model diagonal */

        mModelDiag = (float)Math.sqrt( (mModelMaxX - mModelMinX) * (mModelMaxX - mModelMinX) +
                (mModelMaxY - mModelMinY) * (mModelMaxY - mModelMinY) +
                (mModelMaxZ - mModelMinZ) * (mModelMaxZ - mModelMinZ) );

		/* Set camera variables - Look down Z axis to model centre, Y axis up */

        mEyeX = mModelCentreX;
        mEyeY = mModelCentreY;
        mEyeZ = mModelMaxZ + 2*(mModelMaxZ - mModelMinZ);

        float lookX = mModelCentreX;
        float lookY = mModelCentreY;
        float lookZ = mModelCentreZ;

        float upX = 0.0f;
        float upY = 1.0f;
        float upZ = 0.0f;

        // Model matrix - Identity

        Matrix.setIdentityM(mModelMatrix, 0);

        // View matrix - Move camera so model is in centre of view

        Matrix.setLookAtM(mViewMatrix, 0, mEyeX, mEyeY, mEyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        // Model View Matrix

        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Scale

        mScaleFactor = 1.0f;


        // Update geometry to frame 0
        updateFrame(0);
    }



    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Set the member variables to the width and height
        mScreenWidth  = width;
        mScreenHeight = height;

        // Calculation of scale

        float scale1 = (mModelMaxX - mModelMinX) / mScreenWidth;
        float scale2 = (mModelMaxY - mModelMinY) / mScreenHeight;

        mScaleProj = Math.max(scale1, scale2);
    }




    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        long start = System.currentTimeMillis();


        // Animate or go to next state

        if (isAnimating || prevState || nextState) {

            if(prevState)
              currentFrame--;
            else
              currentFrame++;

            if ( currentFrame >= mFam.getNumOfStates()) {
                currentFrame = 0;
            }

            if(currentFrame < 0)
                currentFrame = mFam.getNumOfStates();

            updateFrame( currentFrame);

            if(nextState || prevState) {
                prevState = false;
                nextState = false;
                mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }
        }



        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);

        // Set program handles for drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle  = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix");
        mLightPosHandle  = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");

        mPositionHandle  = GLES20.glGetAttribLocation(mPerVertexProgramHandle,  "a_Position");
        mColorHandle     = GLES20.glGetAttribLocation(mPerVertexProgramHandle,  "a_Color");
        mNormalHandle    = GLES20.glGetAttribLocation(mPerVertexProgramHandle,  "a_Normal");

        // Rotate ModelView matrix

        Matrix.translateM(mMVMatrix, 0, mModelCentreX, mModelCentreY, mModelCentreZ);

        Matrix.rotateM(mMVMatrix, 0, mRx, mMVMatrix[1], mMVMatrix[5], mMVMatrix[9]);
        Matrix.rotateM(mMVMatrix, 0, mRy, mMVMatrix[0], mMVMatrix[4], mMVMatrix[8]);

        Matrix.translateM(mMVMatrix, 0, -mModelCentreX, -mModelCentreY, -mModelCentreZ);

        // Translate ModelView matrix

        mMVMatrix[12] += mTx;
        mMVMatrix[13] -= mTy;

        // Projection matrix

        float left   = -mScaleProj*mScreenWidth;
        float right  =  mScaleProj*mScreenWidth;
        float bottom = -mScaleProj*mScreenHeight;
        float top    =  mScaleProj*mScreenHeight;
        float near   = -10.0f*mModelDiag;
        float far    =  10.0f*mModelDiag;

        //Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        Matrix.orthoM(mProjectionMatrix, 0, left, right, bottom, top, near, far);

        // Apply current scale to Projection matrix

        Matrix.scaleM(mProjectionMatrix, 0, mScaleFactor, mScaleFactor, mScaleFactor);


        // Model View Projection matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);


        // Set position of the light behind the camera eye position.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, mEyeX, mEyeY, mEyeZ+10.0f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // Draw Parts

        drawParts();


        long time = System.currentTimeMillis() - start;

        // Log.d(TAG, "Frame time " + Long.toString(time) + "mS");
    }


    /**
     * Initialise things for holding vertex data
     */
    private void initialiseVertexData()
    {
        mNPoints = new int[mNParts];

        int sizeCoords, sizeNormal, sizeColour;

        int sizeVertexData  = 0;

        for(int i=0; i<mNParts; i++) {

            mNPoints[i] = Part.getNumElsInPart(i) * VERTICES_PER_SHELL;
            sizeCoords = mNPoints[i] * POSITION_DATA_SIZE;
            sizeNormal = mNPoints[i] * NORMAL_DATA_SIZE;
            sizeColour = mNPoints[i] * COLOUR_DATA_SIZE;

            sizeVertexData  = Math.max(sizeVertexData, sizeCoords + sizeNormal + sizeColour);
        }

        mVertexData = ByteBuffer.allocateDirect(sizeVertexData * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }




    /**
     * Initialise VBO handles for parts
     */
    private void initialiseVBOs()
    {
// For each part set up a VBO of the correct size, and initialise it to NULL

// Get buffers and allocate array for VBO handles 

        final int buffer[] = new int[mNParts];
        GLES20.glGenBuffers(mNParts, buffer, 0);

        mVBOHandle = new int[mNParts];

// Loop over each part and initialise VBO

         for(int i=0; i<mNParts; i++) {

            int points = Part.getNumElsInPart(i) * VERTICES_PER_SHELL;

            int sizeVertexData = points * (POSITION_DATA_SIZE +
                                           NORMAL_DATA_SIZE   +
                                           COLOUR_DATA_SIZE);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[i]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, sizeVertexData * BYTES_PER_FLOAT, null, GLES20.GL_DYNAMIC_DRAW);

            // Store handle for this part
            mVBOHandle[i] = buffer[i];
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }





    /*
    ** Update the frame
     */
    public void updateFrame(int istate) {

        // Update of vertex coordinates with current state data
        // TODO - currently only renders SHELLs

        int sizeVertexData ;


        if(istate == 0) {
            //Node.getUndefCoords().rewind();

            mCoords = Node.getUndefCoords();
        }
        else
            mCoords = mFam.getStateFromID(istate).getStateCoords();


        for(int i=0; i<mNParts; i++) {

            if(Part.getType(i) != Element.SHELL) continue;  // TODO - only renders shells

            sizeVertexData  = mNPoints[i] * (POSITION_DATA_SIZE +
                                             NORMAL_DATA_SIZE +
                                             COLOUR_DATA_SIZE) ;

// Call native function to populate vertex data

            populatePartVertexData( Part.getNumElsInPart(i),
                                    Node.getUndefCoords(),
                                    mCoords,
                                    Shell.getTop(),
                                    Part.getElsInPart(i),
                                    mVertexData,
                                    doSIPlot,
                                    mFam.getEswap());

// Now update VBO for this part
            updateVBO(i, sizeVertexData);
        }
    }


    /**
     * Update VBO data for internal part <i>
     */

    private void updateVBO(int ipart, int sizeVertex)
    {
        // Update GPU from client-side buffers and then release them

        if(mVBOHandle[ipart] != 0) {

            mVertexData.position(0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBOHandle[ipart]);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, sizeVertex * BYTES_PER_FLOAT,  mVertexData);

            //GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
    }


    /**
     * Draw parts
     */
    private void drawParts()
    {
        // Log.d(TAG, "Drawing Parts...");

        // Pass in the model view matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);


        // Render by part

        final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + COLOUR_DATA_SIZE) * BYTES_PER_FLOAT;

        for(int i=0; i<mNParts; i++) {

            // Bind the vertex data buffer
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBOHandle[i]);

            // Pass in the position information - from VBO
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false, stride, 0);

            // Pass in the normal information - from VBO
            GLES20.glEnableVertexAttribArray(mNormalHandle);
            GLES20.glVertexAttribPointer(mNormalHandle, NORMAL_DATA_SIZE, GLES20.GL_FLOAT, false, stride, (POSITION_DATA_SIZE) * BYTES_PER_FLOAT);

            // Pass in the colour information - from VBO
            GLES20.glEnableVertexAttribArray(mColorHandle);
            GLES20.glVertexAttribPointer(mColorHandle, COLOUR_DATA_SIZE, GLES20.GL_FLOAT, false, stride, (POSITION_DATA_SIZE + NORMAL_DATA_SIZE) * BYTES_PER_FLOAT);

            // Draw the shells.

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mNPoints[i]);

            // Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }
    }


    protected String getVertexShader()
    {
        return "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
               + "uniform mat4 u_MVMatrix;       \n"		// A constant representing the combined model/view matrix.
               + "uniform vec3 u_LightPos;       \n"	    // The position of the light in eye space.

               + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
               + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
               + "attribute vec3 a_Normal;       \n"		// Per-vertex normal information we will pass in.

               + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.

               + "const float ambient    = 0.3;  \n"
               + "const float brightness = 0.7;  \n"

               + "void main()                    \n" 	// The entry point for our vertex shader.
               + "{                              \n"
               // Ambient colour
               + "    v_Color = a_Color * ambient;\n"
               // Transform the vertex into eye space.
               + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
               // Normalise normal - do it here rather than on the CPU as it's quicker
               + "   vec3 normalNormal = normalize(a_Normal);                           \n"
               // Transform the normal's orientation into eye space.
               + "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(normalNormal, 0.0));     \n"
               // Get a lighting direction vector from the light to the vertex.
               + "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
               // Calculate the dot product of the light vector and vertex normal.
               + "   float diffuse = abs(dot(modelViewNormal, lightVector));       \n"
               // Multiply the color by the illumination level. It will be interpolated across the triangle.
               + "   v_Color += brightness * a_Color * diffuse;                                       \n"
               // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
               + "   gl_Position = u_MVPMatrix * a_Position;                            \n"
               + "}                                                                     \n";
    }

    protected String getFragmentShader()
    {
        return  "precision mediump float;        \n"		// Set the default precision to medium. We don't need as high of a
                                                            // precision in the fragment shader.
               + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                                                            // triangle per fragment.
               + "void main()                    \n"		// The entry point for our fragment shader.
               + "{                              \n"
               + "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.
               + "}                              \n";
    }

    /**
     * Helper function to compile a shader.
     *
     * @param shaderType The shader type.
     * @param shaderSource The shader source code.
     * @return An OpenGL handle to the shader.
     */
    private int compileShader(final int shaderType, final String shaderSource)
    {
        int shaderHandle = GLES20.glCreateShader(shaderType);

        if (shaderHandle != 0)
        {
            // Pass in the shader source.
            GLES20.glShaderSource(shaderHandle, shaderSource);

            // Compile the shader.
            GLES20.glCompileShader(shaderHandle);

            // Get the compilation status.
            final int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0)
            {
                Log.e("MY_DEBUG", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
                GLES20.glDeleteShader(shaderHandle);
                shaderHandle = 0;
            }
        }

        if (shaderHandle == 0)
        {
            throw new RuntimeException("Error creating shader.");
        }

        return shaderHandle;
    }

    /**
     * Helper function to compile and link a program.
     *
     * @param vertexShaderHandle An OpenGL handle to an already-compiled vertex shader.
     * @param fragmentShaderHandle An OpenGL handle to an already-compiled fragment shader.
     * @param attributes Attributes that need to be bound to the program.
     * @return An OpenGL handle to the program.
     */
    private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes)
    {
        int programHandle = GLES20.glCreateProgram();

        if (programHandle != 0)
        {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle);

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle);

            // Bind attributes
            if (attributes != null)
            {
                final int size = attributes.length;
                for (int i = 0; i < size; i++)
                {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle);

            // Get the link status.
            final int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

            // If the link failed, delete the program.
            if (linkStatus[0] == 0)
            {
                Log.e("MY_DEBUG", "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
                GLES20.glDeleteProgram(programHandle);
                programHandle = 0;
            }
        }

        if (programHandle == 0)
        {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }
}

