package com.tcs.experiment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;
import static com.tcs.experiment.Main2Activity.EXTRA_MESSAGE;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

// Variable for Text to Speech
    TextToSpeech textToSpeech;

    /*3 Variables for barcode scanner
     */
    TextView barcodeInfo;   //1st variable
    TextView barcodeInfoMaterial; //2nd variable
    View view;    //3rd variable

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String ACTION_SEARCH="action";
    private static final String MENU_SEARCH = "menu";

    private static final int CAMERA_REQUEST = 1888;

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "oh mighty computer";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        captions = new HashMap  <String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(DIGITS_SEARCH, R.string.digits_caption);
        captions.put(PHONE_SEARCH, R.string.phone_caption);
        captions.put(ACTION_SEARCH, R.string.action_caption);
        captions.put(FORECAST_SEARCH, R.string.forecast_caption);
        setContentView(R.layout.activity_main);
        final float f= (float) 0.7;
        //Text To Speech
        textToSpeech= new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int status){
                if(status!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setSpeechRate(f);
                }
            }
        });
        //((TextView) findViewById(R.id.caption_text)).setText("Preparing the recognizer");
        //final_text=(TextView) findViewById(R.id.final_text);

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    //((TextView) findViewById(R.id.caption_text)).setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if  (text.equals(DIGITS_SEARCH)){
            switchSearch(DIGITS_SEARCH);
            System.out.println("*************************************************digits******************************************");}
        else if (text.equals(PHONE_SEARCH)){
            switchSearch(PHONE_SEARCH);
            System.out.println("**************************************************phone**************************************");}
        else if (text.equals(FORECAST_SEARCH)){
            switchSearch(FORECAST_SEARCH);
            System.out.println("********************************************forecast*********************************************");}

        else if (text.equals(ACTION_SEARCH)) {
            switchSearch(ACTION_SEARCH);
            System.out.println("********************************************action*********************************************");

        }


//        else
//            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        //((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            //final_text.setText(text);
            //if(final_text.getText().toString().equals("open camera"))
            if(text.equals("scan transfer")){
                transferOrder(view);
            }
            if(text.equals("scan material")){
                materialOrder(view);
            }
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else {
            try {
                recognizer.startListening(searchName, 10000);
            }catch(Exception e){
                System.out.println(e+"########################################################################");
            }
        }

        //String caption = getResources().getString(captions.get(searchName));
        //((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);



        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        File actionGrammar = new File(assetsDir, "action.gram");
        recognizer.addGrammarSearch(ACTION_SEARCH, actionGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    @Override
    public void onError(Exception error) {
        //((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    public void takeImageFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }




/* Coding for Bar Code Scanner***************************************************************************************************************
*/

    public void transferOrder(View view){
        Intent intent_i = new Intent(this,Main2Activity.class);
        startActivityForResult(intent_i, 1);
    }

    public void materialOrder(View view){
        Intent intent_i = new Intent(this,Main2Activity.class);
        startActivityForResult(intent_i, 2);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK) {
                String strEditTextt = data.getStringExtra(EXTRA_MESSAGE);
                barcodeInfo=(TextView) findViewById(R.id.transferText);
                barcodeInfo.setText(strEditTextt);
                Toast.makeText(getApplicationContext(),"Scanning Done For Transfer Order",Toast.LENGTH_SHORT).show();
                textToSpeech.speak("scanning done, and the barcode is"+strEditTextt, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
        if (requestCode == 2) {
            if(resultCode == RESULT_OK) {
                String strEditTextm = data.getStringExtra(EXTRA_MESSAGE);
                barcodeInfoMaterial=(TextView) findViewById(R.id.materialText);
                barcodeInfoMaterial.setText(strEditTextm);
                Toast.makeText(getApplicationContext(),"Scanning Done For Material",Toast.LENGTH_SHORT).show();
                textToSpeech.speak("scanning done, and the barcode is"+strEditTextm, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }


    @Override
    public void onStop(){
        super.onStop();
        textToSpeech.stop();
    }
}