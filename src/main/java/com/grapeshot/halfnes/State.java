package com.grapeshot.halfnes;

import com.grapeshot.halfnes.mappers.Mapper;

import java.io.Serializable;

public class State implements Serializable {

    // CPU
    public int cycles;
    public int clocks;
    public int A, X, Y, S, PC;
    public boolean carryFlag, zeroFlag, interruptsDisabled, decimalModeFlag, overflowFlag, negativeFlag,
            previntflag, nmi, prevnmi, logging;
    public int pb, interrupt;
    public boolean nmiNext, idle, interruptDelay;

    // CPURAM
    public int[] wram;

    //PPU
    public int oamaddr;
    public int oamstart;
    public int readbuffer;
    public int loopyV;
    public int loopyT;
    public int loopyX;
    public int scanline;
    public int cyclesPPU;
    public int framecount;
    public int div;
    public int[] OAM;
    public int[] secOAM;
    public int[] spriteshiftregH;
    public int[] spriteshiftregL;
    public int[] spriteXlatch;
    public int[] spritepals;
    public int found;
    public int bgShiftRegH;
    public int bgShiftRegL;
    public int bgAttrShiftRegH;
    public int bgAttrShiftRegL;
    public int bgShiftRegH_ai;
    public int bgShiftRegL_ai;
    public boolean[] spritebgflags;
    public boolean even;
    public boolean bgpattern;
    public boolean sprpattern;
    public boolean spritesize;
    public boolean nmicontrol;
    public boolean grayscale;
    public boolean bgClip;
    public boolean spriteClip;
    public boolean bgOn;
    public boolean spritesOn;
    public boolean vblankflag;
    public boolean sprite0hit;
    public boolean spriteoverflow;
    public int emph;
    public int[] pal;
    public int vraminc;
    public int[] bgcolors;
    public int openbus;
    public int nextattr;
    public int linelowbits;
    public int linehighbits;
    public int linelowbits_ai;
    public int linehighbits_ai;
    public int penultimateattr;
    public int numscanlines;
    public int vblankline;
    public int[] cpudivider;


    //NES
    public boolean runEmulation;
    public boolean dontSleep;
    public boolean shutdown;
    public long frameStartTime;
    public long NESframecount;
    public long frameDoneTime;
    public boolean frameLimiterOn;
    public String curRomPath;
    public String curRomName;

    //Mapper
    public int[] prg;
    public int[] chr;
    public int[] chr_map;
    public int[] prg_map;
    public int[] prgram;
    public int mappertype;
    public int submapper;
    public int prgoff;
    public int prgsize;
    public int chroff;
    public int chrsize;
    public Mapper.MirrorType scrolltype;
    public boolean haschrram;
    public boolean hasprgram;
    public boolean savesram;
    public int[] pput0, pput1, pput2, pput3;
    public int[] nt0, nt1, nt2, nt3;
    public long crc;
    public Mapper.TVType region;


}
