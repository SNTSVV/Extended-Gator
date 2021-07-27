/*
 * MethodNames.java - part of the GATOR project
 *
 * Copyright (c) 2018 The Ohio State University
 *
 * This file is distributed under the terms described in LICENSE in the
 * root directory.
 */
package presto.android;

import com.google.common.collect.Sets;

import java.util.Set;

public interface MethodNames {
  String setContentViewSubSig = "void setContentView(int)";

  String setContentViewViewSubSig = "void setContentView(android.view.View)";

  String setContentViewViewParaSubSig = "void setContentView(android.view.View,android.view.ViewGroup$LayoutParams)";

  String layoutInflaterInflate =
          "<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup)>";

  String layoutInflaterInflateBool =
          "<android.view.LayoutInflater: android.view.View inflate(int,android.view.ViewGroup,boolean)>";

  String viewCtxInflate =
          "<android.view.View: android.view.View inflate(android.content.Context,int,android.view.ViewGroup)>";

  String viewFindViewById =
          "<android.view.View: android.view.View findViewById(int)>";

  String actFindViewById =
          "<android.app.Activity: android.view.View findViewById(int)>";

  String findViewByIdSubSig = "android.view.View findViewById(int)";

  //Deal with Kotlin Android Extension
  String findCachedViewByIdSubSig = "android.view.View _$_findCachedViewById(int)";

  String setIdSubSig = "void setId(int)";

  String addViewName = "addView";

  String findFocusSubSig = "android.view.View findFocus()";

  //=== post CGO'14

  //--- Menu

  String onCreateOptionsMenuName = "onCreateOptionsMenu";
  String onPrepareOptionsMenuName = "onPrepareOptionsMenu";

  String onOptionsItemSelectedName = "onOptionsItemSelected";
  String onContextItemSelectedName = "onContextItemSelected";

  String onCreateOptionsMenu1Subsig = "boolean onCreateOptionsMenu(android.view.Menu)";
  String onCreateOptionsMenu2Subsig = "void onCreateOptionsMenu(android.view.Menu,android.view.MenuInflater)";
  String onPrepareOptionsMenuSubsig = "boolean onPrepareOptionsMenu(android.view.Menu)";
  String onCloseOptionsMenuSubsig = "void onOptionsMenuClosed(android.view.Menu)";

  String onOptionsItemSelectedSubSig = "boolean onOptionsItemSelected(android.view.MenuItem)";
  String onContextItemSelectedSubSig = "boolean onContextItemSelected(android.view.MenuItem)";
  String onMenuItemSelectedSubSig = "boolean onMenuItemSelected(int,android.view.MenuItem)";

  String viewOnCreateContextMenuSubSig = "void onCreateContextMenu(android.view.ContextMenu)";
  String onCloseContextMenuSubsig = "void onContextMenuClosed(android.view.Menu)";

  String activityOnBackPressedSubSig = "void onBackPressed()";
  String activityOnSearchRequestedSubSig = "boolean onSearchRequested()";

  String viewShowContextMenuSubsig = "boolean showContextMenu()";
  String activityOpenContextMenuSubsig = "void openContextMenu(android.view.View)";
  String activityOpenOptionsMenuSubsig = "void openOptionsMenu()";
  String activityCloseOptionsMenuSubsig = "void closeOptionsMenu()";

  String onActivityResultSubSig = "void onActivityResult(int,int,android.content.Intent)";

  String onCreateContextMenuName = "onCreateContextMenu";
  String onCreateContextMenuSubSig = "void onCreateContextMenu(android.view.ContextMenu,android.view.View,android.view.ContextMenu$ContextMenuInfo)";
  String registerForContextMenuSubSig = "void registerForContextMenu(android.view.View)";
  String setOnCreateContextMenuListenerName = "setOnCreateContextMenuListener";

  String menuAddCharSeqSubSig = "android.view.MenuItem add(java.lang.CharSequence)";
  String menuAddIntSubSig = "android.view.MenuItem add(int)";
  String menuAdd4IntSubSig = "android.view.MenuItem add(int,int,int,int)";
  String menuAdd3IntCharSeqSubSig = "android.view.MenuItem add(int,int,int,java.lang.CharSequence)";

  String menuItemSetTitleCharSeqSubSig = "android.view.MenuItem setTitle(java.lang.CharSequence)";
  String menuItemSetTitleIntSubSig = "android.view.MenuItem setTitle(int)";
  String menuItemSetIntentSubSig = "android.view.MenuItem setIntent(android.content.Intent)";

  String menuFindItemSubSig = "android.view.MenuItem findItem(int)";
  String menuGetItemSubSig = "android.view.MenuItem getItem(int)";

