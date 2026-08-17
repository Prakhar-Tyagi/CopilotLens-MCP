/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.ActionContainer;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.publisher.PublisherPropertiesAction;
import chs.ctf.caf.ui.TextAttributesEditor;

public class PublisherSmartEditPropertiesAction extends PublisherPropertiesAction
{

	public PublisherSmartEditPropertiesAction(ICapletController controller, IPropertiesClient client,
			TextAttributesEditor textAttrEditor)
	{
		super(controller, client,
				textAttrEditor);	//To change body of overridden methods use File | Settings | File Templates.
	}

	public boolean isEnabled()
	{
		// todo ActionHierarchy this action does not call super.isEnabled - is this correct
		// This will make enabling and disabling from the framework difficult
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		// Ask the client if the selections are something they edit the
		// properties of.  If so, put ourselves in the context menu.
		return m_client.doSelectionsHaveProperties(selections) && isModeEnabled();
	}

	@Override public String getActionUIClass()
	{
		return PublisherSmartEditPropertiesActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
