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

// caf imports

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;

/**
 * * Called when the user attempts to cut some object(s) * * The method is undoable, becuase it makes changes to the
 * caplet model
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalArchitect, Application.CapitalEssentialsDesign, Application.ArtisanArchitect,
				Application.SEElectricalDesign})
public class AnalysisPopupMenuBuilderActionUI extends ActionUI
{

	public AnalysisPopupMenuBuilderActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setupUI()
	{
	}

	public void updateUI()
	{
		//System.err.println("In updateUI of AnalysisPopupMenuBuilderActionUI")  ;
		super.updateUI();
		AnalysisPopupMenuBuilderAction action = (AnalysisPopupMenuBuilderAction) getAction();
		if (action != null) {
			//System.err.println("Calling update of AnalysisPopupMenuBuilderAction" ) ;
			action.updateUI();
		}
	}

	public String getActionClass()
	{
		return AnalysisPopupMenuBuilderAction.class.getName();
	}
}