/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.capture.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caplets.logic.actions.DeleteActionUI;
import org.jetbrains.annotations.NotNull;

/**
 * Subclass of Logic Delete action UI to refresh styleset when a port from function is deleted.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalCapture, Application.CapitalArchitect, Application.ArtisanFunction})

public class CaptureDeleteActionUI extends DeleteActionUI
{

	public CaptureDeleteActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@NotNull @Override public String getActionClass()
	{
		return CaptureDeleteAction.class.getName();
	}
}
