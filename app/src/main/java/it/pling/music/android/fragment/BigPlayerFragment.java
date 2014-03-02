package it.pling.music.android.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import net.sourceforge.subsonic.androidapp.domain.MusicDirectory;
import net.sourceforge.subsonic.androidapp.service.DownloadService;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceImpl;
import net.sourceforge.subsonic.androidapp.util.Util;

import java.util.ArrayList;
import java.util.List;

import it.pling.music.android.R;

/**
 * Created by eskerda on 2/21/14.
 */
public class BigPlayerFragment extends Fragment{
    private static final String TAG = "BigPlayerFragment";

    private TextView song_title;
    private TextView song_artist;
    private ImageButton play_button;
    private ImageButton pause_button;

    private MusicDirectory.Entry cSong;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v =inflater.inflate(R.layout.big_song_player, container, false);
        song_title = (TextView) v.findViewById(R.id.big_player_song_title);
        song_artist = (TextView) v.findViewById(R.id.big_player_song_artist);
        play_button = (ImageButton) v.findViewById(R.id.big_player_play);
        pause_button = (ImageButton) v.findViewById(R.id.big_player_pause);
        play_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cSong == null) return;
                download(cSong);
            }
        });
        pause_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadService dw = getDownloadService();
                if (dw != null) {
                    dw.pause();
                    togglePlaying(false);
                }
            }
        });
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void attachSong(MusicDirectory.Entry song) {
        song_title = (TextView) getView().findViewById(R.id.big_player_song_title);
        song_artist = (TextView) getView().findViewById(R.id.big_player_song_artist);
        song_title.setText(song.getTitle());
        song_artist.setText(song.getArtist());
        cSong = song;
    }

    private void togglePlaying(boolean playing) {
        if (playing) {
            play_button.setVisibility(View.GONE);
            pause_button.setVisibility(View.VISIBLE);
        } else {
            play_button.setVisibility(View.VISIBLE);
            pause_button.setVisibility(View.GONE);
        }
    }

    private void download(MusicDirectory.Entry entry) {
        final List<MusicDirectory.Entry> songs = new ArrayList<MusicDirectory.Entry>();
        songs.add(entry);
        final DownloadService dw = getDownloadService();
        Log.i(TAG, "Should be downloading " + entry.getTitle());
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (dw != null) {
                    dw.clear();
                    dw.download(songs, false, true, false);
                    togglePlaying(true);
                } else {
                    Log.i(TAG, "Download Service is null, fuck you.");
                }
            }
        };
        task.run();

    }

    public DownloadService getDownloadService() {
        // If service is not available, request it to start and wait for it.
        for (int i = 0; i < 10; i++) {
            DownloadService downloadService = DownloadServiceImpl.getInstance();
            if (downloadService != null) {
                Log.i(TAG, "Returning assumed download service...");
                return downloadService;
            }
            Log.w(TAG, "DownloadService not running. Attempting to start it.");
            getActivity().startService(new Intent(getActivity(), DownloadServiceImpl.class));
            Util.sleepQuietly(50L);
        }
        return DownloadServiceImpl.getInstance();
    }
}
