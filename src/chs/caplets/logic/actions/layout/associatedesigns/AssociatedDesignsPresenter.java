/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.associatedesigns;

import chs.caf.caplet.helpers.associatedesigns.AbstractAssociatedDesignsPresenter;
import chs.caf.caplet.helpers.associatedesigns.IAssociateDesignsModel;
import chs.caf.caplet.helpers.associatedesigns.IAssociatedDesignsButtons;
import chs.caf.caplet.helpers.associatedesigns.IInvokeSelectDesignsPresenter;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AssociatedDesignsPresenter extends AbstractAssociatedDesignsPresenter
{

	public AssociatedDesignsPresenter(
			@NotNull IAssociateDesignsModel associateDesignsModel,
			@NotNull IInvokeSelectDesignsPresenter selectPresenter,
			@NotNull IAssociatedDesignsButtons buttons)
	{
		super(associateDesignsModel, selectPresenter, buttons);
		buttons.setPresenter(this);
	}

	@Override protected boolean checkForWiringDesigns(@NotNull List<ILogicDesign> associatedDesigns)
	{
		return false;
	}

	@Override public boolean shouldSupportDragAndDrop()
	{
		return false;
	}

	@Nullable @Override public String getHelpLeafID()
	{
		return null;
	}
}
