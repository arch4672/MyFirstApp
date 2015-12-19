package com.example.myfirstapp;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class State {

    private final static String TAG = "STATE";

    /** Contour colour */
    private static float[] mRGB = new float[3];

    /** Coords as float buffer */
    private static FloatBuffer mCoords;

    /** Coords as byte buffer */
    private static ByteBuffer mBB;

    int     mNum;
    int     mMember;
    float   mTime;
    long    mAddr;

    private static Family  mFam;

    /**
     * State constructor
     * @param fam Family
     * @param istate State number
     * @param imember File member
     * @param addr Address in file member
     * @param time State time
     */
    public State(Family fam, int istate, int imember, long addr, float time) {
        super();

        // Store state address


        mFam = fam;

        mNum = istate;

        mMember = imember;

        mAddr = addr;

        mTime = time;

    }


    /**
     * Scans for states in the family, returning the states in an array
     * @param fam Family
     * @return Array of states
     */
    public static State[] scanStates(Family fam) {

        ArrayList<State> stateList = new ArrayList<State>();

        int n = fam.getNumMembers();

        int istate = 1;

        for(int imember=0; imember<n; imember++) {

            // Open this member (closing the current one as well)
            fam.openMember(imember);

            long addr = 0;
            if(imember == 0) addr = fam.getFirstStateAddr();


            try {
                float time = 0.0f;

                while(time != -999999.0f) {

                    // Read time value

                    FileChannel ch = fam.getFileChannel();

                    int length = 4;

                    // ByteBuffer

                    ByteBuffer bb = ByteBuffer.allocateDirect( length );

                    // Byte order

                    bb.order(fam.getEndianess());

                    // View as FloatBuffer

                    FloatBuffer state = bb.asFloatBuffer();


                    // Position to start of state data

                    ch.position(addr*4);  // * 4 for bytes

                    // Read time into byte buffer

                    ch.read( bb );

                    time = state.get(0);

                    if(time != -999999.0f) stateList.add(new State(fam, istate++, imember, addr, time));

                    addr += fam.getStateLength() + 1;
                }

            } catch (IOException e) {

                Log.d(TAG, "Failed to read state time in <scanStates>");

                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Return state list as array

        State[] states = new State[stateList.size()];

        stateList.toArray(states);


// Now allocate objects

        allocateStateObjects();

        return states;

    }


    public float getTime() {
        return mTime;
    }



    // TODO -> Reuse objects - declare them as class members - don't keep allocating them -> GC...
    // Allocate them just once - see MyRenderer

    /**
     * One time allocation of objects/arrays for getting state coords.
     * constant allocation leads to slow down because of GC
     */
    private static void allocateStateObjects() {

        // Byte buffer for coords
        int length = mFam.getNumNodes() * Node.L_COR * 4; // * 4 for bytes

        mBB = ByteBuffer.allocateDirect( length );

        mBB.order(mFam.getEndianess());


        // View as FloatBuffer

        mCoords = mBB.asFloatBuffer();

    }


    public FloatBuffer getStateCoords() {

        mCoords.rewind();
        mBB.rewind();

        mCoords.position(0);
        mBB.position(0);

        // Open the family member for this state

        mFam.openMember(mMember);

        // Now read coordinates
        try {
// Position to start of coordinate data

            int nglbv = mFam.getNumGlblVariables();

            long addr =   mAddr
                    + 1       // Time
                    + nglbv;  // Global variables

            mFam.getFileChannel().position(addr*4);  // * 4 for bytes

// Read coordinates into byte buffer

            mFam.getFileChannel().read( mBB );

        } catch (IOException e) {

            Log.d(TAG, "Failed to get state coordinates...");

            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mCoords;
    }




// TODO - probably should be in a Contour class
    /**
     * @param val, value
     * @return the default rgb colour for internal part i
     */
    public static float[] getContourColour(float val) {

        mRGB[0] = 0.0f;
        mRGB[1] = 0.0f;
        mRGB[2] = 0.0f;

// TODO - levels shouldn't be hard coded


        // sled.ptf model
        /*
             if(val < 303.0f) { mRGB[0]=0.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  }
        else if(val < 341.0f) { mRGB[0]=0.0f;  mRGB[1]=0.5f;  mRGB[2]=1.0f;  }
        else if(val < 379.0f) { mRGB[0]=0.0f;  mRGB[1]=0.75f; mRGB[2]=1.0f;  }
        else if(val < 417.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=1.0f;  }
        else if(val < 454.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.66f; }
        else if(val < 492.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 530.0f) { mRGB[0]=0.75f; mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 568.0f) { mRGB[0]=1.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 606.0f) { mRGB[0]=1.0f;  mRGB[1]=0.75f; mRGB[2]=0.0f;  }
        else if(val < 644.0f) { mRGB[0]=1.0f;  mRGB[1]=0.5f;  mRGB[2]=0.0f;  }
        else if(val < 682.0f) { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.0f;  }
        else if(val < 720.0f) { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.58f; }
        else                  { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  }
        */

        // crush4.ptf

             if(val < 30.0)   { mRGB[0]=0.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  }
        else if(val < 59.0f)  { mRGB[0]=0.0f;  mRGB[1]=0.5f;  mRGB[2]=1.0f;  }
        else if(val < 88.0f)  { mRGB[0]=0.0f;  mRGB[1]=0.75f; mRGB[2]=1.0f;  }
        else if(val < 118.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=1.0f;  }
        else if(val < 147.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.66f; }
        else if(val < 176.0f) { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 206.0f) { mRGB[0]=0.75f; mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 235.0f) { mRGB[0]=1.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  }
        else if(val < 264.0f) { mRGB[0]=1.0f;  mRGB[1]=0.75f; mRGB[2]=0.0f;  }
        else if(val < 293.0f) { mRGB[0]=1.0f;  mRGB[1]=0.5f;  mRGB[2]=0.0f;  }
        else if(val < 323.0f) { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.0f;  }
        else if(val < 352.0f) { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.58f; }
        else                  { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  }

        return mRGB;
    }
}
