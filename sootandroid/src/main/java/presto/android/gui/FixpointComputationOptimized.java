/*
 * FixpointComputationOptimized.java - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui;

import com.google.common.collect.Sets;
import presto.android.Configs;
import presto.android.Debug;
import presto.android.Logger;
import presto.android.MultiMapUtil;
import presto.android.gui.graph.*;
import soot.SootClass;
import soot.jimple.toolkits.scalar.NopEliminator;
import sun.rmi.runtime.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class include two methods:
 * 1) optimized computePathsFromViewProducerToViewConsumer() of FixpointSolver.
 * 2) modified (to support the optimization) windowReachability() of FixpointSolver.
 * Calls to these two methods are changed to these in here.
 */
public class FixpointComputationOptimized {
    static void optimizedComputePathsFromViewProducerToViewConsumer(FixpointSolver solver) {
        Logger.verb("Fixpoint", "Begin optimizedComputePathsFromViewProducerToViewConsumer...");
        for (NActivityNode activityNode: solver.flowgraph.butterKnifeActivityRootBinding.keySet()) {
            if (!solver.activityRoots.containsKey(activityNode)){
                continue;
            }
            NNode bindingRoot = solver.flowgraph.butterKnifeActivityRootBinding.get(activityNode);
            for (NNode root: solver.activityRoots.get(activityNode)) {
                root.addEdgeTo(bindingRoot);
            }
        }
        for (NNode source : solver.flowgraph.allNNodes) {

            Set<NNode> reachables = null;
            if (source instanceof NViewAllocNode
                    || source instanceof NInflNode
                    || source instanceof NOptionsMenuNode
                    || source instanceof NContextMenuNode
                    || source instanceof NFindView1OpNode
                    || source instanceof NFindView2OpNode
                    || source instanceof NFindView3OpNode
                    || source instanceof NInflate1OpNode) {
                reachables = solver.graphUtil.reachableNodes(source);
               /* if (source instanceof  NInflNode){
                        Logger.verb("FixpointOptimized", "InfNode: "+ source.toString());
                }*/

                for (NNode target : reachables) {

                    if (!(target instanceof NOpNode)) {
                        continue;
                    }
                    NOpNode opNode = (NOpNode) target;
                   /* if (source.id == 5860)
                        Logger.verb("DEBUG","Target: " + target.toString());*/
                    // View as parameter
                    if (target instanceof NAddView1OpNode
                            || (target instanceof NAddView2OpNode
                                && reachables.contains(((NOpNode) target).getParameter())
                    /*&& reachables.contains(((NOpNode) target).getReceiver())*/)) {
                        MultiMapUtil.addKeyAndHashSetElement(
                                solver.reachingParameterViews, (NOpNode) target, source);

                        if (source instanceof NOpNode) {
                            //reverse
                            MultiMapUtil.addKeyAndHashSetElement(solver.reachedParameterViews,
                                    (NOpNode) source, (NOpNode) target);
                            /*if (source instanceof NInflate1OpNode)
                                Logger.verb("DEBUG","Add to reachingParameterViews: "+source+ " - "+target);*/
                        } else if (source instanceof NObjectNode
                                && solver.isValidFlowByType(source, (NOpNode) target,
                                    FixpointSolver.VarType.Parameter)) {
                            if (source instanceof NViewAllocNode
                                    || source instanceof NInflNode) {
                                MultiMapUtil.addKeyAndHashSetElement(solver.solutionParameters,
                                        (NOpNode) target, source);
                                /*if (source.id == 5860)
                                    Logger.verb("DEBUG","Add to solutionParameters" + target.toString());*/
                            } else {
                                if (Configs.sanityCheck) {
                                    throw new RuntimeException(
                                            "Unhandled reaching parameter at " + target + " for " + source);
                                } else {
                                    Logger.warn(FixpointComputationOptimized.class.getSimpleName(),
                                            "Unhandled reaching parameter at " + target + " for " + source);
                                }
                            }
                        }
                    }
                    else if (target instanceof NSetListenerOpNode
                            && reachables.contains(((NSetListenerOpNode) target).getParameter())) {
                        if (source instanceof NOpNode) {
//                            Logger.verb("FixpointOptimized", "ReachedListener: " + source.toString() + "--> "+ target.toString());

                            //the result of source could flow to SetListener
                            //If source is NObjectNode, it will be taken care of later.
                            MultiMapUtil.addKeyAndHashSetElement(solver.reachingListeners,
                                    ((NSetListenerOpNode) target), source);
                            //reverse
                            MultiMapUtil.addKeyAndHashSetElement(solver.reachedListeners,
                                    (NOpNode) source, (NSetListenerOpNode) target);
                        }
                    }
                    // View as receiver
                    if (target instanceof NFindView1OpNode
                            || target instanceof NFindView3OpNode
                            || target instanceof NSetIdOpNode
                            || target instanceof NSetTextOpNode
                            || (target instanceof NSetListenerOpNode
                                && reachables.contains(((NOpNode) target).getReceiver()))
                            || (target instanceof NAddView2OpNode
                                && reachables.contains(((NOpNode) target).getReceiver()))) {
                        if ((source instanceof NOptionsMenuNode
                                    || source instanceof NContextMenuNode)
                                && target instanceof NSetIdOpNode) {
                            //MenuNode cannot be a receiver of SetId
                            continue;
                        }

                        //Logger.verb("FixpointOptimized", "reachedReceiverViews: " + source.toString() +"--> "+ target.toString());
                        MultiMapUtil.addKeyAndHashSetElement(solver.reachingReceiverViews,
                                (NOpNode) target, source);
                        if (source instanceof NOpNode) {
                            //reverse
                            //TODO: If target is NSetListenerOpNode, add reachedReceiverViews only with source's...
                            MultiMapUtil.addKeyAndHashSetElement(solver.reachedReceiverViews,
                                    (NOpNode) source, (NOpNode) target);
                          /*  if (source instanceof NInflate1OpNode)
                                Logger.verb("DEBUG","Add to reachedReceiverViews: "+source+ " - "+target);*/
                        } else if (source instanceof NObjectNode
                                && solver.isValidFlowByType(source, (NOpNode) target,
                                    FixpointSolver.VarType.Receiver)) {
                            if (source instanceof NViewAllocNode
                                    || source instanceof NOptionsMenuNode
                                    || source instanceof NContextMenuNode
                                    || source instanceof NInflNode
                            ) {
                                MultiMapUtil.addKeyAndHashSetElement(solver.solutionReceivers,
                                        (NOpNode) target, source);

                            } else {
                                if (Configs.sanityCheck) {
                                    throw new RuntimeException(
                                            "Unhandled reaching receiver at " + target + " for " + source);
                                } else {
                                    Logger.warn(FixpointComputationOptimized.class.getSimpleName(),
                                            "Unhandled reaching receiver at " + target + " for " + source);
                                }
                            }
                        }
                    }
                }
            }
            // Any object could be a listener
            if (source instanceof NObjectNode
                    && solver.listenerSpecs.isListenerType(((NObjectNode) source).getClassType())) {
                //Logger.verb("FixpointOptimized", "View: "+ source.toString());
                if (reachables == null) {
                    reachables = solver.graphUtil.reachableNodes(source);
                }
                for (NNode target : reachables) {

                    if (target instanceof NSetListenerOpNode
                            && reachables.contains(((NSetListenerOpNode) target).getParameter())) {
                        //Logger.verb("FixpointOptimized", "Set Listener: "+target.toString());
                        MultiMapUtil.addKeyAndHashSetElement(solver.reachingListeners,
                                ((NSetListenerOpNode) target), source);
                        MultiMapUtil.addKeyAndHashSetElement(solver.solutionListeners,
                                (NSetListenerOpNode) target, source);
                    }
                }
            }
            //Resolve fields calling

        }

        solver.solutionResultsReachability();
        Logger.verb("Fixpoint", "optimizedComputePathsFromViewProducerToViewConsumer Done.");
    }

