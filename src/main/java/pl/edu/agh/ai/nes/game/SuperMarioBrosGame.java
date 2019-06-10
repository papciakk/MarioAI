package pl.edu.agh.ai.nes.game;

import com.grapeshot.halfnes.CPURAM;

public class SuperMarioBrosGame implements Game {
    private CPURAM cpuram;

    private static final int PLAYER_STATE_DYING = 0x0B;
    private static final int PLAYER_STATE_LEFTMOST_OF_SCREEN = 0x00;
    private static final int PLAYER_STATE_NORMAL = 0x08;

    private static final int RIGHT = 0x01;
    private static final int LEFT = 0x02;
    private static final int NOT_ON_SCREEN = 0x00;

    private static final int PLAYER_STATE_ADDR = 0x000E;
    private static final int PLAYER_HORIZONTAL_SPEED_ADDR = 0x0057;
    private static final int PLAYER_FACING_DIRECTION_ADDR = 0x0033;


    public SuperMarioBrosGame(CPURAM cpuram) {
        this.cpuram = cpuram;
    }

    public int getPlayerState() {
        return cpuram.read(PLAYER_STATE_ADDR);
    }

    public boolean isPlayerDying() {
        return getPlayerState() == PLAYER_STATE_DYING;
    }

    public boolean isPlayerLeftmostOfScreen() {
        return getPlayerState() == PLAYER_STATE_LEFTMOST_OF_SCREEN;
    }

    public boolean isPlayerStateNormal() {
        return getPlayerState() == PLAYER_STATE_NORMAL;
    }

    public int getPlayerHorizontalSpeed() {
        return cpuram.read(PLAYER_HORIZONTAL_SPEED_ADDR);
    }

    public boolean isPlayerNotMoving() {
        return getPlayerHorizontalSpeed() == 0x00;
    }

    private int getPlayerFacingDirection() {
        return cpuram.read(PLAYER_FACING_DIRECTION_ADDR);
    }

    public boolean isPlayerFacingLeft() {
        return getPlayerFacingDirection() == LEFT;
    }

    public boolean isPlayerFacingRight() {
        return getPlayerFacingDirection() == RIGHT;
    }

    public boolean isPlayerOnScreen() {
        return getPlayerFacingDirection() != NOT_ON_SCREEN;
    }

    public boolean isPlayerMovingLeft() {
        return !isPlayerNotMoving() && isPlayerFacingLeft();
    }

    public boolean isPlayerMovingRight() {
        return !isPlayerNotMoving() && isPlayerFacingRight();
    }

    public int getFlagpoleCollisionYPos() {
        return cpuram.read(0x70F);
    }

    public boolean isPlayerSlidingDownFlagpole() {
        return cpuram.read(0x01D) == 0x03;
    }

    public boolean isDeathMusicLoaded() {
        return cpuram.read(0x712) == 1;
    }

    public int getGameScore() {
        return cpuram.read(0x7DD) * 1000000 +
                cpuram.read(0x7DE) * 100000 +
                cpuram.read(0x7DF) * 10000 +
                cpuram.read(0x7E0) * 1000 +
                cpuram.read(0x7E1) * 100 +
                cpuram.read(0x7E2) * 10;
    }

    public int getCoins() {
        return cpuram.read(0x7ED) * 10 +
                cpuram.read(0x7EE);
    }

    public int getPlayerXLevel() {
        return cpuram.read(0x06D);
    }

    public int getPlayerXScreen() {
        return cpuram.read(0x086);
    }

    public int getPlayerAbsoluteX() {
        return getPlayerXLevel() * 0x100 + getPlayerXScreen() + 4;
    }

    public int getGameTime() {
        return cpuram.read(0x7F8) * 100 +
                cpuram.read(0x7F9) * 10 +
                cpuram.read(0x7FA);
    }


    public double getScore() {
        return getPlayerAbsoluteX() * 10;
//                + getGameScore() * 0.5;
    }

    public boolean fail() {
        return isPlayerDying() || isDeathMusicLoaded();
    }

    public boolean success() {
        return getFlagpoleCollisionYPos() != 0;
    }
}
