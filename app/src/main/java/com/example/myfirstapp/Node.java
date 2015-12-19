package com.example.myfirstapp;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

public class Node {

    final static int L_COR = 3;
    final static int L_VEL = 3;
    final static int L_ACC = 3;
    final static int L_TEM = 1;
    final static int L_MSC = 1;
    final static int L_FLX = 3;
    final static int L_TDT = 1;

    private static int NUM;

    /** Undeformed Nodal coordinates list */
    private static FloatBuffer undefCoords = null;

    public static int getNum() {
        return NUM;
    }

    /**
     * Node constructor
     * @param fam  Family
     */
    public Node(Family fam) {
        super();

        NUM = fam.getNumNodes();

        // Read the undeformed coordinates
        readUndefCoords(fam);
    }

    /** Read the undeformed nodal coordinates.
     *
     * @param fam  Family
     */
    private void readUndefCoords(Family fam) {

        FileChannel ch   = fam.getFileChannel();
        long        addr = fam.getUndefCoordAddr();

        int length = NUM * L_COR * 4; // * 4 for bytes

        // ByteBuffer for coordinates

        ByteBuffer bb = ByteBuffer.allocateDirect( length );

        // Byte order

        bb.order(fam.getEndianess());

        // View as FloatBuffer

        undefCoords = bb.asFloatBuffer();


        try {
// Position to start of undeformed coordinate data

            ch.position(addr*4);  // * 4 for bytes

// Read coordinates into byte buffer

            ch.read( bb );

        } catch (IOException e) {

            Log.d("MY_DEBUG", "Failed to get undeformed coordinates...");

            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    /**
     * @return the undeformed nodal coordinates list
     */
    public static FloatBuffer getUndefCoords() {

        undefCoords.position(0);

        return undefCoords;
    }






}
