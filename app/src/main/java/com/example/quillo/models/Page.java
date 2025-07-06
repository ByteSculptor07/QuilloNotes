package com.github.bytesculptor07.quillo.models;

import com.google.gson.Gson;

public class Page {
    private String uuid;
    private int status;
    private String data;
    private int number;
    private String noteid;
    private String background; // New field
    private String pdf;        // New field
    private int pdfPage;       // New field

    // Getters and setters for all fields
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getNoteId() {
        return noteid;
    }

    public void setNoteId(String noteid) {
        this.noteid = noteid;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    // Getter and setter for background
    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    // Getter and setter for pdf
    public String getPdf() {
        return pdf;
    }

    public void setPdf(String pdf) {
        this.pdf = pdf;
    }

    // Getter and setter for pdfPage
    public int getPdfPage() {
        return pdfPage;
    }

    public void setPdfPage(int pdfPage) {
        this.pdfPage = pdfPage;
    }
    
    public String exportToJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
    
    public static Page importFromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Page.class);
    }
}