package com.github.bytesculptor07.quillo;

import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Display;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Button;
import android.graphics.pdf.PdfRenderer;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.IOException;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.ContentResolver;

import com.github.bytesculptor07.quillo.Utils.DrawUtils;
import com.github.bytesculptor07.quillo.Utils.GeneralUtils;
import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Page;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.LabelFormatter;
import java.util.Locale;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult;
import androidx.activity.result.IntentSenderRequest;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import android.app.Activity;
import androidx.core.content.FileProvider;
import java.io.File;

import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//import com.github.bytesculptor07.quillo.drawFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.bumptech.glide.Glide;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.common.MlKitException;

import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import android.animation.ValueAnimator;

//import com.websitebeaver.documentscanner;

public class drawFragment extends Fragment implements DrawingView.DrawingListener {
    List<DrawingView> undoDrawingViews = new ArrayList<>();
    List<DrawingView> redoDrawingViews = new ArrayList<>();
    LinearLayout layout_MainMenu;
    PopupWindow popupWindow;
    PopupWindow popupWindowLine;
    Slider lineSlider;
    float zoomFactor;
    LinearLayout parentContainer;
    CustomScrollView zoomlayout;
    ImageButton undoButton;
    ImageButton redoButton;
    ImageView pdfView;
    Uri exportUri;
    ActivityResultLauncher<IntentSenderRequest> scannerLauncher;
    List<DrawingView> pages = new ArrayList<>();
    List<ImageView> pageBackgroundImageViews = new ArrayList<>();
    ImageView lastBackgroundImageView;
    NotesDatabaseHelper dbhelper;
    ImageButton oldColorButton;
    ImageButton oldLineButton;
    ImageButton oldToolButton;
    int oldColor = 0xFF000000;
    int currentLine;
    //int[] buttonColors = {0xFF000000, 0xFF00FF00, 0xFF0000FF};
    ArrayList<Integer> buttonColors = new ArrayList<>(Arrays.asList(0xFF000000, 0xFF00AF5D, 0xFF0051B1));
    ArrayList<Integer> buttonLines = new ArrayList<>(Arrays.asList(10, 50, 100));
    String noteid = ""; //change for every note that has to be loaded
    boolean autosave = true; //disable if needed
    
    View fragmentView;
    Timer autosaveTimer;
    
    private ExecutorService executor;
    private Handler mainHandler;
    
    int colorpickerColor;
    int selectedTemplate;
    
    boolean isSidebarVisible = false;
    LinearLayout sidebar;

    pagesortFragment pagesortFragment;
    assistantFragment assistantFragment;
    Fragment currentSidebarFragment;
    
    Button colorpickerButtonLeft;
    Button colorpickerButtonRight;
    Button linePopupButtonLeft;
    Button linePopupButtonRight;

    int currentColor;
    int currentTool;
    int currentStrokeWidth;

    TextView titleView;

    DocumentScannerHelper scanner;
    
    public final int ACTION_ADD_COLOR = 6;
    public final int ACTION_CHANGE_COLOR = 7;
    
    public final int BACKGROUND_LINES = 8;
    public final int BACKGROUND_SQUARE = 9;
    public final int BACKGROUND_EMPTY = 10;
    
    public final int ACTION_ADD_LINE = 11;
    public final int ACTION_CHANGE_LINE = 12;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment's layout
        
        if (getArguments() != null) {
            noteid = getArguments().getString("noteid");
        }

        /*scanner = new DocumentScannerHelper(requireActivity(), new DocumentScannerHelper.SuccessCallback() {
            @Override
            public void onScanSuccess(ArrayList<String> scannedImages) {
                for (String image : scannedImages) {
                    DrawingView dv = addImageBitmap(Uri.parse(image), null, -1, null);
                    dv.changes = true;
                }

            }
        });*/

        scanner = DocumentScannerHelper.getInstance(requireActivity());
        scanner.registerFragment(
                getTag(),
                scannedImages -> {
                    scanSuccess(scannedImages);
                }
        );
        
