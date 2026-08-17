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

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.ReplaceInstanceSymbolActionUI;
import chs.caf.caplet.ICaplet;

@ApplicationSpecification(
		includeIn = {Application.SvcDoc})
public class PublisherReplaceInstanceSymbolActionUI extends ReplaceInstanceSymbolActionUI
{

	public PublisherReplaceInstanceSymbolActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public String getActionClass()
	{
		return PublisherReplaceInstanceSymbolAction.class.getName();
	}
}
