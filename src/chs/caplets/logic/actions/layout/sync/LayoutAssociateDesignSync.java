/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout.sync;

import chs.caplets.logic.actions.layout.sync.rules.SyncAssociatedSourceDesignsRule;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.buildlist.IBuildList;
import chs.common.ICommandHelper;
import chs.common.IDesignDescriptor;
import chs.common.sync.ISyncRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LayoutAssociateDesignSync extends AbstractLayoutDesignSync
{

	@NotNull private List<ILogicDesign> mAssociatedDesigns;
	@Nullable protected IBuildList mBuildList;

	/**
	 * @param theDesign Design that will be modified
	 * @param theCommandHelper Abstract CommandHelper to give access to framework implementation
	 * @param asDesigns associated designs
	 * @param associatedBuildList associated buildlist
	 */
	public LayoutAssociateDesignSync(@NotNull ILayoutLogicDesign theDesign,
			@NotNull ICommandHelper theCommandHelper,
			@NotNull List<ILogicDesign> asDesigns,
			@Nullable IBuildList associatedBuildList)
	{
		super(theDesign, theCommandHelper);
		mAssociatedDesigns = asDesigns;
		mBuildList = associatedBuildList;
	}

	@NotNull @Override protected String getNoSyncMessageResource()
	{
		return "LayoutAssociateDesignSync.noSyncRequired";
	}

	@Override protected boolean hasDesignsAssociated()
	{
		// If no changes had occured in associate design UI, we wouldn't have gotten here
		return true;
	}

	@Override public boolean syncRequired()
	{
		// If no changes had occured in associate design UI, we wouldn't have gotten here
		return true;
	}

	@Override protected void doAddAssociateDesignsRules(@NotNull List<ISyncRule<ILayoutLogicDesign>> rules)
	{
		rules.add(new SyncAssociatedSourceDesignsRule(this, mAssociatedDesigns, mBuildList));
	}

	@NotNull @Override protected Collection<IDesignDescriptor> getEffectivelyAssociatedDesigns()
	{
		return Collections.unmodifiableList(mAssociatedDesigns);
	}
}
