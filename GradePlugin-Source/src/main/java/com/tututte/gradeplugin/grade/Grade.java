package com.tututte.gradeplugin.grade;

import java.util.List;

public class Grade {

    private final String id;
    private String displayName;
    private String prefix;
    private String suffix;
    private String color;
    private int priority;
    private List<String> permissions;
    private Double price; // null = non achetable directement

    public Grade(String id, String displayName, String prefix, String suffix,
                 String color, int priority, List<String> permissions, Double price) {
        this.id = id;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
        this.priority = priority;
        this.permissions = permissions;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getColoredPrefix() {
        return (color != null ? color : "") + (prefix != null ? prefix : "");
    }
}
