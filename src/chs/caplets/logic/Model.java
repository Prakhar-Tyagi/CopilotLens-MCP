/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2025 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.cafmain.actions.bridges.change.report.PersistantCMReport;
import chs.caf.caplet.ICOFObjectModelChangeAdaptor;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletStyleModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.IUndoableInverseOperation;
import chs.caf.caplet.IUndoableModel;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.IVirtualChild;
import chs.caf.caplet.IVirtualParent;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.ObjectModelChanges;
import chs.caf.caplet.helpers.DRCRunnerHelper;
import chs.caf.caplet.helpers.DiagramStyleSetValidityChecker;
import chs.caf.caplet.helpers.GfxCapletModelHelper;
import chs.caf.caplet.helpers.VoidUndoObjectHelper;
import chs.caf.caplet.logic.HWMNameSpaceIndexSynchronizer;
import chs.caplets.logic.analysis.LogicAnalysisColoringProcessor;
import chs.caplets.logic.connectivity.LogicPointConnector;
import chs.caplets.logic.harness.propagate.AutoPropagateHarnessController;
import chs.caplets.logic.layout.ParameterizedPhysicalDimensionUpdateListener;
import chs.caplets.logic.shared.SharedHighwayConnectionsListener;
import chs.caplets.shared.IGfxDisplayableModel;
import chs.caplets.topo.actions.topologyroutingassistant.RoutingAssistantLogicModelListener;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IBasePrintRegionInfo;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IJoint;
import chs.cof.drawplus.IPrintRegionGroup;
import chs.cof.drawplus.IPrivilegedPrintRegionGroupsManager;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.LogicDesignChangeNotifier;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.logical.concurrency.MulticoreIndicatorStructureUpdateHandler;
import chs.cofUtils.scrubber.LogicOnTheFlyScrubber;
import chs.cofUtils.scrubber.OnTheFlyScrubber;
import chs.common.DesignUtils;
import chs.common.IAttributeDatum;
import chs.common.IBOMID;
import chs.common.IDatum;
import chs.common.IDesignContainer;
import chs.common.IFunctionalModuledDesign;
import chs.common.IProductionModuledDesign;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.introspection.ObjectRelationship;
import chs.common.validation.IInternalErrorReporter;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ListSet;
import chs.utilities.SupportedFeatureInfo;
import chs.utilities.suite.ApplicationSuiteInfo;
import chs.utilities.suite.IApplicationSuite;
import chs.utilities.topology.TopologyServices;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Data model for capital logic.
 */
public class Model extends GfxCapletModelHelper implements IGfxDisplayableModel, ILogicModel, ICapletStyleModel
{

	@ObjectRelationship(type = ObjectRelationship.Type.NONE)
	private IUID m_design;

	@ObjectRelationship(type = ObjectRelationship.Type.NONE)
	private List<IUID> diagrams = new ListSet<IUID>();

	@ObjectRelationship(type = ObjectRelationship.Type.NONE)
	private IUID currentDiagram; //ISchemDiagram

	private IModelChangeListener m_currentDiagramModelListener = null;

	@ObjectRelationship(type = ObjectRelationship.Type.NONE)
	private IUndoableModel m_undoableModel;

	private boolean m_orthoMode;

	private static final LogicOnTheFlyScrubber scrubber = new LogicOnTheFlyScrubber();

	@ObjectRelationship(type = ObjectRelationship.Type.NONE)
	private LogicDiagramTableDataProjectChangeListener m_diagramTableListener;
	private boolean styleSetValid = true;

	private AutoGenerateConnectorSupport m_autoGenerateConnectorSupport = new AutoGenerateConnectorSupport();