        return inflater.inflate(R.layout.fragment_draw, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        fragmentView = view;
        dbhelper = NotesDatabaseHelper.getInstance(getContext());

        parentContainer = fragmentView.findViewById(R.id.parent_container);
        zoomlayout = fragmentView.findViewById(R.id.zoomlayout);
        FrameLayout addPage = fragmentView.findViewById(R.id.add_page);
        undoButton = fragmentView.findViewById(R.id.undo);
        redoButton = fragmentView.findViewById(R.id.redo);
        pdfView = fragmentView.findViewById(R.id.pdfView);
        ImageButton saveButton = fragmentView.findViewById(R.id.save);
        ImageButton backButton = fragmentView.findViewById(R.id.back);
        
        ImageButton penButton = fragmentView.findViewById(R.id.pen);
        ImageButton highlighterButton = fragmentView.findViewById(R.id.highlighter);
        ImageButton eraserButton = fragmentView.findViewById(R.id.eraser);
        ImageButton lassoButton = fragmentView.findViewById(R.id.lasso);
        ImageButton laserButton = fragmentView.findViewById(R.id.laser);
        ImageButton pathButton = fragmentView.findViewById(R.id.path);
        
        ImageButton addColorButton = fragmentView.findViewById(R.id.add_color);
        ImageButton addLineButton = fragmentView.findViewById(R.id.add_line);

        TextView titleView = fragmentView.findViewById(R.id.title);
        titleView.setText(GeneralUtils.getNameOfNoteId(noteid));
        
        penButton.setTag(DrawUtils.TOOL_PEN);
        highlighterButton.setTag(DrawUtils.TOOL_HIGHLIGHTER);
        eraserButton.setTag(DrawUtils.TOOL_ERASER);
        lassoButton.setTag(DrawUtils.TOOL_LASSO);
        laserButton.setTag(DrawUtils.TOOL_LASER);
        pathButton.setTag(DrawUtils.TOOL_PATH);
        
        ImageButton imageButton = fragmentView.findViewById(R.id.image);
        ImageButton pdfButton = fragmentView.findViewById(R.id.pdf);
        ImageButton exportButton = fragmentView.findViewById(R.id.export);
        ImageButton optionsButton = fragmentView.findViewById(R.id.options);
        ImageButton chatButton = fragmentView.findViewById(R.id.chat);

        chatButton.setVisibility(View.GONE);  // remove chat button as long as there is no chat
        
        sidebar = fragmentView.findViewById(R.id.sidebar);
        
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        scannerLauncher = registerForActivityResult(new StartIntentSenderForResult(), this::handleActivityResult);
        
        //downloadModels();
        
        oldToolButton = penButton;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(0x22000000);
        penButton.setBackground(drawable);
        View.OnClickListener toolClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (oldToolButton != (ImageButton) view) {
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.OVAL);
                    drawable.setColor(0x22000000);
                    view.setBackground(drawable);
                    oldToolButton.setBackgroundResource(R.drawable.button_background);
                    oldToolButton = (ImageButton) view;
                    int tool = (int) view.getTag();
                    changeTool(tool);
                }
            }
        };
        
        if (!addAllPages()) {
            DrawingView dv = addContainer();
            dv.changes = true;
        }
        
        loadColorpickerPopup();
        loadLinePopup();
        loadSidebar();
        
        penButton.setOnClickListener(toolClickListener);
        highlighterButton.setOnClickListener(toolClickListener);
        eraserButton.setOnClickListener(toolClickListener);
        lassoButton.setOnClickListener(toolClickListener);
        laserButton.setOnClickListener(toolClickListener);
        pathButton.setOnClickListener(toolClickListener);

        zoomlayout.setOnScrollListener(new CustomScrollView.OnScrollListener() {
            @Override
            public void onScroll(float distanceX, float distanceY) {
                // Handle scroll events
                updateBackgroundImages();
            }
        });
        
        zoomlayout.setOnZoomListener(new CustomScrollView.OnZoomListener() {
            @Override
            public void onZoom(float scaleFactor) {
                // Handle zoom events
                updateBackgroundImages();
            }
        });
        
        addPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContainer();
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requireActivity().onBackPressed();
            }
        });
        
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanImage();
            }
        });
        
        pdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });
        
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //exportInBackground();
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("application/pdf");
                //intent.addCategory(Intent.CATEGORY_OPENABLE);
                exportActivityResultLaunch.launch(intent);
            }
        });

        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentSidebarFragment != pagesortFragment && !isSidebarVisible) {
                    hideFragment(assistantFragment);
                    showFragment(pagesortFragment);
                    toggleSidebar();
                } else if (currentSidebarFragment != pagesortFragment && isSidebarVisible) {
                    hideFragment(assistantFragment);
                    showFragment(pagesortFragment);
                } else if (currentSidebarFragment == pagesortFragment) {
                    toggleSidebar();
                }

                currentSidebarFragment = pagesortFragment;
            }
        });

        chatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentSidebarFragment != assistantFragment && !isSidebarVisible) {
                    hideFragment(pagesortFragment);
                    showFragment(assistantFragment);
                    toggleSidebar();
                } else if (currentSidebarFragment != assistantFragment && isSidebarVisible) {
                    hideFragment(pagesortFragment);
                    showFragment(assistantFragment);
                } else if (currentSidebarFragment == assistantFragment) {
                    toggleSidebar();
                }

                currentSidebarFragment = assistantFragment;
            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undo();
            }
        });
        
        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redo();
            }
        });
        
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePages(false);
            }
        });
        
        addColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorpickerPopup(ACTION_ADD_COLOR, addColorButton);
            }
        });
        
        addLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLinePopup(ACTION_ADD_LINE, addLineButton);
            }
        });

        // Add color buttons
        addColorButtons(true);
        addLineButtons();
        
        // Save every second if autosave is enabled
        autosaveTimer = new Timer();
        if (autosave) {
            autosaveTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    requireActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            savePages(false);
                        }
                    });
                }
            }, 1000, 1000);
        }
    }
    
    public void loadSidebar() {
        //Fragment fragment = new assistantFragment();
        //String uniqueTag = "pagesortFragment_" + noteid;
        pagesortFragment = new pagesortFragment();
        pagesortFragment.loadNote(noteid);
        pagesortFragment.setParent(this);

        assistantFragment = new assistantFragment();

        createFragment(pagesortFragment);
        createFragment(assistantFragment);
    }

    private void createFragment(Fragment fragment){
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.sidebar, fragment)
                .hide(fragment)
                .commit();
    }
    private void showFragment(Fragment fragment){
        requireActivity().getSupportFragmentManager().beginTransaction()
                .show(fragment)
                .commit();
    }
    private void hideFragment(Fragment fragment){
        requireActivity().getSupportFragmentManager().beginTransaction()
                .hide(fragment)
                .commit();
    }
    
    private void toggleSidebar() {
        int startWidth = isSidebarVisible ? GeneralUtils.dpToPx(getContext(), 300) : 0; // Sidebar width
        int endWidth = isSidebarVisible ? 0 : GeneralUtils.dpToPx(getContext(), 300);
    
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);
        animator.setDuration(300); // Animation duration in milliseconds
    
        animator.addUpdateListener(animation -> {
            int width = (int) animation.getAnimatedValue();
            LinearLayout.LayoutParams sidebarParams = (LinearLayout.LayoutParams) sidebar.getLayoutParams();
            sidebarParams.width = width;
            sidebar.setLayoutParams(sidebarParams);
        });
    
        animator.start();
        isSidebarVisible = !isSidebarVisible;
    }

    public void scanSuccess(ArrayList<String> scannedImages) {
        for (String image : scannedImages) {
            DrawingView dv = addImageBitmap(Uri.parse(image), null, -1, null);
            dv.changes = true;
            dv.attachmentChanges = true;
        }
    }

    public void exportPage(Page page) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("application/pdf");
        exportPageActivityResultLaunch.launch(intent);
    }
    
    private void downloadModels() {
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-shapes"); // zxx-Zsym-x-autodraw
            DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();
            if (true) {
                RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();

                remoteModelManager
                        .download(model, new DownloadConditions.Builder().build())
                        .addOnSuccessListener(
                                aVoid ->
                                        Toast.makeText(
                                                        getContext(),
                                                        "Model downloaded",
                                                        Toast.LENGTH_SHORT)
                                                .show())
                        .addOnFailureListener(
                                e ->
                                        Toast.makeText(
                                                        getContext(),
                                                        "Error while downloading a model: " + e,
                                                        Toast.LENGTH_SHORT)
                                                .show());
            }
        } catch (MlKitException e) {
            Toast.makeText(getContext(), "failed to parse model", Toast.LENGTH_SHORT).show();
        }
    }

    private void addColorButtons(boolean firstrun) {
        LinearLayout colorsLayout = fragmentView.findViewById(R.id.colors);
        colorsLayout.removeAllViews();
        /*
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (sharedPreferences.contains("buttonColors")) {
            loadColors();
        } else {
            saveColors();
        }
        */
        loadColors();
        //oldColorButton = new ImageButton(getContext()); //placeholder
        if (firstrun) {
            oldColor = buttonColors.get(0);
            changeColor(oldColor);
        }

        // Add the buttons to the layout
        for (int color : buttonColors) {
            boolean current = false;
            if (color == oldColor) {
                current = true;
            }
            addColorButton(color, current);
        }
    }

    public void addColorButton(int color, boolean clicked) {
        LinearLayout colorsLayout = fragmentView.findViewById(R.id.colors);
        ImageButton colorButton = new ImageButton(getContext());

        // Create a drawable for the button background with a white border
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        //drawable.setStroke(2, 0xFFFFFFFF);

        // Set the drawable as the background
        colorButton.setImageDrawable(drawable);
        if (!clicked) {
            colorButton.setBackgroundResource(R.drawable.button_background);
        } else {
            GradientDrawable drawable2 = new GradientDrawable();
            drawable2.setShape(GradientDrawable.OVAL);
            drawable2.setColor(0x22000000);
            
            if (oldColorButton != null) {
                oldColorButton.setBackgroundResource(R.drawable.button_background);
            }
            oldColorButton = colorButton;
            colorButton.setBackground(drawable2);
        }

        // Set layout parameters for the button
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        GeneralUtils.dpToPx(getContext(), 30), // Width in dp
                        GeneralUtils.dpToPx(getContext(), 30) // Height in dp
                        );
        params.setMargins(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5)); // Set margins as needed
        colorButton.setLayoutParams(params);

        // Set other properties
        colorButton.setPadding(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
        colorButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        colorButton.setContentDescription(null);

        // Optionally, set an OnClickListener to handle button clicks
        colorButton.setOnLongClickListener(
                v -> {
                    showColorpickerPopup(ACTION_CHANGE_COLOR, color);
                    return false;
                });

        colorButton.setOnClickListener(
                v -> {
                    // Handle the button click
                    changeColor(color);
                    GradientDrawable drawable2 = new GradientDrawable();
                    drawable2.setShape(GradientDrawable.OVAL);
                    drawable2.setColor(0x22000000);
                    // set original color to old button
                    oldColorButton.setBackgroundResource(R.drawable.button_background);
                    oldColorButton = colorButton;
                    oldColor = color;
                    Handler handler2 = new Handler(Looper.getMainLooper());
                    handler2.postDelayed(() -> colorButton.setBackground(drawable2), 100);
                });

        // Add the button to the layout
        colorsLayout.addView(colorButton);
    }
    
    private void addLineButtons() {
        LinearLayout lineLayout = fragmentView.findViewById(R.id.lines);
        lineLayout.removeAllViews();
        
        loadLines();
        //oldColorButton = new ImageButton(getContext()); //placeholder
        
        if (currentLine == 0 && buttonLines.size() > 0) {
            currentLine = buttonLines.get(0);
            changeStrokeWidth(currentLine);
        }

        // Add the buttons to the layout
        for (int line : buttonLines) {
            boolean current = false;
            if (line == currentLine) {
                current = true;
            }
            addLineButton(line, current);
        }
    }
    
    public void addLineButton(int line, boolean clicked) {
        LinearLayout linesLayout = fragmentView.findViewById(R.id.lines);
        ImageButton lineButton = new ImageButton(getContext());
        
        Bitmap btm = Bitmap.createBitmap(GeneralUtils.dpToPx(getContext(), 30), GeneralUtils.dpToPx(getContext(), 30), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(btm);
                
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(line / 10 + 3);
        paint.setColor(0xFF000000);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        canvas.drawLine(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 25), GeneralUtils.dpToPx(getContext(), 15), paint);
        /*
        // Create a drawable for the button background with a white border
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setStroke(2, 0xFFFFFFFF);
        */
        // Set the drawable as the background
        //lineButton.setImageDrawable(drawable);
        lineButton.setImageBitmap(btm);
        if (!clicked) {
            lineButton.setBackgroundResource(R.drawable.button_background);
        } else {
            GradientDrawable drawable2 = new GradientDrawable();
            drawable2.setShape(GradientDrawable.OVAL);
            drawable2.setColor(0x22000000);
            // set original color to old button
            GradientDrawable drawable3 = new GradientDrawable();
            drawable3.setShape(GradientDrawable.OVAL);
            drawable3.setColor(0xFF000000);
            
            if (oldLineButton != null) {
                oldLineButton.setBackgroundResource(R.drawable.button_background);
                //oldLineButton.setImageDrawable(drawable3);
            }
            
            oldLineButton = lineButton;
            lineButton.setBackground(drawable2);
        }

        // Set layout parameters for the button
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        GeneralUtils.dpToPx(getContext(), 30), // Width in dp
                        GeneralUtils.dpToPx(getContext(), 30) // Height in dp
                        );
        params.setMargins(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5)); // Set margins as needed
        lineButton.setLayoutParams(params);

        // Set other properties
        lineButton.setPadding(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
        lineButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        lineButton.setContentDescription(null);

        // Optionally, set an OnClickListener to handle button clicks
        lineButton.setOnLongClickListener(
                v -> {
                    showLinePopup(ACTION_CHANGE_LINE, line);
                    return false;
                });
        
        lineButton.setOnClickListener(
                v -> {
                    // Handle the button click
                    changeStrokeWidth(line);
                    GradientDrawable drawable2 = new GradientDrawable();
                    drawable2.setShape(GradientDrawable.OVAL);
                    drawable2.setColor(0x22000000);
                    // set original color to old button
                    if (oldLineButton != null) {
                        oldLineButton.setBackgroundResource(R.drawable.button_background);
                    }
                    oldLineButton = lineButton;
                    currentLine = line;
                    Handler handler2 = new Handler(Looper.getMainLooper());
                    handler2.postDelayed(() -> lineButton.setBackground(drawable2), 100);
                });
        

        // Add the button to the layout
        linesLayout.addView(lineButton);
    }

    public void saveColors() {
        StringBuilder sb = new StringBuilder();
        for (int color : buttonColors) {
            sb.append(color).append(",");
        }
        String colorString = sb.toString();

        // Den String in SharedPreferences speichern
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("buttonColors", colorString);
        editor.apply();
    }
    
    public void loadColors() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String colorString = sharedPreferences.getString("buttonColors", "");
        
        if (!colorString.isEmpty()) {
            buttonColors.clear();
            String[] colorStrings = colorString.split(",");
            for (String color : colorStrings) {
                buttonColors.add(Integer.parseInt(color));
            }
        }
    }
    
    public void saveLines() {
        StringBuilder sb = new StringBuilder();
        for (int line : buttonLines) {
            sb.append(line).append(",");
        }
        String lineString = sb.toString();

        // Den String in SharedPreferences speichern
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("buttonLines", lineString);
        editor.apply();
    }
    
    public void loadLines() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String lineString = sharedPreferences.getString("buttonLines", "");
        
        if (!lineString.isEmpty()) {
            buttonLines.clear();
            String[] lineStrings = lineString.split(",");
            for (String line : lineStrings) {
                buttonLines.add(Integer.parseInt(line));
            }
        }
    }

    public void exportInBackground() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            export();

                            new Handler(Looper.getMainLooper())
                                    .post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    try (InputStream is = new FileInputStream(new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "export.pdf")); OutputStream os = requireActivity().getContentResolver().openOutputStream(exportUri)) {
                                                        byte[] buffer = new byte[1024];
                                                        int length;
                                                        while ((length = is.read(buffer)) > 0) {
                                                            os.write(buffer, 0, length);
                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    Toast.makeText(getContext(), "document saved", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                        } catch (Exception e) {
                            e.printStackTrace(); // Handle or log exceptions as needed
                            // Post Toast message if an exception occurs
                            new Handler(Looper.getMainLooper())
                                    .post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "An error occurred: "
                                                                            + e.getMessage(),
                                                                    Toast.LENGTH_LONG)
                                                            .show();
                                                }
                                            });
                        } finally {
                            // Shutdown the executor service gracefully
                            executorService.shutdown();
                        }
                    }
                });
    }

    public void export() {
        try {
            PDDocument newDocument = new PDDocument();
            
            List<PDDocument> sourceDocuments = new ArrayList<>();

            for (DrawingView page : pages) {
                PDDocument sourceDocument = null;
                PDPage sourcePage = null;
                if (page.Pdf != null && page.PdfPage != -1) {
                    File pdfFile =
                            new File(
                                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), page.Pdf);
                    sourceDocument = PDDocument.load(pdfFile);
                    sourcePage = sourceDocument.getPage(page.PdfPage);
                } else {
                    sourcePage = new PDPage(PDRectangle.A4);
                }

                newDocument.addPage(sourcePage);

                PDRectangle mediaBox = sourcePage.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                List<SerializablePath> rawData = page.getRawData();
                Bitmap combinedBitmap = DrawUtils.renderDrawing(rawData, page.currentBackground, pageWidth, pageHeight, getContext());
                /*
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                
                List<SerializablePath> rawData = page.getRawData();
                for (SerializablePath line : rawData) {
                    paint.setColor(line.getColor());
                    canvas.drawPath(line, paint);
                }
                */
                
                PDImageXObject drawingImage =
                        LosslessFactory.createFromImage(newDocument, combinedBitmap);
                
                PDPageContentStream contentStream =
                        new PDPageContentStream(
                                newDocument,
                                sourcePage,
                                PDPageContentStream.AppendMode.APPEND,
                                true);
                contentStream.drawImage(drawingImage, -3, 3, pageWidth, pageHeight);
                contentStream.close();
                if (page.Pdf != null && page.PdfPage != -1) {
                    sourceDocuments.add(sourceDocument);
                }
            }
            
            newDocument.save(new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "export.pdf"));
            newDocument.close();

            for (PDDocument doc : sourceDocuments) {
                doc.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ActivityResultLauncher<Intent> exportPageActivityResultLaunch =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK
                                    && result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    exportUri = uri;
                                    exportInBackground();
                                }
                            }
                        }
                    }
            );

    ActivityResultLauncher<Intent> exportActivityResultLaunch =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK
                                    && result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    exportUri = uri;
                                    exportInBackground();
                                }
                            }
                        }
                    }
            );
    
    ActivityResultLauncher<Intent> activityResultLaunch =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK
                                    && result.getData() != null) {
                                Uri uri = result.getData().getData();
                                if (uri != null) {
                                    // Get the PDF file from the Uri
                                    ContentResolver contentResolver = requireActivity().getContentResolver();
                                    try (InputStream inputStream =
                                            contentResolver.openInputStream(uri)) {
                                        // Create a temporary file
                                        String filename = UUID.randomUUID().toString() + ".pdf";
                                        File tempFile =
                                                new File(
                                                        requireActivity().getExternalFilesDir(
                                                                Environment.DIRECTORY_DOCUMENTS),
                                                        filename);
                                        try (OutputStream outputStream =
                                                new FileOutputStream(tempFile)) {
                                            byte[] buffer = new byte[1024];
                                            int bytesRead;
                                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                                outputStream.write(buffer, 0, bytesRead);
                                            }
                                            outputStream.flush();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        //openPDF(tempFile);
                                        try {
                                            openPDFInBackground(tempFile, filename);
                                        } catch (Exception e) {
                                            Toast.makeText(getContext(), "error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                        /*
                                        int iii = 0;
                                        for (DrawingView pg : pages) {
                                            iii ++;
                                            if (isPageVisible(pg)) {
                                                Toast.makeText(drawFragment.this, "Page " + String.valueOf(iii) + "/" + String.valueOf(pages.size()) + " is visible", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(drawFragment.this, "Page " + String.valueOf(iii) + "/" + String.valueOf(pages.size()) + " is not visible", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                        */

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    });

    public void openPDFInBackground(final File tempFile, final String filename) {
    // Create a single-threaded executor
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    // Execute the background task
    executorService.execute(new Runnable() {
        @Override
        public void run() {
            try {
                // Open PDF and get bitmaps
                List<Bitmap> pdf = openPDF(tempFile);
                final List<Uri> vs = new ArrayList<>();
                
                // Process bitmaps
                for (Bitmap bt : pdf) {
                    Uri pdfu = saveTmpImg(bt);
                    vs.add(pdfu);
                }
                
                // Post results back to the main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // Update the UI
                        //Toast.makeText(drawFragment.this, "Page count: " + vs.size(), Toast.LENGTH_LONG).show();
                        
                        // Optionally add views to the parent container
                        int iii = 0;
                        for (Uri v : vs) {
                            DrawingView dv = addImageBitmap(v, filename, iii, null);
                            dv.changes = true;
                            dv.attachmentChanges = true;
                            iii ++;
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace(); // Handle or log exceptions as needed
                // Post Toast message if an exception occurs
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "An error occurred: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } finally {
                // Shutdown the executor service gracefully
                executorService.shutdown();
            }
        }
    });
}

    public void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activityResultLaunch.launch(intent);
    }
    
    public boolean isPageVisible(View view) {
        Rect scrollBounds = new Rect();
        zoomlayout.getHitRect(scrollBounds);
        if (view.getLocalVisibleRect(scrollBounds)) {
            return true;
        } else {
            return false;
        }
    }
    
    public void scanImage() {
        scanner.start();
        /*
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            //.setPageLimit(2)   dont set a page limit
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build();
        
            GmsDocumentScanning.getClient(options)
                .getStartScanIntent(requireActivity())
                .addOnSuccessListener(intentSender ->
                scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(
            e -> Toast.makeText(getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show());

         */
    }

    private void handleActivityResult(ActivityResult activityResult) {
        int resultCode = activityResult.getResultCode();
        GmsDocumentScanningResult result =
                GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
        if (resultCode == Activity.RESULT_OK && result != null) {
            for (GmsDocumentScanningResult.Page page : result.getPages()) {
                //Toast.makeText(drawFragment.this, page.getImageUri().toString(), Toast.LENGTH_SHORT).show();
            }
            
            if (result.getPdf() != null) {
                File file = new File(result.getPdf().getUri().getPath());
                    
                    Uri externalUri =
                            FileProvider.getUriForFile(getContext(), requireActivity().getPackageName() + ".provider", file);
                    openPDFInBackground(file, null);
                    /*
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.setDataAndType(externalUri, "application/pdf");
                    viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(viewIntent, "view pdf"));
                    */
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getContext(), "scanner cancelled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "error", Toast.LENGTH_SHORT).show();
        }
    }

    public List<Bitmap> openPDF(File file) {
        ParcelFileDescriptor fileDescriptor = null;
        PdfRenderer pdfRenderer = null;
        List<Bitmap> result = new ArrayList<>();
        
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            final int pageCount = pdfRenderer.getPageCount();
            //Toast.makeText(this, "pageCount = " + pageCount, Toast.LENGTH_LONG).show();

            // Display page 0
            for (int i = 0;  i < pageCount; i++) {
                PdfRenderer.Page rendererPage = pdfRenderer.openPage(i);
                int rendererPageWidth = rendererPage.getWidth();
                int rendererPageHeight = rendererPage.getHeight();
                final float scale = getResources().getDisplayMetrics().density;
                int newWidth  = (int) (2100 * scale);
                int newHeight = (int) ((double) newWidth / rendererPageWidth * rendererPageHeight);
                Bitmap bitmap = Bitmap.createBitmap(
                            newWidth, newHeight, Bitmap.Config.ARGB_8888);
                rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                
                result.add(bitmap);
                //addImageBitmap(bitmap);
                //pdfView.setImageBitmap(bitmap);
                rendererPage.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
        } finally {
            try {
                if (pdfRenderer != null) {
                    pdfRenderer.close();
                }
                if (fileDescriptor != null) {
                    fileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                /*
                Toast.makeText(
                                this,
                                "Error closing resources: " + e.getMessage(),
                                Toast.LENGTH_LONG)
                        .show();
                */
            }
            return result;
        }
    }

    public static String generateRandomFileName(int length, String extension) {
        String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random RANDOM = new Random();
        StringBuilder fileName = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            fileName.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        
        return fileName.toString() + extension;
    }
    
    public Uri saveTmpImg(Bitmap bitmap) {
        String filename = UUID.randomUUID().toString() + ".png";
        File tempFile =
                    new File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);
        Uri uri = Uri.fromFile(tempFile);
        // Ensure the bitmap is not null
        if (bitmap != null) {
            // Create a temporary file in the external files directory

            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                // Compress and save the bitmap to the temporary file
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }
    
    public DrawingView addImageBitmap(Uri uri, String pdf, int pdfPage, DrawingView dv) {
        //ImageView pdfview = new ImageView(getContext());
        ImageView pdfview = new ImageView(getContext());
        //ImageView pdfview = new ImageView(getContext());
        pdfview.setTag(uri);

        float originalWidth = 0;
        float originalHeigth = 0;

        try {
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // Reduce the size to 1/4 of the original

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            //pdfview.setImage(ImageSource.bitmap(bitmap));
            pdfview.setImageBitmap(bitmap);

            if (bitmap != null) {
                originalHeigth = bitmap.getHeight();
                originalWidth = bitmap.getWidth();
            }

            
            // Close the input stream after use
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        final float scale = getResources().getDisplayMetrics().density;
        int width  = (int) (2100 * scale);
        //Toast.makeText(getContext(), "ratio: " + String.valueOf(aspectRatio), Toast.LENGTH_SHORT).show();
        //int height = (int) (2970 * scale);
        int height = (int) ((width / originalWidth) * originalHeigth);
        int margin = (int) (10 * scale);
        
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        //layoutParams.setMargins(margin, margin, margin, margin);
        pdfview.setScaleType(ImageView.ScaleType.FIT_XY);
        pdfview.setBackgroundColor(Color.parseColor("#ffffff"));
        //pdfview.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE);
        // pdfview.setLayoutParams(layoutParams);
        // pdfview.setAdjustViewBounds(true);

        // SubsamplingScaleImageView imageView =
        // (SubsamplingScaleImageView)view.findViewById(id.imageView);
        // Bitmap thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(uri), width,
        // height);
        // Picasso.get().load(uri).into(pdfview);
        //Glide.with(getContext()).load(uri).into(pdfview);


        //:::::pdfview.setImage(ImageSource.uri(uri).tilingEnabled());
        //return pdfview;
        //pdfview.setImageBitmap(bitmap);
        //parentContainer.addView(pdfview);
        //Glide.with(getContext()).load(uri).into(pdfView);
        FrameLayout drawingContainer;
        DrawingView drawingView;
        if (dv == null) {
            View[] result = createContainer(true, width, height);
            drawingContainer = (FrameLayout) result[0];
            drawingView = (DrawingView) result[1];
        } else {
            drawingContainer = (FrameLayout) dv.getParent();
            drawingView = dv;

            ViewGroup.MarginLayoutParams containerParams = new LinearLayout.LayoutParams(width, height);
            containerParams.setMargins(margin, margin, margin, margin);
            drawingContainer.setLayoutParams(containerParams);
        }
        
        drawingContainer.addView(pdfview, 0, layoutParams);
        drawingContainer.setElevation(GeneralUtils.dpToPx(getContext(), 1));

        if (dv == null) {
            parentContainer.addView(drawingContainer);
        }
        
        drawingView.Background = uri.toString();
        if (pdf != null) {
            drawingView.Pdf = pdf;
        }
        if (pdfPage != -1) {
            drawingView.PdfPage = pdfPage;
        }
        if (dv == null) {
            pages.add(drawingView);
        }
        pageBackgroundImageViews.add(pdfview);
        zoomToContent();
        
        return drawingView;
    }
    
    public ImageView getMostVisibleImageView() {
        /*
        SubsamplingScaleImageView mostVisibleImageView = null;
        int maxVisibleHeight = 0;
    
        // Loop through all ImageViews
        for (SubsamplingScaleImageView imageView : pageBackgroundImageViews) {
            // Get the position of the ImageView in the ScrollView
            Rect rect = new Rect();
            imageView.getGlobalVisibleRect(rect);  // Get the visible rectangle of the ImageView
    
            // Calculate the visible height of the ImageView
            int visibleHeight = rect.height();
    
            // Check if this image has the largest visible height
            if (visibleHeight > maxVisibleHeight) {
                maxVisibleHeight = visibleHeight;
                mostVisibleImageView = imageView;
            }
        }
    
        return mostVisibleImageView;
        */
        Rect scrollBounds = new Rect();
        zoomlayout.getHitRect(scrollBounds);
        int i = 0;
        ImageView res = null;
        for (ImageView imageView : pageBackgroundImageViews) {
            if (imageView.getLocalVisibleRect(scrollBounds)) {
                i++;
                res = imageView;
            }
        }
        //Toast.makeText(drawFragment.this, "visible: " + String.valueOf(i), Toast.LENGTH_SHORT).show();
        return res;
    }
    
    public void updateBackgroundImages() {
        ImageView img = getMostVisibleImageView();
        /* working example
        if (img != null) {
            if (zoomlayout.getRealZoom() > 0.5) {
                img.setBackgroundColor(R.color.folder_green);
            }
            if (lastBackgroundImageView != null && img != lastBackgroundImageView) {
                lastBackgroundImageView.setBackgroundColor(android.R.color.transparent);
            }
            lastBackgroundImageView = img;
            //Toast.makeText(drawFragment.this, String.valueOf(zoomlayout.getRealZoom()), Toast.LENGTH_SHORT).show();
        }
        */
        if (img != null) {
            //img.setImageURI((Uri) img.getTag());
            //Picasso.get().load((Uri) img.getTag()).into(img);
            //Toast.makeText(drawFragment.this, "load big image", Toast.LENGTH_SHORT).show();
            if (lastBackgroundImageView != null && img != lastBackgroundImageView) {
                Glide.with(getContext()).load((Uri) img.getTag()).into(img);
                try {
                    InputStream inputStream = requireActivity().getContentResolver().openInputStream((Uri) lastBackgroundImageView.getTag());
        
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // Reduce the size to 1/4 of the original
        
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    //lastBackgroundImageView.setImage(ImageSource.bitmap(bitmap));
                    lastBackgroundImageView.setImageBitmap(bitmap);
                    lastBackgroundImageView = img;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (lastBackgroundImageView == null) {
                Glide.with(getContext()).load((Uri) img.getTag()).into(img);
                lastBackgroundImageView = img;
            }
            
            //Toast.makeText(drawFragment.this, String.valueOf(zoomlayout.getRealZoom()), Toast.LENGTH_SHORT).show();
        }
        /*
        if (lastBackgroundImageView != null && img != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream((Uri) lastBackgroundImageView.getTag());
    
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // Reduce the size to 1/4 of the original
    
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                //lastBackgroundImageView.setImage(ImageSource.bitmap(bitmap));
                lastBackgroundImageView.setImageBitmap(bitmap);
                lastBackgroundImageView.setBackgroundColor(android.R.color.transparent);
    
                // Close the input stream after use
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Toast.makeText(drawFragment.this, img.getTag().toString(), Toast.LENGTH_SHORT).show();
            if (img != lastBackgroundImageView) {
                //img.setImage(ImageSource.uri((Uri) img.getTag()).tilingEnabled());
                //img.setImageURI((Uri) img.getTag());
                img.setBackgroundColor(R.color.folder_green);
                lastBackgroundImageView = img;
            }
        } else {
            if (img != null) {
                //img.setImage(ImageSource.uri((Uri) img.getTag()).tilingEnabled());
                img.setImageURI((Uri) img.getTag());
                lastBackgroundImageView = img;
            }
        }
        */
    }

    public View[] createContainer(boolean bg) {
        final float scale = getResources().getDisplayMetrics().density;
        int width  = (int) (2100 * scale);
        int height = (int) (2970 * scale);
        return createContainer(bg, width, height);
    }
    
    public View[] createContainer(boolean bg, int width, int height) {
        FrameLayout drawingContainer = new FrameLayout(getContext());
        drawingContainer.setElevation(GeneralUtils.dpToPx(getContext(), 1));

        //Toast.makeText(getContext(), String.)
        final float scale = getResources().getDisplayMetrics().density;
        int margin = (int) (10 * scale);

        drawingContainer.setBackgroundColor(Color.parseColor("#ffffffff"));

        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(width, height);
        layoutParams.setMargins(margin, margin, margin, margin);
        drawingContainer.setLayoutParams(layoutParams);
        
        DrawingView drawingView = new DrawingView(getContext());
        drawingView.changes = false;
        
        drawingView.setDrawingListener(this);
        drawingContainer.addView(drawingView);
        
        return new View[]{drawingContainer, drawingView};
    }

    public DrawingView addContainer() {
        View[] result = createContainer(false);

        FrameLayout drawingContainer = (FrameLayout) result[0];
        DrawingView drawingView = (DrawingView) result[1];
        parentContainer.addView(drawingContainer);

        if (selectedTemplate != 0) {
            drawingView.setTemplate(selectedTemplate);
        }

        drawingView.changeColor(currentColor);
        drawingView.changeTool(currentTool);
        drawingView.changeStrokeWidth(currentStrokeWidth);

        pages.add(drawingView);

        zoomToContent();

        return drawingView;
    }
    
    @Override
    public void onDrawingChanged(DrawingView drawingView) {
        // This method will be called from DrawingView when the drawing changes
        // Implement actions to be performed when drawing changes, such as saving or updating UI
        //savePages(); // Example: Save pages when drawing changes
        //Toast.makeText(MainActivity.this, Integer.toString(undoDrawingViews.size()), Toast.LENGTH_SHORT).show();
        //drawingView.undo();
        //Toast.makeText(MainActivity.this, "change", Toast.LENGTH_SHORT).show();
        undoButton.setImageResource(R.drawable.icon_backward);
        redoButton.setImageResource(R.drawable.icon_forward_disabled);
        
        if (drawingView != null) {
            undoDrawingViews.add(drawingView);
        }
    }
    
    public void undo() {
        // Check if the list is not empty
        if (!undoDrawingViews.isEmpty()) {
            // Get the last DrawingView
            DrawingView lastDrawingView = undoDrawingViews.get(undoDrawingViews.size() - 1);
            redoDrawingViews.add(lastDrawingView);

            // Remove the last DrawingView from the list
            undoDrawingViews.remove(undoDrawingViews.size() - 1);
            if (undoDrawingViews.isEmpty()) {
                undoButton.setImageResource(R.drawable.icon_backward_disabled);
            }
            redoButton.setImageResource(R.drawable.icon_forward);

            lastDrawingView.undo();
        }
    }
    
    public void redo() {
        if (!redoDrawingViews.isEmpty()) {
            // Get the last DrawingView
            DrawingView lastDrawingView = redoDrawingViews.get(redoDrawingViews.size() - 1);
            undoDrawingViews.add(lastDrawingView);

            // Remove the last DrawingView from the list
            redoDrawingViews.remove(redoDrawingViews.size() - 1);
            if (redoDrawingViews.isEmpty()) {
                redoButton.setImageResource(R.drawable.icon_forward_disabled);
            }
            undoButton.setImageResource(R.drawable.icon_backward);

            lastDrawingView.redo();
        }
    }
    
    public boolean addAllPages() {
        parentContainer.removeAllViews();
        try{
        List<Pair<DrawingView, Page>> drawingviews = new ArrayList<>();
        for (Page pg : dbhelper.getPagesByNoteId(noteid)) {
            DrawingView drawingView = addContainer();
            drawingviews.add(new Pair<>(drawingView, pg));
        }
        
        executor.execute(() -> {
            // Loop through the list in reverse
            for (int i = drawingviews.size() - 1; i >= 0; i--) {
                final DrawingView drawingView = drawingviews.get(i).first;
                final Page pg = drawingviews.get(i).second;

                // Post the loadData call to the main thread
                mainHandler.post(() -> {
                    // This will run on the UI thread
                    if ((pg.getBackground() != null) && !(pg.getBackground().startsWith("B")) && !(pg.getBackground().length() <= 3)) {
                        //Toast.makeText(this, pg.getBackground(), Toast.LENGTH_LONG).show();
                        addImageBitmap(Uri.parse(pg.getBackground()), null, -1, drawingView);
                    } else {
                        if (pg.getBackground() != null && pg.getBackground().startsWith("B") && pg.getBackground().length() <= 3) {
                            selectedTemplate = Integer.parseInt(pg.getBackground().substring(1));
                            drawingView.setTemplate(selectedTemplate);
                        } else {
                            drawingView.Background = pg.getBackground();
                        }
                    }
                    String data = pg.getData();
                    if (data != null) {
                        drawingView.setData(data);
                    }
                });
                        /*
                // Optional: add a delay if needed (e.g., simulate processing time)
                try {
                    Thread.sleep(100);  // 100ms delay between each item (adjust if needed)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        */
            }
        });
            } catch(Exception e) {
                Toast.makeText(getContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        /*
        for (Page pg : dbhelper.getPagesByNoteId(noteid)) {
            DrawingView drawingView;
            //Toast.makeText(drawFragment.this, "background: " + pg.getBackground(), Toast.LENGTH_SHORT).show();
            if ((pg.getBackground() != null) && !(pg.getBackground().startsWith("B")) && !(pg.getBackground().length() <= 3)) {
                //Toast.makeText(this, pg.getBackground(), Toast.LENGTH_LONG).show();
                drawingView = addImageBitmap(Uri.parse(pg.getBackground()), null, -1);
            } else {
                drawingView = addContainer();
                if (pg.getBackground() != null && pg.getBackground().startsWith("B") && pg.getBackground().length() <= 3) {
                    selectedTemplate = Integer.parseInt(pg.getBackground().substring(1));
                    drawingView.setTemplate(selectedTemplate);
                } else {
                    drawingView.Background = pg.getBackground();
                }
            }
            String data = pg.getData();
            if (data != null) {
                drawingView.setData(data);
            }
            
            if (pg.getPdf() != null) {
                drawingView.Pdf = pg.getPdf();
            }
            if (pg.getPdfPage() != -1) {
                drawingView.PdfPage = pg.getPdfPage();
            }
        }
        */
        return true;
    }
    
    public void changeColor(int color) {
        currentColor = color;
        for (DrawingView page : pages) {
            page.changeColor(color);
        }
    }
    
    public void changeTool(int tool) {
        currentTool = tool;
        for (DrawingView page : pages) {
            //Toast.makeText(drawFragment.this, String.valueOf(tool), Toast.LENGTH_SHORT).show();
            page.changeTool(tool);
        }
    }
    
    public void changeStrokeWidth(int width) {
        currentStrokeWidth = width;
        for (DrawingView page : pages) {
            page.changeStrokeWidth(width);
        }
    }
    
    public void savePages(boolean force) {
        int i = 0;
        for (DrawingView page : pages) {
            if (page.getChanges() && (!page.getActiveLine() || force)) {
                //Toast.makeText(drawFragment.this, "saving...", Toast.LENGTH_SHORT).show();
                
                Page pg = new Page();
                pg.setData(page.getData());
                pg.setNoteId(noteid);
                pg.setNumber(i);
                if (page.Background != null) {
                    //Toast.makeText(this, page.Background, Toast.LENGTH_LONG).show();
                    pg.setBackground(page.Background);
                } else if (selectedTemplate != 0) {
                    pg.setBackground("B" + Integer.toString(selectedTemplate));
                }
                if (page.Pdf != null) {
                    //Toast.makeText(this, page.Pdf, Toast.LENGTH_LONG).show();
                    pg.setPdf(page.Pdf);
                }
                if (page.PdfPage != -1) {
                    //Toast.makeText(this, String.valueOf(page.PdfPage), Toast.LENGTH_LONG).show();
                    pg.setPdfPage(page.PdfPage);
                }
                
                if (dbhelper.pageExist(pg, false)) {
                    if (page.getAttachmentChanges()) {
                        dbhelper.updatePage(pg, SyncUtils.STATUS_ATTACHMENT_SYNC_PENDING, false);
                    } else {
                        dbhelper.updatePage(pg, false);
                    }
                } else {
                    if (page.getAttachmentChanges()) {
                        dbhelper.addPage(pg, SyncUtils.STATUS_ATTACHMENT_SYNC_PENDING);
                    } else {
                        dbhelper.addPage(pg);
                    }
                }
            }
            i++;
        }
    }
    
    public void zoomToContent() {
        LinearLayout layout = fragmentView.findViewById(R.id.parent_container);
        ViewTreeObserver vto = layout.getViewTreeObserver(); 
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
            @Override 
            public void onGlobalLayout() { 
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                
                // Get the layout width
                int layoutHeight = layout.getMeasuredHeight();

                // Get the display width
                WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics displayMetrics = new DisplayMetrics();
                display.getMetrics(displayMetrics);
                int displayHeight = displayMetrics.heightPixels;

                // Calculate zoom factor
                zoomFactor = (float) layoutHeight / displayHeight;

                // Display results using Toast
                /*
                Toast.makeText(MainActivity.this, "Layout Height: " + layoutHeight + " pixels", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Display Height: " + displayHeight + " pixels", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "Zoom Factor: " + zoomFactor, Toast.LENGTH_SHORT).show();
                */
                    
                //zoomlayout.zoomTo(zoomFactor / 2, true);
                int contentBottomY = layoutHeight;  // The bottom y-coordinate of the content
                int viewportHeight = zoomlayout.getHeight();  // The height of the visible area
                int scrollY = contentBottomY - viewportHeight;  // The y-coordinate to scroll to
                    
                final float scale = getResources().getDisplayMetrics().density;
                int container_height = (int) (2970 * scale);
 
                zoomlayout.panTo(0, -(scrollY - container_height * 1.5f), false);
                zoomlayout.realZoomTo(0.5f, true);
            } 
        });
    }
    
    private void loadColorpickerPopup() {
        // Inflate your custom layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View customView = inflater.inflate(R.layout.colorpicker_popup, null);
        
        //View root_view = this.getWindow().getDecorView().getRootView();
        layout_MainMenu = requireActivity().findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);
        colorpickerButtonLeft = customView.findViewById(R.id.left);
        colorpickerButtonRight = customView.findViewById(R.id.right);
        ColorPickerView colorPickerView = customView.findViewById(R.id.colorPickerView);
        BrightnessSlideBar brightnessSlideBar = customView.findViewById(R.id.brightnessSlide);
        colorPickerView.attachBrightnessSlider(brightnessSlideBar);
        
        colorPickerView.setColorListener(new ColorEnvelopeListener() {
            @Override
            public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                colorpickerColor = envelope.getColor();
            }
        });

        int height = getResources().getDisplayMetrics().heightPixels - GeneralUtils.dpToPx(getContext(), 100); // 50 pixels margin on top and bottom
        popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set focusable to true to prevent outside touches from dismissing the popup
        popupWindow.setFocusable(true);
        
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                layout_MainMenu.getForeground().setAlpha(0);
            }
        });
    }
        
    private void showColorpickerPopup(int action, Object obj) {
        View parentView = fragmentView.findViewById(R.id.parentLayout);
        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(getContext(), 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
        
        switch (action) {
            case ACTION_ADD_COLOR:
                colorpickerButtonLeft.setText("save");
                colorpickerButtonRight.setText("use");
                //save
                colorpickerButtonLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(drawFragment.this, String.valueOf(colorpickerColor), Toast.LENGTH_SHORT).show();
                        if (!buttonColors.contains(colorpickerColor)) {
                            buttonColors.add(colorpickerColor);
                            saveColors();
                            addColorButton(colorpickerColor, false);
                            //addColorButtons();
                            popupWindow.dismiss();
                        }
                    }
                });
            //use
                colorpickerButtonRight.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                changeColor(colorpickerColor);
                                // btn.setImageTintList(ColorStateList.valueOf(colorpickerColor));
                                GradientDrawable drawable2 = new GradientDrawable();
                                drawable2.setShape(GradientDrawable.OVAL);
                                drawable2.setColor(0x22000000);
                                // set original color to old button
                                GradientDrawable drawable3 = new GradientDrawable();
                                drawable3.setShape(GradientDrawable.OVAL);
                                drawable3.setColor(oldColor);
                                drawable3.setStroke(2, 0xFFFFFFFF);
                                if (oldColorButton != null) {
                                    oldColorButton.setBackgroundResource(
                                            R.drawable.button_background);
                                    oldColorButton.setImageDrawable(drawable3);
                                }
                                ImageButton btn = (ImageButton) obj;
                                oldColorButton = btn;
                                btn.setBackground(drawable2);
                                popupWindow.dismiss();
                            }
                        });
                break;
            case ACTION_CHANGE_COLOR:
                colorpickerButtonLeft.setText("delete");
                colorpickerButtonRight.setText("save");
                //save
                colorpickerButtonRight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int index = buttonColors.indexOf((int) obj);
                        if (index != -1) {
                            // Ersetze das Element
                            buttonColors.set(index, colorpickerColor);
                        }
                        oldColor = (int) obj;
                        changeColor((int) obj);
                        saveColors();
                        addColorButtons(false);
                        
                        GradientDrawable drawable2 = new GradientDrawable();
                        drawable2.setShape(GradientDrawable.OVAL);
                        drawable2.setColor(0x22000000);
                        
                        ImageButton btn = fragmentView.findViewById(R.id.add_color);
                        oldColorButton = btn;
                        btn.setBackground(drawable2);
                        
                        popupWindow.dismiss();
                    }
                });
                //delete
                colorpickerButtonLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        buttonColors.remove(Integer.valueOf((int) obj));
                        saveColors();
                        addColorButtons(false);
                        
                        ImageButton btn = fragmentView.findViewById(R.id.add_color);
                        if (!buttonColors.contains(oldColor)) {
                            GradientDrawable drawable2 = new GradientDrawable();
                            drawable2.setShape(GradientDrawable.OVAL);
                            drawable2.setColor(0x22000000);
                            btn.setBackground(drawable2);
                        }
                        
                        oldColorButton = btn;
                        
                        popupWindow.dismiss();
                    }
                });
                break;
        }
        
        //right button to save and left to delete if long click to edit
        //right button to use and left to add if add color button click
        /*
        if (colorPickerView != null) {
            colorPickerView.attachBrightnessSlider(brightnessSlideBar);
        } else {
            Toast.makeText(drawFragment.this, "color picker view is null", Toast.LENGTH_SHORT).show();
        }
        */
    }
    
    private void loadLinePopup() {
        // Inflate your custom layout
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View customView = inflater.inflate(R.layout.addline_popup, null);
        
        //View root_view = this.getWindow().getDecorView().getRootView();
        layout_MainMenu = requireActivity().findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);
        linePopupButtonLeft = customView.findViewById(R.id.left);
        linePopupButtonRight = customView.findViewById(R.id.right);
        lineSlider = customView.findViewById(R.id.slider);

        lineSlider.setLabelFormatter(new LabelFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        int height = getResources().getDisplayMetrics().heightPixels - GeneralUtils.dpToPx(getContext(), 100); // 50 pixels margin on top and bottom
        popupWindowLine = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set focusable to true to prevent outside touches from dismissing the popup
        popupWindowLine.setFocusable(true);
        
        popupWindowLine.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                layout_MainMenu.getForeground().setAlpha(0);
            }
        });
    }
    
    private void showLinePopup(int action, Object obj) {
        View parentView = fragmentView.findViewById(R.id.parentLayout);
        popupWindowLine.showAtLocation(parentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(getContext(), 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
        
        switch (action) {
            case ACTION_ADD_LINE:
                linePopupButtonLeft.setText("save");
                linePopupButtonRight.setText("use");
            
                //save
                linePopupButtonLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Toast.makeText(drawFragment.this, String.valueOf(colorpickerColor), Toast.LENGTH_SHORT).show();
                        int line = (int) lineSlider.getValue();
                        if (!buttonLines.contains(line)) {
                            buttonLines.add(line);
                            saveLines();
                            addLineButton(line, false);
                            //addColorButtons();
                            popupWindowLine.dismiss();
                        }
                    }
                });

                //use
                linePopupButtonRight.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                popupWindowLine.dismiss();
                            }
                        });
            
                break;
            case ACTION_CHANGE_LINE:
                linePopupButtonLeft.setText("delete");
                linePopupButtonRight.setText("save");
                lineSlider.setValue((int) obj);
                //save
            
                linePopupButtonRight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int line = (int) lineSlider.getValue();
                        int index = buttonLines.indexOf((int) obj);
                        if (index != -1) {
                            // Ersetze das Element
                            buttonLines.set(index, line);
                        }
                        currentLine = (int) obj;
                        //changeColor((int) obj);
                        saveLines();
                        addLineButtons();
                        
                        GradientDrawable drawable2 = new GradientDrawable();
                        drawable2.setShape(GradientDrawable.OVAL);
                        drawable2.setColor(0x22000000);
                        
                        ImageButton btn = fragmentView.findViewById(R.id.add_line);
                        oldLineButton = btn;
                        btn.setBackground(drawable2);
                        
                        popupWindowLine.dismiss();
                    }
                });
                //delete
                linePopupButtonLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        buttonLines.remove(Integer.valueOf((int) obj));
                        saveLines();
                        addLineButtons();
                        
                        ImageButton btn = fragmentView.findViewById(R.id.add_line);
                        if (!buttonLines.contains(currentLine)) {
                            GradientDrawable drawable2 = new GradientDrawable();
                            drawable2.setShape(GradientDrawable.OVAL);
                            drawable2.setColor(0x22000000);
                            btn.setBackground(drawable2);
                        }
                        
                        oldLineButton = btn;
                        
                        popupWindowLine.dismiss();
                    }
                });
            
                break;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autosaveTimer != null) {
            autosaveTimer.cancel();
        }
    }
}
