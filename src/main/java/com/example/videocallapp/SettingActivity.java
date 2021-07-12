package com.example.videocallapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;


public class SettingActivity extends AppCompatActivity {

    private Button saveBtn;
    private EditText usernameET, userBioET;
    private ImageView profileImageView;
    private static int galleryPick = 1;
    private Uri imageUri;
    private StorageReference userProfileImageRef;
    private String downloadUrl;
    private DatabaseReference userRef;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");
        userRef = FirebaseDatabase.getInstance().getReference().child("user");

        saveBtn = findViewById(R.id.save_setting_btn);
        userBioET = findViewById(R.id.bio_settings);
        usernameET = findViewById(R.id.username_settings);
        profileImageView = findViewById(R.id.setting_profile_image);
        progressDialog = new ProgressDialog(this);

        profileImageView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, galleryPick);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });

        retriveUserInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == galleryPick && resultCode == RESULT_OK && data != null){
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void saveUserData() {

        final String getUserName = usernameET.getText().toString();
        final String getUserStatus = userBioET.getText().toString();
        if(imageUri == null){
            // store name and status
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")){

                        saveInfoOnlyWithoutImage();

                    }else{
                        Toast.makeText(SettingActivity.this, "Hey Please select an Profile Image.", Toast.LENGTH_SHORT).show();
                    }

                }


                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }else if(getUserName.equals("")){
            Toast.makeText(this, "User name is mandatory", Toast.LENGTH_SHORT).show();
        }else if(getUserStatus.equals("")){
            Toast.makeText(this, "User Status is mandatory", Toast.LENGTH_SHORT).show();
        }else{

            progressDialog.setTitle("Account Setting");
            progressDialog.setMessage("Please wait ....");
            progressDialog.show();

            // save image name and status
            final StorageReference filePath = userProfileImageRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    downloadUrl = filePath.getDownloadUrl().toString();
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    if(task.isSuccessful()){
                        downloadUrl = task.getResult().toString();
                        final HashMap<String, Object> profileMap = new HashMap<>();
                        profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("name", getUserName);
                        profileMap.put("status", getUserStatus);
                        profileMap.put("image", downloadUrl);

                        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(
                                profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Intent intent = new Intent(SettingActivity.this, contectsActivity.class);
                                    startActivity(intent);
                                    finish();
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingActivity.this, "Profile Setting has been Updated successfully.", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(SettingActivity.this, "Some Error can be occured.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void saveInfoOnlyWithoutImage() {

        final String getUserName = usernameET.getText().toString();
        final String getUserStatus = userBioET.getText().toString();


        if(getUserName.equals("")){
            Toast.makeText(this, "User name is mandatory", Toast.LENGTH_SHORT).show();
        }else if(getUserStatus.equals("")){
            Toast.makeText(this, "User Status is mandatory", Toast.LENGTH_SHORT).show();
        }else{

            progressDialog.setTitle("Account Setting");
            progressDialog.setMessage("Please wait ....");
            progressDialog.show();


            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name", getUserName);
            profileMap.put("status", getUserStatus);

            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(
                    profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Intent intent = new Intent(SettingActivity.this, contectsActivity.class);
                        startActivity(intent);
                        finish();
                        progressDialog.dismiss();
                        Toast.makeText(SettingActivity.this, "Profile Setting has been Updated successfully.", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(SettingActivity.this, "Some Error can be occured.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void retriveUserInfo(){
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.exists()){
                            String imageDb = dataSnapshot.child("image").getValue().toString();
                            String nameDb = dataSnapshot.child("name").getValue().toString();
                            String bioDb = dataSnapshot.child("status").getValue().toString();

                            usernameET.setText(nameDb);
                            userBioET.setText(bioDb);
                            Picasso.get().load(imageDb).placeholder(R.drawable.profile_image).into(profileImageView);

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }
}
