/*
 * Copyright 2004-2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.actions.LogicActionMessageHelper;
import chs.caplets.logic.shared.AddSharedPinListDialog;
import chs.cof.draw.ICircle;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.CommonInSharedPinDBReservationAndDesignScope;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinIterator;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.symbol.ISymbolDef;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.cofUtils.parameterized.AddSpliceHelper;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.common.preferencesets.IPreferenceSet;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.ResourceMgr;
import chs.utility.ConductorSplitter;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.preferences.PreferenceSetHelper;
import chs.utility.ui.ISharedPinListSymbolInstance;
import chs.utility.ui.SharedPinListSymbolInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: May 3, 2004 Time: 8:08:46 PM
 */
public class AddSharedSpliceAction extends AddSharedDeviceAction
{

	private static Cursor m_spliceCursor = null;

	//	private IDynamicGfxService m_dynamics;
	private Point m_currValidPoint;
	private ICircle m_feedback;

	private IPinList m_schemSplice;
	private ISplice m_cableSplice;

	private IDynamicGfx m_transientSymbolGraphics;
	private ISymbolDef m_symbolDef;

	public AddSharedSpliceAction(ICapletController controller, ISpecialSelectMgr libSelectMgr)
	{
		super(controller, libSelectMgr);
		m_dynamics = getLocalModel().getDynamicGfxService();
		if (m_spliceCursor == null) {
			m_spliceCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_splice.gif", new Point(7, 7));
		}
	}

	protected Point getcurrValidPoint()
	{
		return m_currValidPoint;
	}

	public String getActionUIClass()
	{
		return AddSharedSpliceActionUI.class.getName();
	}

	private void initializeMembers()
	{
		ISchemDiagram diagram = getLocalModel().getDiagram();
		m_grid = diagram.getGrid();
		addedDevices = new HashSet<chs.cof.logical.cable.IPinList>();
		symbolCablePins = new ArrayList<IGenericPin>();
		m_sharedPinList = getOperand();
	}

	private boolean isPlaceable(ISharedPin sharedPin, ILogicDesign design, ISharedPinReservationView sharedPinView,
			Set<ILogicDesign> designScope)
	{
		return SharedPinHelper.isSharedPinUnplaced(sharedPin, design) ||
				isSharedPinAvailable(sharedPin, design, sharedPinView.getSharedPinDBReservations(sharedPin),
						designScope);
	}

	private boolean isSharedPinAvailable(@NotNull ISharedPin pin, @NotNull ILogicDesign design,
			Set<IUID> dbReservations, @NotNull Set<ILogicDesign> designScope)
	{
		CommonInSharedPinDBReservationAndDesignScope commonInSharedPinDBReservationAndDesignScope =
				SharedPinHelper.createSharedPinDBReservationScope(design, dbReservations, pin);
		return SharedPinHelper.isAvailable(pin, commonInSharedPinDBReservationAndDesignScope,
				designScope);
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		//Initialize member variables
		initializeMembers();
		Object source = e.getSource();
		ILogicDesign design = getLocalModel().getDesign();
		if (!loadSharedDesignUsageMgrs(design)) {
			return IActionEnum.eCanceled;
		}
		if (source instanceof IBrowserClient || isContextButton(source)) {
			// invoked from context menu of shared splice - just add the selected one
			if (m_sharedPinList == null) {
				return IActionEnum.eCanceled;
			}
			RefreshStatusEnum rs = m_sharedPinList.refresh();
			if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
				LogicActionMessageHelper.warnDeleted(m_sharedPinList);
				return IActionEnum.eCanceled;
			}

			if (AddSharedHelper.isSharedObjectPermissionDenied()) {
				return IActionEnum.eCanceled;
			}

			// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
			final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
			if (!new SharedObjectAvailabilityChecker().check(m_sharedPinList, design, reporter, true, true)) {
				return IActionEnum.eCanceled;
			}

			Collection<? extends ISharedObject> placeableObjects =
					ShareConcurrencyHelper.trySharedObjectPlacement(design, Collections.singleton(m_sharedPinList));
			if (!placeableObjects.contains(m_sharedPinList)) {
				return IActionEnum.eCanceled;
			}
			//FEAT15620-PinDuplication:check for pin duplication. though shared splice pins are always re-usable.
			//but this action should not be dependent upon that implementation. chandras
			// This is always true, it is causing performance and this code is redundant
//			ISharedPinReservationView sharedPinView =
//					FactoryMgr.getCommonFactory().constructSharedPinReservationView(m_sharedPinList);
//			designScope = SharedPinHelper.getLogicDesignsUsingSharedPinList(design, m_sharedPinList);
//			for (ISharedPin sPin : m_sharedPinList.getPins()) {
//				if (!isPlaceable(sPin, design, sharedPinView, designScope)) {
//					return IActionEnum.eCanceled;
//				}
//			}

			if (!lockLogicObjects(design, m_sharedPinList)) {
				return IActionEnum.eCanceled;
			}
		}
		else {
			AddSharedPinListDialog addPinListDialog = createAddSharedPinListDialog();
			boolean success =
					addPinListDialog
							.selectPinList(design, createPlacementOptionParams(PinListTypeEnum.TypeSplice, true));
			if (success) {
				m_sharedPinList = addPinListDialog.getSharedPinList();
			}
			else {
				return IActionEnum.eCanceled;
			}
		}

