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
import chs.caplets.publisher.PublisherPropertiesActionUI;

public class PublisherSmartEditPropertiesActionUI extends PublisherPropertiesActionUI
{

	public PublisherSmartEditPropertiesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		super.setupUI();
		putValue(ACCELERATOR_KEY, null);
	}

	public String getActionClass()
	{
		return PublisherSmartEditPropertiesAction.class.getName();
	}
}
