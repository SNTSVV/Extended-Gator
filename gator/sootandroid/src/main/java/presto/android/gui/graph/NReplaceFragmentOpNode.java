/*
 * NReplaceFragmentOpNode.java - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.graph;

import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

//FragmentTransaction.add(int, Fragment, ...)
public class NReplaceFragmentOpNode extends NOpNode {
    public NReplaceFragmentOpNode(NNode idNode, NVarNode fragment, NNode receiverNode,
                                  Pair<Stmt, SootMethod> callSite, boolean artificial) {
        super(callSite, artificial);
        receiverNode.addEdgeTo(this);
        idNode.addEdgeTo(this);
        fragment.addEdgeTo(this);
        if (fragment.l!=null)
        {
            fragmentClass = (((RefType) fragment.l.getType()).getSootClass());
        }
    }
    private SootClass fragmentClass;
    @Override
    public boolean hasReceiver() {
        return true;
    }

    @Override
    public boolean hasParameter() {
        return true;
    }

    @Override
    public boolean hasLhs() {
        return false;
    }

    @Override
    public NVarNode getReceiver() {
        return (NVarNode) this.pred.get(0);
    }

    @Override
    //get fragment local
    public NNode getParameter() {
        return (NNode) this.pred.get(2);
    }
    public SootClass getFragmentClass() {
        return fragmentClass;
    }
}
