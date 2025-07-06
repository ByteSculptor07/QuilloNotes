package com.github.bytesculptor07.quillo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoteParser {
    public static String extractNoteId(String fileName) {
        /*
        if (fileName == null || !fileName.endsWith(".qdoc")) {
            throw new IllegalArgumentException("Invalid file format");
        }
         */
        // Remove the first 5 characters
        if (fileName.length() <= 6) {
            throw new IllegalArgumentException("File name is too short");
        }
        String trimmedFileName = fileName.substring(6);
    
        // Remove the ".qdoc.<status>" extension
        String baseName = trimmedFileName.substring(0, trimmedFileName.length() - 7);
    
        // Find the last underscore and extract the note ID
        int separatorIndex = baseName.lastIndexOf('_');
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Invalid file naming convention");
        }
    
        return baseName.substring(0, separatorIndex); // Extract note ID
    }

    public static int extractNumber(String fileName) {
        /*
        if (fileName == null || !fileName.endsWith(".qdoc")) {
            throw new IllegalArgumentException("Invalid file format");
        }
         */
        String baseName = fileName.substring(0, fileName.length() - 7); // remove ".qdoc.<staus>"
        int separatorIndex = baseName.lastIndexOf('_');
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("Invalid file naming convention");
        }
        String numberPart = baseName.substring(separatorIndex + 1); // extract number
        return Integer.parseInt(numberPart);
    }

    public static String extractUUID(String path) {
        Pattern pattern = Pattern.compile("([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12})");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null; // UUID not found
    }

    public static int extractStatus(String filename) {
        return Integer.parseInt(filename.substring(filename.length() - 1));
    }
}