/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.caplet.ICaplet;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;

@ApplicationSpecification(
		includeIn = {Application.SvcDoc})
public class SelectActionUI extends chs.caplets.shared.actions.SelectActionUI
{

	/**
	 * Constructor for the SelectActionUI object
	 *
	 * @param caplet Caplet this UI belongs to
	 */
	public SelectActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public String getActionClass()
	{
		return SelectActionImpl.class.getName();
	}
}
