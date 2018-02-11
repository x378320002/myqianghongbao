package com.wzx.qianghongbao;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


/**
 * 解锁系统锁屏用的activity
 */
public class LockScreenNullActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, R.anim.activity_retain);
        super.onCreate(savedInstanceState);
        Window window = getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        View v = new View(this);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    finish();
                    overridePendingTransition(0, 0);
                }
                return false;
            }
        });
        setContentView(v);
        mIntent = getIntent();
        init();
        Log.d("MyAccessibilityService", "null activity --- onCreate");
        final MyAccessibilityService myService = MyAccessibilityService.getMyService();
        if (myService != null) {
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    myService.openNotifyByNullActivity();
                }
            }, 0);
//            finish();
//            overridePendingTransition(0, 0);
        }

        //        int flags = 0;
//        flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
//        flags = flags | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
//        flags = flags | View.SYSTEM_UI_FLAG_LOW_PROFILE;
//        flags = flags | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//        //flags = flags | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        flags = flags | View.SYSTEM_UI_FLAG_IMMERSIVE;
//        flags = flags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        v.setSystemUiVisibility(flags);
    }

    Intent mIntent;
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mIntent = intent;
        Log.d("MyAccessibilityService", "null activity --- onNewIntent");
        init();
    }

    private void init() {
        if (mIntent.getIntExtra("startType", 100) == 0) {
            finish();
            overridePendingTransition(0, 0);
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                }
//            }, 50);
        }
//        int startType = getIntent().getIntExtra("startType", 0);
//        if (startType == 1) {
//            finish();
//        }
    }
}
