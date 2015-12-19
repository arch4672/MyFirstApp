
#include <string.h>
#include <math.h>
#include <jni.h>
#include <android/log.h>
#include "utils.h"

#define  DEBUG    0

#define  LOG_TAG    "JNI_UTILS"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define  OK    1

#define  DISP_X   0    // X Displacement
#define  DISP_Y   1    // Y Displacement
#define  DISP_Z   2    // Z Displacement
#define  DISP_R   3    // Resultant Displacement

#define  L_COR    3

#define  L_SH_TOP 5

#define  X        0
#define  Y        1
#define  Z        2

#define NUM_CONTOUR_LEVELS    24

static float contour_values[24];


jint
Java_com_example_myfirstapp_MyRenderer_populatePartVertexData( JNIEnv* env,
                                                               jobject thiz,
                                                               jint numEls,             // Number of elements in part
                                                               jobject bufUndef,        // All undeformed coordinates
                                                               jobject bufCoords,       // All current coordinates
                                                               jobject bufShTop,        // All shell topology
                                                               jintArray elsInPart,     // List of internal element indices in part
                                                               jobject bufVertexData,   // Vertex data buffer to fill
                                                               jboolean siPlot,         // Flag whether to do SI plot or not
                                                               jint eswap)
{
// TODO - 1. Contour values should not be hard-coded...
//        2. ElsInPart -> pass as IntBuffer rather then int Array??
//        3. Call OpenGL functions in native code...
//        4. Pass size of vertexData buffer -> check we don't overflow it

    if(DEBUG) LOGD("Called populatePartVertexData");

    int   i, j, el, pid, prev_pid, offsetVtx;
    int   top[4];
    float x[4], y[4], z[4];
    float dm[4];
    float norm[3];
    float r, g, b;


// TODO Should check for null

    jint* pElsInPart = (*env)->GetIntArrayElements(env, elsInPart, 0);

    int*   pbufShTop      = (int *)  (*env)->GetDirectBufferAddress( env, (jobject)bufShTop      );
    float* pbufVertexData = (float *)(*env)->GetDirectBufferAddress( env, (jobject)bufVertexData );
    float* pbufUndef      = (float *)(*env)->GetDirectBufferAddress( env, (jobject)bufUndef      );
    float* pbufCoords     = (float *)(*env)->GetDirectBufferAddress( env, (jobject)bufCoords     );


    jlong len = (*env)->GetDirectBufferCapacity(env, (jobject)bufShTop);

// Put required data into pbufVertexData

    offsetVtx = 0;
    prev_pid = -1;

    for(i=0; i<numEls; i++)
    {
        el = pElsInPart[i];

// Get topology and coordinates for this elements

        for(j=0; j<4; j++)
        {
            top[j] = pbufShTop[j + L_SH_TOP*el];
            if(eswap) do_eswap((unsigned char *)&top[j]);
            top[j]--;

            x[j] = pbufCoords[L_COR*top[j] + X];
            y[j] = pbufCoords[L_COR*top[j] + Y];
            z[j] = pbufCoords[L_COR*top[j] + Z];

            if(eswap)
            {
              do_eswap((unsigned char *)&x[j]);
              do_eswap((unsigned char *)&y[j]);
              do_eswap((unsigned char *)&z[j]);
            }
        }

// Internal part ID for this element

        pid = pbufShTop[4 + L_SH_TOP*el];
        if(eswap) do_eswap((unsigned char *)&pid);
        pid--;

// Get element normal

        get_sh_norm(x, y, z, norm);


// Get colours. Calculate data value if contouring

        if (siPlot)
        {
            for(j=0; j<4; j++)
            {
                dm[j] = get_node_disp(x[j], y[j], z[j], pbufUndef, top[j], DISP_R, eswap);
            }

            get_contour_colour(dm[0], &r, &g, &b);
        }
        else
        {
            // Colour by part

            if(pid != prev_pid) {
                get_part_default_colour(pid, &r, &g, &b);
                prev_pid = pid;
            }
        }

// Populate vertex data buffer

        pbufVertexData[offsetVtx++] = x[0];	     pbufVertexData[offsetVtx++] = y[0];      pbufVertexData[offsetVtx++] = z[0];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;

        pbufVertexData[offsetVtx++] = x[1];	     pbufVertexData[offsetVtx++] = y[1];      pbufVertexData[offsetVtx++] = z[1];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];

        if (siPlot) get_contour_colour(dm[1], &r, &g, &b);
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;

        pbufVertexData[offsetVtx++] = x[2];	     pbufVertexData[offsetVtx++] = y[2];      pbufVertexData[offsetVtx++] = z[2];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];

        if (siPlot) get_contour_colour(dm[2], &r, &g, &b);
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;

        pbufVertexData[offsetVtx++] = x[2];	     pbufVertexData[offsetVtx++] = y[2];      pbufVertexData[offsetVtx++] = z[2];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;

        pbufVertexData[offsetVtx++] = x[3];	     pbufVertexData[offsetVtx++] = y[3];      pbufVertexData[offsetVtx++] = z[3];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];

        if (siPlot) get_contour_colour(dm[3], &r, &g, &b);
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;

        pbufVertexData[offsetVtx++] = x[0];	     pbufVertexData[offsetVtx++] = y[0];      pbufVertexData[offsetVtx++] = z[0];
        pbufVertexData[offsetVtx++] = norm[X];   pbufVertexData[offsetVtx++] = norm[Y];   pbufVertexData[offsetVtx++] = norm[Z];

        if (siPlot) get_contour_colour(dm[0], &r, &g, &b);
        pbufVertexData[offsetVtx++] = r;         pbufVertexData[offsetVtx++] = g;         pbufVertexData[offsetVtx++] = b;
    }