    static void windowReachability(FixpointSolver solver) {
        Logger.verb(FixpointComputationOptimized.class.getSimpleName(), ".......");
        for (NWindowNode windowNode : NWindowNode.windowNodes) {
            Logger.verb("windowReachability","windowNode: "+windowNode);
            Set<NNode> reachables = solver.graphUtil.reachableNodes(windowNode);
            Logger.verb("windowReachability","targetNode count: "+reachables.size());
            for (NNode target : reachables) {
                if (!(target instanceof NOpNode)) {
                    continue;
                }
                NOpNode opNode = (NOpNode) target;
                if ((opNode instanceof NInflate2OpNode
                        || opNode instanceof NAddView1OpNode
                        || opNode instanceof NFindView2OpNode)
                       && reachables.contains(opNode.getReceiver())) {
                    if(Configs.debugCodes.contains(Debug.WINDOW_REACHABILITY_DEBUG)) {
                        Logger.verb("windowReachability","reachable: "+opNode + "--"+opNode.getReceiver());
                    }
                    MultiMapUtil.addKeyAndHashSetElement(solver.reachingWindows, opNode, windowNode);
                } else if (opNode instanceof NSetListenerOpNode
                      /* && reachables.contains(opNode.getParameter())*/) {
                    if (Configs.debugCodes.contains(Debug.LISTENER_DEBUG)) {
                        Logger.verb(FixpointComputationOptimized.class.getSimpleName(),
                                "[WindowAsListener] " + windowNode + " -> " + opNode);
                    }
                    MultiMapUtil.addKeyAndHashSetElement(solver.reachingListeners, opNode, windowNode);
                    if (solver.listenerSpecs.isListenerType((windowNode).getClassType())) {
                        MultiMapUtil.addKeyAndHashSetElement(solver.solutionListeners, opNode, windowNode);
                    }
                } else {
                    //throw new RuntimeException(objectNode + " reaching " + opNode);
                }
            }
        }
        Logger.verb(FixpointComputationOptimized.class.getSimpleName(), "Done.");
    }
}
