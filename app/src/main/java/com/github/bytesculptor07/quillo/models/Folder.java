package com.github.bytesculptor07.quillo.models;
import com.google.gson.Gson;

public class Folder {
    private long id;
    private String name;
    private int icon;
    private int color;
    private String path;

    // Getter und Setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public String exportToJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    public static Folder importFromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Folder.class);
    }
}