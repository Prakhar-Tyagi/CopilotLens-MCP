/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2023 Siemens
 */
package chs.caplets.logic.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.DynSimControllerChangeEvent;
import chs.analysis.IAnalysisColoringProcessor;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.IAnalysisSimulationResultsConsumer;
import chs.analysis.IAnalysisSimulationSessionClient;
import chs.analysis.IAnalysisSimulationSessionController;
import chs.analysis.IVHDLMeasurementDataMapping;
import chs.analysis.IVHDLModelMapping;
import chs.analysis.exporter.AnalysisNetlistExporter;
import chs.analysis.importer.AnalysisColoringImporter;
import chs.analysis.importer.AnalysisSimulationResultsImporter;
import chs.analysis.importer.AnalysisVariableImporter;
import chs.analysis.scope.ScopeComponent;
import chs.analysis.ui.AnalysisControlPanel;
import chs.analysis.ui.AnalysisToolTip;
import chs.analysis.ui.IAnalysisSimulationStatusIndicator;
import chs.analysis.ui.SimulationMonitorsTableModel;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ActionNodeIterator;
import chs.caf.CAFUtils;
import chs.caf.IActionNode;
import chs.caf.ICAFWindow;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.IGraphicsFilterChangeListener;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.IToolTipProvider;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.ViewChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.VoidUndoObjectHelper;
import chs.caf.caplet.helpers.graphics.IGraphicsFilterControl;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.logic.LogicFilterControlMgr;
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.analysis.DynSimBackgroundActionUI;
import chs.caplets.logic.actions.analysis.DynSimOffActionUI;
import chs.caplets.logic.actions.analysis.DynSimOnDemandActionUI;
import chs.caplets.logic.actions.analysis.SimulateActionUI;
import chs.caplets.logic.analysis.ui.AnalysisBrowserPanel;
import chs.caplets.logic.analysis.ui.AnalysisPropertiesDialog;
import chs.caplets.shared.BaseController;
import chs.caplets.shared.actions.SelectAction;
import chs.cof.draw.IText;
import chs.cof.draw.Text;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.symbol.IBorder;
import chs.common.IAnalysable;
import chs.common.IDeletedObject;
import chs.common.IDesignContainer;
import chs.common.INamedPropertiedObject;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IPropertyIterator;
import chs.common.IReadOnlyNamedObject;
import chs.common.ISVAnalysisModelMapper;
import chs.common.IUID;
import chs.common.IUIDIterator;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeProvider;
import chs.images.CHSImageLoader;
import chs.services.ui.UIDHyperlinkListener;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.BasicUIFactory;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.tree.Drawer;
import chs.utility.AnalysisHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.ui.CHSSwingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * @author rharring
 * @version 1.0 Created on 15 March 2004
 */