	/**
	 * Constructor for the Model object
	 *
	 * @param cont   Description of the Parameter
	 * @param design Description of the Parameter
	 */
	public Model(ICapletController cont, ILogicDesign design)
	{
		super(cont, design);

		m_design = design.getUID();        // simons - store these by UID
		m_undoableModel = (IUndoableModel) design;
		m_orthoMode = false;

		// make DSUM receive notification for changes in this model
		// the DSUM will manage design shared usages on at the end of model notifications
		// The register/deregister was moved here from BaseLifeCycle after a code review of the DSUM
		// TODO jacobt FEAT13040 : listener leak here?  It was like this in 8.1
		// This object is added as a listener but the destory attempts to remove underlying DSUM
		IModelChangeListener sharedUsageMCL = new ModelChangeRedirector(design)
		{
			@Nullable protected ICOFObjectModelChangeAdaptor getProxy()
			{
				ILogicDesign des = getDesign();
				if (des != null) {
					return (ICOFObjectModelChangeAdaptor) des.getSharedUsageMgr();
				}
				return null;
			}
		};
		addModelChangeListener(sharedUsageMCL);
		addModelChangeListener(new HWMNameSpaceIndexSynchronizer(design));
		addModelChangeListener(new MulticoreIndicatorStructureUpdateDelegate(design));

		IApplicationSuite applicationSuite = ApplicationSuiteInfo.getInstance().getCurrentApplicationSuite();
		if (applicationSuite.supportsCapability(SupportedFeatureInfo.Feature.DESIGN_RULE_CHECKS)) {
			addModelChangeListener(DRCRunnerHelper.getDRCBackgroundRunner().getDRCBackgroundController());
			addModelActivationListener(DRCRunnerHelper.getDRCBackgroundRunner().getDRCBackgroundController());
		}

		// Add the listener to refresh the information in RA tables
		addModelChangeListener(
				TopologyServices.instance().locateServiceNullable(RoutingAssistantLogicModelListener.class));

		IConnectivity connectivity = design.getConnectivity();
		if (connectivity != null) {
			addModelChangeListener(new SharedHighwayConnectionsListener(design));
			addModelChangeListener(new HighwayContentUpdateListener(design));
			addModelChangeListener(new ModularSchematicUpdateListener(design));
			if (design instanceof ILayoutLogicDesign) {
				addModelChangeListener(new ParameterizedPhysicalDimensionUpdateListener(design));
			}
			CommonUtils.castOptional(AutoPropagateHarnessController.getInstance(), IModelChangeListener.class)
					.ifPresent(this::addModelChangeListener);
		}

		// register the model with the analysis coloring processor...
		LogicAnalysisColoringProcessor.registerModel(this);

		// Add the listener to refresh the information in diagram tables
		m_diagramTableListener = new LogicDiagramTableDataProjectChangeListener(design);
		CAFUtils.getInstance().getCAFProjectMgr().addProjectChangeListener(m_diagramTableListener);
	}

	public void destroy()
	{
		// unregister the model from the analysis coloring processor...
		LogicAnalysisColoringProcessor.unregisterModel(this);

		// unregister DSUM from this diagrams notifications
		// DSUM was never added to listeners, only the redirector was added. so why remove dum here ? Redirector should be removed here
		// Why to remove anything? super.destroy would anyway clear all the listners?
//		removeModelChangeListener((IModelChangeListener) getDesign().getSharedUsageMgr());
//		removeModelChangeRedirector();

		CAFUtils.getInstance().getCAFProjectMgr().removeProjectChangeListener(m_diagramTableListener);
		IDynamicGfxService svc = getDynamicGfxService();
		if (svc != null) {
			svc.setCurrentPointConnector(null);
		}
		if (diagrams != null) {
			diagrams.clear();
			diagrams = null;
		}
		m_design = null;
		m_undoableModel = null;
		m_diagramTableListener = null;
		removeCurrentDiagramModelChangeListener();
		currentDiagram = null;
		super.destroy();
	}

//	private void removeModelChangeRedirector()
//	{
//		for (IModelChangeListener changeListener : getModelChangeListeners()) {
//			if (changeListener instanceof ModelChangeRedirector) {
//				ModelChangeRedirector redirector = (ModelChangeRedirector) changeListener;
//				if (getDesign() == redirector.getDesign()) {
//					removeModelChangeListener(redirector);
//					break;
//				}
//			}
//		}
//	}
//