// Release the JAVA array copy
    (*env)->ReleaseIntArrayElements(env, elsInPart, pElsInPart, 0);


    return OK;
}



void do_eswap(unsigned char *buffer)
{
    int i;

    unsigned char b0, b1;

    b0 = buffer[0];
    b1 = buffer[1];

    buffer[0] = buffer[3];
    buffer[1] = buffer[2];

    buffer[2] = b1;
    buffer[3] = b0;
}



jint
Java_com_example_myfirstapp_MyRenderer_updateContourLimits( JNIEnv* env,
                                                            jobject thiz,
                                                            jobject bufUndef,        // All undeformed coordinates
                                                            jobject bufCoords,       // All current coordinates
                                                            jint    numNodes,
                                                            jint    eswap)
{
// Updates the contour bar values based on current state values
//

    if(DEBUG) LOGD("Called updateContourLimits");

    int     i;
    float   x, y, z, val, inc, max = -1e20;

// TODO - check for NULL

    float* pbufUndef  = (float *)(*env)->GetDirectBufferAddress( env, (jobject)bufUndef  );
    float* pbufCoords = (float *)(*env)->GetDirectBufferAddress( env, (jobject)bufCoords );

// Calc max value
// Assume plotting DISP_R for now... so limits will go from 0 to max
// And assumes all nodes are being plotted...

    for(i=0; i<numNodes; i++)
    {
        x = pbufCoords[X + i * L_COR];
        y = pbufCoords[Y + i * L_COR];
        z = pbufCoords[Z + i * L_COR];

        if(eswap)
        {
            do_eswap((unsigned char *)&x);
            do_eswap((unsigned char *)&y);
            do_eswap((unsigned char *)&z);
        }

        val = get_node_disp(x, y, z, pbufUndef, i, DISP_R, eswap);

        if(val > max) max = val;

    }

// Set contour levels

    inc = max / (NUM_CONTOUR_LEVELS - 1);
    val = 0.0;

    for(i=0; i<NUM_CONTOUR_LEVELS; i++)
    {
        contour_values[i] = val;

        val += inc;
    }

    return OK;
}



