package com.github.bytesculptor07.quillo;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.List;
import java.util.ArrayList;
import android.util.Pair;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import com.github.bytesculptor07.quillo.Utils.GeneralUtils;

public class drawActivity extends AppCompatActivity {
    List<drawFragment> fragments = new ArrayList<>();
    List<String> noteIds = new ArrayList<>();
    List<RelativeLayout> tabs = new ArrayList<>();
    String currentNote;
    LinearLayout tabView;
    RelativeLayout oldTab;
    Fragment visibleFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        
        tabView = findViewById(R.id.tabView);
        HorizontalScrollView tabScrollBar = findViewById(R.id.tabScrollBar);

        adjustStatusBar(tabScrollBar);
        
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        try {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("noteid")) {
                String noteId = intent.getStringExtra("noteid");
                Pair<drawFragment, Boolean> pair = addFragment(noteId);
                drawFragment fragment = pair.first;
                boolean found = pair.second;
                for (Fragment fg : fragments) {
                    hideFragment(fg);
                }
                showFragment(fragment);
                currentNote = noteId;
                if (!found) {
                    createTab(fragment, noteId);
                }
            }
        } catch(Exception e) {
            Toast.makeText(drawActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
        }
        
        View root_view = this.getWindow().getDecorView().getRootView();
        LinearLayout layout_MainMenu = root_view.findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressed();
            }
        });

    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (intent.hasExtra("noteid")) {
            String noteId = intent.getStringExtra("noteid");
            Pair<drawFragment, Boolean> pair = addFragment(noteId);
            drawFragment fragment = pair.first;
            boolean found = pair.second;
            for (Fragment fg : fragments) {
                hideFragment(fg);
            }
            showFragment(fragment);
            currentNote = noteId;
            if (!found) {
                createTab(fragment, noteId);
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for (drawFragment fg : fragments) {
            //fg.initUi();
        }
    }

    public void adjustStatusBar(HorizontalScrollView tabScrollBar) {
        ViewCompat.setOnApplyWindowInsetsListener(tabScrollBar, (v, windowInsets) -> {
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

        boolean isDarkMode = (drawActivity.this.getResources().getConfiguration().uiMode &
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
    
    public Pair<drawFragment, Boolean> addFragment(String noteid) {
        if (!noteIds.contains(noteid)) {
            drawFragment fragment = new drawFragment();
            fragments.add(fragment);
            noteIds.add(noteid);
            Bundle args = new Bundle();
            
            args.putString("noteid", noteid);
            fragment.setArguments(args);
            
            createFragment(fragment);
            return new Pair<>(fragment, false);
        } else {
            int index = noteIds.indexOf(noteid);
            return new Pair<>(fragments.get(index), true);
        }
    }
    
    private void createFragment(Fragment fragment){
                 getSupportFragmentManager().beginTransaction()
                .add(R.id.drawFragmentContainer, fragment)
                .hide(fragment)
                .commit();
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
    /*
    public void showFragment(drawFragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.drawFragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }
    */

    public Fragment getVisibleFragment() {
        return visibleFragment;
    }
    
    @SuppressLint("ResourceAsColor")
    public void createTab(drawFragment fragment, String noteid) {
        RelativeLayout relativeLayout = new RelativeLayout(drawActivity.this);
        RelativeLayout.LayoutParams relativeLayoutParams = new RelativeLayout.LayoutParams(
                GeneralUtils.dpToPx(this, 200),
                GeneralUtils.dpToPx(this, 40)
        );
        relativeLayoutParams.setMargins(0, 0, GeneralUtils.dpToPx(this, 25), GeneralUtils.dpToPx(this, 10));
        relativeLayout.setLayoutParams(relativeLayoutParams);
        relativeLayout.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background));
        
        TextView textView = new TextView(drawActivity.this);
        RelativeLayout.LayoutParams textViewParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        textViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setLayoutParams(textViewParams);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text));
        textView.setText(GeneralUtils.getNameOfNoteId(noteid));

        ImageButton imageButton = new ImageButton(drawActivity.this);
        RelativeLayout.LayoutParams imageButtonParams = new RelativeLayout.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GeneralUtils.dpToPx(this, 30), this.getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GeneralUtils.dpToPx(this, 30), this.getResources().getDisplayMetrics())
        );
        imageButtonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        imageButtonParams.addRule(RelativeLayout.CENTER_VERTICAL);
        imageButtonParams.setMargins(GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5)); // Margin in dp
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageResource(R.drawable.icon_close);
        imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageButton.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.button_background));
        ImageViewCompat.setImageTintList(imageButton, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text)));
        imageButton.setPadding(GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5), GeneralUtils.dpToPx(this, 5)); // Padding in dp

        //set selected tab
        if (oldTab != null) {
            oldTab.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background));
        }
        oldTab = relativeLayout;
        relativeLayout.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background_selected));

        relativeLayout.addView(textView);
        relativeLayout.addView(imageButton);
        
        tabView.addView(relativeLayout);
        
        relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //change color here, new and old
                if (oldTab != null) {
                    oldTab.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background));
                }
                oldTab = (RelativeLayout) v;

                ((RelativeLayout) v).setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background_selected));
                
                for (Fragment fg : fragments) {
                    hideFragment(fg);
                }
                showFragment(fragment);
                currentNote = noteid;
            }
        });
        
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.savePages(true);
                fragments.remove(fragment);
                noteIds.remove(noteid);
                tabView.removeView(relativeLayout);
                if (noteIds.size() == 0) {
                    finish();
                } else {
                        /*
                    for (Fragment fg : fragments) {
                        hideFragment(fg);
                    }
                    showFragment(fragments.get(fragments.size()-1));
                        */
                        hideFragment(fragment);
                        if (!noteIds.contains(currentNote)) {
                            showFragment(fragments.get(fragments.size()-1));

                            if (tabView.getChildCount() > 0) {
                                oldTab = (RelativeLayout) tabView.getChildAt(tabView.getChildCount()-1);
                                oldTab.setBackground(ContextCompat.getDrawable(drawActivity.this, R.drawable.tab_background_selected));
                            }
                        }
                        currentNote = noteIds.get(noteIds.size()-1);
                }
            }
        });
    }

    public void onBackPressed() {
        Intent intent = new Intent("DRAW_ACTIVITY_CLOSED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        moveTaskToBack(true);
        Intent intent2 = new Intent(this, MainActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent2);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent("DRAW_ACTIVITY_CLOSED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
