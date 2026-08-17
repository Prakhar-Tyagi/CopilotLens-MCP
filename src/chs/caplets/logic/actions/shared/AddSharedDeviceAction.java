/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2026 Siemens
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFProfilingKey;
import chs.caf.CAFUtils;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.actions.AddPinActionHelper;
import chs.caplets.logic.actions.CreateParameterizedObjectAction;
import chs.caplets.logic.actions.PinListDeferredAddPinHelper;
import chs.caplets.logic.actions.ghc.ConnectivityGHCHelper;
import chs.caplets.logic.shared.AddSharedPinListDialog;
import chs.cof.draw.FlipAxisEnum;
import chs.cof.draw.IArrowable;
import chs.cof.draw.IColor;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IGfxAttributeContainer;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGrid;
import chs.cof.draw.IGriddable;
import chs.cof.draw.ISheet;
import chs.cof.draw.IText;
import chs.cof.draw.ITransform;
import chs.cof.draw.ITransformCompound;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LineStyle;
import chs.cof.draw.WritableGfxAttribute;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.ICrossReferenceable;
import chs.cof.drawplus.IGfxView;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IXRefPlaceholder;
import chs.cof.drawplus.IXRefText;
import chs.cof.drawplus.XRefTextWrapper;
import chs.cof.logical.IDesign;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IInternalPinIterator;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemFactory;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.logical.shared.IDesignSharedPinListUsage;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsageMgr;
import chs.cof.logical.shared.IProjectSharedUsageView;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinListUsage;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.logical.shared.UsageUpdateStrategy;
import chs.cof.project.IProject;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolDefIterator;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.SymbolTypeEnum;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.GeneratorStyle;
import chs.common.ICommonFactory;
import chs.common.IDesignContainer;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.IParameterized;
import chs.common.IProperty;
import chs.common.IPropertyIterator;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUIDProvider;
import chs.common.LazyLoadingStrategy;
import chs.common.Location;
import chs.common.UIDUtils;
import chs.common.preferencesets.IPreferenceSet;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IGenericPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.ctf.dataservices.CapitalProjectDataServices;
import chs.dataservices.SharedObjectUsageInfo;
import chs.services.dynamicgfx.DynamicRectangle;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CollectionUtils;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ListMap;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.WrappingRuntimeException;
import chs.utility.ConductorSplitter;
import chs.utility.DiagramHelper;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.TransformUtils;
import chs.utility.attr.AttributeUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.CrossReferenceUtils;
import chs.utility.helpers.DesignSharedUsageHelper;
import chs.utility.helpers.GfxAttributeHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.SharedPinListEditUtils;
import chs.utility.ui.SymbolProxy;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AddSharedDeviceAction extends CreateParameterizedObjectAction
{

	protected static final double SYMBOL_SPACING = 1920.0;
	protected static final int AUTO_GEN_DEV_WIDTH = 7;

	private SymbolProxy[] m_symbolInstances;

	protected IGfxObject m_symbolDyn;

	protected AddSharedPinListDialog m_addPinListDialog;
	private Generator m_generator;

	private static Cursor m_deviceCursor = null;

	protected Collection<ISharedPinListUsage> m_sharedUsageInfo = null;

	private ActionEvent m_restartEvent = null;

	private AddPinActionHelper m_addPinActionHelper; // Delegate action after shared device has been created
	protected ISharedPinList m_sharedPinList;
	protected ISpecialSelectMgr m_sharedSelectMgr;
	private IPinList m_device;
	@NotNull private Set<IUIDObject> m_preemies = new HashSet<>();
	private List<PinListGroup> m_groupList;
	private List<PinListGroup> m_symbolDynList;
	private Map<IPinList, SymbolProxy> m_pinListToSymbolMap;
	private ISymbolLibraryMgr m_symLibMgr;
	private IGfxObject pinRect;
	protected Collection<IGenericPin> symbolCablePins;
	protected Set<chs.cof.logical.cable.IPinList> addedDevices;
	protected ListMap<IGfxObject, TransformType> transformList;  //the rotate/flip list for the selected blocks
	protected static final double DEG_90 = 90;
	protected static final double DEG_360 = 360;
	private int m_flipState = 0;
	private boolean m_bNewDevice = false;

	private enum TransformType
	{
		Rotate, Flip
	}

	public AddSharedDeviceAction(ICapletController controller, @Nullable ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_sharedAutoGenDynamic = null;
		pinRect = null;
		m_addPinActionHelper = getAddPinActionHelper();
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null && m_sharedSelectMgr != null) {
			m_sharedSelectMgr.contextMenuAddAction(new ActionEntry(getActionUI(),
					(String) getActionUI().getValue(Action.SHORT_DESCRIPTION), getCtxCommand())
			{
				public boolean shouldDisplay()
				{
					return getOperand() != null && isEnabled() && super.shouldDisplay();
				}
			});
		}
		m_generator = Generator.getGenerator();
		m_symLibMgr = FactoryMgr.getSystemFactory().getCHSSystem().getSymbolLibraryMgr();
		if (m_deviceCursor == null) {
			m_deviceCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_device.gif", new Point(8, 8));
		}
		transformList = new ListMap<>();
	}

	@NotNull protected AddPinActionHelper getAddPinActionHelper()
	{
		return new AddPinActionHelper(this, false, true);
	}

	protected String getCtxCommand()
	{
		return "AddSharedDevice";
	}

	/**
	 * Description of the Method
	 *
	 * @param e Description of Parameter
	 *
	 * @return Description of the Returned Value
	 */
	public IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum result = IActionEnum.eCanceled;

		try {
			resetMemberData();
			m_restartEvent = e;

			// Find our connectivity device
			Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			Object source = e.getSource();
			ISharedPinList spl = getOperand();
			StringBuilder title = new StringBuilder(CAFUtils.getInstance().getDialogTitleByAction(this, true));
			if (spl != null) {
				title.append(" - ");
				title.append(spl.getName());
			}
			Profiler openDialogProfiler =
					ProfilingService.createAndStartProfiler(CAFProfilingKey.UI_INTERACTION.getKeyName());

			ILogicDesign design = getLocalModel().getDesign();
			assert design != null;
			if (!loadSharedDesignUsageMgrs(design)) {
				return IActionEnum.eCanceled;
			}

			final boolean isNonInteractiveObjectSelection = isNonInteractiveObjectSelection(source);
			if (isNonInteractiveObjectSelection && spl != null) {
				// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
				final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
				if (!new SharedObjectAvailabilityChecker().check(spl, design, reporter, true,true)) {
					return IActionEnum.eCanceled;
				}
			}

			m_addPinListDialog = createSharedPinListDialog(owner, title);
			ProfilingService.stopAndLogProfiler(openDialogProfiler, "Open Add Shared PinList dialog :");
			IPlacementOptionParams params =
					spl != null ? createPlacementOptionParams(spl) : createPlacementOptionParams(getType(), true);
			boolean success;

			if (isNonInteractiveObjectSelection && spl != null) {
				ISharedPinReservationView pinResView =
						FactoryMgr.getCommonFactory().constructSharedPinReservationView(spl);
				success = m_addPinListDialog.selectPinList(getLocalModel().getDesign(), spl, pinResView, params);
			}
			else if (allowPinListSelection()) {
				success = m_addPinListDialog.selectPinList(getLocalModel().getDesign(), getType(), params);
			}
			else {
				return IActionEnum.eCanceled;
			}
			// pinlist was locked by the dialog even if we cancelled it, need to remember to unlock it in all cases
			m_sharedPinList = m_addPinListDialog.getSharedPinList();

			if (!success) {
				return IActionEnum.eCanceled;
			}

			// Check that another revision of this shared object does not already exist in this design
			boolean failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(design, m_sharedPinList);
			if (failure) {
				return IActionEnum.eCanceled;
			}
			Collection<? extends ISharedObject> placeableObjects =
					ShareConcurrencyHelper.trySharedObjectPlacement(design,
							Collections.singleton(m_sharedPinList));
			if (!placeableObjects.contains(m_sharedPinList)) {
				return IActionEnum.eCanceled;
			}
			IProjectSharedUsageView suView = CAFUtils.getInstance().getCurrentProject().getSharedUsageView();
			m_sharedUsageInfo = getSharedUsageInfo(suView);
			m_symbolInstances = m_addPinListDialog.getSelectedSymbolInstances();

			if (m_symbolInstances.length > 0) {
				setState(STATE_SYMBOL);

				// Create Map for block symbols.

				for (SymbolProxy m_symbolInstance : m_symbolInstances) {
					IBlock block = m_symbolInstance.getBlock();
					if (block != null) {
						ISymbolRef symRef = block.getSymbolRef();
						ISymbolDef blockSymDef = (ISymbolDef) FactoryMgr.getSystemFactory().getCHSSystem()
								.getSymbolLibraryMgr().getReferencedSymbol(symRef);
						if (blockSymDef != null) {
							blockSymDef.getContainerLibrary().loadFully(blockSymDef);
						}
					}
				}

				m_groupList = createAndGroupSymbolPinLists();
				m_symbolDynList = new ArrayList<PinListGroup>(m_groupList);
				PinListGroup plg = m_symbolDynList.remove(0);
				m_symbolDyn = plg.dynamicGraphics;
				getLocalModel().getDynamicGfxService().addTransientGfx(m_symbolDyn);
			}
			else if (m_addPinListDialog.getAutoGenerate()) {
				setState(STATE_GENERATE);

				int numPins = m_addPinListDialog.getNumUsedPins();
				int pinspacing = m_grid.getGridSpacing();
				int width = AUTO_GEN_DEV_WIDTH * pinspacing;
				int height;

				if (numPins == 0) {
					height = 2 * pinspacing;
				}
				else if (numPins % 2 == 0) {
					height = (numPins / 2 + 1) * pinspacing;
				}
				else {
					height = (numPins / 2 + 2) * pinspacing;
				}

				Point p1 = new Point(0, 0);
				Point p2 = new Point(width, height);
				m_sharedAutoGenDynamic =
						getDynamicGfxService().getFactory().constructRectangle(p1, p2, new Point(0, 0), false);
				getLocalModel().getDynamicGfxService().addTransientGfx(m_sharedAutoGenDynamic);
			}
			else {
				setState(STATE_PARAM);
			}
			result = super.onActivate(e);

			if (getState() == STATE_GENERATE) {
				DynamicRotationIndicator indicator = getRotationIndicator();
				indicator.hide();
			}
		}
		finally {
			if (!IActionEnum.eActivated.equals(result) && m_sharedPinList != null && m_sharedPinList.isLocked()) {
				m_sharedPinList.unlock();
			}
		}
		return result;
	}

	protected boolean isNonInteractiveObjectSelection(Object source)
	{
		final boolean isNonInteractiveObjectSelection = source instanceof IBrowserClient || isContextButton(source);
		return isNonInteractiveObjectSelection;
	}

	protected void resetMemberData()
	{
		m_flipState = 0;
		m_device = null;
		pinRect = null;
		m_preemies = new HashSet<IUIDObject>();
		addedDevices = new HashSet<chs.cof.logical.cable.IPinList>();
		m_groupList = Collections.emptyList();
		m_symbolDynList = Collections.emptyList();
		m_symbolDyn = null;
		symbolCablePins = new ArrayList<IGenericPin>();
		//
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		m_grid = diagram.getGrid();
		m_bNewDevice = false;
	}

	protected AddSharedPinListDialog createSharedPinListDialog(Frame owner, StringBuilder title)
	{
		return createSharedPinListDialog(owner, title.toString());
	}

	protected boolean allowPinListSelection()
	{
		return true;
	}

	protected Collection<ISharedPinListUsage> getSharedUsageInfo(IProjectSharedUsageView suView)
	{
		Set<IUID> designUIDs = getDesignUsagesFromDB(m_sharedPinList);
		populateDesignsFromLoadedDesignUsages(suView, designUIDs, m_sharedPinList);
		return suView.getUsagesOf(ISharedPinListUsage.class, m_sharedPinList, designUIDs);
	}

	private void populateDesignsFromLoadedDesignUsages(IProjectSharedUsageView suView, Set<IUID> designUIDs,
			ISharedPinList sharedpinlist)
	{
		LazyLoadingStrategy preLazyLoadStrategy = suView.getSharedUsageManagerLoadingStrategy();
		suView.setSharedUsageManagerLoadingStrategy(LazyLoadingStrategy.DO_NOT_LOAD_IF_NOT_LOADED);
		try {
			for (ISharedUsage usage : suView.getUsagesOf(ISharedUsage.class, sharedpinlist)) {
				designUIDs.add(usage.getDesignUID());
			}
		}
		finally {
			suView.setSharedUsageManagerLoadingStrategy(preLazyLoadStrategy);
		}
	}

	private Set<IUID> getDesignUsagesFromDB(ISharedPinList sharedpinlist)
	{
		Set<IUID> designUIDs = new HashSet<IUID>();
		SharedObjectUsageInfo sharedObjectUsageInfo =
				CapitalProjectDataServices.getDataServices().getSharedObjectUsageData(sharedpinlist);
		if (sharedObjectUsageInfo != null) {
			IProject project = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
			Collection<IDesignContainer> designs = LogicUtils
					.loadDesigns(project.getDesignMgr(), sharedObjectUsageInfo.getUsedDesignNames());
			designUIDs.addAll(UIDUtils.convertToUID(designs));
		}
		return designUIDs;
	}

	protected AddSharedPinListDialog createSharedPinListDialog(Frame owner, String title)
	{
		return new AddSharedPinListDialog(owner, title, getType());
	}

	protected double calculateBorderSize()
	{
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		IParameterized params = commonFactory.createParameterized();
		GeneratorParameters gp = new GeneratorParameters(m_grid, PreferenceSetHelper.getStyleSet(getDiagram()));
		Generator generator = Generator.getGenerator();
		GeneratorStyle gs = generator.getStyle();
		gs.addDefaults(params, getObjectType());
		double borderSize = calculateBorderSize(gp, params);

		UIDMgr.removeObject(params.getUID());
		CreationDeletionHelper.getTheCreationHelper().removeCreationObject(params);

		return borderSize;
	}

	protected String getObjectType()
	{
		return "device";
	}

	protected PinListTypeEnum getType()
	{
		return PinListTypeEnum.TypeDevice;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return AddSharedDeviceActionUI.class.getName();
	}

	public boolean onTerminate(boolean successful)
	{
		boolean pinCreationCancelled = false;
		boolean actionSuccess = successful;
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		try {
			cdh.addCreationObjects(m_preemies);
			// If add pin action is cancelled after mouseReleased(), is called m_preemies will not be empty
			if (!successful && !m_preemies.isEmpty()) {
				pinCreationCancelled = true;
			}
			actionSuccess = super.onTerminate(successful);
		}
		catch (Exception e) {
			actionSuccess = false;
			throw new WrappingRuntimeException(e);
		}
		finally {
			clearAction(cdh, actionSuccess, pinCreationCancelled);
		}
		return actionSuccess;
	}

	@Override protected boolean createParamObject(boolean actionSuccess)
	{
		if (actionSuccess) {
			ISchemDiagram diag = (ISchemDiagram) getLocalModel().getSheet();
			UsageUpdateStrategy previousDWStrategy =
					setDesignWideUsageUpdateStrategy(UsageUpdateStrategy.UPDATE_ON_CHANGE);
			//UsageUpdateStrategy previousSharedStrategy =
			//		setSharedUsageUpdateStrategy(UsageUpdateStrategy.UPDATE_ON_CHANGE);
			try {
				LogicUtils.deferRegenerationOfSchemDeviceConnectors();
				// Put back the stuff that we created "prematurely"
				//
				// See if we have any symbols - place them down at the recorded locations.
				//
				List<Pair<IPinList, SymbolProxy>> symbolInstancesToUpdate =
						new ArrayList<Pair<IPinList, SymbolProxy>>();
				placeSymbols(symbolInstancesToUpdate, diag);

				CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(getDiagram());

				chs.cof.logical.cable.IPinList connectivityToUpdate;
				List<Pair<IPinList, SymbolProxy>> instancesToUpdate = new ArrayList<>();

				if (getState() == STATE_GENERATE) {
					connectivityToUpdate = auotGeneratePinList(instancesToUpdate, connectivityFinder);
				}
				else {
					connectivityToUpdate = generatePinList(symbolInstancesToUpdate, instancesToUpdate,
							connectivityFinder);
				}

				if (connectivityToUpdate != null) {
					LogicObjectLockFinder.tryEdit(connectivityToUpdate);
				}

				for (Pair<IPinList, SymbolProxy> instanceToUpdate : instancesToUpdate) {
					createUsageAndProcessFootprint(instanceToUpdate.getFirst(), instanceToUpdate.getSecond());
				}

				Runnable ghcRunner = () -> {
					if (m_device != null) {
						m_addPinActionHelper.regenerateGraphics(m_device);
					}
					generateHarnessConnectors();
				};

				boolean ghcExected = splitConductors(connectivityFinder, instancesToUpdate, ghcRunner);
				if (!ghcExected) {
					ghcRunner.run();
				}

				updateInternalConnectivity(connectivityToUpdate);
			}
			finally {
				setDesignWideUsageUpdateStrategy(previousDWStrategy);
				//	setSharedUsageUpdateStrategy(previousSharedStrategy);
			}
			applyStyle(diag);

			symbolCablePins.clear();
			m_sharedPinList.flush();
		}
		return actionSuccess;
	}

	private boolean splitConductors(CompositePinConnectivityFinder connectivityFinder,
			List<Pair<IPinList, SymbolProxy>> instancesToUpdate, Runnable ghcRunner)
	{
		List<IPinList> pinlists = new ArrayList<>();
		instancesToUpdate.forEach(instanceToUpdate -> pinlists.add(instanceToUpdate.getFirst()));
		if (!pinlists.isEmpty()) {
			ConductorSplitter splitter =
					ConductorSplitter.createConductorSplitter(pinlists.get(0), getState() == STATE_SYMBOL);
			return splitter.splitConductors(pinlists, (IGfxView) CAFUtils.getInstance().getActiveCapletView(),
					connectivityFinder, ghcRunner);
		}
		return false;
	}

	@Override protected void clearAction(boolean actionSuccess)
	{
		super.clearAction(actionSuccess);
	}

	protected void clearAction(CreationDeletionHelper cdh, boolean ok, boolean pinCreationCancelled)
	{
		//
		// Ugly way to do this [will always be called regardless of exception or whatever]
		// Set the undo idler here because this will clean up a bunch of objects on the temporary symbol layers
		// and clearing those will add undersierables to the undo stack.  Lets not.
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		if (pinCreationCancelled) {
			for (IUIDObject obj : m_addPinActionHelper.getCreatedPinObjects()) {
				UIDMgr.removeObject(obj.getUID());
			}
		}
		if (m_addPinListDialog != null) {
			m_addPinListDialog.cleanUp();
		}
		if (m_addPinActionHelper != null) {
			m_addPinActionHelper.cleanUp(ok &&
					(m_addPinListDialog == null || !m_addPinListDialog.getUsedPins().isEmpty()));
		}
		if (!ok && m_preemies.isEmpty() && addedDevices != null) {
			for (chs.cof.logical.cable.IPinList pinlist : addedDevices) {
				getLogicModel().getDesign().getConnectivity().removePinList(pinlist);
			}
		}
		if (addedDevices != null) {
			addedDevices.clear();
		}
		CAFUtils.getInstance().clearTempUndoableContainer();
		if (m_sharedPinList != null && !m_sharedPinList.isDeleted()) {
			m_sharedPinList.unlock();
		}
		getLocalModel().getDynamicGfxService().removeAllDynamicGfx();
		getLocalModel().getDynamicGfxService().removeAllTransientGfx();
		setState(STATE_LOITER);
		setFeedbackText(null);
		ICapletView cv = CAFUtils.getInstance().getActiveCapletView();
		if (cv != null) {
			((GfxView) cv).refresh();
			updateFeedback(cv);
		}
		removeTemporaryPinListObjects();

		transformList.clear();

		if (pinCreationCancelled) {
			chs.cof.logical.cable.IPinList cablePinList = getCablePinList();
			m_preemies.remove(cablePinList);
			// ensure that the shared pins and pinlists are deleted at the end, otherwise it would result in exception
			// while deleting cable pins
			Set<ISharedPin> sharedPins = m_preemies.stream().filter(obj -> obj instanceof ISharedPin)
					.map(obj -> (ISharedPin) obj)
					.collect(Collectors.toSet());
			m_preemies.removeAll(sharedPins);
			Set<ISharedPinList> sharedPinLists = m_preemies.stream().filter(obj -> obj instanceof ISharedPinList)
					.map(obj -> (ISharedPinList) obj)
					.collect(Collectors.toSet());
			m_preemies.removeAll(sharedPinLists);
			List<IUIDObject> toDelObjList = LogicUtils.getObjectsToBeDeleted(m_preemies);
			for (IUIDObject object : toDelObjList) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(object);
			}
			if (cablePinList != null) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(cablePinList);
			}
			for (ISharedPin sharedPin : sharedPins) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(sharedPin);
			}
			for (ISharedPinList sharedPinList : sharedPinLists) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(sharedPinList);
			}
			getController().getUndoableContainer().startEdit("Add Shared Device -Cancel");
			cdh.processObjects();
			getController().getUndoableContainer().endEdit();
		}
		//dts0100966739 [CH] java.lang.ClassCastException: chs.caf.caplet.helpers.VoidUndoObjectHelper cannot be cast to chs.cof.draw.ICompoundObject
		m_symbolDyn = null;
	}

	@Nullable
	protected chs.cof.logical.cable.IPinList generatePinList(List<Pair<IPinList, SymbolProxy>> symbolInstancesToUpdate,
			List<Pair<IPinList, SymbolProxy>> instancesToUpdate, CompositePinConnectivityFinder connectivityFinder)
	{
		//
		// It will be null if we only do symbols
		//
		chs.cof.logical.cable.IPinList connectivityToUpdate = null;

		if (m_device != null) {
			IPinList schemPinList = m_device;
			//
			// Add the shared pins then do the XRef.
			//
			m_addPinActionHelper.setIsReference(m_addPinListDialog.getReference());
			m_addPinActionHelper.addPins(m_device, getDiagram(), connectivityFinder);

			instancesToUpdate.add(new Pair<IPinList, SymbolProxy>(schemPinList, null));
			connectivityToUpdate = m_device.getConnectivity();
		}
		else if (!m_groupList.isEmpty()) {
			connectivityToUpdate = addNewSymbolCablePinsToOwner();
			// dts0100456387 - Shared pinlists with library parts and symbols created in C-project -
			// device conenctors are not displayed on the diagram
			// Defer calling createUsageAndProcessFootprint until after we have added connectivity pins
			IGfxView gview = (IGfxView) CAFUtils.getInstance().getActiveCapletView();
			IGfxContext context = gview.getGfxContext();
			for (Pair<IPinList, SymbolProxy> symbolInstance : symbolInstancesToUpdate) {
				IPinList schemPinlist = symbolInstance.getFirst();
				SymbolProxy sharedSymbolInstance = symbolInstance.getSecond();
				if (schemPinlist != null && sharedSymbolInstance != null) {
					instancesToUpdate.add(new Pair<IPinList, SymbolProxy>(schemPinlist, sharedSymbolInstance));
					connectivityFinder.collectConnectionMakers(context, schemPinlist.getAllPins());
				}
			}
		}

		return connectivityToUpdate;
	}

	private chs.cof.logical.cable.IPinList auotGeneratePinList(List<Pair<IPinList, SymbolProxy>> instancesToUpdate,
			CompositePinConnectivityFinder connectivityFinder)
	{
		int pinspacing = m_grid.getGridSpacing();
		ILocation loc = m_sharedAutoGenDynamic.getLocation();
		Point p1 = new Point(loc.getX(), loc.getY() + pinspacing);
		Point p2 = new Point(loc.getX() + AUTO_GEN_DEV_WIDTH * pinspacing, loc.getY() + pinspacing);
		m_device = (IPinList) createParamObject(p1, p2);
		// add the schem pin list to the diagram. This must be done before
		// addPins is called below
		addPins(m_device, connectivityFinder);

		instancesToUpdate.add(new Pair<IPinList, SymbolProxy>(m_device, null));
		return m_device.getConnectivity();
	}

	private void applyStyle(@NotNull ISchemDiagram diag)
	{
		if (m_device != null/* && diag != null*/) {
			PreferenceSetHelper.applyStyleSet(m_device.getObjectsForStyling(), diag, true);
			String projectUid =
					CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
			ISharedPinList sharedDevice = (ISharedPinList) m_device.getSharedObject();
			IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
			assert sharedDevice != null;
			auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_ADDED, null, projectUid,
					sharedDevice.getFullName(), sharedDevice.getUID().getString());
		}

		for (Object aM_groupList : m_groupList) {
			PinListGroup plg = (PinListGroup) aM_groupList;
			for (Object pinList : plg.getPinLists()) {
				IPinList symbolPinList = (IPinList) pinList;
				PreferenceSetHelper.applyStyleSet(symbolPinList.getObjectsForStyling(), diag, true);
			}
			//Audit Trail this event
			ISharedPinList sharedDevice = (ISharedPinList) plg.primaryPinList.getSharedObject();
			String projectUid =
					CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
			IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
			assert sharedDevice != null;
			auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_ADDED, null, projectUid,
					sharedDevice.getFullName(), sharedDevice.getUID().getString());
		}
	}

	private void generateHarnessConnectors()
	{
		//melmorsy - FEAT12331
		//Generate harness connectors for this device, if it has a Harness Footprint, and contained in a design-abstraction
		//that allows auto-cretion.
		//dts0100600448: We might have to generate shared connectors
		//So call for generation on shared device
		//dts0100598513: This could be a composite symbol
		//So look for primary pin list and get the shared device
		//dts0101195972 for non-symbol case the GHC is already being done before coming here.
		//handle assisted flow also.
		if (getState() == STATE_SYMBOL && !m_groupList.isEmpty()) {
			IPinList[] primaryPinLists = new IPinList[m_groupList.size()];
			int idx = 0;
			for (PinListGroup pinListGroup : m_groupList) {
				primaryPinLists[idx] = pinListGroup.primaryPinList;
				++idx;
			}
			ISchemDiagram diagram = (ISchemDiagram) getLocalModel().getSheet();
			ConnectivityGHCHelper.generateHarnessConnectors(diagram, true, false, primaryPinLists);
		}
	}

	private void placeSymbols(List<Pair<IPinList, SymbolProxy>> symbolInstancesToUpdate, ISchemDiagram diag)
	{
		for (Object aM_groupList : m_groupList) {
			PinListGroup plg = (PinListGroup) aM_groupList;
			IPinList mainPinList = plg.primaryPinList;
			IDynamicGfx graphics = plg.dynamicGraphics;
			ILocation placedLoc = graphics.getLocation();
			mainPinList.getLocation().setX(placedLoc.getX());
			mainPinList.getLocation().setY(placedLoc.getY());
			for (Object pinList : plg.getPinLists()) {
				IPinList symbolPinList = (IPinList) pinList;
				SymbolProxy inst = m_pinListToSymbolMap.get(symbolPinList);
				if (symbolPinList != mainPinList) {
					// Adjust the block pinlist location relative to the 'main' pinlist
					ILocation blockLoc = inst.getBlock().getLocation();
					ILocation plLoc = symbolPinList.getLocation();
					plLoc.setX(placedLoc.getX() + blockLoc.getX());
					plLoc.setY(placedLoc.getY() + blockLoc.getY());
				}
				else if (mainPinList.getNumObjects() == 0) {
					// We're not going to keep this one, don't bother making extra stuff for it
					continue;
				}
				int flipState = 0;
				for (TransformType transformType : transformList.pullReadOnlySafeList(graphics)) {
					if (TransformType.Rotate.equals(transformType)) {
						symbolPinList.rotate(DEG_90, mainPinList.getLocation().getX(),
								mainPinList.getLocation().getY(), 0, 0);
					}
					else if (TransformType.Flip.equals(transformType)) {
						switch (flipState) {
							case (0):
								symbolPinList.flip(FlipAxisEnum.XAxis, mainPinList.getLocation().getX(),
										mainPinList.getLocation().getY(), 0, 0);
								break;
							case (1):
								symbolPinList.flip(FlipAxisEnum.XAxis, mainPinList.getLocation().getX(),
										mainPinList.getLocation().getY(), 0, 0);
								symbolPinList.flip(FlipAxisEnum.YAxis, mainPinList.getLocation().getX(),
										mainPinList.getLocation().getY(), 0, 0);
								break;
							case (2):
								symbolPinList.flip(FlipAxisEnum.YAxis, mainPinList.getLocation().getX(),
										mainPinList.getLocation().getY(), 0, 0);
								break;
							default:
								break;
						}
						flipState = (flipState + 1) % 3;
					}
				}
				diag.addObject(symbolPinList);

				// Record shared pin usages
				for (IPin pin : symbolPinList.getObjects(IPin.class)) {
					ISharedPin shpin = pin.getConnectivity().getSharedPin();
					IPin symPin =
							m_sharedPinList.getSymbolPin(inst.getSymbolDef(), inst.getInstanceNumber(), shpin);

					if (symPin != null) {
						pin.setSymbolPin(symPin);
					}
					pin.setReference(m_addPinListDialog.getReference());
				}

				SymbolProxy symInst = m_pinListToSymbolMap.get(symbolPinList);
				// dts0100456387 - Shared pinlists with library parts and symbols created in C-project -
				// device conenctors are not displayed on the diagram
				// Defer calling createUsageAndProcessFootprint until after we have added connectivity pins
				symbolInstancesToUpdate
						.add(new Pair<IPinList, SymbolProxy>(symbolPinList, symInst));

				//dts0100710644 - Shared symbol graphics missing after flatten the block in csymbol
				IBlock block = symInst.getBlock();
				if (symInst.isBlock() && block != null) {
					//If the parent composite is selected, we don't want to add block symbol graphics
					boolean bAddBlockGraphics = !m_pinListToSymbolMap.values().contains(symInst.getContainingSymbol());
					if (bAddBlockGraphics) {
						double scale = getScale(block.getBlockOwner());
						SymbolUtils.synchronizeBlockSourceGfx(block, scale, symbolPinList,
								SymbolUtils.sourceSymbolPinDeriver(block.getBlockOwner()), false);
						if (!SymbolUtils.isUnitScale(scale)) {
							SymbolUtils.adjustOffGridPinsToAGridPoint(symbolPinList, getGrid());
						}
					}
				}
			}

			if (plg.primaryPinList.getNumObjects() == 0) {
				// If there's nothing in the top-level pinlist, then we have no further use for it.(this will
				// happen when the composite symbol contains nothing except blocks.
				plg.getPinLists().remove(plg.primaryPinList);
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(mainPinList);
			}

			// Glue the pinlists generated from the symbol together
			for (int i = 0; i < plg.getPinLists().size() - 1; i++) {
				IPinList aPinList = plg.getPinLists().get(i);
				for (int j = i + 1; j < plg.getPinLists().size(); j++) {
					IPinList anotherPinList = plg.getPinLists().get(j);
					aPinList.addAttachedObject(anotherPinList);
					anotherPinList.addAttachedObject(aPinList);
				}
			}
		}
	}

	private void removeTemporaryPinListObjects()
	{
		// To remove the real objects that will exist on the design after the action has completed
		// (these are the transient gfx uids)
		List<chs.cof.logical.cable.IPinList> dynamicPLCable = new ArrayList<chs.cof.logical.cable.IPinList>();
		List<IPinList> dynamicPLSchem = new ArrayList<IPinList>();
		if (m_groupList != null) {
			for (Object aM_groupList : m_groupList) {
				PinListGroup plg = (PinListGroup) aM_groupList;
				for (IGfxObject obj : plg.dynamicGraphics.getObjects()) {
					IUIDObject uidObj = (IUIDObject) obj;
					if (uidObj != null) {
						//dts0100884836 AddSharedObject Instance, undo, redo will add an unplaced device in the connectivity
						if (uidObj instanceof IPinList) {
							dynamicPLSchem.add(((IPinList) uidObj));
							dynamicPLCable.add(((IPinList) uidObj).getConnectivity());
						}
					}
				}
			}
		}
		//dts0100884836 AddSharedObject Instance, undo, redo will add an unplaced device in the connectivity
		//Instead, move this to DeleteHelper.delete(), but it would expect schemPL.digram set etc, for these
		// temporary objects, they are not set and hence would get exception. So, call delete() instead
		for (IPinList schemPL : dynamicPLSchem) {
			schemPL.delete();
		}
		for (chs.cof.logical.cable.IPinList cablePL : dynamicPLCable) {
			cablePL.delete();
		}
	}

	protected final chs.cof.logical.cable.IPinList addNewSymbolCablePinsToOwner()
	{
		chs.cof.logical.cable.IPinList symbolPinList = getLogicDevice();
		for (IGenericPin cablePin : symbolCablePins) {
			if (cablePin.getOwner() == null) {
				symbolPinList.addGenericPin(cablePin);
				cablePin.setOwner(symbolPinList);
			}
		}
		return symbolPinList;
	}

	@Nullable private chs.cof.logical.cable.IPinList getCablePinList()
	{
		chs.cof.logical.cable.IPinList cablepinList = null;
		for (Object obj : m_preemies) {
			if (obj instanceof chs.cof.logical.cable.IPinList) {
				cablepinList = (chs.cof.logical.cable.IPinList) obj;
			}
		}
		return cablepinList;
	}

	private void createUsageAndProcessFootprint(IPinList schemPinList, @Nullable SymbolProxy symInst)
	{
		if (schemPinList == null) {
			return;
		}
		ISymbolDef symDef = null;
		int instNum = -1;
		IBlock block = null;
		if (symInst != null) {
			symDef = symInst.getSymbolDef();
			instNum = symInst.getInstanceNumber();
			block = symInst.getBlock();
		}

		setHome(schemPinList, symDef, block);
		IECAttributeResolver.inheritIECAttributesIfNotPresent(schemPinList.getDiagram(), schemPinList);

		IUID symbolDefUID = symDef != null ? symDef.getUID() : null;
		IUID blockUID = block != null ? block.getUID() : null;
		schemPinList.setSymbolInformation(symbolDefUID, blockUID, instNum);
		assert schemPinList.getConnectivity().getSharedPinList() == m_sharedPinList;

		//
		// Rebuild the device connectors [if there is a footprint]
		//
		// Don't rebuild from scratch, as:
		//  1. The Footprint may have changed
		//  2. The info must be tied up with the shared device.
		// - so we get it from the Shared Device directly [should be fast!]
		//
		ISheet diag = getLocalModel().getSheet();
		rebuildDeviceConnectors(schemPinList, m_sharedPinList, diag, m_generator);
	}

	private void setHome(IPinList schemPinList, ISymbolDef symDef, IBlock block)
	{
		boolean home = true;
		if (!m_sharedUsageInfo.isEmpty()) {
			if (block == null) {
				home = false;
			}
			else {
				for (ISharedPinListUsage splu : m_sharedUsageInfo) {
					if (isEquivalentMatchOfUsageForHome(splu, symDef, block)) {
						home = false;
						break;
					}
				}
			}
		}
		// Have to check locally, for things that have previously been added in this action.
		ILogicDesign design = getLocalModel().getDesign();
		if (home && block != null) {
			IDesignSharedUsageMgr dsuMgr = design.getSharedUsageMgr();
			for (Iterator<IDesignSharedUsage> dsuitr = dsuMgr.getUsages(m_sharedPinList).iterator();
					home && dsuitr.hasNext(); ) {
				IDesignSharedPinListUsage dsplu = (IDesignSharedPinListUsage) dsuitr.next();
				if (isEquivalentMatchOfUsageForHome(dsplu, symDef, block)) {
					home = false;
				}
			}
		}

		// must be set before usage construction
		schemPinList.setHome(home);
		if (symDef != null && block == null && design != null) {
			SymbolUtils.setupHomeForPins(schemPinList, symDef, design);
		}
	}

	private boolean isEquivalentMatchOfUsageForHome(@NotNull IDesignSharedPinListUsage dsplu,
			@NotNull ISymbolDef symDef, @NotNull IBlock block)
	{
		//we must be having symbol and its blocks loaded. because we are placing them here.
		return (dsplu.getBlock() == block) || (dsplu.getBlock() == null && dsplu.getSymbol() == symDef);
	}

	private boolean isEquivalentMatchOfUsageForHome(@NotNull ISharedPinListUsage splu, @NotNull ISymbolDef symDef,
			@NotNull IBlock block)
	{
		//we must be having symbol and its blocks loaded. because we are placing them here.
		return (splu.getBlock() == block) || (splu.getBlock() == null && splu.getSymbol() == symDef);
	}

	protected void rebuildDeviceConnectors(IPinList schemPinList, ISharedPinList sharedPinlist, ISheet diag,
			Generator generator)
	{

		chs.cof.logical.cable.IPinList plc = schemPinList.getConnectivity();
		if (plc instanceof IDevice) {
			IDevice pld = (IDevice) plc;
			if (plc.getSharedObject() != sharedPinlist) {
				pld.setFootprintId(sharedPinlist.getFootprintId());
				pld.setFootprintDescription(sharedPinlist.getFootprintDescription());
			}

			if (sharedPinlist instanceof ISharedDevice &&
					((ISharedDevice) sharedPinlist).getNumDeviceConnectors() != 0) {
				IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(getDiagram());
				GeneratorParameters genParams = new GeneratorParameters(((IGriddable) diag).getGrid(), styleSet);
				SharedPinListHelper.fixupSharedDeviceConnectors(pld);
				generator.regenerateSchemDeviceConnectors(schemPinList, genParams, null);
			}
		}
	}

	@SuppressWarnings({"NumericCastThatLosesPrecision"}) protected IGfxObject createParamObject(Point p1, Point p2)
	{
		// Get our factories
		final ISchemFactory schemFactory = FactoryMgr.getSchemFactory();
		final ICommonFactory commonFactory = FactoryMgr.getCommonFactory();

		DynamicRotationIndicator indicator = getRotationIndicator();
		IParameterized params = commonFactory.createParameterized();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(getDiagram());
		gp.setNewObject(true);

		Generator generator = Generator.getGenerator();
		GeneratorStyle gs = generator.getStyle();
		gs.addDefaults(params, "device");

		SizeHelper sizeH = new SizeHelper(p1, p2, indicator.getVertical(), params, gp);
		sizeH.setMinModelWidth((int) (gp.getSpacing() * gp.getWidth()));

		int height = sizeH.getModelHeight();
		int width = sizeH.getModelWidth();
		Point lowerLeft = sizeH.getModelLocation();

		IExtent sharedObjExt = commonFactory.constructExtent(0, 0, width, height);
		chs.cof.logical.cable.IPinList device = getLogicDevice();

		// Create visible schem representation & adds pins to it as well as the connectivity.
		IUID uid = commonFactory.createUID();
		IPinList schem_dev = schemFactory.constructPinList(uid, device, lowerLeft.x, lowerLeft.y);
		params.setExtent(sharedObjExt);
		schem_dev.setParameterized(params);
		ISchemDiagram diagram = (ISchemDiagram) getLocalModel().getSheet();
		diagram.addObject(schem_dev);
		//
		// Get the generator, add the defaults, and go!
		//
		generator.generateDevice(schem_dev, gp, Generator.REGENERATE_PROPERTIES);
		sizeH.rotateModel(schem_dev);

		return schem_dev;
	}

	private void addPins(@NotNull IPinList schem_dev, CompositePinConnectivityFinder connectivityFinder)
	{
		if (getState() != STATE_GENERATE) {
			return;
		}

		Collection<IAbstractPin> pins = getAbstractPins(SharedPinListEditUtils
				.createAndAddCablePins(getLogicDevice(), m_addPinListDialog.getUsedPins(),
						m_addPinListDialog.getReference()));

		ISchemDiagram diagram = (ISchemDiagram) getLocalModel().getSheet();
		PinListDeferredAddPinHelper ph = new PinListDeferredAddPinHelper(schem_dev, m_addPinListDialog.getReference());
		ph.addPins(diagram, pins, connectivityFinder);
	}

	private Collection<IAbstractPin> getAbstractPins(Collection pins)
	{
		Collection<IAbstractPin> abstractPinCollection = new ArrayList<IAbstractPin>(pins.size());
		for (Object pin : pins) {
			IAbstractPin abstractPin = null;
			if (pin instanceof IAbstractPin) {
				abstractPin = (IAbstractPin) pin;
			}
			else if (pin instanceof IGenericPinProxy) {
				IGenericPin gpin = ((IGenericPinProxy) pin).getCablePin();
				if (gpin instanceof IAbstractPin) {
					abstractPin = (IAbstractPin) gpin;
				}
			}
			if (abstractPin != null) {
				abstractPinCollection.add(abstractPin);
			}
		}

		return abstractPinCollection;
	}

	@Nullable
	protected Class<?> snappingSource()
	{
		try {
			return Class.forName("chs.cof.logical.schem.IPinList");
		}
		catch (ClassNotFoundException ex) {
			return null;
		}
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_deviceCursor;
	}

	public void mousePressed(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return; // Does nothing.
			case STATE_PARAM:
				super.mousePressed(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mousePressed(e);
				break;
			case STATE_GENERATE:
				super.mousePressed(e);
				break;
		}
		updateStatusbarText();
	}

	public void mouseEntered(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return; // Does nothing.
			case STATE_PARAM:
				super.mouseEntered(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseEntered(e);
				break;
			case STATE_GENERATE:
				super.mouseEntered(e);
				break;
		}
	}

	public void mouseExited(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return; // Does nothing.
			case STATE_PARAM:
				super.mouseExited(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseExited(e);
				break;
			case STATE_GENERATE:
				super.mouseEntered(e);
				break;
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (getState() == STATE_SYMBOL) {
			setFeedbackText(null);
			updateFeedback(e.getSource());

			cleanupTrans(false);

			//
			// More symbols to add...
			//
			//
			// Get the next one [if any].
			//
			if (!m_symbolDynList.isEmpty()) {
				PinListGroup plg = m_symbolDynList.remove(0);
				m_symbolDyn = plg.dynamicGraphics;
				getLocalModel().getDynamicGfxService().addTransientGfx(m_symbolDyn);
				super.onActivate(m_restartEvent);
			}
			else {
				if (!m_addPinListDialog.isSymbelSelectionSelected() && m_addPinListDialog.getNumUsedPins() > 0) {
					setState(STATE_PARAM); // move on to adding the parameterized symbol
					super.onActivate(m_restartEvent);
				}
				else {
					//
					// Terminate now.
					//
					getController().getActionMgr().terminateActiveAction(true);
				}
				return;
			}
		}
		else if (getState() == STATE_PARAM) {
			if (m_goingToTerminate) {
				setFeedbackText(null);
				updateFeedback(e.getSource());
				cleanupTrans(false);
				final List<ISmartPoint> devicePointList = getPointList();

				if (devicePointList != null) {
					m_device = (IPinList) createDisplayObject(devicePointList);
					// We created stuff before onTerminate() was called - cache what's in the
					// CreationDeletionHelper and clear it out.
					CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
					m_preemies.addAll(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
					cdh.clearNewObjects();
					if (m_device != null && m_addPinListDialog.getNumUsedPins() > 0) {
						m_addPinActionHelper.setIsReference(m_addPinListDialog.getReference());
						m_addPinActionHelper.setPlaceAsStack(m_addPinListDialog.getPlaceAsStack());
						m_addPinActionHelper.setPlaceAsGroup(m_addPinListDialog.getPlaceAsGroup());
						m_addPinActionHelper.setUp(m_device, m_addPinListDialog.getUsedPins());
						setState(STATE_PINS);
					}
					else {
						getController().getActionMgr().terminateActiveAction(m_device != null);
					}
				}
			}
		}
		else if (getState() == STATE_GENERATE) {
			getController().getActionMgr().terminateActiveAction(m_sharedAutoGenDynamic != null);
		}
		else {
			m_addPinActionHelper.mouseReleased(e);
		}
		updateStatusbarText();
	}

	public void mouseClicked(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return; // does nothing
			case STATE_PARAM:
				super.mouseClicked(e);
				break;
			case STATE_PINS:
				// If this is the first pin being placed, redraw the transient graphics
				// to indicate where the pins can be placed.
				if (pinRect == null) {
					if (m_dynGfx instanceof DynamicRectangle) {
						// If we drew a simple rectangle ...
						// Redraw the initial transient rectangle grey and dotted.

						DynamicRectangle rect = (DynamicRectangle) m_dynGfx;
						IGfxObject innerRect = rect.getDrawableNoGrip();
						rect.removeSmartPoints();
						IColor color = FactoryMgr.getDrawFactory().lookupColor("transient");
						IGfxAttribute attr = new WritableGfxAttribute(color,
								FactoryMgr.getDrawFactory().constructGraphicSize(1, null), LineStyle.DOTTED);
						innerRect.setAttribute(attr);
						// Draw a new solid-outline rectangle to indicate where the pins can be placed.
						ILocation loc = innerRect.getLocation();
						int x = loc.getX();
						int y = loc.getY();

						//FEAT 3097: get extent of innerRect not rect, we don't want to include the rotation arrows.
						IExtent ext = innerRect.getExtent();
						int w = ext.getWidth();
						int h = ext.getHeight();

						pinRect = FactoryMgr.getDrawFactory().constructRectangle(x, y, x + w, y + h);
						getDynamicGfxService().addTransientGfx(pinRect);
					}
				}
				// Add the next pin specified by the dialog.
				updateStatusbarText();
				m_addPinActionHelper.mouseClicked(e);
				break;
			case STATE_GENERATE:
				super.mouseClicked(e);
				break;
		}
		updateStatusbarText();
	}

	public void mouseDragged(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return; // does nothing
			case STATE_PARAM:
				super.mouseDragged(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseDragged(e);
				break;
			case STATE_GENERATE:
				super.mouseDragged(e);
				break;
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		switch (getState()) {
			case STATE_SYMBOL:
				Point wp = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
				ILocation loc = m_symbolDyn.getLocation();
				loc.setLocation(m_grid.snap(wp.x), m_grid.snap(wp.y));
				m_symbolDyn.setLocation(loc);

				//noinspection fallthrough
			case STATE_PARAM:
				super.mouseMoved(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseMoved(e);
				break;
			case STATE_GENERATE:
				if (m_sharedAutoGenDynamic != null) {
					Point p = new Point(e.getX(), e.getY());
					GfxView gv = (GfxView) CAFUtils.getInstance().getActiveCapletView();
					p = gv.deviceToWorld(p);

					ILocation devLoc = new Location(m_grid.snap(p.x), m_grid.snap(p.y));
					m_sharedAutoGenDynamic.setLocation(devLoc);
				}
				super.mouseMoved(e);
				break;
		}
	}

	/**
	 * Set the status text for this action
	 */
	@Nullable
	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_SYMBOL:
				return ResourceMgr
						.getString(AddSharedDeviceAction.class, "AddSharedDeviceAction.StatusBar.SymbolAdd.text");
			case STATE_PARAM:
				return ResourceMgr
						.getString(AddSharedDeviceAction.class, "AddSharedDeviceAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
	}

	protected boolean isContextButton(Object src)
	{
		return src instanceof AbstractButton
				&& ((AbstractButton) src).getActionCommand()
				.equals(getCtxCommand()); // Test for same object, not string equal
	}

	protected IPinList createSchemFromSymbol(ISymbolDef associatedSymbol, int instNum, @Nullable IBlock block,
			@Nullable ISymbolDef blockSource)
	{
		IPinList symbolAsPinlist = FactoryMgr.getSchemFactory().constructPinList(
				FactoryMgr.getCommonFactory().createUID(), getLogicDevice(), 0, 0);
		createSchemFromSymbol(associatedSymbol, instNum, block, blockSource, symbolAsPinlist, null);
		return symbolAsPinlist;
	}

	private void createSchemFromSymbol(ISymbolDef associatedSymbol, int instNum, @Nullable IBlock block,
			@Nullable ISymbolDef blockSource, IPinList symbolAsPinlist, @Nullable IBlock referenceBlockToTransform)
	{
		Collection<ISharedPin> sharedPins;
		if (block != null) {
			// when adding a shared composite instance, we recurse into here for blocks
			// need to make sure that we don't add multiple cable pins to the symbolCablePins field for the same shared pins
			Set<ISharedPin> doneShared = new HashSet<ISharedPin>();
			for (IGenericPin pin : symbolCablePins) {
				ISharedPin spin = pin.getSharedPin();
				assert spin != null;
				boolean didNotContain = doneShared.add(spin);
				assert didNotContain;
			}

			sharedPins = new ArrayList<ISharedPin>();
			//FEAT00013786: stack pins not allowed in symbols
			assert block.getPinList().getStackPins().isEmpty();
			for (IPin pin : block.getPinList().getPins()) {
				ISharedPin sharedPin = m_sharedPinList.getSharedPin(associatedSymbol, instNum, pin);
				if (sharedPin != null && !doneShared.contains(sharedPin)) {
					sharedPins.add(sharedPin);
				}
			}
		}
		else {
			sharedPins = m_sharedPinList.getSymbolInstancePinMapping(associatedSymbol, instNum).keySet();
		}

		//
		// Some things (splices) may be added directly without going via the dialog - in these cases,
		// thet are treated as normal pins always.
		//
		boolean isReference = false;
		if (m_addPinListDialog != null) {
			isReference = m_addPinListDialog.getReference();
		}

		// dts0100589691 - Primary Fix: remove any pins previously created by adding other instances in same action
		for (IGenericPin pin : symbolCablePins) {
			ISharedPin spin = pin.getSharedPin();
			sharedPins.remove(spin);
		}

		if (!sharedPins.isEmpty()) {
			symbolCablePins.addAll(getAbstractPins(SharedPinListEditUtils
					.createAndAddCablePins(getLogicDevice(),
							sharedPins.stream().map(aSharedPin -> new PinProxy(aSharedPin))
									.collect(Collectors.toList()),
							isReference)));
		}

		// Need to replicate manually.
//		double scale = (double) m_grid.getGridSpacing() / SYMBOL_SPACING;
		Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);

		ISymbolDef symbolToCopy;
		if (block != null) {
			symbolToCopy = block;
		}
		else if (blockSource != null) {

			symbolToCopy = blockSource;
		}
		else {
			symbolToCopy = associatedSymbol;
		}

		// Seed the replicator map
		// ...with the schem and cable pinlists
		replicator.setNewObject(symbolToCopy.getPinList().getUID(), symbolAsPinlist);
		replicator.setNewObject(symbolToCopy.getPinList().getConnectivity().getUID(), getLogicDevice());
		// ...and the cable pins
		ISharedPinList spl = getSharedPinList();
		for (Object cablePin : symbolCablePins) {
			IAbstractPin ap = (IAbstractPin) cablePin;
			IPin symbolPin = spl.getSymbolPin(associatedSymbol, instNum, ap.getSharedPin());
			if (symbolPin != null) {
				if (symbolToCopy == blockSource) {
					// If we're copying the block's source, don't add pins from other blocks to the replicator map.
					// If more than one block on the composite is from the same source, the mapping will be overwritten.
					if (block.getPinList().findPin(symbolPin.getConnectivity()) != null) {
						replicator.setNewObject(symbolPin.getConnectivity().getReference(), ap);
					}
				}
				else {
					replicator.setNewObject(symbolPin.getConnectivity().getUID(), ap);
				}
			}
		}
		// ...create a property map to seed the replicator map later
		Map<String, IProperty> propMap = new HashMap<String, IProperty>();
		for (IPropertyIterator pitr = getSharedPinList().getProperties(); pitr.hasNext(); ) {
			IProperty prop = pitr.getNext();
			propMap.put(prop.getName(), prop);
		}

		// Copy pins, property text, name text, cross-reference text
		copyReferentialGraphics(symbolAsPinlist, symbolToCopy, propMap, replicator, referenceBlockToTransform);
	}

	protected void copyReferentialGraphics(IPinList symbolAsPinlist, ISymbolDef symbolToCopy,
			Map<String, IProperty> propMap, Replicator replicator, @Nullable IBlock referenceBlockToTransform)
	{

		if (referenceBlockToTransform == null) {
			if (symbolToCopy instanceof IBlock) {

				symbolAsPinlist.setAttribute(
						Replicator.replicateGfxAttribute(((IGfxAttributeContainer) symbolToCopy).getAttribute()));
			}
			else {
				IPinList pinList = symbolToCopy.getPinList();
				if (pinList != null) {
					symbolAsPinlist.setAttribute(Replicator.replicateGfxAttribute(pinList.getAttribute()));
				}
			}
		}

		double scale = symbolToCopy instanceof IBlock ? getScale(((IBlock) symbolToCopy).getBlockOwner()) :
				getScale(symbolToCopy);

		for (IGfxObjectIterator gitr = symbolToCopy.getGfx().getObjects(); gitr.hasNext(); ) {
			IGfxObject go = gitr.getNext();
			if (go instanceof ISchemInternalLink) {
				continue;
			}
			IGfxObject gorepl = null;
			if (go instanceof IPin) {
				// Handle differently, as we want to map to the original object.
				IPin symPin = (IPin) go;
				IAbstractPin ap = (IAbstractPin) replicator.getNewObject(symPin.getConnectivity().getUID());
				if (ap == null) {
					continue;
				}
				IPin newSchemPin = FactoryMgr.getSchemFactory().constructPin(FactoryMgr.getCommonFactory().createUID(),
						ap,
						Replicator.scale(symPin.getLocation().getX(), scale),
						Replicator.scale(symPin.getLocation().getY(), scale));
				newSchemPin.setAttribute(Replicator.replicateGfxAttribute(symPin.getAttribute()));

				for (IGfxObjectIterator pitr = symPin.getObjects(); pitr.hasNext(); ) {
					IGfxObject gobj = pitr.getNext();

					IGfxObject replica = null;
					if (gobj instanceof IXRefPlaceholder) {
						CrossReferenceUtils.createRealXRefFromPlaceholder((IXRefPlaceholder) gobj, newSchemPin);
					}
					else {
						replica = replicator.replicateGfx(gobj, scale);
					}
					if (symbolToCopy instanceof IBlock && replica instanceof IText) {
						IText text = (IText) replica;
						IText symText = findSymPinText(symPin, (IText) gobj);
						if (symText != null) {
							Replicator.transferTextInfo(text, symText);
						}
					}
					if (replica != null) {
						newSchemPin.addObject(replica);
					}
					if (AttributeUtils.isNameText(gobj)) {
						if (symbolToCopy instanceof IBlock) {
							IAttributeText symNameText = findSymPinNameText(symPin);
							if (symNameText != null) {
								IAttributeText nameText = (IAttributeText) replica;
								Replicator.transferTextInfo(nameText, symNameText);
								nameText.setLocation(FactoryMgr.getCommonFactory().constructLocation(
										symNameText.getLocation().getX(), symNameText.getLocation().getY()));
								nameText.setRotation(symNameText.getRotation());
							}
						}
					}
				}
				gorepl = newSchemPin;
			}
			else if (go instanceof IPropText) {
				IPropText gref = (IPropText) go;
				IProperty prop = gref.getProperty();
				IProperty sprop = propMap.get(prop.getName());
				//
				// Paranoid check.
				//
				if (sprop != null) {
//					replicator.setNewObject(prop.getUID(), sprop);
					gorepl = replicator.replicateGfx(go, scale);
				}
			}
			else if (go instanceof IXRefPlaceholder) {
				CrossReferenceUtils.createRealXRefFromPlaceholder((IXRefPlaceholder) go, symbolAsPinlist,
						referenceBlockToTransform);
			}
			else {
				if (go instanceof IArrowable) {
					Collection<IGfxObject> replicatedGfxs = replicator.replicateCompoundGfx(go, scale);
					for (IGfxObject replicatedGfx : replicatedGfxs) {
						applyBlockInformationOnReplicatedBlockGfx(referenceBlockToTransform, replicatedGfx, scale);
						symbolAsPinlist.addObject(replicatedGfx);
					}
				}
				else {
					gorepl = replicator.replicateGfx(go, scale);
				}
			}

			if (gorepl != null) {
				ILocation reploc = gorepl.getLocation();
				reploc.setX(reploc.getX());
				reploc.setY(reploc.getY());
				if (!(gorepl instanceof IXRefText)) {
					applyBlockInformationOnReplicatedBlockGfx(referenceBlockToTransform, gorepl, scale);
					symbolAsPinlist.addObject(gorepl);
				}
			}
		}
		if (!SymbolUtils.isUnitScale(scale)) {
			SymbolUtils.adjustOffGridPinsToAGridPoint(symbolAsPinlist, getGrid());
		}
	}

	private double getScale(@NotNull ISymbolDef symbolDef)
	{
		boolean shouldScale =
				symbolDef.getSymbolType() == SymbolTypeEnum.COMMENT || getController().getCaplet().isLayoutCaplet();
		return shouldScale ? SymbolUtils.getSymbolScale(symbolDef, getGrid()) : 1;
	}

	@NotNull private IGrid getGrid()
	{
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		return diagram.getGrid();
	}

	private void applyBlockInformationOnReplicatedBlockGfx(@Nullable IBlock blk, IGfxObject dgfx, double scale)
	{
		if (blk != null && dgfx != null) {
			ITransform trans = ((ITransformCompound) blk).getTransform();
			if (trans != null) {
				AffineTransform blkTrans = trans.getAffineTransform();
				TransformUtils.transform(blk, blkTrans, dgfx, scale);
			}
			// Merge the attributes from the block to the graphics objects.
			IGfxAttribute blkAttr = blk.getAttribute();
			if (blkAttr != null) {
				IGfxAttribute dgfxAttr = dgfx.getAttribute();
				IWritableGfxAttribute mergedAttr = FactoryMgr.getDrawFactory().createGfxAttribute();
				//noinspection ConstantConditions
				dgfx.setAttribute(GfxAttributeHelper.merge(blkAttr, dgfxAttr, mergedAttr));
			}
		}
	}

	@Nullable
	protected IText findSymPinText(IPin pin, IText text)
	{
		IText symText = null;
		IGfxObjectIterator iter = pin.getObjects();
		while (iter.hasNext()) {
			IGfxObject obj = iter.getNext();
			if (obj instanceof IText) {
				if (((IText) obj).getString().equals(text.getString())) {
					symText = (IText) obj;
					break;
				}
			}
		}
		return symText;
	}

	@Nullable
	protected IAttributeText findSymPinNameText(IPin pin)
	{
		IAttributeText name = null;
		IGfxObjectIterator iter = pin.getObjects();
		while (iter.hasNext()) {
			IGfxObject obj = iter.getNext();
			if (AttributeUtils.isNameText(obj)) {
				name = (IAttributeText) obj;
				break;
			}
		}

		return name;
	}

	chs.cof.logical.cable.IPinList getLogicDevice()
	{
		ISharedPinList sharedDevice = m_addPinListDialog.getSharedPinList();
		chs.cof.logical.cable.IPinList device =
				getLogicModel().getDesign().getConnectivity().findSharedPinList(sharedDevice);
		if (device == null) {
			m_bNewDevice = true;
			device = createLogicDevice();
			getLogicModel().getDesign().getConnectivity().addPinList(device);
			addedDevices.add(device);
			device.setSharedPinList(sharedDevice);
		}
		return device;
	}

	protected chs.cof.logical.cable.IPinList createLogicDevice()
	{
		return FactoryMgr.getCableFactory().createDevice(FactoryMgr.getCommonFactory().createUID());
	}

	public boolean isEnabled()
	{
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
		ISharedPinList spl = getOperand();
		if (!super.isEnabled()) {
			return false;
		}
		else if (spl == null) {
			// Null operand means that the device will be chosen interactively.
			return true;
		}
		else {
			return true;
		}
	}

	@Nullable
	protected ISharedPinList getOperand()
	{
		if (m_sharedSelectMgr != null && m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_sharedSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedPinList) {
				ISharedPinList sharedPinlist = (ISharedPinList) uidObj;
				if (sharedPinlist.getType().equals(getType())) {
					return sharedPinlist;
				}
			}
		}
		return null;
	}

	protected void stripText(ICompoundObject comp)
	{
		for (IGfxObjectIterator itr = comp.getObjects(); itr.hasNext(); ) {
			IGfxObject gobj = itr.getNext();
			if (gobj instanceof XRefTextWrapper) {
				((ICrossReferenceable) comp).removeAllXRefTextContainers();
				CreationDeletionHelper.getTheCreationHelper().removeCreationObject((IUIDObject) gobj);
				UIDMgr.removeObject(((IUIDProvider) gobj).getUID());
			}
			else if (gobj instanceof IText) {
				comp.removeObject(gobj);
				if (gobj instanceof IUIDObject) {
					//dts0100565516: we should remove the property text from the UIDMgr.
					CreationDeletionHelper.getTheCreationHelper().removeCreationObject((IUIDObject) gobj);
					UIDMgr.removeObject(((IUIDProvider) gobj).getUID());
				}
			}
			else if (gobj instanceof ICompoundObject) {
				stripText((ICompoundObject) gobj);
			}
		}
	}

	private class PinListGroup
	{

		private List<IPinList> pinLists = new ArrayList<IPinList>();
		public IDynamicGfx dynamicGraphics =
				getDynamicGfxService().getFactory().constructCompound(FactoryMgr.getCommonFactory().createLocation());
		public IPinList primaryPinList;

		private PinListGroup(SymbolProxy symInst)
		{
			IBlock block = null;
			ISymbolDef blockSource = null;
			if (symInst.isBlock()) {
				// If the symbol instance is the primary, then it must be an individual placement of the block, so get the
				// graphics from the original block symbol definition
				ISymbolRef symRef = symInst.getBlock().getSymbolRef();
				blockSource = (ISymbolDef) m_symLibMgr.getReferencedSymbol(symRef);
				if (blockSource != null) {
					blockSource = (ISymbolDef) blockSource.getContainerLibrary().loadFully(blockSource);
				}

				block = symInst.getBlock();
			}
			primaryPinList =
					createSchemFromSymbol(symInst.getSymbolDef(), symInst.getInstanceNumber(), block, blockSource);
			m_pinListToSymbolMap.put(primaryPinList, symInst);
			getPinLists().add(primaryPinList);

			IPinList dynPinList = getDynamicPinListForSymbol(symInst, block, blockSource);
			dynamicGraphics.addObject(dynPinList);
			for (IAbstractSchemPin pin : dynPinList.getAllPins()) {
				dynamicGraphics.addObject(pin);
				// Removed for dts0100506577 - to ensure that the object uids is available in the UIDMgr
				// in order to be able to rotate the transient gfx while insertion
				/*CreationDeletionHelper.getTheCreationHelper().removeCreationObject(pin);
				 UIDMgr.removeObject(pin.getUID());*/
			}
			// Removed for dts0100506577 - to ensure that the object uids is available in the UIDMgr
			// in order to be able to rotate the transient gfx while insertion
			/*CreationDeletionHelper.getTheCreationHelper().removeCreationObject(dynPinList);
			 UIDMgr.removeObject(dynPinList.getUID());*/
		}

		private IPinList getDynamicPinListForSymbol(SymbolProxy symInst, IBlock block, ISymbolDef blockSource)
		{
			double scale = getScale(blockSource != null ? blockSource : symInst.getSymbolDef());
			Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);

			// Create dynamic display pinlist without pins and text
			IPinList dynPinList;
			if (symInst.isBlock() && !SymbolUtils.isValidSymbol(blockSource)) {
				// Instance is a block, but its correspoding symbol is not present in symbol library, so create dynamic grahpics from blcok itself
				dynPinList = replicator.replicate(block, scale);
			}
			else {
				dynPinList = replicator.replicate(blockSource != null ? blockSource : symInst.getSymbolDef(), scale);
			}
			if (!SymbolUtils.isUnitScale(scale)) {
				SymbolUtils.adjustOffGridPinsToAGridPoint(dynPinList, getGrid());
			}
			stripText(dynPinList);
			return dynPinList;
		}

		private void addBlock(SymbolProxy blockInst)
		{
			if (!blockInst.isBlock()) {
				throw new IllegalArgumentException("Can only add a block");
			}

			// Since we're adding a block to this group, we know that the primary is a composite.
			//primaryPinList.setCompositeInstance(true);
			// For instances of the top-level symbol of a composite, cross-referencing is only enabled if the
			// user requested it explicitly by putting XREF placeholders on the composite symbol
//			primaryPinList.setCrossReferencingEnabled(!primaryPinList.getObjects(IXRefText.class).isEmpty());
			IBlock block = blockInst.getBlock();
			createSchemFromSymbol(blockInst.getSymbolDef(), blockInst.getInstanceNumber(), block, null,
					primaryPinList, block);
			//blockPinList.getTransform().setTransform(block.getGfx().getTransform());
			//ILocation blockLoc = block.getLocation();
			//ILocation plLoc = blockPinList.getLocation();
			//plLoc.setX(blockLoc.getX());
			//plLoc.setY(blockLoc.getY());
			//m_pinListToSymbolMap.put(blockPinList, blockInst);
			//pinLists.add(blockPinList);
		}

		private List<IPinList> getPinLists()
		{
			return pinLists;
		}
	}

	// If the user has selected a composite symbol and all of its blocks, group them all together.  Otherwise
	// evertything is in a group of one.

	private List<PinListGroup> createAndGroupSymbolPinLists()
	{
		int cap = CollectionUtils.calculateHashMapCapacity(m_symbolInstances.length);
		Map<SymbolProxy, PinListGroup> groupMap =
				new LinkedHashMap<SymbolProxy, PinListGroup>(cap);
		m_pinListToSymbolMap = new HashMap<IPinList, SymbolProxy>(cap);

		// First pass - create a group for every non-block.
		for (SymbolProxy symInst : m_symbolInstances) {
			if (!symInst.isBlock()) {
				groupMap.put(symInst, new PinListGroup(symInst));
			}
		}
		// Second pass - find all blocks. If the block belongs to a composite that is being placed
		// add the block to the composite's group, otherwise give the block it's own group.
		for (SymbolProxy symInst : m_symbolInstances) {
			if (symInst.isBlock()) {
				PinListGroup plg = groupMap.get(symInst.getContainingSymbol());
				if (plg != null) {
					plg.addBlock(symInst);
				}
				else {
					groupMap.put(symInst, new PinListGroup(symInst));
				}
			}
		}
		return new ArrayList<PinListGroup>(groupMap.values());
	}

	/**
	 * @return width the minimum width for hte object
	 */
	protected int getMinimumWidth()
	{
		return CHSConstants.PIN_SPACING * 2;
	}

	protected ISharedPinList getSharedPinList()
	{
		return m_addPinListDialog.getSharedPinList();
	}

	public void keyPressed(KeyEvent e)
	{
		// canChangeRotation() is false when sizing second box of inline connector.
		// m_current_point is null when placing pins of shared connector.
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.keyPressed(e);
		}
		else if (e.getKeyCode() == KeyEvent.VK_R) {
			rotateKeyPressed();
		}
		else if (e.getKeyCode() == KeyEvent.VK_F) {
			flipKeyPressed();
		}
	}

	protected void rotateKeyPressed()
	{
		if (rotationIndicator != null
				&& canChangeRotation()
				&& m_current_point != null) {
			pinsVertical = !pinsVertical;
			rotationIndicator.setVertical(pinsVertical);

			// Make a dummy change SmartPoint change to trigger change notification and refresh the dynamic lines.
			m_current_point.applyAbsoluteDelta(new Point(0, 0), true, null);
			rotatePinListGraphics();
			// Invalidate the dynamics so they're redrawn.
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	protected void flipKeyPressed()
	{
		if (canflip()
				&& m_current_point != null) {
			// Make a dummy change SmartPoint change to trigger change notification and refresh the dynamic lines.
			m_current_point.applyAbsoluteDelta(new Point(0, 0), true, null);
			flipPinListGraphics();
			// Invalidate the dynamics so they're redrawn.
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	protected void rotatePinListGraphics()
	{
		if (m_symbolDyn != null) {
			m_symbolDyn
					.rotate(DEG_90, m_current_point.getAbsoluteLocation().x, m_current_point.getAbsoluteLocation().y, 0,
							0);
			transformList.add(m_symbolDyn, TransformType.Rotate);
		}
	}

	protected void flipPinListGraphics()
	{
		if (m_symbolDyn != null) {
			switch (m_flipState) {
				case (0):
					m_symbolDyn.flip(FlipAxisEnum.XAxis, m_current_point.getAbsoluteLocation().x,
							m_current_point.getAbsoluteLocation().y, 0, 0);
					break;
				case (1):
					m_symbolDyn.flip(FlipAxisEnum.XAxis, m_current_point.getAbsoluteLocation().x,
							m_current_point.getAbsoluteLocation().y, 0, 0);
					m_symbolDyn.flip(FlipAxisEnum.YAxis, m_current_point.getAbsoluteLocation().x,
							m_current_point.getAbsoluteLocation().y, 0, 0);
					break;
				case (2):
					m_symbolDyn.flip(FlipAxisEnum.YAxis, m_current_point.getAbsoluteLocation().x,
							m_current_point.getAbsoluteLocation().y, 0, 0);
					break;
			}
			transformList.add(m_symbolDyn, TransformType.Flip);
			m_flipState = (m_flipState + 1) % 3;
		}
	}

	protected void updateInternalConnectivity(@Nullable chs.cof.logical.cable.IPinList pinList)
	{
		if (!(pinList instanceof IDevice)) {
			return;
		}
		if (m_bNewDevice) {
			populateInternalConnectivity(pinList);
			return;
		}
		IDevice dev = (IDevice) pinList;
		Map<IUID, IAbstractPin> symRefExtPinMap = new HashMap<IUID, IAbstractPin>(dev.getNumPins());
		for (IAbstractPin pin : dev.getPinCollection()) {
			ISharedPin shpin = pin.getSharedPin();
			if (shpin != null) {
				IPin symPin = m_sharedPinList.getSymbolPin(shpin);
				if (symPin != null) {
					symRefExtPinMap.put(symPin.getConnectivityUID(), pin);
				}
			}
		}
		Set<IInternalPin> toDelIntPinsSet = new HashSet<IInternalPin>();
		for (IInternalPin ipin : dev.getInternalPins()) {
			IUID pinRef = ipin.getReference();
			if (!ipin.isFromSymbol()) {
				if (symRefExtPinMap.containsKey(pinRef)) {
					toDelIntPinsSet.add(ipin);
				}
			}
		}
		if (!toDelIntPinsSet.isEmpty()) {
			for (IInternalLink link : dev.getInternalLinkCollection()) {
				IGenericPin startPin = link.getStartPin();
				IGenericPin endPin = link.getEndPin();
				assert (startPin != null && endPin != null);
				if (toDelIntPinsSet.contains(startPin)) {
					link.setStartPin(symRefExtPinMap.get(startPin.getReference()));
				}
				if (toDelIntPinsSet.contains(endPin)) {
					link.setEndPin(symRefExtPinMap.get(endPin.getReference()));
				}
			}
		}

		for (IInternalPin pin : toDelIntPinsSet) {
			pin.delete();
		}
	}

	protected void populateInternalConnectivity(chs.cof.logical.cable.IPinList pinList)
	{
		IDevice dev = (IDevice) pinList;
		if (dev.getNumInternalPins() != 0 || !dev.getInternalLinkCollection().isEmpty()) {
			return;
		}
		Replicator m_replicator = new Replicator(Replicator.INSTANTIATE, true);
		List<IUID> devExtPinsSymRefsList = new ArrayList<IUID>();
		for (IAbstractPin pin : dev.getPinCollection()) {
			ISharedPin shpin = pin.getSharedPin();
			if (shpin != null) {
				IPin symPin = m_sharedPinList.getSymbolPin(shpin);
				if (symPin != null) {
					devExtPinsSymRefsList.add(symPin.getConnectivityUID());
					m_replicator.setNewObject(symPin.getConnectivityUID(), pin);
				}
			}
		}

		ISymbolDefIterator symDefItr = m_sharedPinList.getSymbols();

		while (symDefItr.hasNext()) {
			ISymbolDef sym = symDefItr.getNext();

			//Iterate all pins/ipins/ilinks of symbol
			//Add all ipins/ilinks of toplevel/block
			Set<IDevice> connectivities = new HashSet<IDevice>();
			//Set<IPinList> schemPinLists = new HashSet<IPinList>();
			connectivities.add(((IDevice) sym.getConnectivity()));
			for (IBlock block : sym.getBlocks()) {
				if (block.getConnectivity() instanceof IDevice) {
					connectivities.add((IDevice) block.getConnectivity());
				}
			}

			for (IDevice src : connectivities) {
				//For each externalPin of symbol/block, check if it already exists in device.
				//If not, create a corresponding internalPin
				for (IAbstractPin pin : src.getPinCollection()) {
					if (!devExtPinsSymRefsList.contains(pin.getUID())) {
						IInternalPin unplacedExtPin = m_replicator.replicateCablePinsAsInternal(pin, dev);
						unplacedExtPin.setName(pin.getName());
					}
				}
				//First populate all the internalPins..Later go for internalLinks..there may be some internalPins of blocks being referred by top-level interrnal Links..
				for (IInternalPinIterator pinIter = src.getInternalPins(); pinIter.hasNext(); ) {
					IInternalPin internalPinOnSymbol = pinIter.next();
					m_replicator.replicatePin(src, dev, internalPinOnSymbol);
				}
			}

			for (IDevice src : connectivities) {
				for (IInternalLink internalLinkOnSymbol : src.getInternalLinkCollection()) {
					m_replicator.replicateInternalLink(src, dev, internalLinkOnSymbol, false);
				}
			}

			addBlockInfo(dev, sym);
		}
	}

	private void addBlockInfo(IDevice cablePl, ISymbolDef symDef)
	{
		if (cablePl != null && symDef != null) {
			//adding 'compositeblocks' makes sense only if there are any blocks in the symbol..
			Map<IUID, IUID> m_SymBlkConn_LogicBlk_uidMaps = new HashMap<IUID, IUID>();
			for (chs.cof.logical.cable.IBlock cmpBlk : cablePl.getBlocks()) {
				//get the symbol block corresponding to this UID
				IUIDObject uobj = UIDMgr.getObject(cmpBlk.getBlockRefID());
				if (uobj != null && uobj instanceof ISymbolDef) {
					m_SymBlkConn_LogicBlk_uidMaps.put(((ISymbolDef) uobj).getConnectivity().getUID(), cmpBlk.getUID());
				}
			}

			if (m_SymBlkConn_LogicBlk_uidMaps.get(symDef.getConnectivity().getUID()) == null) {
				populateLogicBlocks(symDef, cablePl, m_SymBlkConn_LogicBlk_uidMaps);
			}
			IBlockIterator blkItr = symDef.getBlocks();

			while (blkItr
					.hasNext())     //for each block of the composite, create a 'compositeblock' in the cable device object
			{
				IBlock blk = blkItr.getNext();
				IUID symBlockId = blk.getConnectivity().getUID();
				if (m_SymBlkConn_LogicBlk_uidMaps.get(symBlockId) != null) {
					continue;
				}
				populateLogicBlocks(blk, cablePl, m_SymBlkConn_LogicBlk_uidMaps);
			}
			//	}
			for (IGenericPin blockPin : cablePl
					.getGenericPins())    //Now update the cable pins to refer to the just created 'compositeblocks'
			{
				IUID symPinRef = blockPin.getReference();
				if (symPinRef == null && blockPin.getSharedPin() != null) {
					IPin symbolPin = m_sharedPinList.getSymbolPin(symDef, 0, blockPin.getSharedPin());

					if (symbolPin !=
							null) {    //The pin doesnot belong to the symbol selected in the symbol selection tab
						symPinRef = symbolPin.getConnectivity().getUID();
					}
				}
				if (symPinRef != null)                    //Probably this pin is not from symbol
				{
					IUIDObject uobj = UIDMgr.getObject(symPinRef);
					if (uobj != null && uobj instanceof IGenericPin) {
						IGenericPin symPin = (IGenericPin) uobj;
						if (symPin.getOwner() !=
								null) //First condition: Top level pins in composite will not have any reference
						{                                                             //symPin.getOwner() refers to block's connectivity within the composite symbol
							IUID logicBlockref = m_SymBlkConn_LogicBlk_uidMaps.get(symPin.getOwner().getUID());
							if (logicBlockref != null) {
								blockPin.setBlockRef(logicBlockref);
							}
						}
					}
				}
			}
			for (IInternalLink blockLink : cablePl.getInternalLinkCollection()) {
				IUID symLinkRef = blockLink.getReference();
				if (symLinkRef != null) {
					IUIDObject uobj = UIDMgr.getObject(symLinkRef);
					if (uobj != null && uobj instanceof IInternalLink) {
						IInternalLink symLink = (IInternalLink) uobj;
						if (symLink.getOwner() !=
								null) //First condition: Top level links in composite will not have any reference
						{                                                             //symLink.getOwner() refers to block's connectivity within the composite symbol
							IUID logicBlockref = m_SymBlkConn_LogicBlk_uidMaps.get(symLink.getOwner().getUID());
							if (logicBlockref != null) {
								blockLink.setBlockRef(logicBlockref);
							}
						}
					}
				}
			}
		}
	}

	private void populateLogicBlocks(ISymbolDef symDef, IDevice iDevice, Map<IUID, IUID> SymBlk_LogicBlk_Maps)
	{

		if (symDef.getSymbolType() != SymbolTypeEnum.DEVICE) {
			return;
		}

		chs.cof.logical.cable.IBlock cmpBlock =
				FactoryMgr.getCableFactory().createBlock(FactoryMgr.createUID());
		cmpBlock.setBlockName(symDef.getName());
		cmpBlock.setAnalysisModel(symDef.getAnalysisModel());
		cmpBlock.setBlockRefID(symDef.getUID());

		if (!(symDef instanceof IBlock)) {
			cmpBlock.setIsSymbol(true);
		}

		iDevice.addBlock(cmpBlock);
		SymBlk_LogicBlk_Maps.put(symDef.getConnectivity().getUID(), cmpBlock.getUID());
	}

	protected boolean loadSharedDesignUsageMgrs(IDesign design)
	{

		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();

		ISharedPinList spl = getOperand();
		if (spl != null) {
			String title = CAFUtils.getInstance().getDialogTitleByAction(this, true);
			String description = title + " - " + spl.getName();

			IProject project = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
			if (((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getSharedPinLists(getType()).getSize() > 0) {
				Set<ILogicDesign> designScope = SharedPinHelper.getLogicDesignsUsingSharedPinList(design, spl);

				String longDes = ResourceMgr.getString(AddSharedDeviceAction.class,
						"AddSharedPinListAction.LoadUsage.progress.longDescription");
				return DesignSharedUsageHelper
						.loadeUsagesWithProgressBar(project, owner, designScope, title, description, longDes);
			}
		}
		return true;
	}
}