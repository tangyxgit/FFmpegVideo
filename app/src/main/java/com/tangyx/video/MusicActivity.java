package com.tangyx.video;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tangyx.video.adapter.MusicAdapter;
import com.tangyx.video.model.Music;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tangyx
 * Date 2017/8/4
 * email tangyx@live.com
 */

public class MusicActivity extends AppCompatActivity {
    private ListView mListView;
    private MusicAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);
        mListView = (ListView) findViewById(R.id.list);
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        new SongTask().execute();
    }
    private class SongTask extends AsyncTask<Void, Void, List<Music>> implements AdapterView.OnItemClickListener{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Music> doInBackground(Void... voids) {
            List<Music> musics = new ArrayList<>();
            Cursor cursor = getApplicationContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                    MediaStore.Audio.Media.DATA + " like ?",
                    new String[]{Environment.getExternalStorageDirectory() + File.separator + "%"},
                    MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
                    if (isMusic != null && isMusic.equals("")) continue;
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    if (!path.endsWith(".mp3") || duration<60 * 1000) {
                        continue;
                    }
                    Music music = new Music();
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    music.setId(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                    music.setName(title);
                    music.setSingerName(artist);
                    music.setSongUrl(path);
                    musics.add(music);
                }
                cursor.close();
            }
            return musics;
        }

        @Override
        protected void onPostExecute(List<Music> musics) {
            super.onPostExecute(musics);
            mAdapter = new MusicAdapter(MusicActivity.this,musics);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(this);
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Music music = mAdapter.getItem(i);
            Intent intent = new Intent();
            intent.putExtra("music",music.getSongUrl());
            setResult(10000,intent);
            finish();
        }
    }
}
