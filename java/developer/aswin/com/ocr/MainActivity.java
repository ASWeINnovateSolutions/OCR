package developer.aswin.com.ocr;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int TAKE_PHOTO = 1;
    ImageView imageView;
    EditText textView;
    Bitmap imageBitmap;
    public String sentiment_text, completion_status;
    public Button button1, button2,sentiment;
    public static String text;
    DatabaseReference ref1;
    public static final int REQUEST_IMAGE_CAPTURE = 1777;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        sentiment = findViewById(R.id.sentiment);
        sentiment.setVisibility(View.GONE);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("flag").child("state");
        ref.setValue("false");
        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference().child("response").child("completion_status");
        ref1.setValue("false");
        ref1 = FirebaseDatabase.getInstance().getReference().child("response");
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                String status = dataSnapshot.child("completion_status").getValue().toString();
                if(status.equals("true")){
                    sentiment.setVisibility(View.VISIBLE);
                    sentiment_text = dataSnapshot.child("sentiment").getValue().toString();
                }
                // ...
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                // Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        };
        ref1.addValueEventListener(postListener);

    }

    public void pickImage(View v) {
        switch (v.getId()) {
            case R.id.button1:
                dispatchTakePictureIntent();
                break;
        }
    }

    public void loadFromGallery(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(intent, 101);
    }

    public void detect(View v) {
        if (imageBitmap == null) {
            Toast.makeText(this, "Image Not taken!!", Toast.LENGTH_SHORT).show();
        } else {
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);
            final FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            detector.processImage(firebaseVisionImage)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                            // Task completed successfully
                            // ...
                            processText(firebaseVisionText);
                        }
                    })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //----- No Text
                                }
                            });
        }
    }

    private void processText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();
        if (blocks.size() == 0) {
            Toast.makeText(this, "No Text Detected!!", Toast.LENGTH_SHORT).show();
        } else {
            text = " ";
            for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
                text = text + block.getText();
            }
            textView.setText(text);
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("messages").child("message");
            ref.setValue(text);
            DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference().child("flag").child("state");
            ref1.setValue("true");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);
        }

        if (requestCode == 101 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                imageView.setImageBitmap(imageBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fetchData(View view) {
        sentiment.setVisibility(View.GONE);
        Toast.makeText(this, ""+sentiment_text, Toast.LENGTH_SHORT).show();
        new AlertDialog.Builder(this)
                .setTitle("Sentiment")
                .setMessage("The Captures text is "+sentiment_text)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference().child("response").child("completion_status");
        ref1.setValue("false");
    }
}
