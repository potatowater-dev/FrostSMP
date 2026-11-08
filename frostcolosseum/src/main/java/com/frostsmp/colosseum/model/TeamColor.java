package com.frostsmp.colosseum.model;

public enum TeamColor {
    RED,
    BLUE;

    public String display() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
