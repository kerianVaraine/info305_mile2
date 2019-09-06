package com.info305.milestone2;

import android.app.Application;
import android.media.MediaPlayer;

public final class SingleMP extends Application {

    static MediaPlayer instance;

    public static MediaPlayer getInstance() {

        if (instance == null)
        {
            instance = new MediaPlayer();

        }


        return instance;
    }


}
