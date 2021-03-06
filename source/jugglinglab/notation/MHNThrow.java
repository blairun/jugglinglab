// MHNThrow.java
//
// Copyright 2019 by Jack Boyce (jboyce@gmail.com)

package jugglinglab.notation;

import jugglinglab.util.*;


public class MHNThrow {
    public int juggler;             // indexed from 0
    public int hand;                // MHNPattern.RIGHT_HAND or LEFT_HAND
    public int index;
    public int slot;
    public int targetjuggler;       // indexed from 0
    public int targethand;          // MHNPattern.RIGHT_HAND or LEFT_HAND
    public int targetindex;
    public int targetslot;
    public int handsindex;          // index of throw in hands sequence, if one exists
    public int pathnum = -1;
    public String mod;
    public MHNThrow master;
    public MHNThrow source;
    public MHNThrow target;
    public boolean catching = false;    // are we catching just before this throw?
    public int catchnum = -1;       // order (starting at 1) to make catches

    public MHNThrow() {}

    public MHNThrow(int j, int h, int i, int s, int tj, int th, int ti, int ts, String m) {
        this.juggler = j;
        this.hand = h;
        this.index = i;
        this.slot = s;
        this.targetjuggler = tj;
        this.targethand = th;   // 0 for right hand, 1 for left hand
        this.targetindex = ti;
        this.targetslot = ts;
        this.mod = m;
    }
}
