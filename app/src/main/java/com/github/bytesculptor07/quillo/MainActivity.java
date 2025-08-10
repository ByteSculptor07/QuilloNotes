package com.github.bytesculptor07.quillo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.res.Configuration;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.content.pm.ActivityInfo;

import com.github.bytesculptor07.quillo.Utils.SyncUtils;

//import com.github.bytesculptor07.quillo.drawActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.DbxException;

public class MainActivity extends AppCompatActivity {

    View oldButton;

    DbxClientV2 client;
    
    boolean fistlaunch = true;

    Fragment visibleFragment;

    HomeFragment homeFrag;
    SharednotebooksFragment sharednotebooksFrag;
    TemplatesFragment templatesFrag;
    FlashcardsFragment flashcardsFrag;
    StatisticsFragment statisticsFrag;
    SettingsFragment settingsFrag;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initUi();
        initFragments();


        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
            new IntentFilter("DRAW_ACTIVITY_CLOSED"));
    }
    
    public void initUi() {
        findViewById(R.id.parentLayout).getForeground().setAlpha(0);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            initLandscape();
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            initPortrait();
        }
    }

    public void initFragments() {
        createAllFragments();
        attachAllFragments(false);
    }

    public void createAllFragments() {
        homeFrag = new HomeFragment();
        sharednotebooksFrag = new SharednotebooksFragment();
        templatesFrag = new TemplatesFragment();
        flashcardsFrag = new FlashcardsFragment();
        statisticsFrag = new StatisticsFragment();
        settingsFrag = new SettingsFragment();
    }

    public void attachAllFragments(boolean replace) {
        createFragment(homeFrag, replace);
        createFragment(sharednotebooksFrag, replace);
        createFragment(templatesFrag, replace);
        createFragment(flashcardsFrag, replace);
        createFragment(statisticsFrag, replace);
        createFragment(settingsFrag, replace);

        showFragment(homeFrag);
        visibleFragment = homeFrag;
    }
    
    public void initLandscape() {
        LinearLayout notebooks = findViewById(R.id.notebooks);
        LinearLayout sharednotebooks = findViewById(R.id.sharednotebooks);
        LinearLayout templates = findViewById(R.id.templates);
        LinearLayout flashcards = findViewById(R.id.flashcards);
        LinearLayout statistics = findViewById(R.id.statistics);
        LinearLayout settings = findViewById(R.id.settings);
        
        oldButton = notebooks; //selected
        
        notebooks.setOnClickListener(v -> menuChange(v, homeFrag));
        sharednotebooks.setOnClickListener(v -> menuChange(v, sharednotebooksFrag));
        templates.setOnClickListener(v -> menuChange(v, templatesFrag));
        flashcards.setOnClickListener(v -> menuChange(v, flashcardsFrag));
        statistics.setOnClickListener(v -> menuChange(v, statisticsFrag));
        settings.setOnClickListener(v -> menuChange(v, settingsFrag));
    }
    
    public void initPortrait() {
        FrameLayout layout = findViewById(R.id.fragmentContainer);
        adjustStatusBar(layout);
    }

    private void createFragment(Fragment fragment, boolean replace){
        if (!replace) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .hide(fragment)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commitNow();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .hide(fragment)
                    .commit();

        }
    }
    private void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .show(fragment)
                .commit();
        visibleFragment = fragment;
    }
    private void hideFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .commit();
    }

    private BroadcastReceiver mMessageReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    homeFrag.loadItems();
                    //syncChanges();
                    //SyncUtils.syncAll(MainActivity.this, client);
                    SyncUtils.syncAllAsynchronous(MainActivity.this, client, new SyncUtils.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            homeFrag.loadItems();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("Sync", "Failed to sync data: " + e.getMessage());
                        }
                    });
                }
            };

    public void syncInBackground() {
        SyncUtils.syncAllAsynchronous(MainActivity.this, client, new SyncUtils.SyncCallback() {
            @Override
            public void onSuccess() {
                //pass
            }
            @Override
            public void onError(Exception e) {
                //pass
            }
        });
    }

    private void menuChange(View view, Fragment destFragment) {
        if (oldButton != view) {
            oldButton.setBackgroundResource(R.drawable.menu_button_background);
            oldButton = view;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> view.setBackgroundResource(R.drawable.button_background_selected), 100);
        }

        hideFragment(visibleFragment);
        showFragment(destFragment);
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        initUi();
        attachAllFragments(true);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (client == null) {
            startAuth();
        }
    }
    
    public void startAuth() {
        SharedPreferences prefs = getSharedPreferences("dropbox", MODE_PRIVATE);
        String serailizedCredental = prefs.getString("credential", null);
        boolean useWithoutAccount = prefs.getBoolean("useWithoutAccount", false);

        if (serailizedCredental == null && !useWithoutAccount) {
            Intent intent = new Intent(MainActivity.this, welcomeActivity.class);
            startActivity(intent);
        } else if(serailizedCredental != null && !useWithoutAccount) {
            try {
                DbxCredential credential = DbxCredential.Reader.readFully(serailizedCredental);
                initDropbox(credential);
                SyncUtils.syncAllAsynchronous(MainActivity.this, client, new SyncUtils.SyncCallback() {
                    @Override
                    public void onSuccess() {
                        homeFrag.loadItems();
                        Log.i("Sync", "Sync successful");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Sync", "Failed to sync data: " + e.getMessage());
                    }
                });

            } catch (JsonReadException e) {
                Toast.makeText(this, "an error occured: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void adjustStatusBar(FrameLayout layout) {
        ViewCompat.setOnApplyWindowInsetsListener(layout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);


            return WindowInsetsCompat.CONSUMED;
        });

        boolean isDarkMode = (MainActivity.this.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+ (API 30+)
            window.setStatusBarColor(getColor(R.color.background)); // Optional background color
            if (!isDarkMode) {
                window.getInsetsController().setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-10 (API 23-29)
            window.setStatusBarColor(getColor(R.color.background)); // Optional background color
            if (!isDarkMode) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }

        }
    }
    public void initDropbox(DbxCredential credential) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("quillo")
                        .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                        .build();
        
        client = new DbxClientV2(config, credential);
        
        ExecutorService executorService = Executors.newSingleThreadExecutor();

                executorService.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    //String name = client.users().getCurrentAccount().getName().getDisplayName();
                                    FullAccount account = client.users().getCurrentAccount();
                                    String profileurl = account.getProfilePhotoUrl();
                                    String name = account.getName().getDisplayName();
                        
                                    new Handler(Looper.getMainLooper())
                                        .post(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //add profile photo here
                                                        Toast.makeText(MainActivity.this, "hello, " + name, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                } catch(DbxException e) {
                                    //we have probably been logged out, so delete access token and start new auth
                                    SharedPreferences prefs = getSharedPreferences("dropbox", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.remove("credential"); // Remove the specific key-value pair
                                    editor.apply(); // Apply the changes
                        
                                    new Handler(Looper.getMainLooper())
                                                .post(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(MainActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                            });
                                    startAuth();
                                }
                            }
                    });
    }

    @Override
    protected void onDestroy() {
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }


}
