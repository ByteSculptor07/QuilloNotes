package com.github.bytesculptor07.quillo;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.PointF;
import android.graphics.DashPathEffect;
import android.graphics.Region;
import android.view.MotionEvent;
import android.view.View;
import android.view.PointerIcon;
import android.widget.Toast;
import com.google.android.datatransport.runtime.dagger.multibindings.IntoMap;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;

public class DrawingView extends View {
    private Paint paint = new Paint();
    private DrawingListener drawingListener;
    private List<SerializablePath> lines = new ArrayList<>();
    private List<Action> undoStack = new ArrayList<>();
    private List<Action> redoStack = new ArrayList<>();
    private SerializablePath currentPath;
    private int currentColor = Color.BLACK; // Current color for new paths
    int currentStrokeWidth = 10;
    Context cntxt;
    float prevX;
    float prevY;
    boolean changes = false;
    boolean attachmentChanges = false;
    boolean activeLine = false;
    SerializablePath lassoPath;
    List<SerializablePath> selectedPaths;
    boolean showLaser = false;
    
    final boolean setting_pen_preview = true; //disable if needed
    final boolean setting_eraser_preview = true; //disable if needed
    final boolean setting_need_lasso_fully_intersecting = false;
    
    private float initialTouchX, initialTouchY;
    private float offsetX, offsetY;
    private boolean isDragging = false;
    private boolean isHolding = false;
    private boolean shapeCorrected = false;
    
    int PdfPage = -1;
    String Pdf;
    String Background;
    
    int currentBackground;
    
    Ink.Builder inkBuilder;
    Ink.Stroke.Builder strokeBuilder;
    
    public final int TOOL_PEN = 0;
    public final int TOOL_HIGHLIGHTER = 1;
    public final int TOOL_ERASER = 2;
    public final int TOOL_LASSO = 3;
    public final int TOOL_LASER = 4;
    public final int TOOL_PATH = 5;
    
    public final int BACKGROUND_LINES = 8;
    public final int BACKGROUND_SQUARE = 9;
    public final int BACKGROUND_EMPTY = 10;
    public final int BACKGROUND_DOT = 11;
    public final int BACKGROUND_MUSIC = 12;
    
    private static final float MOVE_THRESHOLD = 100f; // pixels
    private static final long HOLD_THRESHOLD = 500; // milliseconds
    private static final long MIN_TIME_THRESHOLD = 50; // milliseconds
    private long lastMoveTimestamp = 0;

    private Paint erasingCirclePaint = new Paint();
    private boolean showErasingCircle = false;
    private boolean showErasingCirclePreview = false;
    private boolean showPenPreview = false;
    private float currentX;
    private float currentY;
    private float startX;
    private float startY;
    private float erasingCircleRadius = 20f;
    private long lastMoveTime;
    
    private boolean currentRectTop = false;
    private boolean currentRectBottom = false;
    private boolean currentRectRight = false;
    private boolean currentRectLeft = false;
    
    
    private int currentTool = TOOL_PEN;
    private String currentShape = null;
    
    private PointF currentPoint = null;
    private PointF lastPoint = null;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Handler handler2 = new Handler(Looper.getMainLooper());
    private Handler handler3 = new Handler(Looper.getMainLooper());
    private Handler hover_handler = new Handler(Looper.getMainLooper());
    private boolean penPreviewHover = false;

