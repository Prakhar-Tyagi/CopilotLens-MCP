/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2025 Siemens
 */

package chs.caplets.logic.actions.layout.associatedesigns;

import chs.caf.IStatusBar;
import chs.caf.caplet.helpers.associatedesigns.AbstractAssociateDesignsModel;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.common.IDesignDescriptor;
import chs.common.IDesignMgr;
import chs.common.IUID;
import chs.system.UIDMgr;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AssociateDesignsModel extends AbstractAssociateDesignsModel
{

	@NotNull private ILayoutDesignMgr mLayoutDesignMgr;
	@NotNull private Set<IUID> mDeletedDesignRefs;

	public AssociateDesignsModel(@NotNull ILayoutDesignMgr layoutDesignMgr, @NotNull IBuildListMgr buildListMgr,
			@NotNull IDesignMgr designManager, @NotNull IStatusBar statusBar)
	{
		super(buildListMgr, designManager, statusBar);
		mLayoutDesignMgr = layoutDesignMgr;
		// Initialize the associated designs list
		originalAssociatedDesignMap.putAll(getOriginalAssociations());
		mDeletedDesignRefs = getMissingDesignReferences(layoutDesignMgr, designManager);
		associatedDesignsMap.putAll(originalAssociatedDesignMap);
		setAssociatedBuildList(mLayoutDesignMgr.getBuildList());
	}

	@NotNull private static Set<IUID> getMissingDesignReferences(@NotNull ILayoutDesignMgr layoutDesignMgr,
			@NotNull IDesignMgr designManager)
	{
		final Set<IUID> missingDesigns = new HashSet<>();
		layoutDesignMgr.getSystemLogicDesignRefs().stream()
				.filter(iFunctionalDesignRef -> designManager.getDesignDescriptor(iFunctionalDesignRef.getReferencedDesignUID()) == null)
				.forEach(iFunctionalDesignRef -> missingDesigns.add(iFunctionalDesignRef.getReferencedDesignUID()));

		return Collections.unmodifiableSet(missingDesigns);
	}

	@NotNull @Override protected Set<IDesignDescriptor> getPlacedDesigns()
	{
		// This is used by 'Remove unplaced' button in DEV Extension. Not supported in layout design associate design UI
		return Collections.emptySet();
	}

	@NotNull @Override protected Map<IUID, IDesignDescriptor> getOriginalAssociations()
	{
		final Map<IUID, IDesignDescriptor> currentAssociations = new HashMap<>();
		mLayoutDesignMgr.getSystemLogicDesignRefs().stream()
				.map(iFunctionalDesignRef -> designMgr.getDesignDescriptor(
						iFunctionalDesignRef.getReferencedDesignUID()))
				.filter(Objects::nonNull)
				.forEach(iDesignDescriptor -> currentAssociations.put(iDesignDescriptor.getDesignBaseId(),
						iDesignDescriptor));
		return currentAssociations;
	}

	@Override protected boolean isBuildListSame()
	{
		return associatedBuildList == mLayoutDesignMgr.getBuildList();
	}

	@Override public boolean hasLogicalAssociationChanges()
	{
		// If a logic design has gone missing, we probably should do a sync!
		return super.hasLogicalAssociationChanges() || !mDeletedDesignRefs.isEmpty();
	}
}
