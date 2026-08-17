/*
* Copyright 2016 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.IStatusDelegator;
import chs.caf.caplet.helpers.IStatusHandler;
import chs.caf.caplet.helpers.browser.LockedLogicObjectNodeDimmer;
import chs.capitalmanager.appserver.ILockInfo;
import chs.caplets.logic.LockobjectsDisplay.LockObjectDisplayTask;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.logical.concurrency.IConcurrentEditReporter;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.cofUtils.logical.concurrency.LogicConcurrentEditReporter;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUID;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LockHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author pbhawsar on 03-04-2016
 */
public class LogicDesignLockConverter implements IStatusDelegator
{

	private static LogicDesignLockConverter m_instance = null;

	private IConcurrentEditReporter reporter = new LogicConcurrentEditReporter();
	private static final String ACQ_EXCLUSIVE_LOCK_FAIL = ResourceMgr.getString(LogicDesignLockConverter.class,
			"LogicDesignLockConverter.AcquireExclusiveLock.failue.msg");
	private static final String ACQ_EXCLUSIVE_LOCK_SUCCESS = ResourceMgr.getString(LogicDesignLockConverter.class,
			"LogicDesignLockConverter.AcquireExclusiveLock.success.msg");
	private static final String REL_EXCLUSIVE_LOCK_FAIL = ResourceMgr
			.getString(LogicDesignLockConverter.class, "LogicDesignLockConverter.ReleaseExclusiveLock.failue.msg");
	private static final String REL_EXCLUSIVE_LOCK_SUCCESS = ResourceMgr
			.getString(LogicDesignLockConverter.class, "LogicDesignLockConverter.ReleaseExclusiveLock.success.msg");
	private IStatusHandler m_messageHandler;

	private LogicDesignLockConverter()
	{
	}

	public static synchronized LogicDesignLockConverter getInstance()
	{
		if (m_instance == null) {
			m_instance = new LogicDesignLockConverter();
		}
		return m_instance;
	}

	public boolean convertWeakLockToFullLock(@NotNull ILogicDesign design)
	{

		if (!isDesignValidToSwitchLock(design,
				(d) -> ((IPrivilegedCOGManagedLockableChildrenContainer) d).isWeakLocked())) {
			return false;
		}
		Set<ISchemDiagram> alreadyEditableDiagrams = new HashSet<>();
		for (ISchemDiagram diagram : design.getDiagrams()) {
			if (diagram.isEditable()) {
				alreadyEditableDiagrams.add(diagram);
			}
		}

		boolean status = ((IPrivilegedCOGManagedLockableChildrenContainer) design).releaseWeakLock();
		if (!status) {
			logMessage(ACQ_EXCLUSIVE_LOCK_FAIL, design.getName());
			return false;
		}

		boolean locked = design.lock();
		if (locked) {
			logMessage(ACQ_EXCLUSIVE_LOCK_SUCCESS, design.getName());
			LockObjectDisplayTask.getInstance().removeDesign(design);
			return true;
		}

		design.lockDiagrams(alreadyEditableDiagrams);
		for (ISchemDiagram openDiagram : alreadyEditableDiagrams) {
			if (!openDiagram.isEditable()) {
				switchDesignToReadOnly(openDiagram);
			}
		}
		showSingleUserModeFailureDialog("LogicDesignLockConverter.designLock.lockfailed", design);
		return false;
	}

	public boolean convertFullLockToWeakLock(@NotNull ILogicDesign design)
	{
		if (!isDesignValidToSwitchLock(design, (d) -> d.isLocked())) {
			return false;
		}
		boolean status = design.unlock();
		if (!status) {
			logMessage(REL_EXCLUSIVE_LOCK_FAIL, design.getName());
			return false;
		}
		LockObjectDisplayTask.getInstance().addDesign(LockedLogicObjectNodeDimmer.getInstance(), design);
		LockObjectDisplayTask.getInstance().createService(design);

		List<ISchemDiagram> openDiagrams = CollectionUtils.getListOfType(CAFUtils.getInstance().getOpenDiagrams(design),
				ISchemDiagram.class);
		design.lockDiagrams(openDiagrams);

		Collection<ISchemDiagram> failedDiagrams = new HashSet<>();
		status = true;
		for (ISchemDiagram openDiagram : openDiagrams) {
			if (!openDiagram.isEditable()) {
				switchDesignToReadOnly(openDiagram);
				failedDiagrams.add(openDiagram);
				status = false;
			}
		}
		if (status) {
			status = LogicConcurrencyHelper.resetDesignLevelHighWaterMarks(design);
		}
		if (!status) {
			showMultiUserModeFailureDialog("LogicDesignLockConverter.diagramLock.lockfailed", design, failedDiagrams);
		}
		else {
			logMessage(REL_EXCLUSIVE_LOCK_SUCCESS, design.getName());
		}
		return status;
	}

