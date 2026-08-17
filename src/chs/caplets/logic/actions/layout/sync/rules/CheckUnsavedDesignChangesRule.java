/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.common.sync.AbstractFunctionalSyncReporter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CheckUnsavedDesignChangesRule extends AbstractLayoutDesignSyncRule
{

	public CheckUnsavedDesignChangesRule(@NotNull AbstractLayoutDesignSync sync)
	{
		super(sync);
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return "CheckUnsavedDesignChangesRule";
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final List<ILogicDesign> designBeingEdited = getAssociatedDesignsBeingEdited(design);
		if (!designBeingEdited.isEmpty()) {
			final String designNames = designBeingEdited.stream()
					.map(d -> d.getFullName())
					.collect(Collectors.joining(", "));
			reporter.reportErrorMessage("CheckUnsavedDesignChangesRule.designsBeingEdited.error", designNames);
			return false;
		}
		return true;
	}

	@NotNull public List<ILogicDesign> getAssociatedDesignsBeingEdited(@NotNull ILayoutLogicDesign design)
	{
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		// Check if any associated/ to be associated designs are being edited in current session
		final List<ILogicDesign> designBeingEdited = new ArrayList<>();
		for (ISourceDesignRef systemLogicDesignRef : layoutDesignMgr.getSystemLogicDesignRefs()) {
			final ILogicDesign logicDesign = systemLogicDesignRef.getReferencedDesign();
			if (logicDesign != null) {
				if (getCommandHelper().isOpenModifiedDesign(logicDesign)) {
					designBeingEdited.add(logicDesign);
				}
			}
		}
		return designBeingEdited;
	}
}
