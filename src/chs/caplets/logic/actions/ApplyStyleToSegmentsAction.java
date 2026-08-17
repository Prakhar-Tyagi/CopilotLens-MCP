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
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.cafmain.actions.BaseStyleAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cofUtils.harness.StyleContextProvider;
import chs.common.preferencesets.IPreferenceSet;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class ApplyStyleToSegmentsAction extends BaseStyleAction implements ICtxMenuProvider
{

	private IGrid m_grid;
	private ICapletModel m_model;

	public ApplyStyleToSegmentsAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull protected IActionEnum onActivate(ActionEvent e)
	{
		super.onActivate(e);
		m_model = getController().getCapletModel();
		m_grid = ((IGriddable) m_model.getModelRoot()).getGrid();

		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			// Get opretans from the select set.
			List<ISegment> operands = getOperands();

			// Clear select sets so we can delete (possibly selected) schem propertied text.
			getController().getSelectMgr().getPreSelections().clear();
			getController().getSelectMgr().getCurrentSelections().clear();

			IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(getDiagram());

			// Do the work!
			PreferenceSetHelper.applyStyleSetToSegments(operands, styleSet, m_grid);
		}

		return true;
	}

	//dts0101012590 - disable enabled-ness cache for this action when save is in progress.
	@Override protected boolean checkCache()
	{
		return checkCacheDuringSave() && super.checkCache();
	}

	public boolean isEnabled()
	{
		//dts0101012590 - disable this action when save is going on
		if (CAFUtils.getInstance().getFIB().isTaskActive(IFIB.TASK_SAVE)) {
			return false;
		}

		if (!getController().getCapletModel().isEditable()) {
			return false;
		}

		boolean rc = false;

		List operands = getOperands();
		if (operands.size() > 0) {
			rc = true;
		}

		return rc && super.isEnabled();
	}

	private List<ISegment> getOperands()
	{

		SelectSet sset = getController().getSelectMgr().getCurrentSelections();

		List<ISegment> operands = new ArrayList<ISegment>(sset.getSelectCount());

		for (IDiagramObject diagramObject : LogicMultiUserSelectionFilter.getValidDiagramObjectOperands(
				sset)) {
			if (diagramObject instanceof ISegment) {
				ISegment seg = (ISegment) diagramObject;
                                                            operands.add(seg);			
                                            }
		}

		return operands;
	}

	public String getActionUIClass()
	{
		return ApplyStyleToSegmentsActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	private static ISchemDiagram getDiagram()
	{
		ICapletView capletView = CAFUtils.getInstance().getActiveCapletView();
		IGfxView gfxView = (IGfxView) capletView;
		IBaseDiagram baseDiagram = gfxView.getDiagram();
		return (ISchemDiagram) baseDiagram;
	}

	@Override protected void postTerminate(boolean autoRecoveryEnabled)
	{
		StyleContextProvider.instance().onApplyStyleFinished();
		super.postTerminate(autoRecoveryEnabled);
	}
}