  String menuInflaterSig = "<android.view.MenuInflater: void inflate(int,android.view.Menu)>";
  // TODO(tony): add SubMenu stuff.

  //--- TabActivity and so on
  String getTabHostSubSig = "android.widget.TabHost getTabHost()";

  String tabHostAddTabSugSig = "void addTab(android.widget.TabHost$TabSpec)";
  String tabHostNewTabSpecSubSig = "android.widget.TabHost$TabSpec newTabSpec(java.lang.String)";

  String tabSpecSetIndicatorCharSeqSubSig =
          "android.widget.TabHost$TabSpec setIndicator(java.lang.CharSequence)";
  String tabSpecSetIndicatorCharSeqDrawableSubSig =
          "android.widget.TabHost$TabSpec setIndicator(java.lang.CharSequence,android.graphics.drawable.Drawable)";
  // API >= 4
  String tabSpecSetIndicatorViewSubSig =
          "android.widget.TabHost$TabSpec setIndicator(android.view.View)";

  String tabSpecSetContentIntSubSig = "android.widget.TabHost$TabSpec setContent(int)";
  String tabSpecSetContentFactorySubSig =
          "android.widget.TabHost$TabSpec setContent(android.widget.TabHost$TabContentFactory)";
  // TODO(tony): it starts a new activity, and gets the root. Worry about this
  //             later.
  String tabSpecSetContentIntentSubSig = "android.widget.TabHost$TabSpec setContent(android.content.Intent)";

  String tabContentFactoryCreateSubSig = "android.view.View createTabContent(java.lang.String)";

  //--- ListView and so on
  String getListViewSubSig = "android.widget.ListView getListView()";

  String setAdapterSubSig = "void setAdapter(android.widget.ListAdapter)";

  String getViewSubSig =
          "android.view.View getView(int,android.view.View,android.view.ViewGroup)";
  String onCreateViewHolderSubSigTemplate =
          "onCreateViewHolder(android.view.ViewGroup,int)";
  String newViewSubSig =
          "android.view.View newView(android.content.Context,android.database.Cursor,android.view.ViewGroup)";
  String getChildViewHolderSubSig1 =
          "android.support.v7.widget.RecyclerView$ViewHolder getChildViewHolder(android.view.ViewGroup)";
  String createViewHolderSubSig1 =
          "android.support.v7.widget.RecyclerView$ViewHolder onCreateViewHolder(android.view.ViewGroup,int)>";
  String getChildViewHolderSubSig2 =
          "androidx.recyclerview.widget.RecyclerView$ViewHolder getChildViewHolder(android.view.ViewGroup)";
  String createViewHolderSubSig2 =
          "androidx.recyclerview.widget.RecyclerView$ViewHolder onCreateViewHolder(android.view.ViewGroup,int)>";
  //--- Some "non-standard" callbacks in the framework
  String onDrawerOpenedSubsig = "void onDrawerOpened()";

  String onDrawerClosedSubsig = "void onDrawerClosed()";

  //--- Containers

  String collectionAddSubSig = "boolean add(java.lang.Object)";
  String collectionIteratorSubSig = "java.util.Iterator iterator()";
  String iteratorNextSubSig = "java.lang.Object next()";
  String mapPutSubSig = "java.lang.Object put(java.lang.Object,java.lang.Object)";
  String mapGetSubSig = "java.lang.Object get(java.lang.Object)";

  //--- framework handlers
  String onActivityCreateSubSig = "void onCreate(android.os.Bundle)";
  String onActivityStartSubSig = "void onStart()";
  String onActivityRestartSubSig = "void onRestart()";
  String onActivityResumeSubSig = "void onResume()";
  String onActivityPauseSubSig = "void onPause()";
  String onActivityStopSubSig = "void onStop()";
  String onActivityDestroySubSig = "void onDestroy()";
  String activityOnNewIntentSubSig = "void onNewIntent(android.content.Intent)";
  String onViewCreatedSubSig = "void onViewCreated(android.view.View,android.os.Bundle)";


  String onListItemClickSubSig = "void onListItemClick(android.widget.ListView,android.view.View,int,long)";

  //--- Dialogs
  String activityShowDialogSubSig = "void showDialog(int)";
  // Added in API level 8
  String activityShowDialogBundleSubSig = "boolean showDialog(int,android.os.Bundle)";

  String activityDismissDialogSubSig = "void dismissDialog(int)";
  String activityRemoveDialogSubSig = "void removeDialog(int)";

  String activityOnCreateDialogSubSig = "android.app.Dialog onCreateDialog(int)";
  // Added in API level 8
  String activityOnCreateDialogBundleSubSig = "android.app.Dialog onCreateDialog(int,android.os.Bundle)";

