package com.example.marlonscheer.facedev;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class MainActivity extends AppCompatActivity {

    Button process;
    Button select;
    Button take;
    ImageView iv;
    Uri imageuri;
    Uri imageURI;

    static int PICK_IMAGE = 100;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        process = (Button) findViewById(R.id.button);

        select = (Button) findViewById(R.id.buttonSelect);

        iv = (ImageView) findViewById(R.id.imgview);

        take = (Button) findViewById(R.id.buttonTake);

        //Take a Picture
        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "New Picture");
                values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                imageURI = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageURI);
                startActivityForResult(intent, 101);

            }
        });


        //Select from gallery
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, PICK_IMAGE);
            }
        });

        //Process the picture
        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable=true;

                //Currently needs to be in res/mipmap-xxhdpi/ to work
                //No specific format needed
                //But hey it works so don't touch it yet :)

                    //Bitmap myBitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.mipmap.test, options);

                //Wow just fixed it with this line below

                Bitmap myBitmap = ((BitmapDrawable)iv.getDrawable()).getBitmap();

                //Creating Face rectangle

                Paint myRectPaint = new Paint();
                myRectPaint.setStrokeWidth(5);
                myRectPaint.setColor(Color.GREEN);
                myRectPaint.setStyle(Paint.Style.STROKE);

                //Create a temporary Bitmap

                Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);

                Canvas tempCanvas = new Canvas(tempBitmap);

                tempCanvas.drawBitmap(myBitmap, 0, 0, null);

                //Actually creating the Face Detection

                FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false).setLandmarkType(FaceDetector.ALL_LANDMARKS).build();
                if(!faceDetector.isOperational()){
                    new AlertDialog.Builder(view.getContext()).setMessage("Could not set up the face detector!").show();
                    return;
                }

                Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
                SparseArray<Face> faces = faceDetector.detect(frame);


                for (int i = 0; i < faces.size(); ++i) {
                    Face face = faces.valueAt(i);
                    for (Landmark landmark : face.getLandmarks()) {
                        int cx = (int) (landmark.getPosition().x);
                        int cy = (int) (landmark.getPosition().y);
                        tempCanvas.drawCircle(cx, cy, 10, myRectPaint);
                    }
                }

                for(int i=0; i<faces.size(); i++) {
                    Face thisFace = faces.valueAt(i);
                    float x1 = thisFace.getPosition().x;
                    float y1 = thisFace.getPosition().y;
                    float x2 = x1 + thisFace.getWidth();
                    float y2 = y1 + thisFace.getHeight();
                    tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                }


                //Setting rectangle over ImageView iv
                iv.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));

                //Make Crouton to say that the processing is finished
                Crouton.makeText(MainActivity.this, "Finished Processing", Style.CONFIRM).show();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("Requestcode: ",String.valueOf(requestCode));

        //If requestCode == 100 then open gallery
        //If requestCode == 101 then open Camera to take a picture
        if (requestCode == 100) {

            if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
                imageuri = data.getData();

                //Set Picture from gallery to ImageView iv
                iv.setImageURI(imageuri);
            }
        }else {


            //Take Picture with Camera

            String imageurl = getRealPathFromURI(imageURI);

            //Rotate the Picture

            try {
                Bitmap thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), imageURI);

                ExifInterface ei = new ExifInterface(imageurl);
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                Bitmap rotatedBitmap;
                switch(orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotatedBitmap = rotateImage(thumbnail, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotatedBitmap = rotateImage(thumbnail, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotatedBitmap = rotateImage(thumbnail, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        rotatedBitmap = thumbnail;
                }

                //Setting rotated Bitmap to ImageView iv
                iv.setImageBitmap(rotatedBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }



    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }



}
