/**
 * @author Andrew Hoffman
 */
package com.grapeshot.halfnes;

import com.grapeshot.halfnes.cheats.ActionReplay;
import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.mappers.Mapper;
import com.grapeshot.halfnes.ui.ControllerInterface;
import com.grapeshot.halfnes.ui.FrameLimiterImpl;
import com.grapeshot.halfnes.ui.FrameLimiterInterface;
import com.grapeshot.halfnes.ui.GUIInterface;
import javafx.application.Platform;
import pl.edu.agh.ai.learning.Learning;
import pl.edu.agh.ai.nes.Sprite;
import pl.edu.agh.ai.nes.TileType;
import pl.edu.agh.ai.nes.game.SuperMarioBrosGame;
import pl.edu.agh.ai.nes.game.SuperMarioBrosTiles;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class NES {

    private Learning learning;

    public void saveState(State s) {
        s.runEmulation = runEmulation;
        s.dontSleep = dontSleep;
        s.shutdown = shutdown;
        s.frameStartTime = frameStartTime;
        s.NESframecount = framecount;
        s.frameDoneTime = frameDoneTime;
        s.frameLimiterOn = frameLimiterOn;
        s.curRomPath = curRomPath;
        s.curRomName = curRomName;
    }

    public void loadState(State s) {
        runEmulation = s.runEmulation;
        dontSleep = s.dontSleep;
        shutdown = s.shutdown;
        frameStartTime = s.frameStartTime;
        framecount = s.NESframecount;
        frameDoneTime = s.frameDoneTime;
        frameLimiterOn = s.frameLimiterOn;
        curRomPath = s.curRomPath;
        curRomName = s.curRomName;
    }

    private Mapper mapper;
    private APU apu;
    private CPU cpu;
    private CPURAM cpuram;
    private PPU ppu;
    private GUIInterface gui;
    private ControllerInterface controller1, controller2;
    final public static String VERSION = "062";
    public boolean runEmulation = false;
    private boolean dontSleep = false;
    private boolean shutdown = false;
    public long frameStartTime, framecount, frameDoneTime;
    private boolean frameLimiterOn = true;
    private String curRomPath, curRomName;
    private final FrameLimiterInterface limiter = new FrameLimiterImpl(this, 16639267);
    // Pro Action Replay device
    private ActionReplay actionReplay;

    private Integer[] learningArray;

    public static final int FRAME_WIDTH = 256;
    public static final int FRAME_HEIGHT = 240;

    public NES(GUIInterface gui) {
        if (gui != null) {
            this.gui = gui;
            gui.setNES(this);
            gui.run();
        }
    }

    public CPURAM getCPURAM() {
        return this.cpuram;
    }

    public CPU getCPU() {
        return this.cpu;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public synchronized void runframe() {
        //run cpu, ppu for a whole frame
        ppu.runFrame();
        SuperMarioBrosGame superMarioBrosGame;

        int[] bitmap_ai = ppu.getBitmap_ai();
        BufferedImage nametableView = ppu.getNametableView();

        Integer[][] fullFrameArray = new Integer[FRAME_WIDTH][FRAME_HEIGHT];
        learningArray = new Integer[32 * 30];

        for (int j = 0; j < FRAME_HEIGHT; j++) {
            for (int i = 0; i < FRAME_WIDTH; i++) {
                int pix = bitmap_ai[FRAME_WIDTH * j + i];
                fullFrameArray[i][j] = pix;
            }
        }

        int[] oam = ppu.getOAM();
        ArrayList<Sprite> sprites = new ArrayList<>();
        for (int i = 0; i < oam.length; i += 4) {
            int y = oam[i];
            int id = oam[i + 1];
            int flags = oam[i + 2];
            int x = oam[i + 3];
            Sprite sprite = new Sprite(id, x, y, flags);
            sprites.add(sprite);
        }

        for (Sprite sprite : sprites) {
            if (sprite.isVisible()) {
                SuperMarioBrosTiles t = new SuperMarioBrosTiles();
                TileType tile = t.getSpriteTileById(sprite.getId());

                if (tile != TileType.EMPTY && tile != TileType.UNKNOWN) {
                    for (int ii = 0; ii < 8; ii++) {
                        for (int jj = 0; jj < 8; jj++) {
                            if (sprite.getX() + ii < FRAME_WIDTH && sprite.getY() + jj < FRAME_HEIGHT) {
                                fullFrameArray[sprite.getX() + ii][sprite.getY() + jj] = tile.getVal();
                            }
                        }
                    }
                }
            }
        }

        for (int j = 0; j < 30; j++) {
            for (int i = 0; i < 32; i++) {
                int x_get = i * 8 + 4;
                int y_get = j * 8 + 4;
                Integer pixVal = fullFrameArray[x_get][y_get];
                int rgb = TileType.getColorByVal(pixVal).getRGB();

                learningArray[j * 32 + i] = pixVal;

                for (int jj = 0; jj < 8; jj++) {
                    for (int ii = 0; ii < 8; ii++) {
                        nametableView.setRGB(i * 8 + ii, j * 8 + jj, rgb);
                    }
                }
            }
        }

//        for (Integer i : learningArray) {
//            System.out.print(i);
//        }
//        System.out.println();

        //do end of frame stuff
//        dontSleep = apu.bufferHasLessThan(1000);
        //if the audio buffer is completely drained, don't sleep for this frame
        //this is to prevent the emulator from getting stuck sleeping too much
        //on a slow system or when the audio buffer runs dry.

//        apu.finishframe();
        cpu.modcycles();

//        if (framecount == 13 * 60) {
//            cpu.startLog();
//            System.err.println("log on");
//        }
        //render the frame
        ppu.renderFrame(gui);
        if ((framecount & 2047) == 0) {
            //save sram every 30 seconds or so
            saveSRAM(true);
        }
        ++framecount;
        //System.err.println(framecount);
    }

    public void setControllers(ControllerInterface controller1, ControllerInterface controller2) {
        this.controller1 = controller1;
        this.controller2 = controller2;
    }

    public void toggleFrameLimiter() {
        frameLimiterOn = !frameLimiterOn;
    }

    public synchronized void loadROM(final String filename) {
        loadROM(filename, null);
    }

    public synchronized void loadROM(final String filename, Integer initialPC) {
        runEmulation = false;
        if (FileUtils.exists(filename)
                && (FileUtils.getExtension(filename).equalsIgnoreCase(".nes")
                || FileUtils.getExtension(filename).equalsIgnoreCase(".nsf"))) {
            Mapper newmapper;
            try {
                final ROMLoader loader = new ROMLoader(filename);
                loader.parseHeader();
                newmapper = Mapper.getCorrectMapper(loader);
                newmapper.setLoader(loader);
                newmapper.loadrom();
            } catch (BadMapperException e) {
                gui.messageBox("Error Loading File: ROM is"
                        + " corrupted or uses an unsupported mapper.\n" + e.getMessage());
                return;
            } catch (Exception e) {
                gui.messageBox("Error Loading File: ROM is"
                        + " corrupted or uses an unsupported mapper.\n" + e.toString() + e.getMessage());
                e.printStackTrace();
                return;
            }
            if (apu != null) {
                //if rom already running save its sram before closing
                apu.destroy();
                saveSRAM(false);
                //also get rid of mapper etc.
                mapper.destroy();
                cpu = null;
                cpuram = null;
                ppu = null;
            }
            mapper = newmapper;
            //now some annoying getting of all the references where they belong
            cpuram = mapper.getCPURAM();
            actionReplay = new ActionReplay(cpuram);
            cpu = mapper.cpu;
            ppu = mapper.ppu;
            apu = new APU(this, cpu, cpuram);
            cpuram.setAPU(apu);
            cpuram.setPPU(ppu);
            curRomPath = filename;
            curRomName = FileUtils.getFilenamefromPath(filename);

            framecount = 0;
            //if savestate exists, load it
            if (mapper.hasSRAM()) {
                loadSRAM();
            }
            //and start emulation
            cpu.init(initialPC);
            mapper.init();
            setParameters();
            runEmulation = true;
        } else {
            gui.messageBox("Could not load file:\nFile " + filename + "\n"
                    + "does not exist or is not a valid NES game.");
        }
    }

    private void saveSRAM(final boolean async) {
        if (mapper != null && mapper.hasSRAM() && mapper.supportsSaves()) {
            if (async) {
                FileUtils.asyncwritetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            } else {
                FileUtils.writetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            }
        }
    }

    private void loadSRAM() {
        final String name = FileUtils.stripExtension(curRomPath) + ".sav";
        if (FileUtils.exists(name) && mapper.supportsSaves()) {
            mapper.setPRGRAM(FileUtils.readfromfile(name));
        }

    }

    public void quit() {
        //save SRAM and quit
        //should wait for any save sram workers to be done before here
        if (cpu != null && curRomPath != null) {
            runEmulation = false;
            saveSRAM(false);
        }
        //there might be some subtle threading bug with saving?
        //System.Exit is very dirty and does NOT let the delete on exit handler
        //fire so the natives stick around...
        shutdown = true;
        Platform.exit();
    }

    public synchronized void reset() {
        if (cpu != null) {
            mapper.reset();
            cpu.reset();
            runEmulation = true;
            apu.pause();
            apu.resume();
        }
        //reset frame counter as well because PPU is reset
        //on Famicom, PPU is not reset when Reset is pressed
        //but some NES games expect it to be and you get garbage.
        framecount = 0;
    }

    public synchronized void reloadROM() {
        loadROM(curRomPath);
    }

    public synchronized void pause() {
        if (apu != null) {
            apu.pause();
        }
        runEmulation = false;
    }

    public long getFrameTime() {
        return frameDoneTime;
    }

    public void setFrameTime(long time) {
        frameDoneTime = time;
    }

    public String getrominfo() {
        if (mapper != null) {
            return mapper.getrominfo();
        }
        return null;
    }

    public synchronized void frameAdvance() {
        runEmulation = false;
        if (cpu != null) {
            runframe();
        }
    }

    public synchronized void resume() {
        if (apu != null) {
            apu.resume();
        }
        if (cpu != null) {
            runEmulation = true;
        }
    }

    public String getCurrentRomName() {
        return curRomName;
    }

    public boolean isFrameLimiterOn() {
        return frameLimiterOn;
    }

    public void messageBox(final String string) {
        if (gui != null) {
            gui.messageBox(string);
        }
    }

    public ControllerInterface getcontroller1() {
        return controller1;
    }

    public ControllerInterface getcontroller2() {
        return controller2;
    }

    public synchronized void setParameters() {
        if (apu != null) {
            apu.setParameters();
        }
        if (ppu != null) {
            ppu.setParameters();
        }
        if (limiter != null && mapper != null) {
            switch (mapper.getTVType()) {
                case NTSC:
                default:
                    limiter.setInterval(16639267);
                    break;
                case PAL:
                case DENDY:
                    limiter.setInterval(19997200);
            }
        }
    }

    /**
     * Access to the Pro Action Replay device.
     */
    public synchronized ActionReplay getActionReplay() {
        return actionReplay;
    }

    public synchronized void saveState() {
        State state = new State();

        mapper.saveState(state);
        cpu.saveState(state);
        cpuram.saveState(state);
        ppu.saveState(state);
        this.saveState(state);

        try {
            FileOutputStream fileOut = new FileOutputStream("quicksave.sav");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(state);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public synchronized void loadState() {
        State state = null;

        try {
            FileInputStream fileIn = new FileInputStream("quicksave.sav");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            state = (State) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("State class not found");
            c.printStackTrace();
            return;
        }

        reset();

        mapper.loadState(state);
        cpu.loadState(state);
        cpuram.loadState(state);
        ppu.loadState(state);
        this.loadState(state);
    }


    public void setLearning(Learning learning) {
        this.learning = learning;
    }

    public void setState(State state) {
        reset();

        mapper.loadState(state);
        cpu.loadState(state);
        cpuram.loadState(state);
        ppu.loadState(state);
        this.loadState(state);
    }

    public void renderGui() {
        limiter.sleepFixed();
        if (ppu != null && framecount > 1) {
            gui.render();
        }
    }

    public boolean isRunEmulation() {
        return runEmulation;
    }

    public Integer[] getLearningArray() {
        return learningArray;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }
}
