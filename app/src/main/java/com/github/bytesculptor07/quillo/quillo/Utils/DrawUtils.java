package com.github.bytesculptor07.quillo.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.github.bytesculptor07.quillo.SerializablePath;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;

import java.util.List;
import android.content.Context;
import android.util.Log;

public class DrawUtils {
    public static final int TOOL_PEN = 0;
    public static final int TOOL_HIGHLIGHTER = 1;
    public static final int TOOL_ERASER = 2;
    public static final int TOOL_LASSO = 3;
    public static final int TOOL_LASER = 4;
    public static final int TOOL_PATH = 5;

    public static final int BACKGROUND_LINES = 8;
    public static final int BACKGROUND_SQUARE = 9;
    public static final int BACKGROUND_EMPTY = 10;
    public static final int BACKGROUND_DOT = 11;
    public static final int BACKGROUND_MUSIC = 12;

    public static Bitmap renderDrawing(List<SerializablePath> rawData, int template, float pageWidth, float pageHeight, Context context) {
        Bitmap combinedBitmap =
                Bitmap.createBitmap(GeneralUtils.dpToPx(context, 2100), GeneralUtils.dpToPx(context, 2970), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(combinedBitmap);
        Paint paint = new Paint();

        if (template != 0) {
            paint.setARGB(100, 100, 100, 100);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setPathEffect(null);

            float width = GeneralUtils.dpToPx(context, 2100);
            float height = GeneralUtils.dpToPx(context, 2970);
            int margin = (int) (width / 8);
            int h;
            switch (template) {
                case BACKGROUND_LINES:
                    int lines_count = 30;
                    int height_step = (int) height / (lines_count + 2);
                    int height_offset = height_step / 2;
                    canvas.drawLine(margin, 0, margin, height, paint);
                    canvas.drawLine(width - margin, 0, width - margin, height, paint);

                    for (int i = 1; i < lines_count + 1; i++) {
                        h = height_step * i + height_offset;
                        canvas.drawLine(0, h, width, h, paint);
                    }
                    break;

                case BACKGROUND_SQUARE:
                    //int square_size = (int) (50 * scale);
                    int square_size = (int) GeneralUtils.dpToPx(context, 50);
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
                    //int dot_distance = (int) (50 * scale);
                    int dot_distance = (int) GeneralUtils.dpToPx(context, 50);
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
                    int step = (int) height / ((count + 1) * 2);
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
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);

        for (SerializablePath line : rawData) {
            paint.setColor(line.getColor());
            //Log.d("Path", String.valueOf(line.getTool()));
            switch (line.getTool()) {
                case TOOL_PEN:
                case TOOL_PATH:
                    paint.setStrokeWidth(line.getWidth());
                    paint.setPathEffect(null);
                    paint.setAlpha(255);
                    break;
                case TOOL_HIGHLIGHTER:
                    paint.setStrokeWidth(line.getWidth() * 2);
                    paint.setPathEffect(null);
                    paint.setAlpha(100);
                    break;
            }
            canvas.drawPath(line, paint);
        }
        return combinedBitmap;
    }
}
