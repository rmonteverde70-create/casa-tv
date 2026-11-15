package local.casa.tv;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.VideoView;
import java.io.File;
import java.io.IOException;


public class LandscapeDream extends DreamService {
    private VideoView playerView;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        File directory = getExternalFilesDir(null);
        File video = directory == null ? null : new File(directory, "screensaver-video");
        if (video == null || !video.isFile()) {
            setContentView(background(this));
            return;
        }

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.BLACK);
        playerView = new VideoView(this);
        playerView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        playerView.setOnPreparedListener(player -> {
            player.setLooping(true);
            player.setVolume(0f, 0f);
            Log.i("CasaTV", "Screensaver video prepared (" + player.getVideoWidth()
                    + "x" + player.getVideoHeight() + ")");
        });
        playerView.setOnInfoListener((player, info, detail) -> {
            if (info == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                Log.i("CasaTV", "Screensaver video rendering");
            }
            return false;
        });
        playerView.setOnErrorListener((player, error, detail) -> {
            Log.w("CasaTV", "Screensaver could not play video: " + error + "/" + detail);
            playerView = null;
            setContentView(background(this));
            return true;
        });
        frame.addView(playerView, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        setContentView(frame);
        playerView.setVideoPath(video.getAbsolutePath());
        playerView.start();
    }

    @Override public void onDetachedFromWindow() {
        if (playerView != null) {
            playerView.stopPlayback();
            playerView = null;
        }
        super.onDetachedFromWindow();
    }

    static ImageView background(Context context) {
        ImageView view = new ImageView(context);
        view.setBackgroundColor(Color.BLACK);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        File directory = context.getExternalFilesDir(null);
        File image = directory == null ? null : new File(directory, "background.jpg");
        if (image != null && image.isFile()) {
            view.setImageBitmap(BitmapFactory.decodeFile(image.getAbsolutePath(), options));
        }
        if (view.getDrawable() == null) {
            view.setBackground(new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                    new int[]{0xff20252c, 0xff050506}));
        }
        return view;
    }
}
