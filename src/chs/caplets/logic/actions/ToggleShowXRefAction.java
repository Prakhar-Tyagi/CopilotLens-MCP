/*
 * Copyright 2004-2008 Mentor Graphics Corporation
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
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.ICrossReferenceable;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.IXRefText;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import chs.utility.helpers.CrossReferenceUtils;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jun 25, 2004 Time: 2:00:38 PM
 */
public class ToggleShowXRefAction extends ControllerActionRT implements ICtxMenuProvider
{

	private Set<ICrossReferenceable> m_xrSet;

	public ToggleShowXRefAction(ICapletController controller, String instanceName)
	{
		super(controller, instanceName);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_xrSet = getOperands(getController());
		if (m_xrSet != null) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			String instanceName = getActionInstanceName();
			// invoked action should be either show or hide
			assert ToggleShowXRefActionUI.SHOW_XREF.equalsIgnoreCase(instanceName) ||
					ToggleShowXRefActionUI.HIDE_XREF.equalsIgnoreCase(instanceName);

			// if the user has invoked show xref action that means we need to set visibility to true on xref containers
			boolean newVisibility = ToggleShowXRefActionUI.SHOW_XREF.equalsIgnoreCase(instanceName);

			for (ICrossReferenceable xrc : m_xrSet) {
				xrc.setCrossReferencesMarkedVisible(newVisibility);
			}

			if (!newVisibility) {
				Set<IXRefText> texts = new HashSet<IXRefText>(m_xrSet.size());
				for (Object aM_xrSet : m_xrSet) {
					ICrossReferenceable xrc = (ICrossReferenceable) aM_xrSet;
					texts.addAll(CrossReferenceUtils.getAllXRefTextsFromCrossReferenceable(xrc));
				}
				for (SelectedUIDObjectIterator iter =
						getController().getSelectMgr().getCurrentSelections().getSelectedUIDObjects();
						iter.hasNext(); ) {
					IUIDObject uidObj = iter.getNext();
					if (texts.contains(uidObj)) {
						iter.remove();
					}
				}
			}
		}
		return successful;
	}

	public boolean isEnabled()
	{
		Set<ICrossReferenceable> operands = getOperands(getController());
		if (operands != null && !operands.isEmpty() && super.isEnabled()) {
			String instanceName = getActionInstanceName();
			assert ToggleShowXRefActionUI.SHOW_XREF.equalsIgnoreCase(instanceName) ||
					ToggleShowXRefActionUI.HIDE_XREF.equalsIgnoreCase(instanceName);

			if (ToggleShowXRefActionUI.SHOW_XREF.equalsIgnoreCase(instanceName)) {
				return shouldEnableShowAction(operands);
			}
			else if (ToggleShowXRefActionUI.HIDE_XREF.equalsIgnoreCase(instanceName)) {
				return shouldEnableHideAction(operands);
			}
			else {
				return false;
			}
		}
		else {
			return false;
		}
	}

	public String getActionUIClass()
	{
		return ToggleShowXRefActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Set<ICrossReferenceable> crossReferenceableObjs = getOperands(getController());
		if (crossReferenceableObjs != null && !crossReferenceableObjs.isEmpty()) {
			String instanceName = getActionInstanceName();
			boolean shouldAddToCtxMenu = false;
			if (ToggleShowXRefActionUI.SHOW_XREF.equalsIgnoreCase(instanceName)) {
				shouldAddToCtxMenu = shouldEnableShowAction(crossReferenceableObjs);
			}
			else if (ToggleShowXRefActionUI.HIDE_XREF.equalsIgnoreCase(instanceName)) {
				shouldAddToCtxMenu = shouldEnableHideAction(crossReferenceableObjs);
			}

			if (shouldAddToCtxMenu) {
				container.add(new ActionEntry(getActionUI()));
			}
		}
	}

	private boolean shouldEnableHideAction(Set<ICrossReferenceable> crossReferenceableObjs)
	{
		for (ICrossReferenceable crossReferenceableObj : crossReferenceableObjs) {
			// if there is atleast one crossreferenceable object which has show set on it then we need to enable hide
			// we need to do the second check of hasNonEmtpyCrossReferenceString() as there can be a scenario when
			// there is a single instance of object and cross-reference text is empty string
			if (crossReferenceableObj.areCrossReferencesMarkedVisible() /*&&
					CrossReferenceUtils.hasNonEmtpyCrossReferenceString(crossReferenceableObj)*/) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldEnableShowAction(Set<ICrossReferenceable> crossReferenceableObjs)
	{
		for (ICrossReferenceable crossReferenceableObj : crossReferenceableObjs) {
			// if there is atleast one crossreferenceable object which has hide set on it then we need to enable show
			if (!crossReferenceableObj.areCrossReferencesMarkedVisible()) {
				return true;
			}
		}
		return false;
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Nullable
	public static Set<ICrossReferenceable> getOperands(ICapletController controller)
	{
		if (controller == null) {
			return null;
		}
		Set<ICrossReferenceable> xrcSet = new HashSet<ICrossReferenceable>(1);
		for (SelectedUIDObjectIterator iter = controller.getSelectMgr().getCurrentSelections().getSelectedUIDObjects();
				iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IDiagramObject) {
				ISchemDiagram diagram = DiagramHelper.getDiagram((IDiagramObject) uidObj);
				if (diagram != null && diagram.isEditable()) {
					collectXRefTextContainers((IDiagramObject) uidObj, xrcSet);
				}
			}
		}

		if (xrcSet.isEmpty()) {
			return null;
		}

		return xrcSet;
	}

	static void collectXRefTextContainers(IDiagramObject dObj, Set<ICrossReferenceable> xrtSet)
	{
		if (dObj instanceof ICrossReferenceable) {
			ICrossReferenceable xref = (ICrossReferenceable) dObj;
			if (xref.getXRefTextContainers().hasNext()) {
				xrtSet.add(xref);
			}
		}
		else if (dObj instanceof IXRefText && dObj.getParent() instanceof ICrossReferenceable) {
			collectXRefTextContainers(dObj.getParent(), xrtSet);
		}
		else if (dObj instanceof ISegment) {
			IConductor conductor = ((ISegment) dObj).getConductor();
			collectXRefTextContainers(conductor, xrtSet);
		}
		else if (dObj instanceof IConductor) {
			Collection<ICrossReferenceable> ports = ((IConductor) dObj).getCrossReferenceables();
			for (ICrossReferenceable port : ports) {
				collectXRefTextContainers(port.getDiagramObject(), xrtSet);
			}
		}
		else if (dObj instanceof IRepresentedObject) {
			IRepresentedObject repObj = (IRepresentedObject) dObj;
			if (repObj.getRawConnectivity() instanceof ILogicObject
					&& ((ILogicObject) repObj.getRawConnectivity()).getSharedObject() != null) {
				for (Object o : repObj.getCrossReferenceables()) {
					ICrossReferenceable xr = (ICrossReferenceable) o;
					collectXRefTextContainers(xr.getDiagramObject(), xrtSet);
				}
			}
		}
	}
}
