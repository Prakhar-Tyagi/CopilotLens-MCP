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
import chs.cof.logical.shared.ISharedConductorMgr;
import chs.cof.logical.shared.ISharedPinListMgr;
import chs.cof.project.IProject;
import chs.common.ILockable;
import chs.common.IUID;
import chs.utilities.OptionalString;
import chs.utilities.ResourceMgr;
import chs.utility.ui.progress.IProgress;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Cmd for share/shareInto objects added by delta.
 */
public abstract class AbstractDeltaShareCmd extends AbstractFindAndShareCmd implements IFindAndShareCmd
{
	@NotNull private Collection<ShareableEntityTypeEnum> m_newlyAddedObjectTypes;
	@NotNull private IProgress m_progress;
	@NotNull private IProgress m_findProgress;
	@NotNull private IProgress m_shareProgress;
	@NotNull private IProgress m_shareIntoProgress;
	protected AbstractDeltaShareCmd(@NotNull IProject project, @NotNull Set<ILogicDesign> designs,
			@NotNull Collection<ShareableEntityTypeEnum> newlyAddedObjectTypes)
	{
		super(project, designs);
		m_newlyAddedObjectTypes = newlyAddedObjectTypes;
	}

	@Override protected boolean isExecutionAllowed()
	{
		EnumSet<ShareableEntityTypeEnum> pinListShareableEntityTypes = EnumSet.of(
				ShareableEntityTypeEnum.DEVICE,
				ShareableEntityTypeEnum.GROUND,
				ShareableEntityTypeEnum.JACK,
				ShareableEntityTypeEnum.PLUG,
				ShareableEntityTypeEnum.SPLICE,
				ShareableEntityTypeEnum.RING_TERMINAL,
				ShareableEntityTypeEnum.INLINE);

		boolean status = true;
		boolean isSharedPinListMgrLockReq = m_newlyAddedObjectTypes.stream()
				.anyMatch(logicObjType -> pinListShareableEntityTypes.contains(logicObjType));
		boolean isSharedCondMgrLockReq = m_newlyAddedObjectTypes.stream()
				.anyMatch(logicObjType -> !pinListShareableEntityTypes.contains(logicObjType));
		ISharedPinListMgr sharedPinListMgr = m_project.getSharedPinListMgr();
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		if (isSharedPinListMgrLockReq && !sharedPinListMgr.isLocked()) {
			status = sharedPinListMgr.lock();
			if (status) {
				m_lockedObjects.add(sharedPinListMgr);
			}
		}
		if (!status) {
			return false;
		}
		if (isSharedCondMgrLockReq && !sharedConductorMgr.isLocked()) {
			status = sharedConductorMgr.lock();
			if (status) {
				m_lockedObjects.add(sharedConductorMgr);
			}
		}
		return status;
	}

	@NotNull @Override protected ISharedMulticoreMappingChecker getMulticoreChecker()
	{
		return new DeltaSharedMulticoreMappingChecker();
	}

	@Override protected void displayBatchShareStatusTab(@NotNull Collection<IBatchShareStatusMessage> reportedMessages)
	{
		//Todo: the tab sometimes shows misleading messages. need to be refined.
//		BatchShareStatusWindowAssistant statusWindow = new BatchShareStatusWindowAssistant(
//				ResourceMgr.getString(AbstractFindAndShareCmd.class, "AbstractDeltaShareCmd.StatusTab.title"),
//				BatchShareFeedbackTableColumnEnum.SEVERITY.toString());
//		statusWindow.addStatusMessages(reportedMessages);
	}

	@Override protected void releaseAcquiredLocks()
	{
		for (ILockable lockable : Objects.requireNonNull(m_lockedObjects)) {
			lockable.unlock();
		}
	}

	protected void refreshSharedTab(@NotNull Set<IUID> sharedObjectsUIDs)
	{
		if (sharedObjectsUIDs.isEmpty()) {
			return;
		}
		ISharedConductorMgr sharedConductorMgr = m_project.getSharedConductorMgr();
		sharedConductorMgr.fireChangeEvent(sharedObjectsUIDs);
	}

	@Override protected boolean doShareUnplacedObjects()
	{
		return true;
	}

	@Override
	@NotNull public OptionalString getProgressTitle()
	{
		return OptionalString.of(ResourceMgr.getString(FindAndShareCmd.class, "AbstractDeltaShareCmd.Progress.title"));
	}

	@Override
	@NotNull public OptionalString getProgressHeader()
	{
		return OptionalString.of(ResourceMgr.getString(FindAndShareCmd.class, "AbstractDeltaShareCmd.Progress.header"));
	}

	@Override public boolean isStoppable()
	{
		return true;
	}

	@Override public boolean supportsChildProgressBars()
	{
		return true;
	}

	@NotNull public IProgress getProgress()
	{
		return m_progress;
	}

	@Override public void run()
	{
		execute();
	}

	public void setProgress(@NotNull IProgress m_progress)
	{
		this.m_progress = m_progress;
	}

	@NotNull public IProgress getFindProgress()
	{
		return m_findProgress;
	}

	public void setFindProgress(@NotNull IProgress m_findProgress)
	{
		this.m_findProgress = m_findProgress;
	}

	@NotNull public IProgress getShareProgress()
	{
		return m_shareProgress;
	}

	public void setShareProgress(@NotNull IProgress m_shareProgress)
	{
		this.m_shareProgress = m_shareProgress;
	}

	@NotNull public IProgress getShareIntoProgress()
	{
		return m_shareIntoProgress;
	}

	public void setShareIntoProgress(@NotNull IProgress m_shareIntoProgress)
	{
		this.m_shareIntoProgress = m_shareIntoProgress;
	}
}