    public DrawingView(Context context) {
        super(context);
        cntxt = context;
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        erasingCirclePaint.setStyle(Paint.Style.STROKE);
        erasingCirclePaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentBackground != 0) {
            paint.setARGB(100, 100, 100, 100);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setPathEffect(null);
            final float scale = getResources().getDisplayMetrics().density;
            int width  = (int) (2100 * scale);
            int height = (int) (2970 * scale);
            int margin = width / 8;
            int h;
            switch (currentBackground) {
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
                    paint.setStrokeWidth(6f);
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
        }
        
        if (currentPath != null && currentPath.getTool() ==  TOOL_LASSO) {
            paint.setARGB(100, 100, 100, 100);
            paint.setPathEffect(new DashPathEffect(new float[] {10f,20f}, 0f));
            paint.setStrokeWidth(5f);
            canvas.drawPath(currentPath, paint);
        } else if(currentTool == TOOL_PATH && currentPath != null && currentPoint != null) {
            if (lastPoint == null) {
                paint.setColor(currentColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(currentStrokeWidth);
                canvas.drawPoint(currentPoint.x, currentPoint.y, paint);
            } else {
                SerializablePath copyPath = new SerializablePath();
                copyPath.setColor(currentColor); // Set the color of the new path
                copyPath.setTool(currentTool);
                copyPath.setPathPoints(currentPath.getPathPoints());
                copyPath.moveTo(currentPath.getPathPoints().get(0)[0], currentPath.getPathPoints().get(0)[1]);
                ArrayList<float[]> copyOfPathPoints = new ArrayList<>(currentPath.getPathPoints());
                copyOfPathPoints.remove(0);
                for (float[] pointSet : copyOfPathPoints) {
                    copyPath.quadTo(pointSet[0], pointSet[1], pointSet[2], pointSet[3]);
                }
                copyPath.quadTo(lastPoint.x, lastPoint.y, (currentPoint.x + lastPoint.x) / 2, (currentPoint.y + lastPoint.y) / 2);
                paint.setColor(currentPath.getColor());
                paint.setStrokeWidth(currentStrokeWidth);
                paint.setPathEffect(null);
                paint.setAntiAlias(true);
                paint.setAlpha(255);
                canvas.drawPath(copyPath, paint);
            }
        }
        for (SerializablePath line : lines) {
            switch(line.getTool()) {
                case TOOL_PATH:
                case TOOL_PEN:
                    //if (line.getShape() == null || line.getShape().equalsIgnoreCase("arrow")) {
                        paint.setColor(line.getColor()); // Use the color stored in each path
                        paint.setStrokeWidth(line.getWidth());
                        paint.setPathEffect(null);
                        paint.setAlpha(255);
                /*
                SerializablePath copyPath = new SerializablePath();
                
                copyPath.setPathPoints(line.getPathPoints());
                copyPath.moveTo(line.getPathPoints().get(0)[0], line.getPathPoints().get(0)[1]);
                ArrayList<float[]> copyOfPathPoints = new ArrayList<>(line.getPathPoints());
                copyOfPathPoints.remove(0);
                for (float[] pointSet : copyOfPathPoints) {
                    copyPath.quadTo(pointSet[0], pointSet[1], pointSet[2], pointSet[3]);
                }
                        canvas.drawPath(copyPath, paint);
                */
                /*
                        ArrayList<float[]> pointsets = line.getPathPoints();
                        float[] oldPointSet = pointsets.get(0);
                        for (float[] pointset : pointsets) {
                            SerializablePath l = new SerializablePath();
                            l.moveTo(oldPointSet[0], oldPointSet[1]);
                            l.quadTo(pointset[0], pointset[1], pointset[2], pointset[3]);
                            oldPointSet = pointset;
                            if (pointset.length == 5) {
                                paint.setStrokeWidth(line.getWidth() * pointset[4]);
                            }
                            canvas.drawPath(l, paint);
                        }
                */
                        canvas.drawPath(line, paint);
                        break;
                    /*
                    } else if (line.getShape().equalsIgnoreCase("ellipse")) {
                        paint.setColor(line.getColor()); // Use the color stored in each path
                        paint.setStrokeWidth(5f);
                        paint.setPathEffect(null);
                        paint.setAlpha(255);
                        float x = line.getPathPoints().get(1)[0];
                        float y = line.getPathPoints().get(1)[1];
                        float centerX = line.getPathPoints().get(0)[0];
                        float centerY = line.getPathPoints().get(0)[1];
                        float radius = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                        canvas.drawCircle(centerX, centerY, radius, paint);
                        break;
                    } else if (line.getShape().equalsIgnoreCase("rectangle")) {
                        paint.setColor(line.getColor()); // Use the color stored in each path
                        paint.setStrokeWidth(5f);
                        paint.setPathEffect(null);
                        paint.setAlpha(255);
                        float left = line.getPathPoints().get(0)[0];
                        float top = line.getPathPoints().get(0)[1];
                        float right = line.getPathPoints().get(1)[0];
                        float bottom = line.getPathPoints().get(1)[1];
                        canvas.drawRect(new RectF(left, top, right, bottom), paint);
                        break;
                    }
                    */
                case TOOL_HIGHLIGHTER:
                    paint.setColor(line.getColor()); // Use the color stored in each path
                    paint.setStrokeWidth(line.getWidth() * 2);
                    paint.setPathEffect(null);
                    paint.setAlpha(100);
                    canvas.drawPath(line, paint);
                    break;
                case TOOL_LASSO:
                    paint.setARGB(100, 100, 100, 100);
                    paint.setPathEffect(new DashPathEffect(new float[] {10f,20f}, 0f));
                    paint.setStrokeWidth(5f);
                    canvas.drawPath(line, paint);
                    break;
            }
        }
        
        if (currentTool == TOOL_LASER && showLaser) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(15f);
            canvas.drawPoint(currentPath.getPathPoints().get(currentPath.getPathPoints().size()-1)[0], currentPath.getPathPoints().get(currentPath.getPathPoints().size()-1)[1], paint);
        }

        if (showErasingCircle) {
            erasingCirclePaint.setColor(Color.RED);
            erasingCirclePaint.setAlpha(50);
            canvas.drawCircle(currentX, currentY, erasingCircleRadius, erasingCirclePaint);
        } else if (showErasingCirclePreview && setting_eraser_preview && (currentPath == null || currentPath.getTool() != TOOL_LASSO)) {
            erasingCirclePaint.setColor(Color.GRAY);
            erasingCirclePaint.setAlpha(50);
            canvas.drawCircle(currentX, currentY, erasingCircleRadius, erasingCirclePaint);
        } else if (showPenPreview && setting_pen_preview && (currentPath == null || currentPath.getTool() != TOOL_LASSO)) {
            switch (currentTool) {
                case TOOL_PATH:
                case TOOL_PEN:
                    paint.setColor(currentColor);
                    paint.setStrokeWidth(currentStrokeWidth);
                    paint.setPathEffect(null);
                    paint.setAlpha(255);
                    canvas.drawPoint(currentX, currentY, paint);
                    break;
                case TOOL_HIGHLIGHTER:
                    paint.setColor(currentColor);
                    paint.setStrokeWidth(currentStrokeWidth * 2);
                    paint.setPathEffect(null);
                    paint.setAlpha(100);
                    canvas.drawPoint(currentX, currentY, paint);
                    break;
            }
        }
    }

