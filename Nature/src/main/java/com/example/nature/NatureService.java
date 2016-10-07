package com.example.nature;

import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class NatureService extends Service{

	private static final String TAG = "com.example.nature.NATURE_SERVICE";

	
	private MediaPlayer mediaPlayer;
	
	private boolean isPlaying = false;
	
	private List<MusicLoader.MusicInfo> musicList;
	
	private Binder natureBinder = new NatureBinder();
	
	private int currentMusic;
	private int currentPosition;
	
	private static final int updateProgress = 1;
	private static final int updateCurrentMusic = 2;
	private static final int updateDuration = 3;
	
	public static final String ACTION_UPDATE_PROGRESS = "com.example.nature.UPDATE_PROGRESS";
	public static final String ACTION_UPDATE_DURATION = "com.example.nature.UPDATE_DURATION";
	public static final String ACTION_UPDATE_CURRENT_MUSIC = "com.example.nature.UPDATE_CURRENT_MUSIC";
	
	private int currentMode = 3; //default sequence playing
	
	public static final String[] MODE_DESC = {"Single Loop", "List Loop", "Random", "Sequence"};
	
	public static final int MODE_ONE_LOOP = 0;
	public static final int MODE_ALL_LOOP = 1;
	public static final int MODE_RANDOM = 2;
	public static final int MODE_SEQUENCE = 3; 
	private List<MusicLoader.MusicInfo> searchList;
	private List<MusicLoader.MusicInfo> tempList;
	private Notification notification; 
		
	private Handler handler = new Handler(){
		
		public void handleMessage(Message msg){
			switch(msg.what){
			case updateProgress:				
				toUpdateProgress();
				break;
			case updateDuration:				
				toUpdateDuration();
				break;
			case updateCurrentMusic:
				toUpdateCurrentMusic();
				break;
			}
		}
	};
	
	private void toUpdateProgress(){
		if(mediaPlayer != null && isPlaying){					
			int progress = mediaPlayer.getCurrentPosition();					
			Intent intent = new Intent();
			intent.setAction(ACTION_UPDATE_PROGRESS);
			intent.putExtra(ACTION_UPDATE_PROGRESS,progress);
			sendBroadcast(intent);
			handler.sendEmptyMessageDelayed(updateProgress, 1000);					
		}
	}
	
	private void toUpdateDuration(){
		if(mediaPlayer != null){					
			int duration = mediaPlayer.getDuration();					
			Intent intent = new Intent();
			intent.setAction(ACTION_UPDATE_DURATION);
			intent.putExtra(ACTION_UPDATE_DURATION,duration);
			sendBroadcast(intent);									
		}
	}
	
	private void toUpdateCurrentMusic(){
		Intent intent = new Intent();
		intent.setAction(ACTION_UPDATE_CURRENT_MUSIC);
		intent.putExtra(ACTION_UPDATE_CURRENT_MUSIC,currentMusic);
		sendBroadcast(intent);				
	}

	public void onCreate(){
		initMediaPlayer();
		musicList = MusicLoader.instance(getContentResolver()).getMusicList();
		tempList = musicList;
		Log.v(TAG, "OnCreate");   
		super.onCreate();
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification = new Notification.Builder(this)
					.setTicker("MusicGo")
					.setSmallIcon(R.drawable.music_go)
					.setContentTitle("Playing")
					.setContentText(musicList.get(currentMusic).getTitle())
					.setContentIntent(pendingIntent)
					.getNotification();		
		notification.flags |= Notification.FLAG_NO_CLEAR;
		  
		startForeground(1, notification);
		
	}	
	
	public void onDestroy(){
		if(mediaPlayer != null){
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}	
	
	/**
	 * initialize the MediaPlayer
	 */
	private void initMediaPlayer(){
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);		
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {				
			@Override
			public void onPrepared(MediaPlayer mp) {				
				mediaPlayer.start();
				mediaPlayer.seekTo(currentPosition);
				handler.sendEmptyMessage(updateDuration);
			}
		});
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {			
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(isPlaying){

					switch (currentMode) {
					case MODE_ONE_LOOP:

						mediaPlayer.start();
						break;					
					case MODE_ALL_LOOP:

						play((currentMusic + 1) % musicList.size(), 0);
						break;
					case MODE_RANDOM:

						play(getRandomPosition(), 0);
						break;
					case MODE_SEQUENCE:

						if(currentMusic < musicList.size() - 1){						
							playNext();
						}
						break;
					default:

						break;
					}

				}
			}
		});
	}
	
	private void setCurrentMusic(int pCurrentMusic){
		currentMusic = pCurrentMusic;
		handler.sendEmptyMessage(updateCurrentMusic);
	}
	
	private int getRandomPosition(){
		int random = (int)(Math.random() * (musicList.size() - 1));
		return random;
	}
	
	private void play(int currentMusic, int pCurrentPosition) {
		currentPosition = pCurrentPosition;
		setCurrentMusic(currentMusic);
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(musicList.get(currentMusic).getUrl());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.v(TAG, "[Play] Start Preparing at " + currentMusic);
		mediaPlayer.prepareAsync();
		handler.sendEmptyMessage(updateProgress);

		isPlaying = true;
	}
	
	private void stop(){
		mediaPlayer.stop();
		isPlaying = false;
	}
	
	private void playNext(){
		switch(currentMode){
		case MODE_ONE_LOOP:
			play(currentMusic, 0);
			break;
		case MODE_ALL_LOOP:
			if(currentMusic + 1 == musicList.size()){
				play(0,0);
			}else{
				play(currentMusic + 1, 0);
			}
			break;
		case MODE_SEQUENCE:
			if(currentMusic + 1 == musicList.size()){
				Toast.makeText(this, "No more song.", Toast.LENGTH_SHORT).show();
			}else{
				play(currentMusic + 1, 0);
			}
			break;
		case MODE_RANDOM:
			play(getRandomPosition(), 0);
			break;
		}
	}
	
	private void playPrevious(){		
		switch(currentMode){
		case MODE_ONE_LOOP:
			play(currentMusic, 0);
			break;
		case MODE_ALL_LOOP:
			if(currentMusic - 1 < 0){
				play(musicList.size() - 1, 0);
			}else{
				play(currentMusic - 1, 0);
			}
			break;
		case MODE_SEQUENCE:
			if(currentMusic - 1 < 0){
				Toast.makeText(this, "No previous song.", Toast.LENGTH_SHORT).show();
			}else{
				play(currentMusic - 1, 0);
			}
			break;
		case MODE_RANDOM:
			play(getRandomPosition(), 0);
			break;
		}
	}
	

	@Override	
	public IBinder onBind(Intent intent) {		
		return natureBinder;
	}	
	
	class NatureBinder extends Binder{
		
		public void startPlay(int currentMusic, int currentPosition){
			play(currentMusic,currentPosition);
		}
		
		public void stopPlay(){
			stop();
		}
		
		public void toNext(){
			playNext();
		}
		
		public void toPrevious(){
			playPrevious();
		}

		public void getMusicList(List<MusicLoader.MusicInfo> newlist){
			searchList = newlist;
			musicList = searchList;
		}

		public void changeMode(){			
			currentMode = (currentMode + 1) % 4;


		}

		public int getCurrentMode(){
			return currentMode; 
		}
		

		public boolean isPlaying(){
			return isPlaying;
		}
		

		public void notifyActivity(){
			toUpdateCurrentMusic();
			toUpdateDuration();			
		}

		public void changeProgress(int progress){
			if(mediaPlayer != null){
				currentPosition = progress * 1000;
				if(isPlaying){
					mediaPlayer.seekTo(currentPosition);
				}else{
					play(currentMusic, currentPosition);
				}
			}
		}
	}

}