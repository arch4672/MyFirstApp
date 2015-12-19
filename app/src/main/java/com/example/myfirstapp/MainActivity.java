package com.example.myfirstapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.d("onCreate", "maxMemory:" + Long.toString(maxMemory));
        */
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        Log.d("onCreate", "memoryClass:" + Integer.toString(memoryClass));


        setContentView(R.layout.activity_main);
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        TextView textView = (TextView) findViewById(R.id.textView1);
        textView.setText("Select File:");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }



    /** Called when the user clicks the Read button */
    public void readFile(View view) {

        TextView textView = (TextView) findViewById(R.id.textView1);

        // Get filename
        EditText editText = (EditText) findViewById(R.id.editText1);

        String fname = editText.getText().toString();


        // Check if it exists...
        if (!(new File(fname)).exists())
        {
            textView.setText("File does not exist...");
            return;
        }



        // Get a Family object

        final Family myFamily = new Family(fname);

        // Store in Application so other parts of the App can see it

        MyApplication myApp = (MyApplication) getApplication();
        myApp.setFamily(myFamily);

        // Get OpenGL activity

        final Intent intent = new Intent(this, GraphicsWindow.class);

        //  User feedback when reading file

        textView.setText("Reading File...");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.VISIBLE);

        // Open Family file in separate thread, reading required data, then start OpenGL activity

        new Thread(new Runnable() {
            public void run() {
                try {
                    myFamily.openFamily();

                    startActivity(intent);

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    Context context = getApplicationContext();
                    String text = e.getMessage();
                    int duration = Toast.LENGTH_LONG;

                    Toast.makeText(context, text, duration).show();
                }

            }
        }).start();
    }



    /** Quick way to set filename to read */
    public void setFilename(View view) {

        ((TextView) findViewById(R.id.editText1)).setText(((Button) view).getText());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
