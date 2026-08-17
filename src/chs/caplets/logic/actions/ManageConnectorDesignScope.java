/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2016-2025 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.capitalmanager.appserver.IUserSession;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ManageConnectorDesignScope
{

	private Collection<IDesignDescriptor> designsInScope;
	private Collection<IDesignDescriptor> editableDesigns;
	private Collection<ILogicDesign> lockedInAction;
	private boolean allEditsConsidered;
	private boolean isReadonly;

	public ManageConnectorDesignScope(Collection<IDesignDescriptor> designsInScope, boolean considerAllDesignForEdit)
	{
		this.designsInScope = designsInScope;
		editableDesigns = new LinkedHashSet<>();
		lockedInAction = new LinkedHashSet<>();
		if (considerAllDesignForEdit) {
			makeAllDesignsInScopeEditable();
		}
		allEditsConsidered = considerAllDesignForEdit;
	}

	public ManageConnectorDesignScope(IBuildList buildList, ISharedPinList sharedPinList,
			boolean considerAllDesignForEdit,
			@Nullable Predicate<Collection<IDesignDescriptor>> isCurrentOpenenedDesignAcceptable)
	{

		designsInScope = getLogicDesignsInScope(sharedPinList, buildList);
		isReadonly =
				isCurrentOpenenedDesignAcceptable != null && !isCurrentOpenenedDesignAcceptable.test(designsInScope);

		editableDesigns = new LinkedHashSet<>();
		lockedInAction = new LinkedHashSet<>();
		if (considerAllDesignForEdit && !isReadonly) {
			makeAllDesignsInScopeEditable();
		}
		allEditsConsidered = considerAllDesignForEdit;
	}

	public ManageConnectorDesignScope(ISharedPinList sharedPinList)
	{
		designsInScope = getLogicDesignsInScope(sharedPinList, null);
		editableDesigns = new LinkedHashSet<>();
		lockedInAction = new LinkedHashSet<>();
	}

	public Collection<IDesignDescriptor> getDesignsInScope()
	{
		return designsInScope;
	}

	public void addDesignToDesignsInScope(@Nullable IDesignDescriptor design)
	{
		if (design != null) {
			designsInScope.add(design);
		}
	}

	public boolean isDesignInScope(IDesignDescriptor design)
	{
		return designsInScope.contains(design);
	}

	public boolean isDesignEditable(IDesignDescriptor design)
	{
		return editableDesigns.contains(design);
	}

	public Collection<IDesignDescriptor> makeAllDesignsInScopeEditable()
	{
		setAllEditsConsidered(true);
		return addEditableDesigns(designsInScope);
	}

	/**
	 * Remove designs from the designScope that are not in the colection of allowed designs. This
	 * is most likely of use in CapitalTopology where the designScope has been build from a shared
	 * connector but only the associated designs are to be included
	 *
	 * @param allowedDesigns the designs to retain in the designScope
	 */
	public void restrictDesignsInScope(@NotNull Collection<? extends IDesignDescriptor> allowedDesigns)
	{
		designsInScope.retainAll(allowedDesigns);
	}

	public void setAllEditsConsidered(boolean areAllEditsConsidered)
	{
		allEditsConsidered = areAllEditsConsidered;
	}

	public boolean areAllEditsConsidered()
	{
		return allEditsConsidered;
	}

	public Collection<IDesignDescriptor> addEditableDesigns(Collection<IDesignDescriptor> toBeAddededitableDesigns)
	{
		Set<ILogicDesign> toLock = new LinkedHashSet<>();
		for (IDesignDescriptor anEditableDesign : toBeAddededitableDesigns) {

			boolean isDesignEditable = anEditableDesign.isEditable();
			if (isDesignEditable) {
				IDesignContainer loadedDesignContainer = anEditableDesign.getLoadedDesignContainer();
				if (loadedDesignContainer != null) {
					isDesignEditable = !CAFUtils.getInstance().isDesignOpenReadOnly(loadedDesignContainer);
				}
			}
			if (designsInScope.contains(anEditableDesign) && isDesignEditable) {
				if (anEditableDesign instanceof ILogicDesign) {
					ILogicDesign logicDesign = (ILogicDesign) anEditableDesign;
					if (logicDesign.isLocked()) {
						editableDesigns.add(anEditableDesign);
					}
					else {
						toLock.add(logicDesign);
					}
				}
			}
		}

		final Set<IUID> failed = batchLockReturnFailures(toLock);
		for(ILogicDesign logicDesign : toLock) {
			if(!failed.contains(logicDesign.getUID())) {
				editableDesigns.add(logicDesign);
				lockedInAction.add(logicDesign);
			}
		}
		return editableDesigns;
	}

	@NotNull protected Set<IUID> batchLockReturnFailures(@NotNull Set<ILogicDesign> toLock)
	{
		return LogicUtils.batchLockDesigns(toLock);
	}

	@NotNull private Collection<IDesignDescriptor> getLogicDesignsInScope(ISharedPinList sharedPinList,
			@Nullable IBuildList buildList)
	{
		Collection<IDesignDescriptor> setOfDesigns = null;
		if (buildList != null) {
			setOfDesigns = buildList.getDesignDescriptors().stream()
					.filter(aDescriptor -> aDescriptor.getDesignType().isLogicBasedDesign())
					.collect(Collectors.toSet());
		}

		IProject project = sharedPinList.getProject();

		IUserSession userSession = UtilsHelper.getCHSSystem().getUserSession();

		Collection<IDesignDescriptor> requiredDesignsInScope = new LinkedHashSet<>();
		if (userSession != null) {
			Collection<IUID> designUIDs = LogicUtils
					.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(project, Collections.singleton(sharedPinList),
							setOfDesigns);

			// Batch load designs using the design UIDs
			Collection<IDesignContainer> designs = project.getDesignMgr().getDesigns(designUIDs);

			// Filter and cast to ILogicDesign
			requiredDesignsInScope = designs.stream()
					.filter(design -> design instanceof ILogicDesign) // Only keep logic designs
					.map(design -> (ILogicDesign) design) // Cast to ILogicDesign
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}

		return requiredDesignsInScope;
	}

	public boolean areMultipleDesignsEdited()
	{
		return editableDesigns.size() > 1;
	}

	public void releaseDesignLocks()
	{
		if (lockedInAction != null) {
			lockedInAction.stream()
					.filter(design -> design.isLocked())
					.forEach(design -> design.unlock());
		}
	}

	public Collection<IDesignDescriptor> getEditableDesigns()
	{
		return editableDesigns;
	}

	public boolean isReadonly()
	{
		return isReadonly;
	}

	/**
	 * Remove the design from the list of designs that should be considered for unlocking, this may be
	 * because the user has opened the design from the ManageConnectors dialog
	 *
	 * @param design to remove from the likst of designs to be considered for unlokcing
	 */
	public void removeFromLockedInAction(@NotNull ILogicDesign design)
	{
		lockedInAction.remove(design);
	}
}