  String activityOnPrepareDialogSubSig = "void onPrepareDialog(int,android.app.Dialog)";
  // Added in API level 8
  String activityOnPrepareDialogBundleSubSig = "void onPrepareDialog(int,android.app.Dialog,android.os.Bundle)";


  String dialogOnCancelSubSig = "void onCancel(android.content.DialogInterface)";
  String dialogOnDismissSubSig = "void onDismiss(android.content.DialogInterface)";
  String dialogOnKeySubSig = "boolean onKey(android.content.DialogInterface,int,android.view.KeyEvent)";
  String dialogOnShowSubSig = "void onShow(android.content.DialogInterface)";

  Set<String> dialogNonLifecycleMethodSubSigs = Sets.newHashSet(
          dialogOnCancelSubSig, dialogOnDismissSubSig,
          dialogOnKeySubSig, dialogOnShowSubSig);

  // This is called by onClick of a button in the dialog
  String dialogOnClickSubSig = "void onClick(android.content.DialogInterface,int)";

  String onDialogCreateSubSig = onActivityCreateSubSig;
  String onDialogStartSubSig = onActivityStartSubSig;
  String onDialogStopSubSig = onActivityStopSubSig;

  Set<String> dialogLifecycleMethodSubSigs = Sets.newHashSet(
          onDialogCreateSubSig, onDialogStartSubSig, onDialogStopSubSig);

  String alertDialogSetButtonCharSeqMsgSubSig =
          "void setButton(java.lang.CharSequence,android.os.Message)";
  String alertDialogSetButtonIntCharSeqListenerSubSig =
          "void setButton(int,java.lang.CharSequence,android.content.DialogInterface$OnClickListener)";
  String alertDialogSetButtonCharSeqListenerSubSig =
          "void setButton(java.lang.CharSequence,android.content.DialogInterface$OnClickListener)";
  String alertDialogSetButtonIntCharSeqMsgSubSig =
          "void setButton(int,java.lang.CharSequence,android.os.Message)";
  String alertDialogSetButton2ListenerSubSig =
          "void setButton2(java.lang.CharSequence,android.content.DialogInterface$OnClickListener)";
  String alertDialogSetButton2MessageSubSig =
          "void setButton2(java.lang.CharSequence,android.os.Message)";
  String alertDialogSetButton3ListenerSubSig =
          "void setButton3(java.lang.CharSequence,android.content.DialogInterface$OnClickListener)";
  String alertDialogSetButton3MessageSubSig =
          "void setButton3(java.lang.CharSequence,android.os.Message)";

  // Intents
  String intentAddFlagsSubSig = "android.content.Intent addFlags(int)";
  String intentSetFlagsSubSig = "android.content.Intent setFlags(int)";
  String intentGetFlagsSubsig = "int getFlags()";

  // start activity
  String activityStartActivityForResultSubSig = "void startActivityForResult(android.content.Intent,int)";

  // Sensor
  String getDefaultSensorSig =
          "<android.hardware.SensorManager: android.hardware.Sensor getDefaultSensor(int)>";

  String getDefaultSensor2Sig =
          "<android.hardware.SensorManager: android.hardware.Sensor getDefaultSensor(int,boolean)>";

  // TextView.setText
  String textViewSetTextSubSig1 = "void setText(java.lang.CharSequence)";
  String textViewSetTextSubSig2 = "void setText(java.lang.CharSequence,android.widget.TextView$BufferType)";
  String textViewSetTextSubSig3 = "void setText(int)";
  String textViewSetTextSubSig4 = "void setText(int,android.widget.TextView$BufferType)";
  String textViewSetTextSubSig5 = "void setText(char[],int,int)";

  // TextView.setHint
  String textViewSetHintSubSig1 = "void setHint(java.lang.CharSequence)";
  String textViewSetHintSubSig2 = "void setHint(int)";

  // StringBuilder.append()
  // TODO: more varieties of append op
  String stringBuilderAppendSig1 = "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>";

  // **** Fragment v4.support
  // Fragment classes
  String fragmentClass1 = "android.support.v4.app.Fragment";
  String fragmentTransactionClass1 = "android.support.v4.app.FragmentTransaction";

