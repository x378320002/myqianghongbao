package com.juzi.myqianghongbao;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Dialog mTipsDialog;
    public static final String PREFERENCE_NAME = "qiangnimei";
    public static final String KEY_PREFERENCE_ENABLE = "KEY_ENABLE";
    public static final String KEY_PREFERENCE_KEYWORD_NOTIFY_HONGBAO = "KEY_PREFERENCE_KEYWORD_NOTIFY";
    public static final String KEY_PREFERENCE_KEYWORD_LIAOTIAN_HONGBAO = "KEY_PREFERENCE_KEYWORD_LIAOTIAN";
    public static final String KEY_PREFERENCE_KEYWORD_CHAIKAI_HONGBAO = "KEY_PREFERENCE_KEYWORD_CHAIKAI";

    public static final String ACTION_QIANGHONGBAO_SERVICE_DISCONNECT = "com.qianghongbao.ACCESSBILITY_DISCONNECT";
    public static final String ACTION_QIANGHONGBAO_SERVICE_CONNECT = "com.qianghongbao.ACCESSBILITY_CONNECT";

    /** 红包消息的关键字*/
    public static  String HONGBAO_TEXT_KEY_NOTIFY = "[微信红包]";
    public static  String HONGBAO_TEXT_KEY_LIAOTIAN = "领取红包";
    //public static  String HONGBAO_TEXT_KEY_CHAIKAI = "个红包";
    public static boolean HONGBAO_ENABLE;
    private SharedPreferences mPreferences;

    private Handler mHandler = new Handler();
    private MainFragment mMainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        mPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        HONGBAO_TEXT_KEY_NOTIFY =  mPreferences.getString(KEY_PREFERENCE_KEYWORD_NOTIFY_HONGBAO, "[微信红包]");
        HONGBAO_TEXT_KEY_LIAOTIAN =  mPreferences.getString(KEY_PREFERENCE_KEYWORD_LIAOTIAN_HONGBAO, "领取红包");
        //HONGBAO_TEXT_KEY_CHAIKAI =  mPreferences.getString(KEY_PREFERENCE_KEYWORD_CHAIKAI_HONGBAO, "个红包");
        HONGBAO_ENABLE = mPreferences.getBoolean(KEY_PREFERENCE_ENABLE, true);

        mMainFragment = new MainFragment();
        getFragmentManager().beginTransaction().add(R.id.container, mMainFragment).commitAllowingStateLoss();

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAccessibilityServiceSettings();
            }
        });

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
//        filter.addAction(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
//        registerReceiver(qhbConnectReceiver, filter);
    }

//    private BroadcastReceiver qhbConnectReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if(isFinishing()) {
//                return;
//            }
//            String action = intent.getAction();
//            Log.d("MainActivity", "receive-->" + action);
//            if(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT.equals(action)) {
//                if (mTipsDialog != null && mTipsDialog.isShowing()) {
//                    mTipsDialog.dismiss();
//                }
//                if (mMainFragment != null) {
//                    mMainFragment.setQiangHongBaoEnable();
//                }
//            } else if(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT.equals(action)) {
//                showOpenAccessibilityServiceDialog();
//            }
//        }
//    };

    @Override
    protected void onDestroy() {
        if (mTipsDialog != null && mTipsDialog.isShowing()) {
            mTipsDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (HONGBAO_ENABLE && !MyAccessibilityService.isRunning()) {
                    showOpenAccessibilityServiceDialog();
                } else {
                    if (mTipsDialog != null && mTipsDialog.isShowing()) {
                        mTipsDialog.dismiss();
                    }
                }
            }
        }, 600);
    }

    //    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        try {
//            unregisterReceiver(qhbConnectReceiver);
//        } catch (Exception e) {}
//        mTipsDialog = null;
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuItem item = menu.add(0, 0, 0, R.string.open_service_button);
//        item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
//        return super.onCreateOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if(item.getItemId() == 0) {
//            openAccessibilityServiceSettings();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    /** 显示未开启辅助服务的对话框*/
    private void showOpenAccessibilityServiceDialog() {
        if (isFinishing()) {
            return;
        }
        if(mTipsDialog != null && mTipsDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请打开抢红包服务");
        builder.setMessage("当前未开启抢红包服务，请进入辅助功能界面，找到 我的抢红包 开启服务");
        builder.setPositiveButton("打开服务", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openAccessibilityServiceSettings();
            }
        });
        builder.setNegativeButton("关闭抢红包功能", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HONGBAO_ENABLE = false;
                mMainFragment.setQiangHongBaoDisable();
            }
        });
        builder.setCancelable(false);
        mTipsDialog = builder.show();
    }

    /** 打开辅助服务的设置*/
    private void openAccessibilityServiceSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "找到 我的抢红包 开启服务", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MainFragment extends PreferenceFragment {

        private SwitchPreference mSwitchPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(PREFERENCE_NAME);
            addPreferencesFromResource(R.xml.main);
            //微信红包开关
            mSwitchPreference = (SwitchPreference) findPreference(KEY_PREFERENCE_ENABLE);
            mSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    HONGBAO_ENABLE = (boolean) newValue;
                    if ((Boolean) newValue && !MyAccessibilityService.isRunning()) {
                        ((MainActivity) getActivity()).showOpenAccessibilityServiceDialog();
                    }
                    return true;
                }
            });

            final EditTextPreference notifyEditTextPre = (EditTextPreference) findPreference(KEY_PREFERENCE_KEYWORD_NOTIFY_HONGBAO);
            notifyEditTextPre.setSummary("当前关键字: " + HONGBAO_TEXT_KEY_NOTIFY);
            notifyEditTextPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String text = String.valueOf(newValue);
                    HONGBAO_TEXT_KEY_NOTIFY = text;
                    preference.setSummary("当前关键字: " + text);
                    return true;
                }
            });

            final EditTextPreference liaotianEditTextPre = (EditTextPreference) findPreference(KEY_PREFERENCE_KEYWORD_LIAOTIAN_HONGBAO);
            liaotianEditTextPre.setSummary("当前关键字: " + HONGBAO_TEXT_KEY_LIAOTIAN);
            liaotianEditTextPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String text = String.valueOf(newValue);
                    HONGBAO_TEXT_KEY_LIAOTIAN = text;
                    preference.setSummary("当前关键字: " + text);
                    return true;
                }
            });

//            final EditTextPreference chaikaiEditTextPre = (EditTextPreference) findPreference(KEY_PREFERENCE_KEYWORD_CHAIKAI_HONGBAO);
//            chaikaiEditTextPre.setSummary("当前关键字: " + HONGBAO_TEXT_KEY_CHAIKAI);
//            chaikaiEditTextPre.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    String text = String.valueOf(newValue);
//                    HONGBAO_TEXT_KEY_CHAIKAI = text;
//                    preference.setSummary("当前关键字: " + text);
//                    return true;
//                }
//            });
        }

        public void setQiangHongBaoDisable() {
            mSwitchPreference.setChecked(false);
        }

        public void setQiangHongBaoEnable() {
            mSwitchPreference.setChecked(true);
        }
    }
}