	private boolean isDesignValidToSwitchLock(ILogicDesign design, Predicate<ILogicDesign> lockStateChecker)
	{
		Collection<ICapletModel> modelsOfDesign = CAFUtils.getInstance().getModelsOfDesign(design);
		if (modelsOfDesign.isEmpty()) {
			return false;
		}
		for (ICapletModel model : modelsOfDesign) {
			if (model.isStateValidToPersist()) {
				return false;
			}
		}
		if (design instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
			if (lockStateChecker.test(design)) {
				return true;
			}
		}
		return false;
	}

	private void logMessage(String message, String objName)
	{
		reporter.report(message + " - " + objName);
	}

	private void showSingleUserModeFailureDialog(String resourceKeyRoot, @NotNull ILogicDesign design)
	{
		List<ILockInfo> lockInfos = LockHelper.getWeakLockableLockInfo(design);
		String designLockUsers = LockHelper.getLockUserNames(lockInfos, true);

		getStatusReporter().showErrorMessage(BaseLifecycleDelegate.class, resourceKeyRoot,
				CollectionUtils.createArray(designLockUsers, design.getName()), null);
	}

	private void showMultiUserModeFailureDialog(String resourceKeyRoot, @NotNull ILogicDesign design,
			@NotNull Collection<ISchemDiagram> failedDiagrams)
	{
		Map<String, String> diagramLockedByUser = collectDiagramLockDetails(design, failedDiagrams);
		String implicationExt = diagramLockedByUserMessage(diagramLockedByUser);

		getStatusReporter().showWarningMessage(BaseLifecycleDelegate.class, resourceKeyRoot,
				CollectionUtils.createArray(implicationExt), null);
	}

	@NotNull private String diagramLockedByUserMessage(@NotNull Map<String, String> diagramLockedByUser)
	{
		StringBuilder messageBldr = new StringBuilder();
		for (Map.Entry<String, String> entry : diagramLockedByUser.entrySet()) {
			messageBldr.append("\n").append(ResourceMgr.getString(LogicDesignLockConverter.class,
					"LogicDesignLockConverter.beingEdited.msg", entry.getKey(), entry.getValue()));
		}
		return messageBldr.toString();
	}

	@NotNull private Map<String, String> collectDiagramLockDetails(@NotNull ILogicDesign design,
			@NotNull Collection<ISchemDiagram> failedDiagrams)
	{
		Map<String, String> diagramLockedByUser = new HashMap<>();

		List<String> uidsToGetLockInfo = collectDiagramUIDs(failedDiagrams);
		uidsToGetLockInfo.add(design.getUID().getString());

		ILockInfo[] lockInfos = LockHelper.getLockInfos(uidsToGetLockInfo);
		if (lockInfos == null) {
			return diagramLockedByUser;
		}

		Map<IUID, ILockInfo> uidLockInfoMap = LockHelper.getUIDToLockInfoMap(lockInfos);

		boolean isDesignFullLocked = false;

		ILockInfo designLockInfo = uidLockInfoMap.get(design.getUID());
		if (designLockInfo != null && designLockInfo.getLockStatus() == ILockInfo.LockStatus.OTHER_USER_LOCKED) {
			isDesignFullLocked = true;
		}

		if (isDesignFullLocked) {
			failedDiagrams.forEach(d -> diagramLockedByUser.put(d.getName(), designLockInfo.getUserName()));
			return diagramLockedByUser;
		}

		for (ISchemDiagram failedDiagram : failedDiagrams) {
			IUID uid = failedDiagram.getUID();
			if (uidLockInfoMap.containsKey(uid)) {
				diagramLockedByUser.put(failedDiagram.getName(), uidLockInfoMap.get(uid).getUserName());
			}
		}

		return diagramLockedByUser;
	}

	private List<String> collectDiagramUIDs(@NotNull Collection<ISchemDiagram> failedDiagrams)
	{
		return failedDiagrams.stream().map((d) -> d.getUID().getString()).collect(Collectors.toList());
	}

	private void switchDesignToReadOnly(IBaseDiagram diagram)
	{
		CAFUtils.getInstance().markWindowsReadOnly(diagram);
	}

	@Override public IStatusHandler setStatusReporter(@NotNull IStatusHandler statusHandler)
	{
		IStatusHandler oldMessageHandler = statusHandler;
		m_messageHandler = statusHandler;
		return oldMessageHandler;
	}

	@Override public IStatusHandler getStatusReporter()
	{
		return m_messageHandler;
	}
}
