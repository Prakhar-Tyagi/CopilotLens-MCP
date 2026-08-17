/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2023 Siemens
 */

package chs.caplets.logic.actions.layout.sync.rules;

import chs.caplets.logic.actions.layout.sync.AbstractLayoutDesignSync;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.common.UIDUtils;
import chs.common.ModificationTimeStampData;
import chs.common.sync.AbstractBaseSync;
import chs.common.sync.AbstractFunctionalSyncReporter;
import chs.utility.IDCRefUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

public class SyncAssociatedSourceDesignsRule extends AbstractLayoutDesignSyncRule
{

	// A list of the designs to be associated - from the associated designs dialog normally.
	// Will include designs to add and will not contain designs to remove.
	@Nullable private Collection<IDesignDescriptor> mAssociatedDesigns;
	@Nullable private IBuildList mBuildList;

	public SyncAssociatedSourceDesignsRule(@NotNull AbstractLayoutDesignSync sync,
			@Nullable Collection<? extends IDesignDescriptor> modifiedDesignList, @Nullable IBuildList newBuildList)
	{
		super(sync);
		mAssociatedDesigns = (Collection<IDesignDescriptor>) modifiedDesignList;
		mBuildList = newBuildList;
	}

	@NotNull @Override protected String getMessageSourceResourceName()
	{
		return AbstractBaseSync.CHECKING_ASSOCIATED_DESIGNS;
	}

	@Override protected boolean doExecute(@NotNull ILayoutLogicDesign design,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		final ILayoutDesignMgr layoutDesignMgr = design.getLayoutDesignMgr();
		// First off, set/unset the build list if this was changed.  Then remove all the designs from associatedDesigns
		syncDesignsToBuildList(layoutDesignMgr);

		// Delete any designs which are no longer associated (or update the refs if they are different revisions
		removeUnassociatedDesigns(layoutDesignMgr, reporter);

		// Add any designs which are in the associated list but not the topoMgr has no refs to (newly associated)
		addNewlyAssociatedDesigns(layoutDesignMgr, reporter);
		return true;
	}

	/**
	 * Make sure all the associated designs belong to the build list also, if not, remove them
	 *
	 * @param layoutDesignMgr layout design manager
	 */
	private void syncDesignsToBuildList(@NotNull ILayoutDesignMgr layoutDesignMgr)
	{
		if (mAssociatedDesigns == null) {
			// We didn't come from anything that can legitimately change the build list.
			return;
		}

		if (mBuildList != layoutDesignMgr.getBuildList()) {
			layoutDesignMgr.setBuildList(mBuildList);
		}

		// Remove designs that aren't in the build list from the final list of designs that should be associated
		if (mBuildList != null) {
			for (Iterator<IDesignDescriptor> iter = mAssociatedDesigns.iterator(); iter.hasNext(); ) {
				IDesignDescriptor des = iter.next();
				if (!mBuildList.containsDesignUID(des.getUID())) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Remove all the references from TopoMgr that point to designs we no longer want to be associated with
	 *
	 * @param layoutDesignMgr layout design manager
	 * @param reporter        Reporter
	 */
	private void removeUnassociatedDesigns(@NotNull ILayoutDesignMgr layoutDesignMgr,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		if (mAssociatedDesigns == null) {
			// We didn't come from anything that can legitimately remove associated designs.
			return;
		}
		Collection<IUID> associatedDesigUids = UIDUtils.convertToUID(mAssociatedDesigns);
		for (ISourceDesignRef ref : layoutDesignMgr.getSystemLogicDesignRefs()) {
			IDesignDescriptor referencedDesign = ref.getReferencedDesign();
			assert referencedDesign != null;
			//noinspection ConstantConditions
			if (referencedDesign != null && !associatedDesigUids.contains(referencedDesign.getUID())) {
				layoutDesignMgr.removeSystemLogicDesignRef(ref);
				reporter.reportSyncChangesMade("SyncAssociatedSourceDesignsRule.unassociateDesign",
						referencedDesign.getFullName());
				ref.delete();
			}
		}
	}

	/**
	 * Add new refs to the TopoMgr if they are now required to be associated designs.
	 *
	 * @param layoutDesignMgr layout design manager
	 * @param reporter        Reporter
	 */
	private void addNewlyAssociatedDesigns(@NotNull ILayoutDesignMgr layoutDesignMgr,
			@NotNull AbstractFunctionalSyncReporter<ILayoutLogicDesign> reporter)
	{
		if (mAssociatedDesigns == null) {
			// We didn't come from anything that can legitimately add associated designs.
			return;
		}
		// Now go and add FDRs for any functional designs we don't already have
		for (IDesignDescriptor design : mAssociatedDesigns) {
			if (!layoutDesignMgr.refForSystemLogicDesignExists(design.getUID())) {
				ISourceDesignRef sdr = IDCRefUtils.constructSourceLogicDesignRef(design);
				// Actual time stamp will be set at the end of sync
				// construct the ModificationTimeStampData with initial values
				// preserving the initial -1 for the modified time stamp attribute
				sdr.setModificationTimeStampData(new ModificationTimeStampData().invalidateTimeStamps());
				layoutDesignMgr.addSystemLogicDesignRef(sdr);
				reporter.reportSyncChangesMade("SyncAssociatedSourceDesignsRule.associateDesign", design.getFullName());
			}
		}
	}
}
