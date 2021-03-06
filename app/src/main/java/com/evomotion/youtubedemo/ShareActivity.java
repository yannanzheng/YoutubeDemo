package com.evomotion.youtubedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by 1510019 on 2017/7/11.
 */

public class ShareActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "ShareActivity";
    private Button youtubeLogin;
    private Button youtubeUpload;
    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_AUTHORIZATION_LIST_EVENT = 9002;
    private static final int REQUEST_AUTHORIZATION = 9003;

    final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = new GsonFactory();
    public static final String[] SCOPES = {Scopes.PROFILE, YouTubeScopes.YOUTUBE,YouTubeScopes.YOUTUBE_UPLOAD};
    private GoogleSignInAccount acct;
    private GoogleAccountCredential credential;
    private YouTube youtube;
    private Button youtubeLoginOut;
    private Button youtubeDisconnect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_activity);
        initView();
        initGoogleSignClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        silentSignIn();
    }

    private void silentSignIn() {
    /*自动登录*/
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d("xxx", "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            if (handleSignInResult(result)) {
                Log.d("xxx", "onstart, result = "+result);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult result = "+ requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
            Log.d(TAG,"displayName = "+result.getSignInAccount().getDisplayName() );
            Log.d(TAG,"email = "+result.getSignInAccount().getEmail() );
            Log.d(TAG,"givenName = "+result.getSignInAccount().getGivenName() );
            Log.d(TAG,"idToken = "+result.getSignInAccount().getIdToken() );
            Log.d(TAG,"photoUrl = "+result.getSignInAccount().getPhotoUrl().toString() );
            Log.d(TAG,"status = "+result.getStatus().getStatus().toString() );
        }
        if(requestCode == REQUEST_AUTHORIZATION) {
            performUploadClick();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG,"连接服务器失败");
    }

    private void initGoogleSignClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(ShareActivity.this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void initView() {
        youtubeLogin = (Button) findViewById(R.id.youtube_login);//登陆
        youtubeUpload = (Button) findViewById(R.id.youtube_upload);//上传按钮
        youtubeLoginOut = (Button) findViewById(R.id.youtube_loginout);//注销
        youtubeDisconnect = (Button) findViewById(R.id.youtube_disconnect_button);//断开连接
        youtubeLogin.setOnClickListener(this);
        youtubeUpload.setOnClickListener(this);
        youtubeLoginOut.setOnClickListener(this);
        youtubeDisconnect.setOnClickListener(this);
    }

    private boolean handleSignInResult(GoogleSignInResult result) {
        Log.e(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            //Status{statusCode=unknown status code: 12501, resolution=null}
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();
            credential = GoogleAccountCredential.usingOAuth2(
                    getApplicationContext(), Arrays.asList(SCOPES));
            credential.setSelectedAccount(acct.getAccount());
            youtube = new YouTube
                    .Builder(transport, jsonFactory, credential)
                    .setApplicationName("YoutubeDemo")
                    .build();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.youtube_login:
                performLoginClick();
                break;
            case R.id.youtube_upload:
                performUploadClick();
                break;
            case R.id.youtube_loginout:
                performLoginOut();
                break;
            case R.id.youtube_disconnect_button:
                performDisconnect();
                break;
        }
    }

    private void performDisconnect() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        Log.e("xxx","performDisconnect state:"+status.isSuccess());
                        // [END_EXCLUDE]
                    }
                });
    }

    private void performLoginOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        Log.e("xxx","performLoginOut state:"+status.isSuccess());
                        // [END_EXCLUDE]
                    }
                });
    }

    private void performUploadClick() {
        MyUploadThread myUploadThread = new MyUploadThread();
        myUploadThread.start();
    }

    private void performLoginClick() {
        Log.d(TAG, "performLoginClick");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        Log.d(TAG, "signInIntent = " + signInIntent);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    class MyUploadThread extends Thread {
        @Override
        public void run() {
            super.run();
            uploadVideo();
        }
    }

    private static final String VIDEO_FILE_FORMAT = "video/*";
    private static final String SAMPLE_VIDEO_FILENAME = "myvideo.mp4";

    private void uploadVideo() {
        try {
            Video videoObjectDefiningMetadata = new Video();
            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus("public");
            videoObjectDefiningMetadata.setStatus(status);
            VideoSnippet snippet = new VideoSnippet();
            Calendar cal = Calendar.getInstance();
            snippet.setTitle("2从ShareActivity分享过来的，你懂得 " + cal.getTime());
            snippet.setDescription(
                    "从ShareActivity分享过来的，你懂得" + "on " + cal.getTime());

            List<String> tags = new ArrayList<String>();
            tags.add("java");
            snippet.setTags(tags);
            videoObjectDefiningMetadata.setSnippet(snippet);
            InputStream open = getAssets().open(SAMPLE_VIDEO_FILENAME);
            int available = open.available();
            Log.e("xxx","available:"+available);
            InputStreamContent mediaContent = new InputStreamContent(VIDEO_FILE_FORMAT, new BufferedInputStream(open));
            mediaContent.setLength(available);
            Log.e("xxx","available:"+mediaContent.getLength());
            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert("snippet,statistics,status", videoObjectDefiningMetadata, mediaContent);

            MediaHttpUploader uploader = videoInsert.getMediaHttpUploader();

            uploader.setDirectUploadEnabled(false);
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);
            MediaHttpUploaderProgressListener progressListener = new MediaHttpUploaderProgressListener() {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            Log.e("xxx","Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            Log.e("xxx","Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            Log.e("xxx","Upload in progress");
                            Log.e("xxx","Upload percentage: " + uploader.getProgress());
                            break;
                        case MEDIA_COMPLETE:
                            Log.e("xxx","progress : " + uploader.getProgress());
                            Log.e("xxx","Upload Completed!");
                            break;
                        case NOT_STARTED:
                            Log.e("xxx","Upload Not Started!");
                            break;
                    }
                }
            };
            uploader.setProgressListener(progressListener);

            Video returnedVideo = videoInsert.execute();

            // Print data about the newly inserted video from the API response.
            Log.e("xxx","\n================== Returned Video ==================\n");
            Log.e("xxx","  - Id: " + returnedVideo.getId());
            Log.e("xxx","  - Title: " + returnedVideo.getSnippet().getTitle());
            Log.e("xxx","  - Tags: " + returnedVideo.getSnippet().getTags());
            Log.e("xxx","  - Privacy Status: " + returnedVideo.getStatus().getPrivacyStatus());
            Log.e("xxx","  - Video Count: " + returnedVideo.getStatistics().getViewCount());

        } catch (GoogleJsonResponseException e) {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();
        } catch (UserRecoverableAuthIOException e) {
            startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }

}
