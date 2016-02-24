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
    private boolean mHasOpenedHongBaoNotify;
    private boolean mHasClickedHongBao;
    private boolean mHasOpenedHongBao;
    private Handler mHandler = new Handler();
    private ScreenOnOffReceiver mReceiver;
    private SharedPreferences mSharedPreferences;
    private PendingIntent mPendingIntent;

    // 详情页返回：com.tencent.mm:id/cdh
    // 手慢了关闭：com.tencent.mm:id/b47

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
                isScreenOff = false;
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
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: //点击红包和拆红包
                    if (mHasOpenedHongBaoNotify) { //进入聊天框，点击红包
                        CharSequence currentClassName1 = event.getClassName();
                        if ("com.tencent.mm.ui.LauncherUI".equals(currentClassName1)) { //微信在前台时，进入聊天界面,去点红包
                            AccessibilityNodeInfo sourceNodeInfo = getRootInActiveWindow();
                            if (sourceNodeInfo == null) {
                                Log.d("MyAccessibilityService", "点红包，sourceNodeInfo is null !!!! ");
                                mHasOpenedHongBaoNotify = false;
                                return;
                            }
                            clickHongBao(sourceNodeInfo);
                        }
                    } else if (mHasClickedHongBao) { //点完了红包，开始拆红包
                        CharSequence currentClassName = event.getClassName();
                        //未拆开的红包，点击拆开
                        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
                            AccessibilityNodeInfo sourceNodeInfo = getRootInActiveWindow();
                            if (sourceNodeInfo == null) {
                                mHasClickedHongBao = false;
                                Log.i("MyAccessibilityService", "拆红包，sourceNodeInfo is null !!!! ");
                                return;
                            }
                            openHongBao(sourceNodeInfo);
                        //已经拆开过的红包，进入了收到多少钱界面
                        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(currentClassName)) {
                            Log.i("MyAccessibilityService", "拆红包，已经拆过了，直接进入详情钱界面");
                            mHasClickedHongBao = false;
//                            if (isScreenOff) {
//                                goDeskTop();
//                            }
                        }
                    } else if (mHasOpenedHongBao) {
                        CharSequence currentClassName = event.getClassName();
                        if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(currentClassName)) {
                            Log.i("MyAccessibilityService", "已经拆过了，直接进入详情钱界面 22222");
                            mHasOpenedHongBao = false;
                            // TODO: 2016/2/24 自动回复的逻辑，从详情页返回聊天也，去聊天也回复
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
////            mHasOpenedHongBaoNotify = false;
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
//            if (!mHasClickedHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mHasClickedHongBao = true;
//            }
////            //直接用
////            AccessibilityNodeInfo node = event.getSource();
////            if (node == null) {
////            } else { //直接用sourceNodeInfo时，虽然已经打开，但是现实未打开，所以用event.getSource()。
////                clickHongBao(node);
////            }
//        } else if ("android.widget.ListView".equals(currentClassName)) { //微信在前台时，进入聊天界面,去点红包
//            if (!mHasClickedHongBao) {
//                clickHongBao(sourceNodeInfo);
//                mHasClickedHongBao = true;
//            }
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(currentClassName)) {
//            //打开红包主页
//            openHongBao(sourceNodeInfo);
//        } else if ("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(currentClassName)) {
//            // 红包详情主页 red envelope detail page
//            mHasOpenedHongBaoNotify = false;
//            if (isScreenOff) {
//                isScreenOff = false;
//                wakeAndUnlock(false);
//            }
//        }
//    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void openHongBao(AccessibilityNodeInfo nodeInfo) {
        mHasClickedHongBao = false;

        AccessibilityNodeInfo targetNode = null;
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b43"); //開字的ID
        Log.d("MyAccessibilityService", "openHongBao list = " + list);

        if(list == null || list.isEmpty()) {
          //找不到开红包入口，红包已经被抢光
            // TODO: 2016/2/24 应该关闭当前窗口，再进行其他如回复等逻辑
            Log.d("MyAccessibilityService", "openHongBao list is null");
//            List<AccessibilityNodeInfo> l = nodeInfo.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_CHAIKAI);
//            if(l != null && !l.isEmpty()) {
//                AccessibilityNodeInfo p = l.get(0).getParent();
//                if(p != null) {
//                    for (int i = 0; i < p.getChildCount(); i++) {
//                        AccessibilityNodeInfo node = p.getChild(i);
//                        if("android.widget.Button".equals(node.getClassName())) {
//                            targetNode = node;
//                            break;
//                        }
//                    }
//                }
//            }
        }

        if(list != null && !list.isEmpty()) {
            targetNode = list.get(0);
        }

        if(targetNode != null) {
            mHasOpenedHongBao = true;
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            targetNode.recycle();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mHasOpenedHongBao) {
                        //// TODO: 2016/2/24
                        /*
                        打开红包后应该进入详情，mHasOpenedHongBao会置为false，再进行关闭，回复等其他逻辑，
                        但是如果红包已经被领没了，将直接显示手慢了，无任何反应，所以需要一个延时的任务，
                        查看下是否mHasOpenedHongBao没有被置为false，那么主动置为false并关闭手慢窗口，
                        再进行回复等逻辑，目前还没有回复的逻辑
                         */
                    }
                }
            }, 1500);
        }
