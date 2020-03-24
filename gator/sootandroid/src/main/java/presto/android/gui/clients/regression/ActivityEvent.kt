/*
 * ActivityEvent.kt - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.clients.regression

import presto.android.gui.graph.NNode
import presto.android.gui.listener.EventType
import soot.SootClass
import soot.SootMethod

 data class ActivityEvent (
     val handler: List<SootMethod>,
    val eventType: EventType,
    val widget: NNode,
    val window: NNode
)