/*
 * Copyright 2004-2014 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.IAnalysisNetlistScope;
import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caplets.logic.Model;
import chs.utilities.ui.tree.Drawer;

import javax.swing.Action;

/**
 * @author rharring
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalEssentialsDesign, Application.ArtisanArchitect, Application.SEElectricalDesign})
public abstract class AnalysisDrawerPersistentUI extends ActionUI
{

	protected AnalysisDrawerPersistentUI(ICaplet c)
	{
		super(c);
	}

	public void setupUI()
	{
		super.putValue(Drawer.DRAWER_PERSISTENT, Boolean.TRUE);
	}

	public void putValue(String key, Object newValue, boolean prefix)
	{
		if (prefix && key.startsWith(Drawer.DRAWER_PREFIX)) {
			String tempKey = key + getDrawerCode();
			super.putValue(tempKey, newValue);
		}
		else {
			super.putValue(key, newValue);
		}
	}

	public void putValue(String key, Object newValue)
	{
		putValue(key, newValue, true);
	}

	public Object getValue(String key)
	{
		if (key.startsWith(Drawer.DRAWER_PREFIX) && !key.equals(Drawer.DRAWER_PERSISTENT)) {
			String tempKey = key + getDrawerCode();
			return super.getValue(tempKey);
		}
		else {
			return super.getValue(key);
		}
	}

	protected String getDrawerCode()
	{
		if (CAFUtils.getInstance().getActiveCapletController() != null) {
			if (CAFUtils.getInstance().getActiveCapletController().getAction(getActionName()) != null) {

				IAction action = CAFUtils.getInstance().getActiveCapletController().getAction(getActionName());
				if (action instanceof ControllerActionRT) {
					ICapletModel model = ((ActionRT) action).getController().getCapletModel();
					if (model instanceof Model) {
						Model lmodel = (Model) model;
						return getDrawerCode(lmodel);
					}
				}
				return String.valueOf(action.hashCode());
			}
		}
		return "";
	}

	protected String getDrawerCode(Model model)
	{
		IAnalysisNetlistScope scope = AnalysisServices.getCurrentAnalysisNetlistScope();
		if (scope != null && scope.isInScope(model.getDesign())) {
			return scope.getUid();
		}
		else {

			return model.getDesign().getUID().toString();
		}
	}

	public String getDrawerPrefix(Model model)
	{
		return Drawer.DRAWER_SELECTED + getDrawerCode(model);
	}

	/**
	 * This method allows the DRAWER_SELECTED property to be fired externally.
	 *
	 * @param newAction, the new action to be selected.
	 */
	public void updateSelection(Action newAction)
	{
		firePropertyChange(Drawer.DRAWER_SELECTED, this, newAction);
	}
}
