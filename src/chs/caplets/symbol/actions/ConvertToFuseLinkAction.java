/*
 * Copyright 2010-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.ICapletController;
import chs.caf.ICtxMenuProvider;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.common.IUIDObject;

import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Feb 16, 2010 Time: 7:25:36 PM To change this template use File |
 * Settings | File Templates.
 */
public class ConvertToFuseLinkAction extends ControllerActionRT implements ICtxMenuProvider
{

	Set<ISchemInternalLink> m_linksToEdit;

	public ConvertToFuseLinkAction(ICapletController controller)
	{
		super(controller);
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_linksToEdit = registerLinks();
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		for (ISchemInternalLink link : m_linksToEdit) {
			link.getConnectivity().setLinktype("Fusing");
			link.getConnectivity().setDCRes(0.0);
			link.getConnectivity().setIMax(0.0);
		}
		return true;
	}

	private Set<ISchemInternalLink> registerLinks()
	{
		Set<ISchemInternalLink> linksToEdit = new HashSet<ISchemInternalLink>();
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		SelectionFilter schemInternalLinksFilter = new SelectionFilter(ISchemInternalLink.class);

		int selectionCount = selections.getSelectCount();
		if (selectionCount > 0) {
			SelectedUIDObjectIterator selectedObjIter = selections.getSelectedUIDObjects();
			//When a link is selected on the symbol, two objects are selected 1.InternalLinkPolyLine 2.CAFSchemInternalLink
			if (selectionCount / 2 == selections.getSelectCount(schemInternalLinksFilter)) {
				while (selectedObjIter.hasNext()) {
					IUIDObject obj = selectedObjIter.getNext();
					if (obj instanceof ISchemInternalLink) {
						String linkType = ((ISchemInternalLink) obj).getConnectivity().getLinkType();
						if (linkType.equalsIgnoreCase("Diode") || linkType.equalsIgnoreCase("Resistance")) {
							linksToEdit.add((ISchemInternalLink) obj);
						}
					}
				}
			}
		}
		return linksToEdit;
	}

	public boolean isEnabled()
	{
		if (registerLinks().size() == 0) {
			return false;
		}
		else {
			return super.isEnabled();
		}
	}

	public String getActionUIClass()
	{
		return ConvertToFuseLinkActionUI.class.getName();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
