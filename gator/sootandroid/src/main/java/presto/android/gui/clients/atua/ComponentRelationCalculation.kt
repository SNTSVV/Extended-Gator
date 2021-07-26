/*
 * ATUA is a test automation tool for mobile Apps, which focuses on testing methods updated in each software release.
 * Copyright (C) 2019 - 2021 University of Luxembourg
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package presto.android.gui.clients.atua

import presto.android.Logger
import presto.android.gui.clients.atua.informationRetrieval.InformationRetrieval
import presto.android.gui.graph.NActivityNode
import presto.android.gui.graph.NDialogNode
import presto.android.gui.graph.NObjectNode
import presto.android.gui.listener.EventType
import presto.android.gui.wtg.ds.WTGEdge
import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.jimple.DefinitionStmt
import soot.jimple.InvokeStmt
import soot.jimple.Stmt
import soot.jimple.StringConstant

class ComponentRelationCalculation {
    companion object{
        val instance: ComponentRelationCalculation by lazy {
            ComponentRelationCalculation()
        }
    }
    val methodCallGraphs = HashMap<SootMethod,HashSet<SootMethod>>()
    val methodDependencyCounts = HashMap<SootMethod, HashSet<String>>()
    val methodRecursiveDependencyCounts = HashMap<SootMethod, HashMap<String,Long>>()
    val eventHandlersDependencyCount = HashMap<SootMethod,HashMap<String,Long>>()
    val eventsDependencyCount = HashMap<Event, HashMap<String,Long>>()
    val windowsDependencyCount = HashMap<NObjectNode, HashMap<String,Long>>()
    val windowHandlersMap = HashMap<NObjectNode,HashSet<SootMethod>>()

    val betweenClassDependencyHashMap = HashMap<SootClass, HashSet<Pair<SootClass,Long>>>()
    val classDependencyAppearance = HashMap<SootClass, Long>() //number of event handlers in which class is a dependency
    val classDependencyInverseFrequency = HashMap<SootClass, Double>()
    val eventHandlerClassWeights = HashMap<SootMethod,HashMap<String,Double>>()
    val eventClassWeights = HashMap<Event,HashMap<String,Double>>()
    val windowClassWeights = HashMap<NObjectNode, HashMap<String,Double>>()
    val eventHandlers = HashSet<SootMethod>()
    val events = HashSet<Event>()
    val windows = HashSet<NObjectNode>()

    val eventCorrelationScores = HashMap<Event, HashMap<Event, Pair<Double, HashMap<String, Double>>>>() //Event -> Map of <Event, SimilarityScore>
    val windowCorrelationScores = HashMap<NObjectNode, HashMap<NObjectNode,Pair<Double, HashMap<String,Double>>>>()
    val eventWindowCorrelationScores = HashMap<Event, HashMap<NObjectNode,Pair<Double,HashMap<String,Double>>>>()
    fun computeComponentCorrelationScore(){
        countMethodsClassDependency()
        countAllEventHandlersClassDependency()
        accumulateEventsDependency()
        accumulateWindowDependency()
        val ifForEventHandlers = InformationRetrieval<String, SootMethod>(eventHandlersDependencyCount)
        eventHandlerClassWeights.putAll(ifForEventHandlers.allDocumentsNormalizedTermsWeight)
        val ifForEvents = InformationRetrieval<String,Event>(eventsDependencyCount)
        eventClassWeights.putAll(ifForEvents.allDocumentsNormalizedTermsWeight)
        computeAllEventCorrelationScores(ifForEvents)

        val ifForWindows = InformationRetrieval<String, NObjectNode>(windowsDependencyCount)
        windowClassWeights.putAll(ifForWindows.allDocumentsNormalizedTermsWeight)
        computeAllWindowCorrelationScores(ifForWindows)
        computeEventWindowCorrelationScores(ifForWindows)
        //computeEventHandler_ClassWeights()
        //normalizeEventHandler_ClassWeights()
        //computeAllEventCorrelationScores()
        //computeAllWindowCorrelationScores()
    }

    private fun computeEventWindowCorrelationScores(informationRetrieval: InformationRetrieval<String, NObjectNode>) {
        events.filter {eventsDependencyCount.containsKey(it)}.forEach { queryEvent->
            val queryWindowDependency = eventsDependencyCount[queryEvent]!!
            val queryResult = HashMap<NObjectNode, Pair<Double,HashMap<String,Double>>>()
            val topRelatedWindows = informationRetrieval.searchSimilarDocuments(query = queryWindowDependency,limit = 0)
            topRelatedWindows.filter {it.first!=queryEvent.source}.forEach {
                queryResult.put(it.first,Pair(first = it.second,second = it.third))
            }
            eventWindowCorrelationScores.put(queryEvent, queryResult)
        }
    }

    private fun normalizeEventHandler_ClassWeights() {
        methodRecursiveDependencyCounts.forEach { ha, u ->  }
    }

    fun registerEvent(wtgEdge: WTGEdge){
        val exisitingEvent = events.find { it.source == wtgEdge.sourceNode.window
                && it.guiWidget == wtgEdge.guiWidget
                && it.eventHandlers.containsAll(wtgEdge.eventHandlers)
                && it.eventType == wtgEdge.eventType}
        if (exisitingEvent == null)
        {
            val newEvent = Event(
                    source = wtgEdge.sourceNode.window,
                    guiWidget = wtgEdge.guiWidget,
                    eventHandlers = wtgEdge.eventHandlers,
                    eventType = wtgEdge.eventType
            )
            events.add(newEvent)
            eventHandlers.addAll(newEvent.eventHandlers)
            eventHandlers.addAll(wtgEdge.callbacks.map { it.eventHandler })
        }
    }

    fun registerMethod(method: SootMethod)
    {
        if (methodDependencyCounts.containsKey(method))
            return
        methodDependencyCounts.put(method, HashSet())

    }

    internal fun countMethodRecursiveClassDependency (sootMethod: SootMethod, recursiveMethods: HashSet<SootMethod>)
    {
        if (methodRecursiveDependencyCounts.containsKey(sootMethod))
            return
        if (!methodDependencyCounts.containsKey(sootMethod))
            return
//        if (methodClassDependencyCounts[sootMethod]!!.isEmpty())
//            return
        recursiveMethods.add(sootMethod)
        val recursiveClassDependency = HashMap<String,Long>()
        var dependencies: HashSet<String> = methodDependencyCounts[sootMethod]!!
        dependencies.forEach { term->
            if (!recursiveClassDependency.containsKey(term))
                recursiveClassDependency.put(term,1)
           /* else
                recursiveClassDependency[term] = recursiveClassDependency[term]!! + 1*/
        }
        val processMethods = HashSet<SootMethod>()
        if (methodCallGraphs.containsKey(sootMethod))
        {
            processMethods.addAll(methodCallGraphs[sootMethod]!!.filterNot { recursiveMethods.contains(it) })
        }
        val nextProcessMethods = HashSet<SootMethod>()
        processMethods.forEach {
            countMethodRecursiveClassDependency(it,recursiveMethods)
            if (methodRecursiveDependencyCounts.containsKey(it))
            {
                if (sootMethod.signature.equals("<de.rampro.activitydiary.ui.generic.EditActivity: void access\$100(de.rampro.activitydiary.ui.generic.EditActivity)>"))
                {
                    Logger.verb("DEBUG", "Recursive method dependency: ${it.signature}")
                }
                val dependency = methodRecursiveDependencyCounts[it]!!
                dependency.forEach {sclass, count ->
                    if (!recursiveClassDependency.containsKey(sclass))
                        recursiveClassDependency.put(sclass,count)
                    else
                        recursiveClassDependency[sclass] = recursiveClassDependency[sclass]!! + count
/*                    if (Scene.v().containsClass(sclass))
                    {
                        val innerClass = Scene.v().getSootClass(sclass)!!
                        if (innerClass.hasOuterClass() && innerClass.outerClass == sootMethod.declaringClass)
                        //get all class method
                        {
                            val classMethods = Scene.v().getSootClass(sclass)!!.methods
                            classMethods.forEach {
                                nextProcessMethods.add(it)
                            }
                        }

                    }*/
                }
            }
        }
        nextProcessMethods.forEach {
            countMethodRecursiveClassDependency(it,recursiveMethods)
            if (methodRecursiveDependencyCounts.containsKey(it))
            {
                val dependency = methodRecursiveDependencyCounts[it]!!
                dependency.forEach {sclass, count ->
                    if (!recursiveClassDependency.containsKey(sclass))
                        recursiveClassDependency.put(sclass,count)
                    else
                        recursiveClassDependency[sclass] = recursiveClassDependency[sclass]!! + count
                }
            }
        }
        methodRecursiveDependencyCounts.put(sootMethod,recursiveClassDependency)
    }

    internal fun computeClassDependency()
    {
        methodRecursiveDependencyCounts.forEach {
            val componentClass = it.key.declaringClass
            val callingClasses = it.value
            if (!betweenClassDependencyHashMap.containsKey(componentClass))
            {
                betweenClassDependencyHashMap.put(componentClass,HashSet())
            }
            val dependencies = betweenClassDependencyHashMap[componentClass]!!
            callingClasses.forEach { sootClass ->
                val existingDependency = dependencies.find { it.first == sootClass }
                /*if (existingDependency!=null)
                {
                    val newDependency = existingDependency.copy(sootClass,existingDependency.second+1)
                    dependencies.remove(existingDependency)
                    dependencies.add(newDependency)
                }
                else
                {
                    val newDependency = Pair(sootClass, 1)
                    dependencies.add(newDependency)
                }*/
            }
        }
    }

    private fun computeActivityCorrelationScore() {
        betweenClassDependencyHashMap.forEach {
            val class1 = it.key
            betweenClassDependencyHashMap.forEach {
/*                val class2 = it.key
                if (class1 != class2 &&
                        !componentRelationScore.containsKey(Pair(class1, class2))
                        && !componentRelationScore.containsKey(Pair(class2, class1))) {
                    val dependencyClass1 = betweenClassDependencyHashMap[class1]!!
                    val dependencyClass2 = betweenClassDependencyHashMap[class2]!!
                    var score = 0.0
                    dependencyClass1.forEach {

                        if (dependencyClass2.any { p -> p.first == it.first }) {
                            score += (inverseTermFrequencies[it.first]!!*it.second.toDouble())
                        }
                    }
                    componentRelationScore.put(Pair(class1, class2), score)
                }*/
            }
        }
    }

    private fun countMethodsClassDependency() {
        //Logger.verb("DEBUG","Number of event Handlers: ${eventHandlers.size}")
        Scene.v().applicationClasses
                .forEach {
                    val sootClass = it
                    //
                    //Logger.verb("DEBUG", "${sootClass.name}")
                    sootClass.methods.forEach {
                        //Logger.verb("DEBUG", "${it.signature}")
                        registerMethodRcv(it)
                    }
                }
        eventHandlers.forEach {
            //Logger.verb("DEBUG", "${it.signature}")
            registerMethodRcv(it)
        }
        methodDependencyCounts.forEach { method, _ ->
            countMethodRecursiveClassDependency(method, HashSet())
        }
    }

    private fun countAllEventHandlersClassDependency() {
        Logger.verb("DEBUG", "Count event handler's dependency classes")
        val flowgraph = GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph
        flowgraph.allNActivityNodes.map { it.key }.forEach {
            val activityMethods = it.methods.filter { methodDependencyCounts.containsKey(it) }
            activityMethods.forEach {
               if (methodRecursiveDependencyCounts.containsKey(it))
                   eventHandlersDependencyCount.put(it.method(),methodRecursiveDependencyCounts[it]!!).also { _ ->
                       //Logger.verb("DEBUG", "Count event handler dependency: ${it.signature}")
                   }
            }
        }

        flowgraph.allNDialogNodes.map { it.value.c }.filter{ it.isApplicationClass}.forEach {
            val dialogMethods = it.methods.filter { methodDependencyCounts.containsKey(it) }
            dialogMethods.forEach {
                if (methodRecursiveDependencyCounts.containsKey(it))
                    eventHandlersDependencyCount.put(it.method(),methodRecursiveDependencyCounts[it]!!).also { _ ->
                        //Logger.verb("DEBUG", "Count event handler dependency: ${it.signature}")
                    }
            }
        }
        eventHandlers.forEach {
            if (methodRecursiveDependencyCounts.containsKey(it))
                eventHandlersDependencyCount.put(it.method(),methodRecursiveDependencyCounts[it]!!).also { _ ->
                    //Logger.verb("DEBUG", "Count event handler dependency: ${it.signature}")
                }
        }

    }

    fun computeEventHandlerCorrelationScore(){
        methodRecursiveDependencyCounts.filter { map->
            //Get only event handlers related to modified methods
            GUIUserInteractionClient.modMethodInvocation.any { it.value.any { it.eventHandlers.contains(map.key) || it.callbacks.any { it.eventHandler==map.key } } }
        }. forEach { handler1, classDependencies1 ->
            methodRecursiveDependencyCounts.filterNot { it.key==handler1 }. forEach { handler2, classDependencies2 ->
               /* if (!eventComponentRelationScore.containsKey(Pair(handler1,handler2))
                        && !eventComponentRelationScore.containsKey(Pair(handler2,handler1))){
                    var score :Double = 0.0
                    classDependencies1.forEach {class1->
                        if (classDependencies2.any { it == class1 })
                        {
                            score += inverseTermFrequencies[class1]!!
                        }
                    }
                    eventComponentRelationScore.put(Pair(handler1,handler2),score)
                }*/
            }
        }
    }

    private fun registerMethodRcv(method: SootMethod) {
        if (methodDependencyCounts.containsKey(method))
            return
        if (!method.hasActiveBody())
            return
        registerMethod(method)
        val dependencies = methodDependencyCounts[method]!!
        val units = method.activeBody.units.snapshotIterator()
        while (units.hasNext()) {
            val unit = units.next()
            //Logger.verb("DEBUG", unit.toString())
            var callingMethod: SootMethod? = null
            if (unit is Stmt )
            {
                if (unit.containsFieldRef())
                {
                    val field = unit.fieldRef.field
                    //Logger.verb("DEBUG", "${field.declaration}")
                    if (!field.name.equals("this$0"))
                        dependencies.add(field.toString())
                    /*if (field.isStatic)
                        dependencies.add(field.declaration)*/
                    //dependencies.add(field.toString())
                }

                if (unit is DefinitionStmt)
                {
                    if (unit.containsInvokeExpr())
                    {
                        val invokeExpr = unit.invokeExpr
                        callingMethod = invokeExpr.method
                        invokeExpr.args.forEach {
                            if (it is StringConstant && it.value.trim().isNotBlank())
                            {
                                val stringLiteral = it.value
                                dependencies.add(stringLiteral)
                            }
                        }
                    }

                }
                if (unit is InvokeStmt)
                {
                    val invokeExpr = unit.invokeExpr
                    callingMethod = invokeExpr.method
                    invokeExpr.args.forEach {
                        if (it is StringConstant)
                        {
                            val stringLiteral = it.value
                            dependencies.add(stringLiteral)
                        }
                    }
                }
            }


            if (callingMethod != null && !callingMethod.isJavaLibraryMethod)
            {
                /*if (callingMethod.isStatic)
                {
                    dependencies.add(callingMethod.signature)
                }*/
                val dependencyClass = callingMethod!!.declaringClass
                /*if (dependencyClass!=method.declaringClass)
                {
                    dependencies.add(callingMethod.declaringClass.name)
                }*/
                if (dependencyClass == method.declaringClass || (dependencyClass.hasOuterClass() && dependencyClass.outerClass == method.declaringClass)
                        || (method.declaringClass.hasOuterClass() && method.declaringClass.outerClass == dependencyClass))
                {
                    if (method.signature.equals("<de.rampro.activitydiary.ui.generic.EditActivity\$1: void afterTextChanged(android.text.Editable)>")
                            || method.signature.equals("<de.rampro.activitydiary.ui.generic.EditActivity: void access\$100(de.rampro.activitydiary.ui.generic.EditActivity)>")
                            || method.signature.equals("<de.rampro.activitydiary.ui.generic.EditActivity: void access\$1100(de.rampro.activitydiary.ui.generic.EditActivity)>"))
                    {
                        Logger.verb("DEBUG", "Calling method: ${callingMethod.signature}")
                    }
                    if (!methodCallGraphs.containsKey(method))
                        methodCallGraphs.put(method, HashSet())
                    methodCallGraphs[method]!!.add(callingMethod)
                    registerMethodRcv(callingMethod)
                }

            }
        }
        //Count its superclass as dependency
        val flowgraph = GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph
//        val superclasses = flowgraph.hier.getSupertypes(method.declaringClass).filter { it.isApplicationClass && it!=method.declaringClass }
//        dependencies.addAll(superclasses)
        //classDependency.add(method.declaringClass)

        method.declaringClass.methods.filterNot {it.isAbstract}.forEach {
            registerMethodRcv(it)
        }
    }


    private fun accumulateEventsDependency(){
        events.filter{it.guiWidget?.idNode!=null
                || it.guiWidget is NActivityNode || it.guiWidget is NDialogNode}. forEach { event ->
            val eventDependency = HashMap<String,Long>()
            event.eventHandlers.forEach {
                if (methodRecursiveDependencyCounts.containsKey(it))
                {
                    if(methodRecursiveDependencyCounts[it]!!.isNotEmpty())
                    {

                        methodRecursiveDependencyCounts[it]!!.forEach {term, count ->
                            if (!eventDependency.containsKey(term))
                                eventDependency.put(term,count)
                            else
                                eventDependency[term] = eventDependency[term]!! + count
                        }
                    }
                }

            }
            eventsDependencyCount.put(event,eventDependency)
        }
    }

    private fun accumulateWindowDependency(){
        windows.forEach { queryWindow ->
            val windowDependency = HashMap<String,Long>()
            val windowMethods = HashSet<SootMethod>()
            windowMethods.addAll(events.filter { it.source == queryWindow }.flatMap { it.eventHandlers })
            //get all propriate methods in queryWindow's class'superType
            GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph.hier.getSupertypes(queryWindow.classType).forEach {
                windowMethods.addAll(it.methods.filter { methodRecursiveDependencyCounts.containsKey(it) })
            }
            windowMethods.addAll(queryWindow.classType.methods.filter {methodRecursiveDependencyCounts.containsKey(it) })
            Scene.v().applicationClasses.filter{it.hasOuterClass()}.filter { it.outerClass == queryWindow.classType }.forEach {
                windowMethods.addAll(it.methods.filter { methodRecursiveDependencyCounts.containsKey(it) })
            }
            windowMethods.forEach {
                if (methodRecursiveDependencyCounts.containsKey(it))
                {
                    if(methodRecursiveDependencyCounts[it]!!.isNotEmpty())
                    {
                        methodRecursiveDependencyCounts[it]!!.forEach {sclass, count ->
                            if (!windowDependency.containsKey(sclass))
                                windowDependency.put(sclass,count)
                            else
                                windowDependency[sclass] = windowDependency[sclass]!! + count
                        }
                    }
                }
            }
            windowsDependencyCount.put(queryWindow,windowDependency)
            windowHandlersMap.put(queryWindow,windowMethods)
        }
    }
    private fun computeAllWindowCorrelationScores(informationRetrieval: InformationRetrieval<String, NObjectNode>)
    {
        windows.forEach { queryWindow->
            val queryWindowDependency = windowsDependencyCount[queryWindow]!!
            val queryResult = HashMap<NObjectNode, Pair<Double,HashMap<String,Double>>>()
            val topRelatedWindows = informationRetrieval.searchSimilarDocuments(query = queryWindowDependency,limit = 0)
            topRelatedWindows.filter{it.first!=queryWindow}.forEach {
                queryResult.put(it.first, Pair(first = it.second,second = it.third))
            }
            windowCorrelationScores.put(queryWindow, queryResult)
        }
    }

    private fun computeAllEventCorrelationScores(informationRetrieval: InformationRetrieval<String,Event>)
    {
        events.filter{eventsDependencyCount.contains(it) && eventsDependencyCount[it]!!.isNotEmpty()}.forEach {queryEvent->
            val queryEventDependency = eventsDependencyCount[queryEvent]!!
            val queryResult = HashMap<Event, Pair<Double, HashMap<String,Double>>>()
            val topRelatedEvents = informationRetrieval.searchSimilarDocuments(query = queryEventDependency,limit = 21)
            topRelatedEvents.filter{it.first!=queryEvent}.forEach {
                queryResult.put(it.first,Pair(first = it.second,second = it.third))
            }
            eventCorrelationScores.put(queryEvent, queryResult)
        }
    }


    private fun isAsyncClass(newClass: SootClass) =
            GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(newClass.name, "android.os.AsyncTask") ||
                    GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(newClass.name, "java.lang.Runnable")

    class Event (val source: NObjectNode,
                 val eventHandlers: Set<SootMethod>,
            val eventType: EventType,
            val guiWidget: NObjectNode)
    {

    }
}