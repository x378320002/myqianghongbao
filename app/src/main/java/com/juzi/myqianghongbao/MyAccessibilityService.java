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
    private boolean isScreenOff;
    private boolean mHasOpenHongBaoNotify;
    private boolean mHasClickHongBao;
    private Handler mHandler = new Handler();
    private ScreenOnOffReceiver mReceiver;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mPendingIntent;

    public static MyAccessibilityService getMyService() {
        return service;
    }

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
        Log.d("MyAccessibilityService", "onServiceConnected ");
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
                Log.d("MyAccessibilityService", "ACTION_SCREEN_OFF");
                isScreenOff = true;
            }
            else if (Intent.ACTION_SCREEN_ON.equals(action)) { //亮屏----
                Log.d("MyAccessibilityService", "ACTION_SCREEN_ON");
                if (!mHasOpenHongBaoNotify && mHasClickHongBao) {
                    isScreenOff = false;
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
        Log.d("MyAccessibilityService", "enable, onAccessibilityEvent event = " + MainActivity.HONGBAO_ENABLE + ", " + event);
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
                    if (mHasClickHongBao) {
                        AccessibilityNodeInfo sourceNodeInfo = getRootInActiveWindow();
                        if (sourceNodeInfo == null) {
                            mHasClickHongBao = false;
                            Log.i("MyAccessibilityService", "拆红包，sourceNodeInfo is null !!!! ");
                            return;
                        }
                        CharSequence currentClassName = event.getClassName();
                        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
                            //打开红包主页
                            chaiHongBao(sourceNodeInfo);
                        } else {
                            Log.i("MyAccessibilityService", "拆红包，false --- !!!! ");
                            //mHasClickHongBao = false;
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    if (mHasOpenHongBaoNotify) { //进入聊天框，点击红包
                        AccessibilityNodeInfo sourceNodeInfo1 = getRootInActiveWindow();
                        if (sourceNodeInfo1 == null) {
                            Log.d("MyAccessibilityService", "点红包，sourceNodeInfo is null !!!! ");
                            mHasOpenHongBaoNotify = false;
                            return;
                        }
                        CharSequence currentClassName1 = event.getClassName();
                        if ("android.widget.ListView".equals(currentClassName1)) { //微信在前台时，进入聊天界面,去点红包
                            clickHongBao(sourceNodeInfo1);
                        } else {
                            Log.d("MyAccessibilityService", "!!点红包，\"android.widget.ListView\".equals(currentClassName1) is false---! ");
                            //mHasOpenHongBaoNotify = false;
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
////            mHasOpenHongBaoNotify = false;
////            if (isScreenOff) {
////                isScreenOff = false;
////                wakeAndUnlock(false);
////            }
//            Log.i("MyAccessibilityService", "sourceNodeInfo is null !!!! ");
//            return;
//        }
//
//        CharSequence currentClassName = event.getClassName();
//        if ("com.tencent.mm.ui.LauncherUI".equals(currentClassName)) { //微信在后台时，进入聊天界面,去点红包
//            if (!mHasClickHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mHasClickHongBao = true;
//            }
////            //直接用
////            AccessibilityNodeInfo node = event.getSource();
////            if (node == null) {
////            } else { //直接用sourceNodeInfo时，虽然已经打开，但是现实未打开，所以用event.getSource()。
////                clickHongBao(node);
////            }
//        } else if ("android.widget.ListView".equals(currentClassName)) { //微信在前台时，进入聊天界面,去点红包
//            if (!mHasClickHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mHasClickHongBao = true;
//            }
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
//            //打开红包主页
//            chaiHongBao(sourceNodeInfo);
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(currentClassName)) {
//            // 红包详情主页 red envelope detail page
//            mHasOpenHongBaoNotify = false;
//            if (isScreenOff) {
//                isScreenOff = false;
//                wakeAndUnlock(false);
//            }
//        }
//    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void chaiHongBao(AccessibilityNodeInfo nodeInfo) {
        AccessibilityNodeInfo targetNode = null;
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b43"); //開字的ID
        Log.d("MyAccessibilityService", "chaiHongBao list = " + list);

        if(list == null || list.isEmpty()) {
            Log.d("MyAccessibilityService", "chaiHongBao list is null");
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

        mHasClickHongBao = false;
//        if (isScreenOff) {
//            isScreenOff = false;
//            wakeAndUnlock(false);
//        }
//        if (mHasOpenHongBaoNotify) {
//        }
    }

    /**
     * 聊天页面点击收到的红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clickHongBao(AccessibilityNodeInfo node) {
        Log.d("MyAccessibilityService", "clickHongBao ---");
        List<AccessibilityNodeInfo> list;
        list = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b_"); //開字的ID,b_ , e4
        boolean hasFind = false;
        if (list != null && !list.isEmpty()) {
            for (int i = list.size() - 1; i >= 0 ; i--) {
                AccessibilityNodeInfo info = list.get(i);
                List<AccessibilityNodeInfo> children = info.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN);
                if (children != null && !children.isEmpty()) {
                    if (info.isClickable()) {
                        mHasClickHongBao = true;
                        hasFind = true;
                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i("MyAccessibilityService", "clickHongBao --- 点击了, 位置1");
                        break;
                    } else {
                        AccessibilityNodeInfo parent = children.get(0).getParent();
                        if(parent != null && parent.isClickable()) {
                            mHasClickHongBao = true;
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            Log.i("MyAccessibilityService", "clickHongBao --- 点击了, 位置2");
                            break;
                        }
                        Log.i("MyAccessibilityService", "clickHongBao --- 找到了不能点击");
                    }
                }


//                AccessibilityNodeInfo info = list.get(i);
//                CharSequence text = info.getText();
//                if (MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN.equals(text)) {
//                    AccessibilityNodeInfo parent = info.getParent();
//                    if(parent != null && parent.isClickable()) {
//                        hasFind = true;
//                        mHasClickHongBao = true;
//                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        break;
//                    }
//                }
            }
        }

        mHasOpenHongBaoNotify = false;
        if (!hasFind) {
            Log.d("MyAccessibilityService", "clickHongBao --- hasFind is false");
            mHasClickHongBao = false;
            goDeskTop(); // 退出当前聊天界面
        }

//        list = node.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN);
//        if(list != null && !list.isEmpty()) {
//            boolean find = false;
//            for(int i = list.size() - 1; i >= 0; i--) {
//                AccessibilityNodeInfo parent = list.get(i).getParent();
//                if(parent != null && parent.isClickable()) {
//                    mHasClickHongBao = true;
//                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    Log.i("MyAccessibilityService", "clickHongBao --- 点击了");
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) {
//                mHasClickHongBao = false;
//                if (isScreenOff) {
//                    isScreenOff = false;
//                    wakeAndUnlock(false);
//                }
//            }
//        } else {
//            mHasClickHongBao = false;
//            if (isScreenOff) {
//                isScreenOff = false;
//                wakeAndUnlock(false);
//            }
//        }
//        mHasOpenHongBaoNotify = false;
//        if (mHasOpenHongBaoNotify) {
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

    /**
     * 主动返回
     */
    private void goDeskTop() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }


    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private synchronized void openNotify(AccessibilityEvent event) {
        if(event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        Notification notification = (Notification) event.getParcelableData();
        //final PendingIntent pendingIntent = notification.contentIntent;
        mPendingIntent = notification.contentIntent;
        Log.d("MyAccessibilityService", "来红包啦!");
        if (km== null) {
            km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (isScreenOff || km.isKeyguardLocked()) {
            isScreenOff = true;
            wakeAndUnlock(true);

//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        mHasOpenHongBaoNotify = true;
//                        pendingIntent.send();
//                    } catch (Exception e) {
//                        mHasOpenHongBaoNotify = false;
//                        mHasClickHongBao = false;
//                        wakeAndUnlock(false);
//                        e.printStackTrace();
//                    }
//                }
//            }, 600);
        } else {
            try {
                mHasOpenHongBaoNotify = true;
                mPendingIntent.send();
                mPendingIntent = null;
            } catch (Exception e) {
                mHasOpenHongBaoNotify = false;
                e.printStackTrace();
            }
        }
//        if (notification.tickerText != null
//                && notification.tickerText.toString().contains(MainActivity.HONGBAO_TEXT_KEY_NOTIFY)) {
//        }
    }

    /**
     * 空白Activity启动后，会解除系统锁屏，然后再调用此方法打开通知栏红包
     */
    public void openNotifyByNullActivity() {
        if (mPendingIntent != null) {
            try {
                mHasOpenHongBaoNotify = true;
                mPendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                mHasOpenHongBaoNotify = false;
                e.printStackTrace();
            }
            mPendingIntent = null;
            Intent intent1 = new Intent(getBaseContext(), LockScreenNullActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.putExtra("startType", 0);
            startActivity(intent1);
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



    private void wakeAndUnlock(boolean b) {
        if(b) {
            //获取电源管理器对象
            Log.d("MyAccessibilityService", "wakeAndUnlock ");
            if (pm == null) {
                pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
            }
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
            if (wl == null) {
                wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            }
            //点亮屏幕
            wl.acquire();


            //自动解锁逻辑：
            //开启nullActivity，强制解锁系统锁屏
            Intent intent1 = new Intent(getBaseContext(), LockScreenNullActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.putExtra("startType", 1);
            startActivity(intent1);

            //得到键盘锁管理器对象
//            if (km == null) {
//                km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
//            }
//            if (kl == null) {
//                kl = km.newKeyguardLock("unLock");
//            }
//            //解锁
//            kl.disableKeyguard();

        } else {
            if (wl != null) {
                //锁屏
                //kl.reenableKeyguard();
                //释放wakeLock，关灯
                wl.release();
            }
        }

    }
}
