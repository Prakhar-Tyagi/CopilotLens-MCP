/*
 * Copyright 2004-2012 Mentor Graphics Corporation
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
import chs.caplets.logic.Model;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IStringIterator;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import chs.utility.Placement;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.LogicObjectUtils;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 15, 2004 Time: 9:31:42 AM To change this template use File |
 * Settings | File Templates.
 */
public class AddIndicatorsAction extends ControllerActionRT implements ICtxMenuProvider
{

	IMulticore m_multicore = null;
	String m_type = null;
	ISchemDiagram m_diagram;
	private IShieldBody m_cableShieldBody = null;

	public AddIndicatorsAction(ICapletController controller)
	{
		super(controller);
		m_diagram = ((Model) controller.getCapletModel()).getDiagram();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_multicore = getOperand(getController().getSelectMgr().getPreSelections());
		if (m_multicore != null) {
			ILogicDesign design = m_diagram.getDesign();
			if (design != null) {
				Set<IMulticore> multicoresToLock = m_multicore.getAllMulticoresInHierarchy();
				Collection<IUID> lockFailedObjects =
						LogicObjectLockFinder.tryEdit(design, multicoresToLock);
				if (lockFailedObjects.isEmpty()) {
					return IActionEnum.eCompleted;
				}
			}
		}
		return IActionEnum.eCanceled;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			if (m_cableShieldBody ==
					null) { // Don't have to recreate cable indicator for shared, they are never deleted.
				m_cableShieldBody = LogicObjectUtils.createCableShieldBody(false, m_multicore);
				m_cableShieldBody.setType(m_type);
			}
			Model model = (Model) getController().getCapletModel();
			ISchemDiagram diagram = model.getDiagram();
			Generator gen = Generator.getGenerator();
			GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);

			Placement.placeIndicators(gen, diagram, m_multicore, m_multicore.getShieldBody(), gp, false);
		}
		return successful;
	}

	public boolean isEnabled()
	{
		return getOperand(getController().getSelectMgr().getPreSelections()) != null && super.isEnabled();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperand(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	private IMulticore getOperand(SelectSet sset)
	{
		//dts0100542142: we must update the m_diagram with the active diagram.
		m_diagram = ((Model) getController().getCapletModel()).getDiagram();
		IMulticore multicore = null;
		Set conductors = new HashSet();
		m_type = null;
		m_cableShieldBody = null;

		// Go through the selection, validating it and the multicore at the same time.

		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IMulticore) {
				if (multicore == null) {
					multicore = (IMulticore) uidObj;
				}
				// Reject if more than one multicore selected;
				if (uidObj != multicore) {
					return null;
				}
			}
			else if (uidObj instanceof ISegment) {
				conductors.add(((ISegment) uidObj).getConductor());
			}
			else if (uidObj instanceof IConductor) {
				conductors.add(uidObj);
			}
			else if (uidObj instanceof chs.cof.logical.cable.IConductor) {
				for (IDiagramObject diagObj : m_diagram.getRepresentations(uidObj.getUID())) {
					if (diagObj instanceof IConductor) {
						conductors.add(diagObj);
					}
				}
			}
			else if (uidObj instanceof IShieldBody) {
				multicore = ((IShieldBody) uidObj).getMulticore();
			}
			else if (uidObj instanceof chs.cof.logical.schem.IShieldBody) {
				multicore = ((chs.cof.logical.schem.IShieldBody) uidObj).getConnectivity().getMulticore();
			}
			// Reject if something other than a multicore, conductors, or segments selected;
			else {
				return null;
			}
		}

		for (Iterator it = conductors.iterator(); it.hasNext(); ) {
			IConductor conductor = (IConductor) it.next();
			IMulticore mc = conductor.getConnectivity().getMulticore();
			// If no multicore was selected, use the one to which this conductor belongs
			if (multicore == null && mc != null) {
				multicore = mc;
			}

			// Reject if this conductor has no multicore, or its multicore is not the selected multicore
			if (mc != multicore) {
				return null;
			}
		}

		// Validate further, and remember what we need to generate.
		if (multicore != null) {
			// Reject if no wires to hang the indicators on (this can only happen with library multicores.)
			if (multicore.getAllConductorsInHierarchy().size() == 0) {
				return null;
			}

			// Different validation for shared vs. library multicores
			m_cableShieldBody = multicore.getShieldBody();
			if (m_cableShieldBody != null) {
				m_type = m_cableShieldBody.getType();
			}

			if (m_type == null) {
				// Shared has precedence - if it's a shared library multicore, validate as shared.
				if (multicore.getSharedMulticore() != null) {
					ISharedMulticore scg = multicore.getSharedMulticore();
					IStringIterator indIt = scg.getIndicators();
					if (indIt.hasNext()) {
						m_type = indIt.getNext(); // Should be only one.
					}
				}
				else if (multicore instanceof IOverbraid) {
					m_type = IndicatorHelper.getDefaultOverbraidIndicatorType();
				}
				else {
					String sheathType = multicore.getSheathType();
					if ("Sheath".equalsIgnoreCase(sheathType)) {
						m_type = IndicatorHelper.getDefaultShieldIndicatorType();
					}
					else if ("Twisted".equalsIgnoreCase(sheathType)) {
						m_type = IndicatorHelper.getDefaultTwistIndicatorType();
					}
					else {
						m_type = IndicatorHelper.getDefaultGenericIndicatorType();
					}
				}
			}
		}

		// Reject if no indicators needed
		if (m_type == null) {
			return null;
		}

		if (!conductors.isEmpty() && multicore != null) {
			return multicore.getRootMulticore();
		}

		return multicore;
	}

	public String getActionUIClass()
	{
		return AddIndicatorsActionUI.class.getName();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