//        if (isScreenOff) {
//            isScreenOff = false;
//            wakeAndUnlock(false);
//        }
//        if (mHasOpenedHongBaoNotify) {
//        }
    }

    /**
     * 聊天页面点击收到的红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clickHongBao(AccessibilityNodeInfo node) {
        Log.d("MyAccessibilityService", "clickHongBao ---");
        mHasOpenedHongBaoNotify = false;

        List<AccessibilityNodeInfo> list;
        list = node.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b_"); //開字的ID,b_ , e4
        boolean hasFind = false;
        if (list != null && !list.isEmpty()) {
            for (int i = list.size() - 1; i >= 0 ; i--) {
                AccessibilityNodeInfo info = list.get(i);
                List<AccessibilityNodeInfo> children = info.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN);
                if (children != null && !children.isEmpty()) {
                    if (info.isClickable()) {
                        mHasClickedHongBao = true;
                        hasFind = true;
                        info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i("MyAccessibilityService", "clickHongBao --- 点击了, 位置1");
                        break;
                    } else {
                        AccessibilityNodeInfo parent = children.get(0).getParent();
                        if(parent != null && parent.isClickable()) {
                            mHasClickedHongBao = true;
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
//                        mHasClickedHongBao = true;
//                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                        break;
//                    }
//                }
            }
        }

        if (!hasFind) {
            Log.d("MyAccessibilityService", "clickHongBao --- hasFind is false");
            mHasClickedHongBao = false;
            goDeskTop(); // 退出当前聊天界面
        }

//        list = node.findAccessibilityNodeInfosByText(MainActivity.HONGBAO_TEXT_KEY_LIAOTIAN);
//        if(list != null && !list.isEmpty()) {
//            boolean find = false;
//            for(int i = list.size() - 1; i >= 0; i--) {
//                AccessibilityNodeInfo parent = list.get(i).getParent();
//                if(parent != null && parent.isClickable()) {
//                    mHasClickedHongBao = true;
//                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                    Log.i("MyAccessibilityService", "clickHongBao --- 点击了");
//                    find = true;
//                    break;
//                }
//            }
//            if (!find) {
//                mHasClickedHongBao = false;
//                if (isScreenOff) {
//                    isScreenOff = false;
//                    wakeAndUnlock(false);
//                }
//            }
//        } else {
//            mHasClickedHongBao = false;
//            if (isScreenOff) {
//                isScreenOff = false;
//                wakeAndUnlock(false);
//            }
//        }
//        mHasOpenedHongBaoNotify = false;
//        if (mHasOpenedHongBaoNotify) {
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
        Log.d("MyAccessibilityService", "goDeskTop --");
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
//        if (km== null) {
//            km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
//        }
        // || km.isKeyguardLocked()
        wakeAndUnlock(true);
//        if (isScreenOff) {
//            isScreenOff = true;
//
////            mHandler.postDelayed(new Runnable() {
////                @Override
////                public void run() {
////                    try {
////                        mHasOpenedHongBaoNotify = true;
////                        pendingIntent.send();
////                    } catch (Exception e) {
////                        mHasOpenedHongBaoNotify = false;
////                        mHasClickedHongBao = false;
////                        wakeAndUnlock(false);
////                        e.printStackTrace();
////                    }
////                }
////            }, 600);
//        } else {
//            try {
//                mHasOpenedHongBaoNotify = true;
//                mPendingIntent.send();
//                mPendingIntent = null;
//            } catch (Exception e) {
//                mHasOpenedHongBaoNotify = false;
//                e.printStackTrace();
//            }
//        }
//        if (notification.tickerText != null
//                && notification.tickerText.toString().contains(MainActivity.HONGBAO_TEXT_KEY_NOTIFY)) {
//        }
    }

    private void closseNullActivityAndOpenNotify() {
        Intent intent1 = new Intent(getBaseContext(), LockScreenNullActivity.class);
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtra("startType", 0);
        startActivity(intent1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mHasOpenedHongBaoNotify = true;
                    mPendingIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    mHasOpenedHongBaoNotify = false;
                    e.printStackTrace();
                }
                mPendingIntent = null;
            }
        });
    }
    /**
     * 空白Activity启动后，会解除系统锁屏，然后再调用此方法打开通知栏红包
     */
    public void openNotifyByNullActivity() {
        if (mPendingIntent != null) {
            if (isScreenOff) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        closseNullActivityAndOpenNotify();
                    }
                }, 600);
            } else {
                closseNullActivityAndOpenNotify();
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



    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void wakeAndUnlock(boolean b) {
        if(b) {
            //获取电源管理器对象
            Log.d("MyAccessibilityService", "wakeAndUnlock ");
//            if (pm == null) {
//                pm=(PowerManager) getSystemService(Context.POWER_SERVICE);
//            }
            //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
//            if (wl == null) {
//            }
            //wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            //点亮屏幕
            //wl.acquire();


            //自动解锁逻辑：
            //开启nullActivity，强制解锁系统锁屏
            Intent intent1 = new Intent(getBaseContext(), LockScreenNullActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.putExtra("startType", 1);
            startActivity(intent1);

            //得到键盘锁管理器对象
            if (km == null) {
                km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
            }
            if (kl == null) {
                kl = km.newKeyguardLock("unLock");
            }
            Log.d("MyAccessibilityService", "km.isKeyguardLocked() = " + km.isKeyguardLocked());
            Log.d("MyAccessibilityService", "km.isKeyguardSecure() = " + km.isKeyguardSecure());
            if (km.isKeyguardLocked() && km.isKeyguardSecure()) {
                //解锁,这个解锁时为了隐藏输密码界面，如果没有密码，只用上面的自动nullactivity就可以了
                kl.disableKeyguard();
            }
        }
//        else {
//            //if (wl != null) {
//                //锁屏
//                //kl.reenableKeyguard();
//                //释放wakeLock，关灯
//                //wl.release();
//            //}
//        }
    }
}
