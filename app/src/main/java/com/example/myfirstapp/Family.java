package com.example.myfirstapp;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

public class Family {

    private final static String TAG = "FAMILY";

    private String mRootName;

    /** Flag to do endian swap when reading data */
    private int eswap;

    /** Control block information */
    private int ndim;
    private int numnp;
    private int nel8;
    private int nelt;
    private int nel2;
    private int nel4;
    private int npart;
    private int nglbv;
    private int sphnod;
    private int nrelem;
    private int nrigv;
    private int nsphv;
    private int labags;
    private int itflg;
    private int iuflg;
    private int ivflg;
    private int iaflg;
    private int imflg;
    private int ifflg;
    private int idflg;
    private int numhv;
    private int numbv;
    private int numsv;
    private int numtv;
    private int narbs;
    private int ldtab;
    private int maxint;

    /** Number of states */
    private int nstates;

    /** State list */
    private State[] stateList;

    /** Length of geometry block (in words) */
    private int lgeom;

    /** Length of state (in words) */
    private int lstate;

    /** Address of start of undeformed coords (in words) */
    private long undefCoordAddr;

    /** Address of start of thin shell topology (in words) */
    private long shellTopAddr;

    /** Address of first state */
    private long firstStateAddr;

    /** Basic Node class */
    private Node basicNode;

    /** Basic Shell class */
    private Shell basicShell;

    /** Basic Part class */
    private Part basicPart;

    /** Number of family members */
    private int numMembers;

    /** List of family member names */
    private String[] memberNames;

    /** Current file input stream */
    private FileInputStream mFis;

    /** Current File channel */
    private FileChannel mChannel;

    /** Current open member number */
    private int mCurrentMember;

    /** Endianess of files */
    private ByteOrder mEndianess;



    /**
     * Family constructor
     * @param mRootName
     */
    public Family(String mRootName) {
        super();
        this.mRootName = mRootName;
    }


    /** Opens a family and reads data needed to initialise model
     *
     * @throws IOException
     */
    public void openFamily() throws IOException {

        // Get family child members
        getChildMembers();

        // Open root member
        openMember(0);

        // Work out endianess of file
        computeFileFormat();

        // Read control block
        readControlBlock();

        // Read geometry block
        readGeometry();

        // Declare states
        declareStates();
    }


    /** Compute the file format
     * @throws IOException
     */
    private void computeFileFormat() throws IOException
    {
        // Compute the Endianess of the file. Look at words 15 and 17 in the control block.
        //
        //        Word                     Byte
        //
        //         15    BIG_ENDIAN         63       (= 4-7 for Dyna, = 3 for Topaz)
        //               LITTLE_ENDIAN      60
        //
        //         17    BIG_ENDIAN         71       (= 6 for Dyna, = 1 for Topaz)
        //               LITTLE_ENDIAN      68
        //
        //
        // TODO - Assumes 32 bit file for now

        // ByteBuffer for first 17 words (72 bytes)

        ByteBuffer  bb = ByteBuffer.allocateDirect( 72 );

        // Read data

        mChannel.position(0);
        mChannel.read( bb );


        if(  bb.get(60) == 0 && bb.get(61) == 0 && bb.get(62) == 0  && 		/* BIG ENDIAN */
             bb.get(68) == 0 && bb.get(69) == 0 && bb.get(70) == 0  &&
           ((bb.get(63) >= 4 && bb.get(63) <= 7 && bb.get(71) == 6) ||
            (bb.get(63) == 3                    && bb.get(71) == 1)))
        {
            mEndianess = ByteOrder.BIG_ENDIAN;
        }

        else if(  bb.get(63) == 0 && bb.get(62) == 0 && bb.get(61) == 0  && 	/* LITTLE ENDIAN */
                  bb.get(71) == 0 && bb.get(70) == 0 && bb.get(69) == 0  &&
                ((bb.get(60) >= 4 && bb.get(60) <= 7 && bb.get(68) == 6) ||
                 (bb.get(60) == 3                    && bb.get(68) == 1)))
        {
            mEndianess = ByteOrder.LITTLE_ENDIAN;
        }

        if(ByteOrder.nativeOrder() != mEndianess) eswap = 1;
        else                                      eswap = 0;

        return;
    }


