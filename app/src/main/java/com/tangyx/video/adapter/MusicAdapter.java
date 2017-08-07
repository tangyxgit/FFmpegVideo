package com.tangyx.video.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tangyx.video.R;
import com.tangyx.video.model.Music;

import java.util.List;

/**
 * Created by tangyx
 * Date 2017/8/4
 * email tangyx@live.com
 */

public class MusicAdapter extends BaseAdapter {

    private Context context;
    private List<Music> musics;

    public MusicAdapter(Context context, List<Music> musics) {
        this.context = context;
        this.musics = musics;
    }

    @Override
    public int getCount() {
        return musics.size();
    }

    @Override
    public Music getItem(int i) {
        return musics.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final MusicHolder holder;
        if(view == null){
            holder = new MusicHolder();
            view = LayoutInflater.from(context).inflate(R.layout.adapter_music,null);
            holder.mName = (TextView) view.findViewById(R.id.name);
            holder.mSingerName = (TextView) view.findViewById(R.id.singer_name);
            view.setTag(holder);
        }else{
            holder = (MusicHolder) view.getTag();
        }
        Music m = getItem(i);
        holder.mName.setText(m.getName());
        holder.mSingerName.setText(m.getSingerName());
        return view;
    }
    private class MusicHolder{
        TextView mName;
        TextView mSingerName;
    }
}
