/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023-2025 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.utilities.IXMLTags;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.StringUtils;
import chs.utility.ui.progress.IProgress;
import chs.utility.ui.progress.ProgressGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Cmd for trying to share objects added by delta across the given designs.
 */
public class DeltaBatchShareCmd extends AbstractDeltaShareCmd
{

	@NotNull private Set<IEntityShareCriteria> m_entitiesShareCriteria;
	@NotNull private SetMap<ShareableEntityTypeEnum, String> m_deltaAddedObjectsMap;

	public DeltaBatchShareCmd(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull SetMap<ShareableEntityTypeEnum, String> deltaAddedObjectsMap)
	{
		super(project, designs, deltaAddedObjectsMap.keySet());

		m_deltaAddedObjectsMap = deltaAddedObjectsMap;
		m_entitiesShareCriteria = getNameMatchingShareCriteria();
		ProgressGroup progressGroup = new ProgressGroup(StringUtils.EMPTY_STRING);
		setFindProgress(progressGroup.createChild(1, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "DeltaBatchShareCmd.MainProgress.FindShareableObjects")));
		setShareProgress(progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "DeltaBatchShareCmd.MainProgress.ShareObjects")));
		setShareIntoProgress(progressGroup.createChild(0, 1,
				ResourceMgr.getString(FindAndShareCmd.class, "DeltaBatchShareCmd.MainProgress.ShareIntoObjects")));
		setProgress(progressGroup);
	}

	@Override protected void doExecute()
	{
		Set<IShareableObjectGroup> shareableObjectGroups =
				findShareableObjectGroups(m_entitiesShareCriteria, getFindProgress());

		if (shareableObjectGroups.isEmpty()) {
			return;
		}
		share(shareableObjectGroups, m_reporter, getShareProgress());
		shareInto(shareableObjectGroups, m_reporter, getShareIntoProgress());
		Set<IUID> uids = shareableObjectGroups.stream().flatMap(group -> group.getTargetSharedObjects().stream())
				.map(ISharedObject::getUID).collect(Collectors.toSet());
		refreshSharedTab(uids);
	}

	@Override @NotNull protected Set<IShareableObjectGroup> findShareableObjectGroups(
			@NotNull Set<IEntityShareCriteria> entitiesShareCriteria, @NotNull IProgress progress)
	{
		if (progress.isCancelled()) {
			return Collections.emptySet();
		}
		progress.increment(
				ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractFindAndShareCmd.ChildProgress.find"));

		Set<ShareableEntityTypeEnum> typesToBeConsidered = m_deltaAddedObjectsMap.keySet();
		if (typesToBeConsidered.isEmpty()) {
			return Set.of();
		}

		ShareableObjectsFinder shareableObjectsFinder = new ShareableObjectsFinder(m_project, m_designs,
				new HashSet<>(),
				getObjectInfoProvider(),
				getObjectNamesFilter());

		Set<IShareableObjectGroup> shareableObjectGroups = shareableObjectsFinder.collectShareableObjectGroups(
				entitiesShareCriteria, typesToBeConsidered);

		shareableObjectGroups.removeIf(group -> group.getTargetSharedObjects().isEmpty() &&
				group.getShareableObjectsNum() < 2);
		return shareableObjectGroups;
	}

	@NotNull private Predicate<IObjectInfo> getObjectNamesFilter()
	{
		Set<String> objectAddedByDeltaNames = m_deltaAddedObjectsMap.values().stream().flatMap(Set::stream).collect(
				Collectors.toSet());
		return o -> objectAddedByDeltaNames.contains(o.getAttributeValue(IXMLTags.NAME));
	}

	@NotNull
	public static Set<IEntityShareCriteria> getNameMatchingShareCriteria()
	{
		return Arrays.stream(ShareableEntityTypeEnum.values())
				.map(type -> new EntityShareCriteria(type, Set.of(IAttributeTypes.NAME), Set.of()))
				.collect(Collectors.toSet());
	}
	@NotNull @Override protected ISharedMulticoreMappingChecker getMulticoreChecker()
	{
		return new DeltaSharedMulticoreUpdater(m_project.getSharedConductorMgr());
	}
}