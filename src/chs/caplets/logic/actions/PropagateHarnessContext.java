/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.cof.DesignTypeUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.SystemLogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.DesignUtils;
import chs.common.ICommandHelper;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IUID;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.logic.autotagharness.DESIGN_LOCK_RESULT;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to keep track of design information
 */
public class PropagateHarnessContext
{

	private IProject m_project;
	private ILogicDesign m_currentDesign;

	private Set<ILogicDesign> m_designsInScope;
	private Set<ILogicDesign> m_lockedInAction;

	private Set<ILogicDesign> m_lockFailed;
	private Set<ILogicDesign> m_openedDesigns;
	private Map<ILogicDesign, Boolean> m_designEditabilityMap;
	private ICommandHelper m_cmdHelper;

	public PropagateHarnessContext(@NotNull ILogicDesign currentDesign, @NotNull ICommandHelper cmdHelper)
	{
		m_currentDesign = currentDesign;
		m_cmdHelper = cmdHelper;
		m_project = getCurrentProject();
		m_designsInScope = determineDesignsInScope();
		m_openedDesigns = getOpenedDesigns();
		m_lockedInAction = new HashSet<>();
		m_lockFailed = new HashSet<>();
		m_designEditabilityMap = new HashMap<>();
	}

	@NotNull private Set<ILogicDesign> getOpenedDesigns()
	{
		return m_designsInScope.stream().filter(o -> CAFUtils.getInstance().hasDiagramDisplayed(o.getUID()))
				.collect(Collectors.toSet());
	}

	@NotNull private IProject getCurrentProject()
	{
		IProject project = m_currentDesign.getProject();
		assert project != null;

		return project;
	}

	@NotNull public Set<ILogicDesign> getDesignsInScope()
	{
		return Collections.unmodifiableSet(m_designsInScope);
	}

	@NotNull private Set<ILogicDesign> determineDesignsInScope()
	{
		Set<IUID> designsToCheck = new HashSet<>();
		designsToCheck.add(m_currentDesign.getUID());

		IBuildList activeBuildList = m_project.getBuildListMgr().getActiveBuildList();
		if (activeBuildList == null || !activeBuildList.containsDesignUID(m_currentDesign.getUID())) {
			for (IDesignDescriptor design : getLogicalDesigns()) {
				designsToCheck.add(design.getUID());
			}
		}
		else {
			for (Iterator<IDesignContainer> it = activeBuildList.getDesignContainers(); it.hasNext(); ) {
				IDesignContainer designContainer = it.next();
				if (designContainer instanceof ILogicDesign) {
					ILogicDesign design = (ILogicDesign) designContainer;
					designsToCheck.add(design.getUID());
				}
			}
		}

		Set<IUID> accessibleDesigns =
				SharedObjectDomainAccessibliltyChecker.filterDesignContainersBasedOnAccessibility(designsToCheck);

		Set<ILogicDesign> designs = new HashSet<>();
		designs.addAll(accessibleDesigns.stream().map(o -> DesignUtils.getDesign(o, ILogicDesign.class))
				.filter(Objects::nonNull).collect(Collectors.toSet()));

		return designs;
	}

	@NotNull private List<IDesignDescriptor> getLogicalDesigns()
	{
		return DesignTypeUtils.getRelatedDesignDescriptors(m_currentDesign, m_project.getDesignMgr());
	}

	@NotNull public IProject getProject()
	{
		return m_project;
	}

	public boolean isDesignInScope(@NotNull ILogicDesign design)
	{
		return m_designsInScope.contains(design);
	}

	public boolean isDesignSafe(@NotNull ILogicDesign design)
	{
		return !m_lockFailed.contains(design);
	}

	@NotNull public DESIGN_LOCK_RESULT lockDesign(@NotNull ILogicDesign design)
	{
		DESIGN_LOCK_RESULT result;
		if (design.isLocked()) {
			result = m_lockedInAction.contains(design) ? DESIGN_LOCK_RESULT.LOCK_ATTAINED :
					DESIGN_LOCK_RESULT.ALREADY_LOCKED;
		}
		else if (design instanceof SystemLogicDesign && design.isUnderConcurrentEdit()) {
			result = DESIGN_LOCK_RESULT.ALREADY_WEAK_LOCKED;
		}
		else {
			boolean lockAttained = false;
			try {
				lockAttained = m_cmdHelper.lockAndRefresh(design);
			}
			finally {
				result = lockAttained ? DESIGN_LOCK_RESULT.LOCK_ATTAINED : DESIGN_LOCK_RESULT.LOCK_FAILED;
			}
		}
		if (result == DESIGN_LOCK_RESULT.LOCK_ATTAINED) {
			m_lockedInAction.add(design);
		}
		if (result == DESIGN_LOCK_RESULT.LOCK_FAILED) {
			m_lockFailed.add(design);
		}
		return result;
	}

	public boolean isDesignEditable(@NotNull ILogicDesign design)
	{
		return m_designEditabilityMap.computeIfAbsent(design, o -> o.isEditable());
	}

	public boolean hasDiagramOpen(@NotNull ILogicDesign design)
	{
		return m_openedDesigns.contains(design);
	}

	public void unlockDesigns()
	{
		for (ILogicDesign design : m_lockedInAction) {
			if (design.isLocked()) {
				m_cmdHelper.unlock(design);
			}
		}
	}

	public void unlockDesign(@NotNull ILogicDesign design)
	{
		if (m_lockedInAction.contains(design) && design.isLocked()) {
			m_cmdHelper.unlock(design);
		}
	}
}
