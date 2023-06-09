package com.github.rwsbillyang.appproxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.e("BootReceiver", "BootReceiver get ACTION_BOOT_COMPLETED")

            val newIntent = Intent(context, MainActivity::class.java) // 要启动的Activity
            //1.如果自启动APP，参数为需要自动启动的应用包名
            //Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);

            //这句话必须加上才能开机自动运行app的界面
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            //2.如果自启动Activity
            context.startActivity(newIntent)

            //3.如果自启动服务
            //context.startService(newIntent);
        }
    }
}
