package com.go.cqh.moneygo;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

/**
 * 屏幕锁屏状态下唤醒
 * Created by caoqianghui on 2016/12/1.
 */
public class PowerManagerUtil {
    /**
     * 唤醒锁
     */
    private PowerManager.WakeLock wakeLock;
    /**
     * 键盘锁
     */
    private KeyguardManager.KeyguardLock keyguardLock;

    public PowerManagerUtil(Context context) {
        /**获取PowerManager电源管理的实例*/
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        /**SCREEN_DIM_WAKE_LOCK：保持CPU 运转，允许保持屏幕显示但有可能是灰的，允许关闭键盘灯*/
        /**ACQUIRE_CAUSES_WAKEUP：强制使屏幕亮起，这种锁主要针对一些必须通知用户的操作.*/
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "HongbaoWakelock");
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock = km.newKeyguardLock("HongbaoKeyguardLock");
    }

    private void acquire() {
        /**开启 保持屏幕唤醒  int 状态停留时间 30分钟*/
        wakeLock.acquire(1800000);
        /**解锁键盘*/
        keyguardLock.disableKeyguard();
    }

    private void release() {
        if (wakeLock.isHeld()) {
            /**关闭 保持屏幕唤醒*/
            wakeLock.release();
            /**锁键盘*/
            keyguardLock.reenableKeyguard();
        }
    }

    /**
     * 提供一个方法，供服务调用
     * @param isWake  是否要唤醒
     */
    public void handleWakeLock(boolean isWake) {
        if (isWake) {
            this.acquire();
        } else {
            this.release();
        }
    }
}
