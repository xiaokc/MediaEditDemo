package com.android.cong.mediaeditdemo.recorder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by xiaokecong on 05/06/2017.
 */

public class AdbShellRecodService extends IntentService {
    String cmdStr = "screenrecord --time-limit 20 --bit-rate 6000000 /sdcard/demo2.mp4";

    public AdbShellRecodService() {
        this("adb");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public AdbShellRecodService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        DataOutputStream outs = null;
        BufferedReader successReader = null;
        BufferedReader errorReader = null;

        try {
            // 权限设置，需要root权限为"su",否则为"sh"
            Process process = Runtime.getRuntime().exec("sh");
            //获取输出流
            outs = new DataOutputStream(process.getOutputStream());
            //将shell命令写入
            outs.write(cmdStr.getBytes());
            outs.writeBytes("\n");
            outs.writeBytes("exit\n");

            //提交命令
            outs.flush();

            int exitCode = process.waitFor(); // 执行结果的退出码
            Log.i("===>xkc", "exitCode=" + exitCode);

            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();

            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String s;
            while ((s = successReader.readLine()) != null) {
                successMsg.append(s);
            }

            Log.i("===>xkc", "success msg:" + successMsg);

            while ((s = errorReader.readLine()) != null) {
                errorMsg.append(s);
            }

            Log.i("===>xkc", "error msg:" + errorMsg);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outs != null) {
                    outs.close();
                }
                if (successReader != null) {
                    successReader.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
}
