/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2025 Siemens
 */
package chs.caplets.logic.analysis.ui;

import chs.analysis.AnalysisServices;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.analysis.AttachSVModelAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.helpers.GfxViewHelper;
import chs.caplets.logic.actions.analysis.LogicAttachModelAction;
import chs.caplets.logic.actions.shared.EditSharedPinListAction;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.common.DesignUtils;
import chs.common.IDesignContainer;
import chs.common.IMemorySnapshotable;
import chs.common.IUIDObject;
import chs.services.gfx.GfxView;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.Pair;
import chs.utility.AnalysisHelper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA. User: melmorsy Date: 06/02/13 Time: 19:44 To change this template use File | Settings |
 * File Templates.
 */
public class AnalysisBrowserTableMouseAdapter extends MouseAdapter
{

	protected ICapletController controller;
	private JTable table;

	public AnalysisBrowserTableMouseAdapter(ICapletController controller, JTable table)
	{
		this.controller = controller;
		this.table = table;
	}

	public void mouseClicked(MouseEvent e)
	{
		//JavaDoc what we are doing on single click and what on double click
		if(e.getClickCount() == 1)
		{
			AnalysisBrowserPanel.AnalysisBrowserTable target =
					(AnalysisBrowserPanel.AnalysisBrowserTable) e.getSource();
			int row = target.getSelectedRow();
			int column = target.getSelectedColumn();
			if(column == 0)
			{
				target.toggleRowVisibility(row,true);
			}
			return;
		}
		if (e.getClickCount() == 2) {
			JTable target = (JTable) e.getSource();
			int row = target.getSelectedRow();
			int column = target.getSelectedColumn();
			if (column == 0 || column == 1) {

				getSelectionMgr().getCurrentSelections().clear();

				String designUID = getDesignUIDForRow(row);
				IDesignContainer design =
						DesignUtils.getLoadedDesign(AnalysisServices.constructUID(designUID), IDesignContainer.class);
                if (design != null) {
                    if (!((IMemorySnapshotable) design).isLoadedInMemory()) {
                        ((IMemorySnapshotable) design).loadToMemory();
                    }

                    String uids = getComponentUIDForRow(row);
					assert uids != null;
                    Set<IUIDObject> objectsToShow = getUIDObjectsToShow(uids);

                    if (!objectsToShow.isEmpty()) {

                        Pair<GfxView, GfxView> discoveredViews = disCoverAndSelectObjectsInViews(design, objectsToShow);

                        GfxView currViewToSel = discoveredViews.getFirst();
                        GfxView lastView = discoveredViews.getSecond();

                        if (column == 1) {
                            zoomSelected(currViewToSel, lastView);
                            resetTableSelectionToRow(row);
                        }
                        else   // (column == 0) view attacher
                        {
                            ISharedObject sharedObj = null;
                            for (IUIDObject objectToShow : objectsToShow) {
                                if (objectToShow instanceof IPinList) {
                                    sharedObj = ((ILogicObject) objectToShow).getSharedObject();
                                    break;
                                }
                            }
                            showAttacher(e, sharedObj);
                        }
                    }
                }
			}
		}
	}

	private void resetTableSelectionToRow(int row)
	{
		table.getSelectionModel().setSelectionInterval(row, row);
	}

	@Nullable protected String getComponentUIDForRow(int row)
	{
		if (table instanceof AnalysisBrowserPanel.AnalysisBrowserTable) {
			return ((AnalysisBrowserPanel.AnalysisBrowserTable) table).getUidForViewRow(row);
		}
		return null;
	}

	@Nullable protected String getDesignUIDForRow(int row)
	{
		if (table instanceof AnalysisBrowserPanel.AnalysisBrowserTable) {
			return ((AnalysisBrowserPanel.AnalysisBrowserTable) table).getDesignUIDForViewRow(row);
		}
		return null;
	}

