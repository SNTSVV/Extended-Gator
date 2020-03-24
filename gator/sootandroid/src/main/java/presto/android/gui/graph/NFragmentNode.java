/*
 * NFragmentNode.java - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

public class NFragmentNode extends NWindowNode {
    @Override
    public String toString() {
        return "FRAG[" + c + "]" + id;
    }
}