		// Check that another revision of this shared object does not already exist in this design
		boolean failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(design, m_sharedPinList);
		if (failure) {
			return IActionEnum.eCanceled;
		}

		//need to set up the transient graphics according to the splice type; whether
		//parametrized or with symbol
		m_symbolDef = m_sharedPinList.getSymbolDef();
		setupTransientGraphics();
		return IActionEnum.eActivated;
	}

	@NotNull protected AddSharedPinListDialog createAddSharedPinListDialog()
	{
		return new AddSharedSpliceDialog();
	}

	private boolean lockLogicObjects(ILogicDesign design, ISharedPinList sharedPinList)
	{
		IConnectivity connectivity = design.getConnectivity();
		assert connectivity != null;
		chs.cof.logical.cable.IPinList pinList = connectivity.findSharedPinList(sharedPinList);
		return lockLogicObjects(design, pinList);
	}

	private boolean lockLogicObjects(ILogicDesign design, @Nullable chs.cof.logical.cable.IPinList pinList)
	{
		if (pinList != null) {
			return LogicObjectLockFinder.tryEdit(design, pinList);
		}
		return true;
	}

	public boolean onTerminate(boolean successful)
	{
		if (successful) {
			m_cableSplice =
					(ISplice) getLogicDevice();//(ISplice)getLocalModel().getDesign().getConnectivity().findSharedPinList(m_sharedPinList);

			if (!LogicObjectLockFinder.tryEdit(m_cableSplice)) {
				return false;
			}

			ICompoundObject diag = getLocalModel().getSheet();

			//fill in the m_schemSplice
			m_schemSplice = createSchemSplice();

			//IConnectivity conn = getLocalModel().getDesign().getConnectivity();
			//conn.addSplice(m_cableSplice);

			prepareForPlacement();
			diag.addObject(m_schemSplice);

			ILogicDesign design = getDiagram().getDesign();
			assert design != null;

			GfxView gview = (GfxView) CAFUtils.getInstance().getActiveCapletView();
			ConductorSplitter spliceSplitter = ConductorSplitter.createConductorSplitter(m_schemSplice);
			spliceSplitter.splitConductors(m_schemSplice, gview, false, false, false, () -> {
			});

			// gdh 11/20/03 re: 3136 add splice should connect to wire end; let "examineGraphics" works its magic
			ConnectionHelper chelper = new ConnectionHelper();
			chelper.examineGraphics(m_schemSplice, gview.getGfxContext(), gview.getSheet(), false);

			CreationDeletionHelper.getTheCreationHelper().addCreationObject(m_schemSplice);

			////////////////////////////////////////////////
			//IAbstractPin absPin = m_cableSplice.getPins().getNext();
			//
			// The 'super.onTerminate' MAY create a new splice for us. However, it knows zilch about
			// shared hookups, so we have to do the hookup here if it was not done.
			//
			if (m_cableSplice.getSharedPinList() == null) {
				m_cableSplice.setSharedPinList(m_sharedPinList);
			}
			for (IAbstractPinIterator pinIterator = m_cableSplice.getPins(); pinIterator.hasNext(); ) {
				IAbstractPin absPin = pinIterator.getNext();
				if (absPin.getSharedPin() == null) {
					absPin.setSharedPin(m_sharedPinList.findSharedPin(absPin));
				}
			}

			int sharedSymbolInstance = (m_symbolDef == null) ? -1 : 0;

			//
			// Get the symbol pin mapping [if any]
			//
			IUID symbolDefUID = null;
			Map<ISharedPin, IPin> sim = null;
			if (m_symbolDef != null) {
				symbolDefUID = m_symbolDef.getUID();
				sim = m_sharedPinList.getSymbolInstancePinMapping(m_symbolDef, sharedSymbolInstance);
			}

			// set the data that will go onto the usages.
			m_schemSplice.setSymbolInformation(symbolDefUID, null, sharedSymbolInstance);
			List<IUID> sharedPinlistUsages =
					LogicUtils.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(m_sharedPinList.getProject(),
							Collections.singleton(m_sharedPinList), null);
			boolean isHome = sharedPinlistUsages.isEmpty();
			m_schemSplice.setHome(isHome);  // must set before usage construction
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), m_schemSplice);

			for (IAbstractPinIterator pinIterator = m_cableSplice.getPins(); pinIterator.hasNext(); ) {
				IAbstractPin absPin = pinIterator.getNext();
				IPin pin = m_schemSplice.findPin(absPin);
				ISharedPin shpin = absPin.getSharedPin();
				if (pin != null && shpin != null) {
					if (sim != null) {
						pin.setSymbolPin(sim.get(shpin));
					}
					pin.setReference(false);
				}
			}
			if (m_symbolDef != null && design != null) {
				SymbolUtils.setupHomeForPins(m_schemSplice, m_symbolDef, design);
			}
			ISchemDiagram schemDiag = getLocalModel().getDiagram();
			IPreferenceSet styleSet = PreferenceSetHelper.getStyleSet(schemDiag);
			PreferenceSetHelper
					.applyStyleSet(m_schemSplice.getObjectsForStyling(), styleSet, true);
			String projectUid =
					CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject().getUID().getString();
			ISharedPinList sharedDevice = (ISharedPinList) m_schemSplice.getSharedObject();
			IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
			auditLogger.postEvent(AuditableEventType.SHARED_OBJECT_ADDED, null, projectUid,
					sharedDevice.getFullName(), sharedDevice.getUID().getString());
		}
		else {
			for (chs.cof.logical.cable.IPinList pinlist : addedDevices) {
				getLogicModel().getDesign().getConnectivity().removePinList(pinlist);
			}
		}

		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		if (m_addPinListDialog != null) {
			m_addPinListDialog.cleanUp();
			//if (!m_addPinListDialog.getSelectedRows().isEmpty()) {
			//	m_addPinActionHelper.cleanUp(successful && ok);
			//}
		}
		CAFUtils.getInstance().clearTempUndoableContainer();

		if (m_sharedPinList != null) {
			m_sharedPinList.unlock();
		}

		// Remove transient graphics
		cleanupTransientGraphics();

		//Clear member variables at the end of all processing
		clearMembers();
		return successful;
	}

	private void clearMembers()
	{
		if (addedDevices != null) {
			addedDevices.clear();
		}
		if (symbolCablePins != null) {
			symbolCablePins.clear();
		}
		m_grid = null;
		m_cableSplice = null;
		m_schemSplice = null;
		m_symbolDef = null;
		m_transientSymbolGraphics = null;
		m_sharedAutoGenDynamic = null;
		m_sharedPinList = null;
	}

	protected chs.cof.logical.cable.IPinList getLogicDevice()
	{
		ISplice splice = (ISplice) getLogicModel().getDesign().getConnectivity().findSharedPinList(m_sharedPinList);
		if (splice == null) {
			splice = createLogicSplice();
			getLogicModel().getDesign().getConnectivity().addSplice(splice);
			addedDevices.add(splice);
			splice.setSharedPinList(m_sharedPinList);
			if (m_sharedPinList.getSymbolDef() != null) {
				splice.setSymbolRef(FactoryMgr.getSymbolFactory().constructSymbolRef(m_sharedPinList.getSymbolDef()));
			}
		}
		return splice;
	}

	protected ISplice createLogicSplice()
	{
		ISplice spl = FactoryMgr.getCableFactory().createSplice(FactoryMgr.getCommonFactory().createUID());
		//
		for (ISharedPinIterator itr = m_sharedPinList.getPins(); itr.hasNext(); ) {
			ISharedPin spin = itr.getNext();
			IAbstractPin apin = FactoryMgr.getCableFactory().createSplicePin(FactoryMgr.getCommonFactory().createUID());
			spl.addPin(apin);
			apin.setSharedPin(spin);
		}
		return spl;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(AddSharedSpliceAction.class, "AddSharedSpliceAction.Statusbar.text");
	}

	protected ISharedPinList getOperand()
	{
		if (m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
			IUIDObject uidObj = m_sharedSelectMgr.getSelectedObjects().getNext();
			if (uidObj instanceof ISharedPinList) {
				ISharedPinList sharedPinlist = (ISharedPinList) uidObj;
				if (sharedPinlist.getType().equals(PinListTypeEnum.TypeSplice)) {
					return sharedPinlist;
				}
			}
		}
		return null;
	}

	protected IPinList createSchemSplice()
	{
		m_sharedAutoGenDynamic =
				m_dynamics.getFactory().constructCompound(FactoryMgr.getCommonFactory().createLocation());
		IPinList schemSplice;
		if (m_symbolDef != null) {
			ISharedPinListSymbolInstance sharedPinListSymbolInstance =
					new SharedPinListSymbolInstance(m_sharedPinList, m_symbolDef, 0);
			IPinList symPinList =
					createSchemFromSymbol(m_symbolDef, sharedPinListSymbolInstance.getInstanceNumber(), null, null);
			double scale = (double) m_grid.getGridSpacing() / SYMBOL_SPACING;
			Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);

			// Create dynamic display pinlist without pins and text
			IPinList dynPinList = replicator.replicate(sharedPinListSymbolInstance.getSymbolDef(), scale);
			//dts0100777407 VALIDATION FAILURE: Property text not found in propertied object of owner
			stripText(dynPinList);
			m_sharedAutoGenDynamic.addObject(dynPinList);
			m_dynamics.addDynamicGfx(m_sharedAutoGenDynamic);
			schemSplice = symPinList;
			schemSplice.setConnectivity(m_cableSplice);
			addNewSymbolCablePinsToOwner();
			for (IAbstractSchemPin pin : dynPinList.getAllPins()) {
				// FEAT00013786: Spice does not support stack pins. assert it here.
				assert pin instanceof IPin;
				m_sharedAutoGenDynamic.addObject(pin);
				CreationDeletionHelper.getTheCreationHelper().removeCreationObject(pin);
				//UIDMgr.removeObject(pin.getUID());
			}
			CreationDeletionHelper.getTheCreationHelper().removeCreationObject(dynPinList);
			//UIDMgr.removeObject(dynPinList.getUID());
		}
		else {
			schemSplice = createSchematicSplice();
			if (m_cableSplice == null) {
				m_cableSplice = (ISplice) schemSplice.getConnectivity();
			}
		}
		return schemSplice;
	}

	protected IPinList createSchematicSplice()
	{
		IPinList schemSplice =
				AddSpliceHelper.generateSplice(m_cableSplice, m_grid, null, m_currValidPoint.x, m_currValidPoint.y);
		return schemSplice;
	}

	public void prepareForPlacement()
	{
		//If it's a splice symbol, place it at the transient symbol location. Otherwise
		//the splice is placed at the current cursor location.  in mouseMoved()
		if (m_symbolDef != null) {
			m_schemSplice.setLocation(m_transientSymbolGraphics.getLocation());
		}
	}

	protected void setupTransientGraphics()
	{
		//
		// Add the graphics for the symbol.
		//
		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();
		//
		// If you're wondering why it's a circle, ask Greg!
		//
		if (m_symbolDef == null) {
			int gp = m_grid.getGridSpacing() / 2;
			m_feedback = FactoryMgr.getDrawFactory().constructCircle(0, 0, gp);
			m_dynamics.addTransientGfx(m_feedback);
		}
		else {
			Replicator replicator = new Replicator(Replicator.INSTANTIATE, true);
			double scale = (double) m_grid.getGridSpacing() / SYMBOL_SPACING;
			IPinList dynPinList = replicator.replicate(m_symbolDef, scale);
			//dts0100777407 VALIDATION FAILURE: Property text not found in propertied object of owner
			stripText(dynPinList);
			m_transientSymbolGraphics =
					m_dynamics.getFactory().constructCompound(FactoryMgr.getCommonFactory().createLocation());
			m_transientSymbolGraphics.addObject(dynPinList);
			m_dynamics.addTransientGfx(m_transientSymbolGraphics);
			for (IAbstractSchemPin pin : dynPinList.getAllPins()) {
				// FEAT00013786: Spice does not support stack pins. assert it here.
				assert pin instanceof IPin;
				m_transientSymbolGraphics.addObject(pin);
				CreationDeletionHelper.getTheCreationHelper().removeCreationObject(pin);
				//UIDMgr.removeObject(pin.getUID());
			}
			CreationDeletionHelper.getTheCreationHelper().removeCreationObject(dynPinList);
			//UIDMgr.removeObject(dynPinList.getUID());
		}
	}

	protected void cleanupTransientGraphics()
	{
		//
		// Clear the old dynamics.
		//
		//cleanup the objects properly so that all associated table data gets deleted from UIDMgr
		if (m_transientSymbolGraphics != null) {
			for (IGfxObject gfxObject : m_transientSymbolGraphics.getObjects()) {
				if (gfxObject instanceof IUIDObject) {
					IUIDObject uidObject = (IUIDObject) gfxObject;
					// PDV-22362 It's possible that objects can get deleted along with their parent objects, we need to
					// check that the object isn't already dealt with
					if (UIDMgr.isValid(uidObject)) {
						uidObject.delete();
					}
				}
			}
		}

		if (m_sharedAutoGenDynamic != null) {
			for (IGfxObject gfxObject : m_sharedAutoGenDynamic.getObjects()) {
				if (gfxObject instanceof IUIDObject) {
					IUIDObject uidObject = (IUIDObject) gfxObject;
					// PDV-22362 It's possible that objects can get deleted along with their parent objects, we need to
					// check that the object isn't already dealt with
					if (UIDMgr.isValid(uidObject)) {
						uidObject.delete();
					}
				}
			}
		}

		m_dynamics.removeAllDynamicGfx();
		m_dynamics.removeAllTransientGfx();

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eFull);
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

	public void mouseDragged(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		//dts0101362495 m_currValidPoint should not be null after mouse clicked
		if (m_currValidPoint == null) {
			setCurrentValidPoint(e);
		}

		//
		// Commit it, and finish up here
		//
		getController().getActionMgr().terminateActiveAction(true);
	}

	public void mouseMoved(MouseEvent e)
	{
		//super.mouseMoved(e);
		//old parent
		//
		// Keep the location around...
		//
		setCurrentValidPoint(e);
		if (m_feedback != null) {
			m_feedback.getLocation().setX(m_currValidPoint.x);
			m_feedback.getLocation().setY(m_currValidPoint.y);
		}
		////////////////
		if (m_symbolDef != null) {
			Point wp = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
			ILocation loc = m_transientSymbolGraphics.getLocation();
			loc.setLocation(m_grid.snap(wp.x), m_grid.snap(wp.y));
			m_transientSymbolGraphics.setLocation(loc);
		}

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void setCurrentValidPoint(MouseEvent e)
	{
		m_currValidPoint = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		m_currValidPoint.setLocation(m_grid.snap(m_currValidPoint.x), m_grid.snap(m_currValidPoint.y));
	}

	/**
	 * Return the cursor for this action
	 */
	public Cursor getCursor()
	{
		return m_spliceCursor;
	}

	protected ISharedPinList getSharedPinList()
	{
		return m_sharedPinList;
	}

	private class AddSharedSpliceDialog extends AddSharedPinListDialog
	{

		AddSharedSpliceDialog()
		{
			super(getController().getCaplet().getFIB().getWindowMgr().getDialogFrame(),
					CAFUtils.getInstance().getDialogTitleByAction(AddSharedSpliceAction.this, true),
					PinListTypeEnum.TypeSplice);
		}

		@Override protected void createAsReferenceCheckBox()
		{
			// Reference check box is not needed for splice
		}

		@Override protected void createAutoGenerateOption()
		{
			// Auto-Generate check box is not needed for splice
		}

		@Override protected void createAsStackOption()
		{
			//
		}

		@Override protected void createAsGroupOption()
		{
			//
		}

		@Override protected void createIndividualOption()
		{
		}

		@NotNull @Override public String getHelpID()
		{
			return AddSharedSpliceAction.class.getName();
		}
	}
}
