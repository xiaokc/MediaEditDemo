package com.android.cong.mediaeditdemo.audiomux;

/**
 * Created by 郑童宇 on 2016/05/10.
 */
public interface ComposeAudioInterface {
    public void updateComposeProgress(int composeProgress);

    public void composeSuccess();

    public void composeFail();
}
