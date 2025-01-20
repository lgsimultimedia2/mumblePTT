package com.jio.jiotalkie.dataclass;

public class HamburgerItemState {
    private String title;
    private  int id;
    private int icon;

    public HamburgerItemState(int id, String title, int icon) {
        this.title = title;
        this.id = id;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }


}
