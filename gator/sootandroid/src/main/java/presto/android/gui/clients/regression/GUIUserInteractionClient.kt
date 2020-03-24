    /*
 * GUIUserInteractionClient.kt - part of the GATOR project
 *
 * Copyright (c) 2019 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */

package presto.android.gui.clients.regression

    import org.json.JSONArray
    import org.json.JSONObject
    import org.slf4j.LoggerFactory
    import presto.android.Configs
    import presto.android.Logger
    import presto.android.gui.GUIAnalysisClient
    import presto.android.gui.GUIAnalysisOutput
    import presto.android.gui.PropertyManager
    import presto.android.gui.clients.energy.VarUtil
    import presto.android.gui.clients.regression.helper.CallbackFinder
    import presto.android.gui.clients.regression.helper.JavaSignatureFormatter
    import presto.android.gui.graph.*
    import presto.android.gui.listener.EventType
    import presto.android.gui.wtg.EventHandler
    import presto.android.gui.wtg.WTGAnalysisOutput
    import presto.android.gui.wtg.WTGBuilder
    import presto.android.gui.wtg.ds.WTG
    import presto.android.gui.wtg.ds.WTGEdge
    import presto.android.gui.wtg.flowgraph.NLauncherNode
    import presto.android.xml.DefaultXMLParser
    import presto.android.xml.XMLParser
    import soot.*
    import java.io.File
    import java.io.IOException
    import java.nio.file.Files
    import java.nio.file.Path
    import java.nio.file.Paths
    import java.util.*
    import kotlin.collections.ArrayList
    import kotlin.collections.HashMap
    import kotlin.collections.HashSet

    public class GUIUserInteractionClient : GUIAnalysisClient {

        companion object {

            val widgetEvents = HashMap<String, ArrayList<WTGEdge>>()  //widget -> list of (source event)
            val allWindow_Widgets = HashMap<String, HashMap<Int, Any>>()
            val allWidgetIds = HashMap<Int, NIdNode>()
            val allActivities= HashMap<Int, String>()
            val allDialogs = HashMap<Int, String>()
            val allActivityOptionMenuItems = HashMap<String, ArrayList<String>>()
            val allActivityContextMenuItems = HashMap<String, ArrayList<String>>()
            val allWindow_Widget_EventHandlers = HashMap<String,HashMap<String,HashMap<String, ArrayList<String>>>>() // window -> widget -> event
            val allEventHandlers = ArrayList<String>()
            val allMeaningfullEventHandlers = ArrayList<String>()
            val allActivity_Dialogs = HashMap<String, ArrayList<String>>()
            val allActivityTransitions = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allActivityDialogTransitions = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allActivityContextMenuOpen = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allMenuDialogTransition = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allMenuActivityTransition = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allDialogActivityTransitions = HashMap<String, ArrayList<HashMap<String, String>>>()
            val allCallbacks = ArrayList<Pair<WTGEdge,LinkedList<String>>>()
            val allTransitions = HashMap<String, ArrayList<HashMap<String,String>>>()
            val allActivityNodes = HashMap<Int,String>()
            val allFragmentNodes = HashMap<Int, String>()
            val allResourceStrings = ArrayList<String>()
            val allEventHandler_WTGEdgeMap = HashMap<SootMethod, ArrayList<WTGEdge>>()
            //val launchActivity = ArrayList<>
            val topCallingModifiedMethods = HashMap<String, ArrayList<String>>()
            val cacheMethodInvocation = HashMap<String, ArrayList<WTGEdge>>()
            // key: source value: hashMapOf(widget, events)
             val modMethodInvocation = HashMap<String, ArrayList<WTGEdge>>()
            val notAppearInTargetCallingMethods = ArrayList<String>()
             val modifiedMethods = ArrayList<String>()
            val allNodes = ArrayList<String>()
            val allOpNodes = ArrayList<String>()
            val allLayoutIdNodes = ArrayList<String>()
            val allInflateNodes = ArrayList<String>()
            val allReachedWindow = ArrayList<Int>()


            val unreachableMethods = ArrayList<String>()
            val menuItemsTexts = HashMap<String, ArrayList<String>>() //menuitemNode -> texts
            val widgetTexts = HashMap<String, ArrayList<String>>()

            var guiAnalysisOutput: GUIAnalysisOutput?=null
        }

        val currentCallingGraph = Stack<String>()
        private val log by lazy { LoggerFactory.getLogger(this::class.java) }
        override fun run(output: GUIAnalysisOutput) {
            guiAnalysisOutput = output
            Logger.verb("INFO", "GUIUserInteractionClient start")
            initWTG(output)
            val apkPath = Paths.get(Configs.project)
            val apkName = output.appPackageName
            val diffFile = Files.list(apkPath.parent).filter { it.fileName.toString().contains(apkName) && it.fileName.toString().endsWith("-diff.json") }.findFirst().orElse(null)
            if (diffFile != null)
            {
                readAppDiffFile(diffFile.toString(), apkName)
                findModifiedMethodInvocation(apkName)
            }
            Logger.verb("DEBUG", "Number of event handler: ${allMeaningfullEventHandlers.size}")
            ComponentRelationCalculation.instance.computeComponentCorrelationScore()
            writeInstrumentationList(apkName, Paths.get("./"))
        }

        private fun initWTG(output: GUIAnalysisOutput) {
            VarUtil.v().guiOutput = output
            output.flowgraph.allNNodes.filter { it is NOpNode }.forEach {
                allOpNodes.add(it.toString())
            }
            output.flowgraph.allNLayoutIdNodes.forEach { t, u ->
                allLayoutIdNodes.add(u.toString())
                u.successors.forEach {
                    if (!it.successors.isEmpty())
                    {
                        allInflateNodes.add(u.toString()+ "---[INFLATE]---" + it.successors.first().toString())
                    }

                }
            }
            val wtgBuilder = WTGBuilder()
            wtgBuilder.build(output)
            val wtgAO = WTGAnalysisOutput(output, wtgBuilder)
            val wtg = wtgAO.wtg
            //get All WidgetIds
            output.flowgraph.allNWidgetIdNodes.forEach { t, u ->
                allWidgetIds[t] = u
            }



            //get all Activities
            var activityId: Int = 0
            val map = output.activities.map { activityId++ to it.name }.toMap()
            map.forEach { t, u -> allActivities[t] = u }
            output.flowgraph.allNNodes.forEach {
                if (it is NWindowNode || it is NOptionsMenuNode || it is NContextMenuNode)
                {
                    getAllWindowWidgets(it as NObjectNode)
                }
            }
            output.flowgraph.allNActivityNodes.forEach {
                allActivityNodes[it.value.id] = it.value.toString()

            }
            output.flowgraph.allNFragmentNodes.forEach { t, u ->
                allFragmentNodes[u.id] = u.toString()
            }

            getAllStringTexts()
            //get all dialogs
            getAllDialogs(output, wtg)
            //get all Option Menu Items
            getAllOptionMenuItems(output)

            getAllContextMenuItems(output)
            wtg.nodes.forEach {
                allNodes.add(it.toString())
            }
            val edges = wtg.edges
            for (e in edges) {
                getAllTransition(e)
                //get all activity-dialog transitions
                getAllActivityDialogOpen(e)

                //get all activity-to-activity transitions
                getAllActivityTransition(e)

                //get all activity-to-contextmenu transitions
                if (e.sourceNode.window is NActivityNode &&
                        e.targetNode.window is NMenuNode) {
                    val sourceActivity = e.sourceNode.window.classType.name
                    val targetContextMenu = e.targetNode.window.classType.name
                    if (!allActivityContextMenuOpen.contains(sourceActivity)) {
                        allActivityContextMenuOpen[sourceActivity] = ArrayList()
                    }
                    val activityTransitions = allActivityContextMenuOpen[sourceActivity]!!
                    val transition = HashMap<String, String>()
                    transition["target"] = targetContextMenu
                    transition["widget"] = e.guiWidget.toString()
                    transition["action"] = e.eventType.toString()
                    activityTransitions.add(transition)
                }

                //get all menu item-dialog open
                if (e.sourceNode.window is NMenuNode &&
                        e.targetNode.window is NDialogNode) {
                    val source = e.sourceNode.window.classType.name
                    val target = e.targetNode.window.classType.name
                    if (!allMenuDialogTransition.contains(source)) {
                        allMenuDialogTransition[source] = ArrayList()
                    }
                    val transitions = allMenuDialogTransition[source]!!
                    val transition = HashMap<String, String>()
                    transition["target"] = target
                    transition["widget"] = e.guiWidget.toString()
                    transition["action"] = e.eventType.toString()
                    transitions.add(transition)
                }

                //get all menu activity open
                if (e.sourceNode.window is NMenuNode &&
                        e.targetNode.window is NActivityNode) {
                    val source = e.sourceNode.window.classType.name
                    val target = e.targetNode.window.classType.name
                    if (!allMenuActivityTransition.contains(source)) {
                        allMenuActivityTransition[source] = ArrayList()
                    }
                    val transitions = allMenuActivityTransition[source]!!
                    val transition = HashMap<String, String>()
                    transition["target"] = target
                    transition["widget"] = e.guiWidget.toString()
                    transition["action"] = e.eventType.toString()
                    transitions.add(transition)
                }

                //get all dialog to source transition
                if (e.sourceNode.window is NDialogNode && e.targetNode.window is NActivityNode
                ) {
                    val sourceDialog = e.sourceNode.window.classType.name
                    val targetActivity = e.targetNode.window.classType.name

                    if (!allDialogActivityTransitions.contains(sourceDialog)) {
                        allDialogActivityTransitions[sourceDialog] = ArrayList<HashMap<String, String>>()
                    }
                    val transition = HashMap<String, String>()
                    transition["target"] = targetActivity
                    transition["widget"] = e.guiWidget.toString()
                    transition["action"] = e.eventType.toString()
                    allDialogActivityTransitions[sourceDialog]!!.add(transition)
                }


                val widget = e.guiWidget
                var meaningfulEvent = true
//                    if (e.guiWidget !is NActivityNode && e.guiWidget !is NDialogNode && e.guiWidget.idNode == null)
//                    {
//                        meaningfulEvent = false
//                    }
                if (meaningfulEvent)
                {
                    processMeaningfulEvent(widget, e)
                    ComponentRelationCalculation.instance.registerEvent(e)
                }
            }
        }

        private fun getAllStringTexts() {
            val parser = XMLParser.Factory.getXMLParser() as DefaultXMLParser

            //parser.intAndStringValues
            parser.allStringLiterals.filter{it != null}.forEach {
                if (!allResourceStrings.contains(it)) {
                    allResourceStrings.add(it)
                }
            }

            parser.intAndStringValues.forEach { _, s ->
                if (!allResourceStrings.contains(s))
                    allResourceStrings.add(s)
            }
            parser.sysIntAndStringValues.forEach { _, s ->
                if (!allResourceStrings.contains(s))
                    allResourceStrings.add(s)
            }
            parser.intAndMultiLangStringValues.forEach { _, arrS ->
                arrS.forEach {
                    if (!allResourceStrings.contains(it))
                        allResourceStrings.add(it)
                }
            }
            parser.sysIntAndMultiLangStringValues.forEach { _, arrS ->
                arrS.forEach {
                    if (!allResourceStrings.contains(it))
                        allResourceStrings.add(it)
                }
            }
        }

        fun isNotSupportedEventType(eventType: EventType): Boolean =
            when (eventType){
                EventType.implicit_async_event,
                EventType.implicit_hierarchy_change,
                EventType.implicit_lifecycle_event,
                EventType.implicit_power_event,
               // EventType.implicit_back_event,
                EventType.implicit_system_ui_change,
                EventType.implicit_time_tick,
                EventType.press_key,
                EventType.dialog_dismiss,
                EventType.dialog_negative_button, // TODO(tony): remove soon
                EventType.dialog_neutral_button,
                    EventType.dialog_positive_button,
                EventType.dialog_press_key -> true
                else -> false
            }

        private fun processMeaningfulEvent(widget: NObjectNode, e: WTGEdge) {
//            if (isNotSupportedEventType(e.eventType) )
//                return
            if (!widgetEvents.containsKey(widget.toString())) {
                widgetEvents[widget.toString()] = ArrayList<WTGEdge>()
            }
            widgetEvents[widget.toString()]!!.add(e)

            val window = e.sourceNode.window

            e.wtgHandlers.forEach {
                if (it != null && it.eventHandler != null) {
                    if (!allMeaningfullEventHandlers.contains(it.eventHandler.signature)) {
                        allMeaningfullEventHandlers.add(it.eventHandler.signature)
//                        Logger.verb("DEBUG","Event handler: ${it.eventHandler.signature}")
                    }
                    if (!allEventHandler_WTGEdgeMap.containsKey(it.eventHandler))
                    {
                        allEventHandler_WTGEdgeMap.put(it.eventHandler, ArrayList())
                    }
                    if (!allEventHandler_WTGEdgeMap[it.eventHandler]!!.contains(e))
                        allEventHandler_WTGEdgeMap[it.eventHandler]!!.add(e)
                    addElementToAllWindow_Widget_EventHandler(window.classType.name, it)
                }
            }
            if (e.callbacks.size > 0) {
                val callbacks = Pair<WTGEdge, LinkedList<String>>(e, LinkedList(e.callbacks.map {
                    it.eventHandler?.signature ?: it.event.name
                }))
                callbacks.second.addFirst(e.eventHandlers.firstOrNull()?.signature ?: e.eventType.name)
                allCallbacks.add(callbacks)
                //ComponentRelationCalculation.instance.eventHandlers.addAll(e.callbacks.map { it.eventHandler })
            }
            e.callbacks.forEach {
                if (it.event != null) {
                    val windowName = it.window.classType.name
                    addElementToAllWindow_Widget_EventHandler(windowName, it)
                }
            }
        }

        internal fun readAppDiffFile(filename: String, refinedPackageName: String){
            val appDiffFile = File(filename)
            if (!appDiffFile.exists())
            {
                log.error("Cannot find app diff file: $filename")
                throw Exception()
            }

            val appdiffJson = JSONObject(String(Files.readAllBytes(appDiffFile.toPath())))
            val modMethods =  appdiffJson.get("methodsChanged") as JSONArray
            for (m in modMethods)
            {
                val sootSignature = JavaSignatureFormatter.translateJavaLowLevelSignatureToSoot(m.toString())
                if (!Scene.v().containsMethod(sootSignature))
                    continue
                val declaredClass = Scene.v().getMethod(sootSignature).declaringClass
                if (!declaredClass.isApplicationClass )
                    continue;

                modifiedMethods.add(sootSignature)
            }
        }

        private fun findModifiedMethodInvocation(refinedPackageName: String){
            val callbackFinder = CallbackFinder(guiAnalysisOutput!!, allMeaningfullEventHandlers, widgetEvents, notAppearInTargetCallingMethods, topCallingModifiedMethods)
            modifiedMethods.forEach {
                if (!Scene.v().containsMethod(it))
                {
                    notFoundModifiedMethods.add(it)
                }
            }
            modMethodInvocation.putAll(callbackFinder.findMethodInvocation(refinedPackageName, modifiedMethods))
        }


        private fun getAllWindowWidgets(window: NObjectNode) {
            if ((window is NWindowNode || window is NDialogNode) && window.classType.isApplicationClass && !guiAnalysisOutput!!.flowgraph.hier.isFragmentClass(window.classType))
                ComponentRelationCalculation.instance.windows.add(window)
            val activity = window.classType
            //if (window is NWindowNode || window is NDialogNode || window is NOptionsMenuNode || window is NContextMenuNode) {
//                if (window.children.filter { it is NInflNode || it is  NViewAllocNode }.size > 0) {
//                    if (window.children.filter { it is NInflNode || it is  NViewAllocNode }.size > 1) {
//                        val windowKey = "$activity - ${window.toString()}"
//                        if (!allWindow_Widgets.containsKey(window.toString())) {
//                            allWindow_Widgets[window.toString()] = HashMap()
//                        }
//                        val widgetsInActivity = allWindow_Widgets[window.toString()]
//
//                        val layouts = window.children.filter { it is NInflNode || it is NViewAllocNode}
//                        layouts.forEach {
//                            getAllChildrenInWindow(it, widgetsInActivity!!)
//                        }
//                    } else {
//                        val layout = window.children.first { it is NInflNode || it is NViewAllocNode }
//                        val windowKey = "$activity - ${layout.toString()}"
//                        if (!allWindow_Widgets.containsKey(window.toString())) {
//                            allWindow_Widgets[window.toString()] = HashMap()
//                        }
//                        val widgetsInActivity = allWindow_Widgets[window.toString()]
//                        getAllChildrenInWindow(layout, widgetsInActivity!!)
//                    }
//                } else {
//                    log.warn("The source $activity doesn't have layout.")
//                }
            //}
            if (!allWindow_Widgets.contains(window.toString()))
            {
                allWindow_Widgets[window.toString()] = HashMap()
            }

            val allEventHandlerMethods = GUIUserInteractionClient.guiAnalysisOutput!!.getAllEventsAndTheirHandlers(window).map { it.value }
            allEventHandlerMethods.forEach {
                it.forEach {
                    if(!allEventHandlers.contains(it.signature))
                    {
                        allEventHandlers.add(it.signature)
                        //ComponentRelationCalculation.instance.eventHandlers.add(it)
                    }

                }
            }

//            getAllChildrenFlattenInWindow(window, allWindow_Widgets[window.toString()]!!,0)
            getAllChildrenInWindow(window, allWindow_Widgets[window.toString()]!!, HashSet())

        }

        private fun getAllTransition(e:WTGEdge)
        {
            if (isNotSupportedEventType(e.eventType) && e.eventType != EventType.implicit_launch_event)
                return
            val sourceWindow = e.sourceNode.window.toString()
            val targetWindow = e.targetNode.window.toString()
            if (!allReachedWindow.contains(e.targetNode.window.id))
            {
                allReachedWindow.add(e.targetNode.window.id)
            }
            if (!allTransitions.contains(sourceWindow)) {
                allTransitions[sourceWindow] = ArrayList()
            }
            if (e.guiWidget !is NActivityNode && e.guiWidget !is NDialogNode && e.guiWidget !is NLauncherNode && e.guiWidget.idNode == null)
            {
                return
            }

            val transitions = allTransitions[sourceWindow]!!
            val transition = HashMap<String, String>()
            transition["target"] = targetWindow
            transition["widget"] = e.guiWidget.toString()
            transition["action"] = e.eventType.toString()
            transitions.add(transition)
//            }
        }
        private fun getAllActivityTransition(e: WTGEdge) {
            if ((e.sourceNode.window is NActivityNode)
                && e.targetNode.window is NActivityNode
            ) {
                val sourceActivity = e.sourceNode.window.classType.name
                val targetActivity = e.targetNode.window.classType.name
                //if (e.targetNode.window is NActivityNode && !targetActivity.equals(sourceActivity))
                if (!allActivityTransitions.contains(sourceActivity)) {
                    allActivityTransitions[sourceActivity] = ArrayList()
                }
                val activityTransitions = allActivityTransitions[sourceActivity]!!
                val transition = HashMap<String, String>()
                transition["target"] = targetActivity
                transition["widget"] = e.guiWidget.toString()
                transition["action"] = e.eventType.toString()
                activityTransitions.add(transition)
            }
        }

        private fun getAllActivityDialogOpen(e: WTGEdge) {
            if ((e.sourceNode.window is NActivityNode)
                && e.targetNode.window is NDialogNode
            ) {
                val activityName = e.sourceNode.window.classType.name
                val dialogName = e.targetNode.window.classType.name

                if (!allActivityDialogTransitions.contains(activityName)) {
                    allActivityDialogTransitions[activityName] = ArrayList()
                }
                val transition = HashMap<String, String>()
                transition["target"] = dialogName
                transition["widget"] = e.guiWidget.toString()
                transition["action"] = e.eventType.toString()
                allActivityDialogTransitions[activityName]!!.add(transition)
            }
        }

        private fun getAllOptionMenuItems(output: GUIAnalysisOutput) {
            allActivities.forEach { k, v -> allActivityOptionMenuItems[v] = ArrayList()

                val activitySootClass = output.activities.find { it.name == v }
                val optionMenuNode = output.getOptionsMenu(activitySootClass)
                if (optionMenuNode != null)
                    getOptionsMenuItems(optionMenuNode!!,v)
            }
        }

        private fun getAllContextMenuItems(output: GUIAnalysisOutput) {
            getContextMenuItems(output)

        }

        internal fun getOptionsMenuItems(parentNode: NNode, activityName: String ){
            val parser = XMLParser.Factory.getXMLParser() as DefaultXMLParser
            val menuItems = parentNode.children
            menuItems.forEach {
                if (it is NMenuItemInflNode)
                {
                    allActivityOptionMenuItems[activityName]!!.add(it.toString())
                    Logger.verb("DEBUG","Menu item: ${it.toString()}")
                    if (it.attrs.containsKey("android:title"))
                    {
                        val menuItemStringValues = ArrayList<String>()
                        val stdString = parser.convertAndroidTextToString(it.attrs["android:title"])
                        if (stdString != null)
                            menuItemStringValues.add(stdString)
                        //Try get multi language string
                        val mulitlangStrings = parser.convertAndroidTextToMultiLangString(it.attrs["android:title"])
                        if (mulitlangStrings != null)
                            menuItemStringValues.addAll(mulitlangStrings)
                        menuItemsTexts[it.toString()] = menuItemStringValues
                    }
                }
                else
                {
                    getOptionsMenuItems(it, activityName)
                }
            }
        }

        internal fun getContextMenuItems(output: GUIAnalysisOutput){
            val parser = XMLParser.Factory.getXMLParser() as DefaultXMLParser
            val menuItems = output.flowgraph.allMenuItems
            menuItems.forEach {
                //Logger.verb("DEBUG","Menu item: ${it.toString()}")
                if (it is NMenuItemInflNode)
                {

                    if (it.attrs.containsKey("android:title"))
                    {
                        val menuItemStringValues = ArrayList<String>()
                        val stdString = parser.convertAndroidTextToString(it.attrs["android:title"])
                        if (stdString != null)
                            menuItemStringValues.add(stdString)
                        //Try get multi language string
                        val mulitlangStrings = parser.convertAndroidTextToMultiLangString(it.attrs["android:title"])
                        if (mulitlangStrings != null)
                            menuItemStringValues.addAll(mulitlangStrings)
                        menuItemsTexts[it.toString()] = menuItemStringValues
                    }
                }
            }
        }
        private fun getAllDialogs(output: GUIAnalysisOutput, wtg: WTG) {
            output.flowgraph.allNDialogNodes.forEach { k, v ->
                allDialogs[v.id] = v.c.name + " " + v.allocMethod.declaringClass.name
                val dialogName = v.classType.name
                val owners = wtg.getOwnerActivity(wtg.getNode(v))

                owners.forEach {
                    val activityName = it.classType.name
                    if (!allActivity_Dialogs.contains(it.toString())) {
                        allActivity_Dialogs[it.toString()] = ArrayList()
                    }
                    if (!allActivity_Dialogs[it.toString()]!!.contains(dialogName)) {
                        allActivity_Dialogs[it.toString()]!!.add(dialogName)
                    }
                }

            }
        }

        private fun addElementToAllWindow_Widget_EventHandler(
            windowName: String,
            eventHandler: EventHandler
        ) {
            if (!allWindow_Widget_EventHandlers.contains(windowName)) {
                allWindow_Widget_EventHandlers[windowName] = HashMap()
            }
            val widgetList = allWindow_Widget_EventHandlers[windowName]!!
            val widget = eventHandler.widget.toString()
            if (!widgetList.contains(widget)) {
                widgetList[widget] = HashMap()
            }
            val eventList = widgetList[widget]!!
            if (!eventList.contains(eventHandler.event.toString())) {
                eventList[eventHandler.event.toString()] = ArrayList()
            }
            val handlers = eventList[eventHandler.event.toString()]!!
            if (!handlers.contains(eventHandler.eventHandler.signature)) {
                handlers.add(eventHandler.eventHandler.signature)
            }
//            if (!allEventHandlers.contains(eventHandler.eventHandler.signature)) {
//                allEventHandlers.add(eventHandler.eventHandler.signature)
//            }
        }

        fun getAllChildrenInWindow(window: NNode, allChildWidgetsMap: HashMap<Int, Any>, allProcessWidgets: HashSet<NNode>)
        {
            val childWidget = window.children
            childWidget.forEach{

                    if (!allChildWidgetsMap.containsKey(it.id))
                    {
                        if (it is NInflNode || it is NViewAllocNode)
                         {
                             val allEventHandlerMethods = GUIUserInteractionClient.guiAnalysisOutput!!.getAllEventsAndTheirHandlers(it as NObjectNode).map { it.value }
                             allEventHandlerMethods.forEach {
                                 it.forEach {
                                     if(!allEventHandlers.contains(it.signature))
                                     {
                                         allEventHandlers.add(it.signature)
                                         ComponentRelationCalculation.instance.eventHandlers.add(it)
                                     }

                                 }
                             }
                            if (it.children.isEmpty())
                            {
                                allChildWidgetsMap[it.id] = it.toString()
                            }
                            else
                            {
                                if (!allProcessWidgets.contains(it))
                                {
                                    allProcessWidgets.add(it)
                                    val childHashMap = HashMap<Int,Any>()
                                    allChildWidgetsMap[it.id] = hashMapOf("widget" to it.toString(), "children" to childHashMap)
                                    getAllChildrenInWindow(it,childHashMap,allProcessWidgets)
                                }


                            }
                        }
                        else
                        {
                            //log.info("NNode ${it.toString()} 's classtype is ${it.javaClass}")
                        }

                    }

                }

        }

        fun getAllChildrenFlattenInWindow(window: NNode, allChildWidgetsMap: HashMap<Int, Any>, level: Int)
        {
            if (level > 10)
                return
            val childWidget = window.children

            childWidget.forEach{
                if (it is NOptionsMenuNode){
                    getAllWindowWidgets(it)
                }
                else
                {
                    if (it is NObjectNode) {
                        val allEventHandlerMethods = GUIUserInteractionClient.guiAnalysisOutput!!.getAllEventsAndTheirHandlers(it).map { it.value }
                        allEventHandlerMethods.forEach {
                            it.forEach {
                                if(!allEventHandlers.contains(it.signature))
                                {
                                    allEventHandlers.add(it.signature)
                                }
                            }
                        }
                    }
                    if (it.idNode!=null && !allChildWidgetsMap.containsKey(it.id))
                    {
                        allChildWidgetsMap[it.id] = it.toString()
                        val textNodeInterator = it.textNodes.iterator()
                        while (textNodeInterator.hasNext())
                        {
                            val text = PropertyManager.v().textNodeToString(textNodeInterator.next())
                            if (!widgetTexts.containsKey(it.id.toString()))
                            {
                                widgetTexts[it.id.toString()] = ArrayList()
                            }
                            widgetTexts[it.id.toString()]!!.add(text)
                        }
                    }
                    getAllChildrenFlattenInWindow(it,allChildWidgetsMap,level+1)
                }
            }

        }







        @Throws(IOException::class)
        private fun writeInstrumentationList(apkName: String ,outputDir: Path): Path {
            val outputMap = HashMap<String, Any>()
            outputMap["outputAPK"] = apkName
            outputMap["allActivityNodes"] = allActivityNodes
            outputMap["allDialogs"]= allDialogs
            outputMap["allActivityDialogs"] = allActivity_Dialogs
            outputMap["allActivityOptionMenuItems"] = allActivityOptionMenuItems
            outputMap["allWidgetEvent"]=produceAllWidgetEvent()
//            //outputMap["allCallbacks"] = allCallbacks
//            //outputMap["allStatements"] = allStatements
//            //outputMap["allMethods"] = allMethods
            outputMap["modifiedMethods"] = modifiedMethods
            outputMap["modiMethodInvocation"] = produceViewInvocationHashMap()
            outputMap["modiMethodTopCaller"] = produceModifiedMethodTopCaller()
            outputMap["allWindow_Widgets"] = allWindow_Widgets
            outputMap["allWindow_Widget_EventHandlers"] = allWindow_Widget_EventHandlers
            outputMap["allTransitions"] = allTransitions
            outputMap["allStrings"] = allResourceStrings
            outputMap["menuItemTexts"] = menuItemsTexts
//            outputMap["widgetTexts"]= widgetTexts
//            outputMap["allNodes"] = allNodes
//            outputMap["allOpNodes"] = allOpNodes
//            outputMap["allLayoutIdNodes"] = allLayoutIdNodes
//            outputMap["allInflateNodes"] = allInflateNodes
//            outputMap["allFragmentNodes"] = allFragmentNodes
            outputMap["unhandledMethods"]= notAppearInTargetCallingMethods
            //outputMap["unreachableMethods"] = unreachableMethods
            outputMap["unreachableModifiedMethods"] = produceUnreachableModifiedMethodsSimple()
            outputMap["unhandleModifiedMethod"] = produceUnhandledModifiedMethods()
            outputMap["allUnreachedActivity"] = produceUnreachedActivity()
            outputMap["numberOfModifiedMethods"] = modifiedMethods.size
            outputMap["numberOfHandledModifiedMethods"] = modMethodInvocation.size
//            outputMap["numberOfUnhandledModifiedMethods"] = (outputMap["unhandleModifiedMethod"] as HashMap<*,*>).size
            outputMap["numberOfNotFoundModifiedMethods"] = notFoundModifiedMethods.size
            outputMap["numberOfUnreachableModifiedMethods"] = (outputMap["unreachableModifiedMethods"] as List<*>).size
//            outputMap["allEventHandlers"] = allEventHandlers
//            outputMap["ActivityCorrelation"] = ComponentRelationCalculation.instance.componentRelationScore.filter { it.value>0.0 }
//            outputMap["EventHandlerCorrelation"] = produceEventCorrelations(ComponentRelationCalculation.instance.eventComponentRelationScore.filter { it.value>0.0})
            outputMap["methodDependency"] = produceMethodDependency(ComponentRelationCalculation.instance.eventHandlersDependencyCount)
            outputMap["eventHandlerDependency"] = produceEventHandlerDependency(ComponentRelationCalculation.instance.eventHandlersDependencyCount)
            outputMap["eventDependency"] = produceEventDependency(ComponentRelationCalculation.instance.eventsDependencyCount)
            outputMap["windowsDependency"] = produceWindowDependency(ComponentRelationCalculation.instance.windowsDependencyCount)
            //outputMap["window_ClassWeight"] = produceWindowClassWeight(ComponentRelationCalculation.instance.windowClassWeights)
//            outputMap["classDependencyApperance"] = ComponentRelationCalculation.instance.classDependencyAppearance
//            outputMap["classDependencyInverseFrequency"] = ComponentRelationCalculation.instance.classDependencyInverseFrequency
            //outputMap["eventHandler_ClassWeight"] = produceEventHandlerClassWeight(ComponentRelationCalculation.instance.eventHandlerClassWeights)
            outputMap["event_ClassWeight"] = produceEventClassWeight(ComponentRelationCalculation.instance.eventClassWeights)
            //outputMap["event_Event_Correlation"] = produceEventCorrelations(ComponentRelationCalculation.instance.eventCorrelationScores)
            //outputMap["window_Window_Correlation"] = produceWindowCorrelations(ComponentRelationCalculation.instance.windowCorrelationScores)
            outputMap["event_window_Correlation"] = produceEventWindowCorrelations(ComponentRelationCalculation.instance.eventWindowCorrelationScores)
            val instrumentResultFile = outputDir.resolve("${apkName}-AppModel.json")
            val resultJson = JSONObject(outputMap)
            Files.write(instrumentResultFile, resultJson.toString(4).toByteArray())
            Logger.verb("INFO","Number of modified methods: ${modifiedMethods.size}")
            Logger.verb("INFO","Number of unreachable modified methods: ${outputMap["numberOfUnreachableModifiedMethods"]}")
            Logger.verb("INFO","Number of handled modified methods: ${outputMap["numberOfHandledModifiedMethods"]}")
            Logger.verb("INFO","Number of unhandled modified methods: ${outputMap["numberOfUnhandledModifiedMethods"]}")
            return instrumentResultFile
        }

        private fun produceEventWindowCorrelations(eventWindowCorrelationScores: HashMap<ComponentRelationCalculation.Event, HashMap<NObjectNode, Pair<Double, HashMap<String,Double>>>>): Any {
            val result: HashMap<String,ArrayList<HashMap<String,Any>>> = HashMap() //root-> Window
            eventWindowCorrelationScores.forEach { event1, correlation ->
                val event1String = HashMap<String, Any>()
                event1String.put("targetWidget", event1.guiWidget.toString())
                event1String.put("eventType", event1.eventType.name)
                val correlationsOutput = ArrayList<Pair<Any,Any>>()
                correlation.filter { it.value.first>0.0 }.map { Pair(it.key,it.value) }.sortedByDescending { it.second.first }. forEach {
                    val window = it.first
                    val score = it.second.first
                    correlationsOutput.add(Pair(window.toString(),String.format("%.4f",score)))
                }
                event1String.put("correlations",correlationsOutput)
                if (!result.containsKey(event1.source.toString()))
                {
                    result.put(event1.source.toString(), ArrayList())
                }
                result[event1.source.toString()]!!.add(event1String)
            }
            return result
        }

        private fun produceEventWindowCorrelations_Debug(eventWindowCorrelationScores: HashMap<ComponentRelationCalculation.Event, HashMap<NObjectNode, Pair<Double, HashMap<String,Double>>>>): Any {
            val result: HashMap<String,ArrayList<HashMap<String,Any>>> = HashMap() //root-> Window
            eventWindowCorrelationScores.forEach { event1, correlation ->
                val event1String = HashMap<String, Any>()
                event1String.put("targetWidget", event1.guiWidget.toString())
                event1String.put("eventType", event1.eventType.name)
                val correlationsOutput = ArrayList<Pair<Any,Any>>()
                correlation.filter { it.value.first>0.0 }.map { Pair(it.key,it.value) }.sortedByDescending { it.second.first }. forEach {
                    val window = it.first
                    val score = it.second.first
                    correlationsOutput.add(Pair(window.toString(),Pair(String.format("%.4f",score), it.second.second)))
                }
                event1String.put("correlations",correlationsOutput)
                if (!result.containsKey(event1.source.toString()))
                {
                    result.put(event1.source.toString(), ArrayList())
                }
                result[event1.source.toString()]!!.add(event1String)
            }
            return result
        }

        private fun produceEventClassWeight(eventClassWeights: HashMap<ComponentRelationCalculation.Event, HashMap<String, Double>>): Any {
            val result = HashMap<String, Any>()
            eventClassWeights.filter{it.value.isNotEmpty() }.forEach {event, classDependencyCount ->
                val window = event.source.toString()
                if (!result.containsKey(window))
                {
                    result.put(window, HashMap<String,Any>())
                }
                val eventList = result[window]!! as HashMap<String,Any>
                val eventInfo = getPrintEventInfo(event)
                val dependencyCount = HashMap<String,String>()
                classDependencyCount.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,String.format("%.4f",f))
                }
                eventList.put(eventInfo,dependencyCount)
            }
            return result
        }

        private fun produceWindowClassWeight(windowClassWeights: HashMap<NObjectNode, HashMap<String, Double>>): Any {
            val result = HashMap<String, Any>()
            windowClassWeights.filter{it.value.isNotEmpty() }.forEach {window, classDependencyCount ->
                val windowStr = window.toString()
                if (!result.containsKey(windowStr))
                {
                    result.put(windowStr, HashMap<String,String>())
                }
                val dependencyCount = result[windowStr]!! as HashMap<String,String>
                classDependencyCount.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,String.format("%.4f",f))
                }
            }
            return result
        }

        private fun produceWindowDependency(windowsDependencyCount: HashMap<NObjectNode, HashMap<String, Long>>): Any {
            val result = HashMap<String, Any>()
            windowsDependencyCount.filter{it.value.isNotEmpty() }.forEach {window, classDependencyCount ->
                val windowStr = window.toString()
                if (!result.containsKey(windowStr))
                {
                    result.put(windowStr, HashMap<String,Long>())
                }
                val dependencyCount = result[windowStr]!! as HashMap<String,Long>
                classDependencyCount.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,f)
                }
            }
            return result
        }

        private fun produceWindowCorrelations(windowCorrelationScores: HashMap<NObjectNode, HashMap<NObjectNode, Pair<Double, HashMap<String,Double>>>>): Any {
            val result: HashMap<String, ArrayList<Pair<String,Any>>> = HashMap()
            windowCorrelationScores.forEach {
                val windowName = it.key.toString()
                val correlations = ArrayList<Pair<String,Any>>()
                it.value.map { Pair(it.key,it.value) }.sortedByDescending { it.second.first }. forEach {
                    correlations.add(Pair(it.first.toString(),Pair(String.format("%.4f",it.second.first),it.second.second)))
                }
                result.put(windowName,correlations)
            }
            return result
        }

        private fun produceModifiedMethodTopCaller(): Any {
            val result = HashMap<String, ArrayList<String>>()
            modifiedMethods.forEach {
                val topCaller = topCallingModifiedMethods[it]
                if (topCaller!=null)
                {
                    result.put(it, topCaller)
                }
            }
            return result
        }

        private fun produceEventHandlerClassWeight(eventClassWeights: HashMap<SootMethod, HashMap<String, Double>>): Any {
            val result = HashMap<String, Any>()
            eventClassWeights.filter{it.value.isNotEmpty() }.forEach {eventHandler, classWeight ->
                val dependencyCount = HashMap<String,Double>()
                classWeight.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,f)
                }
                result.put(eventHandler.signature,classWeight)
            }
            return result
        }

        private fun produceEventDependency(eventClassDependencyCounts: HashMap<ComponentRelationCalculation.Event, HashMap<String, Long>>): Any {
            val result = HashMap<String, Any>()
            eventClassDependencyCounts.filter{it.value.isNotEmpty() }.forEach {event, classDependencyCount ->
                val window = event.source.toString()
                if (!result.containsKey(window))
                {
                    result.put(window, HashMap<String,Any>())
                }
                val eventList = result[window]!! as HashMap<String,Any>
                val eventInfo = getPrintEventInfo(event)
                val dependencyCount = HashMap<String,Long>()
                classDependencyCount.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,f)
                }
                eventList.put(eventInfo,dependencyCount)
            }
            return result
        }

        private fun getPrintEventInfo(event: ComponentRelationCalculation.Event): String {
            val eventInfo = "${event.guiWidget.toString()} - ${event.eventType}"
            return eventInfo
        }

        private fun produceEventHandlerDependency(eventHandlerRecursiveClassDependencyCounts: HashMap<SootMethod, HashMap<String, Long>>): Any {
            val result = HashMap<String, Any>()
            eventHandlerRecursiveClassDependencyCounts.forEach {eventHandler, classDependencyCount ->
                val dependencyCount = HashMap<String,Long>()
                classDependencyCount.forEach { dependencyClass, f ->
                    dependencyCount.put(dependencyClass,f)
                }
                result.put(eventHandler.signature,dependencyCount)
            }
            return result
        }

        private fun produceMethodDependency(methodClassDependencyCounts: HashMap<SootMethod, HashMap<String,Long>>): Any {
            val result = HashMap<String, Any>()
            methodClassDependencyCounts.filter{it.value.isNotEmpty()}.forEach {
                result.put(it.key.signature,it.value)
            }
            return result
        }

        private fun produceEventCorrelations(eventCorrelationScore: Map<ComponentRelationCalculation.Event, Map<ComponentRelationCalculation.Event,Pair<Double, HashMap<String,Double>>>>): Any {
            val result: HashMap<String,HashMap<Any,Any>> = HashMap() //root-> Window
            eventCorrelationScore.forEach { event1, correlation ->
                val event1String = HashMap<String, Any>()
                event1String.put("Window", event1.source.toString())
                event1String.put("targetWidget", event1.guiWidget.toString())
                event1String.put("eventType", event1.eventType.name)
                val correlationsOutput = ArrayList<Pair<Any,Any>>()
                correlation.filter { it.value.first>0.0 }.map { Pair(it.key,it.value) }.sortedByDescending { it.second.first }. forEach {
                    val event2 = it.first
                    val score = it.second.first
                    val event2String = HashMap<String,Any>()
                    event2String.put("Window", event2.source.toString())
                    event2String.put("targetWidget", event2.guiWidget.toString())
                    event2String.put("eventType", event2.eventType.name)
                    correlationsOutput.add(Pair(event2String,Pair(String.format("%.4f",score),it.second.second)))
                }
                if (!result.containsKey(event1.source.toString()))
                {
                    result.put(event1.source.toString(), HashMap<Any,Any>())

                }
                result[event1.source.toString()]!!.put(event1String,correlationsOutput)
            }
            return result
        }

        private fun produceUnreachableModifiedMethodsSimple(): ArrayList<String> {
            val unreachableModifiedMethods = ArrayList<String>()
            for (m in modifiedMethods){
                if (!modMethodInvocation.contains(m) && topCallingModifiedMethods.contains(m)){
                    var isUnreachable = true
                    topCallingModifiedMethods[m]!!.forEach {
                        if (!unreachableMethods.contains(it))
                            isUnreachable = false
                    }
                    if (isUnreachable)
                    {
                        unreachableModifiedMethods.add(m)
                        //unreachableMethods.add(m)
                    }
                }
            }
            return unreachableModifiedMethods
        }

        private fun produceUnreachableModifiedMethods(): HashMap<String, ArrayList<String>> {
            val unreachableModifiedMethods = HashMap<String, ArrayList<String>>()
            for (m in modifiedMethods){
                if (!modMethodInvocation.contains(m) && topCallingModifiedMethods.contains(m)){
                    var isUnreachable = true
                        topCallingModifiedMethods[m]!!.forEach {
                            if (!unreachableMethods.contains(it))
                                isUnreachable = false
                        }
                    if (isUnreachable)
                    {
                        unreachableModifiedMethods[m] = ArrayList(topCallingModifiedMethods[m])
                        //unreachableMethods.add(m)
                    }
                }
            }
            return unreachableModifiedMethods
        }

        val notFoundModifiedMethods = ArrayList<String>()
        private fun getHandlerModifiedMethodNumber(): Int {
            return 0
        }

        internal fun produceAllWidgetEvent(): HashMap<String,Any>  // source -> widget list -> event list
        {
            val result = HashMap<String, Any>()
            widgetEvents.forEach { widget, activityEvents ->
                activityEvents.forEach {
                    val window = it.sourceNode.window

                    if (!result.contains(window.toString()))
                    {
                        result[window.toString()] = HashMap<String,Any>() // widget -> event list
                    }

                    val widgetList = result[window.toString()]!! as HashMap<String,Any>
                    val widget = it.guiWidget.toString()
                    if (!widgetList.contains(widget))
                    {
                        widgetList[widget] = ArrayList<HashMap<String, Any>>() // list of event
                    }
                    val eventList = widgetList[widget]!! as ArrayList<HashMap<String, Any>>
                    val action = it.eventType.name
                    if (!eventList.any {it["action"]!! == action })
                    {
                        val eventHandler = it.eventHandlers.map { it.signature }
                        eventList.add(hashMapOf("action" to action, "handler" to eventHandler))
                    }

                }
            }
            return result
        }

        internal fun produceViewInvocationHashMap(): HashMap<String, Any>{
            val hashmapResult = HashMap<String, Any>()
            for ((k,v) in modMethodInvocation)
            {

                for (e in v)
                {
                    if (!hashmapResult.containsKey(e.sourceNode.window.toString()))
                    {
                        hashmapResult[e.sourceNode.window.toString()] = HashMap<String, Any>()
                    }
                    val window = hashmapResult[e.sourceNode.window.toString()] as HashMap<String, Any>
                    if (!window.contains(e.guiWidget.toString()))
                    {
                        window[e.guiWidget.toString()] = ArrayList<HashMap<String, Any>>()
                    }

                    val widget = window[e.guiWidget.toString()] as ArrayList<HashMap<String, Any>>
                    var hasEvent = false
                    var eventIndex: Int? = null
                    for ((i,rE) in widget.withIndex())
                    {
                        if (rE["eventType"]==e.eventType.name)
                        {
                            hasEvent = true
                            eventIndex = i
                            break
                        }
                    }
                    if (hasEvent)
                    {
                        if (!(widget[eventIndex!!]["modMethods"] as ArrayList<String>).contains(k))
                            (widget[eventIndex!!]["modMethods"] as ArrayList<String>).add(k)
                    }
                    else
                    {
                        widget.add(hashMapOf("eventType" to e.eventType.name, "eventHandlers" to e.eventHandlers.map { it.signature },
                                "modMethods" to arrayListOf<String>(k)))

                    }

                }

            }
            return hashmapResult
        }

        internal fun produceUnhandledModifiedMethods(): HashMap<String, ArrayList<String>>{
            val unhandledModifiedMethods = HashMap<String, ArrayList<String>>()
            for (m in modifiedMethods){
                if (!modMethodInvocation.contains(m) && !unreachableMethods.contains(m) && topCallingModifiedMethods.contains(m)){
                    topCallingModifiedMethods[m]!!.forEach {
                        addMethodTopCalling(m, it,unhandledModifiedMethods )
                    }
                }
            }
            return unhandledModifiedMethods
        }

        internal fun produceUnreachedActivity(): List<String>{
            val unreachedActivity = ArrayList<String>()
            allActivityNodes.forEach { k, v ->
                if (!allReachedWindow.contains(k))
                {
                    unreachedActivity.add(v)
                }
            }
            return unreachedActivity
        }

        private fun addMethodTopCalling(method: String, eventHandler: String, methodInvocations: java.util.HashMap<String, ArrayList<String>>) {
            if (!methodInvocations.containsKey(method))
            {
                methodInvocations[method] = ArrayList()
            }
            val eventHandlerMethod = Scene.v().getMethod(eventHandler)
            val declareClass = eventHandlerMethod.declaringClass
            val eventExisted = methodInvocations[method]!!.contains(eventHandler)
            if (!eventExisted) {
                methodInvocations[method]!!.add(eventHandler)
            }
        }


    }
