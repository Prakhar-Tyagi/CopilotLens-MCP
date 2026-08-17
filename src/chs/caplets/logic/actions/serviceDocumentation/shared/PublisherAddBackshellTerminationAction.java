/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.serviceDocumentation.shared;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.AddBackshellTerminationAction;
import chs.caplets.logic.actions.AddBackshellTerminationActionUI;
import chs.caplets.logic.actions.AddBackshellTerminationDialog;
import chs.caplets.logic.actions.IBackshellUtils;
import chs.cof.drawplus.ISupplementaryObject;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.common.INamedPropertiedObject;
import chs.utilities.ResourceMgr;
import chs.utility.ui.BaseDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Difference between this action and its parent are
 * <p>
 * 1.Only allow on those pinlists which already have a BST
 * <p>
 * 2.Mark added BST as supplementary
 */
public class PublisherAddBackshellTerminationAction extends AddBackshellTerminationAction
{

	public PublisherAddBackshellTerminationAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return UI.class.getName();
	}

	protected void setUpActionHelper()
	{
		m_addPinActionHelper = new PublisherAddBSTerminationActionHelper(this);
	}

	protected void processNewSchemTerms(Set<IPin> newSchemTerms)
	{
		super.processNewSchemTerms(newSchemTerms);
		newSchemTerms
				.stream()
				.forEach(ISupplementaryObject::markAsSupplementary);
	}

	@Override public boolean isEnabled()
	{
		boolean enabled = super.isEnabled();
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		IPinList pinList =
				m_addPinActionHelper.getPinListThatAllowsBackshellAddition(selections);
		if (pinList != null) {
			chs.cof.logical.cable.IPinList connectivity = pinList.getConnectivity();
			if (connectivity instanceof IConnector && enabled) {
				IBackshellUtils backshellUtils = new IBackshellUtils()
				{
					@NotNull @Override public IConnector getConnector()
					{
						return (IConnector) connectivity;
					}
				};
				Set<INamedPropertiedObject> existingBackshellTerminations =
						backshellUtils.getExistingBackshellTerminations();
				return !existingBackshellTerminations.isEmpty();
			}
		}
		return false;
	}

	@NotNull public Object getNextBackshellTermination()
	{
		Set<INamedPropertiedObject> existingBackshellTerminations = getExistingBackshellTerminations();
		assert !existingBackshellTerminations.isEmpty();
		return existingBackshellTerminations.iterator().next();
	}

	protected boolean selectNextBackshellTermination()
	{
		Set<INamedPropertiedObject> existingBackshellTerminations = getExistingBackshellTerminations();
		assert !existingBackshellTerminations.isEmpty();
		if (existingBackshellTerminations.size() == 1) {
			selectTermination(existingBackshellTerminations.iterator().next());
			return true;
		}
		else {
			BaseDialog dialog = new AddBackshellTerminationDialog(getFrame(), getTitle(), true, this);
			dialog.setVisible(true);
			return !dialog.isCancelled();
		}
	}

	@ApplicationSpecification(
			includeIn = {Application.SvcDoc})
	public static class UI extends AddBackshellTerminationActionUI
	{

		public UI(ICaplet caplet)
		{
			super(caplet);
		}

		@NotNull @Override public String getActionClass()
		{
			return PublisherAddBackshellTerminationAction.class.getName();
		}

		@Override public void setupUI()
		{
			super.setupUI();
			putValue(NAME, ResourceMgr.getString(PublisherAddBackshellTerminationAction.class,
					"PublisherAddBackshellTerminationAction.name.decl"));
			putValue(SHORT_DESCRIPTION,
					ResourceMgr.getString(PublisherAddBackshellTerminationAction.class,
							"PublisherAddBackshellTerminationAction.shortDesc.decl"));
			putValue(LONG_DESCRIPTION,
					ResourceMgr.getString(PublisherAddBackshellTerminationAction.class,
							"PublisherAddBackshellTerminationAction.longDesc.decl"));
		}
	}
}
