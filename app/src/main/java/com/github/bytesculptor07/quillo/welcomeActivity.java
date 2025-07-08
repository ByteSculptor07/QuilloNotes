package com.github.bytesculptor07.quillo;

import android.os.Bundle;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.os.Build;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.android.Auth;

public class welcomeActivity extends AppCompatActivity {

    private static final String APP_KEY = BuildConfig.DROPBOX_KEY;
    boolean authRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow()
                    .setFlags(
                            WindowManager.LayoutParams.FLAG_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Button close = findViewById(R.id.close);
        Button signin = findViewById(R.id.signin);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences prefs = getSharedPreferences("dropbox", MODE_PRIVATE);
                prefs.edit().putBoolean("useWithoutAccount", true).apply();
                finish();
            }
        });

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    try {
                startDropboxSignIn();
            } catch(Exception e) {
                Toast.makeText(welcomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            }
        });
        
        
        /*

        // Konfiguration für die Dropbox-Authentifizierung erstellen
        if (savedInstanceState == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("Quillo").build();
            DbxAppInfo info = new DbxAppInfo(APP_KEY);
            webAuth = new DbxPKCEWebAuth(config, info);
        }
        sessionStore = new NoteSessionStore(welcomeActivity.this);
        */
    }

    private void startDropboxSignIn() {
        // Erzeuge die Autorisierungs-URL
        /*
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .withRedirectUri(REDIRECT_URI, sessionStore) // Implementiere OAuth2SessionStore wenn nötig
                //.withNoRedirect()
                .build();
        
        authorizeUrl = webAuth.authorize(webAuthRequest);
        // Öffne die URL im Browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl));
        startActivity(browserIntent);
        */
        authRequested = true;
        DbxRequestConfig sDbxRequestConfig = DbxRequestConfig.newBuilder("quillo")
                    .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                    .build();
        Auth.startOAuth2PKCE(this, APP_KEY, sDbxRequestConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();/*
        DbxCredential authDbxCredential = Auth.getDbxCredential(); //fetch the result from the AuthActivity
        if (authDbxCredential == null) {
            Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "not null", Toast.LENGTH_SHORT).show();
        }
        */
        
        
        SharedPreferences prefs = getSharedPreferences("dropbox", MODE_PRIVATE);

        String serailizedCredental = prefs.getString("credential", null);

        if (serailizedCredental == null && authRequested) {
            DbxCredential credential = Auth.getDbxCredential();

            if (credential != null) {
                prefs.edit().putString("credential", credential.toString()).apply();
                prefs.edit().putBoolean("useWithoutAccount", false).apply();
                finish();
            } else {
                Toast.makeText(welcomeActivity.this, "an error occured, please try again", Toast.LENGTH_SHORT).show();
            }
        } else if (serailizedCredental != null) {
            finish();
        }
        
        /*
            DbxCredential credential = Auth.getDbxCredential();
            if (credential != null) {
                //Log.d("DropboxAuth", "Access Token: " + credential.getAccessToken());
                Toast.makeText(this, "Access Token: " + credential.getAccessToken(), Toast.LENGTH_SHORT).show();
                DbxRequestConfig config = DbxRequestConfig.newBuilder("quillo")
                        .withHttpRequestor(new OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
                        .build();
            
                ExecutorService executorService = Executors.newSingleThreadExecutor();

                executorService.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                DbxClientV2 client = new DbxClientV2(config, credential);
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
                                                        Toast.makeText(welcomeActivity.this, "url: " + profileurl, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                } catch(DbxException e) {
                                    new Handler(Looper.getMainLooper())
                                                .post(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(welcomeActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                            });
                                }
                            }
                    });
            } else {
                //Log.e("DropboxAuth", "Credential ist null");
                Toast.makeText(this, "c is null", Toast.LENGTH_SHORT).show();
            }
        */
        /*
        Intent intent = getIntent();
    if (intent != null && intent.getData() != null) {
        String uri = intent.getData().toString();
        Toast.makeText(this, "uri: " + uri, Toast.LENGTH_SHORT).show();
        if (uri.startsWith("db-" + APP_KEY)) {
            // Analysiere die URI und extrahiere die Tokens
            //DbxCredential credential = Auth.getDbxCredential();
            if (credential != null) {
                //Log.d("DropboxAuth", "Access Token: " + credential.getAccessToken());
                Toast.makeText(this, "Access Token: " + credential.getAccessToken(), Toast.LENGTH_SHORT).show();
            } else {
                //Log.e("DropboxAuth", "Credential ist null");
                Toast.makeText(this, "c is null", Toast.LENGTH_SHORT).show();
            }
        }
    }
        */
        //dropboxApiWrapper.getCurrentAccount()
    }
    
/*
    @Override
    protected void onResume() {
        super.onResume();
        try{
        DbxCredential credential = Auth.getDbxCredential();
        Toast.makeText(this, "credentials: " + credential.toString(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
    */
        /*
        Toast.makeText(this, "sessionstore: " + sessionStore.get(), Toast.LENGTH_SHORT).show();
        // Prüfen, ob wir von der Redirect-URI zurückgekommen sind
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            if (code != null) {
                Map<String, String[]> params = new HashMap<>();
                params.put("code", new String[]{code});
                params.put("state", new String[]{state});
                try {
                    DbxAuthFinish authFinish = webAuth.finishFromRedirect(uri.toString(), sessionStore, params);
                    Toast.makeText(welcomeActivity.this, "id: " + authFinish.getUserId() + ", token: " + authFinish.getAccessToken(), Toast.LENGTH_SHORT).show();
                } catch(Exception e) {
                    Toast.makeText(this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                //finishDropboxSignIn(code);
            } else {
                Toast.makeText(this, "Fehler bei der Anmeldung", Toast.LENGTH_SHORT).show();
            }
        }
        */
    //}
}