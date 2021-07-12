package com.example.videocallapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity

        implements Session.SessionListener,
        PublisherKit.PublisherListener,
        EasyPermissions.PermissionCallbacks
{

    private static String API_Key = "46631332";
    private static String SESSION_ID = "2_MX40NjYzMTMzMn5-MTU4NTY1NjE5MDUyOX5vLzlMN3F0SGVTWjFmY3ArZVR0OWltL05-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjYzMTMzMiZzaWc9ODQ5ZWQxMWRlZDFjZTM5YzQ5NTdiMTM2MzhmNWIzNDgxOWIxMzhhYzpzZXNzaW9uX2lkPTJfTVg0ME5qWXpNVE16TW41LU1UVTROVFkxTmpFNU1EVXlPWDV2THpsTU4zRjBTR1ZUV2pGbVkzQXJaVlIwT1dsdEwwNS1mZyZjcmVhdGVfdGltZT0xNTg1NjU2MjUyJm5vbmNlPTAuMjMxMzA2NjQyOTk5NDk5JnJvbGU9cHVibGlzaGVyJmV4cGlyZV90aW1lPTE1ODgyNDgyNTEmaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdD0=";
    private static final String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_VIDEO_APP_PERM = 124;

    private ImageView closeVideoChatBtn;
    private DatabaseReference userRef;
    private String userID = "";

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;

    private Session mSession;
    private Publisher mPublisher;

    private  Subscriber mSubscriber;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat2);

        requestPermission();

        userRef = FirebaseDatabase.getInstance().getReference().child("user");
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        closeVideoChatBtn = findViewById(R.id.close_video_chat_btn);


        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(userID).hasChild("Ringing")){

                            userRef.child(userID).child("Ringing").removeValue();
                            if( mPublisher != null){
                                mPublisher.destroy();
                            }
                            if( mSubscriber != null){
                                mSubscriber.destroy();
                            }
                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }

                        if(dataSnapshot.child(userID).hasChild("Calling")){
                            userRef.child(userID).child("Calling").removeValue();
                            if( mPublisher != null){
                                mPublisher.destroy();
                            }
                            if( mSubscriber != null){
                                mSubscriber.destroy();
                            }
                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }else{
                            if( mPublisher != null){
                                mPublisher.destroy();
                            }
                            if( mSubscriber != null){
                                mSubscriber.destroy();
                            }
                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, VideoChatActivity.this);

    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermission(){
        String[] perms = {Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        if(EasyPermissions.hasPermissions(this, perms)){
            mPublisherViewController = findViewById(R.id.publisher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);

            //mSession = new Session.Builder(this, API_Key, SESSION_ID).build();
            mSession = new Session.Builder(this, API_Key, SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);

        }else{
            EasyPermissions.requestPermissions(this, "Hey this app needs the Mice & Camera Permission", RC_VIDEO_APP_PERM);

        }
    }


    // 2 Piblishing stream to SESSion
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG,"Session Conntected");
        //mPublisher = new Publisher.Builder(this).build();
        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);
        mPublisherViewController.addView(mPublisher.getView());
        if(mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }
        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

        Log.i(LOG_TAG, "Stream Disconnected");

    }

    // 3 Subscribining
    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.i(LOG_TAG, "Stream Received");
        if(mSubscriber == null){
            //mSubscriber = new Subscriber.Builder(this, stream).build();
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewController.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if(mSubscriber != null){
            mSubscriber = null;
            mSubscriberViewController.removeAllViews();
        }


    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOG_TAG, "Stream Error");

    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }
}
