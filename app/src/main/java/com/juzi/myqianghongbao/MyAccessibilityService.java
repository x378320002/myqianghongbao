package com.juzi.myqianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Iterator;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private static MyAccessibilityService service;
    private boolean isLockScreen;
    private boolean mIsClickHongBao;
    private boolean mIsChaiHongBao;
    private Handler mHandler = new Handler();
    private ScreenOnOffReceiver mReceiver;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onDestroy() {
        service = null;
        unregisterReceiver(mReceiver);
        super.onDestroy();
//        //发送广播，已经断开辅助服务
//        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
//        sendBroadcast(intent);
    }

    @Override
    protected void onServiceConnected() {
        Log.i("MyAccessibilityService", "onServiceConnected ");
        service = this;
        super.onServiceConnected();
//        //发送广播，已经连接上了
//        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
//        sendBroadcast(intent);
//        Toast.makeText(this, "成功开启抢红包服务", Toast.LENGTH_SHORT).show();

        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.setPriority(Integer.MAX_VALUE);

        mReceiver = new ScreenOnOffReceiver();
        registerReceiver(mReceiver, filter);
    }

    class ScreenOnOffReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i("MyAccessibilityService", "ACTION_SCREEN_OFF");
                isLockScreen = true;
            }
            else if (Intent.ACTION_SCREEN_ON.equals(action)) { //亮屏----
                Log.i("MyAccessibilityService", "ACTION_SCREEN_ON");
                if (!mIsClickHongBao && mIsChaiHongBao) {
                    isLockScreen = false;
                }
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i("MyAccessibilityService", "enable, onAccessibilityEvent event = " + MainActivity.HONGBAO_ENABLE + ", " + event);
        if (MainActivity.HONGBAO_ENABLE) {
            final int eventType = event.getEventType();
            //通知栏事件
            switch (eventType) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED: //接到通知，有红包
                    List<CharSequence> texts = event.getText();
                    if(!texts.isEmpty()) {
                        for(CharSequence t : texts) {
                            String text = String.valueOf(t);
                            if(text.contains(MainActivity.HONGBAO_TEXT_KEY_NOTIFY)) {
                                openNotify(event);
                                break;
                            }
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: //拆红包
                    if (mIsChaiHongBao) {
                        AccessibilityNodeInfo sourceNodeInfo = getRootInActiveWindow();
                        if (sourceNodeInfo == null) {
                            Log.i("MyAccessibilityService", "拆红包，sourceNodeInfo is null !!!! ");
                            return;
                        }
                        CharSequence currentClassName = event.getClassName();
                        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
                            //打开红包主页
                            chaiHongBao(sourceNodeInfo);
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    if (mIsClickHongBao) { //进入聊天框，点击红包
                        AccessibilityNodeInfo sourceNodeInfo1 = getRootInActiveWindow();
                        if (sourceNodeInfo1 == null) {
                            Log.i("MyAccessibilityService", "点红包，sourceNodeInfo is null !!!! ");
                            return;
                        }
                        CharSequence currentClassName1 = event.getClassName();
                        if ("android.widget.ListView".equals(currentClassName1)) { //微信在前台时，进入聊天界面,去点红包
                            clickHongBao(sourceNodeInfo1);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    private synchronized void openHongBao(AccessibilityEvent event) {
////        AccessibilityNodeInfo sourceNodeInfo = event.getSource();
//        AccessibilityNodeInfo sourceNodeInfo = getRootInActiveWindow();
//        if (sourceNodeInfo == null) {
////            mIsClickHongBao = false;
////            if (isLockScreen) {
////                isLockScreen = false;
////                wakeAndUnlock(false);
////            }
//            Log.i("MyAccessibilityService", "sourceNodeInfo is null !!!! ");
//            return;
//        }
//
//        CharSequence currentClassName = event.getClassName();
//        if ("com.tencent.mm.ui.LauncherUI".equals(currentClassName)) { //微信在后台时，进入聊天界面,去点红包
//            if (!mIsChaiHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mIsChaiHongBao = true;
//            }
////            //直接用
////            AccessibilityNodeInfo node = event.getSource();
////            if (node == null) {
////            } else { //直接用sourceNodeInfo时，虽然已经打开，但是现实未打开，所以用event.getSource()。
////                clickHongBao(node);
////            }
//        } else if ("android.widget.ListView".equals(currentClassName)) { //微信在前台时，进入聊天界面,去点红包
//            if (!mIsChaiHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mIsChaiHongBao = true;
//            }
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
//            //打开红包主页
//            chaiHongBao(sourceNodeInfo);
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(currentClassName)) {
//            // 红包详情主页 red envelope detail page
//            mIsClickHongBao = false;
//            if (isLockScreen) {
//                isLockScreen = false;
//                wakeAndUnlock(false);
//            }
//        }
//    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void chaiHongBao(AccessibilityNodeInfo nodeInfo) {
        AccessibilityNodeInfo targetNode = null;
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_CHAIKAI);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b43"); //開字的ID
        }

        Log.i("MyAccessibilityService", "chaiHongBao list = " + list);

        if(list == null || list.isEmpty()) {
            Log.i("MyAccessibilityService", "chaiHongBao list is null");
            List<AccessibilityNodeInfo> l = nodeInfo.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_CHAIKAI);
            if(l != null && !l.isEmpty()) {
                AccessibilityNodeInfo p = l.get(0).getParent();
                if(p != null) {
                    for (int i = 0; i < p.getChildCount(); i++) {
                        AccessibilityNodeInfo node = p.getChild(i);
                        if("android.widget.Button".equals(node.getClassName())) {
                            targetNode = node;
                            break;
                        }
                    }
                }
            }
        }

        if(list != null && !list.isEmpty()) {
            targetNode = list.get(0);
        }

        if(targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            targetNode.recycle();
        }

        mIsChaiHongBao = false;
        if (isLockScreen) {
            isLockScreen = false;
            wakeAndUnlock(false);
        }
//        if (mIsClickHongBao) {
//        }
    }

    /**
     * 聊天页面点击收到的红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clickHongBao(AccessibilityNodeInfo node) {
        Log.i("MyAccessibilityService", "clickHongBao ---");
        List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN);
        if(list != null && !list.isEmpty()) {
            boolean find = false;
            for(int i = list.size() - 1; i >= 0; i --) {
                AccessibilityNodeInfo parent = list.get(i).getParent();
                if(parent != null && parent.isClickable()) {
                    mIsChaiHongBao = true;
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.i("MyAccessibilityService", "clickHongBao --- 点击了");
                    find = true;
                    break;
                }
            }
            if (!find) {
                mIsChaiHongBao  = false;
                if (isLockScreen) {
                    isLockScreen = false;
                    wakeAndUnlock(false);
                }
            }
        } else {
            mIsChaiHongBao = false;
            if (isLockScreen) {
                isLockScreen = false;
                wakeAndUnlock(false);
            }
        }
        mIsClickHongBao = false;
//        if (mIsClickHongBao) {
//        }


//        if(list != null && list.isEmpty()) {
//            // 从消息列表查找红包
//            list = node.findAccessibilityNodeInfosByText("[微信红包]");
//            if(list == null || list.isEmpty()) {
//                return;
//            }
//
//            for(AccessibilityNodeInfo n : list) {
//                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                break;
//            }
//        } else if(list != null) {
//            //最新的红包领起
//            for(int i = list.size() - 1; i >= 0; i --) {
//                AccessibilityNodeInfo parent = list.get(i).getParent();
//                if(parent != null) {
//                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    parent.recycle();
//                    break;
//                }
//            }
//        }
    }


    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private synchronized void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        Notification notification = (Notification) event.getParcelableData();
        final PendingIntent pendingIntent = notification.contentIntent;
        if (notification.tickerText != null
                && notification.tickerText.toString().contains(MainActivity.HONGBAO_TEXT_KEY_NOTIFY)) {
            Log.i("MyAccessibilityService", "来红包啦!");
            if ( km== null) {
                km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            }
            if (isLockScreen || km.isKeyguardLocked()) {
                isLockScreen = true;
                wakeAndUnlock(true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mIsClickHongBao = true;
                            pendingIntent.send();
                        } catch (Exception e) {
                            mIsClickHongBao = false;
                            mIsChaiHongBao = false;
                            wakeAndUnlock(false);
                            e.printStackTrace();
                        }
                    }
                }, 600);
            } else {
                try {
                    mIsClickHongBao = true;
                    pendingIntent.send();
                } catch (Exception e) {
                    wakeAndUnlock(false);
                    mIsClickHongBao = false;
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.i("MyAccessibilityService", "onInterrupt ");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        Log.i("MyAccessibilityService", "onKeyEvent event = " + event);
        return super.onKeyEvent(event);
    }

    /**
     * 判断当前服务是否正在运行
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        AccessibilityServiceInfo info = service.getServiceInfo();
        if(info == null) {
            return false;
        }

        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);

        Iterator<AccessibilityServiceInfo> iterator = list.iterator();
        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if(i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        return isConnect;
    }

    //锁屏、唤醒相关
    private KeyguardManager  km;
    private KeyguardManager.KeyguardLock kl;
    private PowerManager pm;
    private PowerManager.WakeLock wl;



    private void wakeAndUnlock(boolean b)
    {
        if(b) {
            //获取电源管理器对象
            Log.i("MyAccessibilityService", "wakeAndUnlock ");
            if (pm == null) {
                pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
            }
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            if (wl == null) {
                wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            }
            //点亮屏幕
            wl.acquire();


            //得到键盘锁管理器对象
            if (km == null) {
                km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            }
            if (kl == null) {
                kl = km.newKeyguardLock("unLock");
            }

            //解锁
            kl.disableKeyguard();

        } else {
            if (kl != null) {
                //锁屏
                //kl.reenableKeyguard();
                //释放wakeLock，关灯
                wl.release();
            }
        }

    }
}
