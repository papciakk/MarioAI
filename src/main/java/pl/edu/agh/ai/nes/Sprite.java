package pl.edu.agh.ai.nes;

public class Sprite {
    private int id;
    private int x;
    private int y;
    private int flags;

    public Sprite(int id, int x, int y, int flags) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.flags = flags;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getFlags() {
        return flags;
    }

    public boolean isVisible() {
        return y < 0xEF;
    }
}