    /** Reads the control block
     *
     * @throws IOException
     */
    private void readControlBlock() throws IOException {

        // ByteBuffer for first 64 words (256 bytes)

        ByteBuffer  bb = ByteBuffer.allocateDirect( 256 );

        // Byte order

        bb.order(mEndianess);

        // Get IntBuffer view of the ByteBuffer

        IntBuffer   ib = bb.asIntBuffer();

        // Read data

        mChannel.position(0);
        mChannel.read( bb );

        // Number of parts

        npart = ib.get(24) +
                ib.get(29) +
                ib.get(32) +
                ib.get(41);

        // Get information needed to read geometry - TODO Add more data as needed

        ndim  = ib.get(15);
        numnp = ib.get(16);
        nglbv = ib.get(18);
        itflg = ib.get(19); // TODO - actually more complicated than this
        iuflg = ib.get(20);
        ivflg = ib.get(21);
        iaflg = ib.get(22);
        nel8  = ib.get(23);
        numhv = ib.get(27);
        numbv = ib.get(30);
        numsv = ib.get(33);
        maxint = ib.get(36);
        narbs = ib.get(39);
        nelt  = ib.get(40);
        numtv = ib.get(42);
        nel2  = ib.get(28);
        nel4  = ib.get(31);

        nrelem = 0;  // TODO - deal with properly
        nrigv  = 0;  // TODO - deal with properly
        nsphv  = 0;  // TODO - deal with properly
        labags = 0;  // TODO - deal with properly

        ifflg = (itflg % 10) > 1 ? 1 : 0;
        imflg = (itflg / 10) > 0 ? 1 : 0;
        idflg = ib.get(56);

        if(ib.get(37) > 0 || ib.get(38) > 0) sphnod = ib.get(37);
        else                                 sphnod = 0;


        lgeom =   numnp * Node.L_COR
                + nel8  * 9
                + nelt  * 9
                + nel2  * 6
                + nel4  * Shell.L_TOP
                + narbs;

        undefCoordAddr = 64;	// TODO: Remove hard coded value

        shellTopAddr =   undefCoordAddr
                + numnp * Node.L_COR
                + nel8  * 9
                + nelt  * 9
                + nel2  * 6;

        firstStateAddr =   undefCoordAddr  // TODO - isn't correct, doesn't take into account SPH, airbags, etc...
                + lgeom;

        lstate =  nglbv
                + itflg * Node.L_TEM  *  numnp
                + iuflg * Node.L_COR  *  numnp
                + ivflg * Node.L_VEL  *  numnp
                + iaflg * Node.L_ACC  *  numnp
                + imflg * Node.L_MSC  *  numnp
                + ifflg * Node.L_FLX  *  numnp
                + idflg * Node.L_TDT  *  numnp
                + numhv               *  nel8
                + numsv               * (nel4 - nrelem)
                + numbv               *  nel2
                + numtv               *  nelt
                + nrigv
                + nsphv               *  sphnod
                + labags;

        // Deletion table

        ldtab = 0;

        if(maxint < 0)
        {
            if(maxint > -10000)		/* VEC_DYNA case */
            {
                ldtab = numnp;
            }
            else				/* Post 930 case */
            {
                ldtab = nel8 + nel2 + nel4 + nelt;
            }

            lstate += ldtab;
        }
    }


    /** Reads the geometry block, storing data in 'basic' classes
     *
     * @throws IOException
     */
    private void readGeometry() throws IOException {

        // Get basic Node data

        basicNode = new Node(this);

        // TODO - get Solid, beam and thick shell data



        // Get basic Shell data

        basicShell = new Shell(this);

        // Get basic Part data

        basicPart = new Part(this);
    }

    /**
     * Searches for family child members
     */
    private void getChildMembers()
    {
        // Count members

        numMembers = 1;  // Root

        while(true) {

            String name;

            if(numMembers < 10) name = mRootName + "0" + Integer.toString(numMembers);
            else                name = mRootName       + Integer.toString(numMembers);

            File f = new File(name);

            if(f.exists()) numMembers++;
            else           break;
        }

        // Populate list

        memberNames = new String[numMembers];

        memberNames[0] = mRootName;

        for(int i=1; i<numMembers; i++) {

            if(i < 10) memberNames[i] = mRootName + "0" + Integer.toString(i);
            else       memberNames[i] = mRootName +       Integer.toString(i);

        }
    }


    /**
     * Searches for states and populates stateList[]
     * @throws IOException
     */
    private void declareStates() {

        stateList = State.scanStates(this);

        nstates = stateList.length;

        for(int i=0; i<nstates; i++) {
            stateList[i].getTime();
        }


        // Close current member and set to first for now
        openMember(0);

    }


