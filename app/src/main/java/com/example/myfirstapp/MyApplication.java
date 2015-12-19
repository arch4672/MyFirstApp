package com.example.myfirstapp;

import android.app.Application;

public class MyApplication extends Application {


// Store the Family object so that it can be seen across the application

    private Family mFamily;

    /**
     * @return the mFamily
     */
    public Family getFamily() {
        return mFamily;
    }

    /**
     * @param f the Family to set
     */
    public void setFamily(Family f) {
        this.mFamily = f;
    }


}
