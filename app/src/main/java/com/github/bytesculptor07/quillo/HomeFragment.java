package com.github.bytesculptor07.quillo;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.bytesculptor07.quillo.Utils.GeneralUtils;
import com.github.bytesculptor07.quillo.Utils.SyncUtils;
import com.github.bytesculptor07.quillo.database.FoldersDatabaseHelper;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Folder;
import com.github.bytesculptor07.quillo.models.Page;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class HomeFragment extends Fragment {
    LinearLayout folderList;

    String currentFolder = "/";

    FoldersDatabaseHelper foldersdbhelper;
    NotesDatabaseHelper notesdbhelper;

    View oldIcon;
    int selectedIcon;
    View oldColor;
    ShapeableImageView oldTemplateButton;
    int selectedTemplate;
    int selectedColor;

    LinearLayout layout_MainMenu;
    PopupWindow popupWindow;
    PopupWindow notebookPopupWindow;
    View fragmentView;

    public final int TOOL_HIGHLIGHTER = 1;

    public final int BACKGROUND_LINES = 8;
    public final int BACKGROUND_SQUARE = 9;
    public final int BACKGROUND_EMPTY = 10;
    public final int BACKGROUND_DOT = 11;
    public final int BACKGROUND_MUSIC = 12;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        foldersdbhelper = FoldersDatabaseHelper.getInstance(getContext());
        notesdbhelper = NotesDatabaseHelper.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fragmentView = view;

        Button create = view.findViewById(R.id.create);
        folderList = view.findViewById(R.id.folderList);

        create.setOnClickListener(createNew);

        loadItems();
        loadPopup();
        loadNotebookPopup();
    }

    private final View.OnClickListener createNew = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showPopupMenu(view);
        }
    };


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
                        placeholder.getLayoutParams().height = GeneralUtils.dpToPx(getContext(), 15);
                        folderList.addView(placeholder);
                        currentLayout = createLayout();
                        folderList.addView(currentLayout);
                        currentWidth = 0;

                        // load notes
                        List<String> ids = new ArrayList<>();
                        for (Page page : notesdbhelper.getAllPages()) {
                            if (GeneralUtils.getPathOfNoteid(page.getNoteId()).equals(currentFolder)
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

    public LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    public View createFolder(Folder folder) {
        ConstraintLayout constraintLayout = new ConstraintLayout(getContext());
        ConstraintLayout.LayoutParams layoutParams =
                new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintLayout.setLayoutParams(layoutParams);

        // Create an ImageButton
        ImageButton imageButton = new ImageButton(getContext());
        ConstraintLayout.LayoutParams imageButtonParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 150), GeneralUtils.dpToPx(getContext(), 150));
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageResource(R.drawable.icon_folder);
        int color = ContextCompat.getColor(getContext(), GeneralUtils.colorIds[folder.getColor()]);
        imageButton.setImageTintList(ColorStateList.valueOf(color));
        imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        //imageButton.setBackgroundResource(android.R.color.transparent);
        imageButton.setBackgroundResource(R.drawable.menu_button_background);
        imageButton.setId(View.generateViewId());

        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentFolder += String.valueOf(folder.getId()) + "/";
                        loadItems();
                    }
                });

        // Create an ImageView
        ImageView imageView = new ImageView(getContext());
        ConstraintLayout.LayoutParams imageViewParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 50), GeneralUtils.dpToPx(getContext(), 50));
        imageView.setLayoutParams(imageViewParams);
        imageView.setImageResource(GeneralUtils.iconIds[folder.getIcon()]);
        if (GeneralUtils.colorIds[folder.getColor()] == R.color.folder_black) {
            int color2 = ContextCompat.getColor(getContext(), R.color.white);
            imageView.setImageTintList(ColorStateList.valueOf(color2));
        }
        imageView.setPadding(0, GeneralUtils.dpToPx(getContext(), 15), 0, 0);
        imageView.setId(View.generateViewId());

        // Create a TextView
        TextView textView = new TextView(getContext());
        textView.setText(folder.getName());
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text));
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
                -GeneralUtils.dpToPx(getContext(), 25));
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
        return constraintLayout;
    }

    public View createNote(String noteid) {
        ConstraintLayout constraintLayout = new ConstraintLayout(getContext());
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
                thumbnail = createThumbnail(GeneralUtils.dpToPx(getContext(), 150), GeneralUtils.dpToPx(getContext(), (int) (150 * 2970f / 2100f)), data, background);
            } else {
                thumbnail = createBackgroundThumbnail(GeneralUtils.dpToPx(getContext(), 150), GeneralUtils.dpToPx(getContext(), (int) (150 * 2970f / 2100f)), background);
            }
        } catch(Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        ShapeableImageView imageButton = new ShapeableImageView(getContext());
        ConstraintLayout.LayoutParams imageButtonParams =
                new ConstraintLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 150), GeneralUtils.dpToPx(getContext(), (int) (150 * 2970f / 2100f)));
        imageButtonParams.setMargins(GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15));
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageBitmap(thumbnail);
        imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);

        imageButton.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.template_button_ripple));
        imageButton.setId(View.generateViewId());
        imageButton.setShapeAppearanceModel(
                imageButton.getShapeAppearanceModel().toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(getContext(), 16))
                        .build());
        imageButton.setElevation(GeneralUtils.dpToPx(getContext(), 4));


        imageButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // open note
                        try {
                            Intent intent = new Intent(getContext(), drawActivity.class);
                            intent.putExtra("noteid", noteid);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(intent);
                        } catch(Exception e) {
                            Toast.makeText(getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Create a TextView
        TextView textView = new TextView(getContext());
        textView.setText(GeneralUtils.getNameOfNoteId(noteid));
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text));
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
                GeneralUtils.dpToPx(getContext(), 4));
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
        return constraintLayout;
    }

    public Bitmap createThumbnail(int imgwidth, int imgheight, String data, int background) {
        Bitmap thumbnail = Bitmap.createBitmap(imgwidth, imgheight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbnail);

        float scaleX = (float) imgwidth / GeneralUtils.dpToPx(getContext(), 2100);
        float scaleY = (float) imgheight / GeneralUtils.dpToPx(getContext(), 2970);
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
            path.convertToPx(getContext());
            path.loadPoints(getContext());
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
        float scaleX = (float) imgwidth / GeneralUtils.dpToPx(getContext(), 2100);
        float scaleY = (float) imgheight / GeneralUtils.dpToPx(getContext(), 2970);
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

        canvas.drawRoundRect(new RectF(0, 0, width, height), GeneralUtils.dpToPx(getContext(), 160), GeneralUtils.dpToPx(getContext(), 160), bgpaint);

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

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(getContext(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_notebook) {
                    showNotebookPopup();
                } else if  (item.getItemId() == R.id.action_folder){
                    showFolderPopup();
                }
                return true;
            }
        });

        popupMenu.show();
    }


    private void loadNotebookPopup() {
        // Inflate your custom layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.notebook_popup, null);

        View root_view = requireActivity().getWindow().getDecorView().getRootView();
        //LinearLayout iconList = customView.findViewById(R.id.iconList);
        //LinearLayout colorList = customView.findViewById(R.id.colorList);
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
            thumbnail = createBackgroundThumbnail(GeneralUtils.dpToPx(getContext(), 120), GeneralUtils.dpToPx(getContext(), (int) (120 * 2970f / 2100f)), bg);

            ShapeableImageView imageButton = new ShapeableImageView(getContext());
            LinearLayout.LayoutParams imageButtonParams =
                    new LinearLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 120), GeneralUtils.dpToPx(getContext(), (int) (120 * 2970f / 2100f)));
            imageButtonParams.setMargins(GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15));
            imageButton.setLayoutParams(imageButtonParams);
            imageButton.setImageBitmap(thumbnail);
            imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);
            //imageButton.setBackgroundResource(android.R.color.holo_red_dark);
            //imageButton.setForeground(ContextCompat.getDrawable(getContext(), android.R.color.holo_red_dark));
            imageButton.setId(View.generateViewId());
            imageButton.setShapeAppearanceModel(
                    imageButton.getShapeAppearanceModel().toBuilder()
                            .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(getContext(), 16))
                            .build());

            if (bg == BACKGROUND_EMPTY) {
                imageButton.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.template_button_selected));
                oldTemplateButton = imageButton;
            } else {
                imageButton.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.template_button_ripple));
            }

            imageButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if ((ShapeableImageView) view != oldTemplateButton) {
                                oldTemplateButton.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.template_button_ripple));
                                oldTemplateButton = (ShapeableImageView) view;
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(() -> view.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.template_button_selected)), 100);
                                selectedTemplate = bg;
                            }
                        }
                    });
            imageButton.setElevation(GeneralUtils.dpToPx(getContext(), 4));

            templateList.addView(imageButton);
        }

        int height = getResources().getDisplayMetrics().heightPixels - GeneralUtils.dpToPx(getContext(), 100); // 50 pixels margin on top and bottom
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
                        notebookPopupWindow.dismiss();
                    }
                });

        button_create.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Page pg = new Page();
                        pg.setNoteId(currentFolder + textedit_name.getText().toString());
                        if (selectedTemplate != BACKGROUND_EMPTY) {
                            pg.setBackground("B" + Integer.toString(selectedTemplate));
                        }
                        pg.setNumber(0);
                        if (!notesdbhelper.pageExist(pg, false)) {
                            notesdbhelper.addPage(pg);
                        } else {
                            Toast.makeText(getContext(), "Notebook exists", Toast.LENGTH_SHORT).show();
                        }
                        loadItems();
                        notebookPopupWindow.dismiss();
                    }
                });

        popupLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    private void loadPopup() {
        // Inflate custom layout
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.create_popup, null);

        View root_view = requireActivity().getWindow().getDecorView().getRootView();
        LinearLayout iconList = customView.findViewById(R.id.iconList);
        LinearLayout colorList = customView.findViewById(R.id.colorList);
        LinearLayout popupLayout = customView.findViewById(R.id.popupLayout);
        Button button_cancel = customView.findViewById(R.id.cancel);
        Button button_create = customView.findViewById(R.id.create);
        EditText textedit_name = customView.findViewById(R.id.textField);
        layout_MainMenu = root_view.findViewById(R.id.parentLayout);
        layout_MainMenu.getForeground().setAlpha(0);

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

        oldIcon = new View(getContext());
        int i = 0;
        for (int iconId : GeneralUtils.iconIds) {
            ImageButton icon = new ImageButton(getContext());
            icon.setImageResource(iconId);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            icon.setPadding(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 50), GeneralUtils.dpToPx(getContext(), 50));
            layoutParams.setMargins(GeneralUtils.dpToPx(getContext(), 20), GeneralUtils.dpToPx(getContext(), 20), GeneralUtils.dpToPx(getContext(), 20), GeneralUtils.dpToPx(getContext(), 20));
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
        oldColor = new View(getContext());
        i = 0;
        for (int colorId : GeneralUtils.colorIds) {
            ImageButton color = new ImageButton(getContext());

            // Create a ShapeDrawable to make a circle with the color
            ShapeDrawable circle = new ShapeDrawable(new OvalShape());
            circle.getPaint().setColor(ContextCompat.getColor(getContext(), colorId));
            InsetDrawable insetCircle = new InsetDrawable(circle, GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));

            // Create a LayerDrawable that combines the menu button background and the circle
            Drawable[] layers = new Drawable[2];
            layers[0] =
                    ContextCompat.getDrawable(
                            getContext(), R.drawable.menu_button_background); // background layer
            layers[1] = insetCircle; // circle layer
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            color.setBackground(layerDrawable);

            // Set the size and margins of the ImageButton
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 40), GeneralUtils.dpToPx(getContext(), 40));
            layoutParams.setMargins(GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 15));
            color.setPadding(GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
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
                                        .setColor(ContextCompat.getColor(getContext(), GeneralUtils.colorIds[selectedColor]));
                                InsetDrawable insetCircle1 = new InsetDrawable(circle1, GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
                                Drawable[] layers1 = new Drawable[2];
                                layers1[0] =
                                        ContextCompat.getDrawable(
                                                getContext(),
                                                R.drawable.menu_button_background); // background
                                // layer
                                layers1[1] = insetCircle1; // circle layer
                                LayerDrawable layerDrawable1 = new LayerDrawable(layers1);
                                oldColor.setBackground(layerDrawable1);

                                selectedColor = i_final;
                                oldColor = view;

                                ShapeDrawable circle2 = new ShapeDrawable(new OvalShape());
                                circle2.getPaint()
                                        .setColor(ContextCompat.getColor(getContext(), GeneralUtils.colorIds[selectedColor]));
                                InsetDrawable insetCircle2 = new InsetDrawable(circle2, GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5), GeneralUtils.dpToPx(getContext(), 5));
                                Drawable[] layers2 = new Drawable[2];
                                layers2[0] =
                                        ContextCompat.getDrawable(
                                                getContext(),
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
                        ((MainActivity) requireActivity()).syncInBackground();
                        popupWindow.dismiss();
                    }
                });
    }

    private void showFolderPopup() {
        popupWindow.showAtLocation(fragmentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(getContext(), 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
    }

    private void showNotebookPopup() {
        notebookPopupWindow.showAtLocation(fragmentView, Gravity.CENTER, 0, -GeneralUtils.dpToPx(getContext(), 25/2));
        layout_MainMenu.getForeground().setAlpha(150);
    }

    public boolean onBackPressed() {
        if (currentFolder.length() > 1) {
            if (currentFolder.endsWith("/")) {
                currentFolder = currentFolder.substring(0, currentFolder.length() - 1);
            }

            int lastSlashIndex = currentFolder.lastIndexOf("/");

            if (lastSlashIndex != -1) {
                currentFolder = currentFolder.substring(0, lastSlashIndex + 1);
            }
            loadItems();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notesdbhelper != null) {
            notesdbhelper.close();
        }
    }

}