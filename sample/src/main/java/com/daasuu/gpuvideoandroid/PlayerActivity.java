package com.daasuu.gpuvideoandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.player.GPUPlayerView;
import com.daasuu.gpuvideoandroid.widget.MovieWrapperView;
import com.daasuu.gpuvideoandroid.widget.PlayerTimer;

import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private static final String STREAM_URL_MP4_VOD_LONG = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4";

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, PlayerActivity.class);
        activity.startActivity(intent);
    }

    private GPUPlayerView gpuPlayerView;
    private ExoPlayer player;
    private Button button;
    private SeekBar timeSeekBar;
    private SeekBar filterSeekBar;
    private PlayerTimer playerTimer;
    private GlFilter filter;
    private FilterAdjuster adjuster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setUpViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMedia3ExoPlayer();
        setUpGlPlayerView();
        setUpTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
        if (playerTimer != null) {
            playerTimer.stop();
            playerTimer.removeMessages(0);
        }
    }

    private void setUpViews() {
        // play/pause button
        button = findViewById(R.id.btn);
        button.setOnClickListener(v -> {
            if (player == null) return;

            if (button.getText().toString().equals(PlayerActivity.this.getString(R.string.pause))) {
                player.setPlayWhenReady(false);
                button.setText(R.string.play);
            } else {
                player.setPlayWhenReady(true);
                button.setText(R.string.pause);
            }
        });

        // seek bar for video
        timeSeekBar = findViewById(R.id.timeSeekBar);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player == null || !fromUser) return;
                player.seekTo(progress * 1000L);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        // filter adjuster seek bar
        filterSeekBar = findViewById(R.id.filterSeekBar);
        filterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjuster != null) {
                    adjuster.adjust(filter, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        // filter list view
        ListView listView = findViewById(R.id.list);
        final List<FilterType> filterTypes = FilterType.createFilterList();
        listView.setAdapter(new FilterAdapter(this, R.layout.row_text, filterTypes));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            filter = FilterType.createGlFilter(filterTypes.get(position), getApplicationContext());
            adjuster = FilterType.createFilterAdjuster(filterTypes.get(position));
            findViewById(R.id.filterSeekBarLayout).setVisibility(adjuster != null ? View.VISIBLE : View.GONE);
            gpuPlayerView.setGlFilter(filter);
        });
    }

    @OptIn(markerClass = UnstableApi.class) private void setUpMedia3ExoPlayer() {
        // Initialize Media3 ExoPlayer
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(new DefaultTrackSelector(this))
                .build();

        // Add media item
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(STREAM_URL_MP4_VOD_LONG));
        player.setMediaItem(mediaItem);

        // Prepare player
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void setUpGlPlayerView() {
        gpuPlayerView = new GPUPlayerView(this);
        gpuPlayerView.setExoPlayer(player);
        gpuPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).addView(gpuPlayerView);
        gpuPlayerView.onResume();
    }

    private void setUpTimer() {
        playerTimer = new PlayerTimer();
        playerTimer.setCallback(timeMillis -> {
            if (player == null) return;

            long position = player.getCurrentPosition();
            long duration = player.getDuration();

            if (duration <= 0) return;

            timeSeekBar.setMax((int) (duration / 1000));
            timeSeekBar.setProgress((int) (position / 1000));
        });
        playerTimer.start();
    }

    private void releasePlayer() {
        if (gpuPlayerView != null) {
            gpuPlayerView.onPause();
            ((MovieWrapperView) findViewById(R.id.layout_movie_wrapper)).removeAllViews();
            gpuPlayerView = null;
        }
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }
}
