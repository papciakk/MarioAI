package pl.edu.agh.ai.nes;

import java.awt.*;

public enum TileType {
    EMPTY(0, new Color(0x383838)),
    OBSTACLE(1, new Color(0x0E07FF)),
    REWARD(2, new Color(0xFFFF0C)),
    MARIO(3, new Color(0x69FF02)),
    ENEMY(4, new Color(0xFF3F05)),
    UNKNOWN(5, new Color(0xff00ff));

    private int val;
    private Color color;

    TileType(int val, Color color) {
        this.val = val;
        this.color = color;
    }

    public static Color getColorByVal(int val) {
        TileType[] values = TileType.values();
        return values[val].getColor();
    }

    public int getVal() {
        return val;
    }

    public Color getColor() {
        return color;
    }
}
