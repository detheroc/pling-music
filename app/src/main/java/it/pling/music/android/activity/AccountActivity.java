package it.pling.music.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import net.sourceforge.subsonic.androidapp.activity.HelpActivity;
import net.sourceforge.subsonic.androidapp.activity.MainActivity;
import net.sourceforge.subsonic.androidapp.service.DownloadServiceImpl;
import net.sourceforge.subsonic.androidapp.service.MusicService;
import net.sourceforge.subsonic.androidapp.service.MusicServiceFactory;
import net.sourceforge.subsonic.androidapp.util.Constants;
import net.sourceforge.subsonic.androidapp.util.ErrorDialog;
import net.sourceforge.subsonic.androidapp.util.ModalBackgroundTask;
import net.sourceforge.subsonic.androidapp.util.Util;

import it.pling.music.android.R;

public class AccountActivity extends Activity {
	
	private static final String TAG = AccountActivity.class.getSimpleName();
	
	private Button DemoButton;
	private Button SignInButton;
	private Button OfflineButton;
	private Button HelpButton;
	private EditText UsernameEditText;
	private EditText PasswordEditText;
	
	private boolean testingConnection = false;
	
	/**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        
        if (getIntent().hasExtra(Constants.INTENT_EXTRA_NAME_EXIT)) {
        	stopService(new Intent(this, DownloadServiceImpl.class));
        }
        
        //If account is configured, go for it..
        if (Util.isAccountConfigured(AccountActivity.this)){
        	launchSubsonic(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
        	initAccountActivity();
        }
    }
    
    public void initAccountActivity() {
    	setContentView(R.layout.account);
        
        DemoButton = (Button) findViewById(R.id.account_demo_button);
        SignInButton = (Button) findViewById(R.id.account_signin_button);
        OfflineButton = (Button) findViewById(R.id.account_offline_button);
        HelpButton = (Button) findViewById(R.id.account_help_button);
        
        UsernameEditText = (EditText) findViewById(R.id.account_username_edittext);
        PasswordEditText = (EditText) findViewById(R.id.account_password_edittext);
        
        DemoButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				configureDemo();
				launchSubsonic(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			}
		});
        
        SignInButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String username = UsernameEditText.getText().toString();
				String password = PasswordEditText.getText().toString();
				Util.configureAccount(AccountActivity.this, 2, username, password);
				testConnection(2);
			}
		});
        
        OfflineButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Util.setOffline(AccountActivity.this, true);
				if (Util.isAccountConfigured(AccountActivity.this))
					Util.setActiveServer(AccountActivity.this, 2);
				else
					Util.setActiveServer(AccountActivity.this, 1);
				
				launchSubsonic(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			}
		});
        
        HelpButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(AccountActivity.this, HelpActivity.class));
			}
		});
        
        if (!Util.getUsername(AccountActivity.this, 2).equals("")){
        	UsernameEditText.setText(Util.getUsername(AccountActivity.this, 2));
        }
    }
    
    public void launchSubsonic(int flags){
    	Intent intent = new Intent(AccountActivity.this, MainActivity.class);
    	intent.setFlags(flags);
    	startActivity(intent);
    }
    
    public void configureDemo() {
    	Util.setActiveServer(AccountActivity.this, 1);
    }
    
    private void testConnection(final int instance) {
        ModalBackgroundTask<Boolean> task = new ModalBackgroundTask<Boolean>(this, false) {
            private int previousInstance;

            @Override
            protected Boolean doInBackground() throws Throwable {
                updateProgress(R.string.settings_testing_connection);

                previousInstance = Util.getActiveServer(AccountActivity.this);
                testingConnection = true;
                Util.setActiveServer(AccountActivity.this, instance);
                try {
                    MusicService musicService = MusicServiceFactory.getMusicService(AccountActivity.this);
                    musicService.ping(AccountActivity.this, this);
                    return true;
                } finally {
                    Util.setActiveServer(AccountActivity.this, previousInstance);
                    testingConnection = false;
                }
            }

            @Override
            protected void done(Boolean responseOk) {
            	if (responseOk) {
            		Util.setActiveServer(AccountActivity.this, instance);
            		launchSubsonic(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            	} else {
            		Log.i(TAG, "Error sucks");
            		Util.setActiveServer(AccountActivity.this, 1);
            	}
            }

            @Override
            protected void cancel() {
                super.cancel();
                Util.setActiveServer(AccountActivity.this, previousInstance);
            }

            @Override
            protected void error(Throwable error) {
                Log.w(TAG, error.toString(), error);
                Util.setActiveServer(AccountActivity.this, 1);
                new ErrorDialog(AccountActivity.this, getResources().getString(R.string.settings_connection_failure) +
                        " " + getErrorMessage(error), false);
            }
        };
        task.execute();
    }
}
