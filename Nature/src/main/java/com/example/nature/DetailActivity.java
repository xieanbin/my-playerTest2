package com.example.nature;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.example.nature.MusicLoader.MusicInfo;
import com.example.nature.NatureService.NatureBinder;

import java.util.List;

public class DetailActivity extends Activity implements OnClickListener{
	
	private static final String TAG = "com.example.natrue.DetailActivity";
	
	public static final String MUSIC_LENGTH = "com.example.nature.DetailActivity.MUSIC_LENGTH";
	public static final String CURRENT_POSITION = "com.example.nature.DetailActivity.CURRENT_POSITION";
	public static final String CURRENT_MUSIC = "com.example.nature.DetailActivity.CURRENT_MUSIC";

	private SeekBar pbDuration;
	private TextView tvTitle,tvTimeElapsed, tvDuration;
	private List<MusicInfo> musicList;
	private int currentMusic;
	
	private int currentPosition;
	
	private ProgressReceiver progressReceiver;	
	
	private NatureBinder natureBinder;
	
	private int[] btnResIds = new int[] {
			R.id.btnMode,
			R.id.btnPrevious, 
			R.id.btnStartStop, 			
			R.id.btnNext,
			R.id.btnExit 
	};
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			natureBinder = (NatureBinder) service;	
			if(natureBinder.isPlaying()){
				CustomAudioIcon btnStartStop = (CustomAudioIcon)findViewById(R.id.btnStartStop);
				btnStartStop.setFlagStart(false);		
			}
			CustomAudioIcon btnMode = (CustomAudioIcon)findViewById(R.id.btnMode);
			btnMode.setCurrentMode(natureBinder.getCurrentMode());
		}
	};
	
	private void connectToNatureService(){		
		Intent intent = new Intent(DetailActivity.this, NatureService.class);				
		bindService(intent, serviceConnection, BIND_AUTO_CREATE);				
	}
		
	
	@Override	
	public void onCreate(Bundle savedInstanceState){
		Log.v(TAG, "OnCreate");
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.push_right_in,R.anim.hold);
		MusicLoader musicLoader = MusicLoader.instance(getContentResolver());		
		musicList = musicLoader.getMusicList();	
		setContentView(R.layout.detail_layout);			
		connectToNatureService();
		initComponents();	
	}
	
	@Override
	public void onResume(){
		Log.v(TAG, "OnResume");
		super.onResume();				
		initReceiver();		
	}
	
	@Override
	public void onPause(){
		Log.v(TAG, "OnPause unregister progress receiver");
		super.onPause();		
		unregisterReceiver(progressReceiver);
		overridePendingTransition(R.anim.hold, R.anim.push_right_out);
	}
		
	public void onStop(){
		Log.v(TAG, "OnStop");
		super.onStop();		
	}
	
	public void onDestroy(){
		Log.v(TAG, "Destroy");
		super.onDestroy();
		if(natureBinder != null){
			unbindService(serviceConnection);
		}
	}	

	private void initComponents(){		
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		currentMusic = getIntent().getIntExtra(CURRENT_MUSIC,0);
		tvTitle.setText(musicList.get(currentMusic).getTitle());	
		
		tvDuration = (TextView) findViewById(R.id.tvDuration);
		int max = getIntent().getIntExtra(MUSIC_LENGTH, 0);
		tvDuration.setText(FormatHelper.formatDuration(max));
		
		pbDuration = (SeekBar) findViewById(R.id.pbDuration);
		pbDuration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {		
				if(fromUser){
					natureBinder.changeProgress(progress);
				}
			}
		});
		pbDuration.setMax(max/1000);
					
		currentPosition = getIntent().getIntExtra(CURRENT_POSITION,0);
		pbDuration.setProgress(currentPosition / 1000);
		
		tvTimeElapsed = (TextView) findViewById(R.id.tvTimeElapsed);
		tvTimeElapsed.setText(FormatHelper.formatDuration(currentPosition));
				
		for(int resId : btnResIds){
			CustomAudioIcon icon = (CustomAudioIcon)findViewById(resId);
			icon.setOnClickListener(this);
		}				
	}	
	
	private void initReceiver(){
		progressReceiver = new ProgressReceiver();	
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(NatureService.ACTION_UPDATE_PROGRESS);
		intentFilter.addAction(NatureService.ACTION_UPDATE_DURATION);
		intentFilter.addAction(NatureService.ACTION_UPDATE_CURRENT_MUSIC);
		registerReceiver(progressReceiver, intentFilter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {		
		case R.id.btnStartStop:		
			play(currentMusic,R.id.btnStartStop);
			break;		
		case R.id.btnNext:
			natureBinder.toNext();
			break;
		case R.id.btnPrevious:
			natureBinder.toPrevious();
			break;
		case R.id.btnExit:	
			finish();
			break;
		case R.id.btnMode:						
			natureBinder.changeMode();
			break;
		default:
			break;
		}			
	}
	
	private void play(int currentMusic, int resId){
		CustomAudioIcon btnStartStop = (CustomAudioIcon) findViewById(resId);
		if(btnStartStop.isStartStatus()){
			natureBinder.stopPlay();
		}else{
			natureBinder.startPlay(currentMusic,currentPosition);
		}
	}

	
	class ProgressReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(NatureService.ACTION_UPDATE_PROGRESS.equals(action)){
				int progress = intent.getIntExtra(NatureService.ACTION_UPDATE_PROGRESS, currentPosition);				
				if(progress > 0){
					currentPosition = progress;
					tvTimeElapsed.setText(FormatHelper.formatDuration(progress));
					pbDuration.setProgress(progress / 1000);
				}
			}else if(NatureService.ACTION_UPDATE_CURRENT_MUSIC.equals(action)){

				currentMusic = intent.getIntExtra(NatureService.ACTION_UPDATE_CURRENT_MUSIC, 0);					
				tvTitle.setText(musicList.get(currentMusic).getTitle());
			}else if(NatureService.ACTION_UPDATE_DURATION.equals(action)){

				int duration = intent.getIntExtra(NatureService.ACTION_UPDATE_DURATION, 0);
				tvDuration.setText(FormatHelper.formatDuration(duration));
				pbDuration.setMax(duration / 1000);						
			}
		}
		
	}
}