	/**
	 * Sets the state of orthogonal creation mode.
	 *
	 * @param val value
	 */
	public void setOrthogonal(boolean val)
	{
		m_orthoMode = val;
	}

	/**
	 * Returns the state of conductor creation mode.
	 *
	 * @return True if creating conductors in orthognal mode.
	 */
	public boolean getOrthogonal()
	{
		return m_orthoMode;
	}

	/**
	 * Overridden here to enable on the fly scrubbing for logic designs
	 *
	 * @return The LogicOnTheFlyScrubber
	 */
	@Override @Nullable public OnTheFlyScrubber getScrubber()
	{
		return scrubber;
	}

	/**
	 * Gets the design attribute of the Model object
	 *
	 * @return The design value
	 */
	public ILogicDesign getDesign()
	{
		return DesignUtils.getLoadedDesign(m_design, ILogicDesign.class);
	}

	/**
	 * Gets the diagram attribute of the Model object
	 *
	 * @return The diagram value
	 */
	public ISchemDiagram getDiagram()
	{
		// this is the current diagram maintained here
		// TODO jacobt FEAT13040 : Is this always the active diagram in CAF?  could assert here to find out
		//assert diagrams.contains(currentDiagram);
		ISchemDiagram diagram = UIDMgr.getObjectOfType(currentDiagram, ISchemDiagram.class);
		//if (diagram == null) {
		// TODO jacobt FEAT13040 : Not clear if CAF should let us get here.  If so then this should be @Nullable
		//	throw new IllegalStateException("Logic Model must have a current diagram");
		//}

		return diagram;
	}

	public void setCurrentDiagram(ISchemDiagram diagram)
	{
		if (diagrams.contains(diagram.getUID())) {
			currentDiagram = diagram.getUID();
			setStyleSetValidty(DiagramStyleSetValidityChecker.isStyleSetValid(getDesign(), diagram));
			// Does the DynamicGfxService need a new LogicPointConnector every time the diagram changes or could it
			// reuse the one that is set when the controller is constructed?
			IDynamicGfxService svc = getDynamicGfxService();
			LogicPointConnector tpc = new LogicPointConnector(() -> diagram);
			svc.setCurrentPointConnector(tpc);

			addCurrentDiagramModelChangeListener();
		}
		else {
			throw new IllegalStateException("Current diagram must have been added");
		}
	}

	private void addCurrentDiagramModelChangeListener()
	{
		removeCurrentDiagramModelChangeListener();
		m_currentDiagramModelListener = getModelChangeListenerForMakeAnchorsDirty(currentDiagram);
		addModelChangeListener(m_currentDiagramModelListener);
	}

	private void removeCurrentDiagramModelChangeListener()
	{
		if (m_currentDiagramModelListener != null) {
			removeModelChangeListener(m_currentDiagramModelListener);
			m_currentDiagramModelListener = null;
		}
	}

	public boolean addDiagram(ISchemDiagram diagram)
	{
		boolean added = diagrams.add(diagram.getUID());
		setCurrentDiagram(diagram);
		return added;
	}

	public boolean removeDiagram(ISchemDiagram diagram)
	{
		// LOGIC-8316 Call home encountered when deleting a diagram , when 2 users open same project and design
		// if currentDiagram is same as the one being removed from Model, then set currentDiagram to null
		if (currentDiagram == diagram.getUID()) {
			currentDiagram = null;
			IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();
			if (activeDiagram instanceof ISchemDiagram && getDesign() == activeDiagram.getDesignContainer()) {
				setCurrentDiagram((ISchemDiagram) activeDiagram);
			}
		}
		return diagrams.remove(diagram.getUID());
	}

	public boolean containsDiagram(ISchemDiagram diagram)
	{
		return diagrams.contains(diagram.getUID());
	}

	public Collection<ISchemDiagram> getDiagrams()
	{
		List<ISchemDiagram> schemDiagrams = UIDUtils.convertToUIDObjectList(diagrams);
		if (schemDiagrams.contains(null)) {
			throw new IllegalStateException("Diagrams in a logic model have been removed from the UIDMgr");
		}
		return schemDiagrams;
	}

