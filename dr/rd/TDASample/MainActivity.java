// TDASample
// Copyright R&D Computer System Co.,Ltd
// 2016 Mar 15 V0.12 Release version

package dr.rd.TDASample;

/****************************** import Package ******************************/
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import rd.TDA.TDA;
/****************************************************************************/

public class MainActivity extends AppCompatActivity {

    TDA TDA;                                                        //Create Object TDA Class
    Button btReadCard, btExit;                                      //Create Object Button
    TextView txData, tv_tdaSample;                                  //Create Object Text View
    ImageView imageView;                                            //Create Object Image View
    private Handler handler = new Handler();                        //Create Object Handler for draw screen in thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

/******************************** Content View ********************************/
        setContentView(R.layout.activity_main);

/***************************** Initial TDA object *****************************/
        TDA = new TDA(this);
/******************************************************************************/

/**************************** get and set Version ****************************/
        tv_tdaSample = (TextView) findViewById(R.id.tv_tdaSample);      //Initial Header Text view
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);    //get Info of Package
            String version = pInfo.versionName;                         //get Version from Info
            tv_tdaSample.setText("TDASample " + version);               //Set Text Version on Screen
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

/****************************** Button Read Card *****************************/
        btReadCard = (Button) findViewById(R.id.btReadCard);            //Initial Read Button

/*********************** Event Click Button Read Card ***********************/
        btReadCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btExit.setEnabled(false);                               //Disable Exit Button
                btExit.setBackgroundColor(Color.GRAY);                  //Set Color Exit Button

/***************** Create Thread for Read Card And Set Text *****************/
                Thread thread = new Thread(new Runnable() {
                    byte[] Photo;
                    Bitmap bPhoto;
                    String Data;
                    @Override
                    public void run() {

                        //clear Screen
                        handler.post(new Runnable() {
                            public void run() {
                                txData.setText("");                     //Clear Data Text  on Screen
                                imageView.setImageBitmap(null);         //Clear Photo on Screen
                            }
                        });

                        //Read Text from NID card
                        Data = TDA.nidTextTA("0");                      //ReadText
                        if (Data.compareTo("-2") == 0) {                //Check if un-registered reader
                            TDA.serviceTA("2");                         //Update license file
                            Data = TDA.nidTextTA("0");                  //Read Text Again
                        }

                        handler.post(new Runnable() {
                            public void run() {
                                txData.setText(Data);                   //Set Data Text on Screen
                            }
                        });

                        //Read Photo from NID card
                        Photo = TDA.nidPhotoTA("0");                     //Read Photo
                        bPhoto = BitmapFactory.decodeByteArray(Photo, 0, Photo.length);     // Decode Byte Array to Bitmap
                        handler.post(new Runnable() {
                            public void run() {
                                imageView.setImageBitmap(bPhoto);       //set Bitmap on Screen
                                btExit.setEnabled(true);                //Enable Exit Button
                                btExit.setBackgroundColor(Color.RED);   //set Color Exit Button
                            }
                        });
                    }
                });
                thread.start();
            }
        });

/********************************* Exit Buton *********************************/
        btExit = (Button) findViewById(R.id.btExit);                    //Initial Exit Button

/*************************** Event Click Exit Button ***************************/
        btExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Exit();                                                 //Method Exit App

            }
        });

        txData = (TextView) findViewById(R.id.txData);                  //Initial Text View for Data
        imageView = (ImageView) findViewById(R.id.imageView);           //Initial Image View for Photo

        startProcess();                                                 //Method Init App
        searchBluetooth();                                              //Method Search Bluetooth //Remove this line if Bluetooth not used
    }

/******************************* Event Click Back *******************************/
    @Override
    public void onBackPressed() {
        Exit();                                                         //Method Exit App
    }

/***************************** Method Exit Application *****************************/
    private void Exit() {
        Intent intent = new Intent(Intent.ACTION_MAIN);                 //Initial Intent
        intent.addCategory(Intent.CATEGORY_HOME);                       //Add Category for Intent
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);                 //Set Flag for Intent
        startActivity(intent);                                          //Start Activity by intent //go to Main
        finish();                                                       //finish for this App //Close App
        TDA.serviceTA("0");                                             //Close service before exit
    }

/*************************************** Init ***************************************/
    private void startProcess(){
        TDA.serviceTA("0");                                             //Close previous service if exist
        while (TDA.serviceTA("9").compareTo("00") != 0);                //Wait until service closed
        TDA.serviceTA("1,TDASample");                                   //Start TDAService with “TDASample”
        while (TDA.serviceTA("9").compareTo("01") != 0);                //Wait until service started

        //Check license file
        String check = TDA.infoTA("4");                                 //Test Command
        Log.i("Check", "check = " + check);                             //Print Log

/************************** check recieve data is Error Code **************************/
// -2 = INVALID LICENSE
// -12 = LICENSE FILE ERROR
        if (check.compareTo("-2") == 0 || check.compareTo("-12") == 0) {
            if (isOnline()) {                                           //Method Check Internet
                TDA.serviceTA("2");                                     //Update license file
            }
        }
    }

/*************************** Start Bluetooth auto scanning ***************************/
    private void searchBluetooth() {
        String result = TDA.readerTA("2");                              //Auto scan Bluetooth reader
        if (result.compareTo("02") == 0) {                              //Check Result //02 = Card Present
            Toast.makeText(this, "Search Blutooth", Toast.LENGTH_SHORT).show();     //Show balloon
        }

    }

/********************************** Check Internet **********************************/
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);     //initail Object Connect Manager
        NetworkInfo netInfo = cm.getActiveNetworkInfo();                //get Network Info
        return netInfo != null && netInfo.isConnectedOrConnecting();    //Check Network and Return
    }

}
