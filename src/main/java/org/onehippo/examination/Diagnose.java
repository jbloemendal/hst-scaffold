package org.onehippo.examination;

public class Diagnose {

    public enum State {
        OK, INCONSISTENT, UNREFERENCED, REDUNDANT
    }

    private State state;
    private String jcrPath;
    private String filePath;
    private String info;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getJcrPath() {
        return jcrPath;
    }

    public void setJcrPath(String jcrPath) {
        this.jcrPath = jcrPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

}
