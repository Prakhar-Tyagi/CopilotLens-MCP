/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.IShareActionHelper;
import chs.caplets.logic.actions.shared.SharePinListActionHelper;
import chs.cof.draw.IGrid;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IGroundDevice;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.system.FactoryMgr;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.audit.AuditableEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

public class SymbolCreateSharedAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IPinList m_pinlist;
	private IShareActionHelper m_pinListHelper;
	private ILogicDesign m_design;
	private Model m_model;

	/**
	 * @param controller .
	 */
	public SymbolCreateSharedAction(ICapletController controller)
	{
		this(controller, new SharePinListActionHelper(
				controller.getCaplet().getFIB(), (Model) controller.getCapletModel(), true));
	}

	/**
	 * Constructor only currently needed for tests, allows SharePinListActionHelper to be swapped out
	 *
	 * @param controller The controller is needed for all controller actions
	 * @param actionHelper The action helper handles most sharing functionality
	 */
	protected SymbolCreateSharedAction(ICapletController controller, SharePinListActionHelper actionHelper)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_design = m_model.getDesign();
		m_pinListHelper = actionHelper;
		setUndoableAction(false);
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.helpers.ActionRT#onActivate(java.awt.event.ActionEvent)
	 */

	protected IActionEnum onActivate(ActionEvent e)
	{
		ISymbolDef symDef = getActiveSymbolDef();
		if (symDef != null) {
			symDef = (ISymbolDef) symDef.getContainerLibrary().loadFully(symDef);
		}

		if (symDef == null || symDef.getPinList() == null || symDef.getPinList().getConnectivity() == null) {
			showSymbolNotFoundError();
			return IActionEnum.eCanceled;
		}
		Replicator m_replicator = new Replicator(Replicator.INSTANTIATE, true);
		double scale = getScale(symDef);
		m_pinlist = m_replicator.replicate(symDef, scale);
		if (!SymbolUtils.isUnitScale(scale)) {
			SymbolUtils.adjustOffGridPinsToAGridPoint(m_pinlist, getGrid());
		}
		// Although the pinlist has nothing to do with the diagram, we need to add it here or the usages will barf
		// because this is a incorrectly constructed schem object (no diagram).
		// Ideally, this whole action should not be a Controller action as it has nothing to do with the diagram,
		// and is not undoable.
		m_model.getSheet().addObject(m_pinlist);
		ISymbolRef sref = FactoryMgr.getSymbolFactory().constructSymbolRef(symDef);
		chs.cof.logical.cable.IPinList cablePl = m_pinlist.getConnectivity();
		cablePl.setSymbolRef(sref);

		BaseShareActionOperands operands = new BaseShareActionOperands();
		operands.target = m_pinlist;

		return m_pinListHelper
				.setup(operands, CAFUtils.getInstance().getDialogTitleByAction(this), m_model.getDiagram());
	}

	private double getScale(@NotNull ISymbolDef symDef)
	{
		boolean shouldScale =
				symDef.getSymbolType() == SymbolTypeEnum.COMMENT || getController().getCaplet().isLayoutCaplet();
		return shouldScale ? SymbolUtils.getSymbolScale(symDef, getGrid()) : 1;
	}

	@NotNull private IGrid getGrid()
	{
		return m_model.getDiagram().getGrid();
	}

	protected void showSymbolNotFoundError()
	{
		MessageHelper.showInformationMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(SymbolCreateSharedAction.class, "SymbolCreateSharedAction.LibOutOfSync.header"),
				ResourceMgr.getString(SymbolCreateSharedAction.class, "SymbolCreateSharedAction.LibOutOfSync.msg"));
	}
	/* (non-Javadoc)
	 * @see chs.caf.caplet.helpers.ActionRT#onTerminate(boolean)
	 */

	protected boolean onTerminate(boolean successful)
	{
		boolean editSuccessful = false;

		if (successful) {
			//
			// We explicitly DO NOT want the doEdit to clean up (a we've created a temporary object)
			//
			editSuccessful = m_pinListHelper.doEdit();
			String projectUid =
					CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
			ISharedPinList sharedDevice = (ISharedPinList) m_pinlist.getSharedObject();
			IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
			auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_ADDED, null, projectUid,
					sharedDevice.getFullName(), sharedDevice.getUID().getString());

			// Delete this before all the housekeeping work.
			// Remove it from the diagram as we added it - and we don't want it.  Do all this before the end edit to
			// make sure everything knows where it stands.
			m_model.getSheet().removeObject(m_pinlist);

			// dts0100401012 & dts0100410776
			// Calling delete on a schematic PinList will *not* delete the connectivity object if the
			// connectivity object references a shared object since there may be other schematic objects
			// pointing to that connectivity object. IF this is the *only* schematic object pointing
			// to this connectivity a call setSharedPinList(null) ensures delete() will delete connectivity.
			IDesignSharedUsageMgr dsum = m_design.getSharedUsageMgr();
			if (!dsum.hasUsage(m_pinlist.getConnectivity().getSharedPinList())) {
				m_pinlist.getConnectivity().setSharedPinList(null);
			}
			m_pinlist.delete();
			m_pinlist = null;
		}
		m_pinListHelper.cleanup();

		//
		// The catch-all cleanup - if cleaned up on the 'succesful' branch, this will do nothing.
		//
		if (m_pinlist != null) {
			m_pinlist.delete();
		}

		return editSuccessful;
	}

	@Nullable protected ISymbolDef getActiveSymbolDef()
	{
		IStamp symdef = CAFUtils.getInstance().getCHSSystem().getSymbolLibraryMgr().getActiveSymbol();
		if (symdef instanceof ISymbolDef && !(((ISymbolDef) symdef).getConnectivity() instanceof IGroundDevice)) {
			return (ISymbolDef) symdef;
		}
		else {
			return null;
		}
	}

	/* (non-Javadoc)
		 * @see chs.caf.caplet.action.IAction#isEnabled()
		 */

	public boolean isEnabled()
	{
		boolean enable = false;

		ISymbolDef symbolDef = getActiveSymbolDef();
		if ((symbolDef != null) && (symbolDef.getSymbolType().equals(SymbolTypeEnum.DEVICE))) {
			enable = true;
		}

		return enable && super.isEnabled();
	}


	@Override protected boolean checkCache()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see chs.caf.caplet.action.IAction#getActionUIClass()
	 */

	public String getActionUIClass()
	{
		return SymbolCreateSharedActionUI.class.getName();
	}

	/* (non-Javadoc)
		 * @see chs.caf.ICtxMenuProvider#populateCtxMenu(chs.caf.ActionContainer, chs.caf.caplet.selection.SelectSet)
		 */

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}

	/* (non-Javadoc)
	 * @see chs.caf.ICtxMenuProvider#populateActiveCtxMenu(chs.caf.ActionContainer)
	 */

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
