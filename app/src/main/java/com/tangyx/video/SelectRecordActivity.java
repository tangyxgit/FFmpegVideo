package com.tangyx.video;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/**
 * Created by tangyx
 * Date 2017/12/4
 * email tangyx@live.com
 */

public class SelectRecordActivity extends AppCompatActivity implements View.OnClickListener{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_select_record);
        findViewById(R.id.single_record).setOnClickListener(this);
        findViewById(R.id.multi_record).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.single_record:
                startActivity(new Intent(this,MainActivity.class));
                break;
            case R.id.multi_record:
                startActivity(new Intent(this,MultiRecordActivity.class));
                break;
        }
    }
}