  // method subsig
  String androidsupportv4 = "android.support.v4";
  String getSupportFragmentManagerv4 =  fragmentTransactionClass1 +" getSupportFragmentManager()";
  String fragmentTransaction1AddSubSig1 = fragmentTransactionClass1 + " add(int,"+ fragmentClass1 +",java.lang.String)";
  String fragmentTransaction1AddSubSig2 = fragmentTransactionClass1 +" add("+ fragmentClass1 +",java.lang.String)";
  String fragmentTransaction1AddSubSig3 = fragmentTransactionClass1 + " add(int," + fragmentClass1 + ")";
  String fragmentTransaction1ReplaceSubSig1 = fragmentTransactionClass1 + " replace(int,"+ fragmentClass1 +",java.lang.String)";
  String fragmentTransaction1ReplaceSubSig2 = fragmentTransactionClass1 + " replace(int,"+ fragmentClass1 +")";

  String pagerAdapterClass = androidsupportv4 + ".view.PagerAdapter";
  String viewPaperSetAdapterSubSig = "void setAdapter("+ pagerAdapterClass + ")";

  String fragmentOnActivityCreatedSubSig = "void onActivityCreated(android.os.Bundle)";
  String fragmentOnCreateViewSubSig = "android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)";
  String fragmentOnAttachSubSig = "void onAttach(android.content.Context)";

  // **** Fragment android.app
  // Class
  String fragmentClass2 = "android.app.Fragment";
  String fragmentTransactionClass2 = "android.app.FragmentTransaction";

  // Method Subsig
  String androidapp = "android.app";
  String getSupportFragmentManager2 =  fragmentTransactionClass2 +" getSupportFragmentManager()";
  String fragmentTransaction2AddSubSig1 = fragmentTransactionClass2 + " add(int,"+ fragmentClass2 +",java.lang.String)";
  String fragmentTransaction2AddSubSig2 = fragmentTransactionClass2 +" add("+ fragmentClass2 +",java.lang.String)";
  String fragmentTransaction2AddSubSig3 = fragmentTransactionClass2 + " add(int," + fragmentClass2 + ")";
  String fragmentTransaction2ReplaceSubSig1 = fragmentTransactionClass2 + " replace(int,"+ fragmentClass2 +",java.lang.String)";
  String fragmentTransaction2ReplaceSubSig2 = fragmentTransactionClass2 + " replace(int,"+ fragmentClass2 +")";

  // **** Fragment androidx
  // Class
  String fragmentClass3 = "androidx.fragment.app.Fragment";
  String fragmentTransactionClass3 = "androidx.fragment.app.FragmentTransaction";

  // Method Subsig
  String androidxapp = "androidx.fragment.app";
  String getSupportFragmentManager3 =  fragmentTransactionClass3 +" getSupportFragmentManager()";
  String fragmentTransaction3AddSubSig1 = fragmentTransactionClass3 + " add(int,"+ fragmentClass3 +",java.lang.String)";
  String fragmentTransaction3AddSubSig2 = fragmentTransactionClass3 +" add("+ fragmentClass3 +",java.lang.String)";
  String fragmentTransaction3AddSubSig3 = fragmentTransactionClass3 + " add(int," + fragmentClass3 + ")";
  String fragmentTransaction3AddSubSig4 = fragmentTransactionClass3 + " add(int," + fragmentClass3 + ",android.os.Bundle,java.lang.String)";
  String fragmentTransaction3AddSubSig5 = fragmentTransactionClass3 + " add(" + fragmentClass3 + ",android.os.Bundle,java.lang.String)";
  String fragmentTransaction3ReplaceSubSig1 = fragmentTransactionClass3 + " replace(int,"+ fragmentClass3 +",java.lang.String)";
  String fragmentTransaction3ReplaceSubSig2 = fragmentTransactionClass3 + " replace(int,"+ fragmentClass3 +")";
  String fragmentTransaction3ReplaceSubSig3 = fragmentTransactionClass3 + " replace(int,"+ fragmentClass3 +",android.os.Bundle)";
  String fragmentTransaction3ReplaceSubSig4 = fragmentTransactionClass3 + " replace(int,"+ fragmentClass3 +",android.os.Bundle,java.lang.String)";

  //VieưPager
  String viewPagerSetAdapterSubSig1 = "void setAdapter(" + "androidx.viewpager.widget.PagerAdapter" + ")";
  String viewPagerSetAdapterSubSig2 = "void setAdapter(" + "android.support.v4.view.PagerAdapter" + ")";
  String pagerAdapterGetItemSubSig1 = fragmentClass1 + " getItem(int)";
  String pagerAdapterGetItemSubSig2 = fragmentClass2 + " getItem(int)";
  String pagerAdapterGetItemSubSig3 = fragmentClass3 + " getItem(int)";
  //recycleview
  String recycleViewsetAdapterSubSig2 = "void setAdapter(" + "androidx.recyclerview.widget.RecyclerView$Adapter" +")";
  String recycleViewsetAdapterSubSig1 = "void setAdapter(" + "android.support.v7.widget.RecyclerView$Adapter" +")";
}
