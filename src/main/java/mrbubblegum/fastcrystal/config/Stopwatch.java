package mrbubblegum.fastcrystal.config;

public final class Stopwatch {

    private long time;

    public Stopwatch() {
        time = -1;
    }

    public boolean passed(double ms) {
        return System.currentTimeMillis() - this.time >= ms;
    }

    public void reset() {
        this.time = System.currentTimeMillis();
    }

    @Deprecated
    public long getTime() {
        return time;
    }

}