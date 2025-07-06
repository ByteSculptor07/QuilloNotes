package com.github.bytesculptor07.quillo;

public class Action {
    public static final int DRAW = 1;
    public static final int ERASE = 2;

    private int type;
    private SerializablePath path;

    public Action(int type, SerializablePath path) {
        this.type = type;
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public SerializablePath getPath() {
        return path;
    }
}