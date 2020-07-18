/*
 * CallBackFinder.kt - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.clients.regression.helper

import org.slf4j.LoggerFactory
import presto.android.Logger
import presto.android.MethodNames
import presto.android.gui.GUIAnalysisOutput
import presto.android.gui.clients.regression.ComponentRelationCalculation
import presto.android.gui.clients.regression.GUIUserInteractionClient
import presto.android.gui.graph.NActivityNode
import presto.android.gui.graph.NVarNode
import presto.android.gui.wtg.ds.WTGEdge
import soot.*
import soot.jimple.*
import soot.jimple.internal.JInvokeStmt
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.callgraph.Sources
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class CallbackFinder (val guiAnalysisOutput: GUIAnalysisOutput,
                      val meaningfulEventHandlers: List<String>,
                      val widgetEvents: HashMap<String, ArrayList<WTGEdge>>,
                      val notAppearInTargetCallingMethods: ArrayList<String>,
                      val topCallingModifiedMethods: HashMap<String, ArrayList<String>>,
                      val implicitIntentActivities: Set<SootClass>,
                      val intentCallingMethods: HashMap<String, HashSet<SootClass>>) {
    //For debug
    val debugCallback = "<com.eleybourn.bookcatalogue.utils.StorageUtils: void sendDebugInfo(android.content.Context,com.eleybourn.bookcatalogue.CatalogueDBAdapter)>"
    var callbackDebug = false

    val currentCallingGraph = Stack<String>()

    val cacheTopCallingMethods = HashMap<String, ArrayList<String>>()

    val methodDependencyVectors = HashMap<SootMethod, HashSet<SootClass>>()

    private val log by lazy { LoggerFactory.getLogger(this::class.java) }

     fun findMethodInvocation(refinedPackageName: String, methodSignatureList: List<String>): HashMap<String,ArrayList<WTGEdge>>{
         recordAsyncTaskUsage()
         val modMethodInvocation = HashMap<String, ArrayList<WTGEdge>>()
         methodSignatureList.forEach {
            if (Scene.v().containsMethod(it))
            {
                val sootMethod = Scene.v().getMethod(it)
                // Logger.verb("DEBUG","Finding invocation for: ${sootMethod.signature}")
                findTopCallingMethod(sootMethod,sootMethod,refinedPackageName,0,null)
            }
        }

        topCallingModifiedMethods.forEach { k, v ->
            v.forEach {
                val eventHandlers = getEventHandlers(it)
                if (eventHandlers.isEmpty())
                {
                    if (!notAppearInTargetCallingMethods.contains(it))
                        notAppearInTargetCallingMethods.add(it)
                }
                else
                {
                    addMethodInvocation(k,eventHandlers, modMethodInvocation)
                }
            }
        }
         return modMethodInvocation
    }

    /***
     * Return true if we can find a topCallingMethod, otherwise return false
     */
     fun findTopCallingMethod(modMethod: SootMethod, callback: SootMethod, refinedPackageName: String, abstractLevel: Int, lastActivityMethod: SootMethod?): Boolean {
        var currentLastActivityMethod = lastActivityMethod
        if (modMethod.signature.contains("signedBy"))
        {
            Logger.verb("DEBUG", "Modified method: ${modMethod.signature} - Callback: ${callback.signature}")
        }
        if (currentCallingGraph.contains(callback.signature)) {
            //Logger.verb("DEBUG","Callback in a loop: ${callback.signature}")
            if (cacheTopCallingMethods.containsKey(callback.signature))
            {

                cacheTopCallingMethods[callback.signature]!!.forEach {
                    addTopCallingMethod(modMethod.signature,it, topCallingModifiedMethods)
                    addTopCallingMethod(modMethod.signature, it, cacheTopCallingMethods)
                }
                if (intentCallingMethods.containsKey(callback.signature))
                {
                    if (intentCallingMethods.containsKey(modMethod.signature))
                    {
                        intentCallingMethods[modMethod.signature]!!.addAll(intentCallingMethods[callback.signature]!!)
                    }
                    else {
                        intentCallingMethods.put(modMethod.signature, intentCallingMethods[callback.signature]!!)
                    }
                }
                return true
//                Logger.verb("DEBUG", "Top calling method of ${modMethod.signature} is got from ${callback.signature}")
            }
            else
            {
//                Logger.verb("DEBUG", "Checked callback. No more calling method found for ${callback.signature}.")
                return true
            }
            return true
        }
        currentCallingGraph.push(callback.signature)
        //ComponentRelationCalculation.instance.registerMethod(method = callback)
        if (callback.signature.equals(debugCallback))
        {
            callbackDebug = true
        }
        if (callbackDebug)
        {
            Logger.verb("DEBUG", "Callback: ${callback.signature}")
        }
        var topCallingPointFound: Boolean = false

        if (isContainsGetIntent(callback))
        {
            //Logger.verb("GetIntent", "Callback: ${callback.signature}")
            val outerClass:SootClass = getMostOuterClass(callback.declaringClass)
            if (GUIUserInteractionClient.guiAnalysisOutput!!.flowgraph.hier.isActivityClass(outerClass))
            {
                //Logger.verb("GetIntent", "Activity: ${outerClass.name}")
                if (implicitIntentActivities.contains(outerClass))
                {
                    //Logger.verb("GetIntent", "Process implicit intent: ${outerClass.name}")
                    if (!intentCallingMethods.containsKey(callback.signature))
                    {
                        intentCallingMethods.put(callback.signature, HashSet() )
                    }
                    intentCallingMethods[callback.signature]!!.add(outerClass)
                    if (!intentCallingMethods.containsKey(modMethod.signature))
                    {
                        intentCallingMethods.put(modMethod.signature, HashSet() )
                    }
                    intentCallingMethods[modMethod.signature]!!.add(outerClass)
                }
            }
        }

        val eventHandlers = getEventHandlers(callback.signature)
        if (eventHandlers.size>0)
        {
            addTopCallingMethod(modMethod.signature, callback.signature, topCallingModifiedMethods)
            addTopCallingMethod(modMethod.signature, callback.signature, cacheTopCallingMethods)
            addTopCallingMethod(callback.signature,callback.signature,cacheTopCallingMethods)
            topCallingPointFound = true
//            Logger.verb("DEBUG", "Callback is event handlers, top calling method of ${modMethod.signature} is ${callback.signature}")
        }
        else
        {
            val callGraph = Scene.v().callGraph
            //check if method is neverused
            if (!isReachable(callGraph, callback, refinedPackageName))
            {
                addTopCallingMethod(modMethod.signature, callback.signature, topCallingModifiedMethods)
                addTopCallingMethod(modMethod.signature, callback.signature, cacheTopCallingMethods)
//                if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                {
//                    Logger.verb("DEBUG", "No more calling method found for ${callback.signature}.")
//                    Logger.verb("DEBUG", "Top calling method of ${modMethod.signature} is ${callback.signature}")
//                }

                topCallingPointFound = true
//                Logger.verb("DEBUG", "${callback.signature} is unreachable.")
//                Logger.verb("DEBUG", "Top calling method of ${modMethod.signature} is ${callback.signature}")
            }
            else
            {
                topCallingPointFound = false
                val declaringClass = callback.declaringClass
                if ((isApplicationGUIClass(declaringClass)) && !callback.isConstructor)
                {
                    currentLastActivityMethod = callback
                }
                val initializingClasses = ArrayList<SootClass>()
                if (!callback.isStatic)
                {
                    //Find declaring class constructor call
                    val initializingClassMethods = ArrayList<SootMethod>()
                    val constructos = declaringClass.methods.filter { it.isConstructor }
                    constructos.forEach {
                        currentCallingGraph.push(it.signature)
                        var callConstructorSources = Sources(callGraph.edgesInto(it))
                        while (callConstructorSources.hasNext())
                        {
                            val initializingClassMethod = callConstructorSources.next().method()
                            if (!initializingClassMethod.method().declaringClass.name.contains("com.google.android.gms.internal")) {
                                initializingClasses.add(initializingClassMethod.declaringClass)
                                initializingClassMethods.add(initializingClassMethod)
                            }
                        }
                    }
                    initializingClassMethods.forEach {
                        if (callbackDebug)
                        {
                            Logger.verb("DEBUG", "Next Callback for initializeConstructors: ${it.signature}`")
                        }
                        findTopCallingMethod(modMethod,it,refinedPackageName, abstractLevel, currentLastActivityMethod)
                        if (cacheTopCallingMethods.containsKey(it.signature)) {
                            cacheTopCallingMethods[it.signature]!!.forEach { callingMethodSig ->
                                addTopCallingMethod(callback.signature, callingMethodSig, cacheTopCallingMethods)
                                addTopCallingMethod(it.signature, callingMethodSig, cacheTopCallingMethods)
                            }
                        }
                    }
                }

                val virtualMethods = ArrayList<SootMethod>()

                guiAnalysisOutput!!.flowgraph.hier.getSupertypes(callback.declaringClass)?.
                        filter{it!=callback.declaringClass}?.
                        forEach { c ->
                            if (c.declaresMethod(callback.subSignature))
                            {
                                //Logger.verb("DEBUG","SuperClass of "+ callback.declaringClass+ " --> "+c.name)
                                val virtualMethod = c.getMethod(callback.subSignature)
                                if (!currentCallingGraph.contains(virtualMethod.signature)){
                                    //Logger.verb("DEBUG","Virtual method: "+virtualMethod.signature)
                                    virtualMethods.add(virtualMethod)
                                }
                            }
                        }
                //Logger.verb("DEBUG","Number of virtual methods: "+virtualMethods.size)
                virtualMethods.add(callback)
                virtualMethods.forEach { m->
                    //if (m.declaringClass.isInnerClass && m.declaringClass.)
                    var hasCallingPoint = false
                    currentCallingGraph.push(m.signature)
                    if (callbackDebug)
                    {
                        Logger.verb("DEBUG", "Virtual method: ${m.signature}")
                    }
                    var sources = Sources(callGraph.edgesInto(m))
                    while (sources.hasNext()) {
                        val cb = sources.next().method()
                        //Logger.verb("DEBUG","Callback of ${m.signature}: ${cb.signature}")
                        if (!virtualMethods.contains(cb) ) {
                            if (m.isStatic || (!m.isStatic && initializingClasses.contains(cb.declaringClass)))
                            {
                                hasCallingPoint = true
                                if ((abstractLevel > 0 &&
                                                !cb.declaringClass.isJavaLibraryClass && !isAndroidLibrary(cb.declaringClass))
                                        || (
                                                abstractLevel == 0
                                                )
                                )
                                {
                                    if (m != callback) {
                                        //if declaring class of m is parent of any class in initializingClasses
                                        if (callbackDebug)
                                        {
                                            Logger.verb("DEBUG", "Next Callback for virtualMethods: ${cb.signature}")
                                        }
                                        findTopCallingMethod(modMethod, cb, refinedPackageName, abstractLevel + 1, currentLastActivityMethod)
                                    }
                                    else {
                                        if (callbackDebug)
                                        {
                                            Logger.verb("DEBUG", "Next Callback for virtualMethods: ${cb.signature}")
                                        }
                                        findTopCallingMethod(modMethod, cb, refinedPackageName, abstractLevel, currentLastActivityMethod)
                                    }
                                    if (cacheTopCallingMethods.containsKey(cb.signature))
                                    {
                                        cacheTopCallingMethods[cb.signature]!!.forEach {
                                            if (callbackDebug)
                                            {
                                                Logger.verb("DEBUG", "Top caller of ${cb.signature}: $it")
                                            }
                                            addTopCallingMethod(callback.signature, it, cacheTopCallingMethods)
                                            addTopCallingMethod(m.signature, it, cacheTopCallingMethods)
                                        }
//                                Logger.verb("DEBUG", "Top calling method of ${callback.signature} is got from ${cb.signature}")
//                                Logger.verb("DEBUG", "Top calling method of ${m.signature} is got from ${cb.signature}")
                                    }
                                }
                                if (m!=callback)
                                {
                                    if(findingFrameworkManagedCallingPoint2(refinedPackageName, m, modMethod,abstractLevel+1,currentLastActivityMethod))
                                    {
                                        hasCallingPoint = true
                                    }
                                }
                                else
                                {
                                    if(findingFrameworkManagedCallingPoint2(refinedPackageName, m, modMethod,abstractLevel,currentLastActivityMethod))
                                    {
                                        hasCallingPoint = true
                                    }
                                }
                            }

                        }
                    }
                    if (!hasCallingPoint)
                    {
                        val topCallingMethod =
                                if (currentLastActivityMethod!=null)
                                {
                                    currentLastActivityMethod
                                }
                                else
                                {
                                    m
                                }
                        addTopCallingMethod(modMethod.signature, topCallingMethod.signature, topCallingModifiedMethods)
                        addTopCallingMethod(modMethod.signature, topCallingMethod.signature, cacheTopCallingMethods)
                        addTopCallingMethod(topCallingMethod.signature, topCallingMethod.signature, cacheTopCallingMethods)
                        topCallingPointFound = true
//                            Logger.verb("DEBUG", "No more calling method found for ${m.signature}.")
//                            Logger.verb("DEBUG", "Top calling method of ${modMethod.signature} is ${m.signature}")
                    }
                    //currentCallingGraph.pop()
                }
            }
        }
        //currentCallingGraph.pop()
        if (callbackDebug )
        {
            Logger.verb("DEBUG", "End finding top caller for callback: ${callback.signature} ")
            if (callback.signature.equals(debugCallback))
            {
                callbackDebug = false
            }
        }
        return topCallingPointFound
    }

    private fun getMostOuterClass(declaringClass: SootClass): SootClass {
        var c = declaringClass
        while (c.hasOuterClass())
        {
            c = c.outerClass
        }
        return c
    }

    private fun isContainsGetIntent(callback: SootMethod): Boolean {
        if (!callback.hasActiveBody())
            return false
        val iterator = callback.activeBody.units.snapshotIterator()
        while (iterator.hasNext())
        {
            val stm = iterator.next()
            if (stm is JInvokeStmt)
            {
                if (stm.invokeExpr.method.subSignature.contains("getIntent"))
                {
                    return true
                }
            }
            else if (stm is DefinitionStmt)
            {
                if (stm.containsInvokeExpr())
                    if (stm.invokeExpr.method.subSignature.contains("getIntent"))
                    {
                        return true
                    }
            }
        }
        return false
    }

    private fun isApplicationGUIClass(declaringClass: SootClass?): Boolean {
        return (guiAnalysisOutput.flowgraph.hier.applicationActivityClasses.contains(declaringClass)
                || guiAnalysisOutput.flowgraph.hier.applicationFragmentClasses.contains(declaringClass)
                || guiAnalysisOutput.flowgraph.hier.applicationDialogClasses.contains(declaringClass)
                || guiAnalysisOutput.flowgraph.hier.isSubclassOf(declaringClass,Scene.v().getSootClass("android.app.Application")))
    }

    /**
     * Return true if callback is reachable, otherwise return false
     */
    private fun isReachable(callGraph: CallGraph, callback: SootMethod, refinedPackageName: String):Boolean {
        //get all base class virtual methods
        val virtualMethods = ArrayList<SootMethod>()
        val superClasses =  guiAnalysisOutput!!.flowgraph.hier.getSupertypes(callback.declaringClass)
       superClasses.forEach { c ->
//           if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                Logger.verb("DEBUG","Superclass of ${callback.declaringClass.name}: ${c.name}")
           if (c.declaresMethod(callback.subSignature) && c != callback.declaringClass)
            {
//                if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                    Logger.verb("DEBUG", "Virtual method declared: ${callback.subSignature}")
                virtualMethods.add(c.getMethod(callback.subSignature))
            }
           else
           {
//               if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                Logger.verb("DEBUG", "Virtual method not declared: ${callback.subSignature}")
           }
        }
        virtualMethods.add(callback)
        var reachable = false
        for (method in virtualMethods)
        {
            if (method.declaringClass.isJavaLibraryClass || isAndroidLibrary(method.declaringClass))
            {
                reachable = true
                break
            }
            else
            {
                var hasCalling = false
                var sources = Sources(callGraph.edgesInto(method))
                if (!sources.hasNext())
                {
//                    if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                        Logger.verb("DEBUG", "${method.signature} has no calling point")
                }
                while (sources.hasNext()) {
                    val callingMethod = sources.next().method()
                    if (!virtualMethods.contains(callingMethod)) {
//                        if (callback.subSignature.contains("void computeScroll()") || callback.subSignature.contains("void onLayout(boolean,int,int,int,int)"))
//                            Logger.verb("DEBUG", "${method.signature}'s calling point: ${callingMethod.signature}")
                        reachable = true
                        hasCalling = true
                        break
                    }
                }
                if (!hasCalling)
                {
                    if (checkFrameworkManagedCall(refinedPackageName, method)) {
                        reachable = true
                        hasCalling = true
                    }
                }
            }
            if (reachable)
                break
        }
        if (!reachable) {
            GUIUserInteractionClient.unreachableMethods.add(callback.signature)
        }

        return reachable
    }

    private fun checkFrameworkManagedCall(refinedPackageName: String, callback: SootMethod): Boolean {
        val callerClass = callback.declaringClass
        var hasCallingMethod: Boolean = false
        if (isAsyncClass(callerClass)) {
            //find execute point
            //get execute method
            val executeCalls = asyncTaskCalls[callerClass]
            if (executeCalls != null) {
                for (executeCaller in executeCalls) {
                    hasCallingMethod = true
                    break
                }
            }
        }
        if (guiAnalysisOutput!!.flowgraph.hier.isFragmentClass(callerClass)) {
            val activitiesCallingMethods = getActivitiesCallingMethods(callerClass, callback)
            for (m in activitiesCallingMethods) {
                hasCallingMethod = true
                break
            }
        }
        return hasCallingMethod
    }

    private fun addTopCallingMethod(method: String, callback: String, topCallingMethods: HashMap<String, ArrayList<String>>)
    {
        if (!topCallingMethods.containsKey(method))
        {
            topCallingMethods[method] = ArrayList<String>()
        }
        if (!topCallingMethods[method]!!.contains(callback))
        {
            topCallingMethods[method]!!.add(callback)
        }

    }

    private fun isAsyncClass(newClass: SootClass) =
            guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(newClass.name, "android.os.AsyncTask") ||
                    guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(newClass.name, "java.lang.Runnable")

    fun getActivitiesCallingMethods(fragmentClass: SootClass, fragmentMethod: SootMethod): List<SootMethod> {
        val activityMethods = ArrayList<SootMethod>()
        Logger.verb("DEBUG","Fragment class: ${fragmentClass.name}")
        for (node in guiAnalysisOutput!!.flowgraph.allAddFragmentNodes){
            val c = node.fragmentClass
            if (c!=null && c.equals(fragmentClass))
            {
                val activityNode = node.receiver
                val activityClass = ((activityNode as NVarNode).l!!.type as RefType).sootClass
                Logger.verb("DEBUG","Activity added class: ${activityClass.name}")
                getActivityCallingMethod(fragmentMethod, activityClass, activityMethods)
            }
        }
        for (node in guiAnalysisOutput!!.flowgraph.allReplaceFragmentNodes){
            val c = node.fragmentClass
            if (c!=null && c.equals(fragmentClass))
            {
                val activityNode = node.receiver
                val activityClass = ((activityNode as NVarNode).l!!.type as RefType).sootClass
                Logger.verb("DEBUG","Activity added class: ${activityClass.name}")
                getActivityCallingMethod(fragmentMethod,activityClass,activityMethods)
            }
        }
        return activityMethods
    }

    private fun getActivityCallingMethod(fragmentMethod: SootMethod, activityClass: SootClass, activityMethods: ArrayList<SootMethod>) {
        Logger.verb("DEBUG","Fragment method: ${fragmentMethod.subSignature}")
        if (fragmentMethod.subSignature.equals(MethodNames.fragmentOnActivityCreatedSubSig)
                || fragmentMethod.subSignature.equals(MethodNames.fragmentOnCreateViewSubSig)
                || fragmentMethod.subSignature.equals(MethodNames.onActivityCreateSubSig)
                || fragmentMethod.subSignature.equals(MethodNames.onActivityStartSubSig)
                || fragmentMethod.subSignature.equals(MethodNames.fragmentOnAttachSubSig)) {
            if (activityClass.declaresMethod(MethodNames.onActivityCreateSubSig))
            {
                val onCreate = activityClass.getMethod(MethodNames.onActivityCreateSubSig)
                if (onCreate != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onCreate.subSignature}")
                    activityMethods.add(onCreate)
                }
            }
            if (activityClass.declaresMethod(MethodNames.onActivityStartSubSig))
            {
                val onStart = activityClass.getMethod(MethodNames.onActivityStartSubSig)
                if (onStart != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onStart.subSignature}")
                    activityMethods.add(onStart)
                }
            }

        }
        if (fragmentMethod.subSignature.equals(MethodNames.onActivityResultSubSig)) {
            if (activityClass.declaresMethod(MethodNames.onActivityResultSubSig))
            {
                val onActivityResultMethod = activityClass.getMethod(MethodNames.onActivityResultSubSig)
                if (onActivityResultMethod != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onActivityResultMethod.subSignature}")
                    activityMethods.add(onActivityResultMethod)
                }
            }
        }
        if (fragmentMethod.subSignature.equals(MethodNames.onActivityPauseSubSig)) {
            if (activityClass.declaresMethod(MethodNames.onActivityPauseSubSig))
            {
                val onPauseMethod = activityClass.getMethod(MethodNames.onActivityPauseSubSig)
                if (onPauseMethod != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onPauseMethod.subSignature}")
                    activityMethods.add(onPauseMethod)
                }
            }

        }
        if (fragmentMethod.subSignature.equals(MethodNames.onActivityStopSubSig)) {
            if (activityClass.declaresMethod(MethodNames.onActivityStopSubSig))
            {
                val onStopMethod = activityClass.getMethod(MethodNames.onActivityStopSubSig)
                if (onStopMethod != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onStopMethod.subSignature}")
                    activityMethods.add(onStopMethod)
                }
            }
        }
        if (fragmentMethod.subSignature.equals(MethodNames.onOptionsItemSelectedSubSig)) {
            if (activityClass.declaresMethod(MethodNames.onOptionsItemSelectedSubSig))
            {
                val onOptionsItemSelectedMethod = activityClass.getMethod(MethodNames.onActivityPauseSubSig)
                if (onOptionsItemSelectedMethod != null) {
                    Logger.verb("DEBUG","Activity invoked method: ${onOptionsItemSelectedMethod.subSignature}")
                    activityMethods.add(onOptionsItemSelectedMethod)
                }
            }
            else
            {
                //Logger.verb("DEBUG","${activityClass.name} does not declare ${MethodNames.onOptionsItemSelectedSubSig}")
            }
        }
    }

    private fun recordAsynsTaskExecuteInvoke(invokeExpr: InvokeExpr?, m: SootMethod) {
        if (invokeExpr is InstanceInvokeExpr) {
            val receiver = invokeExpr.base as Local
            if (allocAsyncTaskMap.contains(receiver)) {
                val asyncTaskClass = allocAsyncTaskMap[receiver]
                if (!asyncTaskCalls.contains(asyncTaskClass)) {
                    asyncTaskCalls[asyncTaskClass!!] = ArrayList()
                }
                asyncTaskCalls[asyncTaskClass!!]!!.add(m)
                Logger.verb("DEBUG","${asyncTaskClass.name} called by ${m.signature}")
            }
        }
    }

    @Deprecated(message = "not used")
    private fun findCallingEvents(modMethod: SootMethod, callBack: SootMethod, refinedPackageName: String): Boolean {
        //
        /*if(allMethodInvocation.containsKey(modMethod.signature))
        {
            allMethodInvocation[modMethod.signature] = ArrayList<HashMap<String,String>>()
        }*/
        currentCallingGraph.push(callBack.signature)
        if (modMethod.equals(callBack)) //first call
        {
            if (!GUIUserInteractionClient.modMethodInvocation.containsKey(modMethod.signature))
            {
                val eventHandlers = getEventHandlers(modMethod.signature)
                if (eventHandlers.size>0)
                {
                    //Logger.verb("DEBUG", "${modMethod.signature} is event handler")
                    addMethodInvocation(modMethod.signature, eventHandlers, GUIUserInteractionClient.modMethodInvocation)
                    addMethodInvocation(modMethod.signature, eventHandlers, GUIUserInteractionClient.cacheMethodInvocation)
                    return true
                }

            }
        }
        if (!modMethod.equals(callBack) && GUIUserInteractionClient.cacheMethodInvocation.containsKey(callBack.signature))
        // callback method is not a GUIElement but it can be processed before
        {
            // if it is, add callback's handlers
            //Logger.verb("DEBUG", "${callBack.signature} has invocation ")
            //Logger.verb("DEBUG", "${modMethod.signature}'s invocation is ${cacheMethodInvocation[callBack.signature]!!}")
            addMethodInvocation(modMethod.signature, GUIUserInteractionClient.cacheMethodInvocation[callBack.signature]!!, GUIUserInteractionClient.modMethodInvocation)
            addMethodInvocation(modMethod.signature, GUIUserInteractionClient.cacheMethodInvocation[callBack.signature]!!, GUIUserInteractionClient.cacheMethodInvocation)
        } else{
            //Logger.verb("DEBUG", "Callback: ${callBack.signature}")
            if (notAppearInTargetCallingMethods.contains(callBack.signature))
                return false
            val eventHandlers = getEventHandlers(callBack.signature)
            if (eventHandlers.size>0)
            {
                //if callback is an event handler, add event handlers related to callback
                //Logger.verb("DEBUG", "${modMethod.signature}'s invocation is ${eventHandlers}")
                addMethodInvocation(modMethod.signature, eventHandlers, GUIUserInteractionClient.modMethodInvocation)
                addMethodInvocation(modMethod.signature, eventHandlers, GUIUserInteractionClient.cacheMethodInvocation)
                //add callback invocation to cache
                addMethodInvocation(callBack.signature, eventHandlers, GUIUserInteractionClient.cacheMethodInvocation)
            } else {
                //Logger.verb("DEBUG", "${callBack.signature} is not an event handler.")
                // if not, try to find its invoked callback
                val callGraph = Scene.v().callGraph
                //val sootMethod = Scene.v().getMethod(callBack.signature)
                val sources = Sources(callGraph.edgesInto(callBack))
                var hasCallingPoint = false
                while (sources.hasNext()) {
                    val method = sources.next().method()
                    if (!method.signature.equals(callBack.signature)
                            && method.declaringClass.name.startsWith(refinedPackageName)
                            && !currentCallingGraph.contains(method.signature)
                            && !notAppearInTargetCallingMethods.contains(method.signature)
                            && guiAnalysisOutput!!.flowgraph.hier.appClasses.contains(method.declaringClass)) {
                        //Logger.verb("DEBUG", "Normal caller: ${method.signature}")
                        if(findCallingEventsAndLog("", modMethod, method, refinedPackageName, callBack,false))
                        {
                            hasCallingPoint = true
                        }
                    }
                }
                //Logger.verb("DEBUG", "Try finding framework managed calling point: ${callBack.signature}")
                if (findingFrameworkManagedCallingPoint1(refinedPackageName, callBack, modMethod))
                {
                    hasCallingPoint = true
                }
                if (!hasCallingPoint)
                {
                    notAppearInTargetCallingMethods.add(callBack.signature)
                    //Logger.verb("DEBUG", "Cannot get calling point for: ${callBack.signature}")
                    return false
                }
            }
        }
        currentCallingGraph.pop()
        return true
    }

    @Deprecated(message = "not used")
    private fun findingFrameworkManagedCallingPoint1(refinedPackageName: String, callBack: SootMethod, modMethod: SootMethod): Boolean {
        val callerClass = callBack.declaringClass
        var hasCallingPoint = false
        if (guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(callerClass, Scene.v().getSootClass("android.os.AsyncTask"))) {
            //Logger.verb("DEBUG", "Callback is belong to AsyncTask: " + callerClass)
            //find execute point
            //get execute method
            val executeCalls = asyncTaskCalls[callerClass]
            if (executeCalls != null) {
                for (executeCaller in executeCalls) {
                    if (!notAppearInTargetCallingMethods.contains(executeCaller.signature) && !currentCallingGraph.contains(executeCaller.signature))
                    {
                        if(findCallingEventsAndLog("ExecuteCaller: " + executeCaller, modMethod, executeCaller, refinedPackageName, callBack, false))
                        {
                            hasCallingPoint = true
                        }
                    }

                }
            }
        }
        if (guiAnalysisOutput!!.flowgraph.hier.isFragmentClass(callerClass)) {
            val activitiesCallingMethods = getActivitiesCallingMethods(callerClass, callBack)
            for (m in activitiesCallingMethods) {
                if (!notAppearInTargetCallingMethods.contains(m.signature) && !currentCallingGraph.contains(m.signature))
                {
                    val message = "Activity calling fragment: " + m
                    if(findCallingEventsAndLog(message, modMethod, m, refinedPackageName, callBack, true))
                    {
                        hasCallingPoint = true
                    }
                }
            }

        }
        if (!hasCallingPoint) {

            return false;
        }
        return true;


    }

    private fun findingFrameworkManagedCallingPoint2(refinedPackageName: String, callback: SootMethod, modMethod: SootMethod,abstractLevel: Int, lastActivityMethod: SootMethod?): Boolean {
        val callerClass = callback.declaringClass
        var topCallingMethodFound: Boolean = false
        if (isAsyncClass(callerClass)) {
            //find execute point
            //get execute method
            //Logger.verb("DEBUG","Async class: $callerClass")
            val executeCalls = asyncTaskCalls[callerClass]
            if (executeCalls != null) {
                for (executeCaller in executeCalls) {
                    if (callbackDebug)
                    {
                        Logger.verb("DEBUG", "Next Callback for async : ${executeCaller.signature}")
                    }
                    topCallingMethodFound = true
                    findTopCallingMethod(modMethod, executeCaller, refinedPackageName,abstractLevel = abstractLevel,lastActivityMethod = lastActivityMethod)
                    if (cacheTopCallingMethods.containsKey(executeCaller.signature))
                    {
                        cacheTopCallingMethods[executeCaller.signature]!!.forEach {
                            addTopCallingMethod(callback.signature, it, cacheTopCallingMethods)
                        }
//                        Logger.verb("DEBUG", "Top calling method of ${callback.signature} is got from ${executeCaller.signature}")
                    }
                        //addTopCallingMethod(callback = executeCaller.signature, method = callback.signature, topCallingMethods = cacheTopCallingMethods)
/*                    if (cacheTopCallingMethods.containsKey(executeCaller.signature))
                    {
                        cacheTopCallingMethods[executeCaller.signature]!!.forEach {
                            addTopCallingMethod(modMethod.signature,it, topCallingModifiedMethods)
                            addTopCallingMethod(modMethod.signature, it, cacheTopCallingMethods)
                            addTopCallingMethod(callback.signature, it, cacheTopCallingMethods)
                            //   Logger.verb("DEBUG", "From cache, top calling method of ${modMethod.signature} is ${it}")
                        }
                    }
                    else
                    {
                        if(!findTopCallingMethod(modMethod, executeCaller, refinedPackageName))
                        {
                            addTopCallingMethod(callback = executeCaller.signature, method = callback.signature, topCallingMethods = cacheTopCallingMethods)
                        }
                    }*/
                }
            }
        }
        if (guiAnalysisOutput!!.flowgraph.hier.isSubclassOf(callerClass.name,"android.content.BroadcastReceiver"))
        {

        }
        if (guiAnalysisOutput!!.flowgraph.hier.isFragmentClass(callerClass)) {
            val activitiesCallingMethods = getActivitiesCallingMethods(callerClass, callback)
            for (m in activitiesCallingMethods) {
                topCallingMethodFound = true
                //val message = "Activity calling fragment: " + m
                //Logger.verb("DEBUG", message)
                if (callbackDebug)
                {
                    Logger.verb("DEBUG", "Next Callback for activityCallingMethod : ${m.signature}")
                }
                findTopCallingMethod(modMethod, m, refinedPackageName,abstractLevel,lastActivityMethod)
               /* if (cacheTopCallingMethods.containsKey(m.signature))
                {
                    cacheTopCallingMethods[m.signature]!!.forEach {
                        addTopCallingMethod(modMethod.signature,it, topCallingModifiedMethods)
                        addTopCallingMethod(modMethod.signature, it, cacheTopCallingMethods)
                        addTopCallingMethod(callback.signature, it, cacheTopCallingMethods)
                        // Logger.verb("DEBUG", "From cache, top calling method of ${modMethod.signature} is ${it}")
                    }
                }
                else
                {
                    if(!findTopCallingMethod(modMethod, m, refinedPackageName))
                    {
                        addTopCallingMethod(callback = m.signature, method = callback.signature, topCallingMethods = cacheTopCallingMethods)
                    }
                }*/
            }
        }
        return topCallingMethodFound
    }

    private fun findCallingEventsAndLog(message: String, modMethod: SootMethod, m: SootMethod, refinedPackageName: String, callBack: SootMethod, log: Boolean): Boolean {
        if (log)
        //Logger.verb("DEBUG", message)
            if(findCallingEvents(modMethod, m, refinedPackageName))
            {
                //add eventhandlers of each method for current callback invocation
                if (GUIUserInteractionClient.cacheMethodInvocation.contains(m.signature))
                    addMethodInvocation(callBack.signature, GUIUserInteractionClient.cacheMethodInvocation[m.signature]!!, GUIUserInteractionClient.cacheMethodInvocation)
                return true
            }
        return false
    }

    val allocAsyncTaskMap = HashMap<Local, SootClass>()
    val asyncTaskCalls = HashMap<SootClass, ArrayList<SootMethod>>()
    fun recordAsyncTaskUsage(){
        for (c in Scene.v().applicationClasses){
            for (m in c.methods)
            {
                if (!m.hasActiveBody())
                    continue
                val body = m.activeBody
                val iterator = body.units.iterator()
                while (iterator.hasNext())
                {
                    val unit = iterator.next()
                    if (unit is DefinitionStmt)
                    {
                        if (unit.containsInvokeExpr())
                        {
                            val invokeExpr = unit.invokeExpr
                            recordAsynsTaskExecuteInvoke(invokeExpr, m)
                        }
                        val rightOp = unit.rightOp
                        if (rightOp is NewExpr)
                        {
                            val newClass = rightOp.baseType.sootClass
                            if (isAsyncClass(newClass))
                            {
                                val leftOp = unit.leftOp as Local
                                allocAsyncTaskMap[leftOp] = newClass
                            }
                        }
                        continue
                    }
                    if (unit is InvokeStmt)
                    {
                        val invokeExpr = unit.invokeExpr
                        recordAsynsTaskExecuteInvoke(invokeExpr,m)
                    }
                }
            }
        }
    }

    private fun addMethodInvocation(method: String, events: ArrayList<WTGEdge>, methodInvocations: HashMap<String, ArrayList<WTGEdge>>) {
        for (e in events)
        {
            if (!methodInvocations.containsKey(method))
            {
                methodInvocations[method] = ArrayList<WTGEdge>()
            }
            val eventExisted = methodInvocations[method]!!.contains(e)
            if (!eventExisted) {
                methodInvocations[method]!!.add(e)
                if (!e.eventType.isImplicit)
                //Logger.verb("DEBUG", "Event handler: ${e.wtgHandlers.first().eventHandler.signature}")
                else
                {
                    if (!e.eventHandlers.isEmpty() && e.eventHandlers.firstOrNull()!=null)
                    {
                        // Logger.verb("DEBUG", "Event handler: ${e.eventHandlers.first().signature}")
                    }
                    else
                    {
                        //  Logger.verb("DEBUG", "Event: ${e.eventType.name}")
                    }
                }

            }
        }
    }

    internal fun getEventHandlers (handler: String): ArrayList<WTGEdge> {
//        if (!GUIUserInteractionClient.allEventHandlers.contains(handler))
//            return ArrayList()
        var guiElements= ArrayList<WTGEdge>()
        val i = GUIUserInteractionClient.widgetEvents.iterator()
        if (i != null) {
            while (i.hasNext()) {
                val element = i.next()
                val wtgEdges = element.value
                for (e in wtgEdges) {
                    if (e.eventHandlers.map { it.signature }.contains(handler)) {
                        if (!guiElements.contains(e))
                            guiElements.add(e)
                    }
                }
            }
        }
        if (!GUIUserInteractionClient.allMeaningfullEventHandlers.contains(handler) && guiElements.isEmpty())
        {
            GUIUserInteractionClient.allCallbacks.filter {
                it.second.contains(handler)
            }.forEach {
                val invokedEventHandler = it.first
                if (!guiElements.contains(invokedEventHandler)) {
                    guiElements.add(invokedEventHandler)
//                    if (handler.equals("<com.teleca.jamendo.activity.PlaylistActivity: boolean onContextItemSelected(android.view.MenuItem)>"))
//                    {
//                        Logger.verb("DEBUG",invokedEventHandler.toString())
//                    }
                }
            }
        }
        return guiElements
    }

    fun isAndroidLibrary(sootClass: SootClass): Boolean{
        val name = sootClass.name
        if (name.startsWith("android.")
                || name.startsWith("androidx."))
                return true
        return false
    }
}