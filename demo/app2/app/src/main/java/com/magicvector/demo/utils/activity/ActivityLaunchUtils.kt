package com.magicvector.demo.utils.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

object ActivityLaunchUtils {
    /**
     * 启动新任务
     * @param context   上下文
     * @param intent    intent
     * @param config    配置
     */
    fun launchNewTask(context: Context, intent: Intent, config: IntentConfig?) {
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

        // 允许额外配置 Intent
        config?.configure(intent)

        context.startActivity(intent)
    }

    fun <T : Activity> launchNewTask(
        context: Context,
        activityClass: Class<T>,
        config: IntentConfig?
    ) {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

        // 允许额外配置 Intent
        config?.configure(intent)

        context.startActivity(intent)
    }


    fun <T : Activity> launch(context: Context, activityClass: Class<T>, config: IntentConfig?) {
        val intent = Intent(context, activityClass)

        // 允许额外配置 Intent
        config?.configure(intent)

        context.startActivity(intent)
    }

    fun getResultLauncher(
        activity: AppCompatActivity,
        callback: handleActivityLaunchResult
    ): ActivityResultLauncher<Intent> {
        return activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult? ->
            callback.onActivityResult(
                result
            )
        }
    }

    fun <T : Activity> launchForResult(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>,
        activityClass: Class<T>,
        config: IntentConfig?
    ) {
        val intent = Intent(activity, activityClass)

        // 允许额外配置 Intent
        config?.configure(intent)

        launcher.launch(intent)
    }

    interface IntentConfig {
        fun configure(intent: Intent)
    }

    interface handleActivityLaunchResult {
        // Intent data = result.getData();
        // Uri uri = data.getData();
        fun onActivityResult(result: ActivityResult?)
    }
}