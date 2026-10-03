package com.example.appmgmt.web.view;

import java.util.ArrayList;
import java.util.List;

/** 申込メニューの領域（申込全体／新規申込／契約変更 N）。折りたたみ表示を切り替えられる。 */
public class MenuSection {
    private final String id;
    private final String title;
    private final String stateName;
    private final String badgeStyle;
    private final String description;
    private final boolean expanded;
    private final List<MenuTile> tiles = new ArrayList<>();

    public MenuSection(String id, String title, String stateName, String badgeStyle, String description, boolean expanded) {
        this.id = id;
        this.title = title;
        this.stateName = stateName;
        this.badgeStyle = badgeStyle;
        this.description = description;
        this.expanded = expanded;
    }

    public MenuSection add(MenuTile t) { tiles.add(t); return this; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getStateName() { return stateName; }
    public String getBadgeStyle() { return badgeStyle; }
    public String getDescription() { return description; }
    public boolean isExpanded() { return expanded; }
    public List<MenuTile> getTiles() { return tiles; }
}
