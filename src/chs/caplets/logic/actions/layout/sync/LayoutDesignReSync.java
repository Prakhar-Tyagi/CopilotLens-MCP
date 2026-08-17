/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2020-2025 Siemens
 */


package chs.caplets.logic.actions.layout.sync;

import chs.caplets.logic.actions.layout.sync.rules.SyncAssociatedSourceDesignsRule;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISourceDesignRef;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.ICommandHelper;
import chs.common.IDesignDescriptor;
import chs.common.IDesignMgr;
import chs.common.IPrivilegedDesignMgr;
import chs.common.IUID;
import chs.common.UIDUtils;
import chs.common.sync.ISyncRule;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LayoutDesignReSync extends AbstractLayoutDesignSync
{

	/**
	 * @param theDesign Design that will be modified
	 * @param theCommandHelper Abstract CommandHelper to give access to framework implementation
	 */
	public LayoutDesignReSync(@NotNull ILayoutLogicDesign theDesign, @NotNull ICommandHelper theCommandHelper)
	{
		super(theDesign, theCommandHelper);
	}

	@NotNull @Override protected String getNoSyncMessageResource()
	{
		return "LayoutDesignReSync.noSyncRequired";
	}

	@Override protected void doAddAssociateDesignsRules(@NotNull List<ISyncRule<ILayoutLogicDesign>> rules)
	{
		rules.add(new SyncAssociatedSourceDesignsRule(this, getEffectivelyAssociatedDesigns(),
				getLayoutDesignMgr().getBuildList()));
	}

	@NotNull @Override protected Collection<IDesignDescriptor> getEffectivelyAssociatedDesigns()
	{
		final IBuildList buildList = getLayoutDesignMgr().getBuildList();
		final IDesignMgr designMgr = getDesignMgr();
		return buildList != null ? getDesignsFromBuildlist(buildList, designMgr) :
				getCurrentlyAssociatedDesigns(designMgr);
	}

	@NotNull private List<IDesignDescriptor> getCurrentlyAssociatedDesigns(@Nullable IDesignMgr designMgr)
	{
		//Some of these designs could have been removed from DesignMgr after refresh
		return getCurrentlyAssociatedDesigns().stream()
				.filter(d -> !isDesignMissing(designMgr, d.getUID()))
				.collect(Collectors.toList());
	}

	@NotNull private List<ILogicDesign> getCurrentlyAssociatedDesigns()
	{
		IDesignMgr designMgr = getDesignMgr();

		if (designMgr == null) {
			return Collections.emptyList();
		}

		List<IUID> designsToLoad = getLayoutDesignMgr().getSystemLogicDesignRefs().stream()
				.map(iSourceDesignRef -> iSourceDesignRef.getReferencedDesignUID())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		List<ILogicDesign> logicDesigns = designMgr.getDesigns(designsToLoad).stream()
				.map(iDesignContainer -> CommonUtils.cast(iDesignContainer, ILogicDesign.class))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		return Collections.unmodifiableList(logicDesigns);
	}

	@Override protected boolean hasDesignsAssociated()
	{
		return getLayoutDesignMgr().getBuildList() != null || getLayoutDesignMgr().getNumSystemLogicDesignRefs() > 0;
	}

	@Override public boolean syncRequired()
	{
		final IDesignMgr designMgr = getDesignMgr();
		final ILayoutDesignMgr layoutDesignMgr = getLayoutDesignMgr();
		for (ISourceDesignRef ref : layoutDesignMgr.getSystemLogicDesignRefs()) {
			ILogicDesign logicDesign = ref.getReferencedDesign();
			// If the design can't be found, it's been deleted.  If we also can't find the design's UID in the project's
			// list of designs, it's still been deleted, but not unloaded yet.
			final IUID referencedObjectUID = ref.getDesignReference().getReferencedObjectUID();
			if (logicDesign == null || isDesignMissing(designMgr, referencedObjectUID)) {
				// If a logic design has gone walkies, we probably should do a sync!
				return true;
			}
		}

		if (isBuildlistModified(layoutDesignMgr, designMgr)) {
			return true;
		}

		// If we have any modified designs, we want to sync
		if (areAssociatedDesignsModified(layoutDesignMgr)) {
			return true;
		}

		// If we have any modified shared objects, we want to sync
		return areSharedObjectsModified(layoutDesignMgr);
	}

	@Nullable private IDesignMgr getDesignMgr()
	{
		final IProject project = getDesign().getProject();
		return project != null ? project.getDesignMgr() : null;
	}

	private boolean isBuildlistModified(@NotNull ILayoutDesignMgr layoutDesignMgr, @Nullable IDesignMgr designMgr)
	{
		final IBuildList buildList = layoutDesignMgr.getBuildList();
		if (buildList == null) {
			return false;
		}
		final List<IDesignDescriptor> designsFromBuildlist = getDesignsFromBuildlist(buildList, designMgr);
		assert designMgr != null;
		final List<IDesignDescriptor> currentlyAssociatedDesignDescriptors = designMgr.getDesignDescriptors(UIDUtils.convertToUID(getCurrentlyAssociatedDesigns()));

		return !(designsFromBuildlist.containsAll(currentlyAssociatedDesignDescriptors) &&
				currentlyAssociatedDesignDescriptors.containsAll(designsFromBuildlist));
	}

	@NotNull
	private List<IDesignDescriptor> getDesignsFromBuildlist(@NotNull IBuildList buildList, @Nullable IDesignMgr designMgr)
	{
		if (designMgr == null) {
			return Collections.emptyList();
		}
		List<IDesignDescriptor> buildListDesigns = new ListSet<>();
		for (IDesignDescriptor design : designMgr.getLogicalDesignDescriptors()) {
			if (buildList.containsDesignUID(design.getUID())) {
				buildListDesigns.add(design);
			}
		}
		return buildListDesigns;
	}

	@Override protected void refreshDesigns()
	{
		super.refreshDesigns();
		final IProject project = getDesign().getProject();
		if (project != null) {
			project.updateDesignLists();
		}
	}

	@Override protected boolean confirmToProceed()
	{
		return showDialogToConfirmProceed();
	}

	private boolean showDialogToConfirmProceed()
	{
		ResourceBasedMessageContent messageContent =
				new ResourceBasedMessageContent(LayoutDesignReSync.class, "LayoutDesignReSync.confirmToProceedForSync");
		Choice chProceed = new Choice(LayoutDesignReSync.class,
				"AbstractLayoutDesignSync.confirmToProceedForSync.choice.proceed");
		Choice chCancel = new Choice(LayoutDesignReSync.class,
				"AbstractLayoutDesignSync.confirmToProceedForSync.choice.cancel");
		Choice response = doShowDialogToConfirmProceed(messageContent, chProceed, chCancel);
		return response != chCancel;
	}

	@Nullable protected Choice doShowDialogToConfirmProceed(@NotNull ResourceBasedMessageContent messageContent,
			@NotNull Choice chProceed, @NotNull Choice chCancel)
	{
		return Question.show(messageContent, chProceed, chCancel);
	}

	private boolean areSharedObjectsModified(@NotNull ILayoutDesignMgr layoutDesignMgr)
	{
		for (ISourceObjectRef sourceObjectRef : layoutDesignMgr.getAllSourceObjectRefs()) {
			if (hasSharedObjectChanged(sourceObjectRef)) {
				return true;
			}
		}
		return false;
	}

	private boolean areAssociatedDesignsModified(@NotNull ILayoutDesignMgr layoutDesignMgr)
	{
		for (ISourceDesignRef ref : layoutDesignMgr.getSystemLogicDesignRefs()) {
			final long lastSyncTimeStamp = ref.getTimestamp();
			final ILogicDesign logicDesign = ref.getReferencedDesign();
			if (logicDesign != null) {
				if (logicDesign.getTimeModified() != lastSyncTimeStamp) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDesignMissing(@Nullable IDesignMgr designMgr, @Nullable IUID referencedObjectUID)
	{
		if (designMgr == null || referencedObjectUID == null) {
			return true;
		}
		return designMgr.getLogicalDesign(referencedObjectUID) == null;
	}
}
