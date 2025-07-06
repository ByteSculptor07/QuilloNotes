package com.github.bytesculptor07.quillo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.res.Configuration;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.shapes.OvalShape;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.content.pm.ActivityInfo;

import com.github.bytesculptor07.quillo.Utils.GeneralUtils;
import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.database.FoldersDatabaseHelper;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Folder;
import com.github.bytesculptor07.quillo.models.Page;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;

//import com.github.bytesculptor07.quillo.drawActivity;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

public class MainActivity extends AppCompatActivity {
    LinearLayout layout_MainMenu;
    PopupWindow popupWindow;
    PopupWindow notebookPopupWindow;
    View oldButton;
    View oldIcon;
    int selectedIcon;
    View oldColor;
    ShapeableImageView oldTemplateButton;
    int selectedTemplate;
    int selectedColor;
    LinearLayout folderList;
    String currentFolder = "/";
    FoldersDatabaseHelper foldersdbhelper;
    NotesDatabaseHelper notesdbhelper;
    DbxClientV2 client;
    
    boolean fistlaunch = true;
    
    public final int TOOL_HIGHLIGHTER = 1;
    
    public final int BACKGROUND_LINES = 8;
    public final int BACKGROUND_SQUARE = 9;
    public final int BACKGROUND_EMPTY = 10;
    public final int BACKGROUND_DOT = 11;
    public final int BACKGROUND_MUSIC = 12;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        foldersdbhelper = FoldersDatabaseHelper.getInstance(this);
        notesdbhelper = NotesDatabaseHelper.getInstance(this);
        
