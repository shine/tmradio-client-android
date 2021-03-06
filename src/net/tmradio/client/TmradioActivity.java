package net.tmradio.client;

import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class TmradioActivity extends Activity 
{
	private PlayerService s;
	private ImageButton playStopButton;
	private TextView artist;
	private TextView song_title;
	private TextView popularity;
	private TextView song_id;
	private TextView song_length;
	
	private IntentFilter metaUpdateFilter;
	private IntentFilter shutUpFilter;
	private MainActivityReceiver activityReceiver;
	
	private String current_song_id;
	
	public final static String PLAYER_STATUS = "rotation_player_status";
	private boolean is_player_works;
	
	private static final String SKYPE_PATH_GENERAL = "com.skype.raider";
	private static final String SKYPE_PATH_OLD = "com.skype.raider.contactsync.ContactSkypeOutCallStartActivity";
	private static final String SKYPE_PATH_NEW = "com.skype.raider.Main";	
	private static final String SKYPE_TMRADIO_NUMBER = "tmradio.net";
	private static final String PHONE_TMRADIO_NUMBER = "79117003831";
	private static final String SKYPE_INTENT_ACTION = "android.intent.action.CALL_PRIVILEGED";
	private static final String SKYPE_INTENT_CATEGORY = "android.intent.category.DEFAULT";
	
	public static final String INTENT_VOTING_VOTE = "vote";
	public static final String INTENT_VOTING_TRACK = "track_id";
	public static final String VOTING_PLUS = "rocks";
	public static final String VOTING_MINUS = "sucks";
	
	public static final String DEFAULT_TRACK_NUMBER = "0";
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        artist = (TextView)findViewById(R.id.textViewArtist);
        song_title = (TextView)findViewById(R.id.textViewSong);
        popularity = (TextView)findViewById(R.id.textViewPopularity);
        song_id = (TextView)findViewById(R.id.textViewSongId);
        song_length = (TextView)findViewById(R.id.textViewLength);

        // event listener creation and registration
        activityReceiver = new MainActivityReceiver();        
        
        // headphones plugout event
        shutUpFilter = new IntentFilter(MusicIntentReceiver.INTENT_AUDIO_SHUT_UP);
        getApplicationContext().registerReceiver(activityReceiver, shutUpFilter);
        
        // stream meta updated event
        metaUpdateFilter = new IntentFilter(MetaUpdateService.ACTION_UPDATED);
        getApplicationContext().registerReceiver(activityReceiver, metaUpdateFilter);

        // Start player service and mp3 stream meta update service
        playStopButton = (ImageButton)findViewById(R.id.imageButtonPlayStop);
        if(savedInstanceState != null && savedInstanceState.getBoolean(TmradioActivity.PLAYER_STATUS))
        {
        	is_player_works = savedInstanceState.getBoolean(TmradioActivity.PLAYER_STATUS);
        }
        else
        {
        	is_player_works = false;
        }
        startRelatedServices();

        playStopButton.setOnClickListener(new View.OnClickListener()
        {
          public void onClick(View v) 
          {
        	if(s != null)
        	{
	        	if(s.status())
	        	{
	        		stopPlayer();
	        	}
	        	else
	        	{
					startPlayer();     
	        	}
        	}
          }
        });
        
        // For case of vote before first meta update. 
        current_song_id = DEFAULT_TRACK_NUMBER;
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	unbindService(mConnection);
    	stopService(new Intent(this, MetaUpdateService.class));
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);
    	outState.putBoolean(TmradioActivity.PLAYER_STATUS, is_player_works);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle item selection
        switch (item.getItemId()) 
        {
        	case R.id.settings:
        		showSettings();
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }    

	private ServiceConnection mConnection = new ServiceConnection() 
	{
		public void onServiceConnected(ComponentName className, IBinder binder) 
		{
			s = ((PlayerService.MyBinder) binder).getService();
			if(is_player_works)
			{
				startPlayer();
			}
		}

		public void onServiceDisconnected(ComponentName className) 
		{
			s = null;
		}
	};
	
	class MainActivityReceiver extends BroadcastReceiver 
	{     
	    @Override
	    public void onReceive(Context context, Intent i) 
	    {
			if(i.getAction().equals(MusicIntentReceiver.INTENT_AUDIO_SHUT_UP))
			{
	    		stopPlayer();
			}
			else if (i.getAction().equals(MetaUpdateService.ACTION_UPDATED)) 
			{
				Bundle b = i.getExtras();
				artist.setText(b.getString(MetaUpdateService.INTENT_UPDATED_ARTIST));
				song_title.setText(b.getString(MetaUpdateService.INTENT_UPDATED_SONG));
				song_id.setText("# "+String.valueOf(b.getInt(MetaUpdateService.INTENT_UPDATED_ID)));
				current_song_id = String.valueOf(b.getInt(MetaUpdateService.INTENT_UPDATED_ID));
				song_length.setText(b.getString(MetaUpdateService.INTENT_UPDATED_LENGTH));
				popularity.setText(String.valueOf(b.getString(MetaUpdateService.INTENT_UPDATED_POPULARITY)));
			}
	    }
	}
	
	void startRelatedServices() 
	{
		bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		startService(new Intent(this, MetaUpdateService.class));
	}
	
	public void showSettings()
	{
		startActivity(new Intent(this, ConfigActivity.class));
	}
	
	public void showChat(View v)
	{
		startActivity(new Intent(this, ChatActivity.class));
	}
	
	public void callToTmradio(View v)
	{
		Context context = getApplicationContext();
		Intent skypeIntent = new Intent().setAction(SKYPE_INTENT_ACTION);

		skypeIntent.addCategory(SKYPE_INTENT_CATEGORY);
		skypeIntent.setData(Uri.parse("tel:" + SKYPE_TMRADIO_NUMBER));

		if (isIntentAvailable(context, skypeIntent.setClassName(SKYPE_PATH_GENERAL, SKYPE_PATH_NEW))) 
		{
		    startActivity(skypeIntent);
		} 
		else if (isIntentAvailable(context, skypeIntent.setClassName(SKYPE_PATH_GENERAL, SKYPE_PATH_OLD))) 
		{
		    startActivity(skypeIntent);
		} 
		else 
		{
	        Intent callIntent = new Intent(Intent.ACTION_CALL);
	        callIntent.setData(Uri.parse("tel:"+PHONE_TMRADIO_NUMBER));
	        startActivity(callIntent);
		}
	}
	
	public void voteRocks(View v)
	{
		vote(VOTING_PLUS);
		
		Toast.makeText(getApplicationContext(), getString(R.string.voted_plus), Toast.LENGTH_SHORT).show();
	}

	public void voteSucks(View v)
	{
		vote(VOTING_MINUS);
		
		Toast.makeText(getApplicationContext(), getString(R.string.voted_minus), Toast.LENGTH_SHORT).show();
	}
	
	private void vote(String vote)
	{
		Intent i = new Intent(this, VotingService.class);
		i.putExtra(INTENT_VOTING_VOTE, vote);
		i.putExtra(INTENT_VOTING_TRACK, current_song_id);
		
		startService(i);
	}
	
	private void startPlayer()
	{
		s.startPlayer();     

		playStopButton.setImageResource(R.drawable.ic_menu_stop);
		is_player_works = true;
	}

	private void stopPlayer()
	{
		s.stopPlayer();
		
		playStopButton.setImageResource(R.drawable.ic_menu_play_clip);
		is_player_works = false;
	}
	
	private static boolean isIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(
	        intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return list.size() > 0;
	}
}