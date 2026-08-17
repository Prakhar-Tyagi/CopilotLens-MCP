/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025-2026 Siemens
 */
package chs.caplets.shared;

import chs.IIECNamingObjectContainer;
import chs.analysis.AnalysisServices;
import chs.analysis.CapitalAnalysisFactory;
import chs.analysis.IAnalysisNetlistScope;
import chs.ans.DesignDescriptorUtils;
import chs.bridges.BridgesIntegrationServices;
import chs.caf.CAFUtils;
import chs.caf.CapletIterator;
import chs.caf.ICAFProjectMgr;
import chs.caf.ICAFWindow;
import chs.caf.IDesignSaveTeamPlayReferenceSync;
import chs.caf.IProjectChangeListener;
import chs.caf.IWindowMgr;
import chs.caf.LifeCycleCacheUtils;
import chs.caf.ProjectChangeEvent;
import chs.caf.caplet.CapletViewIterator;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDataTransfer;
import chs.caf.caplet.ILifecycleType;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caf.caplet.helpers.CapletModelHelper;
import chs.caf.caplet.helpers.CloseDiagramSaveQuestionHelper;
import chs.caf.caplet.helpers.DRCRunnerHelper;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.FileTypeHolder;
import chs.caf.caplet.helpers.LifecycleTypeHolder;
import chs.caf.caplet.helpers.OpenDiagramCustomLogicRegistry;
import chs.caf.caplet.helpers.RefreshableLogicDesignFilter;
import chs.caf.helpers.LinkSaveHelper;
import chs.caf.helpers.autorecovery.ControllerSaveTaskAutorecoveryListener;
import chs.caf.utility.tc.AutomatedPublishToTeamcenter;
import chs.capitalmanager.appserver.IDesignManipulationPackage.DesignCopyTeamPlayData;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.IDesignLockStrategy;
import chs.caplets.base.BaseModelMapData;
import chs.caplets.logic.CrossReferenceMonitor;
import chs.caplets.logic.ILogicLifecycle;
import chs.caplets.logic.LogicFilterControlMgr;
import chs.caplets.logic.Model;
import chs.caplets.logic.View;
import chs.caplets.logic.actions.CrossLinkActionUI;
import chs.caplets.logic.actions.CrossLinkHelper;
import chs.caplets.logic.actions.DesignBlockHyperlinkProducer;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.caplets.logic.analysis.ui.AnalysisBrowserPanel;
import chs.cof.draw.Grid;
import chs.cof.draw.IColor;
import chs.cof.draw.IGrid;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.helpers.DefaultDesignUpgradeContext;
import chs.cof.helpers.DesignRevisionHelper;
import chs.cof.links.ILinkDelta;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ILogicDesignRefreshListener;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.shared.RefreshHelper;
import chs.cof.project.IOption;
import chs.cof.project.IOptionExpression;
import chs.cof.project.IOptionIterator;
import chs.cof.project.IProject;
import chs.cof.project.IProjectAnalysisMgr;
import chs.cof.project.buildlist.BlockAssociationDesignProvider;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.effectivity.EffectivityModelUpdater;
import chs.cof.project.folder.FolderMgrEditException;
import chs.cof.project.folder.IDesignFolder;
import chs.cof.project.folder.IFolder;
import chs.cof.project.folder.IFolderMgr;
import chs.cof.project.folder.IFolderMgrNode;
import chs.cof.project.folder.IFolderMgrNodeIterator;
import chs.cof.project.folder.INormalFolder;
import chs.cof.project.folder.RevisionNode;
import chs.cof.security.FunctionalPermissionEnum;
import chs.cofUtils.DiagramUtils;
import chs.cofUtils.importer.DesignImporter;
import chs.cofUtils.logical.concurrency.LogicConcurrencyController;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.cofUtils.scrubber.CommonScrubbableChecker;
import chs.cofUtils.scrubber.ILogicInvalidStateHandler;
import chs.cofUtils.scrubber.ScrubberFactory;
import chs.cog.ICOGLockable;
import chs.cog.ICOGManagedLockable;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.cog.PersistenceLockFailureCheckedException;
import chs.common.DesignUtils;
import chs.common.ICHSIterator;
import chs.common.IDesignContainer;
import chs.common.IDesignDescriptor;
import chs.common.IDesignMgr;
import chs.common.IDesignMgrAccessibilityChangeEvent;
import chs.common.IDesignMgrAccessibilityChangeListener;
import chs.common.IGuard;
import chs.common.IIECNamingObject;
import chs.common.IIncLoadable;
import chs.common.IPreferenceChangeEvent;
import chs.common.IPreferenceChangeListener;
import chs.common.IProjectPreferenceMgr;
import chs.common.ISystemPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUpgradeableDesignContainer;
import chs.common.PreferenceContext;
import chs.common.RefreshStatusEnum;
import chs.common.UIDUtils;
import chs.common.attr.IAttributeTypes;
import chs.common.styles.IStyleableDiagram;
import chs.common.validation.ValidationException;
import chs.common.validation.ValidationHelper;
import chs.ctf.caf.interfaces.IAdditionalDesignUIPropsContext;
import chs.ctf.caf.ui.DesignEditDialog;
import chs.ctf.caf.ui.DesignInfoDialog;
import chs.ctf.caf.ui.IDesignCopyInfo;
import chs.ctf.caf.ui.NoReleaseLevelsException;
import chs.ctf.caf.ui.PropertyEditor;
import chs.ctf.caf.ui.TCLogicDesignEditDialog;
import chs.ctf.caf.utils.ApplicableOptionsHelper;
import chs.ctf.caf.utils.IReleaseLevelController;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.ctf.deletedesign.DeleteDesignHelper;
import chs.ctf.drc.DRCResultCollector;
import chs.ctf.drc.DRCResultList;
import chs.ctf.drc.IDRCMgr;
import chs.ctf.drc.IDRCResultCollector;
import chs.ctf.drc.IDRCResultList;
import chs.ctf.drc.IDRCViolation;
import chs.ctf.ui.copydesign.common.CopyDesignHelper;
import chs.ctf.ui.effectivity.EffectivityHelper;
import chs.ctf.ui.engineeringchangeorder.drc.designtools.DRCIssueController;
import chs.ctf.ui.engineeringchangeorder.drc.designtools.DRCIssueFailureMsgHandler;
import chs.ctf.ui.engineeringchangeorder.drc.designtools.DRCIssueProcessingParams;
import chs.dataservices.DesignBlockUsageInfo;
import chs.images.CHSImageLoader;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.ICHSSystem;
import chs.system.UIDMgr;
import chs.utilities.AppInfo;
import chs.utilities.CapabilityHelper;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.DomainInaccessibleSharedObjectDesignHandledRuntimeException;
import chs.utilities.Environment;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.IXMLTags;
import chs.utilities.LifecycleUtils;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.SupportedFeatureInfo;
import chs.utilities.WrappingRuntimeException;
import chs.utilities.exception.ExceptionHandlerCreationFactory;
import chs.utilities.exception.IExceptionHandler;
import chs.utilities.suite.DesignType;
import chs.utilities.ui.messaging.ICloseAllContext;
import chs.utilities.ui.property.IBooleanProperty;
import chs.utility.AuditTrailLogHelper;
import chs.utility.DesignAuditTrailInfo;
import chs.utility.DesignHelper;
import chs.utility.SharedObjectDomainAccessibliltyChecker;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.dataservices.designmanipulation.DesignManipulation;
import chs.utility.dataservices.designmanipulation.DesignManipulationException;
import chs.utility.gfx.IDrawingComponentOwner;
import chs.utility.harness.HarnessProcessingUtils;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DesignUpgradeHelper;
import chs.utility.helpers.SystemPreferencesHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.ILogicCopyDesignPreCheck;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedPinListFilters;
import chs.utility.persist.DesignPersistenceUtils;
import chs.utility.persist.PersistPayload;
import chs.utility.persist.ProjectStorageHelper;
import chs.utility.persist.promise.IPromise;
import chs.utility.persist.promise.PromiseFactory;
import chs.utility.persist.saveparameters.ISaveDesignParameters;
import chs.utility.project.LogicDesignPromiseHelper;
import chs.utility.task.ITask;
import chs.utility.task.ITaskClient;
import chs.utility.ui.HTMLHelper;
import chs.utility.ui.ScrubOnTheFlyDataPacketDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseLifecycle extends DesignCapletLifecycleHelper implements IProjectChangeListener,
		ITaskClient, IPreferenceChangeListener, ILifeCycleChangeListener, ILogicLifecycle, ILogicCopyDesignPreCheck,
		IDesignSaveTeamPlayReferenceSync, IDesignMgrAccessibilityChangeListener
{

	// map Logic design UID --> Logic Model representing that design
	private Map<IUID, Model>
			models = null;

	protected Map<IUID, Model> removable_models = null;

	private DeleteDesignHelper m_deleteDesignHelper = null;

	private static final String lifeCycleReleaseLockOnDesigns = "LifeCycleReleaseLockOnDesigns";

	private WipeOutDesignsAtSaveCompletionTask m_wipeOutDesignsAtLast = null;

	private DesignUpgradeHelper designUpgradeHelper = new DesignUpgradeHelper();
	@Nullable private Icon m_Icon;

	@Override protected Class<? extends DesignCapletLifecycleHelper> getResourceClass()
	{
		return BaseLifecycle.class;
	}

	protected BaseLifecycle(ICaplet caplet)
	{
		super(caplet);
		models = new HashMap<IUID, Model>();
		removable_models = new HashMap<IUID, Model>();

		// Add the file types.
		FileTypeHolder xml = createFileTypeHolder();
		addFileTypeForOpen(xml);
		addFileTypeForSave(xml);

		// Add the Lifecycle Types
		// New Types
		addTypesForNew(caplet);

		// Register ourselves for project change notification
		getFIB().getProjectMgr().addProjectChangeListener(this);
	}

	protected FileTypeHolder createFileTypeHolder()
	{
		return new FileTypeHolder(AppInfo.getFullApplicationName(AppInfo.App.LOGIC_DESIGNER) + " - XML format",
				"xml", "application/x-CapitalLogic-xml");
	}

	@NotNull protected abstract DesignType getDesignType();

	protected abstract Class<? extends ISchemDiagram> getLogicDiagramClass();

	protected abstract Class<? extends ILogicDesign> getLogicDesignClass();

	@Override public boolean doesSupportObjectForInvoke(@NotNull final Object object)
	{
		if (object instanceof IDesignFolder) {
			IDesignFolder designFolder = (IDesignFolder) object;
			IFolderMgrNodeIterator iterator = designFolder.getChildren();
			IFolderMgrNode folderMgrNode = iterator.getNext();
			if (folderMgrNode instanceof RevisionNode) {
				RevisionNode revisionNode = (RevisionNode) folderMgrNode;
				if (revisionNode.getDesignDescriptor() != null &&
						!getDesignType().equals(revisionNode.getDesignDescriptor().getDesignType())) {
					return false;
				}
			}
		}
		return super.doesSupportObjectForInvoke(object);
	}

	@NotNull protected abstract OpenDiagramDelegate getOpenDiagramDelegate(IProjectPreferenceMgr preferences);

	@NotNull protected EditDiagramDelegate getEditDiagramDelegate()
	{
		return new EditDiagramDelegate(this, getResourceClass(), getEditDesignPermission());
	}

	@NotNull protected EditDesignDelegate getEditDesignDelegate()
	{
		return new EditDesignDelegate(this, getResourceClass(), getEditDesignPermission());
	}

	@NotNull protected AbstractDeleteDiagramsDelegate getDeleteDiagramsDelegate(boolean diagramOnlyDeletionMode)
	{
		if (diagramOnlyDeletionMode) {
			return new DeleteDiagramsOnlyDelegate(this, getResourceClass(), m_caplet, getEditDesignPermission());
		}
		return new DeleteDiagramsDelegate(this, getResourceClass(), m_caplet, getEditDesignPermission());
	}

	protected void addTypesForGeneralLifecycleActivities()
	{
		// Open Types
		ILifecycleType openDiagramType = getDiagramLifecycleType(getLogicDiagramClass(), "Lifecycle.Open.");
		addTypeForOpen(openDiagramType);
		ILifecycleType openRODiagramType = getRODiagramLifecycleType(getLogicDiagramClass(), "Lifecycle.OpenReadOnly.");
		addTypeForOpen(openRODiagramType);

		// Close Types
		ILifecycleType closeDesignType = getLifecycleType(getLogicDesignClass(), "Lifecycle.Close.");
		addTypeForClose(closeDesignType);
		ILifecycleType closeDiagramType = getLifecycleType(getLogicDiagramClass(), "Lifecycle.Close.");
		addTypeForClose(closeDiagramType);

		// Delete Types
		ILifecycleType deleteDesignType = getLifecycleType(getLogicDesignClass(), "Lifecycle.Delete.");
		addTypeForDelete(deleteDesignType);

		ILifecycleType deleteDesignonFolderType = getLifecycleType(IFolder.class, "Lifecycle.Delete.");
		addTypeForDelete(deleteDesignonFolderType);

		ILifecycleType deleteDiagramType = getLifecycleType(getLogicDiagramClass(), "Lifecycle.Delete.");
		addTypeForDelete(deleteDiagramType);

		// Edit Types
		ILifecycleType editDesignType = getLifecycleType(getLogicDesignClass(), "Lifecycle.Edit.");
		addTypeForEdit(editDesignType);
		ILifecycleType editDiagramType = getLifecycleType(getLogicDiagramClass(), "Lifecycle.Edit.");
		addTypeForEdit(editDiagramType);

		// Export
		ILifecycleType exportDesignType = getLifecycleType(getLogicDesignClass(), "Lifecycle.Export.");
		addTypeForExport(exportDesignType);

		// Copy Types
		ILifecycleType copyDesignType = getLifecycleType(getLogicDesignClass(), "Lifecycle.Copy.");
		addTypeForCopy(copyDesignType);

		// Create Revision Types
		ILifecycleType createRevisionDesignType =
				getLifecycleType(getLogicDesignClass(), "Lifecycle.CreateRevision.");
		addTypeForCreateRevision(createRevisionDesignType);
	}

	protected void addTypesForNew(ICaplet caplet)
	{
		ILifecycleType newDesignType = getDesignLifecyleType(IProject.class, "Lifecycle.NewDesign.",
				caplet.getDesignType());
		addTypeForNew(newDesignType);

		ILifecycleType newDesignOnFolderType =
				getDesignLifecyleType(INormalFolder.class, "Lifecycle.NewDesign.", caplet.getDesignType());
		addTypeForNew(newDesignOnFolderType);

		addTypeForNew(getDiagramLifecycleType(getLogicDesignClass(), "Lifecycle.NewDiagram."));
	}

	@NotNull @Override protected BaseModelMapData<?, ?, ?> createModelMapData(@NotNull List<?> context)
	{
		assert false;
		return null;
	}

	protected ILifecycleType getLifecycleType(Class<?> actionClass, String rsrcPrefix)
	{
		char mnemonic = ResourceMgr.getMnemonic(getResourceClass(), rsrcPrefix + "mnemonic");
		String menuText = ResourceMgr.getString(getResourceClass(), rsrcPrefix + "text");
		return new LifecycleTypeHolder(actionClass, menuText, mnemonic);
	}

	protected ILifecycleType getDiagramLifecycleType(Class<?> actionClass, String rsrcPrefix)
	{
		char mnemonic = ResourceMgr.getMnemonic(getResourceClass(), rsrcPrefix + "mnemonic");
		String menuText = ResourceMgr.getString(getResourceClass(), rsrcPrefix + "text");
		Icon icon = getDiagramIcon();
		return new LifecycleTypeHolder(actionClass, menuText, mnemonic, icon);
	}

	@NotNull
	protected ILifecycleType getRODiagramLifecycleType(@NotNull Class<?> actionClass, @NotNull String rsrcPrefix)
	{
		char mnemonic = ResourceMgr.getMnemonic(getResourceClass(), rsrcPrefix + "mnemonic");
		String menuText = ResourceMgr.getString(getResourceClass(), rsrcPrefix + "text");
		Icon icon = getDiagramIcon();
		return new LifecycleTypeHolder(actionClass, menuText, mnemonic, null, icon, true);
	}

	protected ILifecycleType getDesignLifecyleType(Class<?> actionClass, String rsrcPrefix, String param)
	{
		char mnemonic = ResourceMgr.getMnemonic(getResourceClass(), rsrcPrefix + "mnemonic");
		String menuText = ResourceMgr.getString(getResourceClass(), rsrcPrefix + "text", param);
		Icon icon = getDesignIcon();
		return new LifecycleTypeHolder(actionClass, menuText, mnemonic, icon);
	}

	protected Icon getDesignIcon()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_logicdesign.gif");
		return icon;
	}

	protected Icon getDiagramIcon()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_logicdiagram.gif");
		return icon;
	}

	@Nullable public Icon getDiagramTabIcon()
	{
		return m_Icon;
	}

	public void setDiagramTabIcon(@Nullable Icon icon)
	{
		m_Icon = icon;
	}

	protected int getDrawGridSpacing(IProjectPreferenceMgr preferenceMgr)
	{
		return preferenceMgr.getDrawGridSpacing(PreferenceContext.LOGIC);
	}

	protected final void registerDesignModel(ILogicDesign design, Model model)
	{
		models.put(design.getUID(), model);
	}

	public boolean closingController(ICapletController controller)
	{
		//**********************************************************************************************************
		// We should only get here if:-
		// a/ we are at the last window open on the design. ie user pressed X button
		// b/ window is closed programmatically , in this case calling code should have set
		//    m_programmaticCloseWindow to avoid infinite recursion. Could also check for modified status of all
		//    models.
		// c/ The window called in context of delete, in this case m_programmaticCloseWindow is set.
		//**********************************************************************************************************
		IUIDObject theClosingWindowsRootModel = controller.getCapletModel().getModelRoot();
		//TODO: 14953 - If diagram returned is null, Skip below checks and return true.
		if (theClosingWindowsRootModel == null) {
			return true;
		}
		ISchemDiagram theWindowsDiagram = (ISchemDiagram) theClosingWindowsRootModel;
		ILogicDesign theClosingWindowsDesign = theWindowsDiagram.getDesign();
		assert theClosingWindowsDesign != null;

		if (m_programmaticCloseWindow) {
			//(SP1202)dts0100830941 Build list DRC results are lost when a design is closed.
			DRCRunnerHelper.closeDesign(theClosingWindowsDesign);
			ICapletModel model = controller.getCapletModel();
			CapletModelHelper capletModelHelper = CommonUtils.cast(model, CapletModelHelper.class);
			if (capletModelHelper != null) {
				capletModelHelper.notifyModelClosed();
			}
			return true;
		}

		// *** Note closeDesign() below will call closeWindow() which will reenter this function, Hence we **must**
		// *** guard against infinite recursion. This can be done by either
		// *** A/ closeDesign, setting the Model to be not modified, where this is then checked for in this function
		// ***    before we get call closeDesign() again.
		// *** B/ In closeDesign, before we call closeWindow we set m_programmaticCloseWindow
		//
		// *** However with soln A/ closing via the X button , when there were no changes made, did not unlock
		// *** the design.  Since with the check of modified flag, we would return to prevent recursion
		// *** hence use of soln B/ data member m_programmaticCloseWindow
		final IProject project = theClosingWindowsDesign.getProject();
		assert project != null;
		if (closeDesign(project, theClosingWindowsDesign)) {
			// closing design invalidates browser which must be updated
			CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
			//(SP1202)dts0100830941 Build list DRC results are lost when a design is closed.
			DRCRunnerHelper.closeDesign(theClosingWindowsDesign);
			return true;
		}
		return false;
	}

	protected CloseDiagramSaveQuestionHelper.SaveQuestionResultEnum doSavePrompt(IDesign theClosingWindowsDesign)
	{
		return determineIfUserWantsToSave(CloseDiagramSaveQuestionHelper.getPromptContent(theClosingWindowsDesign),
				theClosingWindowsDesign);
	}

	@Override protected void doForceClosePrompt(String headMessage, ActionType actionType)
	{
		Frame parent = getParentFrame();
		if (actionType == ActionType.CREATE_DETAILEDDESIGN) {
			getStatusReporter().showInformationMessage(parent, headMessage,
					ResourceMgr
							.getString(getResourceClass(), "BaseLifecycle.msg.forceCloseCreateDetailedDesignMessage"));
		}
		else {
			getStatusReporter().showInformationMessage(parent, headMessage,
					ResourceMgr.getString(getResourceClass(), "BaseLifecycle.msg.forceClose"));
		}
	}

	/**
	 * Called when a particular object is closing.
	 *
	 * @see ICapletLifecycle#close(List)
	 */
	public boolean close(List<?> context)
	{
		// Get the project & design from the context
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		List<ISchemDiagram> diagramList = LifecycleUtils.getContextObjects(context, ISchemDiagram.class);
		if (project == null || design == null) {
			throw new IllegalArgumentException("Wrong context for close()");
		}

		if (diagramList.isEmpty()) { // Closing a design
			return closeDesign(project, design);
		}

		// Closing diagrams - possible multiple diagrams
		return closeDiagrams(project, design, diagramList);
	}

	@Override public void notifyAccessibility(IDesignMgrAccessibilityChangeEvent notifier)
	{
		if (notifier.getRefreshStatus() != RefreshStatusEnum.eRefreshNotNeeded) {
			List<IUID> designsWithNoDomainAccess = CAFUtils.getInstance().getDesignsForOpenDiagramsWithNoDomainAccess();
			for (IUID designUID : designsWithNoDomainAccess) {
				ILogicDesign design = UIDMgr.getObjectOfType(designUID, ILogicDesign.class);
				if (design != null) {
					closeDesign(Objects.requireNonNull(design.getProject()), design);
				}
			}
		}
	}

	private boolean closeDiagrams(@NotNull IProject iproject, @NotNull ILogicDesign design,
			List<ISchemDiagram> diagramList)
	{
		for (ISchemDiagram diagram : diagramList) {
			final ILogicDesign parentDesign = diagram.getDesign();
			if (parentDesign != design) {
				throw new IllegalArgumentException("Wrong context for close()");
			}
			if (parentDesign.getProject() != iproject) {
				throw new IllegalArgumentException("Wrong context for close()");
			}
		}

		for (ISchemDiagram diagram : diagramList) {
			List<ICapletWindow> windowsOnDesign = CAFUtils.getInstance().getCapletWindowsForDesign(design);
			if (windowsOnDesign.size() == 1) {
				// about to close the last window on the design, prompt for save
				return closeDesign(iproject, design);
			}
			else if (!closeDiagram(design, diagram)) {
				return false;
			}
		}
		return true;
	}

	protected void capletViewClosed(GfxView view, Model model)
	{
		IBaseDiagram vDiagram = view.getDiagram();
		if (vDiagram instanceof ISchemDiagram) {
			ISchemDiagram diagram = (ISchemDiagram) vDiagram;
			// Here we remove the model change listener added for analyis services
			if (CapitalAnalysisFactory.getAnalysisInterface() != null) {
				model.removeModelChangeListener((IModelChangeListener) LogicAnalysisServices.getAnalysisServices());
				IAnalysisNetlistScope scope = AnalysisServices.getCurrentAnalysisNetlistScope();
				if (scope != null) {

					ILogicDesign design = diagram.getDesign();
					if (design != null) {
						Iterator<ICAFWindow> windows = CAFUtils.getInstance().getWindowMgr().getWindows().iterator();
						windows.next();
						if (!windows.hasNext()) {
							//If all windows are getting closed with this stop the simulation.
							LogicAnalysisServices.getAnalysisServices().setDynamicSimulationMode(
									scope.getUid(), "", AnalysisServices.DYN_SIM_OFF, null);
						}
						else if (isLastWindowOnContainer(view.getWindow())) {
							//if this is the last diagram of design remove it from active analysis set
							AnalysisServices.removeActiveAnalysis(design.getUID().getString());
						}
					}
				}
			}
		}
	}

	protected void capletModelClosed(ILogicModel model)
	{
		// TODO jacobt FEAT13040 : generify ICAFProjectMgr.getWindows()
		IProject project = model.getDesign().getProject();
		if (project != null) {
			Collection<ICAFWindow> windows = getFIB().getProjectMgr().getWindows(project);
			for (ICAFWindow cw : windows) {
				if (cw instanceof ICapletWindow) {
					ICapletWindow ccw = (ICapletWindow) cw;
					ICapletView currView = ccw.getCurrentView();
					if (currView != null) {
						if (model == currView.getCapletModel()) {
							if (currView instanceof GfxView && model instanceof Model) {
								capletViewClosed((GfxView) currView, (Model) model);
							}
						}
					}
				}
			}
		}
		else {
			assert false : "Failed to get project";
		}
	}

	public boolean capletWindowClosed(ICapletWindow capWin)
	{
		Set<IBaseDiagram> diagrams = CAFUtils.getInstance().getWindowDiagrams(capWin);
		ILogicDesign design = CommonUtils.
				cast(!diagrams.isEmpty() ? diagrams.iterator().next().getDesignContainer() : null, ILogicDesign.class);
		if (design != null && design.isUnderConcurrentEdit()) {
			boolean allOtherWindowsHaveUnlockedDiagram = true;
			for (ICapletWindow window : CAFUtils.getInstance().getCapletWindowsForDesign(design)) {
				if (window != capWin) {
					boolean hasLockedDiagrams = false;
					for (IBaseDiagram diagram : CAFUtils.getInstance().getWindowDiagrams(window)) {
						if (diagram instanceof ISchemDiagram && ((ICOGManagedLockable) diagram).isLocked()) {
							hasLockedDiagrams = true;
							break;
						}
					}
					if (hasLockedDiagrams) {
						allOtherWindowsHaveUnlockedDiagram = false;
						break;
					}
				}
			}
			if (allOtherWindowsHaveUnlockedDiagram) {
				SaveResult saveResult = showDialogAndStartSave(design);
				if (!saveResult.result) {
					//user cancelled.
					return false;
				}
				doCapletWindowClosed(capWin);
				final IProject project = design.getProject();
				assert project != null;
				String saveTaskId = DesignPersistenceUtils.getSaveTaskId(project);
				ensureTaskExecutedOnlyAfterSaveCompletes(project, new OnSaveCompleteTask(saveTaskId)
				{
					@Override public void execute()
					{
						Model model = getModel(design);
						//first release the locks before doing revert and unload. otherwise deleted objects
						//(which will be in locked state) will not be reloaded and would cause issue due to
						//in-memory out-of-sync connectivity and diagrams.
						IDesignLockStrategy.releaseLock(design);
						//do revert the design only if design was not being saved.
						if (model != null && saveResult.dialogOption == JOptionPane.NO_OPTION) {
							//dts0100874896 Incorrect usage information is reported on checking show usage after Move to action.
							//If the model is not editable, no need to revert shared usages
							//though we should not be coming here now for non-editable models.
							//but no harm in putting extra defences. check the conditions again.
							boolean doRollbackSharedHighwayConnections = model.isStateValidToPersist();
							for (ISchemDiagram diag : model.getDiagrams()) {
								if (diag instanceof IIncLoadable) {
									//
									// If it can't be skeletonized, it is completely local to logic, and so should be deleted.
									//
									IIncLoadable iload = (IIncLoadable) diag;
									if (iload.isSkeletonizable()) {

										iload.setSkeleton(true);
									}
								}
							}
							design.revert();
							model.setModified(false);
							//dts0100874896 Incorrect usage information is reported on checking show usage after Move to action.
							//If the model is not editable, no need to revert shared usages
							//though we should not be coming here now for non-editable models.
							//but no harm in putting extra defences. check the conditions again.
							if (doRollbackSharedHighwayConnections) {
								DesignPersistenceUtils.rollBackSharedHighwayConnections(project, design);
							}
							clearPasteBuffer(model);
							ICapletController controller = model.getController();
							if (controller != null) {
								controller.clearUndoQueue();
							}
						}
					}
				});
			}
			else {
				doCapletWindowClosed(capWin);
			}
		}
		else {
			doCapletWindowClosed(capWin);
		}
		return true;
	}

	private void doCapletWindowClosed(ICapletWindow capWin)
	{
		CapletViewIterator cvIt = capWin.getViews();
		while (cvIt.hasNext()) {
			ICapletView capView = cvIt.getNext();
			if (capView instanceof GfxView) {
				GfxView view = (GfxView) capView;
				ICapletModel capModel = view.getCapletModel();
				if (capModel instanceof Model) {
					capletViewClosed(view, (Model) capModel);
				}
			}
		}
	}

	private boolean closeDiagram(@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		// construct separate window list because we need to remove windows from window mgr as we iterate over them
		for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (cafWin instanceof ICapletWindow && cafWin.getCaplet() == m_caplet) {
				CapletViewIterator cvIt = ((ICapletWindow) cafWin).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();

					if (view.getDiagram() == diagram) {
						try {
							m_programmaticCloseWindow = true;  // prevent any prompts
							cafWin.closeWindow();
						}
						finally {
							m_programmaticCloseWindow = false;
						}
					}
				}
			}
		}
		// For non-editable diagrams, we want to remove the model from memory here. (simons)
		if (!IDesignLockStrategy.isLocked(design)) {
			removeDiagramFromModel(diagram);
		}
		return true;
	}

	private void wipeOutDesignAtLast(@NotNull IProject project, @NotNull ILogicDesign design, boolean unlockDesign,
			@NotNull WindowCloseStatus windowCloseStatus)
	{
		WipeOutDesignOnSaveCompleteTask wipeOutDesignOnSaveTask =
				createUnloadDesignTask(project, design, unlockDesign, windowCloseStatus);
		ensureTaskExecutedOnlyAfterSaveCompletes(design, wipeOutDesignOnSaveTask);
	}

	/**
	 * Creates a task that will unload and optionally unlock an IDesign.
	 * <p>
	 *
	 * @param project        Project that owns the design
	 * @param design         The design the task will unload
	 * @param unlockDesign   If true, design will be unlocked after unloading
	 * @param windowCloseResult specifies the status of the caplet windows
	 * @return The task to do the work
	 */
	@NotNull protected WipeOutDesignOnSaveCompleteTask createUnloadDesignTask(@NotNull IProject project,
			@NotNull ILogicDesign design, boolean unlockDesign, @NotNull WindowCloseStatus windowCloseStatus)
	{
		return new WipeOutDesignOnSaveCompleteTask(project, design, unlockDesign, windowCloseStatus);
	}

	private void ensureTaskExecutedOnlyAfterSaveCompletes(@NotNull IUIDObject designObject,
			@NotNull OnSaveCompleteTask saveCompleteTask)
	{
		boolean scheduled = m_wipeOutDesignsAtLast != null &&
				m_wipeOutDesignsAtLast.scheduleWipeOutDesignTask(designObject, saveCompleteTask);
		if (!scheduled) {
			//release lock itself. save task is done for now.
			saveCompleteTask.execute();
		}
	}

	/**
	 * Closes a design and its diagrams. Goes through each diagram to see if there have been any changes.
	 *
	 * @param project      the project
	 * @param design       to be closed
	 * @param unlockDesign if true, the design is unlocked after successful close. If the design could not be closed,
	 *                     the design is not unlocked.
	 * @return <code>true</code> if design was closed; <code>false</code> otherwise.
	 */
	public boolean closeDesign(IProject project, ILogicDesign design, boolean unlockDesign)
	{
		final ILogicDesign logicDesign = design;
		Model model = getModel(logicDesign);

		// to check the design is actually closed in plane and trigger sync or it was only unloaded from memory
		// (eg. while generating logic design from functional design logic design is only loaded and unloaded from memory)
		WindowCloseStatus windowCloseStatus = WindowCloseStatus.NOT_CLOSED;
		//delete the XML dump of the design if it exists.
		if (model != null) {
			SaveResult result = showDialogAndStartSave(design);
			if (!result.result) {
				return false;
			}


			//Autorecovery and close design should be mutually exclusive.
			//Autorecovery does a validate and export design and the close does a unload design and hence the mutex
			CAFUtils.getInstance().getScanningLock().withScanningLockDo(() -> {
				// Clear autorecovery data if cancel was not pressed
				CAFUtils.getInstance().getAutoRecovery().discardDesignRecoveryInfoIfDesignLocked(design);
			});
			//do some preprocessing before closing the model. analysias related stuff are done here.
			capletModelClosed(model);

			// We have to set discardAndClose before closeAllWindows(model);
			// because FunctionalChangeDetector.windowChanged(WindowChangeEvent wce) is getting called by this method
			// which is triggering sync if user switching from one diagram to another
			boolean isDiscardAndClose = result.dialogOption == JOptionPane.NO_OPTION;
			model.setDiscardAndClose(isDiscardAndClose);

			// Close all the windows [before we unload the design, and after we [may have] saved]
			windowCloseStatus = closeAllWindows(model);

			if (isDiscardAndClose) {
				// remove the model and revert any changes
				for (ISchemDiagram diag : model.getDiagrams()) {
					if (diag instanceof IIncLoadable) {
						//
						// If it can't be skeletonized, it is completely local to logic, and so should be deleted.
						//
						IIncLoadable iload = (IIncLoadable) diag;
						if (!iload.isSkeletonizable()) {
							logicDesign.removeDiagram(diag);
						}
						iload.setSkeleton(true);
					}
				}
				//dts0100874896 Incorrect usage information is reported on checking show usage after Move to action.
				//If the model is not editable, no need to revert shared usages
				//though we should not be coming here now for non-editable models.
				//but no harm in putting extra defences. check the conditions again.
				if (model.isStateValidToPersist()) {
					DesignPersistenceUtils.rollBackSharedHighwayConnections(project, design);
				}
				clearPasteBuffer(model);
			}
			// Cleanup the model and remove it from the lifecycle
			destroyModel(model);
		}
		// some clients use this to close a design with no model - e.g. CAVAI

		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		btm.waitForBoundaryCompletion();

		// Now this Logic Design is unloaded notify any Integrator designs it may be associated with, may need sync.
		// Obviously doing this work here will not catch *all* cases where a LogicDesign has been unloaded and
		// Integrator needs synchronization, so this code may have to be refactored.
		// Do this prior to unlocking we are not interested in changes from other users at this time.
		// See dts0100481789 - Exception: Auto Place all devices after deleting an instance of shared source net in
		//                     Logical design is throwing assertion error.
//		List<ICapletWindow> capletWindows =
//				CollectionUtils.getObjectList(getFIB().getWindowMgr().getWindows(), ICapletWindow.class);
//		for (ICapletWindow capletWindow : capletWindows) {
//			capletWindow.getController().updateFrom(design.getUID());
//		}

		wipeOutDesignAtLast(project, design, unlockDesign, windowCloseStatus);

		return true;
	}

	private class SaveResult
	{

		int dialogOption;
		boolean result;

		SaveResult(int dialogOption, boolean result)
		{
			this.dialogOption = dialogOption;
			this.result = result;
		}
	}

	@NotNull private SaveResult showDialogAndStartSave(IDesign design)
	{
		ICloseAllContext closeAllActionContext = CAFUtils.getInstance().getCloseContext();
		IProject project = design.getProject();
		Model model = getModel((ILogicDesign) design);
		SaveResult result = new SaveResult(JOptionPane.CANCEL_OPTION, true);
		if (model.isStateValidToPersist()) {
			// See if we really do want to close..
			boolean isCloseAllDiagramsAction = closeAllActionContext != null;
			if (isCloseAllDiagramsAction && !closeAllActionContext.promptMessageForUnSavedDesign()) {
				if (closeAllActionContext.getUserSelectedChoice().equals(
						CloseDiagramSaveQuestionHelper.SaveQuestionResultEnum.DISCARDANDCLOSE.name())) {
					result = new SaveResult(JOptionPane.NO_OPTION, true);
				}
			}
			else {
				switch (doSavePrompt(design)) {
					case SAVEANDCLOSE:
						// Check for running save
						performAutomatedPublish(design, closeAllActionContext, project);
						if (isCloseAllDiagramsAction && !closeAllActionContext.promptMessageForUnSavedDesign()) {
							return new SaveResult(JOptionPane.CANCEL_OPTION, false);
						}
						boolean anyRunningTask = anyRunningSaveForProject(project);
						if (anyRunningTask) { // The prev save is still not completed
							return new SaveResult(JOptionPane.YES_OPTION, false);
						}
						// Save the design.
						if (save(project, design, (ISchemDiagram) null, false, true) == null) {
							// Design is not saved, so do not close the design
							return new SaveResult(JOptionPane.YES_OPTION, false);
						}
						result = new SaveResult(JOptionPane.YES_OPTION, true);
						break;
					case DISCARDANDCLOSE:
						result = new SaveResult(JOptionPane.NO_OPTION, true);
						break;
					case CANCEL:
					default:
						return new SaveResult(JOptionPane.CANCEL_OPTION, false);
				}
			}
		}
		// Did a scrub occur in the model being closed?
		boolean scrubOccurred = model.scrubOccurred();
		if (scrubOccurred) {
			ScrubOnTheFlyDataPacketDisplay dlg =
					new ScrubOnTheFlyDataPacketDisplay(CAFUtils.getInstance().getDialogFrame());
			dlg.displayDialog();
		}
		return result;
	}

	private void performAutomatedPublish(@NotNull IDesign design, @Nullable ICloseAllContext closeAllContext,
			@NotNull IProject project)
	{
		if (!BridgesIntegrationServices.isTeamCenterHandshakeActive()) {
			return;
		}

		Set<IDesignContainer> modifiedDesigns = new HashSet<>(Collections.singletonList(design));

		if (closeAllContext != null && closeAllContext.promptMessageForUnSavedDesign()) {
			return;
		}
		if (closeAllContext != null) {
			modifiedDesigns.addAll(getModifiedDesignsForProject(project));
			if (!closeAllContext.getActivatedStatusForAllDiagrams()) {
				String uidOfDesignNotToBePublished = closeAllContext.getUIDOfDesignNotToBeSaved();
				Optional<IDesignContainer> designContainerToNotPublish = modifiedDesigns.stream()
						.filter(designContainer -> designContainer.getUID().getString()
								.equals(uidOfDesignNotToBePublished))
						.findFirst();
				designContainerToNotPublish.ifPresent(modifiedDesigns::remove);
			}
		}

		new AutomatedPublishToTeamcenter().performPublishOnSave(modifiedDesigns);
	}

	private Set<IDesignContainer> getModifiedDesignsForProject(@NotNull IProject project)
	{
		Set<IDesignContainer> modifiedDesigns = new HashSet<>();
		CapletIterator capletIterator = CAFUtils.getInstance().getCapletMgr().getCaplets();

		while (capletIterator.hasNext()) {
			ICaplet caplet = capletIterator.getNext();
			ICapletLifecycle lifecycle = caplet.getLifecycle();

			if (lifecycle.isModified(project)) {
				modifiedDesigns.addAll(lifecycle.getModifiedDesignsForSave(project).keySet());
			}
		}
		return modifiedDesigns;
	}

	/**
	 * Closes a design and its diagrams. Goes through each diagram to see if there have been any changes. The design is
	 * unlocked after a succesful close.
	 *
	 * @param project the project
	 * @param design  to be closed
	 * @return <code>true</code> if design was closed; <code>false</code> otherwise.
	 */
	public boolean closeDesign(@NotNull IProject project, ILogicDesign design)
	{
		return closeDesign(project, design, true);
	}

	/**
	 * Close any window open on the Model
	 *
	 * @param model The model
	 */
	@NotNull
	protected WindowCloseStatus closeAllWindows(@NotNull ILogicModel model)
	{
		// TODO jacobt FEAT13040 : generify ICAFProjectMgr.getWindows()
		WindowCloseStatus closeStatus = WindowCloseStatus.NOT_CLOSED;
		IProject project = model.getDesign().getProject();
		if (project != null) {
			Collection<ICAFWindow> windows = getFIB().getProjectMgr().getWindows(project);
			for (ICAFWindow cw : windows) {
				if (cw instanceof ICapletWindow) {
					ICapletWindow ccw = (ICapletWindow) cw;
					ICapletView currView = ccw.getCurrentView();
					if (currView != null) {
						if (model == currView.getCapletModel()) {
							try {
								m_programmaticCloseWindow = true;// prevent recusrion and prompts
								ccw.closeWindow();
								closeStatus = WindowCloseStatus.CLOSED;
							}
							finally {
								m_programmaticCloseWindow = false;
							}
						}
					}
				}
			}
		}
		return closeStatus;
	}

	protected void destroyModel(IDesignContainer design)
	{
		Model model = getModel((ILogicDesign) design);
		if (model != null) {
			destroyModel(model);
		}
	}

	@Override protected DesignEditDialog createEditDialog(Frame frame, String title, IProject project,
			IDesignContainer srcDesign, boolean isDesignChanged, boolean isCopy, boolean isRevision, boolean readonly,
			IReleaseLevelController teamCenterReleaseLevelController)
			throws NoReleaseLevelsException

	{

		return new TCLogicDesignEditDialog(frame, title, project, srcDesign, true, isDesignChanged, false, isCopy,
				isRevision, NOT_EVALUATION, NOT_NEW, readonly, teamCenterReleaseLevelController,
				(() -> getAdditionalDesignUIUserAttrsAndPropsContext()));
	}

	public void destroyModel(Model model)
	{
		for (ISchemDiagram diagram : model.getDiagrams()) {
			diagram.setWithModel(false);
		}
		ILogicDesign design = model.getDesign();
		if (design != null) {
			design.removePersistenceSessionListener();
			models.remove(design.getUID());
			removable_models.remove(design.getUID());
		}

		//destroy should be done at the end.
		model.getController().destroy();
		model.destroy();
	}

	@Override public void removeDiagramFromModel(ISchemDiagram diagram)
	{
		ILogicDesign design = diagram.getDesign();
		if (design != null) {
			Model model = getModel(design);
			if (model != null) {
				model.removeDiagram(diagram);
				diagram.setWithModel(false);
				// Remove fix for DR569329 - see DR for details
				if (model.getDiagrams().isEmpty()) {
					//models.remove(des.getUID());
					removable_models.put(design.getUID(), model);
				}
			}
		}
	}

	@Override public void diagramDeleted(Model model, ISchemDiagram diagram)
	{
		showDiagramDeletionMessageRemoveFromFramework(model, diagram);
	}

	@Override public void designModified(@NotNull ILogicDesign design, boolean designAlreadyLocked,
			boolean releaseLevelChanged)
	{
		processLocksAfterEditDesign(design, designAlreadyLocked, releaseLevelChanged);
	}

	protected abstract ICapletController createController(ICaplet caplet, ILogicDesign design, ISchemDiagram
			diagram);

	/**
	 * Creates a new Schematic Diagram. The context should have a project, and a design in which to create the
	 * schematic. If there is no design or connectivity then create them.
	 */
	public boolean createNew(List<?> context)
	{
		if (!checkEditDesignPermission()) {
			return false;
		}
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		CreateNewDelegate lifeCycleDelegate;
		if (design == null) {
			IProject project = LifecycleUtils.getContextObject(context, IProject.class);
			if (project == null) {
				throw new IllegalArgumentException("Wrong context for createNew()");
			}
			lifeCycleDelegate = getCreateNewDesignDelegate(project.getPreferences());
		}
		else {
			IProject project = design.getProject();
			assert project != null;
			lifeCycleDelegate = getCreateNewDiagramDelegate(project.getPreferences(), context);
		}
		Pair<Boolean, IBaseDiagram> resultPair = lifeCycleDelegate.createNew(context);
		ISchemDiagram diagram = (ISchemDiagram) resultPair.getSecond();
		if (diagram != null && resultPair.getFirst()) {
			DiagramAuditTrialHelper.getInstance().postDiagramAuditTrail(diagram, AuditableEventType.DIAGRAM_CREATED);
		}
		return resultPair.getFirst();
	}

	@NotNull protected abstract CreateNewDelegate getCreateNewDiagramDelegate(IProjectPreferenceMgr preferences,
			List<?> context);

	@NotNull protected abstract CreateNewDelegate getCreateNewDesignDelegate(IProjectPreferenceMgr preferences);

	@NotNull protected abstract CreateNewDelegate getCreateFilteredDiagramDelegate(IProjectPreferenceMgr
			preferences);

	/**
	 * Creates a schematic diagram based on filtering criteria.
	 *
	 * @param context, the context (project/design/diagram).
	 * @return boolean baseDiagram pair, first element is 'true' if the object was created, second element is optional
	 * diagram to open once boundary transaction is reached (see ProjectWindow.LifeCycleCreateNewAction).
	 */
	public Pair<Boolean, IBaseDiagram> createFiltered(List<?> context)
	{
		return createFilteredWithMultipleDesigns(List.of(context)).iterator().next();
	}

	/**
	 * Creates a schematic diagram based on filtering criteria.
	 *
	 * @param contextList, the contexts (project/design/diagram).
	 * @return boolean baseDiagram pairslist, first element is 'true' if the object was created, second element is optional
	 * diagram to open once boundary transaction is reached (see ProjectWindow.LifeCycleCreateNewAction).
	 */
	public List<Pair<Boolean, IBaseDiagram>> createFilteredWithMultipleDesigns(List<List<?>> contextList)
	{

		if (contextList == null || contextList.isEmpty()) {
			throw new IllegalArgumentException("Wrong context for createNew()");
		}
		List<?> context = contextList.get(0);
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		if (design != null) {
			IProject project = design.getProject();
			assert project != null;
			CreateNewDelegate lifeCycleDelegate = getCreateFilteredDiagramDelegate(project.getPreferences());
			return lifeCycleDelegate.createNewWithMultipleDesigns(contextList);
		}
		else {

			throw new IllegalArgumentException("Wrong context for createNew()");
		}
	}

	@Nullable
	public ICapletModel openDiagram(IProject project, @NotNull ISchemDiagram diagram)
	{
		OpenDiagramDelegate lifeCycleDelegate = getOpenDiagramDelegate(project.getPreferences());
		return lifeCycleDelegate.openDiagram(null, project, diagram, true);
	}

	@Override public void openDiagramOnMove(ISchemDiagram diagram)
	{
		IProject project = diagram.getProject();
		OpenDiagramDelegate lifeCycleDelegate = getOpenDiagramDelegate(project.getPreferences());
		lifeCycleDelegate.openDiagramOnMove(diagram);
	}

	/**
	 * Creates a diagram with the given name without displaying the new diagram dialog. If the name is already used then
	 * an exception is thrown.
	 *
	 * @param design the design in which to create diagram
	 * @param name   the name of the diagram
	 * @return ISchemDiagram
	 * @throws RuntimeException thrown if name is already used.
	 */
	@Override public ISchemDiagram createDiagramWithName(ILogicDesign design, String name)
	{
		String theName = name;
		if (theName == null) {
			theName = getValidDiagramName(design);
		}
		if (!DiagramUtils.validDiagramName(name, design)) {
			throw new IllegalArgumentException("Diagram name already used");
		}
		IUID uid = CAFUtils.getInstance().getCommonFactory().createUID();
		ISchemFactory factory = FactoryMgr.getSchemFactory();
		ISchemDiagram diagram = factory.constructDiagram(uid, theName, design);
		design.addDiagram(diagram);
		updateOpenDiagramBorder();
		return diagram;
	}

	@Override public boolean openExistingInEditableMode(List<?> context)
	{
		return openExistingWithDiagramOpenContext(new DiagramOpenContext(context, false), true);
	}

	@Override public boolean openExisting(boolean isForReadyOnly, List<?> context)
	{
		return openExistingWithDiagramOpenContext(getDiagramOpenContext(context, isForReadyOnly), true);
	}

	@NotNull private DiagramOpenContext getDiagramOpenContext(List<?> context, boolean isForReadyOnly)
	{
		return new DiagramOpenContext(context, isForReadyOnly);
	}

	public boolean openExisting(List<?> context)
	{
		return openExisting(context, true);
	}

	/**
	 * Open a window on the existing schematic
	 */
	@Override public boolean openExisting(List<?> context, boolean showAltBuildListDlg)
	{
		return openExistingWithDiagramOpenContext(getDiagramOpenContext(context, false), showAltBuildListDlg);
	}

	private boolean openExistingWithDiagramOpenContext(DiagramOpenContext diagramOpenContext,
																	 boolean showAltBuildListDlg)
	{
		List<?> context = diagramOpenContext.getContext();
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		IPromise promise = PromiseFactory.createPromise();
		if (project != null) {
			LogicDesignPromiseHelper.whilstOpeningDiagramDo(promise,project);
		}
		Boolean result = promise.issue(Boolean.class).thenApply(() -> {
			return openExistingWithDiagramOpenContextWithoutPromise(diagramOpenContext, showAltBuildListDlg);
		});
		return result != null ? result.booleanValue() : false;
	}

	private boolean openExistingWithDiagramOpenContextWithoutPromise(DiagramOpenContext diagramOpenContext,
			boolean showAltBuildListDlg)
	{
		List<?> context = diagramOpenContext.getContext();
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);

		final String titleCannotOpenDiagram = "BaseLifecycle.diagram.cannotopendiagram.title";
		final Class<BaseLifecycle> resourceClass = BaseLifecycle.class;
		if (project == null) {
			getStatusReporter().showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(resourceClass, titleCannotOpenDiagram),
					ResourceMgr.getString(resourceClass, "BaseLifecycle.diagram.noProject.message"));
			return false;
		}
		if (design == null) {
			getStatusReporter().showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(resourceClass, titleCannotOpenDiagram),
					ResourceMgr.getString(resourceClass, "BaseLifecycle.diagram.noDesign.message"));
			return false;
		}
		UtilsHelper.refreshDesignsIfNeeded(design);
		if (LogicConcurrencyHelper.isLogicInMultiUserMode(project) && designUpgradeHelper.isUpgradeNeeded(
				(IUpgradeableDesignContainer) design, new DefaultDesignUpgradeContext())) {
			boolean designAlreadyLocked = design.isLocked();
			if (!designAlreadyLocked) {
				if (!design.lock()) {
					getStatusReporter().showErrorMessage(BaseLifecycle.class,
							"BaseLifecycle.openDiagram.notUpgradedDesign",
							CollectionUtils.createArray(design.getFullName()), null);
					return false;
				}
			}
		}
		boolean bStatus = false;
		boolean bDesignAlreadyLocked = false;
		try {
			if (design.getLoadedConnectivity() != null) {
				SharedObjectDomainAccessibliltyChecker.authenticateInvalidSharedObjectsFromLoadedDesign(design);
			}
			bDesignAlreadyLocked = IDesignLockStrategy.isLocked(design);
			OpenDiagramDelegate lifeCycleDelegate = getOpenDiagramDelegate(project.getPreferences());
			lifeCycleDelegate.setOpenAsReadOnly(diagramOpenContext.isForReadOnly());
			bStatus = lifeCycleDelegate.openExisting(diagramOpenContext.getContext(), showAltBuildListDlg);
		}
		catch (DomainInaccessibleSharedObjectDesignHandledRuntimeException e) {
			IExceptionHandler handler = ExceptionHandlerCreationFactory.createHandler(e);
			if (handler != null) {
				handler.handle(e);
			}
			IBaseDiagram diagram = LifecycleUtils.getContextObject(context, IBaseDiagram.class);
			if (diagram != null) {
				// close views and unload the diagram.
				for (ICapletView view : CAFUtils.getInstance().getViewsForDiagram(diagram)) {
					view.getWindow().closeWindow();
				}
				diagram.unloadData();
			}
			return false;
		}
		finally {
			if (!bStatus) {
				Model model = getModel(design);
				if (model != null && removable_models.containsValue(model)) {
					destroyModel(model);
					model = null;
				}

				if (model == null && !bDesignAlreadyLocked) {
					if (executeWithReturn(Result.DONOTBYPASS,
							OpenDiagramCustomLogicRegistry.ID.DECIDERELEASELOCK_INQUERYPHASE) ==
							Result.DONOTBYPASS) {
						IDesignLockStrategy.releaseLock(design);
					}
				}
			}
		}
		return bStatus;
	}

	public void createNewWindow(ICAFWindow window)
	{
		CAFUtils.getInstance().getOutputWindow()
				.sendDebugMessage("Creating new window based on an existing one", true);

		IWindowMgr wm = getFIB().getWindowMgr();
		if (window instanceof ICapletWindow) {
			ICapletWindow cw = (ICapletWindow) window;
			ICapletController sc = cw.getController();
			Model lm = (Model) sc.getCapletModel();

			ISchemDiagram diagram = (ISchemDiagram) ((IDrawingComponentOwner) cw.getCurrentView()).getSheet();

			ICapletWindow newwin = wm.createCapletWindow(m_caplet, sc);

			String title = LogicCapletUtils.getDiagramTitle(lm.getDiagram(), !lm.isEditable());
			String viewName = LogicCapletUtils.getViewName(lm.getDiagram());

			newwin.setTitle(title);
			newwin.getContainer().setLayout(new GridLayout(1, 1, 4, 4));

			// Create the view in the window
			View lv = new View(lm, newwin);
			lv.setDiagram(diagram);
			lv.setName(viewName);

			// Set the new window as active
			newwin.display();
		}
	}

	public boolean isModified(@NotNull Object obj)
	{
		if (obj instanceof ISchemDiagram) {
			ICapletModel model = getModel(((ISchemDiagram) obj).getDesign());
			return (model != null && model.isModified());
		}
		else if (obj instanceof IProject || obj instanceof IDesign) {
			// See if there have been any changes to any models belonging to
			// this object.
			if (!removable_models.isEmpty()) {
				for (IUID objUID : removable_models.keySet()) {
					models.remove(objUID);
				}
				removable_models.clear();
			}
			for (Model model : models.values()) {
				if (((model.getDesign() == obj || model.getDesign().getProject() == obj)) && model.isModified()) {
					return true;
				}
			}
			return false;
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public Map<IDesignContainer, ICapletModel> getModifiedDesignsForSave(IProject proj)
	{
		Map<IDesignContainer, ICapletModel> theList = new HashMap<IDesignContainer, ICapletModel>();
		for (Model model : models.values()) {
			IDesign design = model.getDesign();
			if (design.getProject() == proj && model.isModified()) {
				theList.put(design, model);
			}
		}
		return theList;
	}

	protected List<ICapletModel> getModifiedModels(IDesignContainer designContainer)
	{
		List<ICapletModel> theList = new ArrayList<ICapletModel>();
		ICapletModel m = models.get(designContainer.getUID());
		if (m != null && m.isModified()) {
			theList.add(m);
			return theList;
		}
		return Collections.emptyList();
	}

	/**
	 * * Save all designs in the current project.
	 */
	@Nullable @Override
	public ITask save(IUIDObject root, @NotNull SaveContext context, boolean runDRCs)
	{
		// DR 445855: 'savealways' mode is for the save done when deleting a diagram, when we want to save designs
		// with no modified diagrams. It should not be used here.
		final boolean savealways = false;
		return save(root, null, (ISchemDiagram) null, savealways, runDRCs);
	}

	/**
	 * * Helper method used to save off a specific design instead of clearing the Undo queue. * *
	 *
	 * @param root    the root
	 * @param sub     the design
	 * @param runDRCs If true runs Design Rule Checks on design objects
	 */
	@Override public ITask save(IUIDObject root, IUIDObject sub, boolean runDRCs)
	{
		if (!(root instanceof IProject && sub instanceof IDesign)) {
			throw new IllegalArgumentException("Invalid arguments for Lifecycle.save()");
		}
		return save(root, (IDesign) sub, (ISchemDiagram) null, false, runDRCs);
	}

	/**
	 * @param container       the container
	 * @param restrictDesign  avoid design
	 * @param restrictDiagram avoid diagram
	 * @param savealways      always save
	 * @param runDRCs         If true runs Design Rule Checks on all design objects
	 * @return ITask
	 */
	@Nullable
	private ITask save(IUIDObject container, @Nullable IDesign restrictDesign, @Nullable ISchemDiagram
			restrictDiagram,
			boolean savealways, boolean runDRCs)
	{
		return save(container, restrictDesign,
				restrictDiagram != null ? Collections.singletonList(restrictDiagram) : null,
				savealways, runDRCs);
	}

	@Override
	public ITask save(IUIDObject container, @Nullable IDesignContainer restrictDesign,
			@Nullable Collection<? extends IBaseDiagram> restrictDiagrams, boolean savealways, boolean runDRCs)
	{
		ILogicDesign logicDesign = null;
		if (restrictDesign != null) {
			// Cast it into a logical design.
			logicDesign = (ILogicDesign) restrictDesign;
		}

		Collection<ISchemDiagram> restrictSchemDiagrams = null;
		if (restrictDiagrams != null && !restrictDiagrams.isEmpty()) {
			restrictSchemDiagrams = new ArrayList<ISchemDiagram>(restrictDiagrams.size());
			for (IBaseDiagram diagram : restrictDiagrams) {
				// Cast it into a ISchemDiagram
				ISchemDiagram schemDiagram = (ISchemDiagram) diagram;
				restrictSchemDiagrams.add(schemDiagram);
			}
		}

		Map<ILogicDesign, Collection<Model>> modDesigns = new HashMap<ILogicDesign, Collection<Model>>();
		IProject proj = (IProject) container;

		// If save always, make sure designs are saved, even if they have no diagrams
		if (savealways) {
			forceAddDesignToSave(proj, logicDesign, modDesigns);
		}

		for (Model model : models.values()) {
			ILogicDesign design = model.getDesign();
			if (design.getProject() == proj) {
				addDesignToSave(design, model, savealways, modDesigns, logicDesign);
			}
		}

		// FEAT3184 - Object Model Integrity
		// FEAT12834 - replaced by lower level call in DesignCapletLifecycleHelper.saveDesign(), called by save() call below.
//		ValidationHelper.validateBeforeSave(modDesigns.keySet());

		if (!modDesigns.isEmpty()) {
			return doSave(proj, modDesigns, runDRCs, restrictSchemDiagrams);
		}
		else {
			// UXFWK-1356: Check to see if we could not save due to the invalid state of the CDH
			throwExceptionIfCreationDeletionHelperIsIncorrect();
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
					ResourceMgr.getString(BaseLifecycle.class, "BaseLifecycle.diagram.NoChangesToSave.message"));
			return null;
		}
	}

	@Nullable public void takeAllOpenedCapletWindowsAndSave(List<ICAFWindow> windows)
	{
		Collection<IDesignContainer> modDesigns = new ArrayList<>();
		IProject iProject = null;
		for (Object obj : windows) {
			ICapletWindow capletWindow = (ICapletWindow) obj;
			IUIDObject theClosingWindowsRootModel = capletWindow.getController().getCapletModel().getModelRoot();
			if (theClosingWindowsRootModel == null) {
				continue;
			}
			ISchemDiagram theWindowsDiagram = (ISchemDiagram) theClosingWindowsRootModel;
			ILogicDesign theClosingWindowsDesign = theWindowsDiagram.getDesign();
			if (iProject == null) {
				iProject = theClosingWindowsDesign.getProject();
			}
			modDesigns.add(theClosingWindowsDesign);
		}
		save(iProject, modDesigns, false, false);
	}

	@Nullable private ITask save(IProject proj, Collection<IDesignContainer> modDesigns1, boolean savealways,
			boolean runDRCs)
	{
		Map<ILogicDesign, Collection<Model>> modDesigns = new HashMap<ILogicDesign, Collection<Model>>();
		for (IDesignContainer i : modDesigns1) {
			ILogicDesign design = (ILogicDesign) i;
			if (design != null) {
				addDesignToSave(design, getModel(design), savealways, modDesigns, null);
			}
		}

		if (!modDesigns.isEmpty()) {
			return doSave(proj, modDesigns, runDRCs, null);
		}
		else {
			// UXFWK-1356: Check to see if we could not save due to the invalid state of the CDH
			throwExceptionIfCreationDeletionHelperIsIncorrect();
			CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(
					ResourceMgr.getString(BaseLifecycle.class, "BaseLifecycle.diagram.NoChangesToSave.message"));
			return null;
		}
	}

	private static class SaveLockMonitor
	{

		private Set<IPrivilegedCOGManagedLockableChildrenContainer> m_saveLockedDesigns = new HashSet<>();

		public void saveLocked(@NotNull IPrivilegedCOGManagedLockableChildrenContainer design)
		{
			m_saveLockedDesigns.add(design);
		}

		public void notGoingToSave(@NotNull IPrivilegedCOGManagedLockableChildrenContainer design)
		{
			design.releaseSaveLock();
			m_saveLockedDesigns.remove(design);
		}

		public void saveAborted()
		{
			for (IPrivilegedCOGManagedLockableChildrenContainer saveLockedDesign : m_saveLockedDesigns) {
				saveLockedDesign.releaseSaveLock();
			}
			m_saveLockedDesigns.clear();
		}
	}

	@Nullable private ITask doSave(IProject proj, Map<ILogicDesign, Collection<Model>> modDesigns,
			boolean runDRCs,
			@Nullable Collection<ISchemDiagram> restrictDiagrams)
	{
		SaveLockMonitor saveLockMonitor = new SaveLockMonitor();
		try {
			return doSave(proj, modDesigns, runDRCs, restrictDiagrams, saveLockMonitor);
		}
		catch (Throwable e) {
			saveLockMonitor.saveAborted();
			throw new WrappingRuntimeException(e);
		}
	}

	@Nullable private ITask doSave(IProject proj, Map<ILogicDesign, Collection<Model>> modDesigns,
			boolean runDRCs,
			@Nullable Collection<ISchemDiagram> restrictDiagrams, SaveLockMonitor saveLockMonitor)
	{
		Map<ILogicDesign, Collection<IUID>> savesetDesigns =
				getDesignsToSave(proj, saveLockMonitor, modDesigns, restrictDiagrams, runDRCs);

		// Purge Unplaced Objects on Save - this seems a bit low level but we want to catch case other than the Save sction - don't we?
		for (IDesign des : savesetDesigns.keySet()) {
			Model model = getModel(((ILogicDesign) des));
			new PurgeUnplacedConnectivity().handleUnplacedObjects((ILogicDesign) des, model);
			if (des instanceof ILayoutLogicDesign) {
				HarnessProcessingUtils.removeOrphanedBOMIDs((ILayoutLogicDesign) des);
			}
		}

		Map<IUID, Collection<IUID>> saveset = new HashMap<IUID, Collection<IUID>>();
		for (IDesign des : savesetDesigns.keySet()) {
			saveset.put(des.getUID(), savesetDesigns.get(des));
		}
		// save without updating design mgr or folder mgr to reduce DB conflicts
		ITask saveTask = save(proj, saveset, false);

		// Adds autorecovery cleaner which will remove itself after activating
		ControllerSaveTaskAutorecoveryListener cleaner =
				new ControllerSaveTaskAutorecoveryListener(listener -> removeSaveCompleteListener(listener));
		addSaveCompleteListener(cleaner);
		if (restrictDiagrams != null) {
			LifeCycleCacheUtils.batchUpdateCacheObjects(restrictDiagrams
					.stream()
					.map(diagram -> (IBaseDiagram) diagram)
					.collect(Collectors.toList()));
		}
		for (IDesign design : savesetDesigns.keySet()) {
			Model model = getModel(((ILogicDesign) design));
			if (model != null) {
				model.setModified(false);
				//delete the XML dump of the design if it exists.
				cleaner.addDesignsToClear(model.getDesign());
				model.getController().getUndoableContainer().clear();
				List<IBaseDiagram> homeCacheDiagramList = new ArrayList<>();
				for (ISchemDiagram diagram : model.getDiagrams()) {
//						diagram.markModified(false);
					if (restrictDiagrams == null) {
						homeCacheDiagramList.add(diagram);
					}
					if (diagram instanceof IIncLoadable) { // but why wouldnt it be?
						((IIncLoadable) diagram).setSkeletonizable(true);
					}
				}
				LifeCycleCacheUtils.batchUpdateCacheObjects(homeCacheDiagramList);
			}
			// DR 405413: we can now longer treat the design as new.
			// (elsewhere this is done by saving via UpdateableHelper.flushNew())
			design.setNew(false);

			//
			// As it's now saved, we can skeletonize it...
			//
			if (design instanceof IIncLoadable) {
				((IIncLoadable) design).setSkeletonizable(true);
			}
		}

		if (runDRCs && savesetDesigns.size() < modDesigns.size()) {
			// Some designs are not saved due to DRC failures, so returns null
			return null;
		}
		return saveTask;
	}

	private void addDesignToSave(ILogicDesign design, Model model, boolean savealways,
			Map<ILogicDesign, Collection<Model>> modDesigns, @Nullable ILogicDesign restrictedDesign)
	{
		// DR 367482 - even for save always we must not save designs we haven't locked. Doing so could
		// overwrite edits made by another user.

		if ((savealways && IDesignLockStrategy.isLocked(design)) || model.isModified()) {
			//
			// If we are restricting the save to a particular design
			// then only add these to the list.
			//
			if (restrictedDesign != null && design != restrictedDesign) {
				return;
			}

			Collection<Model> designModels = modDesigns.get(design);
			if (designModels == null) {
				designModels = new ArrayList<Model>();
			}
			designModels.add(model);
			modDesigns.put(design, designModels);
		}
	}

	private void forceAddDesignToSave(IProject proj, @Nullable ILogicDesign logicDesign,
			Map<ILogicDesign, Collection<Model>> modDesigns)
	{
		if (logicDesign != null) {
			modDesigns.put(logicDesign, new ArrayList<Model>());
		}
		else {
			for (IUID designUID : proj.getDesignMgr().getLoadedDesigns()) {

					ILogicDesign loadedDesign = DesignUtils.getLoadedDesign(designUID, ILogicDesign.class);

			if (loadedDesign != null && IDesignLockStrategy.isLocked(loadedDesign)) {
					modDesigns.put(loadedDesign, new ArrayList<Model>());
				}
			}
		}
	}

	// Convert to the UID format required by the save
	private Map<ILogicDesign, Collection<IUID>> getDesignsToSave(IProject proj, SaveLockMonitor saveLockMonitor,
			Map<ILogicDesign, Collection<Model>> modDesigns,
			@Nullable Collection<ISchemDiagram> restrictDiagram, boolean runDRCs)
	{
		//
		// ModDesigns contains a hash of designs -> vector of modified models
		// ModDiagrams contains a hash of modified models -> diagrams
		//
		// Convert to the UID format required by the save - simplest to do it in
		// 2 stages.
		//

		Map<ILogicDesign, Collection<IUID>> savesetDesigns = new HashMap<ILogicDesign, Collection<IUID>>();
		Map<ILogicDesign, Collection<IUID>> savesetWithWarnings = new HashMap<ILogicDesign, Collection<IUID>>();
		Set<IPrivilegedCOGManagedLockableChildrenContainer> saveLockedDesigns = new HashSet<>();

		try {
			// TODO jacobt FEAT13040 : test this defect : DR 406981: don't save diagrams that have not been modified.

			Collection<IDRCViolation> drcViolations = new ArrayList<IDRCViolation>();
			boolean updateTable = false;
			IDRCResultCollector collector = new DRCResultCollector();

			// For each design we've been asked to save.
			for (ILogicDesign des : modDesigns.keySet()) {
				Model model = getModel(des);
				if (!des.isNew() && !des.isLocked() &&
						!((IPrivilegedCOGManagedLockableChildrenContainer) des).isWeakLocked()) {
					outputDesignNotLockedMessage(des);
					continue;
				}

				if (des instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
					final IPrivilegedCOGManagedLockableChildrenContainer lockableChildrenContainer =
							(IPrivilegedCOGManagedLockableChildrenContainer) des;
					if (lockableChildrenContainer.isWeakLocked()) {

						Long timeBeforeRefresh =
								des.getConnectivity() != null ? des.getConnectivity().getTimeModified() : null;
						if (!lockableChildrenContainer.saveLock()) {
							outputDesignNotSaveLockedMessage(des);
							continue;
						}

						saveLockMonitor.saveLocked(lockableChildrenContainer);
						saveLockedDesigns.add(lockableChildrenContainer);

						Long timeAfterRefresh = des.getConnectivity() != null ?
								des.getConnectivity().getTimeModified() : null;
						if (timeBeforeRefresh != null && timeAfterRefresh != null &&
								timeAfterRefresh > timeBeforeRefresh) {
							if (model != null) {
								model.notifyModelChange(new ModelChangeEvent(model, Collections.<IUID>emptyList()));
							}
						}
					}
				}

				//FEAT00013678 - Auto run DRC background save with visual feedback Changes
				// Runs DRCs configured as ONSAVE on objects in the design
				Collection<IDRCViolation> violationList = new ArrayList<IDRCViolation>();
				if (runDRCs) {
					if (!updateTable) {
						updateTable = DRCRunnerHelper.isDRCPresent(des, IDRCMgr.DRCRunningModeEnum.ONSAVE);
					}

					violationList =
							DRCRunnerHelper.runDRCsAndCollectViolations(des, IDRCMgr.DRCRunningModeEnum.ONSAVE,
									collector);
					drcViolations.addAll(violationList);
					DRCRunnerHelper.auditTrail(proj, des, violationList.size());
				}
				DRCIssueFailureMsgHandler msgHandler = new DRCIssueFailureMsgHandler();
				populateIssuesForDRCViolations(proj, collector, msgHandler);
				msgHandler.flushMessages();
				if (!runDRCs ||
						DRCRunnerHelper.getErrorStatus(violationList, proj) != IDRCMgr.ViolationStatusEnum.ERROR) {

					// the modified state for Logic diagrams is now stored on the COF object
					// just save any modified diagram regardless of which diagrams exist on the Model
					// TODO jacobt FEAT13040 : perhaps we don't need the diagrams on the Model after all?
					Collection<IUID> modDiagramList = new HashSet<IUID>();
					if (restrictDiagram != null) {
						//dts0101226292|CH] org.xml.sax.SAXParseException; No device found with id
						//we can't leave the decision to determine the diagrams to save upon the clients.
						//because the design is a consolidated entity for persistence including its
						//children connectiivty and diagrams. however, we are now going to change the meaning
						//of the passed set of diagrams. instead they will be treated as mandatory set only
						//but not restricted to only them.
						for (ISchemDiagram diagram : restrictDiagram) {
							if (diagram.isLocked()) {
								modDiagramList.add(diagram.getUID());
							}
						}
					}
					for (ICHSIterator<ISchemDiagram> it = des.getDiagrams(); it.hasNext(); ) {
						ISchemDiagram diagram = it.getNext();
						if (diagram.isModified() && diagram.isLocked()) {
							modDiagramList.add(diagram.getUID());
						}
					}

					// TODO jacobt FEAT13040 : unfortunately the modified flag on the diagram is not yet reliable
					// also save all diagrams in the Model - between the two it should be enough for most cases
					// I will aim to fix this properly in the near future

					if (model != null) {
						for (ISchemDiagram diagram : model.getDiagrams()) {
							if (diagram.isLocked()) {
								modDiagramList.add(diagram.getUID());
							}
						}
					}
					//UpdateComposite designs before save.defect dts0100669614
					updateDecorationsBeforeSave(modDiagramList);

					if (runDRCs &&
							DRCRunnerHelper.getErrorStatus(violationList, proj) ==
									IDRCMgr.ViolationStatusEnum.WARNING) {
						savesetWithWarnings.put(des, modDiagramList);
					}
					else { // If there are no DRC failures, then save the design
						// DR 445855 save the design even if none of its diagrams have been modified.
						//saveset.put(des.getUID(), modDiagramList);
						savesetDesigns.put(des, modDiagramList);
					}
				}
			}

			if (updateTable) {
				IDRCResultList resList = new DRCResultList();
				resList.addAll(drcViolations);
				DRCRunnerHelper.updateTable(resList);
			}

			if (!drcViolations.isEmpty()) { // If DRC failures, update the table in "Check" tab
				//Pops up the dialog with failure information and takes the user option
				if (DRCRunnerHelper.isSaveAllowed(drcViolations, null, proj)) {
					for (ILogicDesign design : savesetWithWarnings.keySet()) {
						savesetDesigns.put(design, savesetWithWarnings.get(design));
					}
				}
			}
		}
		finally {
			for (IPrivilegedCOGManagedLockableChildrenContainer saveLockedDesign : saveLockedDesigns) {
				if (!savesetDesigns.containsKey(saveLockedDesign)) {
					saveLockMonitor.notGoingToSave(saveLockedDesign);
				}
			}
		}
		return savesetDesigns;
	}

	protected void populateIssuesForDRCViolations(@NotNull IProject proj, @NotNull IDRCResultCollector collector,
			@NotNull DRCIssueFailureMsgHandler msgHandler)
	{
		new DRCIssueController(new DRCIssueProcessingParams(proj, collector, true, true, true),
				msgHandler).populateIssuesForDRCViolations();
	}

	protected void outputDesignNotLockedMessage(IDesign des)
	{
		String skipDesignSaveMsg = ResourceMgr.getString(BaseLifecycle.class,
				"BaseLifecycle.diagram.SavingUnlockedDesign.message", des.getName());
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(skipDesignSaveMsg);
		System.out.println(skipDesignSaveMsg);
		assert false : skipDesignSaveMsg;
	}

	protected void outputDesignNotSaveLockedMessage(IDesign des)
	{
		String skipDesignSaveMsg = ResourceMgr.getString(BaseLifecycle.class,
				"BaseLifecycle.diagram.SavingLockedOnDesignFailed.message", des.getName());
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(skipDesignSaveMsg);
		LogicConcurrencyController.getInstance().getCAFView().getConcurrentEditReporter().report(HTMLHelper.color(
				IColor.RED, skipDesignSaveMsg));
	}

	@Override
	public boolean foregroundSave(@NotNull final IDesignContainer design, boolean asNewDesign)
	{
		ILogicDesign logicDesign = CommonUtils.cast(design, ILogicDesign.class);
		if (logicDesign == null) {
			return false;
		}
		ValidationHelper.validateBeforeSave(logicDesign);
		PersistPayload payload = PersistPayload.createPayload();
		ProjectStorageHelper.saveLogicDesignRequest(payload, logicDesign, false);
		payload.close();
		boolean success = false;
		try {
			payload.sendRequest();
			success = true;
		}
		catch (UserSessionException e) {
			e.printStackTrace();
		}
		if (success) {
			LifeCycleCacheUtils.batchUpdateCacheObjects(logicDesign.getDiagrams()
					.stream()
					.map(diagram -> (IBaseDiagram) diagram)
					.collect(Collectors.toList()));
			setSkeletonizable(logicDesign);
			//after design is saved diagrams are also skeletonizable.
			for (ISchemDiagram diagram : logicDesign.getDiagrams()) {
				setSkeletonizable(diagram);
			}
		}
		return success;
	}

	private void setSkeletonizable(IUIDObject uidObject)
	{
		IIncLoadable incLoadable = CommonUtils.cast(uidObject, IIncLoadable.class);
		if (incLoadable != null) {
			incLoadable.setSkeletonizable(true);
		}
	}

	/**
	 * @param project         the containing project
	 * @param saveset         the save set is a map design UID --> collection of diagrams within the design
	 * @param createNewDesign save in a way to create a new design rather than save an existing one
	 * @return returns the save task
	 */
	@Nullable
	public ITask save(IProject project, Map<IUID, Collection<IUID>> saveset, final boolean createNewDesign)
	{
		// Now we know what to save, lets get to work
		// Save each design which has modified contents [and save only is
		// modified diagrams

		if (saveset.isEmpty()) {
			return null;
		}

		List<IDesignContainer> designContainers = new ArrayList<IDesignContainer>(saveset.keySet().size());
		UIDUtils.addUIDObjects(designContainers, saveset.keySet().iterator());
		validateBeforeSaveWithAutoRepair(designContainers);

		ISaveDesignParameters parameters = new ISaveDesignParameters()
		{
			@Override public boolean runValidations()
			{
				return false;
			}

			@Override public boolean createNewDesign()
			{
				return createNewDesign;
			}

			@Override public boolean createNewDiagram()
			{
				return isCreatingNewDiagram();
			}
		};
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		try {
			btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.NESTED);
			return saveDesign(getFIB(), project, saveset, this, parameters);
		}
		catch (Throwable e) {
			btm.exitTransactionBoundary(this, false);
			throw new WrappingRuntimeException(e);
		}
	}

	/**
	 * Save the changes to the specified Caplet Controller's model.
	 */
	@Override public void saveChanges(ICapletController controller)
	{
		IProject root = getFIB().getProjectMgr().getCurrentProject();

		// DR 445855: 'savealways' mode is for the save done when deleting a diagram, when we want to save designs
		// with no modified diagrams. It should not be used here.
		save(root, null, (ISchemDiagram) null, false, true);
	}

	public void discard(IUIDObject root)
	{
		if (root instanceof IProject) {
			IProject proj = (IProject) root;
			for (Model model : models.values()) {
				if (model.getDesign().getProject() == proj) {
					model.setModified(false);
				}
			}
		}
		else if (root instanceof ISchemDiagram) {
			ISchemDiagram diagram = (ISchemDiagram) root;
			Model model = getModel(diagram.getDesign());
			if (model != null) {
				model.setModified(false);
			}
		}
		else if (root instanceof ILogicDesign) {
			Model model = getModel(((ILogicDesign) root));
			if (model != null) {
				model.setModified(false);
			}
		}
	}

	public void projectChanged(ProjectChangeEvent e)
	{
		// To prevent potential current modification of m_models
		Set<Model> modelSet = new LinkedHashSet<Model>(models.values());

		// If a project was closed, see if one of our models was
		// in the project, and if so forget about it.
		ICAFProjectMgr projectMgr = getFIB().getProjectMgr();
		if (e.getChangeType() == ProjectChangeEvent.PROJECT_OPENED) {
			doProjectOpened(e);
			e.getProject().getDesignMgr().addAccessibilityChangeListener(this);
		}
		else if (e.getChangeType() == ProjectChangeEvent.PROJECT_CLOSED) {
			doProjectClosed(e, modelSet);
		}
		else if (e.getChangeType() == ProjectChangeEvent.PROJECT_CHILD_CLOSED) {
			doProjectChildClosed(e, projectMgr);
		}
		else if (e.getChangeType() == ProjectChangeEvent.PROJECT_EDITED) {
			doProjectEdited(e, modelSet);
		}
		performIntegrationPersistenceChecks("shared.BaseLifecycle.projectChanged", e);
	}

	protected void batchRefreshDesigns(@NotNull Set<ILogicDesign> designsToRefresh)
	{
		RefreshHelper.batchRefreshDesigns(designsToRefresh);
	}

	private void doProjectEdited(ProjectChangeEvent event, Set<Model> modelSet)
	{
		// If a model is readonly, it might have been changed under us.
		IProject project = event.getProject();
		Collection<ILogicDesign> designsNeedRefresh =
				new LinkedHashSet<ILogicDesign>(5);// For refreshing deleted read-only diagram case.
		Collection<Model> modelsNeedNotify = new LinkedHashSet<Model>(5);// For models need to send notifications.
		// For read-only design (refresh DB changes)
		for (Model model : modelSet) {
			if (model.getDesign().getProject() == project) {

				if (!model.isEditable()) {
					// For diagram that was deleted from db.

					// TODO jacobt FEAT13040 : do we still need this ugliness?
					// @TODO - This is ugly. In LogicDesign.refresh(...) it ALWAYS reload the diagrams for read-only
					// design. It effectively makes mmd.m_diagram out-dated.
					for (ICHSIterator<ISchemDiagram> ditr = model.getDesign().getDiagrams(); ditr.hasNext(); ) {
						ISchemDiagram diagram = ditr.getNext();
						if (model.containsDiagram(diagram)) {
							if (diagram.isDeleted()) {
								messageAndClearDiagram(model.getDesign(), model, diagram);
								designsNeedRefresh.add(model.getDesign());
							}
							else {
								modelsNeedNotify.add(model);
							}
						}
					}
				}
			}
		}

		// If designs need refresh, then this method will be invoked again, so there is no need to send model
		// change notifications right away.
		if (!designsNeedRefresh.isEmpty()) {
			batchRefreshDesigns(new HashSet<>(designsNeedRefresh));
			CAFUtils.getInstance().getCAFProjectMgr().projectChanged(project);
		}
		else {
			//@TODO - We didn't bother to check if a Model is modified or not. This could be very inefficient,
			// as the listeners will respond repeatedly.
			for (Model model : modelsNeedNotify) {

				boolean modified = model.isModified(); // should always be
				// false !??
				Collection<IUID> emptyList = Collections.emptyList();
				ModelChangeEvent mce = new ModelChangeEvent(model, emptyList);
				model.notifyModelChange(mce);
				model.setModified(modified); // need to reset after model
				// change event
			}
		}

		List<?> editRoot = event.getEditRoot();
		if (editRoot != null && editRoot.size() == 2) {
			Object editedObject = editRoot.get(1);
			if (editedObject != null && editedObject instanceof IProjectAnalysisMgr) {
				IProjectAnalysisMgr analysisMgr = (IProjectAnalysisMgr) editedObject;
				AnalysisBrowserPanel.removeFunctionListCache(analysisMgr.getSubsystemId());
				ICapletController activeController = CAFUtils.getInstance().getActiveCapletController();
				if (activeController instanceof BaseController) {
					AnalysisBrowserPanel analysisBrowserPanel =
							((BaseController) CAFUtils.getInstance().getActiveCapletController())
									.getAnalysisBrowserPanel();
					if (analysisBrowserPanel != null) {
						analysisBrowserPanel.updateFunctionList(true);
					}
				}
			}
		}
	}

	private void doProjectChildClosed(ProjectChangeEvent e, ICAFProjectMgr projectMgr)
	{
		//
		// A design was closed.
		//
		//	CAFUtils.getInstance().getOutputWindow()
		//			.sendDebugMessage("Sravanthi Design has been closed", true);
		List<?> editRoot = e.getEditRoot();
		if (!editRoot.isEmpty()) {
			Object o = DesignDescriptorUtils.toDesignContainerIfDesignDescriptor(editRoot.get(editRoot.size() - 1));
			if (o instanceof ILogicDesign) {
				IDesign d = (IDesign) o;
				if (IDesignLockStrategy.isLocked(d)) {
//                    relinquishLock(d);
					projectMgr.relinquishLock(d);
				}
			}
		}
	}

	public void relinquishLockWithSaveTaskSanity(@Nullable IProject project, @Nullable IUIDObject designOrDiagram)
	{
		if (project != null && designOrDiagram != null) {
			ensureTaskExecutedOnlyAfterSaveCompletes(designOrDiagram,
					new ReleaseLockFromDesignOnSaveCompleteTask(project, designOrDiagram));
		}
	}

	private void doProjectClosed(ProjectChangeEvent e, Set<Model> modelSet)
	{
		// This event occurs when the project is closed, including the implicit close performed when the
		// project is deleted.

		IProject project = e.getProject();
		if (project != null) {
			for (Model model : modelSet) {
				if (model.getDesign().getProject() == project) {
					// Forget about this model since its project has closed
					destroyModel(model);
				}
			}
			// Unlock any locked designs. We are doing this independently of the models because if
			// all diagrams that were opened have been deleted from the design, there will be no models.
			// dts0100490023 - changed to call unlockAllDesigns, since the project may have been deleted & unloaded
			IDesignMgr designMgr = project.getLoadedDesignMgr();
			if (designMgr != null) {
				designMgr.unlockAllDesigns();
			}

			// Remove a filter control shared by logic diagrams in the project. This will only exist if a logic
			// diagram in the project was created/opened in the current session.
			LogicFilterControlMgr.getInstance().deleteFilterControl(project);

			// If the project is closing let the Analysis Browser Panel clear its cache of project models
			// that are in use. (We can't use a listener directly on the browser panel as it may be removed
			// if the controller is closed for the design upon which it is registered).
			// Make sure we don't force an attempt to load the the analysys mgr; if not loaded then nothing to do
			IProjectAnalysisMgr analysisMgr = project.getLoadedAnalysisMgr();
			if (analysisMgr != null) {
				AnalysisBrowserPanel.removeFunctionListCache(analysisMgr.getSubsystemId());
			}

			//delete the XML dump from recovery
			CAFUtils.getInstance().getAutoRecovery().discardProjectRecoveryInfo(project);
		}
	}

	private void doProjectOpened(ProjectChangeEvent event)
	{
		IProject project = event.getProject();
		project.setCrossReferenceMonitor(new CrossReferenceMonitor(project));
		project.getPreferences().addPreferenceChangeListener(this);
	}

	public void validateDesigns(DeleteDesignHelper deleteDesignHelper)
	{

		m_deleteDesignHelper = deleteDesignHelper;
		m_deleteDesignHelper.setCollectWeakLockDetails(true);
		List<ILogicDesign> designList =
				LifecycleUtils.getContextObjects(
						m_deleteDesignHelper.getDesignContainers(DesignUtils.getDesignType(getLogicDesignClass())),
						ILogicDesign.class);
		m_deleteDesignHelper.validateDesignsAndLock(designList,
				RefreshableLogicDesignFilter::createRefreshableDesignFilter);

		List<ISchemDiagram> diagramList = LifecycleUtils.getContextObjects(
				m_deleteDesignHelper.getDiagramsFromDeleteHelper(), ISchemDiagram.class);
		m_deleteDesignHelper.validateInUseDiagrams(diagramList);
	}

	/**
	 * Get the model for this Logic design
	 *
	 * @param design The design
	 * @return The Model or null if there is none
	 */
	@Nullable
	public Model getModel(@Nullable ILogicDesign design)
	{
		if (design == null) {
			return null;
		}
		return models.get(design.getUID());
	}

	public boolean delete(DeleteDesignHelper deleteDesignHelper)
	{
		m_deleteDesignHelper = deleteDesignHelper;

		if (!checkEditDesignPermission()) {
			m_deleteDesignHelper.collectErrors(ResourceMgr.getString(BaseLifecycle.class,
					"Lifecycle.CannotDelete.Message.text"));
			return false;
		}

		List<? extends ISchemDiagram> diagramList = LifecycleUtils
				.getContextObjects(m_deleteDesignHelper.getDiagramsFromDeleteHelper(), getLogicDiagramClass());
		if ((!diagramList.isEmpty())) {
			AbstractDeleteDiagramsDelegate deleteDiagramsDelegate =
					getDeleteDiagramsDelegate(m_deleteDesignHelper.isDiagramOnlyDeletionMode());
			if (!deleteDiagramsDelegate.deleteDiagrams(m_deleteDesignHelper, diagramList)) {
				return false;
			}
		}

		List<? extends ILogicDesign> designList = LifecycleUtils.getContextObjects(
				m_deleteDesignHelper.getDesignContainers(DesignUtils.getDesignType(getLogicDesignClass())),
				getLogicDesignClass());
		if (designList.isEmpty()) {
			return true;
		}

		DeleteDesignsDelegate deleteDesignsDelegate = new DeleteDesignsDelegate(this, getResourceClass());
		return deleteDesignsDelegate.deleteDesigns(m_deleteDesignHelper, designList);
	}

	public boolean deleteDiagramsOnly(@NotNull IProject project, @NotNull List<ISchemDiagram> diagramList,
			@NotNull IDesign design)
	{
		DeleteDiagramsOnlyDelegate deleteDiagramsDelegate =
				(DeleteDiagramsOnlyDelegate) getDeleteDiagramsDelegate(true);
		return deleteDiagramsDelegate.deleteDiagramsOnly(project, diagramList, (ILogicDesign) design);
	}

	protected void clearPasteBuffer(@Nullable Model model)
	{
		if (model != null) {
			ICapletController capletController = model.getController();
			if (capletController != null) {
				IDataTransfer dataTransfer = capletController.getDataTransfer();
				Objects.requireNonNull(dataTransfer).clearPasteBuffer();
			}
		}
	}

	/**
	 * Process edit().
	 *
	 * @param context, the context (project/design/diagram).
	 * @return boolean, 'true' if the edit was successful.
	 */
	public boolean edit(List<?> context)
	{
		if (getData() == null) {
			return false;
		}

		boolean result;/*= false*/
		// CARCH-1291 - changed the creation deletion helper guard
		try (IGuard ignored = CreationDeletionHelper.createSuspendUndoableChangesGuard()) {
			IProject project = LifecycleUtils.getContextObject(context, IProject.class);
			ILogicDesign design = (ILogicDesign) getContextDesignContainer(context, IDesign.class);

			if (project == null || design == null) {
				throw new IllegalArgumentException("Wrong context for edit()");
			}

			if (projectDeleted(project) || !checkDesignExists(project, design)) {
				return false;
			}

			ISchemDiagram diagram = LifecycleUtils.getContextObject(context, ISchemDiagram.class);

			boolean isDiagramEdited = false;
			if (diagram == null) {
				EditDesignDelegate lifeCycleDelegate = getEditDesignDelegate();
				result = lifeCycleDelegate.editDesign(project, design, context);
			}
			else {
				EditDiagramDelegate lifeCycleDelegate = getEditDiagramDelegate();
				isDiagramEdited = lifeCycleDelegate.editDiagram(project, diagram, context);
				result = isDiagramEdited;
			}
			if (result && !isDiagramEdited) {
				DesignAuditTrailInfo info = new DesignAuditTrailInfo(design);
				AuditTrailLogHelper.postAuditTrail(AuditableEventType.DESIGN_ATTRIBUTES_CHANGED, null,
						project.getUID().getString(),
						info,
						design.getUID().getString());
				LifeCycleCacheUtils.updateCacheObject(design);
			}
			if (isDiagramEdited) {
				LifeCycleCacheUtils.updateDiagramNameInCache(diagram);
			}
		}
		return result;
	}

	@Nullable protected IUserSession getData()
	{
		return CAFUtils.getInstance().getData();
	}

	/**
	 * Process copy().
	 *
	 * @param context, the context (project/design/diagram).
	 * @return IUIDObject, the design copy, if successfully created, else null.
	 */
	public IUIDObject copy(List<?> context)
	{
		if (getData() == null) {
			return null;
		}
		final ILogicDesign logicDesign = getContextDesignContainer(context, ILogicDesign.class);
		if (checkifProjectisNull(context) || logicDesign == null) {
			throw new IllegalArgumentException("Wrong context for copy().");
		}
		final IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		assert project != null;
		if (projectDeleted(context, getResourceClass()) || !checkDesignExists(project, logicDesign)) {
			return null;
		}
		String title = ResourceMgr.getString(DesignInfoDialog.class, "DesignInfoDialog.copyDialog.title.text");
		return copyDesign(project, logicDesign, title, context);
	}

	@Nullable protected IDesignContainer copyDesignSpecific(
			IDesignCopyInfo dialog, IDesignContainer srcDesign, IProject project, List<?> context,
			IFolder parentFolder)
			throws DesignManipulationException
	{
		// Do some basic validity checks.
		if (dialog == null || project == null || srcDesign == null) {
			return null;
		}
		IDesignMgr designMgr = project.getDesignMgr();
		IFolderMgr folderMgr = project.getFolderMgr();
		if (folderMgr == null) {
			return null;
		}
		// Ensure that the source design is fully loaded. Messing with the folder manager may have unloaded it.
		IUID theDesignId = srcDesign.getUID();
		ILogicDesign sourceDesign = designMgr.getAbstractLogicDesign(theDesignId);
		if (sourceDesign == null) {
			return null;
		}

		boolean lockError = false;
		ICHSSystem chsSystem = UtilsHelper.getCHSSystem();
		IUserSession userSession = chsSystem.getUserSession();
		IDesignContainer tgtDesign = null; // The design copy that we are about to create.

		try {
			if (designMgr.lock()) {
				// We need to utilise the design manipulation service to copy the design.
				DesignManipulation dm = createDesignManipulation(sourceDesign, userSession);
				if (dialog instanceof DesignEditDialog) {
					DesignCopyTeamPlayData teamPlayData = ((DesignEditDialog) dialog).getTeamPlayData();
					dm.setTeamPlayData(teamPlayData);
				}
				// Get the shared objects that we will need.
				//tool copy needed?
				project.getSharedPinListMgr().ensureFullyLoadedAndUpToDate(
						SharedPinListFilters.getAllForDesign(sourceDesign));

				// COG =>
				// (1) we need to lock the getSharedPinListMgr() before we save it,
				// this will also to a refresh to ensure all the shared objects are up-to-date/
				// (2) this code relies on scrub done in the dm.copyDesign(...) method call
				// to add the new pin reservations for the new design. This edit of the shared objects
				// is now done on refreshed objects within a lock
				// COG-PSM Fix: dts0100874258 - added lock check with dialog support.
				if (new LockUpdateHelper((ICOGLockable) project.getSharedPinListMgr(), true).lockAndRefresh()) {
					tgtDesign =
							doCopyDesign(project, designMgr, folderMgr, dialog, sourceDesign, chsSystem,
									userSession,
									dm);
					if (tgtDesign != null) {
						DesignManipulation.copyCustomData(srcDesign, tgtDesign);
						CopyDesignHelper
								.moveCopiedDesignToFolderSpecifiedIfAnyOnDialog(tgtDesign, folderMgr, parentFolder,
										true);
					}
					if (tgtDesign == null) {
						// We got a lock error.
						// Already logged the error
						lockError = true;
					}
				}
			}
			else {
				// We got a lock error.
				System.err.println("Could not lock the DesignMgr");
				lockError = true;
			}
		}
		catch (UserSessionException use) {
			// We need to roll the transaction back.
			userSession.rollbackClientTransaction();
			use.printStackTrace();
		}
		catch (FolderMgrEditException fmee) {
			throw new WrappingRuntimeException(fmee);
		}
		catch (SecurityException securEx) {
			// TODO JEAN-MARC : WHY DO WE CATCH THIS?
			// DO We need to roll the transaction back ???
			//userSession.rollbackClientTransaction();
			securEx.printStackTrace();
		}
		catch (PersistenceLockFailureCheckedException e) {
			// TODO FEAT14949 : report a better lock failure now that we have more info in the exception - sticking with existing functionality for now

			lockError = true;
		}
		finally {
			// Ensure that we unlock the folder manager and the design manager.
			project.getSharedPinListMgr().unlock();  //dts0100756644
			folderMgr.unlock();
			designMgr.unlock();
			if (dialog.getEffectivityUpdateData() != null) {
				EffectivityHelper.unlockObjects(dialog.getEffectivityUpdateData().getLockedObjects());
			}
		}
		if (lockError) {
			throw new DesignManipulationException("Lock error while copying design", true);
		}
		return tgtDesign;
	}

	@NotNull protected DesignManipulation createDesignManipulation(ILogicDesign sourceDesign, IUserSession
			userSession)
	{
		return new DesignManipulation(sourceDesign, userSession);
	}

	@Nullable protected IDesignContainer doCopyDesign(IProject project, IDesignMgr designMgr, IFolderMgr folderMgr,
			IDesignCopyInfo dialog, ILogicDesign sourceDesign, ICHSSystem chsSystem, IUserSession userSession,
			DesignManipulation dm)
			throws PersistenceLockFailureCheckedException, FolderMgrEditException, UserSessionException
	{
		IOptionExpression optionExpression = dialog.getOptionExpression();
		// It must be a Logic design.
		String name = dialog.getDesignName();
		DesignImporter importer = dm.copyDesign(name, dialog.getRetainNamesDuringCopy(), sourceDesign.getObjType());
		// We must create a Revision node and add it to the parent.
		IDesign tgtDesign = importer != null ? importer.getDesign() : null;
		if (tgtDesign != null) {
			PropertyEditor.editProperties(tgtDesign, dialog.getProperties());
			tgtDesign.setRevision(dialog.getRevision());
			tgtDesign.setName(name);
			tgtDesign.setDescription(dialog.getDescription());
			tgtDesign.setShortDescription(dialog.getShortDescription());

			if (dialog instanceof DesignInfoDialog) {
				propagateLocationAndFunction((DesignInfoDialog) dialog, sourceDesign, tgtDesign);
			}
			tgtDesign.setReleaseLevel(dialog.getReleaseLevel());
			tgtDesign.setDesignAbstraction(dialog.getDesignAbstraction());
			tgtDesign.setDomain(dialog.getDomain());
			tgtDesign.setContainer(designMgr);
			tgtDesign.setCreatedBy(UtilsHelper.getNameOfCurrentUser());
			tgtDesign.setOptionExpression(optionExpression);

			// IESCD-5910 fix missing ports on copy action
			ILogicDesign targetLogicDesign = CommonUtils.cast(tgtDesign, ILogicDesign.class);
			if (targetLogicDesign != null) {
				DesignHelper.fixMissingPorts(targetLogicDesign);
			}
			designMgr.addDesign(tgtDesign);
			ApplicableOptionsHelper.removeUnusedApplicableOptions(tgtDesign, dialog
							.getChosenApplicableOptions().iterator(),
					dialog.getUnchosenApplicableOptions().iterator());
			// CFE-ROM: designs now automatically added to folderMgr when added to designMgr
			// folderMgr.addDesign(folderMgr, (IDesignDescriptor) tgtDesign);
			DesignHelper.copyScopeCodes(tgtDesign, dialog.getApplicableScopes());

			// dts0100925014 fix
			CopyDesignHelper.processEffectivityDesignCopy(project, tgtDesign, dialog);
			// Begin transaction here.
			userSession.startClientTransaction();
			tgtDesign.flushNew(designMgr.getObjType(), designMgr);
			designMgr.flush();
			//dts0100592996 DATA CORRUPTION Copy/revision design does not add shared pin reservations
			project.getSharedPinListMgr().flush();
			folderMgr.flush();
			// End transaction here.

			// validate the objects before save
			ValidationHelper.shallowValidateBeforeSave(designMgr);
			ValidationHelper
					.validateBeforeSave(tgtDesign, project.getSharedPinListMgr(), folderMgr);
			tgtDesign.flushNew(designMgr.getObjType(), designMgr);
			designMgr.flush();
			//dts0100592996 DATA CORRUPTION Copy/revision design does not add shared pin reservations
			project.getSharedPinListMgr().flush();
			folderMgr.flush();
			importer.saveCollaborationData();
			// End transaction here.
			userSession.commitClientTransaction();
			DesignAuditTrailInfo info = new DesignAuditTrailInfo(sourceDesign);
			AuditTrailLogHelper.postAuditTrail(AuditableEventType.DESIGN_COPIED, null, project.getUID().getString(),
					info,
					sourceDesign.getUID().getString());
			DesignAuditTrailInfo trailInfo = new DesignAuditTrailInfo(tgtDesign);
			AuditTrailLogHelper.postAuditTrail(AuditableEventType.DESIGN_CREATED, null, project.getUID().getString(),
					trailInfo,
					tgtDesign.getUID().getString());
		}
		return tgtDesign;
	}

	/**
	 * Process createRevision().
	 *
	 * @param context, the context (project/design/diagram).
	 * @return IUIDObject, the design revision, if successfully revisioned, else null.
	 */
	@Nullable
	public IUIDObject createRevision(List<?> context)
	{
		if (getData() == null) {
			return null;
		}
		final ILogicDesign logicDesign = getContextDesignContainer(context, ILogicDesign.class);
		if (checkifProjectisNull(context) || logicDesign == null) {
			throw new IllegalArgumentException("Wrong context for copy().");
		}
		final IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		assert project != null;
		if (projectDeleted(context, getResourceClass()) || !checkDesignExists(project, logicDesign)) {
			return null;
		}

		return createRevisionOfDesign(project, logicDesign, context);
	}

	public boolean acquireExclusiveLock(List<?> context)
	{
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		if (design == null) {
			return false;
		}
		final LogicDesignLockConverter instance = LogicDesignLockConverter.getInstance();
		instance.setStatusReporter(getStatusReporter());
		return instance.convertWeakLockToFullLock(design);
	}

	public boolean releaseExclusiveLock(List<?> context)
	{
		ILogicDesign design = getContextDesignContainer(context, ILogicDesign.class);
		if (design == null) {
			return false;
		}

		final LogicDesignLockConverter instance = LogicDesignLockConverter.getInstance();
		instance.setStatusReporter(getStatusReporter());
		return instance.convertFullLockToWeakLock(design);
	}

	public boolean viewRelatedBlocks(List<?> context)
	{
		if (getData() == null) {
			return false;
		}
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		ILogicDesign design = null;
		if (project != null) {
			Pair<IUID, DesignType> baseIDTypePair = LogicUtils.getActiveBlockAssignmentBaseID(context);
			if (baseIDTypePair != null) {
				IDesignDescriptor descriptor = BlockAssociationDesignProvider.getInstance()
						.getBlockAssignedDesign(project, baseIDTypePair.getFirst(), baseIDTypePair.getSecond());
				design = descriptor == null ? null :
						CommonUtils.cast(descriptor.getDesignContainer(), ILogicDesign.class);
			}
		}
		if (design != null) {
			Set<DesignBlockUsageInfo> usages =
					CapitalProjectDataServices.getDataServices().getDesignBlockUsages(design);
			Set<DesignBlockUsageInfo> results = new HashSet<DesignBlockUsageInfo>(usages);
			IBuildList activeBL =
					LogicUtils.getApplicableActiveBuildListForBlockAssociation(project, design.getDesignType());
			if (activeBL != null) {
				for (DesignBlockUsageInfo info : usages) {
					if (!activeBL.containsDesignUID(info.getDesignID())) {
						results.remove(info);
					}
				}
			}
			final DesignBlockHyperlinkProducer hyperlinkProducer =
					new DesignBlockHyperlinkProducer(design, results);
			CrossLinkHelper crossLinkHelper = new CrossLinkHelper(design.getProject(), hyperlinkProducer)
			{
				@Override public String getViewRelatedDialogTitle()
				{
					return hyperlinkProducer.getViewRelatedDialogTitle();
				}

				@Override public Frame getViewRelatedDialogParent()
				{
					return hyperlinkProducer.getParentDialogFrame();
				}

				@Override public String getConfirmDialogTitle()
				{
					return ResourceMgr.getString(CrossLinkActionUI.class, "CrossLinkActionUI.name");
				}
			};
			IActionEnum status = crossLinkHelper.onActivate();
			crossLinkHelper.onTerminate(status != IActionEnum.eCanceled);
		}
		return true;
	}

	// TODO ##MG## MOVE THIS METHOD TO DesignManipulation? THIS IS A MORE GENERAL FORM OF THE CODE USED BY CPROJECT.
	// TODO ##MG## IT SHOULD WORK FOR ANY TYPE OF DESIGN, NOT JUST FOR LOGIC DESIGNS, SO CPROJECT COULD USE IT TOO,
	// TODO ##MG## INSTEAD OF DesignSupportThread.onCreateLogicRevision().

	/**
	 * This method creates a revision of the specified logic design.
	 *
	 * @param dialog,    the design info dialog that specifies all the details for the new design revision.
	 * @param project,   the project that contains the source design (and will contain the target revision).
	 * @param srcDesign, the design to be revisioned.
	 * @return IDesignContainer, the newly created (target) revision of the design.
	 * @throws DesignManipulationException, when there are problems.
	 */
	@Nullable
	public IDesignContainer createRevisionOfAnyDesign(
			IDesignCopyInfo dialog, IProject project, IDesignContainer srcDesign, List<?> context)
			throws DesignManipulationException
	{
		// Do some basic validity checks.
		if (dialog == null || project == null || srcDesign == null) {
			return null;
		}
		IDesignMgr designMgr = project.getDesignMgr();
		IFolderMgr folderMgr = project.getFolderMgr();
		if (folderMgr == null) {
			return null;
		}
		// Ensure that the source design is fully loaded. Messing with the folder manager may have unloaded it.
		IUID theDesignId = srcDesign.getUID();
		ILogicDesign sourceDesign = designMgr.getAbstractLogicDesign(theDesignId);
		if (sourceDesign == null) {
			return null;
		}

		// Get the design details from the values specified in the dialog.
		boolean lockError = false;
		ICHSSystem chsSystem = UtilsHelper.getCHSSystem();
		IUserSession userSession = chsSystem.getUserSession();
		EffectivityModelUpdater.EffectivityModelUpdateData effectivityUpdateData = null;
		IDesignContainer tgtDesign = null; // The design revision that we are about to create.

		try {
			if (designMgr.lock()) {
				// We need to utilise the design manipulation service to create the design revision.
				DesignManipulation dm = new DesignManipulation(sourceDesign, userSession);
				if (dialog instanceof DesignEditDialog) {
					DesignCopyTeamPlayData teamPlayData = ((DesignEditDialog) dialog).getTeamPlayData();
					dm.setTeamPlayData(teamPlayData);
				}
				IAdditionalDesignUIPropsContext uiPropsContext = getUIPropsContext(dialog);
				// Get the shared objects that we will need.
				// tool revise needed?
				project.getSharedPinListMgr()
						.ensureFullyLoadedAndUpToDate(SharedPinListFilters.getAllForDesign(sourceDesign));
				IOptionExpression optionExpression = dialog.getOptionExpression();
				// Default the new revision to the value from the dialog.
				// It must be a Logic design.
				String revision = dialog.getRevision();
				tgtDesign = DesignRevisionHelper.createDesignRevision(revision, dm);
				if (tgtDesign != null) {
					List<String> propsToBeDeleted = DesignRevisionHelper.getPropsToBeDeletedOnRevise(tgtDesign);
					if (uiPropsContext != null) {
						// RDD Revise Flow
						propsToBeDeleted.add("PDM_EXTERNAL_ID");
					}
					DesignRevisionHelper.removePDMProps(tgtDesign, propsToBeDeleted);
					PropertyEditor.editProperties(tgtDesign, dialog.getProperties());
					tgtDesign.setRevision(revision);
					tgtDesign.setName(dialog.getDesignName());
					tgtDesign.setDescription(dialog.getDescription());
					tgtDesign.setShortDescription(dialog.getShortDescription());

					if (dialog instanceof DesignInfoDialog) {
						propagateLocationAndFunction((DesignInfoDialog) dialog, sourceDesign, tgtDesign);
					}

					tgtDesign.setReleaseLevel(dialog.getReleaseLevel());
					tgtDesign.setDesignAbstraction(dialog.getDesignAbstraction());
					tgtDesign.setDomain(dialog.getDomain());
					tgtDesign.setContainer(designMgr);
					DesignHelper.copyScopeCodes(tgtDesign, dialog.getApplicableScopes());
					tgtDesign.setOptionExpression(optionExpression);
					updateDesignAuthorNameIfAllowed(tgtDesign);
					final Set<IOption> chosenApplicableOptions = dialog.getChosenApplicableOptions();
					if (!(chosenApplicableOptions == null || chosenApplicableOptions.isEmpty())) {
						final IOptionIterator optionIterator =
								FactoryMgr.getProjectFactory().createOptionIterator(chosenApplicableOptions);
						DesignHelper.setoptions(tgtDesign, optionIterator);
					}
					tgtDesign.flushNew(designMgr.getObjType(), designMgr);
					designMgr.addDesign(tgtDesign, false);

					project.refreshDesignListAndFolderMgr();
					effectivityUpdateData = dialog.getEffectivityUpdateData();
					EffectivityHelper.doEffectivityChanges(project.getEffectivityMgr(), tgtDesign,
							effectivityUpdateData);

					//Don't do it always
					if (Environment.isMemoryIntensiveMode()) {
						((IIncLoadable) tgtDesign).setSkeleton(true);
					}

					// CFE-ROM: designs now automatically added to folderMgr when added to designMgr (false)
					// Create the revision node, but don't notify the listeners via folder mgr.
//					folderMgr.addDesign(sourceDesign.getUID(), (IDesignDescriptor) tgtDesign, false);

					// Begin transaction here.
					userSession.startClientTransaction();
					folderMgr.flush();
					designMgr.flush();
					// End transaction here.
					dm.saveTeamPlayData();
					userSession.commitClientTransaction();
					DesignAuditTrailInfo info = new DesignAuditTrailInfo(tgtDesign);
					IBooleanProperty teamcenterCheckBox = null;
					if (dialog instanceof DesignInfoDialog) {
						teamcenterCheckBox = ((DesignInfoDialog) dialog).getTeamcenterCheckBox();
					}
					String auditEventDescription = getAuditEventDescription();
					if (teamcenterCheckBox != null && teamcenterCheckBox.getValue()) {
						auditEventDescription = ResourceMgr.getString(DesignInfoDialog.class,
								"DesignInfoDialog.AuditEvent.Description.NewDesignWithTeamcenter");
					}
					AuditTrailLogHelper.postAuditTrail(AuditableEventType.DESIGN_CREATED, auditEventDescription, project.getUID().getString(),
							info,
							tgtDesign.getUID().getString());
					DesignManipulation.copyCustomData(srcDesign, tgtDesign);
				}
			}
			else {
				// We got a lock error.
				System.err.println("DesignMgr could not be locked.");
				lockError = true;
			}
		}
		catch (UserSessionException use) {
			// We need to roll the transaction back.
			userSession.rollbackClientTransaction();
			use.printStackTrace();
		}
		catch (SecurityException securEx) {
			// TODO JEAN-MARC : WHY DO WE CATCH THIS?
			// DO We need to roll the transaction back ???
			//userSession.rollbackClientTransaction();
			securEx.printStackTrace();
		}
		catch (PersistenceLockFailureCheckedException e) {
			// TODO FEAT14949 : We should make use of the details in the PersistenceLockFailureCheckedException
			lockError = true;
		}
		finally {
			// Ensure that we unlock the folder manager and the design manager.
			folderMgr.unlock();
			designMgr.unlock();
			if (effectivityUpdateData != null) {
				EffectivityHelper.unlockObjects(effectivityUpdateData.getLockedObjects());
			}
		}
		if (lockError) {
			throw new DesignManipulationException("Lock error while revisioning design.", true);
		}
		return tgtDesign;
	}

	private void updateDesignAuthorNameIfAllowed(@NotNull IDesignContainer design)
	{
		ISystemPreferenceMgr systemPrefMgr = SystemPreferencesHelper.getSystemPreferences();

		if(systemPrefMgr != null && !systemPrefMgr.getShouldDesignAuthorNameBeDerivedFromParentDesign()) {
			design.setCreatedBy(UtilsHelper.getNameOfCurrentUser());
		}
	}

	@Nullable private IAdditionalDesignUIPropsContext getUIPropsContext(IDesignCopyInfo dialog)
	{
		if (dialog instanceof DesignInfoDialog) {
			return ((DesignInfoDialog) dialog).getAdditionalDesignUIPropsContext();
		}
		return null;
	}

	protected void propagateLocationAndFunction(DesignInfoDialog dialog, ILogicDesign sourceDesign,
			IDesignContainer tgtDesign)
	{
		final DesignType designType = tgtDesign.getDesignType();
		if (DesignType.isLocationAttributeEnabled(designType)) {
			String newLocationValue = dialog.getLocationValue();
			String oldLocationValue = (sourceDesign).getIECLocation();
			((IIECNamingObject) tgtDesign).setIECLocation(newLocationValue);
			((IIECNamingObjectContainer) tgtDesign)
					.propagateAttributeChange(IAttributeTypes.IEC_LOCATION,
							StringUtils.ensureNotNull(oldLocationValue), newLocationValue);
		}
		if (DesignType.isFunctionAttributeEnabled(designType)) {
			String newFunctionValue = dialog.getFunctionValue();
			String oldFunctionValue = (sourceDesign).getIECFunction();
			((IIECNamingObject) tgtDesign).setIECFunction(newFunctionValue);
			((IIECNamingObjectContainer) tgtDesign)
					.propagateAttributeChange(IAttributeTypes.IEC_FUNCTION,
							StringUtils.ensureNotNull(oldFunctionValue), newFunctionValue);
		}
	}

	/**
	 * Try lock design if lockable; otherwise give user msg if necessary and decide whether the subsequent action should
	 * continue.
	 *
	 * @param srcDesign  the design to lock
	 * @param actionType one of COPY_DESIGN, CREATE_REVISION
	 * @return true, if processed ok and the action should continue. It doesn't necessarily mean the design must have
	 * been locked.
	 */
	@Override protected boolean processLocking(IDesignContainer srcDesign, @NotNull ActionType actionType)
	{
		// Lock the design.
		assert (!srcDesign.isLocked());
		IProject project = srcDesign.getProject();

		assert project != null;
		boolean isMultiUserMode = CapletLifecycleHelper.projectHasTopoOpenInMultiUser(project);
		if (!isMultiUserMode) {
			return processDesignLockingForCopy(srcDesign, actionType);
		}
		if (actionType == ActionType.COPY_DESIGN) {
			getStatusReporter().showInformationMessage(m_mainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotCopy"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotCopy"));
		}
		else if (actionType == ActionType.CREATE_REVISION) {
			getStatusReporter().showInformationMessage(m_mainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotRevision"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotRevision"));
		}
		else if (actionType == ActionType.CREATE_DETAILEDDESIGN) {
			getStatusReporter().showInformationMessage(m_mainFrame,
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msghdr.cannotCreateDetailedDesign"),
					ResourceMgr.getString(DesignCapletLifecycleHelper.class,
							"DesignCapletLifecycleHelper.msg.cannotCreateDetailedDesign"));
		}
		else {
			assert false : "Invalid action type";
		}
		return false;
	}

	@Override protected boolean checkifLocked(IDesignContainer srcDesign, ActionType actionType)
	{
		// Inform the user that he needs to close all the diagrams before copying the design.
		if (IDesignLockStrategy.isLocked(srcDesign)) {
			StringBuilder arg = new StringBuilder('\'');
			arg.append(srcDesign.getName());
			if (CapabilityHelper.supports(SupportedFeatureInfo.Feature.REVISIONING)) {
				arg.append(':');
				arg.append(srcDesign.getRevision());
			}
			arg.append('\'');
			String header;
			if (actionType == ActionType.CREATE_DETAILEDDESIGN) {
				header = ResourceMgr
						.getString(getResourceClass(), "BaseLifecycle.msg.forceCloseCreateDetailedDesignHeader",
								arg.toString());
			}
			else {
				header =
						ResourceMgr
								.getString(getResourceClass(), "BaseLifecycle.msg.forceCloseForCopy",
										arg.toString());
			}
			doForceClosePrompt(header, actionType);
			return true;
		}
		return false;
	}

	public boolean projectDeleted(IProject project)
	{
		if (super.projectDeleted(project)) {
			getStatusReporter().showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(getResourceClass(),
							"Lifecycle.ProjectDeletedWarning.Title.text"), ResourceMgr.getString(getResourceClass(),
							"Lifecycle.ProjectDeletedWarning.Heading.text", project.getName()),
					ResourceMgr.getString(getResourceClass(),
							"Lifecycle.ProjectDeletedWarning.Message.text"));
			discard(project);
			CAFUtils.getInstance().getCAFProjectMgr().closeProject(project);
			//dts0100878301:Create new design is enabled after project deletion which leads to NPE.
			CAFUtils.getInstance().getWindowMgr().tickleUI();
			return true;
		}

		return false;
	}

	/**
	 * Deals with a deleted diagram. Send user message, close opened diagram, and delete it from CAF - not from db and
	 * removes the diagram from memory (owning design MUST be locked).
	 * <p>
	 *
	 * @param design  the design
	 * @param model   the model
	 * @param diagram the diagram
	 */
	private void messageAndClearDiagram(@NotNull ILogicDesign design, @Nullable ICapletModel model,
			@NotNull ISchemDiagram diagram)
	{
		showDiagramDeletionMessageRemoveFromFramework(model, diagram);
		design.removeDiagram(diagram);
		diagram.unload();
	}

	/**
	 * Deals with a deleted diagram. Send user message, close opened diagram, and delete it from CAF - not from db.
	 * <p>
	 *
	 * @param model   the model
	 * @param diagram the diagram
	 */
	private void showDiagramDeletionMessageRemoveFromFramework(@Nullable ICapletModel model,
			@NotNull ISchemDiagram diagram)
	{
		getStatusReporter().showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
				ResourceMgr.getString(getResourceClass(), "Lifecycle.DiagramDeletedWarning.Title.text"),
				ResourceMgr.getString(getResourceClass(), "Lifecycle.DiagramDeletedWarning.Heading.text",
						diagram.getName()),
				ResourceMgr.getString(getResourceClass(), "Lifecycle.DiagramDeletedWarning.Message.text"));

		closeWindowsForDiagram(diagram);

		if (model != null) {
			IUndoableContainer undoableContainer = model.getController().getUndoableContainer();
			undoableContainer.endEdit();
			undoableContainer.clear();
		}

		removeDiagramFromModel(diagram);
	}

	@Override public void setWindowTitle(@NotNull List<?> context, boolean notifyModelChange)
	{
		IProject project = LifecycleUtils.getContextObject(context, IProject.class);
		ILogicDesign theDesign = getContextDesignContainer(context, ILogicDesign.class);
		ISchemDiagram theDiagram = LifecycleUtils.getContextObject(context, ISchemDiagram.class);

		if (project == null || theDesign == null) {
			throw new IllegalArgumentException("Wrong context for setWindowTitle()");
		}

		// update the browser if the diagram name has changed
		if (notifyModelChange && theDiagram != null) {
			// send a model change event so that the browser tree will
			// be notified that the diagram name has been changed
			Model model = getModel(theDesign);
			if (model != null) {
				Collection<IUID> emptyList = Collections.emptyList();
				model.notifyModelChange(new ModelChangeEvent(model, emptyList));
			}
		}
		for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			if (cafWin instanceof ICapletWindow && cafWin.isDisplayed()) {
				CapletViewIterator cvIt = ((ICapletWindow) cafWin).getViews();
				while (cvIt.hasNext()) {
					GfxView view = (GfxView) cvIt.getNext();
					if (view.getCapletModel() instanceof Model) {
						Model model = (Model) view.getCapletModel();
						IDesign design = model.getDesign();
						ISchemDiagram diagram = model.getDiagram();
						//dts0100601494 : we need to take the diagram of a view rather than
						//active diagram. model.getDiagram gives current active diagram.
						IBaseDiagram baseDiagram = view.getDiagram();
						if (baseDiagram instanceof ISchemDiagram) {
							diagram = (ISchemDiagram) baseDiagram;
						}

						boolean readOnly = !(model.isEditable() && diagram.isEditable());
						String title = LogicCapletUtils.getDiagramTitle(diagram, readOnly);

						if (theDiagram == null) { // The context contains a
							// design only
							// As far as the design matched - rename the window
							if (design == theDesign) {
								cafWin.setTitle(title);
							}
						}
						else { // Only matched design & diagram window gets
							// rename
							if ((design == theDesign) && (diagram == theDiagram)) {
								cafWin.setTitle(title);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean releaseLock(@NotNull IDesignContainer design)
	{
		return IDesignLockStrategy.releaseLock(design);
	}

	public void preferenceChanged(IPreferenceChangeEvent e)
	{
		IProjectPreferenceMgr prefMgr = (IProjectPreferenceMgr) e.getPreferenceMgr();
		for (Model model : models.values()) {
			final IProject project = model.getDesign().getProject();
			assert project != null;
			if (project.getPreferences() == prefMgr) {
				for (ISchemDiagram diagram : model.getDiagrams()) {
					setDrawGridSpacing(diagram, getDrawGridSpacing(prefMgr));
				}
			}
		}
	}

	private void setDrawGridSpacing(ISchemDiagram diagram, int drawGridSpacing)
	{
		IGrid grid = diagram.getGrid();
		if (grid != null) {
			IGrid subGrid = grid.getSubGrid();
			if (subGrid == null) {
				subGrid = new Grid();
			}
			subGrid.setGridSpacing((grid.getGridSpacing() * drawGridSpacing) / 100);
		}
	}

	private String getValidDiagramName(IDesign design)
	{
		Set<String> names = new HashSet<String>();
		for (ICHSIterator<ISchemDiagram> diagrams = ((ILogicDesign) design).getDiagrams(); diagrams.hasNext(); ) {
			names.add(diagrams.getNext().getName());
		}
		String validName = null;
		int count = design.getNumDiagrams();
		while (validName == null) {
			String name =
					ResourceMgr.getString(getResourceClass(), "Lifecycle.NewDiagram.DefaultNamePrefix.text") +
							count;
			count++;
			if (!names.contains(name)) {
				validName = name;
			}
		}
		return validName;
	}

	/**
	 * This will be called for save task (currently) before creating task.
	 */
	public void creatingTask()
	{
		m_wipeOutDesignsAtLast = new WipeOutDesignsAtSaveCompletionTask(lifeCycleReleaseLockOnDesigns);
		m_onSaveTasks.add(m_wipeOutDesignsAtLast);
	}

	/**
	 * This will be called for save task (currently) if task creation failed.
	 */
	public void taskCreationFailed()
	{
		m_wipeOutDesignsAtLast = null;
		m_onSaveTasks.clear();
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		if (btm.isWithinBoundaryForInitiator(this)) {
			btm.exitTransactionBoundary(this, false);
		}
	}

	/**
	 * Performing the time consuming task - i.e. send save request to the server
	 *
	 * @param task - The task information to be performed
	 * @return the object result of the performTask - In this case we don't care - null
	 * @throws Exception - If there is any problem
	 */
	@Override public Object performTask(ITask task) throws Exception
	{
		boolean mySuccess = false;
		Object o;
		Collection<ILinkDelta> linkDelta = new ArrayList<ILinkDelta>();
		LinkSaveHelper linkSaveHelper = new LinkSaveHelper();
		try {
			o = DesignPersistenceUtils.performSaveTask(task, linkDelta, linkSaveHelper);
			if (o instanceof IUID) {
				syncTeamPlayReferencesForCurrentDesign(((IUID) o).getString());
			}
			mySuccess = true;
		}
		finally {
			IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
			btm.exitTransactionBoundary(this, mySuccess);
		}
		linkSaveHelper.notifySaveListners(linkDelta);
		return o;
	}

	@Override protected void processOnSaveCompletionTasks()
	{
		m_wipeOutDesignsAtLast = null;
		super.processOnSaveCompletionTasks();
	}

	/**
	 * The background task is finished - Enable the SaveAction back - If there is any error - process appropriately
	 * <p>
	 * NOTE: Be careful using the Task's arguments, since they might not be valid anymore
	 *
	 * @param task      - The finished task
	 * @param success   - true if the task is successful; false otherwise
	 * @param result    - The resulting object
	 * @param throwable - null if success; a Throwable otherwise
	 */
	public void taskFinished(ITask task, boolean success, Object result, Throwable throwable)
	{
		// Notify save listeners prior to re-enabling the SaveAction, should be refactored to be
		// part of finishSaveTask, if the other harness and topo LifeCycle classes were to use it!
		IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
		boolean localSuccess = success;
		boolean completedTransaction = false;
		try {
			notifySaveCompleteListeners(task, success, result, throwable);
			completedTransaction = true;
			//
			finishSaveTask(task, success, throwable);
			processOnSaveCompletionTasks();
			if (!success) { // The save to dbase failed - Check the exception
				// Get the currentProject from the task first argument
				IProject currProject = (IProject) task.getArgs().get(0);
				// Discard the changes
				discard(currProject);
			}
		}
		catch (Error e) {
			localSuccess = false;
			throw e;
		}
		finally {
			m_onSaveTasks.clear();
			if (!completedTransaction && btm.isWithinBoundaryForInitiator(this)) {
				btm.exitTransactionBoundary(this, localSuccess);
			}
		}
	}

	public boolean allowPromptOnClosingLastWindowOnDesign()
	{
		return true;
	}

	@NotNull protected FunctionalPermissionEnum getEditDesignPermission()
	{
		return FunctionalPermissionEnum.EditLogicDesigns;
	}

	@NotNull protected ILogicInvalidStateHandler getLogicDesignScrubber()
	{
		return new ScrubberFactory().getLogicDesignScrubber();
	}

	private void updateDecorationsBeforeSave(Collection<IUID> modDiagramList)
	{
		for (IUID modifiedObject : modDiagramList) {
			IUIDObject modifedDiagram = UIDMgr.getObject(modifiedObject);
			assert modifedDiagram instanceof ISchemDiagram;
			((IStyleableDiagram) modifedDiagram).updateCompositeTexts();
			//we will fix-up the comment symbol attribute texts at save/export.
			//can't do at import because it might impact released designs.
			CommonScrubbableChecker
					.checkAttributeTextsOnComment(getLogicDesignScrubber(), (IBaseDiagram) modifedDiagram);
		}
	}

	protected void finishSaveTask(ITask task, boolean success, Throwable throwable)
	{
		DesignCapletLifecycleHelper.finishSaveTask(task, getFIB(), success, throwable);
	}

	private class DesignRefreshListener implements ILogicDesignRefreshListener
	{

		@Override public void diagramRemoved(ILogicDesign design, ISchemDiagram diagram)
		{
			if (SwingUtilities.isEventDispatchThread()) {
				cleanUpView(design, diagram);
			}
			else {
				waitAndCleanUpView(design, diagram);
			}
		}

		private void cleanUpView(ILogicDesign design, ISchemDiagram diagram)
		{
			Model model = (Model) CAFUtils.getInstance().getModelOfDiagram(diagram);

			if (model != null) {
				//we will be putting the message to output window instead of message dialog.
				String diagramName = design.getFullName() + ":" + diagram.getName();
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(HTMLHelper.color(IXMLTags.RED,
						ResourceMgr.getString(getResourceClass(), "Lifecycle.DiagramDeletedRemotely.Message.text",
								diagramName)));
			}

			closeWindowsForDiagram(diagram);

			if (model != null) {
				IUndoableContainer undoableContainer = model.getController().getUndoableContainer();
				undoableContainer.endEdit();
				undoableContainer.clear();
			}

			removeDiagramFromModel(diagram);
		}

		private void waitAndCleanUpView(final ILogicDesign design, final ISchemDiagram diag)
		{
			Runnable runnable = new Runnable()
			{
				public void run()
				{
					cleanUpView(design, diag);
				}
			};
			try {
				SwingUtilities.invokeAndWait(runnable);
			}
			catch (InterruptedException e) {
				throw new WrappingRuntimeException(e);
			}
			catch (InvocationTargetException e) {
				throw new WrappingRuntimeException(e);
			}
		}

		@Override public void diagramAdded(ILogicDesign design, ISchemDiagram diagram)
		{
		}
	}

	@Override public void designRemotelyDeleted(IProject project, ILogicDesign design)
	{
		clearDeletedDesign(project, design);
	}

	@Override public void diagramValidationFailed(ValidationException exception, @Nullable ILogicDesign design)
	{
		handleOpenDiagramValidation(exception, design);
	}

	@Nullable @Override public Model findModel(ILogicDesign design)

	{
		// See if we already have a model for this diagram, and if so just return it. We only want one model/controller
		// per diagram.
		// Note:: we do this before processing the locks, i.e if its allready open in read only don't prompt the
		// user again.
		Model model = getModel(design);
		//Check if this model is present in "removable_models" list
		//If so, first remove this model from models list--dts0100710104
		if (model != null && removable_models.containsValue(model)) {
			destroyModel(model);
			model = null;
		}
		return model;
	}

	private boolean destroyOrphanedModel(@Nullable Model model)
	{
		// if we have no model for this design, create one
		if (model != null && removable_models.containsValue(model)) {
			destroyModel(model);
			return true;
		}
		return false;
	}

	@Override public Model createModel(IProject project, ILogicDesign design, ISchemDiagram diagram, Model model)
	{
		Model newModel = model;
		if (destroyOrphanedModel(newModel)) {
			newModel = null;
		}
		if (newModel == null) {
			// Create a new controller (which will create a model)
			ICapletController lc = createController(m_caplet, design, diagram);
			newModel = (Model) lc.getCapletModel();
			registerDesignModel(design, newModel);

			design.addRefreshListener(new DesignRefreshListener());
		}
		newModel.addDiagram(diagram);
		diagram.setWithModel(true);
		setDrawGridSpacing(diagram, getDrawGridSpacing(project.getPreferences()));
		return newModel;
	}

	public void forcePurgeUnplacedObjects(IDesign design)
	{
		if (design instanceof ILogicDesign) {
			ILogicDesign logicDesign = (ILogicDesign) design;
			Model model = getModel(logicDesign);
			new PurgeUnplacedConnectivity().forcePurgeUnplacedObjects(logicDesign, model);
		}
	}

	private static class ReleaseLockFromDesignOnSaveCompleteTask extends OnSaveCompleteTask
	{

		private final IUIDObject designOrDiagram;

		protected ReleaseLockFromDesignOnSaveCompleteTask(@NotNull IProject project,
				@NotNull IUIDObject designOrDiagram)
		{
			super(DesignPersistenceUtils.getSaveTaskId(project));
			this.designOrDiagram = designOrDiagram;
		}

		@Override public void execute()
		{
			IDesignLockStrategy.releaseLock(designOrDiagram);
		}
	}

	protected class WipeOutDesignOnSaveCompleteTask extends OnSaveCompleteTask
	{

		private final ILogicDesign m_design;
		private final boolean m_unlockDesign;
		@NotNull private final WindowCloseStatus m_windowCloseStatus;

		protected WipeOutDesignOnSaveCompleteTask(@NotNull IProject project, @NotNull ILogicDesign design,
				boolean unlockDesign, @NotNull WindowCloseStatus windowCloseStatus)
		{
			super(DesignPersistenceUtils.getSaveTaskId(project));
			m_design = design;
			m_unlockDesign = unlockDesign;
			m_windowCloseStatus = windowCloseStatus;
		}

		@Override public void execute()
		{
			if (m_unlockDesign) {
				// Finally unlock the design
				ICAFProjectMgr projectMgr = getFIB().getProjectMgr();
				//			relinquishLock(m_design);
				projectMgr.relinquishLock(m_design);
				if (m_design instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
					IDesignLockStrategy.releaseLock(m_design);
				}
			}

			// Remove the design and everything that it references, from memory [only if it is not a local-only design]
			//
			// If the design is incrementally loadable tell it that it is a skeleton. This will load it next time round.
			if (m_design instanceof IIncLoadable loadable) {

				if (loadable.isSkeletonizable()) {
					// unload the designs children, keeping the design skeletonally loaded...
					unloadDesignChildren(m_design);
				}

				loadable.setSkeleton(true);
			}

			// Triggering sync if current activated design is instance of Integrator/Topology/Platform design
			Optional<ICapletWindow> capletWindow =
					CommonUtils.castOptional(getFIB().getWindowMgr().getCurrentWindow(), ICapletWindow.class);

			// To make sure sync is invoked only when user has actually closed some design.
			// eg. while generating logic design from functional design logic design is only loaded and unloaded from memory
			// in above scenario sync is not required since user is present in same design
			if (m_windowCloseStatus == WindowCloseStatus.CLOSED && capletWindow.isPresent()) {
				// will invoke sync to make sure all the associated design are up to date.
				capletWindow.map(ICapletWindow::getController)
								.ifPresent(ICapletController::synchronizeChangeOnActivation);
			}
		}

		/**
		 * Unloads all or some of the children of the design.
		 * <p>
		 *
		 * @param theDesign The design to unload children from
		 */
		protected void unloadDesignChildren(@NotNull ILogicDesign theDesign)
		{
			theDesign.unloadChildren();
		}
	}

	private static class WipeOutDesignsAtSaveCompletionTask extends OnSaveCompleteTask
	{

		private Map<IUID, OnSaveCompleteTask> m_tasks = new HashMap<>();

		protected WipeOutDesignsAtSaveCompletionTask(String taskId)
		{
			super(taskId);
		}

		@Override public void execute()
		{
			//as per TaskWorker.finished documentation. This code is expected to be executend in only EDT only.
			//So we don't need to have a mutex (ReentrantLock) and put under try(lock)/finally(unlock) block. we might do if needed.
			for (OnSaveCompleteTask completeTask : m_tasks.values()) {
				completeTask.execute();
			}
			m_tasks.clear();
		}

		public boolean scheduleWipeOutDesignTask(@NotNull IUIDObject designObject,
				@NotNull OnSaveCompleteTask onSaveCompleteTask)
		{
			//if using ReentrantLock. Need to wrap this under try(lock)/finally(unlock) if tryLock succeds.
			//if ReentrantLock.tryLock fails return false.
			m_tasks.put(designObject.getUID(), onSaveCompleteTask);
			return true;
		}
	}

	@Override
	public boolean prepareForLogicDesignCopy(IDesignContainer srcDesign, IProject project,
			boolean createDetailedDesign)
	{

		assert project != null;
		if (projectDeleted(project, getResourceClass()) || !checkDesignExists(project, srcDesign)) {
			return false;
		}
		return super.prepareForLogicDesignCopy(srcDesign, project, createDetailedDesign);
	}
}
