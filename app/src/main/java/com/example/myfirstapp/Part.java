package com.example.myfirstapp;

import java.nio.IntBuffer;

public class Part {

    /** Number of parts */
    int mNum;

    /** Element type list */
    private static int[] mType;

    /** Number of elements in part */
    private static int[] mNumInPart;

    /** List of elements in parts */
    private static int[][] mElList;

    /** Part colour */
    private static float[] mRGB = new float[3];

    public Part(Family fam) {
        super();

        mNum = fam.getNumParts();

        createPartLists(fam);
    }


    /** Create the part lists
     *
     * @param fam Family
     */
    private void createPartLists(Family fam) {

        // Initialise number of elements in part list
        // and element type list

        mNumInPart = new int[mNum];
        mType      = new int[mNum];

        for(int i=0; i<mNum; i++) {
            mNumInPart[i] = 0;
            mType[i]      = 0;
        }

        // For each element type count how many are in each part
        // and store the element type of the part
        // TODO: for now we only render shell elements


        for(int type=Element.FIRST; type<=Element.LAST; type++) {

            int n = 0, ltop = 0;
            IntBuffer top = null;

            switch(type) {

                case Element.SOLID:  { continue; }   // Ignore SOLIDs for now
                case Element.BEAM:   { continue; }   // Ignore BEAMS for now
                case Element.SHELL:  { top = Shell.getTop();  ltop = Shell.L_TOP;  n = fam.getNumShells();  break; }
                case Element.TSHELL: { continue; }   // Ignore TSHELLs for now

            }

            // Number of elements per part

            int pid;

            for(int i=0; i<n; i++) {

                pid = top.get(((ltop-1) + ltop*i)) - 1;

                if(pid >= 0 && pid < mNum)
                {
                    mNumInPart[pid]++;

                    mType[pid] = type;  // Element type
                }
            }

            // Now initialize element list
            // and populate element type list

            mElList = new int[mNum][];

            for(int i=0; i<mNum; i++) {

                mElList[i] = new int[mNumInPart[i]];

                // Reset so we can use it as an index when we populate
                // the list below.  It gets recreated there.
                mNumInPart[i] = 0;
            }

            // Populate list

            for(int i=0; i<n; i++) {

                pid = top.get(((ltop-1) + ltop*i)) - 1;

                if(pid >=0 && pid < mNum) mElList[pid][mNumInPart[pid]++] = i;
            }
        }

    }


    /**
     * @param i, internal part
     * @return The number of elements in internal part i
     */
    public static int getNumElsInPart(int i) { return mNumInPart[i]; }


    /**
     *
     * @param i, internal part
     * @param j, element
     * @return Internal element ID of j'th element in i'th part
     */
    public static int getElementInPart(int i, int j) {
        return mElList[i][j];
    }

    /**
     *
     * @param i, internal part
     * @return Array of element indices in part i
     */
    public static int[] getElsInPart(int i) {
        return mElList[i];
    }


    /**
     * @param i, internal part
     * @return the element type for internal part i
     */
    public static int getType(int i) { return mType[i]; }


    /**
     * @param i, internal part
     * @return the default rgb colour for internal part i
     */
    public static float[] getDefaultColour(int i) {

        mRGB[0] = 0.0f;
        mRGB[1] = 0.0f;
        mRGB[2] = 0.0f;

        switch (i % 13) {

            case 0:  { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.0f;  break; }
            case 1:  { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  break; }
            case 2:  { mRGB[0]=0.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  break; }
            case 3:  { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=1.0f;  break; }
            case 4:  { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=1.0f;  break; }
            case 5:  { mRGB[0]=1.0f;  mRGB[1]=1.0f;  mRGB[2]=0.0f;  break; }
            case 6:  { mRGB[0]=1.0f;  mRGB[1]=0.0f;  mRGB[2]=0.58f; break; }
            case 7:  { mRGB[0]=1.0f;  mRGB[1]=0.75f; mRGB[2]=0.0f;  break; }
            case 8:  { mRGB[0]=0.66f; mRGB[1]=1.0f;  mRGB[2]=0.0f;  break; }
            case 9:  { mRGB[0]=0.0f;  mRGB[1]=1.0f;  mRGB[2]=0.66f; break; }
            case 10: { mRGB[0]=0.0f;  mRGB[1]=0.5f;  mRGB[2]=1.0f;  break; }
            case 11: { mRGB[0]=1.0f;  mRGB[1]=0.5f;  mRGB[2]=0.0f;  break; }
            case 12: { mRGB[0]=0.0f;  mRGB[1]=0.75f; mRGB[2]=1.0f;  break; }
        }

        return mRGB;

    }
}
