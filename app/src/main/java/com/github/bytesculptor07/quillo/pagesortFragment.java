package com.github.bytesculptor07.quillo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.bytesculptor07.quillo.Utils.GeneralUtils;
import com.github.bytesculptor07.quillo.database.NotesDatabaseHelper;
import com.github.bytesculptor07.quillo.models.Page;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class pagesortFragment extends Fragment{
    String noteid;
    LinearLayout parentContainer;
    List<View> placeholders = new ArrayList<>();
    List<View> pages = new ArrayList<>();
    
    public final int TOOL_HIGHLIGHTER = 1;
    
    public final int BACKGROUND_LINES = 8;
    public final int BACKGROUND_SQUARE = 9;
    public final int BACKGROUND_EMPTY = 10;
    public final int BACKGROUND_DOT = 11;
    public final int BACKGROUND_MUSIC = 12;
    NotesDatabaseHelper notesdbhelper;
    public drawFragment parent;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pagesort, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notesdbhelper = NotesDatabaseHelper.getInstance(getContext());
        
        parentContainer = view.findViewById(R.id.container);
        ScrollView scrollview = view.findViewById(R.id.scrollview);
        
        DragAndDropHandler.DropListener listener = new DragAndDropHandler.DropListener(placeholders, scrollview);
        listener.setCallback(new DragAndDropHandler.dropCallback() {
            @Override
            public void onDrop(View page, View placeholder) {
                int pageIndex = pages.indexOf(page);
                int placeholderIndex = placeholders.indexOf(placeholder);
                Toast.makeText(getContext(), String.valueOf(pageIndex) + " -> " + String.valueOf(placeholderIndex), Toast.LENGTH_SHORT).show();
                resortPages(pageIndex, placeholderIndex);
            }
        });
        parentContainer.setOnDragListener(listener);
        
        if (noteid != null) {
            initSidebar();
        }
    }

    public void setParent(drawFragment fragment) {
        parent = fragment;
    }
    
    public void resortPages(int start, int end) {
        List<Page> pgs = notesdbhelper.getPagesByNoteId(noteid);

        Page pg = pgs.remove(start);
        if (start < end) {
            end --;
        }
        pgs.add(end, pg);

        int i = 0;
        for (Page p : pgs) {
            p.setNumber(i);
            notesdbhelper.updatePage(p, true);
            i ++;
        }

        refresh();
    }

    public void refresh() {
        initSidebar();
        parent.addAllPages();
    }
    
    public void loadNote(String noteid) {
        this.noteid = noteid;
        
        if (parentContainer != null) {
            initSidebar();
        }
    }
    
    private View createPlaceholder(Context context, boolean first) {
        View placeholder = new View(context);
    
        // Set layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            GeneralUtils.dpToPx(getContext(), 2),
            LinearLayout.LayoutParams.MATCH_PARENT
        );
        
        if (first) {
            params.setMargins(GeneralUtils.dpToPx(getContext(), 9), GeneralUtils.dpToPx(getContext(), 0), GeneralUtils.dpToPx(getContext(), 0), GeneralUtils.dpToPx(getContext(), 0));
        }
        
        placeholder.setLayoutParams(params);
    
        // Set initial background as transparent
        placeholder.setBackgroundColor(android.graphics.Color.TRANSPARENT);
    
        return placeholder;
    }
    
    @SuppressLint("ClickableViewAccessibility")
    public void initSidebar() {
        parentContainer.removeAllViews();
        pages.clear();
        placeholders.clear();

        List<Page> pgs = notesdbhelper.getPagesByNoteId(noteid);
        
        boolean left = true;
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        
        for (Page pg : pgs) {
            ShapeableImageView note = createPage(pg, left);
            pages.add(note);
            note.setContentDescription(getString(R.string.open_page_options));
            note.setOnTouchListener(new DragAndDropHandler.DragListener(getContext(), new DragAndDropHandler.DragListener.TapListener() {
                @Override
                public void onShortClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(getContext(), view);
                    MenuInflater inflater = popupMenu.getMenuInflater();
                    inflater.inflate(R.menu.page_menu, popupMenu.getMenu());
                    popupMenu.show();
                    popupMenu.setOnMenuItemClickListener(new PagePopupMenuListener(getContext(), pg, pagesortFragment.this));
                }
            }));
            if (left) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                
                parentContainer.addView(row);
                
                View placeholder = createPlaceholder(getContext(), true);
                placeholders.add(placeholder);
                row.addView(placeholder);
                
                row.addView(note);
                
                View placeholder2 = createPlaceholder(getContext(), false);
                placeholders.add(placeholder2);
                row.addView(placeholder2);
            } else {
                row.addView(note);
            }
            left = !left;
        }
        
        if (left) {
            View placeholder = createPlaceholder(getContext(), false);
            placeholders.add(placeholder);
            row.addView(placeholder);
        }
    }
    
    public ShapeableImageView createPage(Page pg, boolean left) {
        Bitmap thumbnail = null;
        
        String data = pg.getData();
        int background = 0;
        if (pg.getBackground() != null && pg.getBackground().startsWith("B") && pg.getBackground().length() <= 3) {
            background = Integer.parseInt(pg.getBackground().substring(1));
        }
        if (data != null) {
            thumbnail = createThumbnail(GeneralUtils.dpToPx(getContext(), 120), GeneralUtils.dpToPx(getContext(), (int) (120 * 2970f / 2100f)), data, background);
        } else {
            thumbnail = Bitmap.createBitmap(GeneralUtils.dpToPx(getContext(), 120), GeneralUtils.dpToPx(getContext(), (int) (120 * 2970f / 2100f)), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(thumbnail);
            canvas.drawColor(Color.WHITE);
        }
        
        ShapeableImageView imageButton = new ShapeableImageView(getContext());
        LinearLayout.LayoutParams imageButtonParams =
            new LinearLayout.LayoutParams(GeneralUtils.dpToPx(getContext(), 120), GeneralUtils.dpToPx(getContext(), (int) (120 * 2970f / 2100f)));
        
        if (left) {
            imageButtonParams.setMargins(GeneralUtils.dpToPx(getContext(), 9), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 9), GeneralUtils.dpToPx(getContext(), 15));
        } else {
            imageButtonParams.setMargins(GeneralUtils.dpToPx(getContext(), 9), GeneralUtils.dpToPx(getContext(), 15), GeneralUtils.dpToPx(getContext(), 20), GeneralUtils.dpToPx(getContext(), 15));
        }
        
        imageButton.setLayoutParams(imageButtonParams);
        imageButton.setImageBitmap(thumbnail);
        imageButton.setScaleType(ShapeableImageView.ScaleType.CENTER_INSIDE);
        imageButton.setBackgroundResource(android.R.color.transparent);
        imageButton.setId(View.generateViewId());
        imageButton.setShapeAppearanceModel(
                imageButton.getShapeAppearanceModel().toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, GeneralUtils.dpToPx(getContext(), 16))
                        .build());

        imageButton.setElevation(GeneralUtils.dpToPx(getContext(), 2));
        
        return imageButton;
    }
    
    public Bitmap createThumbnail(int imgwidth, int imgheight, String data, int background) {
        Bitmap thumbnail = Bitmap.createBitmap(imgwidth, imgheight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(thumbnail);

        // Berechnen des Skalierungsfaktors
        float scaleX = (float) imgwidth / GeneralUtils.dpToPx(getContext(), 2100);
        float scaleY = (float) imgheight / GeneralUtils.dpToPx(getContext(), 2970);
        canvas.scale(scaleX, scaleY);
        
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        Type listType = new TypeToken<List<SerializablePath>>() {}.getType();
        List<SerializablePath> paths = gson.fromJson(data, listType);

        // Zeichnen der Pfade auf der Canvas des Thumbnails
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
}