    /**
     * Open a family member, closing the current one if needed
     * @param iMember
     */
    public void openMember(int iMember) {

        // Check it's a valid member number
        if(iMember > numMembers - 1) {
            Log.e(TAG, "<iMember> too large in <openMember>. Ignoring...");
            return;
        }

        // Nothing to do if already current
        if(mFis != null && iMember == mCurrentMember) return;

        // Close current member
        if(mFis != null) {
            try {
                mFis.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Open new member

        String name = memberNames[iMember];

        try {

            mFis = new FileInputStream( name );

            mChannel = mFis.getChannel( );

            mCurrentMember = iMember;

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    /**
     * @return the mRootName
     */
    public String getRootName() {
        return mRootName;
    }


    /**
     * @return the number of Nodes
     */
    public int getNumNodes() {
        return numnp;
    }


    /**
     * @return the number of Solids
     */
    public int getNumSolids() {
        return nel8;
    }


    /**
     * @return the number of Thick shells
     */
    public int getNumThickShells() {
        return nelt;
    }


    /**
     * @return the number of Beams
     */
    public int getNumBeams() {
        return nel2;
    }


    /**
     * @return the number of Shells
     */
    public int getNumShells() {
        return nel4;
    }


    /**
     * @return the number of Parts
     */
    public int getNumParts() {
        return npart;
    }


    /**
     * @return the length of the geometry block
     */
    public int getGeomLength() {
        return lgeom;
    }

    /**
     *
     * @return number of global variables
     */
    public int getNumGlblVariables() {
        return nglbv;
    }


    /**
     * @return the address of the undeformed coordinates
     */
    public long getUndefCoordAddr() {
        return undefCoordAddr;
    }


    /**
     * @return the address of the shell topology
     */
    public long getShellTopAddr() {
        return shellTopAddr;
    }


    /**
     * @return the address of the first state
     */
    public long getFirstStateAddr() {
        return firstStateAddr;
    }


    /**
     * @return the basicNode
     */
    public Node getBasicNode() {
        return basicNode;
    }


    /**
     * @return the basicShell
     */
    public Shell getBasicShell() {
        return basicShell;
    }


    /**
     * @return the basicPart
     */
    public Part getBasicPart() {
        return basicPart;
    }


    /**
     * @return the length of a state (in words)
     */
    public int getStateLength() {
        return lstate;
    }

    /**
     * @return the current FileChannel
     */
    public FileChannel getFileChannel() {
        return mChannel;
    }

    /**
     * @return the eswap flag
     */
    public int getEswap() {
        return eswap;
    }

    /**
     * @param istate
     * @return State
     */
    public State getStateFromID(int istate) {

        // Just return last state if <istate> is too large
        if(istate - 1 > nstates - 1) {

            Log.e(TAG, "<istate> too large in <getStateFromID>. Returning last state.");

            istate = nstates - 1;
        }

        return stateList[istate - 1];

    }

    /**
     * @return the model bounds
     */
    public float[] getModelBounds() {

        float[] bounds = new float[6];

        bounds[0] =  Float.MAX_VALUE; // X min
        bounds[1] =  Float.MAX_VALUE; // Y min
        bounds[2] =  Float.MAX_VALUE; // Z min

        bounds[3] = -Float.MAX_VALUE; // X max
        bounds[4] = -Float.MAX_VALUE; // Y max
        bounds[5] = -Float.MAX_VALUE; // Z max

        // TODO - currently uses undeformed coords. Should depend on current frame

        FloatBuffer nodeCoords = Node.getUndefCoords();

        float[] coords = new float[nodeCoords.capacity()];
        nodeCoords.get(coords);

        for(int i=0; i<this.numnp; i++) {

            bounds[0] = Math.min(bounds[0], coords[0 + Node.L_COR * i]);
            bounds[1] = Math.min(bounds[1], coords[1 + Node.L_COR * i]);
            bounds[2] = Math.min(bounds[2], coords[2 + Node.L_COR * i]);

            bounds[3] = Math.max(bounds[3], coords[0 + Node.L_COR * i]);
            bounds[4] = Math.max(bounds[4], coords[1 + Node.L_COR * i]);
            bounds[5] = Math.max(bounds[5], coords[2 + Node.L_COR * i]);
        }

        return bounds;
    }


    /**
     * @return the numMembers
     */
    public int getNumMembers() {
        return numMembers;
    }


    /**
     * @return the memberNames
     */
    public String[] getMemberNames() {
        return memberNames;
    }


    /**
     * @return the Endianess of the files
     */
    public ByteOrder getEndianess() {
        return mEndianess;
    }


    /**
     * @return the nstates
     */
    public int getNumOfStates() {
        return nstates;
    }
}