	public ISheet getSheet()
	{
		return getDiagram();
	}

	/**
	 * Description of the Method
	 *
	 * @param oldObj Description of the Parameter
	 * @param newObj Description of the Parameter
	 */
	public void replaceObject(IUndoableObject oldObj, IUndoableObject newObj)
	{
		IPrivilegedPrintRegionGroupsManager.replaceObject(CAFUtils.getInstance().getActiveDiagram(), oldObj, newObj);
		// FEAT13040 - the logic model (and undoable model) is now the design don't try to sneak anything else in here
		assert m_undoableModel instanceof IDesign;

		// find out if this is a schemObject or a logicObject and send off
		// the replace object to the correct undoableModel.

		if ((oldObj instanceof IDiagramObject) || (newObj instanceof IDiagramObject) ||
				(oldObj instanceof IJoint) || (newObj instanceof IJoint)) {
			// schem object, so do it here
			if ((oldObj instanceof IVirtualChild) || (newObj instanceof IVirtualChild)) {
				// see notes in CAFDiagram.replaceObject() regarding isDirectDiagramChild().  This mechanism should ideally
				// replace use of IVirtualChild, as is done in topology and harness caplets.
				m_undoableModel.replaceObject(oldObj, newObj);
				//
				// For these Schem Objects, when it is not an add/remove,
				// get the virtual parent. If it is a sheet, then call the
				// replace object...
				//
				IVirtualParent vpar = null;
				if (!(oldObj instanceof VoidUndoObjectHelper) &&
						!(newObj instanceof VoidUndoObjectHelper)) {
					if (oldObj instanceof IVirtualChild) {
						vpar = ((IVirtualChild) oldObj).getVirtualParent();
					}
					else /*if (newObj instanceof IVirtualChild)*/ {
						vpar = ((IVirtualChild) newObj).getVirtualParent();
					}
				}
				if (vpar instanceof ISheet) {
					vpar.replaceObject(oldObj, newObj);
				}
			}
		}
		else if ((oldObj instanceof IUndoableInverseOperation) || (newObj instanceof IUndoableInverseOperation)) {
			// go do the right thing with the port manager
			IUndoableInverseOperation pmuh;
			if (oldObj instanceof IUndoableInverseOperation) {
				pmuh = (IUndoableInverseOperation) oldObj;
			}
			else /*if (newObj instanceof IUndoableInverseOperation)*/ {
				pmuh = (IUndoableInverseOperation) newObj;
			}
			pmuh.processUndo();
		}
		else if ((oldObj instanceof IPrintRegionGroup) || (newObj instanceof IPrintRegionGroup) ||
				(oldObj instanceof IBasePrintRegionInfo) || (newObj instanceof IBasePrintRegionInfo)) {
			m_undoableModel.replaceObject(oldObj, newObj);
		}
		else if (!(oldObj instanceof IDatum || newObj instanceof IDatum) &&
				!(oldObj instanceof IBOMID || newObj instanceof IBOMID) &&
				!(oldObj instanceof IAttributeDatum || newObj instanceof IAttributeDatum)) {
			//LOGIC-11460:java.lang.AssertionError: Generate BOM ID action and Undo cause an CH.
			// additionally, the connectivity of the design must be undone.
			// This cast should be checked, but Glenn says I don't have to.
			IUndoableModel connUndoableModel = (IUndoableModel) getDesign().getConnectivity();
			assert connUndoableModel != null;
			connUndoableModel.replaceObject(oldObj, newObj);
		}
	}

//	public boolean removeObject(IUIDObject obj)
//	{
//		return true;
//	}
//
//	public boolean insertObject(IUIDObject schemobj)
//	{
//		return true;
//	}

	/**
	 * For now all diagrams in the designs of the following description are read-only 1) Non-draft designs 2) Generated
	 * designs 3) Topology plane designs (generated wiring diagrams) 4) Designs that were opened in read-only mode
	 * <p>
	 * <p>
	 * Fix for 'dts0100646451 - Logic Design does not open when the user states no to changing the styleset to
	 * Default'.
	 *
	 * @see ICapletModel#isEditable()
	 */
	public boolean isEditable()
	{
		return isContextEditable(getDiagram());
	}