float get_node_disp(float x, float y, float z, float *undef, int n, int type, int eswap)
{
// <x>,<y>,<z>  Current coords
// <undef>      Undeformed coords
// <n>          Internal node

    float ux, uy, uz, dx, dy, dz, val;

    switch(type)
    {
        case DISP_X:  { ux = undef[X + L_COR * n];  if(eswap) do_eswap((unsigned char *)&ux);  val = x - ux;    break; }
        case DISP_Y:  { uy = undef[Y + L_COR * n];  if(eswap) do_eswap((unsigned char *)&uy);  val = y - uy;    break; }
        case DISP_Z:  { uz = undef[Z + L_COR * n];  if(eswap) do_eswap((unsigned char *)&uz);  val = z - uz;    break; }

        case DISP_R:  { ux = undef[X + L_COR * n];
                        uy = undef[Y + L_COR * n];
                        uz = undef[Z + L_COR * n];

                        if(eswap)
                        {
                            do_eswap((unsigned char *)&ux);
                            do_eswap((unsigned char *)&uy);
                            do_eswap((unsigned char *)&uz);
                        }

                        dx = x - ux;
                        dy = y - uy;
                        dz = z - uz;

                        val = sqrtf(dx * dx + dy * dy + dz * dz);

                        break;
                      }

        default: val = 0.0;
    }

    return val;
}



void  get_sh_norm(float *x, float *y, float *z, float *norm)
{
// Calculate shell Normal
//
// Don't normalise the normal vector here, the sqrt() calls are expensive
// it gets normalised in the shader

    float dx1, dy1, dz1;
    float dx2, dy2, dz2;

    dx1 = x[2] - x[0];
    dy1 = y[2] - y[0];
    dz1 = z[2] - z[0];

    dx2 = x[3] - x[1];
    dy2 = y[3] - y[1];
    dz2 = z[3] - z[1];

    norm[X] = dy1*dz2 - dz1*dy2;
    norm[Y] = dz1*dx2 - dx1*dz2;
    norm[Z] = dx1*dy2 - dy1*dx2;

    return;
}



void get_part_default_colour(int ipart, float *r, float *g, float *b)
{
    switch (ipart % 13) {

                case 0:  { *r=1.0f;  *g=0.0f;  *b=0.0f;  break; }
                case 1:  { *r=0.0f;  *g=1.0f;  *b=0.0f;  break; }
                case 2:  { *r=0.0f;  *g=0.0f;  *b=1.0f;  break; }
                case 3:  { *r=0.0f;  *g=1.0f;  *b=1.0f;  break; }
                case 4:  { *r=1.0f;  *g=0.0f;  *b=1.0f;  break; }
                case 5:  { *r=1.0f;  *g=1.0f;  *b=0.0f;  break; }
                case 6:  { *r=1.0f;  *g=0.0f;  *b=0.58f; break; }
                case 7:  { *r=1.0f;  *g=0.75f; *b=0.0f;  break; }
                case 8:  { *r=0.66f; *g=1.0f;  *b=0.0f;  break; }
                case 9:  { *r=0.0f;  *g=1.0f;  *b=0.66f; break; }
                case 10: { *r=0.0f;  *g=0.5f;  *b=1.0f;  break; }
                case 11: { *r=1.0f;  *g=0.5f;  *b=0.0f;  break; }
                case 12: { *r=0.0f;  *g=0.75f; *b=1.0f;  break; }
            }

    return;
}



