/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic;

import chs.caf.cafmain.actions.ProjectGenerateCAVALAction;
import chs.caf.cafmain.actions.TCSearchableObjects;
import chs.caf.cafmain.actions.ela.publisher.ui.ELAReportLayoutAction;
import chs.caf.cafmain.actions.servicedoc.GenerateServiceDocumentationAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.caplet.helpers.IConductorRouteAction;
import chs.caf.caplet.helpers.LifecycleActionTypeHolder;
import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.caplets.logic.actions.ObjectConnectionsGetter;
import chs.caplets.logic.actions.query.BuildListDesignGenerateAction;
import chs.caplets.logic.actions.query.QueriedDesignGenerateAction;
import chs.caplets.shared.BaseLifecycle;
import chs.caplets.shared.CreateFilteredDiagramDelegate;
import chs.caplets.shared.CreateNewDelegate;
import chs.caplets.shared.CreateNewLogicDesignDelegate;
import chs.caplets.shared.CreateNewLogicDiagramDelegate;
import chs.caplets.shared.CreateNewPartitionLogicDiagramDelegate;
import chs.caplets.shared.OpenDiagramDelegate;
import chs.caplets.shared.WindowCloseStatus;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ISystemLogicDesign;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISystemLogicDiagram;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.ILogicBuildList;
import chs.cof.project.folder.IDesignFolder;
import chs.common.IProjectPreferenceMgr;
import chs.common.PreferenceContext;
import chs.utilities.AppInfo;
import chs.utilities.IXMLTags;
import chs.utilities.LifecycleUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.suite.DesignType;
import chs.utility.helpers.LogicDesignAssociationChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public abstract class BaseSystemLogicLifecycle extends BaseLifecycle
{

	protected BaseSystemLogicLifecycle(ICaplet caplet)
	{
		super(caplet);

		addTypeForNew(getPartitionedTypeHolder(getLogicDiagramClass(), "Lifecycle.NewPartitionedDiagram."));
		//For CAVAL
		addTypesForFilter(getLifecycleType(ISystemLogicDesign.class, "Lifecycle.FilteredDiagram."));

		//Generate dias for project
		addProjectGenerateCavalAction(caplet);
		addTypesForFilter(new LifecycleActionTypeHolder(IProject.class, new QueriedDesignGenerateAction(caplet)));

		addTypesForFilter(
				new LifecycleActionTypeHolder(ILogicBuildList.class, new BuildListDesignGenerateAction(caplet)));

		addTypesForFilter(
				new LifecycleActionTypeHolder(ILogicBuildList.class, new GenerateServiceDocumentationAction(caplet)));
		addElaActions(caplet);
		addTypesForGeneralLifecycleActivities();

		// View Related Blocks Types
		ILifecycleType viewRelatedBlocksType = getLifecycleType(IDesignFolder.class, "Lifecycle.ViewRelatedBlocks.");
		addTypeForViewRelatedBlocks(viewRelatedBlocksType);
		viewRelatedBlocksType = getLifecycleType(ISystemLogicDesign.class, "Lifecycle.ViewRelatedBlocks.");
		addTypeForViewRelatedBlocks(viewRelatedBlocksType);

		// Design Multiuser lock types
		ILifecycleType lockType = getLifecycleType(ISystemLogicDesign.class, "Lifecycle.AcquireExclusiveLock.");
		addTypesForAcquireExclusiveLock(lockType);
		lockType = getLifecycleType(ISystemLogicDesign.class, "Lifecycle.ReleaseExclusiveLock.");
		addTypesForReleaseExclusiveLock(lockType);

		String publishActionName = "Lifecycle.PublishDesign.";
		String extendPublishActionName = "Lifecycle.ExtendedPublishDesign.";
		if(AppInfo.isSvcDoc()){
			publishActionName = "Lifecycle.PublishDesigns.";
			extendPublishActionName = "Lifecycle.ExtendedPublishDesigns.";
		}

		//Publish design to TC types
		ILifecycleType publishDesignType =
				getLifecycleType(ISystemLogicDesign.class, publishActionName);
		addTypeForPublish(publishDesignType);
		List<LifecycleTypeHolder> searchTypes = new TCSearchableObjects().getLifecycleTypesHolderForSearch();
		searchTypes.forEach(this::addTypeForSearch);
		//Publish design to TC types
		ILifecycleType extpublishDesignType =
				getLifecycleType(ISystemLogicDesign.class, extendPublishActionName);
		addTypeForExtendedPublish(extpublishDesignType);

		//Publish design to TC Share types
		ILifecycleType publishTCShareDesignType =
				getLifecycleType(ISystemLogicDesign.class, "Lifecycle.PublishDesignToTCShare.");
		addTypeForPublishTCShare(publishTCShareDesignType);
	}

	protected void addProjectGenerateCavalAction(ICaplet caplet)
	{
		addTypesForFilter(new LifecycleActionTypeHolder(IProject.class, new ProjectGenerateCAVALAction(caplet)));
	}

	protected void addElaActions(@NotNull ICaplet caplet)
	{
		addTypesForFilter(new LifecycleActionTypeHolder(ILogicBuildList.class, new ELAReportLayoutAction(caplet)));
	}

	protected Class<? extends ISchemDiagram> getLogicDiagramClass()
	{
		return ISystemLogicDiagram.class;
	}

	protected Class<? extends ILogicDesign> getLogicDesignClass()
	{
		return ISystemLogicDesign.class;
	}

	@NotNull @Override protected OpenDiagramDelegate getOpenDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		final OpenDiagramDelegate openDiagramDelegate =
				new OpenDiagramDelegate(this, m_caplet, getResourceClass(), getLogicaldesignXMLTag(),
						preferences.getUpdateXrefOnReadOnly(preferenceContext),
						preferences.getDrawGridSpacing(preferenceContext));
		openDiagramDelegate.setStatusReporter(getStatusReporter());
		openDiagramDelegate.setCustomLogicSupport(getCustomLogicSupport());

		return openDiagramDelegate;
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDiagramDelegate(IProjectPreferenceMgr preferences,
			List<?> context)
	{
		final Class<? extends ISchemDiagram> logicDiagramClass = getLogicDiagramClass();
		ISchemDiagram diagramContext = LifecycleUtils.getContextObject(context, logicDiagramClass);
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		if (diagramContext == null) {
			return new CreateNewLogicDiagramDelegate(this, getResourceClass(), m_caplet, getLogicaldesignXMLTag(),
					preferences.getUpdateXrefOnReadOnly(preferenceContext),
					preferences.getDrawGridSpacing(preferenceContext));
		}
		else {
			return new CreateNewPartitionLogicDiagramDelegate(this, getResourceClass(), m_caplet,
					getLogicaldesignXMLTag(), preferences.getUpdateXrefOnReadOnly(preferenceContext),
					preferences.getDrawGridSpacing(preferenceContext),
					logicDiagramClass);
		}
	}

	@NotNull protected String getLogicaldesignXMLTag()
	{
		return IXMLTags.LOGICALDESIGN;
	}

	@NotNull @Override protected CreateNewDelegate getCreateNewDesignDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new CreateNewLogicDesignDelegate(this, getResourceClass(), m_caplet, getLogicaldesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	@NotNull @Override protected CreateNewDelegate getCreateFilteredDiagramDelegate(IProjectPreferenceMgr preferences)
	{
		final PreferenceContext preferenceContext = getProjectPreferenceContext();
		return new CreateFilteredDiagramDelegate(this, getResourceClass(), m_caplet, getLogicaldesignXMLTag(),
				preferences.getUpdateXrefOnReadOnly(preferenceContext),
				preferences.getDrawGridSpacing(preferenceContext));
	}

	@Override @NotNull protected DesignType getDesignType()
	{
		return DesignType.LOGICAL;
	}

	@NotNull private PreferenceContext getProjectPreferenceContext()
	{
		return PreferenceContext.LOGIC;
	}

	protected void addTypesForGeneralLifecycleActivities()
	{
		super.addTypesForGeneralLifecycleActivities();

		if (AppInfo.isCapitalLogic() || AppInfo.isCapitalArchitect() || AppInfo.isCapitalDesign()) {
			ILifecycleType sysyemBlcokDesignCopyType =
					getLifecycleType(getLogicDesignClass(), "Lifecycle.SystemBlockDesignCopy.");
			addLifeCycleCreateDetailedDesign(sysyemBlcokDesignCopyType);
		}
	}

	private ILifecycleType getPartitionedTypeHolder(Class<?> actionClass, String rsrcPrefix)
	{
		String menuText = ResourceMgr.getString(getResourceClass(), rsrcPrefix + "text");
		Icon icon = getDiagramIcon();
		return new CavaiLifecycleTypeHolder(actionClass, menuText, icon);
	}

	@Nullable public ICapletModel getModel(ISchemDiagram diagram)
	{
		return getModel(diagram.getDesign());
	}

	public void createConnectionSchematics(IPinList schemPinlist, ISchemDiagram destDiagram)
	{
		ObjectConnectionsGetter.createConnectionSchematics(schemPinlist, destDiagram);
	}

	public IConductorRouteAction getConductorRouteAction()
	{
		return ConductorRouteAction.getInstance();
	}

	/**
	 * PDVC-915 - ST171BashXPDV7: Concurrency refresh after view functional source action on deleted Logic Net is
	 * throwing Validation Exception (VALIDATION FAILURE: SlotDevicePin has Functional Source Pin with no Conductor -
	 * DEV1:DEV1:PWR)
	 * <p>
	 * Specialized version that unloads diagrams instead of all of the children if this design is associated with an
	 * Integrator design that is open in multi user mode. Reason is that we cannot do functional sync on diagrams in
	 * multi user mode, which means that once we get associated connectivity in-memory for an MU Integrator design we
	 * must preserve it exactly as is, no refresh, no unloading and reloading.
	 */
	protected class LogicWipeOutDesignOnSaveCompleteTask extends WipeOutDesignOnSaveCompleteTask
	{

		protected LogicWipeOutDesignOnSaveCompleteTask(@NotNull IProject project, @NotNull ILogicDesign design,
				boolean unlockDesign, @NotNull WindowCloseStatus windowCloseStatus)
		{
			super(project, design, unlockDesign, windowCloseStatus);
		}

		/**
		 * @see WipeOutDesignOnSaveCompleteTask#unloadDesignChildren(ILogicDesign)
		 */
		@Override protected void unloadDesignChildren(@NotNull ILogicDesign theDesign)
		{
			if (new LogicDesignAssociationChecker(theDesign).execute()) {
				// PDVC-915 - ST171BashXPDV7: Concurrency refresh after view functional source action on deleted Logic
				theDesign.unloadDiagramsFully();
				return;
			}
			super.unloadDesignChildren(theDesign);
		}
	}
}
