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
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.IShieldBody;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IUIDObject;
import chs.utility.DiagramHelper;
import chs.utility.Placement;
import chs.utility.logic.ILogicModel;

import java.awt.event.ActionEvent;
import java.util.Collection;

public class ToggleIndicatorConstraintsAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IShieldBody m_indicator;
	private Generator m_generator;

	public ToggleIndicatorConstraintsAction(ICapletController controller)
	{
		super(controller);
		m_generator = Generator.getGenerator();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		if (getOperands(getController())) {
			return IActionEnum.eCompleted;
		}
		else {
			return IActionEnum.eCanceled;
		}
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			ISchemDiagram diagram = ((ILogicModel) getController().getCapletModel()).getDiagram();
			GeneratorParameters genParams = DiagramHelper.createGeneratorParameters(diagram);
			/*
			There are 3 states for indicator
			1. Stretched
			2. Splitted
			3. Individual
			Current state is determined based on the these rules
			1. If m_indicator.getConstrained() false then its stretched
			2. If m_indicator.getContrained() true then its splitted
			3. If m_indicator.getIndividual() true then its individual
			 */
			boolean reTry = !m_indicator.isIndividual();
			boolean deleteOverlappingIndicators = setIndicatorParameters(m_indicator);
			Collection<IShieldBody> newIndicators = Placement
					.replaceIndicator(m_generator, diagram, m_indicator, genParams, null, deleteOverlappingIndicators);
			if (reTry && newIndicators.size() == 1) {
				//See if this action has made any difference.
				IShieldBody newIndicator = newIndicators.iterator().next();
				//IShieldBody newIndicator =
				deleteOverlappingIndicators = setIndicatorParameters(newIndicator);
				Placement.replaceIndicator(m_generator, diagram, newIndicator, genParams, null,
						deleteOverlappingIndicators);
			}
		}
		return successful;
	}

	private boolean setIndicatorParameters(IShieldBody indicator)
	{
		boolean constrained = indicator.isConstrained();
		boolean individual = indicator.isIndividual();
		boolean deleteOverlappingIndicators = false;
		if (!constrained) {
			indicator.setConstrained(true);
			indicator.setIndividual(false);
			deleteOverlappingIndicators = individual;
		}
		else if (!individual) {
			indicator.setIndividual(true);
		}
		else {
			indicator.setConstrained(false);
			indicator.setIndividual(false);
			deleteOverlappingIndicators = true;
		}
		return deleteOverlappingIndicators;
	}

	public boolean isEnabled()
	{
		return getOperands(getController()) && super.isEnabled() ;
	}

	public String getActionUIClass()
	{
		return ToggleIndicatorConstraintsActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperands(getController())) {
			ToggleIndicatorConstraintsActionUI action = ((ToggleIndicatorConstraintsActionUI) getActionUI());
			action.updateUI();
			container.add(new ActionEntry(action));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	private boolean getOperands(ICapletController controller)
	{
		m_indicator = null;
		if (controller == null) {
			return false;
		}
		// The only valid selection is a single indicator
		for (SelectedUIDObjectIterator iter = controller.getSelectMgr().getCurrentSelections().getSelectedUIDObjects();
				iter.hasNext();) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IShieldBody) {
				if (m_indicator == null) {
					m_indicator = (IShieldBody) uidObj;
				}
				else {
					return false;
				}
			}
			else if (uidObj instanceof chs.cof.logical.cable.IShieldBody) {
				// the connectivity shield body will now be in the selection following a diagram selection (dts0100559480)
				// i think IJ got this wrong
				//noinspection UnnecessaryContinue
				continue;
			}
			else {
				return false;
			}
		}
		return m_indicator != null;
	}
}
