package com.zx.tv.camera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.zx.tv.camera.ui.DisplayFragment;

/**
 * User: ShaudXiao
 * Date: 2017-08-04
 * Time: 10:28
 * Company: zx
 * Description:
 * FIXME
 */


public class MainActivity2 extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, flag);

        setContentView(R.layout.activity_my);

        DisplayFragment displayFragment = new DisplayFragment();
        getFragmentManager().beginTransaction().add(R.id.container, displayFragment, "display").commit();
    }
}