    public void changeColor(int color) {
        currentColor = color; // Set the current color for future paths
    }
    
    public void changeTool(int tool) {
        if (currentPath != null && currentPath.getTool() == TOOL_LASSO) {
            currentPath = null;
            invalidate();
        }

        currentPoint = null;
        lastPoint = null;
        currentTool = tool;
    }
    
    public void changeStrokeWidth(int width) {
        currentStrokeWidth = width;
    }
    
    public void setTemplate(int background) {
        currentBackground = background;
        invalidate();
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            if (event.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY || currentTool == TOOL_ERASER) {
                currentX = event.getX();
                currentY = event.getY();
                showErasingCirclePreview = true;
                invalidate();
                hover_handler.postDelayed(
                    () -> {
                        showErasingCirclePreview = false;
                        showPenPreview = false;
                        penPreviewHover = false;
                        invalidate();
                    },
                100);
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        hover_handler.postDelayed(
                            () -> {
                                penPreviewHover = true;

                                hover_handler.postDelayed(
                                        () -> {
                                            showErasingCirclePreview = false;
                                            showPenPreview = false;
                                            penPreviewHover = false;
                                            invalidate();
                                        },
                                        100);
                            },
                        500); //show preview after 500ms
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        if (penPreviewHover) {
                            currentX = event.getX();
                            currentY = event.getY();
                            showPenPreview = true;
                            invalidate();
                            hover_handler.removeCallbacksAndMessages(null);
                            hover_handler.postDelayed(
                                () -> {
                                    showErasingCirclePreview = false;
                                    showPenPreview = false;
                                    penPreviewHover = false;
                                    invalidate();
                                },
                            100); //remove preview when pen exited the screen and doesn't hover anymore
                        }
                        break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        long t = System.currentTimeMillis();
        
        showPenPreview = false;
        penPreviewHover = false;
        showErasingCirclePreview = false;
        hover_handler.removeCallbacksAndMessages(null);

        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            if (event.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY || currentTool == TOOL_ERASER) {
                // Erase logic goes here
                eraseLine(x, y);
                showErasingCircle = true;
                currentX = x;
                currentY = y;
                activeLine = true;
                handler2.removeCallbacksAndMessages(null);
                handler2.postDelayed(() -> activeLine = false, 1000);
            } else {
                showErasingCircle = false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        changes = true;
                        activeLine = true;
                        handler2.removeCallbacksAndMessages(null);
                    
                        startX = x;
                        startY = y;
                        if (isTouchOnLasso(x, y) && currentPath.getTool() == TOOL_LASSO) {
                            isDragging = true;
                            initialTouchX = x;
                            initialTouchY = y;
                        } else {
                            if (currentTool != TOOL_PATH) { //if ((currentPath != null && currentPath.getTool() != TOOL_PATH) || (currentPath == null)) {
                                currentPath = new SerializablePath();
                                currentPath.setColor(currentColor); // Set the color of the new path
                                currentPath.setTool(currentTool);
                                currentPath.setWidth(currentStrokeWidth);
                                currentPath.moveTo(x, y);
                                currentPath.addPathPoints(new float[]{x, y});
                                //currentPath.addPathPoints(new float[]{prevX, prevY, (x + prevX) / 2, (y + prevY) / 2});
                                //lastMoveTime = System.currentTimeMillis();
                            } else {
                                if (currentPoint == null && lastPoint == null) {
                                    currentPath = new SerializablePath();
                                    currentPath.setColor(currentColor); // Set the color of the new path
                                    currentPath.setWidth(currentStrokeWidth);
                                    currentPath.setTool(currentTool);
                                    //Toast.makeText(getContext(), "new line started", Toast.LENGTH_SHORT).show();
                                    currentPoint = new PointF(x, y);
                                } else if (currentPoint == null && lastPoint != null && currentPath.getTool() == TOOL_PATH) {
                                    currentPoint = new PointF(x, y);
                                    currentPoint.set(x, y);
                                } else {
                                    currentPoint.set(x, y);
                                }
                                invalidate();
                            }
                            /*
                            if (currentTool != TOOL_LASER && currentTool != TOOL_PATH) {
                                strokeBuilder = Ink.Stroke.builder();
                                inkBuilder = Ink.builder();
                                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                            }
                            */
                            if (currentTool != TOOL_LASSO && currentTool != TOOL_LASER && currentTool != TOOL_HIGHLIGHTER) {
                                lines.add(currentPath);
                            } else if (currentTool == TOOL_HIGHLIGHTER) {
                                lines.add(0, currentPath);
                            } else if (currentTool == TOOL_LASER) {
                                showLaser = true;
                            }
                        }

                        prevX = x;
                        prevY = y;
                        notifyDrawingChanged();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        changes = true;
                        activeLine = true;
                        handler2.removeCallbacksAndMessages(null);
                        if (!isDragging) {
                            if (currentTool == TOOL_PATH && currentPath != null && currentPath.getTool() == TOOL_PATH) {
                            /*
                                currentPath.removeLastPoint();
                            //currentPath.removeLastPoint();
                            //currentPath.removeLastPoint();
                                //currentPath.removeLastPoint();
                                currentPath.quadTo(prevX, prevY, (x + prevX) / 2, (y + prevY) / 2);
                                currentPath.addPathPoints(new float[]{prevX, prevY, (x + prevX) / 2, (y + prevY) / 2});
                            ///
                                ArrayList<float[]> currentPathPoints = currentPath.getPathPoints();
                                currentPath = new SerializablePath();
                                currentPath.setColor(currentColor); // Set the color of the new path
                                currentPath.setTool(currentTool);
                                currentPath.setPathPoints(currentPathPoints);
                                currentPath.removeLastPoint();
                                currentPath.addPathPoints(new float[]{prevX, prevY, (x + prevX) / 2, (y + prevY) / 2});
                                currentPath.loadPoints();
                                invalidate();
                            */
                                if (currentPoint == null) {
                                    currentPoint = new PointF(x, y);
                                } else {
                                    currentPoint.set(x, y);
                                }
                                invalidate();
                            } else {
                                currentPath.quadTo(prevX, prevY, (x + prevX) / 2, (y + prevY) / 2);
                                currentPath.addPathPoints(new float[]{prevX, prevY, (x + prevX) / 2, (y + prevY) / 2, event.getAxisValue(MotionEvent.AXIS_PRESSURE)});
                            }
                        
                            //Toast.makeText(getContext(), "pressure: " + event.getAxisValue(MotionEvent.AXIS_PRESSURE), Toast.LENGTH_SHORT).show();
                            
                            // Pen movement check
                            if (currentTool == TOOL_PEN || currentTool == TOOL_HIGHLIGHTER) {
                                long currentTime = System.currentTimeMillis();
                                long timeDiff = currentTime - lastMoveTimestamp;
                                float distance = (float) Math.sqrt(Math.pow(prevX - x, 2) + Math.pow(prevY - y, 2));
                                float velocity = MOVE_THRESHOLD;
                                if (lastMoveTimestamp != 0) {
                                    velocity = (distance / timeDiff) * 1000; // velocity in pixels per second
                                }
                            
                                //Toast.makeText(getContext(), "speed: " + String.valueOf(velocity), Toast.LENGTH_SHORT).show();
                            
                                if (velocity < MOVE_THRESHOLD && !isHolding) {
                                    isHolding = true;
                                    lastMoveTime = currentTime;
                                } else if (!(velocity < MOVE_THRESHOLD) && isHolding) {
                                    //Toast.makeText(getContext(), "too fast: " + String.valueOf(velocity), Toast.LENGTH_SHORT).show();
                                    isHolding = false;
                                    //lastMoveTime = 0;
                                }
                            
                                if (isHolding || shapeCorrected) {
                                    long holdTime = System.currentTimeMillis() - lastMoveTime;
                                    //Toast.makeText(getContext(), "holding time: " + String.valueOf(holdTime), Toast.LENGTH_SHORT).show();
                                    if (holdTime >= HOLD_THRESHOLD || shapeCorrected) {
                                        //Toast.makeText(getContext(), "correcting", Toast.LENGTH_SHORT).show();
                                        shapeCorrected = true;
                                        correctShape("arrow", true);
                                    }
                            
                                }
                            
                                prevX = x;
                                prevY = y;
                                lastMoveTimestamp = currentTime;
                            }
                            /*
                            if (!isHolding && !shapeCorrected && currentTool != TOOL_LASER) {
                                // Calculate time difference
                                long currentTime = System.currentTimeMillis();
                                long timeDiff = currentTime - lastMoveTimestamp;
                            
                                // Calculate movement velocity (distance over time)
                                float distance = (float) Math.sqrt(Math.pow(prevX - x, 2) + Math.pow(prevY - y, 2));
                                float velocity = MOVE_THRESHOLD;
                                if (lastMoveTimestamp != 0) {
                                    velocity = (distance / timeDiff) * 1000; // velocity in pixels per second
                                }
                                                        Toast.makeText(getContext(), "speed: " + String.valueOf(velocity), Toast.LENGTH_SHORT).show();
                            
                                if (velocity < MOVE_THRESHOLD) {
                                    // Start hold detection if the pen moves slowly enough
                                    isHolding = true;
                                    lastMoveTime = currentTime;
                                } else {
                                    // Reset holding state if movement is detected
                                    isHolding = false;
                                }
                            
                                // Update previous point and timestamp
                                prevX = x;
                                prevY = y;
                                lastMoveTimestamp = currentTime;
                                
                            } else if (isHolding && currentTool != TOOL_LASSO && currentTool != TOOL_PATH) {
                                // Same logic for checking hold duration
                            
                                long holdTime = System.currentTimeMillis() - lastMoveTime;
                            
                                if (holdTime >= HOLD_THRESHOLD) {
                                    shapeCorrected = true;
                                    lastMoveTime = 0;
                            
                                    // Call shape correction logic
                                    correctShape("arrow", true);
                            
                                } else {
                                    // Pen is still moving, continue building the stroke
                                    if (strokeBuilder != null) {
                                        strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                                    }
                                }
                            }
                        */
                                                    
                        } else {
                            float deltaX = x - initialTouchX;
                            float deltaY = y - initialTouchY;
                            moveSelectedPaths(deltaX, deltaY);
                            initialTouchX = x;
                            initialTouchY = y;
                            invalidate(); // Redraw the canvas
                        }
                        prevX = x;
                        prevY = y;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (currentTool != TOOL_LASSO) {
                            undoStack.add(new Action(Action.DRAW, currentPath));
                            redoStack.clear(); // Clear the redo stack when a new action is performed
                            /*
                            if (strokeBuilder != null) {
                                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                                inkBuilder.addStroke(strokeBuilder.build());
                                strokeBuilder = null;
                                Ink ink = inkBuilder.build();
                            }
                            */
                        } else if (currentTool == TOOL_LASSO && !isDragging) {
                            currentPath.close();
                            selectedPaths = getPathsInsideLasso();
                        }
                    
                        if (currentTool == TOOL_PATH) {
                            if (currentPoint == null) {
                                currentPoint = new PointF(x, y);
                            } else {
                                currentPoint.set(x, y);
                            }
                        
                            if (lastPoint != null) {
                                currentPath.quadTo(lastPoint.x, lastPoint.y, (x + lastPoint.x) / 2, (y + lastPoint.y) / 2);
                                currentPath.addPathPoints(new float[]{lastPoint.x, lastPoint.y, (x + lastPoint.x) / 2, (y + lastPoint.y) / 2});
                            } else {
                                currentPath.moveTo(x, y);
                                currentPath.addPathPoints(new float[]{x, y});
                            }
                            invalidate();
                            lastPoint = new PointF(currentPoint.x, currentPoint.y);
                            currentPoint = null;
                        }
                    
                        showLaser = false;
                        shapeCorrected = false;
                        isDragging = false;
                        isHolding = false;
                        currentShape = null;
                        handler2.postDelayed(() -> activeLine = false, 1000);
                        break;
                }
            }

            invalidate(); // Redraw the canvas
        } else if(selectedPaths != null && currentPath != null && currentPath.getTool() == TOOL_LASSO) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (isTouchOnLasso(x, y)) {
                        isDragging = true;
                        initialTouchX = x;
                        initialTouchY = y;
                    }
                    /*
                    for (SerializablePath path : selectedPaths) {
                            path.setColor(currentColor);
                        }
                    */
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float deltaX = x - initialTouchX;
                        float deltaY = y - initialTouchY;
                        moveSelectedPaths(deltaX, deltaY);
                        initialTouchX = x;
                        initialTouchY = y;
                        invalidate(); // Redraw the canvas
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    isDragging = false;
                    break;
            }

        } else if (currentPath != null && isPointInsideLasso(currentPath, x, y)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = currentTime - lastMoveTimestamp;
                    float distance = (float) Math.sqrt(Math.pow(prevX - x, 2) + Math.pow(prevY - y, 2));
                    float velocity = MOVE_THRESHOLD;
                    if (lastMoveTimestamp != 0) {
                        velocity = (distance / timeDiff) * 1000; // velocity in pixels per second
                    }

                    if (velocity < MOVE_THRESHOLD && !isHolding) {
                        isHolding = true;
                        lastMoveTime = currentTime;
                    } else if (!(velocity < MOVE_THRESHOLD) && isHolding) {
                        isHolding = false;
                        //lastMoveTime = 0;
                    }

                    if (isHolding) {
                        long holdTime = System.currentTimeMillis() - lastMoveTime;
                        if (holdTime >= HOLD_THRESHOLD) {
                            currentPath.setTool(TOOL_LASSO);
                            lines.remove(currentPath);
                            currentPath.close();
                            selectedPaths = getPathsInsideLasso();
                            invalidate();

                            isHolding = false;
                        }

                    }

                    prevX = x;
                    prevY = y;
                    lastMoveTimestamp = currentTime;
                    break;

                case MotionEvent.ACTION_UP:
                    isHolding = false;
            }
        }

        return true;
    }

    private void detectShape(Ink ink) {
        // Specify the recognition model for a language
        DigitalInkRecognitionModelIdentifier modelIdentifier;
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("zxx-Zsym-x-shapes"); // zxx-Zsym-x-autodraw
            DigitalInkRecognitionModel model =
                DigitalInkRecognitionModel.builder(modelIdentifier).build();

        // Get a recognizer for the language
        DigitalInkRecognizer recognizer =
                DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build());
        
        recognizer.recognize(ink)
            .addOnSuccessListener(
                // `result` contains the recognizer's answers as a RecognitionResult.
                // Logs the text from the top candidate.
                result -> correctShape(result.getCandidates().get(0).getText(), true))
            .addOnFailureListener(
                e -> Toast.makeText(getContext(), "Error during recognition: " + e, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(getContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void correctShape(String shape, boolean firstCall) {
        currentShape = shape;
        float x = prevX;
        float y = prevY;
        if (shape.equalsIgnoreCase("arrow")) {
            //Toast.makeText(getContext(), "arrow", Toast.LENGTH_SHORT).show();
            //float[] startPoints = currentPath.getPathPoints().get(0);
                
            if (currentTool == TOOL_HIGHLIGHTER) {
                lines.remove(0);
            } else{
                lines.remove(lines.size() - 1);
            }
            
            currentPath = new SerializablePath();
            currentPath.setColor(currentColor); // Set the color of the new path
            currentPath.setTool(currentTool);
            currentPath.setWidth(currentStrokeWidth);
            currentPath.setShape(shape);
            currentPath.moveTo(startX, startY);
            currentPath.addPathPoints(new float[]{startX, startY});
            if (currentTool == TOOL_HIGHLIGHTER) {
                lines.add(0, currentPath);
            } else {
                lines.add(currentPath);
            }

            currentPath.lineTo(x, y);
            currentPath.addPathPoints(new float[]{x, y});
            //currentPath.loadPathPointsAsLineTo();
            
            //currentPath = null;
            invalidate();
        } else if (shape.equalsIgnoreCase("ellipse")) {
            if (firstCall) {
                List<float[]> points = currentPath.getPathPoints();

                // Initialize variables for extremes
                float minX = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE;
                float minY = Float.MAX_VALUE;
                float maxY = Float.MIN_VALUE;

                // Iterate through all points
                for (float[] point : points) {
                    if (point[0] < minX) minX = point[0];
                    if (point[0] > maxX) maxX = point[0];
                    if (point[1] < minY) minY = point[1];
                    if (point[1] > maxY) maxY = point[1];
                }

                Toast.makeText(getContext(), "minX: " + minX + ", maxX: " + maxX, Toast.LENGTH_SHORT).show();
                
                float centerX = (minX + maxX) / 2;
                float centerY = (minY + maxY) / 2;
                
                lines.remove(lines.size() - 1);
                
                currentPath = new SerializablePath();
                currentPath.setColor(currentColor); // Set the color of the new path
                currentPath.setTool(currentTool);
                currentPath.setShape(shape);
                //currentPath.moveTo(centerX, centerY);
                currentPath.addPathPoints(new float[]{centerX, centerY});
                lines.add(currentPath);
    
                //currentPath.lineTo(x, y);
                currentPath.addPathPoints(new float[]{x, y});
                
                invalidate();
                
                //float distance = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            } else {
                //Toast.makeText(getContext(), "editing circle", Toast.LENGTH_SHORT).show();
                currentPath.removeLastPoint();
                currentPath.removeLastPoint();
                currentPath.addPathPoints(new float[]{x, y});
                invalidate();
            }
        } else if (shape.equalsIgnoreCase("rectangle") || shape.equalsIgnoreCase("triangle")) {
            /*
            if (firstCall) {
                List<float[]> points = currentPath.getPathPoints();

                // Initialize variables for extremes
                float minX = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE;
                float minY = Float.MAX_VALUE;
                float maxY = Float.MIN_VALUE;

                // Iterate through all points
                for (float[] point : points) {
                    if (point[0] < minX) minX = point[0];
                    if (point[0] > maxX) maxX = point[0];
                    if (point[1] < minY) minY = point[1];
                    if (point[1] > maxY) maxY = point[1];
                }

                //Toast.makeText(getContext(), "minX: " + minX + ", maxX: " + maxX, Toast.LENGTH_SHORT).show();
                
                float centerX = (minX + maxX) / 2;
                float centerY = (minY + maxY) / 2;
                
                if (y < centerY) currentRectTop = true;
                if (x < centerX) currentRectLeft = true;
                if (x > centerX) currentRectRight = true;
                if (y > centerY) currentRectBottom = true;
                
                ////
                float left = 0;
                float top = 0;
                float right = 0;
                float bottom = 0;
                
                if (currentRectTop) top = y;
                if (currentRectLeft) left = x;
                if (currentRectRight) right = x;
                if (currentRectBottom) bottom = y;

                if (top == 0) top = maxY;
                if (left == 0) left = maxX;
                if (right == 0) right = minX;
                if (bottom == 0) bottom = minY;
                
                currentPath = new SerializablePath();
                currentPath.setColor(currentColor); // Set the color of the new path
                currentPath.setTool(currentTool);
                currentPath.setShape(shape);
                //currentPath.moveTo(centerX, centerY);
                currentPath.addPathPoints(new float[]{left, top});
                lines.add(currentPath);
    
                //currentPath.lineTo(x, y);
                currentPath.addPathPoints(new float[]{right, bottom});
                
                invalidate();
            } else {
                currentPath.removeLastPoint();
                currentPath.removeLastPoint();
                currentPath.addPathPoints(new float[]{x, y});
                invalidate();
            }
            */
        } else {
            //Toast.makeText(getContext(), "shape: " + shape, Toast.LENGTH_SHORT).show();
            currentShape = "arrow";
        }
    }

    private boolean isTouchOnLasso(float x, float y) {
        if (currentPath != null && currentPath.getTool() == TOOL_LASSO) {
            RectF bounds = new RectF();
            currentPath.computeBounds(bounds, true);
            return bounds.contains(x, y);
        }
        return false;
    }
    
    private void moveSelectedPaths(float deltaX, float deltaY) {
        if (selectedPaths != null) {
            for (SerializablePath path : selectedPaths) {
                //path.updatePathPoints(deltaX, deltaY);
                path.setOffset(deltaX, deltaY);
            }
        }
        currentPath.offset(deltaX, deltaY);
    }
    
    public List<SerializablePath> getPathsInsideLasso() {
        List<SerializablePath> selectedPaths = new ArrayList<>();
        
        // Get the bounding box of the lasso path
        RectF lassoBounds = new RectF();
        currentPath.computeBounds(lassoBounds, true);
        
        for (SerializablePath line : lines) {
            // Get the bounding box of the current line
            RectF lineBounds = new RectF();
            line.computeBounds(lineBounds, true);
            
            // Quick elimination test: check if the bounding boxes intersect
            if (RectF.intersects(lassoBounds, lineBounds)) {
                // Further check if the line path intersects the lasso path
                //if (isPathIntersecting(currentPath, line)) {
                    //selectedPaths.add(line);
                //}
                if (line.getPathPoints().size() <= 2) {
                    float x = line.getPathPoints().get(0)[0];
                    float y = line.getPathPoints().get(0)[1];
                    if (isPointInsideLasso(currentPath, x, y)) {
                        selectedPaths.add(line);
                    }
                } else {
                    if ((isPathIntersecting(currentPath, line, false) && !setting_need_lasso_fully_intersecting) ||
                            (isPathIntersecting(currentPath, line, true) && setting_need_lasso_fully_intersecting)) {
                        selectedPaths.add(line);
                    }
                }
            }
        }
        
        return selectedPaths;
    }

    private boolean isPathIntersecting(SerializablePath lassoPath, SerializablePath targetPath, boolean needFullIntersect) {
        // Convert lassoPath to Region
        Region lassoRegion = new Region();
        RectF lassoBounds = new RectF();
        lassoPath.computeBounds(lassoBounds, true);
        lassoRegion.setPath(lassoPath, new Region((int) lassoBounds.left, (int) lassoBounds.top, (int) lassoBounds.right, (int) lassoBounds.bottom));

        // Sample points along the target path and check if all are inside lassoRegion
        PathMeasure pm = new PathMeasure(targetPath, false);
        float[] pos = new float[2];
        float length = pm.getLength();
        int sampleCount = 20; // Increase for more accuracy

        for (int i = 0; i <= sampleCount; i++) {
            float distance = (i / (float) sampleCount) * length;
            if (pm.getPosTan(distance, pos, null)) {
                if (lassoRegion.contains((int) pos[0], (int) pos[1]) && !needFullIntersect) {
                    return true; // A point is inside
                }

                if (!lassoRegion.contains((int) pos[0], (int) pos[1]) && needFullIntersect) {
                    return false; // A point is outside
                }
            }
        }
        return needFullIntersect ? true : false;
    }

    private boolean isPointInsideLasso(SerializablePath lassoPath, float x, float y) {
        Region region = new Region();
        RectF bounds = new RectF();
        lassoPath.computeBounds(bounds, true);

        // Ensure the path is closed, or Region won't work properly
        Path pathCopy = new Path(lassoPath); // clone to avoid modifying original
        pathCopy.close(); // important

        // Set region for the filled path
        region.setPath(pathCopy, new Region(
                (int) bounds.left, (int) bounds.top,
                (int) bounds.right, (int) bounds.bottom
        ));

        return region.contains((int) x, (int) y);
    }

    private void eraseLine(float x, float y) {
        float touchRadius = 20f;
        float touchRadiusSquared =
                touchRadius * touchRadius; // Use squared value for distance comparison

        for (int i = lines.size() - 1; i >= 0; i--) {
            SerializablePath line = lines.get(i);
            RectF bounds = new RectF();
            line.computeBounds(bounds, true);

            // Create a bounding box around the path with a threshold
            bounds.inset(-touchRadius, -touchRadius);

            if (bounds.contains(x, y)) {
                // Check if the touch point is close to any point on the path
                if (isPointCloseToLine(line, x, y, touchRadiusSquared)) {
                    undoStack.add(new Action(Action.ERASE, lines.remove(i)));
                    redoStack.clear(); // Clear the redo stack when a new action is performed
                    invalidate(); // Redraw the canvas without the erased line
                    changes = true;
                    notifyDrawingChanged(); // Notify listener
                    break; // Exit loop after erasing one line to maintain performance
                }
            }
        }
        handler.postDelayed(
                () -> {
                    showErasingCircle = false;
                    invalidate();
                },
                100);
    }

    // Helper method to check if a point is close to a line by sampling points
    private boolean isPointCloseToLine(Path line, float x, float y, float touchRadiusSquared) {
        PathMeasure pathMeasure = new PathMeasure(line, false);
        float pathLength = pathMeasure.getLength();
        float[] point = new float[2];

        // Sample the path at regular intervals
        for (float distance = 0; distance < pathLength; distance += 5) {
            pathMeasure.getPosTan(distance, point, null);

            // Check the squared distance between the sampled point and the touch coordinates
            float dx = point[0] - x;
            float dy = point[1] - y;
            if (dx * dx + dy * dy < touchRadiusSquared) {
                return true;
            }
        }
        return false;
    }
    
    public List<SerializablePath> getRawData() {
        return lines;
    }

    public boolean getChanges() {
        return changes;
    }
    public boolean getAttachmentChanges() { return attachmentChanges; }

    public boolean getActiveLine() {
        return activeLine;
    }

    public String getData() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        changes = false;

        ArrayList<SerializablePath> copyOfLines = new ArrayList<SerializablePath>();
        for (SerializablePath path : lines) {
            SerializablePath copyOfPath = new SerializablePath(path);
            copyOfPath.convertToDp(getContext());
            copyOfLines.add(copyOfPath);
        }

        return gson.toJson(copyOfLines);
    }

    public void setData(String data) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        Type listType = new TypeToken<List<SerializablePath>>() {}.getType();
        lines = gson.fromJson(data, listType);

        for (SerializablePath path : lines) {
            /*
            try {
                String shape = path.getShape();
                Toast.makeText(getContext(), "shape: " + shape, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
            String shape = path.getShape();
            if (shape == null) {
                path.loadPathPointsAsQuadTo();
            } else if (shape.equalsIgnoreCase("arrow")) {
                path.loadPathPointsAsLineTo();
                //Toast.makeText(getContext(), "arrow found", Toast.LENGTH_SHORT).show();
            }
            */
            path.convertToPx(getContext());
            path.loadPoints(getContext());
            float[] offset = path.getOffset();
            //Toast.makeText(getContext(), String.valueOf(offset[0]) + ", " + String.valueOf(offset[1]), Toast.LENGTH_SHORT).show();
            
        }

        invalidate();
    }
    
    public interface DrawingListener {
        void onDrawingChanged(DrawingView drawingView);
    }
    
    public void setDrawingListener(DrawingListener listener) {
        this.drawingListener = listener;
    }
    
    private void notifyDrawingChanged() {
        if (drawingListener != null) {
            drawingListener.onDrawingChanged(this); // Pass the current DrawingView instance
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Action lastAction = undoStack.remove(undoStack.size() - 1);
            redoStack.add(lastAction);

            if (lastAction.getType() == Action.DRAW) {
                lines.remove(lastAction.getPath());
            } else if (lastAction.getType() == Action.ERASE) {
                lines.add(lastAction.getPath());
            }

            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Action lastUndoneAction = redoStack.remove(redoStack.size() - 1);
            undoStack.add(lastUndoneAction);

            if (lastUndoneAction.getType() == Action.DRAW) {
                lines.add(lastUndoneAction.getPath());
            } else if (lastUndoneAction.getType() == Action.ERASE) {
                lines.remove(lastUndoneAction.getPath());
            }

            invalidate();
        }
    }
}