	protected void zoomSelected(GfxView currViewToSel, GfxView lastView)
	{
		GfxView viewTobeZoomedOn = lastView;
		if (currViewToSel != null) {
			viewTobeZoomedOn = currViewToSel;
		}
		// zoom to the selected object
		GfxViewHelper.zoomSelected(viewTobeZoomedOn, true);
	}

	public Pair<GfxView, GfxView> disCoverAndSelectObjectsInViews(IDesignContainer design,
			Set<IUIDObject> objectsToShow)
	{
		ICapletWindow currWin = CommonUtils.cast(CAFUtils.getInstance().getWindowMgr().getCurrentWindow(),
				ICapletWindow.class);
		IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();

		Collection<GfxView> views = new ArrayList<GfxView>();

		for (IUIDObject objectToShow : objectsToShow) {
			views.addAll(GfxViewHelper.openDiagramsForObject(design, objectToShow.getUID(), false));
		}

		// Current window may have been changed by opening diagrams
		ICapletWindow capletWindow =
				CommonUtils.cast(CAFUtils.getInstance().getWindowMgr().getCurrentWindow(),
						ICapletWindow.class);
		if (capletWindow != null) {
			if (views.isEmpty()) {
				GfxView view = CommonUtils.cast(capletWindow.getCurrentView(), GfxView.class);
				if (view != null) {
					views.add(view);
				}
			}
		}

		GfxView currViewToSel = null;
		GfxView lastView = null;

		for (IUIDObject objectToShow : objectsToShow) {
			for (GfxView gfxView : views) {
				if (gfxView.getDiagram() == activeDiagram) {
					currViewToSel = gfxView;
				}
				lastView = gfxView;
				GfxViewHelper.locateAndSelectObjectWithNotify(gfxView, objectToShow, true, true);
			}
		}

		if (currViewToSel != null) {
			CAFUtils.getInstance().getWindowMgr().setCurrentWindow(currWin);
		}

		return new Pair<GfxView, GfxView>(currViewToSel, lastView);
	}

	private Set<IUIDObject> getUIDObjectsToShow(String uids)
	{
		Set<IUIDObject> objectsToShow = new HashSet<IUIDObject>();
		StringTokenizer uidTokenizer = new StringTokenizer(uids);
		while (uidTokenizer.hasMoreTokens()) {
			IUIDObject object = UIDMgr.getNonDeletedObject(AnalysisServices.constructUID(uidTokenizer.nextToken()));
			if (object != null) {
				objectsToShow.add(object);
			}
		}
		return objectsToShow;
	}

	protected void showAttacher(MouseEvent e, ISharedObject sharedObj)
	{
		// fire the correct action for the selected object
		IAction action = null;
		if (sharedObj == null) {
			Class<? extends ControllerActionRT> actionClass;
			if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
				actionClass = LogicAttachModelAction.class;
			}
			else {
				actionClass = AttachSVModelAction.class;
			}

			action = CAFUtils.getInstance().getActiveCapletController().getAction(actionClass);


		}
		else {
			if (sharedObj instanceof ISharedPinList) {
				action =
						CAFUtils.getInstance().getActiveCapletController()
								.getAction(EditSharedPinListAction.class);
				((EditSharedPinListAction) action).setOperand(false); // we need to ensure
				// our shared object is available
				// to the dialog!
				((EditSharedPinListAction) action).setShowAnalysisTab(true);
			}
		}
		if (action != null) {
			ActionEvent ae =
					new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "addinstance",
							e.getModifiers()); // You may not have e here so may need to pass no modifiers
			IActionMgr actionMgr = CAFUtils.getInstance().getActiveActionMgr();
			assert actionMgr != null;
			actionMgr.actionPerformed(action, ae);

			if (action instanceof EditSharedPinListAction) {
				((EditSharedPinListAction) action).setShowAnalysisTab(false);
			}
		}
	}

	protected ISelectMgr getSelectionMgr()
	{
		return controller.getSelectMgr();
	}
}