        initUi();


        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
            new IntentFilter("DRAW_ACTIVITY_CLOSED"));
    }
    
    public void initUi() {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            initLandscape();
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            initPortrait();
        }
        
        loadItems();
        loadPopup();
        loadNotebookPopup();
    }
    
    public void initLandscape() {
        LinearLayout notebooks = findViewById(R.id.notebooks);
        LinearLayout sharednotebooks = findViewById(R.id.sharednotebooks);
        LinearLayout templates = findViewById(R.id.templates);
        LinearLayout flashcards = findViewById(R.id.flashcards);
        LinearLayout statistics = findViewById(R.id.statistics);
        LinearLayout settings = findViewById(R.id.settings);
        
        Button create = findViewById(R.id.create);
        folderList = findViewById(R.id.folderList);
        
        oldButton = notebooks; //selected
        
        notebooks.setOnClickListener(menuChange);
        sharednotebooks.setOnClickListener(menuChange);
        templates.setOnClickListener(menuChange);
        flashcards.setOnClickListener(menuChange);
        statistics.setOnClickListener(menuChange);
        settings.setOnClickListener(menuChange);
        
        create.setOnClickListener(createNew);
    }
    
    public void initPortrait() {
        LinearLayout buttonPanel = findViewById(R.id.buttonPanel);
        adjustStatusBar(buttonPanel);

        folderList = findViewById(R.id.folderList);
        Button create = findViewById(R.id.create);

        create.setOnClickListener(createNew);
    }

    private BroadcastReceiver mMessageReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    loadItems();
                    //syncChanges();
                    //SyncUtils.syncAll(MainActivity.this, client);
                    SyncUtils.syncAllAsynchronous(MainActivity.this, client, new SyncUtils.SyncCallback() {
                        @Override
                        public void onSuccess() {
                            loadItems();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("Sync", "Failed to sync data: " + e.getMessage());
                        }
                    });
                }
            };

    private final View.OnClickListener menuChange = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (oldButton != view) {
                oldButton.setBackgroundResource(R.drawable.menu_button_background);
                oldButton = view;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> view.setBackgroundResource(R.drawable.button_background_selected), 100);
            }
        }
    };
    
    private final View.OnClickListener createNew = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            /*
            Folder folder = new Folder();
            folder.setName("Maths");
            folder.setIcon(R.drawable.icon_folder_math);
            folder.setColor(R.color.folder_blue);
            folder.setPath(currentFolder);
            foldersdbhelper.addFolder(folder);
            loadFolder();
            */
            //showPopup();
            showPopupMenu(view);
        }
    };
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
        initUi();
        /*
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main_landscape); // Manually inflate landscape layout
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main); // Manually inflate portrait layout
        }
        */
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
                        loadItems();
                        Log.i("Sync", "Sync successful");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Sync", "Failed to sync data: " + e.getMessage());
                    }
                });
                /*
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                //syncNotes();
                                SyncUtils.syncAll(MainActivity.this, client);
                                //syncFolder();
                            }
                        });
                 */
            } catch (JsonReadException e) {
                Toast.makeText(this, "an error occured: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void adjustStatusBar(LinearLayout buttonPanel) {
        ViewCompat.setOnApplyWindowInsetsListener(buttonPanel, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as a margin to the view. This solution sets only the
            // bottom, left, and right dimensions, but you can apply whichever insets are
            // appropriate to your layout. You can also update the view padding if that's
            // more appropriate.
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
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
    
    public String hex(byte[] data) {
        char[] buf = new char[2*data.length];
        int i = 0;
        char[] hex_digits = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'};
        for (byte b : data) {
            buf[i++] = hex_digits[(b & 0xf0) >>> 4];
            buf[i++] = hex_digits[b & 0x0f];
        }
        return new String(buf);
    }
    /*
    public void syncNotes() {
        listFolderRecursive(client, "", new FolderListCallback() {
            @Override
            public void onSuccess(List<Metadata> syncedNotes) {
                //new Handler(Looper.getMainLooper()).post(() -> {
                    List<Page> allNotes = notesdbhelper.getAllPages();
                    new Handler(Looper.getMainLooper()).post(() -> 
                        Toast.makeText(MainActivity.this, "Synced Notes: " + syncedNotes.size() + ", total: " + allNotes.size(), Toast.LENGTH_SHORT).show()
                    );
                    List<String> syncedNoteNames = new ArrayList<>();
                    for (Metadata md : syncedNotes) {
                        syncedNoteNames.add(md.getPathLower());
                    }
                            
                    //check for old notes which are not synced
                    List<String> localNoteNames = new ArrayList<>();
                    for (Page pg : allNotes) {
                        localNoteNames.add(pg.getNoteId() + "_" + pg.getNumber() + ".qdoc");
                        if (!syncedNoteNames.contains("/notes" + pg.getNoteId() + "_" + pg.getNumber() + ".qdoc")) {
                            notesdbhelper.updatePage(pg); //add changes flag
                            //Log.d("Debug", "note not found: " + pg.getNoteId());
                            //Log.d("Debug", "Number: " + pg.getNumber());
                            new Handler(Looper.getMainLooper()).post(() -> 
                                Toast.makeText(MainActivity.this, "syncing note: " + pg.getNoteId(), Toast.LENGTH_SHORT).show()
                            );
                        } else {
                            //Log.d("Debug", "note was found: " + pg.getNoteId());
                        }
                    }
                    syncChanges();
                            
                    //check for new notes from another device which are not downloaded
                    for (String name : syncedNoteNames) {
                        if (!localNoteNames.contains(name.substring(6))) {
                            //download note
                            try {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                try {
                                    FileMetadata metadata = client.files()
                                            .downloadBuilder(name)
                                            .download(outputStream);

                                    byte[] fileContent = outputStream.toByteArray();
                                    String fileContentAsString = new String(fileContent, StandardCharsets.UTF_8);
                                    notesdbhelper.addPage(Page.importFromJson(fileContentAsString), SyncUtils.STATUS_SYNCED);
                                } finally {
                                    outputStream.close();
                                }
                            } catch(IOException e) {
                                new Handler(Looper.getMainLooper()).post(() -> 
                                    Toast.makeText(MainActivity.this, "ioexception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            } catch(DbxException e) {
                                new Handler(Looper.getMainLooper()).post(() -> 
                                    Toast.makeText(MainActivity.this, "dbxecception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            }
                        }
                    }
                    
                    
                    //check for deprecated notes
                    for (Metadata mtd : syncedNotes) {
                        String cloudHash = ((FileMetadata) mtd).getContentHash();
                        //Toast.makeText(MainActivity.this, "hash: " + cloudHash, Toast.LENGTH_SHORT).show();
                                
                        String path = mtd.getPathLower();
                        List<Page> pgs = notesdbhelper.getPagesByNoteId(NoteParser.extractNoteId(path));
                        String content = null;
                        Page localPage = new Page();
                        for (Page pg : pgs) {
                            if (pg.getNumber() == NoteParser.extractNumber(path)) {
                                localPage = pg;
                                content = pg.exportToJson();
                                break;
                            }
                        }
                                
                        MessageDigest hasher = new DropboxContentHasher();
                        byte[] buf = new byte[1024];
                        InputStream in = null;
                        if (content != null) {
                            in = new ByteArrayInputStream(content.getBytes());
                        }
                        try {
                            while (true) {
                                int n = 0;
                                if (in != null) {
                                    n = in.read(buf);
                                }
                                if (n < 0) break;  // EOF
                                hasher.update(buf, 0, n);
                            }
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        byte[] rawHash = hasher.digest();
                        //System.out.println(hex(rawHash));
                        String localHash = hex(rawHash);

                        if (cloudHash != null && !cloudHash.equals(localHash)) {
                            //Toast.makeText(MainActivity.this, "note needs to be synced", Toast.LENGTH_SHORT).show();
                            //if hashes are different, changes are probably made by another device, because we synced our changes, so download
                            try {
                                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                                    FileMetadata metadata = client.files()
                                            .downloadBuilder(path)
                                            .download(outputStream);

                                    byte[] fileContent = outputStream.toByteArray();
                                    String fileContentAsString = new String(fileContent, StandardCharsets.UTF_8);
                                    notesdbhelper.updatePage(Page.importFromJson(fileContentAsString), SyncUtils.STATUS_SYNCED);
                                    Log.d("Debug", "different hash, downloading note: " + localPage.getNoteId() + ", page: " + localPage.getNumber());
                                    //Log.d("Debug", "cloudHash: " + cloudHash);
                                    //Log.d("Debug", "localHash: " + localHash);
                                }
                            } catch(IOException e) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(MainActivity.this, "ioexception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            } catch(DbxException e) {
                                new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(MainActivity.this, "dbxecception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                            }
                        } else {
                            Log.d("Debug", "same hash, keeping note: " + localPage.getNoteId() + ", page: " + localPage.getNumber());
                        }
                    }
                new Handler(Looper.getMainLooper()).post(() -> 
                    loadItems()
                );
            }
    
            @Override
            public void onError(Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    */
    /*
    public void syncChanges() {
        List<Page> pages = notesdbhelper.getPagesWithChanges();
        for (Page pg : pages) {
            //Toast.makeText(MainActivity.this, pg.exportToJson(), Toast.LENGTH_SHORT).show();
            
            ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String content = pg.exportToJson();
                                    try {
                                        InputStream in = new ByteArrayInputStream(content.getBytes());
                                        String filepath = "/notes" + pg.getNoteId() + "_" + pg.getNumber() + ".qdoc";
                                        FileMetadata metadata = client.files().uploadBuilder(filepath)
                                        .withMode(WriteMode.OVERWRITE)
                                        .uploadAndFinish(in);
                                        notesdbhelper.removePageChanges(pg);
                                    } catch(IOException e) {
                                        new Handler(Looper.getMainLooper()).post(() -> 
                                            Toast.makeText(MainActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                } catch(DbxException e) {
                                    new Handler(Looper.getMainLooper())
                                                .post(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(MainActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                            });
                                }
                            }
                    });
            
        }
    }

    public interface FolderListCallback {
        void onSuccess(List<Metadata> result);
        void onError(Exception e);
    }
    
    public void listFolders(FolderListCallback callback) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            try {
                List<Metadata> funcresult = new ArrayList<>();
                ListFolderResult result = client.files().listFolder("/folder");
    
                while (true) {
                    funcresult.addAll(result.getEntries());
    
                    if (!result.getHasMore()) {
                        break;
                    }
    
                    result = client.files().listFolderContinue(result.getCursor());
                }
    
                callback.onSuccess(funcresult);
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        });
    }
    
    public void syncFolder() {
        //upload changes
        syncFolderChanges();
        //download new
        listFolders(new FolderListCallback() {
            @Override
            public void onSuccess(List<Metadata> folders) {
                List<Folder> localFolderList = foldersdbhelper.getFolders();
                List<String> localFolders = new ArrayList<>();
                for (Folder localF : localFolderList) {
                    localFolders.add("/folder/" + String.valueOf(localF.getId()) + ".qdata");
                }
                for (Metadata folder : folders) {
                    if (!localFolders.contains(folder.getPathLower())) {
                        //download
                        try {
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            try {
                                FileMetadata metadata = client.files()
                                        .downloadBuilder(folder.getPathLower())
                                        .download(outputStream);

                                byte[] fileContent = outputStream.toByteArray();
                                String fileContentAsString = new String(fileContent, StandardCharsets.UTF_8);
                                foldersdbhelper.addFolder(Folder.importFromJson(fileContentAsString), SyncUtils.STATUS_SYNCED);
                            } finally {
                                outputStream.close();
                            }
                        } catch(IOException e) {
                            new Handler(Looper.getMainLooper()).post(() -> 
                                Toast.makeText(MainActivity.this, "ioexception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        } catch(DbxException e) {
                            new Handler(Looper.getMainLooper()).post(() -> 
                            Toast.makeText(MainActivity.this, "dbxecception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                }
            }
        
            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        //download changes
        //upload changes
    }
    
    public void syncFolderChanges() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                List<Folder> folders = foldersdbhelper.getFolderWithChanges();
                                for (Folder folder : folders) {
                                    String content = folder.exportToJson();
                                    try {
                                        InputStream in = new ByteArrayInputStream(content.getBytes());
                                        String filepath = "/folder/" + String.valueOf(folder.getId()) + ".qdata";
                                        FileMetadata metadata = client.files().uploadBuilder(filepath)
                                        .withMode(WriteMode.OVERWRITE)
                                        .uploadAndFinish(in);
                                        foldersdbhelper.removeFolderChanges(folder);
                                    } catch(IOException e) {
                                        new Handler(Looper.getMainLooper()).post(() -> 
                                            Toast.makeText(MainActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                }
                            } catch(DbxException e) {
                                    new Handler(Looper.getMainLooper())
                                                .post(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Toast.makeText(MainActivity.this, "exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                            });
                                }
                            }
                    });
    }
    */

    public void loadItems() {
        folderList.post(
                new Runnable() {
                    @Override
                    public void run() {
                        folderList.removeAllViews();
                        int maxLayoutWidth = folderList.getWidth();
                        int currentWidth = 0;

                        LinearLayout currentLayout = createLayout();
                        folderList.addView(currentLayout);

                        // load folders
                        for (Folder folder : foldersdbhelper.getFoldersByPath(currentFolder)) {
                            View folderView = createFolder(folder);
                            folderView.measure(
                                    View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                            int folderWidth = folderView.getMeasuredWidth();

                            if (currentWidth + folderWidth > maxLayoutWidth) {
                                currentLayout = createLayout();
                                folderList.addView(currentLayout);
                                currentWidth = 0;
                            }
                            currentLayout.addView(folderView);
                            currentWidth += folderWidth;
                        }
                        
                        LinearLayout placeholder = createLayout();
                        placeholder.getLayoutParams().height = GeneralUtils.dpToPx(MainActivity.this, 15);
                        folderList.addView(placeholder);
                        currentLayout = createLayout();
                        folderList.addView(currentLayout);
                        currentWidth = 0;

                        // load notes
                        List<String> ids = new ArrayList<>();
                        for (Page page : notesdbhelper.getAllPages()) {
                            if (getPathOfNoteid(page.getNoteId()).equals(currentFolder)
                                    && !ids.contains(page.getNoteId())) {
                                ids.add(page.getNoteId());
                                View noteView = createNote(page.getNoteId());
                                noteView.measure(
                                        View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                                int noteWidth = noteView.getMeasuredWidth();

                                if (currentWidth + noteWidth > maxLayoutWidth) {
                                    currentLayout = createLayout();
                                    folderList.addView(currentLayout);
                                    currentWidth = 0;
                                }
                                currentLayout.addView(noteView);
                                currentWidth += noteWidth;
                            }
                        }
                    }
                });
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
    
    public LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT));
        return layout;
    }
    
    public static String getPathOfNoteid(String path) {
        int lastIndex = path.lastIndexOf('/');
        if (lastIndex > 0) {
            return path.substring(0, lastIndex + 1);
        }
        return "/";
    }
    
    public static String getNameOfNoteid(String path) {
        int lastIndex = path.lastIndexOf('/');
        if (lastIndex >= 0 && lastIndex < path.length() - 1) {
            return path.substring(lastIndex + 1);
        }
        return "";
    }
    
    private void showPopupMenu(View view) {
        //Context wrapper = new ContextThemeWrapper(this, R.style.Quillo_PopupMenuStyle);
        //PopupMenu popupMenu = new PopupMenu(wrapper, view);
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_notebook) {
                        //Toast.makeText(MainActivity.this, "create notebook", Toast.LENGTH_SHORT).show();
                        showNotebookPopup();
                } else if  (item.getItemId() == R.id.action_folder){
                        //Toast.makeText(home.this, "Ordner ausgewählt", Toast.LENGTH_SHORT).show();
                        showFolderPopup();
                }
                return true;
            }
        });

        popupMenu.show();
    }

    public View createFolder(Folder folder) {
        ConstraintLayout constraintLayout = new ConstraintLayout(this);
        ConstraintLayout.LayoutParams layoutParams =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintLayout.setLayoutParams(layoutParams);

        // Create an ImageButton
        ImageButton imageButton = new ImageButton(this);
        ConstraintLayout.LayoutParams imageButtonParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 150), GeneralUtils.dpToPx(MainActivity.this, 150));
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageResource(R.drawable.icon_folder);
        int color = ContextCompat.getColor(this, GeneralUtils.colorIds[folder.getColor()]);
        imageButton.setImageTintList(ColorStateList.valueOf(color));
        imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //imageButton.setBackgroundResource(android.R.color.transparent);
        imageButton.setBackgroundResource(R.drawable.menu_button_background);
        imageButton.setId(View.generateViewId());

        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Code to be executed when the button is clicked
                        currentFolder += String.valueOf(folder.getId()) + "/";
                        loadItems();
                    }
                });

        // Create an ImageView
        ImageView imageView = new ImageView(this);
        ConstraintLayout.LayoutParams imageViewParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 50), GeneralUtils.dpToPx(MainActivity.this, 50));
        imageView.setLayoutParams(imageViewParams);
        imageView.setImageResource(GeneralUtils.iconIds[folder.getIcon()]);
        if (GeneralUtils.colorIds[folder.getColor()] == R.color.folder_black) {
            int color2 = ContextCompat.getColor(this, R.color.white);
            imageView.setImageTintList(ColorStateList.valueOf(color2));
        }
        imageView.setPadding(0, GeneralUtils.dpToPx(MainActivity.this, 15), 0, 0);
        imageView.setId(View.generateViewId());

        // Create a TextView
        TextView textView = new TextView(this);
        textView.setText(folder.getName());
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text));
        textView.setGravity(Gravity.CENTER);
        textView.setId(View.generateViewId());

        // Add views to ConstraintLayout
        constraintLayout.addView(imageButton);
        constraintLayout.addView(imageView);
        constraintLayout.addView(textView);

        // Create ConstraintSet to apply constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        // Set constraints for ImageButton
        constraintSet.connect(
                imageButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(
                imageButton.getId(),
                ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT);
        constraintSet.connect(
                imageButton.getId(),
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT);

        // Set constraints for ImageView (icon) to be on top of the ImageButton, centered
        constraintSet.connect(
                imageView.getId(), ConstraintSet.TOP, imageButton.getId(), ConstraintSet.TOP);
        constraintSet.connect(
                imageView.getId(), ConstraintSet.BOTTOM, imageButton.getId(), ConstraintSet.BOTTOM);
        constraintSet.connect(
                imageView.getId(), ConstraintSet.LEFT, imageButton.getId(), ConstraintSet.LEFT);
        constraintSet.connect(
                imageView.getId(), ConstraintSet.RIGHT, imageButton.getId(), ConstraintSet.RIGHT);

        // Set constraints for TextView to be below the ImageButton, centered
        constraintSet.connect(
                textView.getId(),
                ConstraintSet.TOP,
                imageButton.getId(),
                ConstraintSet.BOTTOM,
                -GeneralUtils.dpToPx(MainActivity.this, 25));
        constraintSet.connect(
                textView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        constraintSet.connect(
                textView.getId(),
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT);

        // Apply constraints
        constraintSet.applyTo(constraintLayout);

        // Set the layout as the content view
        //folderList.addView(constraintLayout);
        return constraintLayout;
    }
    
    public View createNote(String noteid) {
        ConstraintLayout constraintLayout = new ConstraintLayout(this);
        ConstraintLayout.LayoutParams layoutParams =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintLayout.setLayoutParams(layoutParams);
        Bitmap thumbnail = null;
        try {
            Page pg = notesdbhelper.getPagesByNoteId(noteid).get(0);
            String data = pg.getData();
            int background = 0;
            if (pg.getBackground() != null && pg.getBackground().startsWith("B") && pg.getBackground().length() <= 3) {
                background = Integer.parseInt(pg.getBackground().substring(1));
            }
            if (data != null) {
                thumbnail = createThumbnail(GeneralUtils.dpToPx(MainActivity.this, 150), GeneralUtils.dpToPx(MainActivity.this, (int) (150 * 2970f / 2100f)), data, background);
            } else {
                thumbnail = Bitmap.createBitmap(GeneralUtils.dpToPx(MainActivity.this, 150), GeneralUtils.dpToPx(MainActivity.this, (int) (150 * 2970f / 2100f)), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(thumbnail);
                canvas.drawColor(Color.WHITE);
            }
        } catch(Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        // Create an ImageButton
        /*
        ImageButton imageButton = new ImageButton(this);
        ConstraintLayout.LayoutParams imageButtonParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 150), GeneralUtils.dpToPx(MainActivity.this, 150));
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageBitmap(thumbnail);
        imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageButton.setBackgroundResource(android.R.color.transparent);
        imageButton.setId(View.generateViewId());
        */
        ShapeableImageView imageButton = new ShapeableImageView(this);
        ConstraintLayout.LayoutParams imageButtonParams =
            new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 150), GeneralUtils.dpToPx(MainActivity.this, (int) (150 * 2970f / 2100f)));
        imageButtonParams.setMargins(GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15));
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageBitmap(thumbnail);
        imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);
        //imageButton.setBackgroundResource(android.R.color.transparent);
        imageButton.setForeground(ContextCompat.getDrawable(MainActivity.this, R.drawable.template_button_ripple));
        imageButton.setId(View.generateViewId());
        imageButton.setShapeAppearanceModel(
                imageButton.getShapeAppearanceModel().toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(MainActivity.this, 16))
                        .build());
        imageButton.setElevation(GeneralUtils.dpToPx(MainActivity.this, 4));
        //imageButton.setPadding(0, GeneralUtils.dpToPx(MainActivity.this, 15), 0, 0);

        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // open note
                        try {
                            Intent intent = new Intent(MainActivity.this, drawActivity.class);
                            intent.putExtra("noteid", noteid);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(intent);
                        } catch(Exception e) {
                            Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Create a TextView
        TextView textView = new TextView(this);
        textView.setText(getNameOfNoteid(noteid));
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.text));
        textView.setGravity(Gravity.CENTER);
        textView.setId(View.generateViewId());

        // Add views to ConstraintLayout
        constraintLayout.addView(imageButton);
        constraintLayout.addView(textView);

        // Create ConstraintSet to apply constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(constraintLayout);

        // Set constraints for ImageButton
        constraintSet.connect(
                imageButton.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        constraintSet.connect(
                imageButton.getId(),
                ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.LEFT);
        constraintSet.connect(
                imageButton.getId(),
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT);

        // Set constraints for TextView to be below the ImageButton, centered
        constraintSet.connect(
                textView.getId(),
                ConstraintSet.TOP,
                imageButton.getId(),
                ConstraintSet.BOTTOM,
                GeneralUtils.dpToPx(MainActivity.this, 4));
        constraintSet.connect(
                textView.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT);
        constraintSet.connect(
                textView.getId(),
                ConstraintSet.RIGHT,
                ConstraintSet.PARENT_ID,
                ConstraintSet.RIGHT);

        // Apply constraints
        constraintSet.applyTo(constraintLayout);

        // Set the layout as the content view
        //folderList.addView(constraintLayout);
        return constraintLayout;
    }
    
    public Bitmap createThumbnail(int imgwidth, int imgheight, String data, int background) {
        Bitmap thumbnail = Bitmap.createBitmap(imgwidth, imgheight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbnail);

        float scaleX = (float) imgwidth / GeneralUtils.dpToPx(MainActivity.this, 2100);
        float scaleY = (float) imgheight / GeneralUtils.dpToPx(MainActivity.this, 2970);
        canvas.scale(scaleX, scaleY);
        
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        Type listType = new TypeToken<List<SerializablePath>>() {}.getType();
        List<SerializablePath> paths = gson.fromJson(data, listType);

        // draw the paths on the canvas of the thumbnail
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        canvas.drawColor(Color.WHITE);
        
        
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2f);
        final float scale = getResources().getDisplayMetrics().density;
        int width = (int) (2100 * scale);
        int height = (int) (2970 * scale);
        int margin = width / 8;
        int h;
        switch (background) {
            case BACKGROUND_LINES:
                int lines_count = 30;
                int height_step = height / (lines_count + 2);
                int height_offset = height_step / 2;
                canvas.drawLine(margin, 0, margin, height, paint);
                canvas.drawLine(width - margin, 0, width - margin, height, paint);

                for (int i = 1; i < lines_count + 1; i++) {
                    h = height_step * i + height_offset;
                    canvas.drawLine(0, h, width, h, paint);
                }
                break;

            case BACKGROUND_SQUARE:
                int square_size = (int) (50 * scale);
                int width_count = (int) Math.ceil(width / square_size);
                int height_count = (int) Math.ceil(height / square_size);
                for (int i = 1; i < height_count + 1; i++) {
                    h = square_size * i;
                    canvas.drawLine(0, h, width, h, paint);
                }
                for (int i = 1; i < width_count + 1; i++) {
                    h = square_size * i;
                    if (i == 6 || width_count - i == 6) {
                        paint.setARGB(200, 100, 100, 100);
                    } else {
                        paint.setARGB(100, 100, 100, 100);
                    }
                    canvas.drawLine(h, 0, h, height, paint);
                }

                break;
            
            case BACKGROUND_DOT:
                paint.setStrokeWidth(8f);
                int dot_distance = (int) (50 * scale);
                int width_count1 = (int) Math.ceil(width / dot_distance);
                int height_count1 = (int) Math.ceil(height / dot_distance);
                for (int i = 1; i < height_count1 + 1; i++) {
                    for (int ii = 1; ii < width_count1 + 1; ii++) {
                        canvas.drawPoint(ii * dot_distance, i * dot_distance, paint);
                    }
                }
                
                break;
            
            case BACKGROUND_MUSIC:
                int count = 11;
                int step = height / ((count + 1) * 2);
                int small_step = step / 5;
                int y;
                
                for (int i = 2; i < count * 2 + 1; i += 2) {
                    for (int ii = 1; ii < 6; ii++) {
                        y = step * i + small_step * ii;
                        canvas.drawLine(margin, y, width - margin, y, paint);
                    }
                }
            
                paint.setARGB(200, 100, 100, 100);
                paint.setStrokeWidth(4f);
                canvas.drawLine(width / 3, step + small_step * 2, width - width / 3, step + small_step * 2, paint);
            
                break;
            
            case BACKGROUND_EMPTY:
                break;
        }
        
        
        for (SerializablePath path : paths) {
            path.convertToPx(MainActivity.this);
            path.loadPoints(MainActivity.this);
            if (path.getTool() == TOOL_HIGHLIGHTER) {
                paint.setColor(path.getColor()); // Use the color stored in each path
                paint.setStrokeWidth(path.getWidth() * 2);
                paint.setPathEffect(null);
                paint.setAlpha(100);
            } else {
                paint.setColor(path.getColor());
                paint.setStrokeWidth(path.getWidth());
                paint.setPathEffect(null);
                paint.setAlpha(255);
            }
            canvas.drawPath(path, paint);
        }

        return thumbnail;
    }
    
    public Bitmap createBackgroundThumbnail(int imgwidth, int imgheight, int bg) {
        Bitmap thumbnail = Bitmap.createBitmap(imgwidth, imgheight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbnail);

        // Berechnen des Skalierungsfaktors
        float scaleX = (float) imgwidth / GeneralUtils.dpToPx(MainActivity.this, 2100);
        float scaleY = (float) imgheight / GeneralUtils.dpToPx(MainActivity.this, 2970);
        canvas.scale(scaleX, scaleY);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        final float scale = getResources().getDisplayMetrics().density;
        int width = (int) (2100 * scale);
        int height = (int) (2970 * scale);
        
        Paint bgpaint = new Paint();
        bgpaint.setColor(Color.WHITE);
        
        canvas.drawRoundRect(new RectF(0, 0, width, height), GeneralUtils.dpToPx(MainActivity.this, 160), GeneralUtils.dpToPx(MainActivity.this, 160), bgpaint);

        //paint.setARGB(200, 100, 100, 100);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2f);
        int margin = width / 8;
        int h;
        switch (bg) {
            case BACKGROUND_LINES:
                int lines_count = 30;
                int height_step = height / (lines_count + 2);
                int height_offset = height_step / 2;
                canvas.drawLine(margin, 0, margin, height, paint);
                canvas.drawLine(width - margin, 0, width - margin, height, paint);

                for (int i = 1; i < lines_count + 1; i++) {
                    h = height_step * i + height_offset;
                    canvas.drawLine(0, h, width, h, paint);
                }
                break;

            case BACKGROUND_SQUARE:
                int square_size = (int) (50 * scale);
                int width_count = (int) Math.ceil(width / square_size);
                int height_count = (int) Math.ceil(height / square_size);
                for (int i = 1; i < height_count + 1; i++) {
                    h = square_size * i;
                    canvas.drawLine(0, h, width, h, paint);
                }
                for (int i = 1; i < width_count + 1; i++) {
                    h = square_size * i;
                    if (i == 6 || width_count - i == 6) {
                        paint.setARGB(200, 100, 100, 100);
                    } else {
                        paint.setARGB(100, 100, 100, 100);
                    }
                    canvas.drawLine(h, 0, h, height, paint);
                }

                break;
            
            case BACKGROUND_DOT:
                paint.setStrokeWidth(8f);
                int dot_distance = (int) (50 * scale);
                int width_count1 = (int) Math.ceil(width / dot_distance);
                int height_count1 = (int) Math.ceil(height / dot_distance);
                for (int i = 1; i < height_count1 + 1; i++) {
                    for (int ii = 1; ii < width_count1 + 1; ii++) {
                        canvas.drawPoint(ii * dot_distance, i * dot_distance, paint);
                    }
                }
                
                break;
            
            case BACKGROUND_MUSIC:
                int count = 11;
                int step = height / ((count + 1) * 2);
                int small_step = step / 5;
                int y;
                
                for (int i = 2; i < count * 2 + 1; i += 2) {
                    for (int ii = 1; ii < 6; ii++) {
                        y = step * i + small_step * ii;
                        canvas.drawLine(margin, y, width - margin, y, paint);
                    }
                }
            
                paint.setARGB(200, 100, 100, 100);
                paint.setStrokeWidth(4f);
                canvas.drawLine(width / 3, step + small_step * 2, width - width / 3, step + small_step * 2, paint);
            
                break;
            
            case BACKGROUND_EMPTY:
                break;
        }

        return thumbnail;
    }
    
    private void loadNotebookPopup() {
        // Inflate your custom layout
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.notebook_popup, null);
        
        View root_view = this.getWindow().getDecorView().getRootView();
        LinearLayout iconList = customView.findViewById(R.id.iconList);
        LinearLayout colorList = customView.findViewById(R.id.colorList);
        LinearLayout popupLayout = customView.findViewById(R.id.popupLayout);
        LinearLayout templateList = customView.findViewById(R.id.templateList);
        Button button_cancel = customView.findViewById(R.id.cancel);
        Button button_create = customView.findViewById(R.id.create);
        EditText textedit_name = customView.findViewById(R.id.textField);
        layout_MainMenu = root_view.findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);
        
        int[] bglist = {BACKGROUND_EMPTY, BACKGROUND_LINES, BACKGROUND_SQUARE, BACKGROUND_DOT, BACKGROUND_MUSIC};
        Bitmap thumbnail = null;
        for (int bg : bglist) {
            thumbnail = createBackgroundThumbnail(GeneralUtils.dpToPx(MainActivity.this, 120), GeneralUtils.dpToPx(MainActivity.this, (int) (120 * 2970f / 2100f)), bg);

            /*
            ConstraintLayout constraintLayout = new ConstraintLayout(this);
            ConstraintLayout.LayoutParams layoutParams =
                    new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT);
            constraintLayout.setLayoutParams(layoutParams);
             */

            //ShapeableImageView imageButton = new ShapeableImageView(this);
            /*
            ImageButton imageButton = new ImageButton(this);
            ConstraintLayout.LayoutParams imageButtonParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 120), GeneralUtils.dpToPx(MainActivity.this, (int) (120 * 2970f / 2100f)));
            imageButtonParams.setMargins(GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15));
            //imageButton.setPadding(GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15));
            imageButton.setLayoutParams(imageButtonParams);
            imageButton.setImageBitmap(thumbnail);*/

            ShapeableImageView imageButton = new ShapeableImageView(this);
            LinearLayout.LayoutParams imageButtonParams =
                    new LinearLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 120), GeneralUtils.dpToPx(MainActivity.this, (int) (120 * 2970f / 2100f)));
            imageButtonParams.setMargins(GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15));
            imageButton.setLayoutParams(imageButtonParams);
            imageButton.setImageBitmap(thumbnail);
            imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);
            //imageButton.setBackgroundResource(android.R.color.holo_red_dark);
            //imageButton.setForeground(ContextCompat.getDrawable(MainActivity.this, android.R.color.holo_red_dark));
            imageButton.setId(View.generateViewId());
            imageButton.setShapeAppearanceModel(
                    imageButton.getShapeAppearanceModel().toBuilder()
                            .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(MainActivity.this, 16))
                            .build());
            //imageButton.setElevation(GeneralUtils.dpToPx(MainActivity.this, 4));

            if (bg == BACKGROUND_EMPTY) {
                //imageButton.setBackgroundResource(R.drawable.button_background_selected);
                imageButton.setForeground(ContextCompat.getDrawable(MainActivity.this, R.drawable.template_button_selected));
                oldTemplateButton = imageButton;
            } else {
                //imageButton.setBackgroundResource(R.drawable.menu_button_background);
                imageButton.setForeground(ContextCompat.getDrawable(MainActivity.this, R.drawable.template_button_ripple));
            }

            //imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);
            //imageButton.setBackgroundResource(android.R.color.transparent);
            //imageButton.setId(View.generateViewId());
            //imageButton.setShapeAppearanceModel(
            //    imageButton.getShapeAppearanceModel().toBuilder()
            //            .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(MainActivity.this, 16))
            //            .build());
            //imageButton.setElevation(GeneralUtils.dpToPx(MainActivity.this, 4));
            
            imageButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                                if ((ShapeableImageView) view != oldTemplateButton) {
                                    oldTemplateButton.setForeground(ContextCompat.getDrawable(MainActivity.this, R.drawable.template_button_ripple));
                                    oldTemplateButton = (ShapeableImageView) view;
                                    Handler handler = new Handler(Looper.getMainLooper());
                                    handler.postDelayed(() -> view.setForeground(ContextCompat.getDrawable(MainActivity.this, R.drawable.template_button_selected)), 100);
                                    selectedTemplate = bg;
                                }
                        }
                    });
            imageButton.setElevation(GeneralUtils.dpToPx(MainActivity.this, 4));

            //constraintLayout.addView(imageButton);
            templateList.addView(imageButton);
        }

        int height = getResources().getDisplayMetrics().heightPixels - GeneralUtils.dpToPx(MainActivity.this, 100); // 50 pixels margin on top and bottom
        notebookPopupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set focusable to true to prevent outside touches from dismissing the popup
        notebookPopupWindow.setFocusable(true);
        
        notebookPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                layout_MainMenu.getForeground().setAlpha(0);
            }
        });
        
        button_cancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(MainActivity.this, "cancel", Toast.LENGTH_SHORT).show();
                        notebookPopupWindow.dismiss();
                    }
                });
        
        button_create.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(MainActivity.this, currentFolder + textedit_name.getText().toString(), Toast.LENGTH_SHORT).show();
                        Page pg = new Page();
                        pg.setNoteId(currentFolder + textedit_name.getText().toString());
                        if (selectedTemplate != BACKGROUND_EMPTY) {
                            pg.setBackground("B" + Integer.toString(selectedTemplate));
                        }
                        pg.setNumber(0);
                        if (!notesdbhelper.pageExist(pg, false)) {
                            notesdbhelper.addPage(pg);
                        } else {
                            Toast.makeText(MainActivity.this, "Notebook exists", Toast.LENGTH_SHORT).show();
                        }
                        loadItems();
                        notebookPopupWindow.dismiss();
                    }
                });
        
        popupLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }
    
    private void loadPopup() {
        // Inflate your custom layout
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.create_popup, null);
        
        View root_view = this.getWindow().getDecorView().getRootView();
        LinearLayout iconList = customView.findViewById(R.id.iconList);
        LinearLayout colorList = customView.findViewById(R.id.colorList);
        LinearLayout popupLayout = customView.findViewById(R.id.popupLayout);
        Button button_cancel = customView.findViewById(R.id.cancel);
        Button button_create = customView.findViewById(R.id.create);
        EditText textedit_name = customView.findViewById(R.id.textField);
        layout_MainMenu = root_view.findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);

        int height = getResources().getDisplayMetrics().heightPixels - GeneralUtils.dpToPx(MainActivity.this, 100); // 50 pixels margin on top and bottom
        popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set focusable to true to prevent outside touches from dismissing the popup
        popupWindow.setFocusable(true);
        
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                layout_MainMenu.getForeground().setAlpha(0);
            }
        });
        
        oldIcon = new View(this);
        int i = 0;
        for (int iconId : GeneralUtils.iconIds) {
            ImageButton icon = new ImageButton(this);
            icon.setImageResource(iconId);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setPadding(GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 50), GeneralUtils.dpToPx(MainActivity.this, 50));
            layoutParams.setMargins(GeneralUtils.dpToPx(MainActivity.this, 20), GeneralUtils.dpToPx(MainActivity.this, 20), GeneralUtils.dpToPx(MainActivity.this, 20), GeneralUtils.dpToPx(MainActivity.this, 20));
            icon.setLayoutParams(layoutParams);
            icon.setBackgroundResource(R.drawable.menu_button_background);

            final int i_final = i;

            icon.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (oldIcon != view) {
                                selectedIcon = i_final;
                                oldIcon.setBackgroundResource(R.drawable.menu_button_background);
                                oldIcon = view;
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(() -> view.setBackgroundResource(R.drawable.button_background_selected), 100);
                            }
                        }
                    });

            iconList.addView(icon);
            i++;
        }
        oldColor = new View(this);
        i = 0;
        for (int colorId : GeneralUtils.colorIds) {
            ImageButton color = new ImageButton(this);

            // Create a ShapeDrawable to make a circle with the color
            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.getPaint().setColor(ContextCompat.getColor(this, colorId));
            InsetDrawable insetCircle = new InsetDrawable(circle, GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5));

            // Create a LayerDrawable that combines the menu button background and the circle
            Drawable[] layers = new Drawable[2];
            layers[0] =
                    ContextCompat.getDrawable(
                            this, R.drawable.menu_button_background); // background layer
            layers[1] = insetCircle; // circle layer
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            color.setBackground(layerDrawable);

            // Set the size and margins of the ImageButton
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(GeneralUtils.dpToPx(MainActivity.this, 40), GeneralUtils.dpToPx(MainActivity.this, 40));
            layoutParams.setMargins(GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15), GeneralUtils.dpToPx(MainActivity.this, 15));
            color.setPadding(GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5));
            color.setLayoutParams(layoutParams);

            final int i_final = i;

            selectedColor = i_final; // Placeholder
            // Set a click listener
            color.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (oldColor != view) {
                                ShapeDrawable circle1 = new ShapeDrawable(new OvalShape());
                                circle1.getPaint()
                                        .setColor(ContextCompat.getColor(MainActivity.this, GeneralUtils.colorIds[selectedColor]));
                                InsetDrawable insetCircle1 = new InsetDrawable(circle1, GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5));
                                Drawable[] layers1 = new Drawable[2];
                                layers1[0] =
                                        ContextCompat.getDrawable(
                                                MainActivity.this,
                                                R.drawable.menu_button_background); // background
                                // layer
                                layers1[1] = insetCircle1; // circle layer
                                LayerDrawable layerDrawable1 = new LayerDrawable(layers1);
                                oldColor.setBackground(layerDrawable1);

                                selectedColor = i_final;
                                // oldColor.setBackgroundResource(R.drawable.menu_button_background);
                                oldColor = view;

                                ShapeDrawable circle2 = new ShapeDrawable(new OvalShape());
                                circle2.getPaint()
                                        .setColor(ContextCompat.getColor(MainActivity.this, GeneralUtils.colorIds[selectedColor]));
                                InsetDrawable insetCircle2 = new InsetDrawable(circle2, GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5), GeneralUtils.dpToPx(MainActivity.this, 5));
                                Drawable[] layers2 = new Drawable[2];
                                layers2[0] =
                                        ContextCompat.getDrawable(
                                                MainActivity.this,
                                                R.drawable
                                                        .button_background_selected); // background
                                // layer
                                layers2[1] = insetCircle2; // circle layer
                                LayerDrawable layerDrawable2 = new LayerDrawable(layers2);

                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(() -> view.setBackground(layerDrawable2), 100);
                            }
                        }
                    });

            // Add the ImageButton to the LinearLayout
            colorList.addView(color);
            i++;
        }

        popupLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) MainActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
        
        button_cancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });
        
        button_create.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Folder folder = new Folder();
                        folder.setName(textedit_name.getText().toString());
                        folder.setIcon(selectedIcon);
                        folder.setColor(selectedColor);
                        folder.setPath(currentFolder);
                        foldersdbhelper.addFolder(folder);
                        loadItems();
                        //syncFolderChanges();
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
                        popupWindow.dismiss();
                    }
                });
        /*
        popupLayoutColors = customView.findViewById(R.id.colorList);
        Button button_cancel = customView.findViewById(R.id.cancel);
        Button button_add = customView.findViewById(R.id.add);
        TextInputLayout textInputLayout = customView.findViewById(R.id.textField);
        popupSubjectInput = textInputLayout.getEditText();
        LinearLayout popupLayout = (LinearLayout) customView.findViewById(R.id.popupLayout);
        LinearLayout subjectLayout = (LinearLayout) customView.findViewById(R.id.subjectList);
        
        popupLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });

        button_cancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

        button_add.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String subject = popupSubjectInput.getText().toString();
                        TextView item = new TextView(requireContext()); // Use the provided context
                        item.setText(subject); // Set text for the item
                        item.setGravity(Gravity.CENTER);
                        ((ViewGroup) lastSubjectLayout).removeAllViews();
                        ((ViewGroup) lastSubjectLayout).addView(item);
                        GradientDrawable borderDrawable = new GradientDrawable();
                        borderDrawable.setColor(
                                getResources().getColor(android.R.color.transparent));
                        borderDrawable.setStroke(5, lastColor);
                        borderDrawable.setCornerRadius(
                                getResources().getDimension(R.dimen.border_radius));
                        ((ViewGroup) lastSubjectLayout).setBackground(borderDrawable);
                        popupWindow.dismiss();

                        int id = ((ViewGroup) lastSubjectLayout).getId();
                        TimetableDatabaseHelper foldersdbhelper = new TimetableDatabaseHelper(getActivity());
                        foldersdbhelper.addSubject(subject, lastColor, id);
                    }
                });

        int[] colorIds = {
            R.color.homework_red,
            R.color.homework_blue,
            R.color.homework_green,
            R.color.homework_yellow,
            R.color.homework_orange,
            R.color.homework_purple,
            R.color.homework_cyan,
            R.color.homework_magenta,
            R.color.homework_lime,
            R.color.homework_teal,
            R.color.homework_pink,
            R.color.homework_lavender,
            R.color.homework_brown,
            R.color.homework_maroon,
            R.color.homework_olive,
            R.color.homework_navy,
            R.color.homework_turquoise,
            R.color.homework_indigo,
            R.color.homework_violet,
            R.color.homework_mauve,
            R.color.homework_crimson,
            R.color.homework_chartreuse,
            R.color.homework_plum,
            R.color.homework_goldenrod,
            R.color.homework_seagreen,
            R.color.homework_salmon,
            R.color.homework_aquamarine,
            R.color.homework_tomato,
            R.color.homework_slateblue,
            R.color.homework_gray,
            R.color.homework_silver,
            R.color.homework_black
        };

        // Loop through the color resource IDs and call loadColors function for each color
        for (int colorId : colorIds) {
            int color = getResources().getColor(colorId);
            loadColors(popupLayoutColors, color);
        }

        TimetableDatabaseHelper foldersdbhelper = new TimetableDatabaseHelper(getActivity());
        List<Triple<String, Integer, Integer>> matchingList = foldersdbhelper.getMatchingColorsSubjectsAndPositions();

        // HashSet to store unique subjects
        HashSet<String> uniqueSubjects = new HashSet<>();

        // Iterate through the list and toast each subject and its corresponding color
        for (Triple<String, Integer, Integer> triple : matchingList) {
            String subject = triple.getFirst();
            // Check if the subject is already displayed
            if (!uniqueSubjects.contains(subject)) {
                int color = triple.getSecond();
                loadSubjects(subjectLayout, subject, color);
                // Add the subject to the set to mark it as displayed
                uniqueSubjects.add(subject);
            }
        }
        */

        /*
        String[] country = { "Mathe", "Deutsch", "Englisch", "Französisch", "Erdkunde", "Sport", "Musik", "Geschichte", "PoWi", "Reli", "Chemie", "Bio", "Erdkunde WPU"};
        
        Spinner s = (Spinner) customView.findViewById(R.id.Spinner01);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, country);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        */

        // Show the popup at a specific location (you can adjust the coordinates)
    }
    
    private void showFolderPopup() {
        View parentView = findViewById(R.id.parentLayout);
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(MainActivity.this, 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
    }
    
    private void showNotebookPopup() {
        View parentView = findViewById(R.id.parentLayout);
        notebookPopupWindow.showAtLocation(parentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(MainActivity.this, 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
    }
    
    @Override
    public void onBackPressed() {
        if (currentFolder.length() > 1) {
            //Toast.makeText(MainActivity.this, currentFolder + ", length: " + String.valueOf(currentFolder.length()), Toast.LENGTH_SHORT).show();
            if (currentFolder.endsWith("/")) {
                currentFolder = currentFolder.substring(0, currentFolder.length() - 1);
            }

            // Finden des letzten Schrägstrichs nach dem Entfernen des letzten Teils
            int lastSlashIndex = currentFolder.lastIndexOf("/");

            // Entfernen des letzten Teils und Beibehalten des abschließenden Schrägstrichs
            if (lastSlashIndex != -1) {
                currentFolder = currentFolder.substring(0, lastSlashIndex + 1);
            }
            loadItems();
        } else {
            finishAffinity();
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        if (notesdbhelper != null) {
            notesdbhelper.close();
        }
    }


}