	private boolean isContextEditable(@Nullable ISchemDiagram diagram)
	{
		return (diagram == null || diagram.isEditable()) && isEditableForDesign();
	}

	private boolean isEditableForDesign()
	{
		ILogicDesign design = getDesign();
		return design != null && design.isOMStateValidForEdit() && isStyleSetValid() && design.isEditable();
	}

	public boolean isEditable(@Nullable IUIDObject child)
	{
		return isContextEditable(CommonUtils.cast(child, ISchemDiagram.class));
	}

	/**
	 * Does this model contain this object, or a representation of this object?
	 *
	 * @param uidObject Object to search for
	 * @return boolean True iff uidObject belongs to Diagram referenced by this Model
	 */
	public boolean containsObject(IUIDObject uidObject)
	{
		return LogicUtils.containsObject(getDiagram(), uidObject);
	}

	/**
	 * ICapletModel#getModelRoot()
	 */
	public IUIDObject getModelRoot()
	{
		// TODO creddy(2015.1): Why is this diagram? Should it not be design!!
		return getDiagram();
	}

	/**
	 * @param reporter - the object that should be used to record errors.
	 */
	public void validate(IInternalErrorReporter reporter)
	{
		super.validate(reporter);

		// dts0100969009 - do not bother doing this work on deleted diagrams
		ISchemDiagram diagram = getDiagram();
		if (diagram == null || diagram.isDeleted()) {
			return;
		}

		// check the usages
		// TODO jacobt FEAT14396 : why the extra usage validation here?
		Objects.requireNonNull(diagram.getDesign()).getSharedUsageMgr().validate(reporter);
	}

	/**
	 * Overriden for logic to ensure that the design will be validated when a change is made to the object model.
	 *
	 * @return a list of object that only contains the design.
	 */
	@Override
	@NotNull
	public List<IUIDObject> getHighLevelObjectsToValidateOnChange()
	{
		if (getDesign() != null) {
			// Validate the whole design instead of just the changed objects
			// if the whole design is present...
			List<IUIDObject> newObjList = new ArrayList<IUIDObject>();
			newObjList.add(getDesign());
			if (getDesign().getProject() != null) {
				// ...and validate the shared conductor/pinlist mgrs
				newObjList.add(getDesign().getProject().getSharedConductorMgr());
				newObjList.add(getDesign().getProject().getSharedPinListMgr());
			}
			return newObjList;
		}
		return Collections.emptyList();
	}

	private void setStyleSetValidty(boolean isStyleSetValid)
	{
		styleSetValid = isStyleSetValid;
	}

	@Override public boolean isStyleSetValid()
	{
		return styleSetValid;// && getDiagram().getPreferenceSet() != null;
	}

	@Override public void ensureStyleSetValidity()
	{
		ISchemDiagram diagram = getDiagram();
		if (diagram != null) {
			setStyleSetValidty(DiagramStyleSetValidityChecker.isStyleSetValid(getDesign(), diagram));
		}
	}

	private abstract static class ModelChangeRedirector implements IModelChangeListener
	{

		private IUID m_watchedDesign;

		protected ModelChangeRedirector(ILogicDesign des)
		{
			m_watchedDesign = des.getUID();
		}

		@Nullable protected ILogicDesign getDesign()
		{
			return UIDMgr.getObjectOfType(m_watchedDesign, ILogicDesign.class);
		}

		@Nullable protected abstract ICOFObjectModelChangeAdaptor getProxy();

		public void modelPreChanged(ModelChangeEvent e)
		{
			ICOFObjectModelChangeAdaptor proxy = getProxy();
			if (proxy != null) {
				proxy.objectModelPreChanged(new ObjectModelChanges(e.getChangedObjectsUIDs()));
			}
		}

		public void modelChanged(ModelChangeEvent e)
		{
			ICOFObjectModelChangeAdaptor proxy = getProxy();
			if (proxy != null) {
				proxy.objectModelChanged(new ObjectModelChanges(e.getChangedObjectsUIDs()));
			}
		}
	}

