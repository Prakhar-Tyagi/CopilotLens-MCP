/*
 * Copyright 2003-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.caplet.IBrowserClient;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.actions.AddPinActionHelper;
import chs.caplets.logic.actions.CreateConnectorAction;
import chs.caplets.logic.shared.AddSharedPinListDialog;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedGenerationEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DesignSharedUsageHelper;
import chs.utility.helpers.ModularSchemPinListInfo;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.ModularConnectorHelper;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class exists for typing only.
 */
public class AddSharedJackConnectorAction extends CreateConnectorAction
{

	private static Cursor m_jackConnectorCursor = null;
	private IPinList m_connector;
	private ISharedPinList m_sharedPinList = null;
	private Set<IUIDObject> m_preemies;

//	protected static final int INITIAL = 0;   // Side defined by first mouse click
//	protected static final int FINAL = 1; // Side defined by second mouse click

	protected AddPinActionHelper m_addPinActionHelper; // Delegate action after shared device has been created
	protected ISpecialSelectMgr m_sharedSelectMgr;
	private String m_ctxCommand = "AddSharedConnector";
	protected ISharedPinList m_sharedConnector;
	private ILogicDesign m_design;

	public AddSharedJackConnectorAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller);
		m_addPinActionHelper = new AddPinActionHelper(this, false, true);
		m_sharedSelectMgr = sharedSelectMgr;
		if (getActionUI() != null) {
			m_sharedSelectMgr.contextMenuAddAction(
					new ActionEntry(getActionUI(), (String) getActionUI().getValue(Action.SHORT_DESCRIPTION),
							m_ctxCommand)
					{
						public boolean shouldDisplay()
						{
							return getOperand() != null && isEnabled() && super.shouldDisplay();
						}
					});
		}
		m_design = ((ILogicModel) getModel()).getDesign();
		if (m_jackConnectorCursor == null) {
			m_jackConnectorCursor = CAFUtils.getInstance()
					.loadCursor(controller.getCaplet(), "chs/images/app/cur_connector_jack.gif", new Point(8, 8));
		}
		setSubType(JACK_CONNECTOR);
	}

	protected boolean shouldAddPins()
	{
		// Don't auto add pins if the ctrl button is held.  We manage that here.. not the generic CreateConnectorAction.
		return false;
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
		m_preemies = Collections.emptySet();
		IActionEnum result = IActionEnum.eCanceled;
		try {
			if (!loadDesignSharedUsageMgrs()) {
				return IActionEnum.eCanceled;
			}

			ILogicDesign logicDesign = getLocalModel().getDesign();
			// Find our connectivity device
			Object source = e.getSource();
			if (source instanceof IBrowserClient || isContextButton(source)) {
				m_sharedConnector = getOperand();
				if (m_sharedConnector != null) {
					// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
					final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
					if (!new SharedObjectAvailabilityChecker().check(m_sharedConnector, logicDesign, reporter, true, true)) {
						return IActionEnum.eCanceled;
					}
				}
			}
			else {
				m_sharedConnector = null;
			}
			Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			m_addPinListDialog = getAddSharedPinListDialog(owner);
			boolean success = selectPinList();

			if (success) {
				m_sharedPinList = getSharedPinList();

				// Check that another revision of this shared object does not already exist in this design
				boolean failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(logicDesign, m_sharedPinList);
				if (failure) {
					return IActionEnum.eCanceled;
				}

				if (ShareConcurrencyHelper
						.trySharedObjectPlacement(logicDesign, Collections.singleton(m_sharedPinList))
						.isEmpty()) {
					return IActionEnum.eCanceled;
				}

				//melmorsy - FEAT12331
				//If the shared object is an automatically generated plug connector, then don't allow instantiation!
				if (m_sharedPinList instanceof ISharedConnector &&
						m_sharedPinList.getGenerationType() == SharedGenerationEnum.TypeAutoGenerated) {
					//Show a message box!
					MessageHelper.showInformationMessage(
							getController().getCaplet().getFIB().getWindowMgr().getDialogFrame(),
							ResourceMgr.getString(AddSharedJackConnectorAction.class,
									"AddSharedJackConnectorAction.AutoGeneratedConnector.Header"),
							ResourceMgr.getString(AddSharedJackConnectorAction.class,
									"AddSharedJackConnectorAction.AutoGeneratedConnector.Message",
									m_sharedPinList.getName()));
					//This action is not supported by the tool
					return IActionEnum.eCanceled;
				}

				m_preemies = Collections.emptySet();
				ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
				m_grid = diagram.getGrid();
				if (m_addPinListDialog.getAutoGenerate()) {
					setState(STATE_GENERATE);

					int numPins = m_addPinListDialog.getNumUsedPins();
					int pinspacing = m_grid.getGridSpacing();
					int width = AUTO_GEN_CONN_WIDTH * pinspacing;
					int height = (numPins > 0 ? numPins + 1 : 2) * pinspacing;

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
			else {
				result = IActionEnum.eCanceled;
			}
		}
		finally {
			if (!IActionEnum.eActivated.equals(result) && m_sharedPinList != null && m_sharedPinList.isLocked()) {
				m_sharedPinList.unlock();
				m_sharedPinList = null;
			}
		}
		return result;
	}

	@NotNull protected AddSharedPinListDialog getAddSharedPinListDialog(@Nullable Frame owner)
	{
		return new AddSharedPinListDialog(owner, CAFUtils.getInstance().getDialogTitleByAction(this, true), getType())
		{
			@NotNull @Override public String getHelpID()
			{
				return AddSharedPinListDialog.class.getName();
			}
		};
	}

	protected ISharedPinList getSharedPinList()
	{
		return m_addPinListDialog.getSharedPinList();
	}

	protected IDynamicGfx constructDynGfx(Point ref_point)
	{
		IDynamicGfx gfx = super.constructDynGfx(ref_point);
		getRotationIndicator().setIsJackStyle(true);
		return gfx;
	}

	protected final void setupHomeCondition(@Nullable IPinList connector)
	{
		IProject project = getLocalModel().getDesign().getProject();
		if (connector != null && project != null) {
			// must set before usage construction
			List<IUID> sharedPinlistUsages =
					LogicUtils.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(project, Collections.singleton(m_sharedPinList), null);
			connector.setHome(sharedPinlistUsages.isEmpty());
		}
	}

	protected IGfxObject createDisplayObject(List<ISmartPoint> point_list)
	{
		IGfxObject connector = super.createDisplayObject(point_list);
		IPinList schemConnector = CommonUtils.cast(connector, IPinList.class);
		if (schemConnector != null) {
			setupHomeCondition(schemConnector);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), schemConnector);
		}
		return connector;
	}

	public boolean onTerminate(boolean successful)
	{
		boolean pinCreationCancelled = false;
		boolean actionSuccess = successful;
		try {
			setFeedbackText(null);
			updateFeedback(CAFUtils.getInstance().getActiveCapletView());

			// Put back the stuff that we created "prematurely"
			// DR 394270 - need to do this even when unsuccessful or we get validation failure with an
			// 'object not ready for snapshot' error on a later successful controller action.
			if (m_preemies != null) {
				CreationDeletionHelper.getTheCreationHelper().addCreationObjects(m_preemies);
				// If add pin action is cancelled after mouseReleased(), is called m_preemies will not be empty
				if (!successful && !m_preemies.isEmpty()) {
					pinCreationCancelled = true;
				}
			}

			actionSuccess = super.onTerminate(successful);
		}
		finally {
			actionSuccess &= cleanUp(successful, pinCreationCancelled, actionSuccess);
		}
		return actionSuccess;
	}

	@Override protected boolean createParamObject(boolean actionSuccess)
	{
		if (getState() == STATE_GENERATE) {
			int pinspacing = m_grid.getGridSpacing();
			ILocation loc = m_sharedAutoGenDynamic.getLocation();
			Point p1 = new Point(loc.getX(), loc.getY() + pinspacing);
			Point p2 = new Point(loc.getX() + AUTO_GEN_CONN_WIDTH * pinspacing, loc.getY() + pinspacing);
			m_connector = (IPinList) createDisplayObject(p1, p2);
		}

		if (m_connector == null) {
			return false;
		}

		//make sure the modular hierarchy in connectivity is created before adding pins.
		chs.cof.logical.cable.IPinList cablePinList = m_connector.getConnectivity();
		if (cablePinList instanceof IConnector) {
			ModularConnectorHelper.createRequiredModularChildConnectors((IConnector) cablePinList,
					(ISharedConnector) getSharedPinList(), m_addPinListDialog.getUsedPins());
		}

		CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(getDiagram());
		if (getState() == STATE_GENERATE) {
			//add the schem pin list to the diagram. This must be done before addPins is called
			//below. however, it should have happened in above createDisplayObject method itself.
			addAutoGenPins(m_connector, null, connectivityFinder);
		}
		else {
			m_addPinActionHelper.setIsReference(m_addPinListDialog.getReference());
			if (this instanceof AddSharedRingTerminalAction) {
				setState(STATE_GENERATE);
				addAutoGenPins(m_connector, null, connectivityFinder);
			}
			else {
				m_addPinActionHelper.addPins(m_connector, getDiagram(), connectivityFinder);
			}
		}

		connectivityFinder.connect();

		for (IPinList candidate : new ModularSchemPinListInfo(m_connector).getCandidates()) {
			m_addPinActionHelper.regenerateGraphics(candidate);

			connectGfxObjectToModel(candidate);

			candidate.regenerateDiagramObject();

			setupHomeCondition(candidate);
			ISchemDiagram schemDiag = getLocalModel().getDiagram();
			PreferenceSetHelper.applyStyleSet(candidate.getObjectsForStyling(), schemDiag, true);
		}

		m_sharedPinList.flush();
		return true;
	}

	private boolean cleanUp(boolean successful, boolean pinCreationCancelled, boolean actionSuccess)
	{
		boolean ok = actionSuccess;
		// Idle the undo container here so we don't accidentially add the removed objects to the undo
		// stack.  These are dummy objects and if you add them to the undo stack they will be added to
		// the main sheet upon undo late.
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		if (pinCreationCancelled) {
			for (IUIDObject obj : m_addPinActionHelper.getCreatedPinObjects()) {
				UIDMgr.removeObject(obj.getUID());
			}
		}
		if (m_addPinListDialog != null) {
			if (!m_addPinListDialog.getUsedPins().isEmpty() && successful) {
				boolean success = false;
				try {
					m_addPinActionHelper.cleanUp(true);
					m_addPinListDialog.cleanUp();
					success = true;
				}
				finally {
					ok = ok && success;
				}
			}
			else {
				m_addPinActionHelper.cleanUp(false);
				m_addPinListDialog.cleanUp();
				getLocalModel().getDynamicGfxService().removeAllDynamicGfx();
				getLocalModel().getDynamicGfxService().removeAllTransientGfx();
			}
		}
		setState(STATE_LOITER);
		if (m_sharedPinList != null) {
			m_sharedPinList.unlock();

			// dts0100874319 - this instance variable needs to be nullified!
			m_sharedPinList = null;
		}
		CAFUtils.getInstance().clearTempUndoableContainer();
		if (pinCreationCancelled) {
			if (m_preemies != null) {
				chs.cof.logical.cable.IPinList cablePinList = getCablePinList();
				m_preemies.remove(cablePinList);
				List<IUIDObject> toDelObjList = LogicUtils.getObjectsToBeDeleted(m_preemies);
				for (IUIDObject object : toDelObjList) {
					CreationDeletionHelper.getTheCreationHelper().addDeletionObject(object);
				}
				if (cablePinList != null) {
					CreationDeletionHelper.getTheCreationHelper().addDeletionObject(cablePinList);
				}
			}
			getController().getUndoableContainer().startEdit("Add Shared Jack Connector -Cancel");
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			getController().getUndoableContainer().endEdit();
		}
		return ok;
	}

	@Nullable private chs.cof.logical.cable.IPinList getCablePinList()
	{
		chs.cof.logical.cable.IPinList cablepinList = null;
		for (IUIDObject obj : m_preemies) {
			if (obj instanceof chs.cof.logical.cable.IPinList) {
				cablepinList = (chs.cof.logical.cable.IPinList) obj;
			}
		}
		return cablepinList;
	}

	protected IPinList getCreatedPinList()
	{
		return m_connector;
	}

	private boolean selectPinList()
	{
		ILogicDesign design = getLocalModel().getDesign();
		IPlacementOptionParams params = m_sharedConnector != null ?
				createPlacementOptionParams(m_sharedConnector) : createPlacementOptionParams(getType(), true);
		if (m_sharedConnector != null) {
			ISharedPinReservationView pinView  =
					FactoryMgr.getCommonFactory().constructSharedPinReservationView(m_sharedConnector);
			return m_addPinListDialog.selectPinList(design, m_sharedConnector, pinView , params);
		}
		return m_addPinListDialog.selectPinList(design, getType(), params);
	}

	protected PinListTypeEnum getType()
	{
		return PinListTypeEnum.TypeJack;
	}

	/**
	 * Gets the ActionUIClass attribute of the CreateCircleAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return AddSharedJackConnectorActionUI.class.getName();
	}

	protected IConnector getLogicConnector()
	{
		ISharedPinList sharedConnector = getSharedPinList();
		return createLogicConnector(sharedConnector);
	}

	private IConnector createLogicConnector(ISharedPinList sharedConnector)
	{
		IConnector connector = (IConnector) m_design.getConnectivity().findSharedPinList(sharedConnector);
		if (connector == null) {
			connector = super.getLogicConnector();
			if (connector != null) {
				connector.setSharedPinList(sharedConnector);
			}
		}
		return connector;
	}

	protected void generateConnGraphics(IPinList schem_conn, int pinspacing, int generationMode, SizeHelper sizeH)
	{
		// Note: does not currently pass generationMode down. Probably should, though that level of control is
		// only really needed when library parts are bound on creation, i.e. for interconnect connectors.
		AddSharedHelper.generateConnectorGraphics(schem_conn, getType(), sizeH, getLocalModel());
	}

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
		return m_jackConnectorCursor;
	}

	public void mousePressed(MouseEvent e)
	{
		switch (getState()) {
			case STATE_PARAM:
				super.mousePressed(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mousePressed(e);
				break;
			case STATE_GENERATE:
				super.mousePressed(e);
				break;
			default:
				break;
		}
		updateStatusbarText();
	}

	public void mouseEntered(MouseEvent e)
	{
		switch (getState()) {
			case STATE_PARAM:
				super.mouseEntered(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseEntered(e);
				break;
			case STATE_GENERATE:
				super.mouseEntered(e);
				break;
			default:
				break;
		}
	}

	public void mouseExited(MouseEvent e)
	{
		switch (getState()) {
			case STATE_PARAM:
				super.mouseExited(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseExited(e);
				break;
			case STATE_GENERATE:
				super.mouseEntered(e);
				break;
			default:
				break;
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (getState() == STATE_PARAM) {
			if (m_goingToTerminate) {
				setFeedbackText(null);
				updateFeedback(e.getSource());
				cleanupTrans();
				final List<ISmartPoint> connectorPointList = getPointList();

				if (connectorPointList != null) {
					m_connector = (IPinList) createDisplayObject(connectorPointList);
				}
				// We created stuff before onTerminate() was called - cache what's in the
				// CreationDeletionHelper and clear it out.
				CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
				m_preemies = new HashSet<IUIDObject>(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
				cdh.clearNewObjects();

				Collection<IPinProxy> pinsToAdd = m_addPinListDialog.getUsedPins();

				if (m_connector != null && !pinsToAdd.isEmpty()) {
					//dts0100526682: the isReference flag must be passed to the AddPinActionHelper to not reserve the non-reusable pins to a design.
					boolean isReference = m_addPinListDialog.getReference();
					m_addPinActionHelper.setIsReference(isReference);
					m_addPinActionHelper.setPlaceAsStack(m_addPinListDialog.getPlaceAsStack());
					m_addPinActionHelper.setPlaceAsGroup(m_addPinListDialog.getPlaceAsGroup());
					// Set delegate AddPinAction so that pins can be added interactively.
					m_addPinActionHelper.setUp(m_connector, pinsToAdd);
					if (this instanceof AddSharedRingTerminalAction) {
						getController().getActionMgr().terminateActiveAction(m_connector != null);
					}
					else {
						setState(STATE_PINS);
					}
				}
				else {
					getController().getActionMgr().terminateActiveAction(m_connector != null);
				}
			}
		}
		else if (getState() == STATE_GENERATE) {
			getController().getActionMgr().terminateActiveAction(m_sharedAutoGenDynamic != null);
		}
		else if (getState() == STATE_PINS) {
			m_addPinActionHelper.mouseReleased(e);
		}
		updateStatusbarText();
	}

	public void mouseClicked(MouseEvent e)
	{
		switch (getState()) {
			case STATE_PARAM:
				super.mouseClicked(e);
				break;
			case STATE_PINS:
				// Add the next pin specified by the dialog.
				updateStatusbarText();
				m_addPinActionHelper.mouseClicked(e);
				break;
			case STATE_GENERATE:
				super.mouseClicked(e);
				break;
			default:
				return; // does nothing
		}
		updateStatusbarText();
	}

	public void mouseDragged(MouseEvent e)
	{
		switch (getState()) {
			case STATE_PARAM:
				super.mouseDragged(e);
				break;
			case STATE_PINS:
				m_addPinActionHelper.mouseDragged(e);
				break;
			case STATE_GENERATE:
				super.mouseDragged(e);
				break;
			default:
				break;
		}
	}

	public void keyPressed(KeyEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.keyPressed(e);
		}
		else {
			super.keyPressed(e);
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		switch (getState()) {
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
			default:
				break;
		}
	}

	private boolean isContextButton(Object src)
	{
		return src instanceof AbstractButton
				&& ((AbstractButton) src).getActionCommand()
				.equals(m_ctxCommand); // Test for same object, not string equal
	}

	public boolean isEnabled()
	{
		ISharedPinList spl = getOperand();
		//
		// If we are in a transaction boundary, we MUST wait
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			return false;
		}
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
		if (m_sharedSelectMgr.getSelectedObjects().getSize() == 1) {
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

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_PARAM:
				return ResourceMgr.getString(AddSharedJackConnectorAction.class,
						"AddSharedJackConnectorAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
	}

	protected boolean loadDesignSharedUsageMgrs()
	{
		IProject project = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
		ISharedPinList spl = getOperand();
		if (spl != null) {
			if (((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getSharedPinLists(getType()).getSize() > 0) {
				Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
				String dialogTitle = CAFUtils.getInstance().getDialogTitleByAction(this, true);
				IDesign design = getLocalModel().getDesign();
				String description = dialogTitle + " - " + spl.getName();
				Set<ILogicDesign> designScope = SharedPinHelper.getLogicDesignsUsingSharedPinList(design, spl);
				return DesignSharedUsageHelper
						.loadeUsagesWithProgressBar(project, owner, designScope, dialogTitle, description,
								getLoadUsagesProgressBarLongDesc());
			}
		}
		return true;
	}

	protected String getLoadUsagesProgressBarLongDesc()
	{
		return ResourceMgr.getString(AddSharedJackConnectorAction.class,
				"AddSharedPinListAction.LoadUsage.progress.longDescription");
	}
}