@SuppressWarnings({"NonThreadSafeLazyInitialization", "AssignmentToStaticFieldFromInstanceMethod"})
public class LogicAnalysisServices extends AnalysisServices implements IModelChangeListener,
		IToolTipProvider, IDisplayContextListener, IGraphicsFilterChangeListener
{

	public static final int DISMISS_DELAY = 4000;
	public static final int EIGHTEEN_PIXELS = 18;
	public static final double DEFAULT_Y_WEIGHT = 0.5;
	public static final int TWENTY_PIXELS = 20;
	public static final int DEFAULT_TOOLTIP_MAX_DELAY = 16000;

	private static final String HTML_TOOLTIP_TABLE_PREFIX =
			"<table border=0 cellpadding=1 cellspacing=0 width=100%>" +
					"<tr><td bgcolor=#000000>" +
					"<table align=centre style=\"font-family: Arial, sans-serif; font-style: light;\" border=0 cellpadding=1 cellspacing=1  width=100%>";

	private static final String HTML_TOOLTIP_TABLE_SUFFIX = "</td></tr></table>";

	private static final String HTML_TOOLTIP_ROW_EVEN_PREFIX = "<tr bgcolor=#CCCCCC><td align=left><i>";

	private static final String HTML_TOOLTIP_ROW_ODD_PREFIX = "<tr bgcolor=#FFFFFF><td align=left><i>";

	private static final String HTML_TOOLTIP_COLUMN_SUFFIX =
			"</i></td><td align=left style=\"padding:0 15px 0 15px;\">";

	private static final String HTML_TOOLTIP_ROW_SUFFIX = "</tr>";
	// /////////////// //
	// Class variables //
	// /////////////// //

	/**
	 * This indicates the maximum tool tip delay that may be used
	 */
	protected static int tooltipMaxDelay;

	// ///////////// //
	// Class methods //
	// ///////////// //

	static {
		// see if the user wishes to override the default for the logic tooltips...
		String temp = System.getProperty("CapitalLogic.AnalysisTooltips.visibleDelay", "0");
		try {
			tooltipMaxDelay = Integer.parseInt(temp);
		}
		catch (NumberFormatException ignore) {
			tooltipMaxDelay = 0;
		}
	}

	/**
	 * This method overrides that of AnalysisServices implanting an instance of the logic services to the
	 * AnalysisServices
	 */
	public static AnalysisServices getAnalysisServices()
	{
		if (logicServices == null) {
			logicServices = new LogicAnalysisServices();
		}
		return logicServices;
	}

	// ////////////////// //
	// Instance variables //
	// ////////////////// //

	/**
	 * The analysis tool tip
	 */
	protected AnalysisToolTip tip;

	/**
	 * The tip panel
	 */
	protected JPanel tipPanel;

	/**
	 * An invisible checkbox for the failure modes tool tip
	 */
	protected JCheckBox noneBox;

	/**
	 * The selection filter to use during dynamic simulation
	 */
	protected SelectionFilter simSelectionFilter;

	/**
	 * The select set
	 */
	protected SelectSet simFilteredSet;

	/**
	 * A count of the number of objects in the model prior to a model change event
	 */
	protected int objectCount;

	/**
	 * A list of all objects interested in model change events after we've processed them
	 */
	protected List<IModelChangeListener> postModelChangeListeners;

	/**
	 * A list of all objects interested in model change events after we've processed them
	 */
	protected List<IGraphicsFilterChangeListener> postFilterChangeListeners;

	// //////////// //
	// Constructors //
	// //////////// //

	/**
	 * Creates a new instance of LogicAnalysisServices
	 */
	public LogicAnalysisServices()
	{

		// create the tool tip
		tip = new AnalysisToolTip();
		noneBox = new JCheckBox();
		//tip.setBackground( java.awt.Color.LIGHT_GRAY ) ;

		// update the tool tip, no model, remove any components and no name
		updateTipSafely(null, true, null);

		// Add us as a DisplayContextListener.
		// CAUTION: Some unit tests do not have a FIB, so getWindowMgr() would fail.
		if (CAFUtils.getInstance().getFIB() != null) {
			CAFUtils.getInstance().getWindowMgr().addDisplayContextListener(this);
		}

		// create the simSelectionFilter
		simSelectionFilter = new SelectionFilter();
		// except any text etc.
		simSelectionFilter.addExceptClass(Text.class);

		simFilteredSet = new SelectSet();
		simFilteredSet.setSelectionFilter(simSelectionFilter);

		objectCount = 0;

		postModelChangeListeners = new ArrayList<IModelChangeListener>();
		postFilterChangeListeners = new ArrayList<IGraphicsFilterChangeListener>();
	}

	public void addPostModelChangeListener(IModelChangeListener listener)
	{
		postModelChangeListeners.add(listener);
	}

	public void removePostModelChangeListener(IModelChangeListener listener)
	{
		postModelChangeListeners.remove(listener);
	}

	public void addPostFilterChangeListener(IGraphicsFilterChangeListener listener)
	{
		postFilterChangeListeners.add(listener);
	}

	public void removePostFilterChangeListener(IGraphicsFilterChangeListener listener)
	{
		postFilterChangeListeners.remove(listener);
	}

	/**
	 * Is dynamic simulation enabled for the given design, for it to be true both dynamic simulation must be on and the
	 * given design must be part of the current scope
	 *
	 * @param object, the object to be compared
	 *
	 * @return true, if it forms part of the current scope
	 */
	public boolean isDynamicSimulationEnabled(IUIDObject object)
	{
		if (isDynamicSimulationEnabled()) {
			if (currentScope != null) {
				if (!currentScope.isInScope(object)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isValidInScope(IUIDObject object)
	{
		// Check that the object is visible on the design (i.e. the object is not filtered out).
		IDesignContainer parentDesign = getDesign(object);
		return LogicFilterControlMgr.getInstance().isObjectVisibleToAnalysis(object, parentDesign);
	}

	@Override public boolean isObjectVisible(IUIDObject object)
	{
		// Check that the object is visible on the design (i.e. the object is not filtered out).
		IDesignContainer parentDesign = getDesign(object);
		return LogicFilterControlMgr.getInstance().isObjectVisible(object, parentDesign);
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 *
	 * @param model, the model being simulated.
	 */
	public void updateSimulation(Model model)
	{
		updateSimulation(model, false);
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 * <p>
	 * A simulation may be forced i.e. this method resimulates event i.e. does not check for background mode. This is
	 * used by the SimulateAction and ResetActions to ensure the diagram is consistent with their settings.
	 *
	 * @param model, the model being simulated.
	 * @param force, force a resimulation ( for use by the reset and simulate actions )
	 */
	public void updateSimulation(Model model, boolean force)
	{
		updateSimulation(model, force, null);
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 * <p>
	 * A simulation may be forced i.e. this method resimulates event i.e. does not check for background mode. This is
	 * used by the SimulateAction and ResetActions to ensure the diagram is consistent with their settings.
	 *
	 * @param model, the model being simulated.
	 * @param force, force a resimulation ( for use by the reset and simulate actions )
	 * @param originalUid, if the simulation is cancelled whilst it is starting this uid may be used to cancel the
	 * simulation. Typically this will be null unless called by the CAF Actions. See DynSimBackgroundAction
	 */
	public void updateSimulation(Model model, boolean force, @Nullable String originalUid)
	{
		if (model.getDesign() == null) {
			if (dynSimControllers != null && originalUid != null) {
				dynSimControllers.remove(originalUid);  // may be better to turn the simulation
				// off here via handleSimulationTransitions TODO RH
			}
			return; // this may happen as a result of a project being closed whilst
			// a simulation is started.
		}
		String uid = model.getDesign().getUID().getString();
		if (getCurrentAnalysisNetlistScope() != null) {
			uid = getCurrentAnalysisNetlistScope().getUid();
		}
		DynSimController ctrl = dynSimControllers.get(uid);
		updateSimulation(ctrl, force);
	}

	protected void updateSimulation(DynSimController ctrl, boolean force)
	{
		// if we have a controller to do the simulation and if we're in BACKGROUND
		// simulation mode or we are forcing the simulatin do it
		if (ctrl != null && (ctrl.getMode() == DYN_SIM_BACKGROUND || force)) {
			updateSimulation(ctrl);
		}
		else if (ctrl != null && ctrl.getMode() == DYN_SIM_DEMAND) {
			// else a change has occured so just note it
			setSimulationPropertiesModified(true);
		}
		ICapletController activeController = CAFUtils.getInstance().getActiveCapletController();
		if (activeController instanceof BaseController) {
			AnalysisBrowserPanel analysisBrowserPanel =
					((BaseController) CAFUtils.getInstance().getActiveCapletController()).getAnalysisBrowserPanel();
			if (analysisBrowserPanel != null) {
				analysisBrowserPanel.updateTable();
			}
		}
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 * <p>
	 * A simulation may be forced i.e. this method resimulates event i.e. does not check for background mode. This is
	 * used by the SimulateAction and ResetActions to ensure the diagram is consistent with their settings.
	 *
	 * @param scope, the scope being simulated.
	 * @param force, force a resimulation ( for use by the reset and simulate actions )
	 */
	public void updateSimulation(IAnalysisNetlistScope scope, boolean force)
	{
		String uid = scope.getUid();
		DynSimController ctrl = dynSimControllers.get(uid);
		updateSimulation(ctrl, force);
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 *
	 * @param scope, the model being simulated.
	 */
	public void updateSimulation(IAnalysisNetlistScope scope)
	{
		updateSimulation(scope, false);
	}

	/**
	 * This method updates the simulation given the model and the session controller. This is called when a model has
	 * changed or a property is changed if the dyn sim mode is BACKGROUND.
	 *
	 * @param ctrl, the Dynamic Simulation Controller
	 */
	protected void updateSimulation(DynSimController ctrl /*, Model model*/)
	{
		String uid = ctrl.getUID();

		try {
			IAnalysisSimulationSessionController session = ctrl.getSession();
			if (ctrl.getColorer() == null) {
				ctrl.setColorer(new DynSimColorer(uid));
			}

			// inform that we are simulating
			((IAnalysisSimulationSessionClient) ctrl.getColorer()).startingSimulation(uid);

			synchronized (session) {
				// stop any running simulations
				session.endSimulation(uid);

				// set the client for callbacks
				session.setAnalysisSimulationSessionClient(uid, (IAnalysisSimulationSessionClient) ctrl.getColorer());
				String netlist = ctrl.exportNetlist();//exportNetList( model.getDesign( ) ) ;

				// log the netlist as required
				if ("true".equals(System.getProperty("debug.AnalysisNetlist.log", "false"))) {
					System.err.println("***** Dynamic Sim Netlist ***** ");
					System.err.println(netlist);
					System.err.println("******************************* ");
				}

				// are we to use the cached netlisting technique ? This offers performance gains
				// over reloading potentially identical netlists each simulation against extra
				// memory usage. We could / should limit this memory usage....
				boolean useCachedNetlist = "true".equals(System.getProperty("AnalysisNetlist.dynamic.cache", "true"));
				// reload the netlist as required
				if (useCachedNetlist) {
					if (netlist != null && !netlist.equals(ctrl.getLastNetlist())) {
						session.reload(uid, StringUtils.getBytes(netlist));
						ctrl.setLastNetlist(netlist);
						//chs.utilities.ui.MessageHelper.showInformationMessage( null, "Netlist", "Reloaded" ) ;
					}
				}
				else {
					if (netlist != null) {
						session.reload(uid, StringUtils.getBytes(netlist));
					}
				}

				//System.err.println("Reload successful (CLogic) = " + reloadSuccessful ) ;

				// do the simulation
				session.resetCatastrophicFailures(uid);
				session.simulate(uid, ctrl.getMode() == DYN_SIM_BACKGROUND);

				// unset the callbacks
				session.setAnalysisSimulationSessionClient(uid, null);
			}
		}
		catch (RuntimeException ignore) {
			System.err.println("LogicAnalysisServices : Could not update the simulation");
		}
	}

	/**
	 * This method updates the coloring info ( redraws the transient gfx ) when a model may have changed but
	 * re-simulation has not occurred
	 *
	 * @param ctrl, the dynamic simulation controller
	 *
	 * @return boolean, could we recolor the design or was the cached coloring info invalid
	 */
	protected boolean updateColoring(DynSimController ctrl)
	{
		if (ctrl != null && ctrl.getColorer() != null) {
			return ((DynSimColorer) ctrl.getColorer()).updateColoring();
		}
		return false;
	}

	public DynSimColorer createTestColorer(String uid)
	{
		return new DynSimColorer(uid);
	}

	private static class RenamedObject
	{

		private String oldName;
		private String newName;

		private RenamedObject(String newName, String oldName)
		{
			this.newName = newName;
			this.oldName = oldName;
		}

		public String getNewName()
		{
			return newName;
		}

		public String getOldName()
		{
			return oldName;
		}
	}

	/**
	 * This allows the AnalysisServices class to be notified of any changes to any models and handle them accordingly.
	 *
	 * @param e, the model that has changed
	 */
	public void modelChanged(ModelChangeEvent e)
	{
		Iterator<IUID> changes = e.getChangedObjectsUIDs().iterator();

		Set<RenamedObject> renamedLogicObjects = new LinkedHashSet<RenamedObject>();
		Set<String> deletedLogicObjectNames = new LinkedHashSet<String>();

		ICapletModel model = e.getModel();
		IDesign design = ((ILogicModel) model).getDesign();
		if (design != null && getCurrentAnalysisNetlistScope() != null &&
				!getCurrentAnalysisNetlistScope().isInScope(design)) {
			notifyPostModelChangeListeners(e);
			return; // do nothing as we're not looking at the same scope...
		}
		boolean propertyOrAttributeChange = false;
		while (changes.hasNext()) {
			IUID uid = changes.next();
			IUIDObject obj = UIDMgr.getObject(uid);
			ILogicObject deletedLogicObject = null;
			// If the object is a VoidUndoObjectHelper it means that the
			// object has been deleted.
			if (obj instanceof VoidUndoObjectHelper) {
				IUIDObject deletedObject = ((IDeletedObject) obj).getOriginalObject();
				if (deletedObject instanceof ILogicObject) {
					deletedLogicObject = (ILogicObject) deletedObject;
				}
			}
			else if (obj instanceof IProperty || (obj instanceof IAttributeProvider && obj instanceof IAnalysable)) {
				propertyOrAttributeChange = true;
				continue;
			}

			if (getCurrentAnalysisNetlistScope() != null) {
				INamedPropertiedObject namedObj =
						deletedLogicObject != null ? deletedLogicObject :
								(obj instanceof ILogicObject ? (INamedPropertiedObject) obj : null);

				if (namedObj != null) {
					if (namedObj instanceof IBlockDevice) {
						//reset the scoped component so that it get calculated again
						getCurrentAnalysisNetlistScope().resetScopedComponentsList();
						populateAssociatedDesignObject(namedObj, deletedLogicObject, deletedLogicObjectNames,
								renamedLogicObjects);
					}
					else {
						String uidString = uid.getString();
						checkInScopedComponents(namedObj, deletedLogicObject, deletedLogicObjectNames,
								renamedLogicObjects, uidString);
					}
				}
			}
		}

		if ((!renamedLogicObjects.isEmpty() || !deletedLogicObjectNames.isEmpty() || propertyOrAttributeChange ||
				!e.getNewObjectsUIDs().isEmpty()) &&
				model instanceof Model) {

			// if anything important has changed at this point get the scope and allow it to update the list
			// of components available in the scope (and their attachments etc). If we're moving or stretching
			// then we don't need to re-netlist the scope... dts545317
			if (getCurrentAnalysisNetlistScope() != null && !isManipulatorActive(model)) {
				boolean areWatchesPresent = !getControlPanel().getWatches().isEmpty();
				if (areWatchesPresent) {
					if (!renamedLogicObjects.isEmpty()) {
						for (RenamedObject obj : renamedLogicObjects) {
							getControlPanel().renameWatches(obj.getOldName(), obj.getNewName());
						}
					}

					if (!deletedLogicObjectNames.isEmpty()) {
						for (String deletedObjectName : deletedLogicObjectNames) {
							getControlPanel().removeWatches(deletedObjectName);
						}
					}
				}

				getCurrentAnalysisNetlistScope().resetScopedComponentsList();
			}

			Model theModel = (Model) model;

			//String uid = theModel.getDesign( ).getUID( ).toString( ) ;
			String uid = (getCurrentAnalysisNetlistScope() != null) ?
					getCurrentAnalysisNetlistScope().getUid() : theModel.getDesign().getUID().toString();
			DynSimController ctrl = dynSimControllers.get(uid);
			if (ctrl != null && ctrl.getMode() == DYN_SIM_BACKGROUND) {

				// we don't really care what objects have changed but we need to
				// do a re-netlist and simulate
				getControlPanel().addMessage(ResourceMgr.getString(LogicAnalysisServices.class,
						"LogicAnalysisServices.string.designModified"));
				// update the simulation
				updateSimulation(ctrl /*, theModel*/);
			}
			else if (ctrl != null && ctrl.getMode() == DYN_SIM_DEMAND) {
				// mark sim as modified.
				setSimulationPropertiesModified(true);

				// update the coloring if anythings been deleted to remove
				// the transient gfx for that object...
				if (!deletedLogicObjectNames.isEmpty()) {
					updateColoring(ctrl);
				}
			}
		}
		else if (model instanceof Model) {
			// should we recolor the model here ? If things have moved it may be required,
			// we'd need to repeat the last coloring op.....
			// Hmmmmm,,,,
			String uid = (getCurrentAnalysisNetlistScope() != null) ?
					getCurrentAnalysisNetlistScope().getUid() : ((ILogicModel) model).getDesign().getUID().toString();
			DynSimController ctrl = dynSimControllers.get(uid);
			if (ctrl != null && ctrl.getMode() != DYN_SIM_OFF) {
				updateColoring(ctrl);
			}
		}
		notifyPostModelChangeListeners(e);
	}

	private void notifyPostModelChangeListeners(ModelChangeEvent e)
	{
		// ensure that all the listeners that require post model change notification
		// get it
		if (!postModelChangeListeners.isEmpty()) {
			for (IModelChangeListener listener : postModelChangeListeners) {
				listener.modelChanged(e);
			}
		}
	}

	private void populateAssociatedDesignObject(INamedPropertiedObject namedObj,
			@Nullable ILogicObject deletedLogicObject,
			Set<String> deletedLogicObjectNames,
			Set<RenamedObject> renamedLogicObjects)
	{
		if (deletedLogicObject == null && deletedLogicObjectNames.isEmpty()) {
			currentScope.updateScopeForBlock();
		}
		IBlockDevice blockDev = (IBlockDevice) namedObj;
		IDesign assocBlockDes = blockDev != null ? (IDesign) blockDev.getAssociatedDesign(null) : null;
		if (assocBlockDes != null && assocBlockDes.getConnectivity() != null) {
			// TODO : rharring, sram optimise this code to work across a set of uids
			for (IDevice iDevice : assocBlockDes.getConnectivity().getAllDevices()) {
				String uidString = iDevice.getUID().getString();

				checkInScopedComponents(namedObj, deletedLogicObject, deletedLogicObjectNames, renamedLogicObjects,
						uidString);
			}
			for (IConductor iConductor : assocBlockDes.getConnectivity().getConductors()) {
				String uidString = iConductor.getUID().getString();

				checkInScopedComponents(namedObj, deletedLogicObject, deletedLogicObjectNames, renamedLogicObjects,
						uidString);
			}
		}
	}

	private void checkInScopedComponents(INamedPropertiedObject namedObj, @Nullable ILogicObject deletedLogicObject,
			Set<String> deletedLogicObjectNames, Set<RenamedObject> renamedLogicObjects,
			String uidString)
	{
		List<ScopeComponent> components =
				getCurrentAnalysisNetlistScope().getScopedComponents();

		for (ScopeComponent sC : components) {
			if (uidString.equals(sC.getUid())) {
				if (deletedLogicObject != null) {
					deletedLogicObjectNames.add(sC.getName());
				}
				else {
					String oldName = sC.getName();
					String newName = namedObj.getName();
					if (!newName.equals(oldName)) {
						renamedLogicObjects.add(new RenamedObject(newName, oldName));
					}
				}
			}
		}
	}

	private boolean isManipulatorActive(ICapletModel model)
	{
		SelectAction action = getSelectAction(model);
		return action != null && action.isManipulatorActive();
	}

	@Nullable private SelectAction getSelectAction(ICapletModel model)
	{
		return (SelectAction) model.getController().getAction(SelectAction.class);
	}

	/**
	 * Indication a model will change BEFORE it has changed.
	 * <p>
	 * Ignored.
	 */
	public void modelPreChanged(ModelChangeEvent mce)
	{
		//System.err.println("Pre change" ) ;
		//objectCount = mce.getChangedObjectsUIDs().size( ) ;
		//System.err.println("Object coutn is : " + objectCount ) ;
	}

	public void filterChanged(IGraphicsFilterControl filterControl)
	{
		updateSimulationAfterFilterChange();
		// ensure that all the listeners that require post filter change notification
		// get it
		if (!postFilterChangeListeners.isEmpty()) {
			for (IGraphicsFilterChangeListener listener : postFilterChangeListeners) {
				listener.filterChanged(filterControl);
			}
		}
	}

	private void updateSimulationAfterFilterChange()
	{
		SwingUtilities.invokeLater(() -> {
					IAnalysisNetlistScope scope = AnalysisServices.getCurrentAnalysisNetlistScope();
					if (scope != null && isAnalysisActive(scope)) {
						DynSimController controller = getDynSimController(scope.getUid());
						if (controller != null) {
							//noinspection ConstantConditions
							setDynamicSimulationMode(scope.getUid(), null, DYN_SIM_OFF, null);
						}
					}
				}
		);
	}

	/**
	 * This class can be passed as a client to the simulation session to allow the design to be colored once a
	 * simulation has been completed
	 */
	public class DynSimColorer implements AbstractSimColorer, IAnalysisSimulationSessionClient
	{

		//Model m_model ;
		protected Map<String, String> tooltips;
		protected IAnalysisColoringProcessor processor;
		protected byte[] lastColoringUpdate;
		protected String m_uid;

		public DynSimColorer(String uid)
		{

			m_uid = uid;
			lastColoringUpdate = null;
			initializeColoringProcessors();
		}

		public void showLicenseError(Exception e)
		{
			e.printStackTrace();
		}

		/**
		 * This method repeats the last coloring operation..
		 *
		 * @return boolean, false if there was no previous coloring info to use
		 */
		public boolean updateColoring()
		{
			if (lastColoringUpdate != null) {
				updateColoring(m_uid, lastColoringUpdate);
				return true;
			}
			return false;
		}

		/**
		 * Update the coloring.
		 */
		public void updateColoring(String uid, byte[] coloring)
		{

			// we cache the coloring results by default but it may be desirable to be able to turn
			// this caching off as on large designs this may cause memory issues. If we do turn them
			// off then we run the risk of not re-coloring the diagrams correctly when a component is
			// moved to another location..... see modelChanged in LogicAnalysisServices /
			// updateColoring in the same class and updateColoring here...
			if ("true".equals(System.getProperty("AnalysisColoring.dynamic.cache", "true"))) {
				lastColoringUpdate = coloring;
			}

			// Need to update here...
			//IDynamicGfxService dgs = m_model.getDynamicGfxService();
			//dgs.removeAllTransientGfx();
			clearColoring();

			if ("true".equals(System.getProperty("debug.AnalysisColoring.log", "false"))) {
				System.err.println("-------- Analysis Coloring --------");
				System.err.println(StringUtils.newString(coloring));
				System.err.println("-----------------------------------");
			}

			// sanity, may occur if dynamically simulating un-netlistable design
			if (coloring == null) {
				return;
			}

			try {
				//LogicAnalysisColoringImporter importer = new LogicAnalysisColoringImporter( m_model ) ;
				//AnalysisColoringImporter importer = new AnalysisColoringImporter( new LogicAnalysisColoringProcessor( m_model ) ) ;
				AnalysisColoringImporter importer = new AnalysisColoringImporter(processor);
				importer.importStream(new ByteArrayInputStream(coloring));
				AnalysisSimulationResultsImporter simResImporter = new AnalysisSimulationResultsImporter();
				simResImporter.importStream(new ByteArrayInputStream(coloring));

				if (resultsConsumers != null) {
					for (IAnalysisSimulationResultsConsumer con : resultsConsumers) {
						con.update(simResImporter);
					}
				}
				tooltips = simResImporter.getToolTips();

				AnalysisVariableImporter variableImporter = new AnalysisVariableImporter();
				variableImporter.importStream(new ByteArrayInputStream(coloring));
				getAnalysisServices().setSimulationResultVariables(uid, variableImporter.getComponentVariables());
				updateWatches(uid);
				//showColoring( ) ;
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			// ensure tooltips are shown for design, this is to be sure
			// no diagrams are missed as in DR9808
			//       if ( tooltips.size() > 0 ) {
			//          setTooltipsEnabledForDesign( m_model.getDesign( ), true ) ;
			//       }

		}

		public void showColoring()
		{
			processor.showColoring();
			// Refresh view
			//ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			//if (view != null) {
			//   view.invalidate(IViewInvalidationEnum.eTransient);
			//}

		}

		public void clearColoring()
		{
			processor.clearColoring();
			//IDynamicGfxService dgs = m_model.getDynamicGfxService();
			//dgs.removeAllTransientGfx();
			// Refresh view
			//ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			//if (view != null) {
			//   view.invalidate(IViewInvalidationEnum.eTransient);
			//}
		}

		public void startingSimulation(String scopeUID)
		{
			setSimulationInProgress(scopeUID, true);
		}

		public void stoppingSimulation(String scopeUID)
		{
			setSimulationInProgress(scopeUID, false);
		}

		public Map<String, String> getToolTips()
		{
			return tooltips;
		}

		public void reinitializeColoringProcessors()
		{
			initializeColoringProcessors();
		}

		protected void initializeColoringProcessors()
		{
			IAnalysisColoringProcessor tmpProcessor = null;
			if (currentScope != null) {
				tmpProcessor = LogicAnalysisColoringProcessor.staticGetScopedColoringProcessor(currentScope);
			}

			//noinspection rawtypes
			processor = (IAnalysisColoringProcessor) Proxy
					.newProxyInstance(IAnalysisColoringProcessor.class.getClassLoader(),
							new Class[]{IAnalysisColoringProcessor.class},
							new SafeAnalysisColorProcessorHandler(tmpProcessor));
		}

		private class SafeAnalysisColorProcessorHandler implements InvocationHandler
		{

			private IAnalysisColoringProcessor m_coloringProcessor;

			private SafeAnalysisColorProcessorHandler(
					@Nullable IAnalysisColoringProcessor coloringProcessor)
			{
				m_coloringProcessor = coloringProcessor;
			}

			@Nullable @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{
				if (m_coloringProcessor != null) {
					return method.invoke(m_coloringProcessor, args);
				}
				return null;
			}
		}
	}

	private void setSimulationInProgress(String scopeUID, boolean simulationInProgress)
	{
		IAnalysisSimulationStatusIndicator status =
				AnalysisServices.getSimulationStatusIndicator(scopeUID);
		if (status != null) {
			status.simulationInProgress(simulationInProgress);
		}
	}

	/**
	 * This method gets the simulate action and sets its modified propery based on the setting given.
	 *
	 * @param modified, have the inputs / failures / model changed since we last simulated.
	 */
	public void setSimulationPropertiesModified(boolean modified)
	{
		//AppAction action2 = CAFUtils.getInstance().getActionUI(
		//   chs.caplets.logic.actions.analysis.SimulateActionUI.class.getName( ) ) ;
		//System.err.println( "AUI  " + action2 ) ;

		// sanity, if the user closes all before we're finished
		if (CAFUtils.getInstance().getActiveCapletController() == null) {
			return;
		}

		IActionUI action = CAFUtils.getInstance().getActiveCapletController().getCaplet().getActionUI(
				SimulateActionUI.class.getName());
		if (action != null) {
			((SimulateActionUI) action).setIndicateModified(modified);
		}
		else {
			System.err.println("Sim properties modified but NO ACTION ");
		}
	}

	/**
	 * This method creates the control panel
	 */
	public void createControlPanel()
	{
		controlPanel = new AnalysisControlPanel(new UIDHyperlinkListener());
		controlPanel.addWatchPaneMouseListener(new WatchPaneMouseListener());
	}

	/**
	 * This method installs the Capital Analysis window to the output tabs at the bottom of the tool.
	 */
	public static void installAnalysisOutputWindow()
	{
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();

		boolean paneExists = outputWindow.paneExists(outputWindowName);
		if (paneExists && (outputWindow.getPane(outputWindowName) != getAnalysisServices().getControlPanel())) {
			outputWindow.removePane(outputWindowName);
			paneExists = false;
		}
		if (!paneExists) {
			outputWindow.addComponentPane(outputWindowName, getAnalysisServices().getControlPanel(), false);
			getAnalysisServices().getControlPanel().setMode(AnalysisControlPanel.NORMAL_MODE);
		}
	}

	public boolean isControlPanelInstalled()
	{
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		Component pane = outputWindow.getPane(outputWindowName);
		return pane != null && pane.equals(getAnalysisServices().getControlPanel());
	}

	private void clearAnalysisOutputWindow()
	{
		getAnalysisServices().getControlPanel().setMessageDocs(null);
	}

	/**
	 * This method attempts to add the given action to the given menu parsing the path to find the appropriate location
	 * for the action entry. The path is tokenized on ','.
	 * <p>
	 * If elements in the path are found they are reused if not they are created.
	 *
	 * @param container, the menu container to add all actions and containers to
	 * @param action, the action to add
	 * @param path, the path to the actions parent i.e. Analysis,Tools
	 */
	public static void addActionToMenu(ActionContainer container, Action action, String path)
	{
		addActionToMenu(container, action, path, ActionContainer.INSERT_END);
	}

	/**
	 * This method attempts to add the given action to the given menu parsing the path to find the appropriate location
	 * for the action entry. The path is tokenized on ','.
	 * <p>
	 * If elements in the path are found they are reused if not they are created.
	 *
	 * @param container, the menu container to add all actions and containers to
	 * @param action, the action to add
	 * @param path, the path to the actions parent i.e. Analysis,Too
	 * @param insertPoint, the point to insert the action to the menu
	 */
	public static void addActionToMenu(ActionContainer container, Action action, String path, int insertPoint)
	{
		if (action == null) {
			return;
		}
		addActionEntryToMenu(container, new ActionEntry(action), path, insertPoint);
	}

	/**
	 * This method attempts to add the given action to the given menu parsing the path to find the appropriate location
	 * for the action entry. The path is tokenized on ','.
	 * <p>
	 * If elements in the path are found they are reused if not they are created.
	 *
	 * @param container, the menu container to add all actions and containers to
	 * @param actionEntry, the action entry to add
	 * @param path, the path to the actions parent i.e. Analysis,Too
	 * @param insertPoint, the point to insert the action to the menu
	 */
	public static void addActionEntryToMenu(ActionContainer container, IActionNode actionEntry, String path,
			int insertPoint)
	{
		// setup the tokenizer
		StringTokenizer sT = new StringTokenizer(path, ",", false);

		// really need to do something more useful about icons ( check action etc... )
		Icon blankIcon = CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif");

		//System.err.println("Asked to add " + path ) ;
		//System.err.println(" Action      " + actionEntry ) ;
		ActionContainer parentContainer = container;
		while (sT.hasMoreTokens()) {
			String name = sT.nextToken();
			ActionNodeIterator iter = parentContainer.getMembers();
			ActionContainer selected = null;
			while (iter.hasNext() && selected == null) {
				IActionNode node = iter.getNext();

				// we're always looking for containers rather than nodes or separators
				if (node instanceof ActionContainer) {
					if (((Action) node).getValue(Action.NAME).equals(name)) {
						selected = (ActionContainer) node;
					}
				}
			}

			// if we ain't got one create one
			if (selected == null) {
				ActionContainer c = new ActionContainer(name, true, blankIcon);
				// if the action here is null then we're simply adding the container and
				// may be doing so according to insertion point so
				// if ( action == null ) {
				//    parentContainer.setInsertionPoint( insertPoint ) ;
				parentContainer.add(c);
				// }
				parentContainer = c;
			}
			else {
				parentContainer = selected;
			}
		}
		// need to check if it already contains ????
		// if ( action != null && ! parentContainer.contains( action ) ) {
		parentContainer.setInsertionPoint(insertPoint);
		//parentContainer.add( new ActionEntry( action ) ) ;

		if (actionEntry instanceof ActionEntry) {
			Action a = ((ActionEntry) actionEntry).getAction();
			if (a.getValue(Action.SMALL_ICON) == null) {
				a.putValue(Action.SMALL_ICON, blankIcon);
			}
		}

		parentContainer.add(actionEntry);
		// }
	}

	public void setDynamicSimulationModel(String uid, String subsystemModelPath, int mode)
	{
		setDynamicSimulationMode(uid, subsystemModelPath, mode, null);
	}

	/**
	 * This method handles the dynamic simulation mode being set
	 */
	public void setDynamicSimulationMode(String uid, String subsystemModelPath, int mode, @Nullable String simType)
	{
		// if the new mode is not off then make sure we've installed the control panel
		if (mode != DYN_SIM_OFF) {
			installAnalysisOutputWindow();
		}
		else {
			clearAnalysisOutputWindow();
		}
		super.setDynamicSimulationMode(uid, subsystemModelPath, mode, simType);
	}

	/**
	 * This method handles the transitions between different simulation modes
	 *
	 * @param startMode, the mode we are currently in
	 * @param newMode, the mode we are changing too
	 */
	public void handleSimulationModeTransition(int startMode, int newMode, DynSimController ctrl)
	{

		super.handleSimulationModeTransition(startMode, newMode, ctrl);

		handleUISimulationModeTransition(startMode, newMode);

		if (newMode == DYN_SIM_DEMAND && startMode == DYN_SIM_BACKGROUND) {
			// clear the coloring...
			ctrl.getColorer().clearColoring();
		}
	}

	/**
	 * This method handles the ui updates required upon a change in the dynamic simulation mode.
	 *
	 * @param startMode, the mode the sim controller was in to start
	 * @param newMode, the mode the sim controller is in noe
	 */
	protected void handleUISimulationModeTransition(int startMode, int newMode)
	{
		//System.err.println( "In handleUISimulationModeTransition sm " + startMode + " nm " + newMode ) ;
		// in case the dyn sim is cancelled other than via the selection buttons
		if ( /*newMode == DYN_SIM_OFF && */ startMode != newMode) {
			// need to update the dyn sim actions / drawer
			// so get the 3 actions.
			if (getCurrentAnalysisNetlistScope() == null) {
				ICAFWindow window = CAFUtils.getInstance().getActiveWindow();
				if (window != null) {
					processWindow(window, startMode, newMode);
				}
			}
			else {
				for (ICAFWindow window :CAFUtils.getInstance().getWindowMgr().getWindows()) {
					processWindow(window, startMode, newMode);
				}
			}
		}
	}

	protected void processWindow(ICAFWindow window, int startMode, int newMode)
	{

		Model updateModel = null;
		// we need to be sure about the windows we're looking at!
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null && window instanceof ICapletWindow) {

			ICapletWindow cWindow = (ICapletWindow) window;
			if (cWindow.getController() == null) {  // if there is no controller...
				return;
			}
			ICapletModel model = cWindow.getController().getCapletModel();
			if (!((model instanceof Model) &&
					AnalysisServices.getCurrentAnalysisNetlistScope().isInScope(((ILogicModel) model).getDesign()))) {
				return;
			}

			updateModel = (Model) model;
		}
		ICaplet caplet = window.getCaplet();
		DynSimOffActionUI simOffAction =
				(DynSimOffActionUI) caplet.getActionUI("chs.caplets.logic.actions.analysis.DynSimOffActionUI");
		DynSimOnDemandActionUI simDemandAction = (DynSimOnDemandActionUI) caplet
				.getActionUI("chs.caplets.logic.actions.analysis.DynSimOnDemandActionUI");
		DynSimBackgroundActionUI simBackAction = (DynSimBackgroundActionUI) caplet
				.getActionUI("chs.caplets.logic.actions.analysis.DynSimBackgroundActionUI");

		// if we can't get the above actions return...
		if (simOffAction == null || simDemandAction == null || simBackAction == null) {
			return;
		}

		// alter their settings
		String key = updateModel != null ? simOffAction.getDrawerPrefix(updateModel) : Drawer.DRAWER_SELECTED;

		boolean force = updateModel == null;

		simOffAction.putValue(key, newMode == DYN_SIM_OFF, force);
		simDemandAction.putValue(key, newMode == DYN_SIM_DEMAND, force);
		simBackAction.putValue(key, newMode == DYN_SIM_BACKGROUND, force);

		Action simAction = null;
		switch (newMode) {
			case DYN_SIM_DEMAND:
				simAction = simDemandAction;
				break;
			case DYN_SIM_BACKGROUND:
				simAction = simBackAction;
				break;
			default:
				simAction = simOffAction;
		}

		// only the selected action has a prop change listener so we need to fire
		// off that
		if (startMode == DYN_SIM_BACKGROUND) {
			simBackAction.updateSelection(simAction);
		}
		else if (startMode == DYN_SIM_DEMAND) {
			simDemandAction.updateSelection(simAction);
		}
		else if (startMode == DYN_SIM_OFF) {
			simOffAction.updateSelection(simAction);
		}
	}

	/**
	 * This method registers the analysis services object as a tool tip provider to the view.
	 *
	 * @param view, the view onto which the services should provide tool tips.
	 */
	public void installColoringTooltips(ICapletView view)
	{
		view.setToolTipProvider(this);
		ToolTipManager.sharedInstance().setDismissDelay(tooltipMaxDelay > 0 ? tooltipMaxDelay :
				DEFAULT_TOOLTIP_MAX_DELAY);
	}

	/**
	 * This method unregisters the analysis services object as a tool tip provider to the view.
	 *
	 * @param view, the view onto which the services should stop providing tool tips.
	 */
	public void uninstallColoringTooltips(ICapletView view)
	{
		view.setToolTipProvider(null);
		// make sure to restore to default tooltip duration -- will have
		// previously been changed for the logic analysis tooltips to increase
		// duration allowing user more control.
		ToolTipManager.sharedInstance().setDismissDelay(DISMISS_DELAY);
	}

	/**
	 * This method enabled / disables the tooltips for all views of the given scope...
	 *
	 * @param scope, the scope containing the designs for which tooltips should be enabled.
	 * @param enabled, should we enable the tooltips
	 */
	public void setTooltipsEnabledForScope(IAnalysisNetlistScope scope, boolean enabled)
	{
		IUIDIterator iterator = scope.getDesignUIDs();
		while (iterator.hasNext()) {
			IUID designUID = iterator.getNext();
			setTooltipsEnabledForDesign(designUID, enabled);
		}
	}

	/**
	 * This method enables / disables the tooltips for all views on the given design..
	 *
	 * @param design, the design to look for
	 * @param enabled, should we enable the tooltips
	 */
	public void setTooltipsEnabledForDesign(IUID designUID, boolean enabled)
	{
		// get all the windows ( views )
		List<ICAFWindow> windows = CAFUtils.getInstance().getWindowMgr().getWindows();

		// sanity, if the user closes all before we're finished
		if (windows == null) {
			return;
		}

		for (ICAFWindow window : windows) {
			if (window instanceof ICapletWindow) {
				ICapletView view = ((ICapletWindow) window).getCurrentView();

				// sanity
				if (view == null) {
					continue;
				}
				/*System.err.println("View is " + view ) ;
										  System.err.println("Design is " + design ) ;
										  System.err.println("Scope is " + getCurrentAnalysisNetlistScope( ) ) ;
										  System.err.println("Enable is " + enabled ) ;*/
				// if the view is on the design
				if (view.getCapletModel() instanceof Model &&
						((ILogicModel) view.getCapletModel()).getDesign().getUID().toString()
								.equals(designUID.toString())) {

					// set tooltips to the enabled state
					if (enabled) {
						installColoringTooltips(view);
					}
					else {
						uninstallColoringTooltips(view);
					}
				}
			}
		}
	}

	public void showPropertiesFor(String uid, String component)
	{
		//System.err.println("uid is " + uid + " component is " + component  );
		//System.err.println("showPropertiesFor( uid, component ) needs to be subclassed.") ;
		Map<String, Map<String, String>> simVariables = getSimulationResultVariables(uid);
		if (simVariables == null) {
			MessageHelper.showWarningMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(LogicAnalysisServices.class, "Watch.properties.error.header"),
					ResourceMgr.getString(LogicAnalysisServices.class, "Watch.properties.error.message"));
			return;
		}
		//System.err.println("Sim variables are " + simVariables ) ;
		Map<String, String> componentVariables = simVariables.get(component);
		if (componentVariables != null) {
			Iterator<String> e = componentVariables.keySet().iterator();
			List<List<String>> tableData = new ArrayList<List<String>>();
			while (e.hasNext()) {
				String key = e.next();
				String data = componentVariables.get(key);
				List<String> row = new ArrayList<String>();
				row.add(key);
				row.add(data);
				tableData.add(row);
			}
			AnalysisPropertiesDialog dialog = new AnalysisPropertiesDialog(uid, tableData, component);
			dialog.setVisible(true);
		}
	}

	// /////////////////////// //
	// IDisplayContextListener //
	// /////////////////////// //

	public void postWindowChanged(WindowChangeEvent wce)
	{
	}

	public void viewChanged(ViewChangeEvent vce)
	{
	}

	// we use the window changing to ctx switch in the AnalysisCtrlPanel
	public void windowChanged(WindowChangeEvent wce)
	{
		ICapletView newView = null;
		ICAFWindow newWindow = wce.getNewWindow();
		if (newWindow instanceof ICapletWindow) {
			newView = ((ICapletWindow) newWindow).getCurrentView();
		}

		// work out the uid
		String uid = null;
		if (getCurrentAnalysisNetlistScope() != null) {
			uid = getCurrentAnalysisNetlistScope().getUid();
		}
		else {
			if (newView != null && newView.getCapletModel() instanceof Model) {
				uid = ((ILogicModel) newView.getCapletModel()).getDesign().getUID().toString();
			}
		}

		if (newView != null && uid != null) {
			updateControlPanel(uid);
		}

		// update the tool tips to ensure on any new diagrams whose design
		// is already being simulated tool tips are present.
		if (newView != null && newView.getCapletModel() instanceof Model) {
			DynSimController ctrl = null;

			if (getCurrentAnalysisNetlistScope() != null) {
				// need to see if the design's uid is active. This will be set by the scope
				// but we may have been checking the scope's uid earlier....
				uid = ((ILogicModel) newView.getCapletModel()).getDesign().getUID().toString();
				if (isAnalysisActive(uid)) {
					installColoringTooltips(newView);
					ctrl = dynSimControllers.get(getCurrentAnalysisNetlistScope().getUid());
				}
			}
			else if (isAnalysisActive(uid)) { // no scope so check design's uid..not likely to happen...
				installColoringTooltips(newView);
				ctrl = dynSimControllers.get(uid);
			}

			// if the dynamic simulation controller is not null then update the coloring.
			if (ctrl != null) {
				// we re initialize the processors to account for the possibility of
				// adding a model
				if (ctrl.getColorer() != null) {
					((DynSimColorer) ctrl.getColorer()).reinitializeColoringProcessors();

					// now update the coloring...
					updateColoring(ctrl);
					if (isAnalysisActive(uid)) {
						installColoringTooltips(newView);
					}
				}
			}
		}
	}

	// //////////////////////// //
	// IToolTipProvider methods //
	// //////////////////////// //

	/**
	 * This method provides tool tip text for the specified model and selection.
	 *
	 * @param model, the model for which we want the tool tip
	 * @param selection, the selection for which we want the tool tip
	 *
	 * @return String, the text to display or null ( not "" ) .
	 */
	@Override @Nullable
	public String getToolTipText(@NotNull ICapletModel model, @NotNull SelectSet selection, @NotNull MouseEvent me)
	{
		// remove any Text ones + pins, wasn't sure the selection filter was doing as I wanted so
		// this 'hack' exists. Worth looking at again. ROH
		if (selection.getSelectCount() > 0) {
			SelectedUIDObjectIterator iter = selection.getSelectedUIDObjects();
			//Iterator iter = selection.getSelected() ;
			while (iter.hasNext()) {
				//Selection sel = (Selection) iter.next() ;
				IUIDObject sel = iter.getNext();
				//System.err.println("Sel is : " + sel.getClass( ).getName( ) ) ;
				//if ( sel.getSelectionClass( ) == chs.cof.draw.Text.class ) {
				if (sel instanceof IText || sel instanceof IPin || sel instanceof ISchemStackPin ||
						sel instanceof IBorder || sel instanceof IPropertiedGraphic) {
					//System.err.println("Discarding selection object" ) ;
					//selection.remove( sel ) ;
					selection.remove(sel.getUID());
				}
			}
		}

		// this block ensures that if the select set has not changed or we'return in the
		// tooltip we don't get rid of the tool tip until the timer has completed. The
		// tooltip will disappear if the user clicks anywhere or stops moving the mouse
		// for a defined period.
		if (tip != null && tip.isShowing()) {

			// get the mouse point
			Point p = me.getPoint();

			//System.err.println( "M : " + p ) ;
			Point ttPoint = SwingUtilities.convertPoint((Component) me.getSource(), p, tip);
			//System.err.println( "CP: " + ttPoint ) ;
			//System.err.println( "B : " + tip.getBounds( ) );
			//System.err.println( "TC: " + tip.contains( ttPoint ) ) ;

			if (tip.contains(ttPoint)) {
				// update the tool tip we have no model nor name and we don't clear the
				// components. Essentially leave it the same as it is as we're inside it
				// on screen
				updateTipSafely(null, false, null);
				return tip.getTipText();
			}

			SelectSet set = (SelectSet) tip.getSelection();
			if (set != null && (set.equals(selection) || (selection.getSelectCount() == 0))) {
				// update the tool tip we've got no model nor name and we don't want to clear th
				// tip
				updateTipSafely(null, false, null);
				return tip.getTipText();
			}
		}

		if ((selection.getSelectCount() != 1) || !(model instanceof Model)) {
			//System.err.println("Returning null 1" ) ;
			// update the tool tip clearing any components
			updateTipSafely(null, true, null);
			tip.setSelection(null);
			return null;
		}

		// must get the sim colorer
		String uid = null;
		if (currentScope != null) {
			uid = currentScope.getUid();
		}
		else {
			uid = ((ILogicModel) model).getDesign().getUID().getString();
		}
		DynSimController ctrl = dynSimControllers.get(uid);

		// safety
		if (ctrl == null) {
			return null;
		}

		DynSimColorer colorer = (DynSimColorer) ctrl.getColorer();

		// safety
		if (colorer == null) {
			return null;
		}

		// get the tooltips
		Map<String, String> tooltips = colorer.getToolTips();

		// safety
		if (tooltips == null) {
			return null;
		}

		// what component are we after...
		IUIDObject sel = selection.getSelectedUIDObjects().getNext();

		// We look for a list of names to support the WDG generated diagrams where
		// a single device may represent multiple simulation devices.
		List<String> name = new ArrayList<String>();
		IUIDObject connObj;
		if (sel instanceof IRepresentedObject) {
			IRepresentedObject repObj = (IRepresentedObject) sel;
			connObj = repObj.getRawConnectivity();
			if (connObj instanceof IReadOnlyNamedObject) {

				List<String> names = obtainWDGNames(connObj);
				boolean possibleWDG = !names.isEmpty(); // did we detect multiple WDG name props  on
				// connObj? If so we're looking at WDG design
				if (possibleWDG) {
					name.add(((IReadOnlyNamedObject) connObj).getName());
					name.addAll(names);
				}
				else {
					// we need to be sure we haven't renamed the object during netlisting
					List<ScopeComponent> components = currentScope.getScopedComponents();
					String cObjUid = connObj.getUID().toString();
					for (ScopeComponent sc : components) {
						//dts0100969323 if the getUid returns null => do nothing
						if (sc.getUid() != null && sc.getUid().contains(cObjUid)) {
							name.add(sc.getName());
							break; // don't do any further comparisons
						}
					}
					//  Don't need to add the name here as we have already done so.
					//	name.add(((INamedObject) connObj).getName());
				}
			}
			else {
				return null;
			}
		}
		else if (sel instanceof ISegment) {
			IReadOnlyNamedObject cond = ((ISegment) sel).getConductor().getConnectivity();
			connObj = (IUIDObject) cond;
			name.add(cond.getName());
		}
		else {
			//System.err.println( "Return null 3. " + sel.getClass( ).getName( ) ) ;
			return null;
		}

		// if we have a tooltip
		if (tip != null) {
			// make sure its nothing that'll kill our code....
			if (!(sel instanceof IPin || sel instanceof ISchemStackPin)) {
				Model m = (Model) model;
				// update the tip using the given model and name and clearing
				// any existing components
				updateTipSafely(m, true, name);
				tip.setSelection(selection);
			}
			else {
				// update the tip clearing any existing components
				updateTipSafely(null, true, null);
				tip.setSelection(null);
				return null;
			}
		}

		// Look up the tooltip based upon the name of the component
		if (name.isEmpty()) {
			//For multicores name becomes of size zero
			return null;
		}
		else {
			String tooltipText = null;
			if (AnalysisHelper.getInstance().isLegacyAnalysisMode()) {
				tooltipText = getTooltipForComponent(tooltips, name);
			}
			else if (connObj instanceof ISVAnalysisModelMapper) {
				Map<String, Map<String, String>> designSimVars =
						getSimulationResultVariables(currentScope.getUid());
				if (designSimVars != null && !designSimVars.isEmpty()) {
					Map<String, String> componentSimVars = designSimVars.get(name.get(0));
					if ((componentSimVars != null && !componentSimVars.isEmpty())) {
						IVHDLModelMapping mapping = ((ISVAnalysisModelMapper) connObj).getModelMapping();
						if (mapping != null) {
							StringBuilder tooltipTextBuilder = new StringBuilder();
							List<IVHDLMeasurementDataMapping> tooltipEnabledMeasurements =
									mapping.getMeasurementMappings().stream()
											.filter(measurementMapping -> measurementMapping.isTooltipEnabled())
											.collect(Collectors.toList());
							if (!tooltipEnabledMeasurements.isEmpty()) {
								int row = 0;
								for (IVHDLMeasurementDataMapping measurement : tooltipEnabledMeasurements) {
									String value = componentSimVars.get("property." + measurement.getName());
									if (!StringUtils.isBlank(value)) {
										if (tooltipTextBuilder.length() == 0) {
											tooltipTextBuilder.append(HTML_TOOLTIP_TABLE_PREFIX);
										}
										createTooltipRow(tooltipTextBuilder, measurement.getName(), value, row);
										row++;
									}
								}
								if (tooltipTextBuilder.length() != 0) {
									tooltipTextBuilder.append(HTML_TOOLTIP_TABLE_SUFFIX);
								}
							}
							tooltipText = tooltipTextBuilder.toString();
						}
						else if (connObj instanceof IWireConductor) {
							StringBuilder tooltipTextBuilder = new StringBuilder();
							int row = 0;
							for (String variable : componentSimVars.keySet()) {
								if (variable.startsWith("property.") && variable.endsWith(".tooltip")) {
									String value = componentSimVars.get(variable);
									if (!StringUtils.isBlank(value)) {
										if (tooltipTextBuilder.length() == 0) {
											tooltipTextBuilder.append(HTML_TOOLTIP_TABLE_PREFIX);
										}
										createTooltipRow(tooltipTextBuilder,
												variable.substring(9, variable.length() - 8),
												value,
												row);
										row++;
									}
								}
							}
							if (tooltipTextBuilder.length() != 0) {
								tooltipTextBuilder.append(HTML_TOOLTIP_TABLE_SUFFIX);
							}
							tooltipText = tooltipTextBuilder.toString();
						}
					}
				}
			}
			tip.setToolTipProviderName(name.get(0));
			return AnalysisToolTip.getTooltipHTML(name.get(0), tooltipText);
		}
	}
	/**
	 * This method is message when the view requires a tool tip to be created. This allows the provider to return a
	 * custom imp of the tooltip if required. If it does not it should return null, this will imply a default tool tip
	 * be created.
	 * <p>
	 * A provider may choose to create a new tooltip each time or hold ref to the original updating it when the
	 * selection changes as described in getToolTipText.
	 *
	 * @return JToolTip, the tool tip to use
	 */
	@Override @Nullable public JToolTip createToolTip()
	{
		if (!tip.isShowing()) {

			/*Runnable r = new Runnable( ) {
								 public void run( ) {
									while( ! tip.isShowing( ) ) {
									   try {
										  Thread.currentThread( ).sleep( 5 ) ;
									   } catch ( Exception e ) {
										  e.printStackTrace( ) ;
									   }
									}
									tip.setAlpha( tip.ALPHA_START ) ;
									tip.repaint( ) ;
									while ( tip.getAlpha( ) < 255 - tip.ALPHA_INCREMENT ) {
									   tip.setAlpha( tip.getAlpha( ) + tip.ALPHA_INCREMENT ) ;
									   tip.repaint( ) ;
									   try {
										  Thread.currentThread( ).sleep( tip.ALPHA_DELAY ) ;
									   } catch ( Exception e ) {
										  e.printStackTrace( ) ;
									   }
									}
									tip.setAlpha( tip.ALPHA_END ) ;
									tip.repaint( ) ;

								 }
							  } ;
							  Thread t = new Thread( r ) ;
							  t.start( ) ; */

		}
		return tip;
	}

	/**
	 * @see IToolTipProvider#getToolTipLocation(MouseEvent)
	 */
	@Override public Optional<Point> getToolTipLocation(@NotNull MouseEvent event)
	{
		return Optional.empty();
	}

	private void createTooltipRow(StringBuilder tooltipTextBuilder, String varName, String value, int row)
	{
		StringBuilder toolTipRowTextBuilder = new StringBuilder();
		if (row % 2 == 0) {
			toolTipRowTextBuilder.append(HTML_TOOLTIP_ROW_EVEN_PREFIX);
		}
		else {
			toolTipRowTextBuilder.append(HTML_TOOLTIP_ROW_ODD_PREFIX);
		}
		toolTipRowTextBuilder.append(varName)
				.append(HTML_TOOLTIP_COLUMN_SUFFIX)
				.append(value)
				.append(HTML_TOOLTIP_ROW_SUFFIX);
		tooltipTextBuilder.append(toolTipRowTextBuilder.toString());
	}

	/**
	 * check what the tooltip wants us to display.. i.e what pane is open --  this will scan the tip for the selected
	 * panel and if that has a tooltip associated then returnthat if not the normal text is retuned.
	 *
	 * @param names, the list of names displayed on the tip
	 *
	 * @return String, the tooltip text
	 */
	protected String getTooltipForComponent(Map<String, String> tooltips, List<String> names)
	{
		if (names.size() == 1) {
			tip.setTooltips(null);
			tip.setPrefix(null);
			return tooltips.get(names.get(0));
		}
		else if (names.size() > 1) {

			JTabbedPane tipPane = tip.getTabbedPane();
			if (tipPane != null) {
				int index = tipPane.getSelectedIndex();
				String name = names.get(0) + AnalysisNetlistExporter.WDG_MULTI_BOUNDARY_CHARS.charAt(0) +
						tipPane.getTitleAt(index) + AnalysisNetlistExporter.WDG_MULTI_BOUNDARY_CHARS.charAt(1);
				tip.setTooltips(tooltips);
				tip.setPrefix(names.get(0));
				return tooltips.get(name);
			}
			else {
				return "";
			}
		}
		else {
			return "";
		}
	}

	public void updateTipSafely(@Nullable final Model model, final boolean removeExisting,
			@Nullable final List<String> names)
	{
		CHSSwingUtils.invoke(new Runnable()
		{
			public void run()
			{
				updateTip(model, removeExisting, names);
			}
		}, true);
	}

	/**
	 * This method updates the tool tip to reflect the current input / failure states
	 */
	public void updateTip(@Nullable Model model, boolean removeExisting, @Nullable List<String> names)
	{

		String uid = null;
		if (currentScope != null) {
			uid = currentScope.getUid();
		}
		if (uid == null && model != null) {
			uid = model.getDesign().getUID().toString();
		}

		JPanel panel = tip.getPanel();
		if (tipPanel == null) {
			tipPanel = new JPanel();
			tipPanel.setLayout(new BorderLayout());
			panel.add(tipPanel, BorderLayout.CENTER);
		}

		if (removeExisting || (uid != null && names != null)) {
			tipPanel.removeAll();
		}

		if (uid != null && names != null) {
			//tipPanel.removeAll( ) ;
			IAnalysisSimulationSessionController simSession = getSimSession(uid);
			Map<String, Map<String, String>> resultVariables = getSimulationResultVariables(uid);
			if (simSession == null || resultVariables == null) {
				return;
			}

			updateTooltipPanel(model, names, uid, simSession);
		}
		tip.validate();
	}

	private void updateTooltipPanel(@Nullable Model model, @NotNull List<String> names,
			String uid, IAnalysisSimulationSessionController simSession)
	{
		JPanel wholeTipPanel = new JPanel();
		wholeTipPanel.setLayout(new BorderLayout());

		JTabbedPane tooltipPane = null;

		for (int i = 0; i < names.size(); i++) {
			if (names.size() > 1 && i == 0) {
				continue; // the first in the list is the name of the slot
			}
			// and we don't want to display it...
			String name = names.get(i);
			String shortName = name;
			// if its a WDG device with multiple simulation devices contained we need to build
			// the name as it is built for the simulator... i.e. DEVICE{SIM_DEVICE}
			if (names.size() > 1) {
				name = names.get(0) + AnalysisNetlistExporter.WDG_MULTI_BOUNDARY_CHARS.charAt(0) +
						name + AnalysisNetlistExporter.WDG_MULTI_BOUNDARY_CHARS.charAt(1);
			}
			JPanel mainPanel = createMainToolTipPanel(model, simSession, uid, name);
			JButton resButton = createButtonToolTipPanel(uid, name);
			JPanel northPanel = new JPanel();
			northPanel.setLayout(new FlowLayout());
			northPanel.add(new JLabel());
			wholeTipPanel.add(northPanel, BorderLayout.NORTH); // for later use as required for tooltips
			wholeTipPanel.add(mainPanel, BorderLayout.CENTER);
			wholeTipPanel.add(resButton, BorderLayout.SOUTH);

			// if its a WDG device with multiple simulation devices contained we need to add the
			// created tooltipPane to the pane
			if (names.size() > 1) {
				if (tooltipPane == null) {
					tooltipPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
					tooltipPane.addChangeListener(tip);
				}
				tooltipPane.add(shortName, wholeTipPanel);
				wholeTipPanel = new JPanel();
				wholeTipPanel.setLayout(new BorderLayout());
			}
		}

		// Only add the pane if there's something to display...
		if (names.size() > 1 && tooltipPane != null) {
			wholeTipPanel.add(tooltipPane, BorderLayout.CENTER);
		}

		tipPanel.add(wholeTipPanel, BorderLayout.CENTER);
		tip.setOpacity(tipPanel);
	}

	/**
	 * This method creates a button used to allow the user to view the simulation properties for the component with the
	 * given name (in the simulation session with the given uid).
	 *
	 * @param uid, the uid of the simulation session
	 * @param name, the name of the device for which we want the information
	 *
	 * @return JButton, linked to the action that will show the properties (may be disabled if there are no properties
	 * available)
	 */
	protected JButton createButtonToolTipPanel(String uid, String name)
	{
		// try to get the simulation results
		Map<String, Map<String, String>> simResults = getSimulationResultVariables(uid);
		// Are any available? If so, we pass the results of the specific named component
		// otherwise we pass null to indicate no results available (disables the action)
		Map<String, String> resultsToDisplay = simResults == null ? null : simResults.get(name);
		// return a newly created button linked to the action we create with the simlation results
		return BasicUIFactory.getInstance().createSiemensCustomJButton(new ShowPropertiesAction(uid, name, resultsToDisplay));
	}

	@SuppressWarnings("UseOfObsoleteCollectionType")
	protected JPanel createMainToolTipPanel(@Nullable Model model, IAnalysisSimulationSessionController simSession,
			String uid,
			String name)
	{
		DefaultMutableTreeNode failureNode = simSession.getComponentFailures(uid, name);

		DefaultMutableTreeNode inputNode = simSession.getComponentInputProperties(uid, name);
		JPanel failurePanel = null;
		JScrollPane failureScrollPane = null;
		JPanel mainPanel = new JPanel();
		int failWidth = 0;
		int failHeight = 0;
		if (failureNode != null && failureNode.getChildCount() > 0) {
			failurePanel = new JPanel();
			failureScrollPane = new JScrollPane(failurePanel);
			failurePanel.setBorder(BorderFactory.createTitledBorder(failureNode.getUserObject().toString()));
			failurePanel.setLayout(new GridBagLayout());
			GridBagConstraints failPanelConstraint = new GridBagConstraints();
			ResetableButtonGroup b = new ResetableButtonGroup(noneBox);
			int checkBoxOffSet = 0;
			for (int i = 0; i < failureNode.getChildCount(); i++) {
				StringTokenizer st = new StringTokenizer(
						((DefaultMutableTreeNode) failureNode.getChildAt(i)).getUserObject().toString(), ",");
				String failureModeName = st.nextToken();

				JCheckBox box =
						new JCheckBox(new FailureModeAction(model, failureModeName, name, getSimSession(uid), uid));

				int newWidth1 = box.getFontMetrics(box.getFont()).stringWidth(failureModeName) + box.getInsets().left +
						box.getInsets().right;
				if (newWidth1 > failWidth) {
					failWidth = newWidth1;
				}

				if (checkBoxOffSet == 0) {
					checkBoxOffSet = box.getFontMetrics(box.getFont()).getHeight() * 2;
				}
				if (i <= 9) {
					failHeight += box.getFontMetrics(box.getFont()).getHeight() * 5 / 2;
				}
				else {
					failHeight = box.getFontMetrics(box.getFont()).getHeight() * EIGHTEEN_PIXELS;
				}
				failPanelConstraint.fill = GridBagConstraints.HORIZONTAL;
				failPanelConstraint.gridx = 0;
				failPanelConstraint.gridy = i;
				failPanelConstraint.weighty = DEFAULT_Y_WEIGHT;
				failurePanel.add(box, failPanelConstraint);
				// must set the selection after adding to group else depending on
				// previous state of noneBox the new box may be deselected!!! ROH
				// HSDP100010369
				b.add(box);
				box.setSelected(Boolean.valueOf(st.nextToken()));
			}
			failHeight += failurePanel.getInsets().top + failurePanel.getInsets().bottom;

			failWidth += failurePanel.getInsets().left + failurePanel.getInsets().right + checkBoxOffSet;
		}

		JScrollPane inputScrollPane = null;
		JPanel inputPanel = null;
		int inpHeight = 0;
		int inpWidth = 0;
		if (inputNode != null && inputNode.getChildCount() > 0) {
			inputPanel = new JPanel();
			inputScrollPane = new JScrollPane(inputPanel);
			inputPanel.setBorder(BorderFactory.createTitledBorder(inputNode.getUserObject().toString()));
			Map<String, Vector<String>> properties = new HashMap<String, Vector<String>>();
			for (int i = 0; i < inputNode.getChildCount(); i++) {
				String nodeData = ((DefaultMutableTreeNode) inputNode.getChildAt(i)).getUserObject().toString();
				StringTokenizer st = new StringTokenizer(nodeData, ",");
				String propName = st.nextToken();

				// add to list of same name props
				Vector<String> v = properties.get(propName);
				if (v == null) {
					v = new Vector<String>();
					properties.put(propName, v);
				}
				v.addElement(nodeData);
			}
			// names of properties
			Iterator<String> keys = properties.keySet().iterator();
			Vector<JPanel> panels = new Vector<JPanel>();
			int propPanelWidth = 0;
			int checkboxCount = 0;
			int checkBoxOffSet = 0;
			while (keys.hasNext()) {
				String key = keys.next();
				Vector<String> v = properties.get(key);
				ButtonGroup b = new ButtonGroup();
				JPanel leftPanel = new JPanel();
				JLabel keyLbl = new JLabel(key);
				leftPanel.add(keyLbl);
				int keyLblWidth =
						keyLbl.getFontMetrics(keyLbl.getFont()).stringWidth(key) + leftPanel.getInsets().left +
								leftPanel.getInsets().right + 10;

				JPanel rightPanel = new JPanel();
				rightPanel.setLayout(new GridLayout(v.size(), 1));
				int rightPanelInsetWidth = rightPanel.getInsets().left + rightPanel.getInsets().right;

				int innerPanelWidth = 0;
				for (int i = 0; i < v.size(); i++) {
					StringTokenizer st = new StringTokenizer(v.elementAt(i), ",");
					String propertyName = st.nextToken();

					String inputNodePropVal = st.nextToken();
					String isSelected = st.nextToken();
					String propType = st.nextToken();

					Map<String, String> simulationResVars = getSimulationResultVariables(uid).get(name);
					if (simulationResVars != null) {
						String propValue = simulationResVars.get("property." + propertyName);

						if (!("F".equals(propType) || "S".equals(propType) || "I".equals(propType)) ||
								StringUtils.isBlank(propValue)) {
							propValue = inputNodePropVal;
						}

						int width1 = keyLblWidth;

						JCheckBox box = new JCheckBox(new InputPropertyAction(model, propertyName, propValue, name,
								getSimSession(uid), uid));
						box.setSelected(Boolean.valueOf(isSelected));
						rightPanel.add(box);
						b.add(box);

						width1 += box.getFontMetrics(box.getFont()).stringWidth(propValue) + rightPanelInsetWidth +
								box.getInsets().left + box.getInsets().right + TWENTY_PIXELS;
						if (width1 > innerPanelWidth) {
							innerPanelWidth = width1;
						}

						if (checkBoxOffSet == 0) {
							checkBoxOffSet = box.getFontMetrics(box.getFont()).getHeight() * 2;
						}

						if (checkboxCount <= 9) {
							inpHeight += box.getFontMetrics(box.getFont()).getHeight() * 5 / 2;
							checkboxCount += 1;
						}
						else {
							inpHeight = box.getFontMetrics(box.getFont()).getHeight() * EIGHTEEN_PIXELS;
						}
					}
				}

				JPanel propPanel = new JPanel();
				propPanel.setLayout(new GridLayout(1, 2));
				propPanel.add(leftPanel);
				propPanel.add(rightPanel);
				panels.add(propPanel);

				int subCotainerWidth =
						innerPanelWidth + propPanel.getInsets().left + propPanel.getInsets().right + checkBoxOffSet;
				if (subCotainerWidth > propPanelWidth) {
					propPanelWidth = subCotainerWidth;
				}
			}

			inputPanel.setLayout(new GridBagLayout());
			GridBagConstraints inpPanelConstraint = new GridBagConstraints();
			for (int i = 0; i < panels.size(); i++) {
				inpPanelConstraint.fill = GridBagConstraints.BOTH;
				inpPanelConstraint.gridx = 0;
				inpPanelConstraint.gridy = i;
				inpPanelConstraint.weighty = DEFAULT_Y_WEIGHT;

				inputPanel.add(panels.elementAt(i), inpPanelConstraint);
			}

			inpWidth = propPanelWidth + inputPanel.getInsets().left + inputPanel.getInsets().right + checkBoxOffSet;
			inpHeight += inputPanel.getInsets().top + inputPanel.getInsets().bottom;
		}

		int height = inpHeight > failHeight ? inpHeight : failHeight;

		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints mainPanelConstraint = new GridBagConstraints();
		mainPanelConstraint.fill = GridBagConstraints.BOTH;

		mainPanelConstraint.weighty = DEFAULT_Y_WEIGHT;
		mainPanelConstraint.gridy = 0;
		if (inputPanel != null) {
			mainPanelConstraint.gridx = 0;
			inputScrollPane.setPreferredSize(new Dimension(inpWidth, height));
			mainPanel.add(inputScrollPane, mainPanelConstraint);
		}
		if (failurePanel != null) {
			mainPanelConstraint.gridx = inputPanel != null ? 1 : 0;
			failureScrollPane.setPreferredSize(new Dimension(failWidth, height));
			mainPanel.add(failureScrollPane, mainPanelConstraint);
		}

		return mainPanel;
	}

	/**
	 * Examine the given object for properties which indicate that the device is generated by the WiringDesignGenerator
	 * and that the device represents multiple devices in the simulation
	 *
	 * @param obj, the object we're interested in
	 *
	 * @return List, a list of the names of the simulation devices
	 */
	protected List<String> obtainWDGNames(IUIDObject obj)
	{
		List<String> temp = new ArrayList<String>();
		if (obj instanceof IPropertiedObject) {
			IPropertiedObject propObj = (IPropertiedObject) obj;
			IPropertyIterator properties = propObj.getProperties();
			while (properties.hasNext()) {
				IProperty prop = properties.getNext();
				// the WDG_AMPP is the Analysis Model Property Prefix that the
				// WiringDesignGenerator has added to the device. Its presence
				// indicates that this device actually represents multiple devices
				// in the simulation. Hence we pick out the names of the represented
				// devices.
				if (prop.getName().startsWith(AnalysisNetlistExporter.WDG_AMPP)) {
					// the name of the referenced device is the suffix to the WDG_AMPP
					temp.add(prop.getName().substring(AnalysisNetlistExporter.WDG_AMPP.length(),
							prop.getName().length()));
				}
			}
		}
		return temp;
	}

	// //////////////////////////////// //
	// DynSimControllerListener methods //
	// //////////////////////////////// //

	public void dynamicSimControllerChange(DynSimControllerChangeEvent dscce)
	{

		DynSimController ctrl = (DynSimController) dscce.getController();
		if (ctrl != null) {
			//System.err.println("dSCC : im " + dscce.getInitialMode( ) + " m " + ctrl.getMode( ) ) ;
			handleUISimulationModeTransition(dscce.getInitialMode(), ctrl.getMode());
			if (CAFUtils.getInstance().getActiveCapletController() != null) {
				CAFUtils.getInstance()
						.tickleUI(CAFUtils.getInstance().getActiveCapletController().getCaplet().getFIB());
			}
		}
	}

	// ///////////// //
	// Inner classes //
	// ///////////// //

	protected static class FailureModeAction extends AbstractAction
	{

		protected String failureName;
		protected String componentName;
		protected IAnalysisSimulationSessionController session;
		protected String uid;
		protected Model model;

		protected FailureModeAction(Model theModel, String theFailureName, String theComponentName,
				IAnalysisSimulationSessionController theSession, String theUid)
		{
			failureName = theFailureName;
			componentName = theComponentName;
			session = theSession;
			uid = theUid;
			model = theModel;

			putValue(NAME, failureName);
			//putValue( SHORT_DESCRIPTION,
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.change")  + failureName +
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.failureModeAction.failureSetting")  ) ;
		}

		public void actionPerformed(ActionEvent e)
		{
			boolean failed = ((AbstractButton) e.getSource()).isSelected();
			//System.err.println("Failure is Selected : " + failed ) ;
			session.setFailure(uid, componentName, failureName, failed);
			if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
				((LogicAnalysisServices) getAnalysisServices()).updateSimulation(
						AnalysisServices.getCurrentAnalysisNetlistScope());
			}
			else {
				((LogicAnalysisServices) getAnalysisServices()).updateSimulation(model);
			}
		}
	}

	protected static class InputPropertyAction extends AbstractAction
	{

		protected String valueName;
		protected String propertyName;
		protected String componentName;
		protected IAnalysisSimulationSessionController session;
		protected String uid;
		protected Model model;

		protected InputPropertyAction(Model theModel, String thePropertyName, String theValueName,
				String theComponentName,
				IAnalysisSimulationSessionController theSession, String theUid)
		{
			propertyName = thePropertyName;
			valueName = theValueName;
			componentName = theComponentName;
			session = theSession;
			uid = theUid;
			model = theModel;

			putValue(NAME, valueName);
			//putValue( SHORT_DESCRIPTION,
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.String.change") + propertyName +
			//   ResourceMgr.getString( AnalysisPopupMenuBuilderAction.class, "AnalysisPopupMenuBuilderAction.inputPropertyAction.propertySetting") ) ;
		}

		public void actionPerformed(ActionEvent e)
		{
			session.setProperty(uid, componentName, propertyName, valueName);

			if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
				((LogicAnalysisServices) getAnalysisServices()).updateSimulation(
						AnalysisServices.getCurrentAnalysisNetlistScope());
			}
			else {
				((LogicAnalysisServices) getAnalysisServices()).updateSimulation(model);
			}
		}
	}

	protected static class ShowPropertiesAction extends AbstractAction
	{

		protected String componentName;
		protected Map<String, String> componentVariables;
		protected String uid;

		protected ShowPropertiesAction(String theUid, String theComponentName, Map<String, String> theVariables)
		{
			uid = theUid;
			componentName = theComponentName;
			componentVariables = theVariables;
			putValue(NAME, ResourceMgr.getString(LogicAnalysisServices.class, "ShowPropertiesAction.String.name"));
			setEnabled(componentVariables != null && !componentVariables.isEmpty());
		}

		public void actionPerformed(ActionEvent e)
		{
			//System.err.println( componentVariables ) ;
			Iterator<String> keys = componentVariables.keySet().iterator();
			List<List<String>> tableData = new ArrayList<List<String>>();
			while (keys.hasNext()) {
				String key = keys.next();
				String data = componentVariables.get(key);
				List<String> row = new ArrayList<String>();
				if ( key.startsWith("properties") && key.endsWith(".tooltip")){
					continue;
				}
				row.add(key);
				row.add(data);
				tableData.add(row);
			}
			AnalysisPropertiesDialog dialog = new AnalysisPropertiesDialog(uid, tableData, componentName);
			dialog.setVisible(true);
		}
	}

	protected static class ResetableButtonGroup extends ButtonGroup
	{

		protected ButtonModel blankModel;

		protected ResetableButtonGroup(AbstractButton b)
		{
			blankModel = b.getModel();
			add(b);
		}

		public void setSelected(ButtonModel m, boolean b)
		{
			if (b) {
				super.setSelected(m, true);
			}
			else {
				// if we're deselecting the selected one then
				// we select the blank one...
				if (m == getSelection()) {
					super.setSelected(blankModel, true);
				}
			}
		}
	}

	protected static class WatchPaneMouseListener implements MouseListener
	{

		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() == 2) {
				if (e.getComponent() instanceof JTable) {
					JTable watchTable = (JTable) e.getComponent();

					int row = watchTable.rowAtPoint(e.getPoint());
					if (row != -1) {
						SimulationMonitorsTableModel model = (SimulationMonitorsTableModel) watchTable.getModel();
						String component = (String) model.getValueAt(row, 0);
						getAnalysisServices().showPropertiesFor(model.getUid(),
								component.substring(0, component.indexOf('.')));
					}
				}
			}
		}

		public void mouseEntered(MouseEvent e)
		{
		}

		public void mouseExited(MouseEvent e)
		{
		}

		public void mousePressed(MouseEvent e)
		{
		}

		public void mouseReleased(MouseEvent e)
		{
		}
	}
}
