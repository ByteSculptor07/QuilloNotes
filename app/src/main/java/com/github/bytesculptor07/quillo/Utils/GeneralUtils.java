package com.github.bytesculptor07.quillo.Utils;

import android.content.Context;

import com.github.bytesculptor07.quillo.R;
import com.github.bytesculptor07.quillo.models.Page;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralUtils {
    public static int[] iconIds = {
            R.drawable.icon_folder_math,
            R.drawable.icon_folder_flask,
            R.drawable.icon_folder_testtube,
            R.drawable.icon_folder_translate,
            R.drawable.icon_folder_globe_east,
            R.drawable.icon_folder_globe,
            R.drawable.icon_folder_basketball,
            R.drawable.icon_folder_gavel,
            R.drawable.icon_folder_bank,
            R.drawable.icon_folder_atom,
            R.drawable.icon_folder_magnet,
            R.drawable.icon_folder_microscope,
            R.drawable.icon_folder_binary,
            R.drawable.icon_folder_piano,
            R.drawable.icon_folder_guitar,
            R.drawable.icon_folder_palette,
            R.drawable.icon_folder_cross,
            R.drawable.icon_folder_church,
            R.drawable.icon_folder_book
    };

    public static int[] colorIds = {
            R.color.folder_blue_dark,
            R.color.folder_red,
            R.color.folder_blue,
            R.color.folder_green,
            R.color.folder_yellow,
            R.color.folder_orange,
            R.color.folder_purple,
            R.color.folder_cyan,
            R.color.folder_magenta,
            R.color.folder_lime,
            R.color.folder_teal,
            R.color.folder_pink,
            R.color.folder_lavender,
            R.color.folder_brown,
            R.color.folder_maroon,
            R.color.folder_olive,
            R.color.folder_navy,
            R.color.folder_turquoise,
            R.color.folder_indigo,
            R.color.folder_violet,
            R.color.folder_mauve,
            R.color.folder_crimson,
            R.color.folder_chartreuse,
            R.color.folder_plum,
            R.color.folder_goldenrod,
            R.color.folder_seagreen,
            R.color.folder_salmon,
            R.color.folder_aquamarine,
            R.color.folder_tomato,
            R.color.folder_slateblue,
            R.color.folder_gray,
            R.color.folder_silver,
            R.color.folder_black
    };
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    public static String getNameOfNoteId(String path) {
        int lastIndex = path.lastIndexOf('/');
        if (lastIndex >= 0 && lastIndex < path.length() - 1) {
            return path.substring(lastIndex + 1);
        }
        return "";
    }

    public static String getPathOfNoteid(String path) {
        int lastIndex = path.lastIndexOf('/');
        if (lastIndex > 0) {
            return path.substring(0, lastIndex + 1);
        }
        return "/";
    }


    public static String getFolderIdOfPath(String path) {
        Pattern pattern = Pattern.compile(".*/(\\d+)\\.qdata");
        Matcher matcher = pattern.matcher(path);

        String id = null;
        if (matcher.matches()) {
            id = matcher.group(1);
        }

        return id;
    }

    public static Page copyPage(Page oldPage) {
        Page newPage = new Page();
        newPage.setBackground(oldPage.getBackground());
        newPage.setStatus(oldPage.getStatus());
        newPage.setPdfPage(oldPage.getPdfPage());
        newPage.setData(oldPage.getData());
        newPage.setUUID(oldPage.getUUID());
        newPage.setPdf(oldPage.getPdf());
        newPage.setNoteId(oldPage.getNoteId());
        newPage.setNumber(oldPage.getNumber());

        return newPage;
    }
}
