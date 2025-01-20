package com.jio.jiotalkie.dataclass;

public class DocumentDownloadState {
    private  int position;
    private String filePath;
    private boolean isLeft;
    private boolean isSos;

    public DocumentDownloadState(int position, String filePath, boolean isLeft, boolean isSos) {
        this.position = position;
        this.filePath = filePath;
        this.isLeft = isLeft;
        this.isSos = isSos;
    }

    public boolean isSos() {
        return isSos;
    }

    public void setSos(boolean sos) {
        isSos = sos;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }
}
