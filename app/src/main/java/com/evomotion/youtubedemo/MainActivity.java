package com.evomotion.youtubedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button youtubeShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        youtubeShare = (Button) findViewById(R.id.youtube_share);
        youtubeShare.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        toShareActivity();
    }

    private void toShareActivity() {
        Intent intent = new Intent(this, ShareActivity.class);
        startActivity(intent);
    }
}
