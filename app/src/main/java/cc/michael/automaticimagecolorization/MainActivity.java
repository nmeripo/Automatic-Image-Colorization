package cc.michael.automaticimagecolorization;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.algorithmia.Algorithmia;
import com.algorithmia.AlgorithmiaClient;
import com.algorithmia.algo.Algorithm;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import id.zelory.compressor.Compressor;
import id.zelory.compressor.FileUtil;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;



public class MainActivity extends AppCompatActivity implements View.OnClickListener /*  implementing click listener */ {

    // A constant to track the file chooser intent
    private static final int PICK_IMAGE_REQUEST = 234;

    // Buttons
    private Button buttonChoose;
    private Button buttonUpload;
    private Button buttonShow;
    private Button buttonColorize;
    private Button buttonSave;

    // Progress bar
    private ProgressBar spinner;

    // ImageView
    private ImageView imageView;

    // A Uri object to store file path
    private Uri filePath;

    // Firebase Authentication
    private FirebaseAuth mAuth;

    // Firebase Storage Reference
    private StorageReference storageReference;

    // File name to refer the image
    private String fileName;

    // Temp Image Files
    private File actualImage;
    private File compressedImage;
    private  Bitmap bitmap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();

        // Getting views from layout
        buttonChoose = (Button) findViewById(R.id.buttonChoose);
        buttonUpload = (Button) findViewById(R.id.buttonUpload);
        buttonColorize = (Button) findViewById(R.id.buttonColorize);
        buttonShow = (Button) findViewById(R.id.buttonShow);
        buttonSave= (Button) findViewById(R.id.buttonSave);


        imageView = (ImageView) findViewById(R.id.imageView);

        // Attaching click listener
        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
        buttonColorize.setOnClickListener(this);
        buttonShow.setOnClickListener(this);
        buttonSave.setOnClickListener(this);


        // Getting Firebase storage reference
        storageReference = FirebaseStorage.getInstance().getReference();


    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // All users are anonymous
        } else {
            signInAnonymously();
        }
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                // do something on success
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("Firebase Sign In", "onFailure:", exception);
                    }
                });
    }

    // Method to pick image from gallery
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Handling the image chooser activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            try {
                actualImage = FileUtil.from(this, data.getData());
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Method to upload the file to Google Cloud Storage using Firebase
    @SuppressWarnings("VisibleForTests")
    private void uploadFile(Uri filePath) {
        // If there is a file to upload

        if (filePath != null) {

            //Set fileName to timestamp
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
            fileName = simpleDateFormat.format(new Date()) + ".jpg";
            filePath = Uri.fromFile(compressedImage);
            final ProgressDialog progressDialog = new ProgressDialog(this);

            progressDialog.setTitle("Uploading");
            progressDialog.show();

            StorageReference riversRef = storageReference.child("images/" + fileName);
            riversRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Hide the progress dialog if upload is successful
                            progressDialog.dismiss();

                            // Success toast
                            Toast toast = Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 500);
                            toast.show();


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //If the upload is not successfull hide the progress dialog
                            progressDialog.dismiss();

                            //Error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // Progress percentage calculatioin
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            // Display progress percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }

    }

    @Override
    public void onClick(View view) {
        // If the clicked button is choose
        if (view == buttonChoose) {
            showFileChooser();
        }
        // If the clicked button is upload
        else if (view == buttonUpload) {

            Compressor.getDefault(this)
                    .compressToFileAsObservable(actualImage)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<File>() {
                        @Override
                        public void call(File file) {
                            compressedImage = file;
                            uploadFile(filePath);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            showError(throwable.getMessage());
                        }
                    });

        }

        // If the clicked button is colorize
        else if (view == buttonColorize) {

            Toast toast = Toast.makeText(getApplicationContext(), "Processing Started...\n Please wait until completion", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 500);
            toast.show();
            new ServletPostAsyncTask().execute(new Pair<Context, String>(this, fileName));
        }

        //if the clicked button is show
        else if (view == buttonShow) {
            new AsyncTask<Void, Void, Bitmap>() {

                @Override
                protected Bitmap doInBackground( Void... voids ) {
                    bitmap = createImage(512, 512);
                    try {
                        String message = fileName;
                        if (message != null) {
                            message.replace(".jpg", "");
                            int index = message.indexOf(".");
                            message = message.substring(0, index);
                            String colorUrl = "data://.algo/deeplearning/ColorfulImageColorization/temp/" + message + ".png";
                            AlgorithmiaClient client = Algorithmia.client("simMbgATg/yyhF6aiFnC8W2Hm1m1");
                            Algorithm algo = client.algo("algo://deeplearning/ColorfulImageColorization/1.1.6");
                            File colorFile = client.file(colorUrl).getFile();
                            String colorFilePath = colorFile.getPath();
                            Log.d("color URL", colorFilePath);
                            bitmap = BitmapFactory.decodeFile(colorFilePath);
                            Log.d("color URL", "bitmap done");
                        }
                    }
                    catch (IOException e) {
                        Log.d("Downlaod", e.toString());
                    }
                    return bitmap;
                }

                protected void onPostExecute(Bitmap result) {
                    imageView.setImageBitmap(bitmap);

                }

            }.execute();

        }

        else if (view == buttonSave) {

            String path_ = Environment.getExternalStorageDirectory().toString() + "/Colorize/";
            File path = new File(path_);

            if (!path.exists()) {
                path.mkdirs();
            }

            File dest = new File(path, fileName);


            try {
                FileOutputStream out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();

                Toast toast = Toast.makeText(getApplicationContext(), "Saved Image", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 500);
                toast.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    public static Bitmap createImage(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }


}