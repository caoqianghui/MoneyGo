package com.go.cqh.moneygo.services;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.go.cqh.moneygo.PowerManagerUtil;

import java.util.List;

/**
 * Created by caoqianghui on 2016/12/1.
 */
public class MoneyGoService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    //一些关键字
    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    //通知栏 检测红包关键字
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
    /*微信现在所弹出的页面 */
    /**
     * 拆红包页面
     */
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI";
    /**
     * 红包详情页面
     */
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    /**
     * 微信主界面
     */
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    /**
     * 聊天界面
     */
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    //当前页面 默认为微信主界面
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    /*根节点  收到红包节点  */
    private AccessibilityNodeInfo rootNodeInfo, mReceiveNode, mUnpackNode;
    /**
     * 红包是否拆开  默认false
     */
    private boolean mLuckyMoneyPicked;
    /**
     * 是否已收到红包  默认false
     */
    private boolean mLuckyMoneyReceived;
    /**
     *未拆开红包数
     */
    private int mUnpackCount = 0;

    private boolean mMutex = false, mListMutex = false, mChatMutex = false;
    /*服务配置*/
    private MoneyGoServiceSignature signature = new MoneyGoServiceSignature();
    /*锁屏唤醒工具类*/
    private PowerManagerUtil powerManagerUtil;
    /*共享存储*/
    private SharedPreferences sharedPreferences;

    /**
     * 当sharepreference值发生变化的监听
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("watch_auto_btn_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
            this.powerManagerUtil.handleWakeLock(changedValue);
        }
    }
    /**
     * 当启动服务的时候就会自动被调用
     */
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        this.watchFlagsFromPreference();
    }

    private void watchFlagsFromPreference() {
        /*获得一个默认的sharepreferences*/
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        /*注册一个值改变的监听*/
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        /*实例一个电源管理类*/
        this.powerManagerUtil = new PowerManagerUtil(this);
        //根据用户设置是否打开息屏抢红包的功能
        boolean watchAutoBtnLock = sharedPreferences.getBoolean("watch_auto_btn_lock", false);
        this.powerManagerUtil.handleWakeLock(watchAutoBtnLock);
    }

    /**
     * 监听窗口变化的回调
     *
     * @param event 事件
     * 当我们指定packageNames的通知栏或者界面发生变化时，会通过onAccessibilityEvent回调我们的事件，接着进行事件的处理
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (sharedPreferences == null) return;

        //根据事件会调类型进行处理
        setCurrentActivityName(event);

        Log.d("-------", "有动静");
        Log.d("-------mMutex--", mMutex+"");
        /* 检测通知消息 */
        if (!mMutex) {
            Log.d("-------", "查通知栏");
            if (sharedPreferences.getBoolean("watch_auto_notification", false) && watchNotifications(event))
                return;
            if (sharedPreferences.getBoolean("watch_auto_list", false) && watchList(event)) return;
            Log.d("-------", "查列表");
            mListMutex = false;
        }

        if (!mChatMutex) {
            mChatMutex = true;
            if (sharedPreferences.getBoolean("watch_auto_btn_chatting", false)) {
                Log.d("-------", "查聊天页面");
                watchChat(event);
            }
            mChatMutex = false;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void watchChat(AccessibilityEvent event) {
        this.rootNodeInfo = getRootInActiveWindow();
        Log.d("----rootNodeInfo---", rootNodeInfo+"");
        if (rootNodeInfo == null) return;

        mReceiveNode = null;
        mUnpackNode = null;

        checkNodeInfo(event.getEventType());
        Log.d("----mReceived---", mLuckyMoneyReceived+"");
        Log.d("----mPicked---", mLuckyMoneyPicked+"");
        Log.d("----mReceiveNode---", mReceiveNode+"");
        /* 如果已经接收到红包并且还没有戳开 */
        if (mReceiveNode != null && mLuckyMoneyReceived && !mLuckyMoneyPicked) {
            Log.d("-------", "111111");
            mMutex = true;

            /*模拟点击 点开红包*/
            mReceiveNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
        }
        Log.d("----mUnpackCount---", mUnpackCount+"");
        Log.d("----mUnpackNode---", mUnpackNode+"");
        /* 如果戳开但还未领取 */
        if (mUnpackNode != null && mUnpackCount == 1) {
            Log.d("-------", "2222222");
            //获得拆包延迟时间，默认0秒不延迟
            int delayFlag = sharedPreferences.getInt("watch_auto_display", 0) * 1000;
            new android.os.Handler().postDelayed(
                    new Runnable() {
                        public void run() {
                            try {
                                /*模拟点击*/
                                mUnpackNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            } catch (Exception e) {
                                mMutex = false;
                                mLuckyMoneyPicked = false;
                                mUnpackCount = 0;
                            }
                        }
                    },
                    delayFlag);
        }
    }
    //根据当前窗口变化  记录下当前页面名字
    private void setCurrentActivityName(AccessibilityEvent event) {
        /*通知栏如果没有发生变化*/
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        try {
            Log.d("-------", "33333");
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
        }
    }

    private boolean watchList(AccessibilityEvent event) {
        if (mListMutex) return false;
        mListMutex = true;
        AccessibilityNodeInfo eventSource = event.getSource();
        // Not a message
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false;

        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
        if (!nodes.isEmpty() && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
            Log.d("-------", "watchList");
            AccessibilityNodeInfo nodeToClick = nodes.get(0);
            if (nodeToClick == null) return false;
            CharSequence contentDescription = nodeToClick.getContentDescription();
            if (contentDescription != null && !signature.getContentDescription().equals(contentDescription)) {
                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                signature.setContentDescription(contentDescription.toString());
                return true;
            }
        }
        return false;
    }

    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;
        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return true;

        Log.d("-------", "watchNotifications");
        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                /* 清除signature,避免进入会话后误判 */
                signature.cleanSignature();

                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 中断服务的回调
     */
    @Override
    public void onInterrupt() {

    }

    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }
        Log.d("------findOpenButton-", "findOpenButton");
        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void checkNodeInfo(int eventType) {
        if (this.rootNodeInfo == null) return;

        if (signature.commentString != null) {
            sendComment();
            signature.commentString = null;
        }

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        AccessibilityNodeInfo node1 = (sharedPreferences.getBoolean("watch_auto_self", false)) ?
                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH) : this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);
        if (node1 != null && (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))) {
            String excludeWords = sharedPreferences.getString("watch_auto_pass_words", "");
            Log.d("-----generateSignature", signature.generateSignature(node1, excludeWords) + "");
            if (signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = node1;
            }
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo node2 = findOpenButton(this.rootNodeInfo);
        if (node2 != null && "android.widget.Button".equals(node2.getClassName()) && currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
            mUnpackNode = node2;
            mUnpackCount += 1;
            return;
        }
        Log.d("-------", "checkNodeInfo");
        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = hasOneOfThoseNodes(WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH, WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH);
        if (mMutex && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && hasNodes&& (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))) {
            mMutex = false;
            mLuckyMoneyPicked = false;
            mUnpackCount = 0;
            performGlobalAction(GLOBAL_ACTION_BACK);
            signature.commentString = generateCommentString();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void sendComment() {
        try {
            AccessibilityNodeInfo outNode =
                    getRootInActiveWindow().getChild(0).getChild(0);
            AccessibilityNodeInfo nodeToInput = outNode.getChild(outNode.getChildCount() - 1).getChild(0).getChild(1);

            if ("android.widget.EditText".equals(nodeToInput.getClassName())) {
                Bundle arguments = new Bundle();
                arguments.putCharSequence(AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, signature.commentString);
                nodeToInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
        } catch (Exception e) {
            // Not supported
        }
    }


    private boolean hasOneOfThoseNodes(String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo getTheLastNode(String... texts) {
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;

        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
                    signature.others = text.equals(WECHAT_VIEW_OTHERS_CH);
                }
            }
        }
        return lastNode;
    }



    @Override
    public void onDestroy() {
        /*服务销毁时关闭唤醒屏幕的功能，为用户节约电量*/
        this.powerManagerUtil.handleWakeLock(false);
        super.onDestroy();
    }

    private String generateCommentString() {
        if (!signature.others) return null;

        Boolean needComment = sharedPreferences.getBoolean("pref_comment_switch", false);
        if (!needComment) return null;

        String[] wordsArray = sharedPreferences.getString("pref_comment_words", "").split(" +");
        if (wordsArray.length == 0) return null;

        Boolean atSender = sharedPreferences.getBoolean("pref_comment_at", false);
        if (atSender) {
            return "@" + signature.sender + " " + wordsArray[(int) (Math.random() * wordsArray.length)];
        } else {
            return wordsArray[(int) (Math.random() * wordsArray.length)];
        }
    }
}
