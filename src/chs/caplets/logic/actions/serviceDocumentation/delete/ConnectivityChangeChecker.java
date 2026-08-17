package chs.caplets.logic.actions.serviceDocumentation.delete;

import chs.api.servicedoc.offPage.IUsagesProvider;
import chs.api.servicedoc.offPage.traceResult.ObjectToGetDiagramReps;
import chs.api.servicedoc.offPage.usages.DesignWideUsages;
import chs.api.servicedoc.offPage.usages.IDiagramRepresentation;
import chs.api.servicedoc.offPage.usages.UsagesAcrossProject;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.common.IPrivilegedDesignMgr;
import chs.servicedoc.schemConnectivity.DiagramObjectConnectivitySufficiencyChecker;
import chs.utility.logic.LogicUtils;
import chs.utility.schemConnectivity.IDiagramObjectInSignalHelper;
import chs.utility.schemConnectivity.IDiagramObjectSufficiencyChecker;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks if the connectivity will change if the selected instances are deleted.
 */
class ConnectivityChangeChecker
{

	@NotNull private ILogicObject m_connectivity;
	@NotNull private Set<IDiagramObject> m_selectedInstances;
	@NotNull private Collection<IDesignContainer> m_designScope;

	ConnectivityChangeChecker(@NotNull ILogicObject connectivity, @NotNull Set<IDiagramObject> instances, @NotNull
	Collection<IDesignContainer> designScope)
	{
		m_connectivity = connectivity;
		m_selectedInstances = instances;
		m_designScope = designScope;
	}

	boolean containsAllInstances()
	{
		int designWideInstancesSize = designWideInstancesSize(m_connectivity);
		int instancesInSelection = m_selectedInstances.size();
		if (instancesInSelection < designWideInstancesSize) {
			return false;
		}
		if (instancesInSelection == designWideInstancesSize) {
			int projectWideInstancesSize = projectWideInstancesSize(m_connectivity);
			return projectWideInstancesSize == 0 || projectWideInstancesSize == 1;
		}
		return false;
	}

	boolean willConnectivityChangeWithDelete()
	{
		IDiagramObjectSufficiencyChecker checker =
				new DiagramObjectConnectivitySufficiencyChecker((p) -> true, (p) -> true);
		IDiagramObjectInSignalHelper helper = (obj, coBj) -> true;
		IUsagesProvider designUsages =
				new DesignWideUsages(null, helper, (a, b) -> true);
		IUsagesProvider projectUsages =
				new UsagesAcrossProject(m_designScope, null, helper, (a, b) -> true);
		Set<IDiagramRepresentation> allUsages = new HashSet<>();
		List<IDiagramRepresentation> usages =
				designUsages.getUsages(new ObjectToGetDiagramReps(m_connectivity)).getRepresentations();
		List<IDiagramRepresentation> usages1 =
				projectUsages.getUsages(new ObjectToGetDiagramReps(m_connectivity)).getRepresentations();
		allUsages.addAll(usages);
		allUsages.addAll(usages1);
		Set<IDiagramObject> otherInstances = allUsages
				.stream()
				.map(usage -> usage.getDiagramObject())
				.filter(obj -> {
					return !m_selectedInstances.contains(obj);
				})
				.collect(Collectors.toSet());
		boolean isDeletable = true;
		for (IDiagramObject obj : m_selectedInstances) {
			isDeletable = isDeletable && checker.areAvailableInstancesSufficient(obj, otherInstances);
		}
		return !isDeletable;
	}

	private int designWideInstancesSize(ILogicObject connectivity)
	{
		ILogicDesign logicDesign = connectivity.getLogicDesign();
		if (logicDesign == null) {
			return 0;
		}
		IDesignWideUsageMgr designWideUsageMgr = logicDesign.getDesignWideUsageMgr();
		List<IDesignSharedUsage> usages = designWideUsageMgr.getUsages(connectivity);
		return usages.size();
	}

	private int projectWideInstancesSize(ILogicObject connectivity)
	{
		ISharedObject sharedObject = connectivity.getSharedObject();
		if (sharedObject == null) {
			return 0;
		}
		IProject project = connectivity.getProject();
		Collection<? extends ISharedUsage> sharedUsages = LogicUtils.getSharedUsages(project, sharedObject);
		return sharedUsages.size();
	}
}