	private static class MulticoreIndicatorStructureUpdateDelegate implements IModelChangeListener
	{

		private IUID m_watchedDesign;

		private MulticoreIndicatorStructureUpdateDelegate(ILogicDesign des)
		{
			m_watchedDesign = des.getUID();
		}

		public void modelPreChanged(ModelChangeEvent e)
		{
			ILogicDesign logicDesign = UIDMgr.getObjectOfType(m_watchedDesign, ILogicDesign.class);
			if (logicDesign != null) {
				MulticoreIndicatorStructureUpdateHandler.getInstance().modelPreChanged(logicDesign);
			}
		}

		public void modelChanged(ModelChangeEvent e)
		{
		}
	}

	@Override public void notifyModelChange(ModelChangeEvent e)
	{
		super.notifyModelChange(e);
		if (!e.getChangedObjectsUIDs().isEmpty()) {
			CAFUtils.getInstance().getAutoRecovery().designChanged(getDesign());
		}
	}

	public boolean isFullScrubPossible()
	{
		return true;
	}

	public PersistantCMReport getCMReport(boolean createNewFlag)
	{
		return null;
	}

	@Override public boolean allowUndoInReadOnlyMode()
	{
		return false;
	}

	public void postProcessUndoRedo()
	{
		Collection<ISchemDiagram> schemDiagrams = getDiagrams();
		for (ISchemDiagram diagram : schemDiagrams) {
			if (diagram.isLoadedInMemory()) {
				diagram.postProcessTablesForUndoRedo();
			}
		}
		ILogicDesign design = getDesign();
		if (design != null) {
			LogicDesignChangeNotifier.notifyUndoRedoHappened(design);
		}
	}

	@Override public IDesignContainer getDesignContainer()
	{
		return getDesign();
	}

	@Override public IFunctionalModuledDesign getFunctionalModuledDesign()
	{
		return getDesign();
	}

	@Override public IProductionModuledDesign getProductionModuledDesign()
	{
		return null;
	}

	public void setDrawingGridSnap(boolean value)
	{
		super.setDrawingGridSnap(value);

		// Update the status bar of any views presenting this model.
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			ICapletWindow capWin = (ICapletWindow) window;
			ICapletView view = capWin.getCurrentView();
			if (view instanceof View) {
				View logicView = (View) view;
				logicView.setSnapToGrid(value);
			}
		}
	}

	public void setDrawingObjectSnap(boolean value)
	{
		super.setDrawingObjectSnap(value);

		// Update the status bar of any views presenting this model.
		for (ICAFWindow window : CAFUtils.getInstance().getWindowMgr().getWindows()) {
			ICapletWindow capWin = (ICapletWindow) window;
			ICapletView view = capWin.getCurrentView();
			if (view instanceof View) {
				View logicView = (View) view;
				logicView.setSnapToObjectMode(value);
			}
		}
	}

	@Override public boolean isStateValidToPersist()
	{
		//only editable and modified models will be allowed to either save the design or update
		//the design i.e revert shared usages. otherwise we may endup in data corruptions.
		return isModified() && isEditableForDesign();
	}

	@Override public boolean isHighlightAllowed()
	{
		return true;
	}

	@Override public boolean isHandleModificationAllowed()
	{
		return true;
	}

	public boolean supportsAutoGenerateConnectorMode()
	{
		return m_autoGenerateConnectorSupport.checkAutoGenerateConnectorSupport(getDesign());
	}

	public boolean getAutoGenerateConnectorMode()
	{
		return m_autoGenerateConnectorSupport.isAutoGenerateConnectorActive(getDesign());
	}

	public void setAutoGenerateConnectorToggleState(boolean value)
	{
		m_autoGenerateConnectorSupport.setAutoGenerateConnectorToggleState(value);
	}

	public void resetAutoGenerateConnectorState()
	{
		m_autoGenerateConnectorSupport.resetAutoGenerateConnectorState();
	}
}