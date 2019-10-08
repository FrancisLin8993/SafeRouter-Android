package com.example.saferouter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutPageActivity extends AppCompatActivity {

    @BindView(R.id.about_close_button)
    Button closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_page);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.about_close_button)
    public void closeButtonOnClick(){
        finish();
    }
}