void get_contour_colour(float val, float *r, float *g, float *b)
{
// Returns the r, g, b contour colour for <val>

/*
         if(val < contour_values[0]) { *r=0.0f;  *g=0.0f;  *b=1.0f;  }
    else if(val < contour_values[1]) { *r=0.0f;  *g=0.5f;  *b=1.0f;  }
    else if(val < contour_values[2]) { *r=0.0f;  *g=0.75f; *b=1.0f;  }
    else if(val < contour_values[3]) { *r=0.0f;  *g=1.0f;  *b=1.0f;  }
    else if(val < contour_values[4]) { *r=0.0f;  *g=1.0f;  *b=0.66f; }
    else if(val < contour_values[5]) { *r=0.0f;  *g=1.0f;  *b=0.0f;  }
    else if(val < contour_values[6]) { *r=0.75f; *g=1.0f;  *b=0.0f;  }
    else if(val < contour_values[7]) { *r=1.0f;  *g=1.0f;  *b=0.0f;  }
    else if(val < contour_values[8]) { *r=1.0f;  *g=0.75f; *b=0.0f;  }
    else if(val < contour_values[9]) { *r=1.0f;  *g=0.5f;  *b=0.0f;  }
    else if(val < contour_values[10]){ *r=1.0f;  *g=0.0f;  *b=0.0f;  }
    else if(val < contour_values[11]){ *r=1.0f;  *g=0.0f;  *b=0.58f; }
    else                             { *r=1.0f;  *g=0.0f;  *b=1.0f;  }
*/

         if(val < contour_values[0])  { *r=0.0f;    *g=0.0f;    *b=1.0f;   }
    else if(val < contour_values[1])  { *r=0.0f;    *g=0.25f;   *b=1.0f;   }
    else if(val < contour_values[2])  { *r=0.0f;    *g=0.5f;    *b=1.0f;   }
    else if(val < contour_values[3])  { *r=0.0f;    *g=0.625f;  *b=1.0f;   }
    else if(val < contour_values[4])  { *r=0.0f;    *g=0.75f;   *b=1.0f;   }
    else if(val < contour_values[5])  { *r=0.0f;    *g=0.875f;  *b=1.0f;   }
    else if(val < contour_values[6])  { *r=0.0f;    *g=1.0f;    *b=1.0f;   }
    else if(val < contour_values[7])  { *r=0.0f;    *g=1.0f;    *b=0.88f;  }
    else if(val < contour_values[8])  { *r=0.0f;    *g=1.0f;    *b=0.66f;  }
    else if(val < contour_values[9])  { *r=0.0f;    *g=1.0f;    *b=0.33f;  }
    else if(val < contour_values[10]) { *r=0.0f;    *g=1.0f;    *b=0.0f;   }
    else if(val < contour_values[11]) { *r=0.375f;  *g=1.0f;    *b=0.0f;   }
    else if(val < contour_values[12]) { *r=0.75f;   *g=1.0f;    *b=0.0f;   }
    else if(val < contour_values[13]) { *r=0.875f;  *g=1.0f;    *b=0.0f;   }
    else if(val < contour_values[14]) { *r=1.0f;    *g=1.0f;    *b=0.0f;   }
    else if(val < contour_values[15]) { *r=1.0f;    *g=0.875f;  *b=0.0f;   }
    else if(val < contour_values[16]) { *r=1.0f;    *g=0.75f;   *b=0.0f;   }
    else if(val < contour_values[17]) { *r=1.0f;    *g=0.625f;  *b=0.0f;   }
    else if(val < contour_values[18]) { *r=1.0f;    *g=0.5f;    *b=0.0f;   }
    else if(val < contour_values[19]) { *r=1.0f;    *g=0.25f;   *b=0.0f;   }
    else if(val < contour_values[20]) { *r=1.0f;    *g=0.0f;    *b=0.0f;   }
    else if(val < contour_values[21]) { *r=1.0f;    *g=0.0f;    *b=0.29f;  }
    else if(val < contour_values[22]) { *r=1.0f;    *g=0.0f;    *b=0.58f;  }
    else                              { *r=1.0f;    *g=0.0f;    *b=1.0f;   }


    return;
}