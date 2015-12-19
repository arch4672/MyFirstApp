package com.example.myfirstapp;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

public class Shell {

    final static int L_TOP = 5;

    private static int mNum;


    /** Shell topology list */
    private static IntBuffer top = null;

    public static int getNum() {
        return mNum;
    }


    /**
     * Shell constructor
     * @param fam Family
     */
    public Shell(Family fam) {
        super();

        mNum = fam.getNumShells();

        // Read the topology
        readTopology(fam);
    }


    /** Read the shell topology
     *
     * @param fam Family
     */
    private void readTopology(Family fam) {

        FileChannel ch   = fam.getFileChannel();
        long        addr = fam.getShellTopAddr();


        int length = mNum * L_TOP * 4; // * 4 for bytes


        // ByteBuffer for topology

        ByteBuffer bb = ByteBuffer.allocateDirect( length );

        // Byte order

        bb.order(fam.getEndianess());

        // View as IntBuffer

        top = bb.asIntBuffer();

        try {
// Position to start of undeformed coordinate data

            ch.position(addr*4);  // * 4 for bytes

// Read coordinates into byte buffer

            ch.read( bb );

        } catch (IOException e) {

            Log.d("MY_DEBUG", "Failed to get shell topology...");

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * @return the shell topology list
     */
    public static IntBuffer getTop() {

        top.position(0);

        return top;
    }

}
