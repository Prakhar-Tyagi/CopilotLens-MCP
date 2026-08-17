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
import chs.caplets.logic.Model;
import chs.caplets.logic.actions.AddPinActionHelper;
import chs.caplets.logic.actions.CreateInlineConnectorAction;
import chs.caplets.logic.shared.AddSharedPinListDialog;
import chs.caplets.logic.shared.ISharedPinListInfoProvider;
import chs.cof.logical.IDesign;
import chs.cof.logical.IECAttributeResolver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedBackshell;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.ISharedPin;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.logical.shared.SharedPinHelper;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.ILocation;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.Location;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.services.dynamicgfx.DynamicRectanglePair;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.ISmartPoint;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CompositePinConnectivityFinder;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.DesignSharedUsageHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.revisioning.SharedObjectRevisionHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.LogicUtils;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import chs.utility.logic.SizeHelper;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddSharedInlineConnectorAction extends CreateInlineConnectorAction
{

	protected AddPinActionHelper m_addPinActionHelper; // Delegate action after shared device has been created
	private Set<IUIDObject> m_preemies;
	private ISharedConnector m_sharedJack = null;
	private ISharedConnector m_sharedPlug = null;
	private ISpecialSelectMgr m_sharedSelectMgr;
	private String m_ctxCommand = "AddSharedInlineConnector";

	public AddSharedInlineConnectorAction(ICapletController controller, ISpecialSelectMgr sharedSelectMgr)
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
	}

	public String getActionUIClass()
	{
		return AddSharedInlineConnectorActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum result = IActionEnum.eCanceled;
		try {
			m_preemies = new HashSet<IUIDObject>();

			Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
			ILogicDesign logicDesign = getLocalModel().getDesign();

			if (!loadDesignSharedUsageMgrs()) {
				return IActionEnum.eCanceled;
			}

			ISharedPinList spl = getOperand();
			Object source = e.getSource();
			final boolean isNonInteractiveObjectSelection = source instanceof IBrowserClient || isContextButton(source);
			if (isNonInteractiveObjectSelection && spl != null) {
				// Can't add an unfrozen shared object to a design that requires all shared objects to be frozen
				final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
				if (!new SharedObjectAvailabilityChecker().check(spl, logicDesign, reporter, true, true)) {
					return IActionEnum.eCanceled;
				}
			}

			PinListTypeEnum pinlistType = spl != null ? spl.getType() : null;
			m_addPinListDialog = getAddSharedPinListDialog(owner, pinlistType);

			m_addPinActionHelper.cleanUp(false);  //dts0100783412

			boolean success = selectPinList(spl, isNonInteractiveObjectSelection, pinlistType);

			if (success) {
				ISharedConnector sharedConnector = (ISharedConnector) m_addPinListDialog.getSharedPinList();

				if (sharedConnector.getType().equals(PinListTypeEnum.TypeInlinePlug)) {
					m_sharedPlug = sharedConnector;
					m_sharedJack = m_sharedPlug.getMate();
				}
				else {
					m_sharedJack = sharedConnector;
					m_sharedPlug = m_sharedJack.getMate();
				}

				// Check that another revision of this shared object does not already exist in this design
				boolean failure = SharedObjectRevisionHelper.checkUsagesOfOtherRevisionForPlaceAction(logicDesign,
						sharedConnector);
				if (failure) {
					return IActionEnum.eCanceled;
				}

				if (ShareConcurrencyHelper
						.trySharedObjectPlacement(logicDesign, Collections.singleton(sharedConnector))
						.isEmpty()) {
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
					Point p3 = new Point(2 * width, height);
					m_sharedAutoGenDynamic =
							new DynamicRectanglePair(p1, p2, p3, FactoryMgr.getDrawFactory(), true, this);
					//need to make advancement so that the pair of rects are added to the compound gfx
					m_sharedAutoGenDynamic.advancePoint();
					m_sharedAutoGenDynamic.advancePoint();
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
			if (!IActionEnum.eActivated.equals(result)) {
				if (m_sharedPlug != null && m_sharedPlug.isLocked()) {
					m_sharedPlug.unlock();
				}
				if (m_sharedJack != null && m_sharedJack.isLocked()) {
					m_sharedJack.unlock();
				}
			}
		}
		return result;
	}

	private boolean selectPinList(@Nullable ISharedPinList spl, boolean isNonInteractiveObjectSelection,
			@Nullable PinListTypeEnum pinListType)
	{
		// We initialize the dialog using the inline plug type - arbitrarily - either half of
		// the selected shared inline will do. The add pin action will place the mate pin on the other half.
		IPlacementOptionParams params = spl != null ? createPlacementOptionParams(spl) :
				createPlacementOptionParams(PinListTypeEnum.TypeInlinePlug, true);
		if (isNonInteractiveObjectSelection && spl != null) {
			ISharedPinReservationView pinview =
					FactoryMgr.getCommonFactory().constructSharedPinReservationView(spl);
			return m_addPinListDialog.selectPinList(getLocalModel().getDesign(), spl, pinview, params);
		}
		PinListTypeEnum plType = pinListType != null ? pinListType : PinListTypeEnum.TypeInlinePlug;
		return m_addPinListDialog.selectPinList(getLocalModel().getDesign(), plType, params);
	}

	@NotNull
	protected ISharedPinListInfoProvider getAddSharedPinListDialog(Frame owner, @Nullable PinListTypeEnum pinlistType)
	{
		return new AddSharedPinListDialog(owner, CAFUtils.getInstance().getDialogTitleByAction(this, true),
				pinlistType);
	}

	private boolean loadDesignSharedUsageMgrs()
	{
		IProject project = CAFUtils.getInstance().getCAFProjectMgr().getCurrentProject();
		ISharedPinList spl = getOperand();
		if (spl != null) {
			if (((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getSharedPinLists(PinListTypeEnum.TypeInlinePlug).getSize() > 0) {

				String dialogTitle = CAFUtils.getInstance().getDialogTitleByAction(this, true);
				String description = dialogTitle + " - " + spl.getName();
				Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
				IDesign design = getLocalModel().getDesign();
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
		return ResourceMgr.getString(AddSharedInlineConnectorAction.class,
				"AddSharedPinListAction.LoadUsage.progress.longDescription");
	}

	public boolean onTerminate(boolean successful)
	{
		boolean pinCreationCancelled = false;
		boolean actionSuccess = successful;
		try {
			setFeedbackText(null);
			updateFeedback(CAFUtils.getInstance().getActiveCapletView());

			if (m_preemies != null) {
				// DR 394270 - do this even if not successful - see AddSharedJackConnector.
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
			autoGeneratePinList();
		}

		if (m_schemPlug != null && m_schemJack != null) {
			Model model = getLocalModel();
			ISchemDiagram diag = model.getDiagram();

			if (getState() != STATE_GENERATE) {
				m_addPinActionHelper.setIsReference(m_addPinListDialog.getReference());
				m_addPinActionHelper.addPins(m_schemPlug, m_schemJack, diag);
			}

			addBackshellAndTermination(m_schemPlug, m_sharedPlug);
			addBackshellAndTermination(m_schemJack, m_sharedJack);

			connectGfxObjectToModel(m_schemPlug);
			connectGfxObjectToModel(m_schemJack);

			m_schemJack.regenerateDiagramObject();
			m_schemPlug.regenerateDiagramObject();

			ILogicDesign design = model.getDesign();
			IProject project = design.getProject();
			assert project != null;
			List<IUID> sharedPlugUsages =
					LogicUtils.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(project,
							Collections.singleton(m_sharedPlug), null);
			boolean isHome = sharedPlugUsages.isEmpty();
			m_schemPlug.setHome(isHome); // must set before usage construction

			List<IUID> sharedJackUsages =
					LogicUtils.getLogicDesignsUsingSharedObjectsWithoutDomainFilter(project,
							Collections.singleton(m_sharedJack), null);
			isHome = sharedJackUsages.isEmpty();
			m_schemJack.setHome(isHome); // must set before usage construction
			PreferenceSetHelper.applyStyleSet(m_schemJack.getObjectsForStyling(), diag, true);
			PreferenceSetHelper.applyStyleSet(m_schemPlug.getObjectsForStyling(), diag, true);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), m_schemPlug);
			IECAttributeResolver.inheritIECAttributesIfNotPresent(getDiagram(), m_schemJack);
		}
		return true;
	}

	private boolean cleanUp(boolean successful, boolean pinCreationCancelled, boolean actionSuccess)
	{
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		if (pinCreationCancelled) {
			for (IUIDObject obj : m_addPinActionHelper.getCreatedPinObjects()) {
				UIDMgr.removeObject(obj.getUID());
			}
		}
		boolean ok = actionSuccess;
		if (m_addPinListDialog != null) {
			if (!m_addPinListDialog.getUsedPins().isEmpty() && successful) {
				boolean cleanUpSuccess = false;
				try {
					m_addPinActionHelper.cleanUp(true);
					m_addPinListDialog.cleanUp();
					cleanUpSuccess = true;
				}
				finally {
					ok &= cleanUpSuccess;
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
		if (m_sharedJack != null) {
			SharedPinListHelper.unlock(m_sharedJack); // Will unlock plug too.
		}
		CAFUtils.getInstance().clearTempUndoableContainer();
		m_sharedJack = null;
		m_sharedPlug = null;
		if (pinCreationCancelled) {
			List<chs.cof.logical.cable.IPinList> cablePinLists = new ArrayList<chs.cof.logical.cable.IPinList>();
			getCablePinLists(cablePinLists);
			for (chs.cof.logical.cable.IPinList cablePinList : cablePinLists) {
				m_preemies.remove(cablePinList);
			}
			List<IUIDObject> toDelObjList = LogicUtils.getObjectsToBeDeleted(m_preemies);
			for (IUIDObject object : toDelObjList) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(object);
			}
			for (chs.cof.logical.cable.IPinList cablePinList : cablePinLists) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(cablePinList);
			}
			getController().getUndoableContainer().startEdit("Add Shared Inline Connector -Cancel");
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			getController().getUndoableContainer().endEdit();
		}
		return ok;
	}

	private void autoGeneratePinList()
	{
		int pinspacing = m_grid.getGridSpacing();
		ILocation loc = m_sharedAutoGenDynamic.getLocation();
		Point p1 = new Point(loc.getX(), loc.getY() + pinspacing);
		Point p2 = new Point(loc.getX() + AUTO_GEN_CONN_WIDTH * pinspacing, loc.getY() + 2 * pinspacing);
		Point p3 =
				new Point(loc.getX() + AUTO_GEN_CONN_WIDTH * 2 * pinspacing, loc.getY() + 2 * pinspacing);
		List<IPinList> pair = constructInlineConnectorPair(p1, p2, p3);
		assert pair != null && pair.size() == 2;
		m_schemJack.addAttachedObject(m_schemPlug);
		m_schemPlug.addAttachedObject(m_schemJack);
		//add the schem pin list to the diagram. This must be done before addPins is called
		//below. however, it should have happened in above createDisplayObject method itself.
		// todo creddy: Work on this in inline story

		//16.1 dts0101238208: ST161BashXSEEDSI10 : Validation failure after placing a shared inline
		setSubType(m_addPinListDialog.getSharedPinList().getType());

		CompositePinConnectivityFinder connectivityFinder = new CompositePinConnectivityFinder(getDiagram());
		addAutoGenPins(m_schemPlug, m_schemJack, connectivityFinder);
		connectivityFinder.connect();
	}

	private void getCablePinLists(List<chs.cof.logical.cable.IPinList> cablePinLists)
	{
		for (IUIDObject obj : m_preemies) {
			if (obj instanceof chs.cof.logical.cable.IPinList) {
				cablePinLists.add((chs.cof.logical.cable.IPinList) obj);
			}
		}
	}

	private static void addBackshellAndTermination(IPinList pl, ISharedPinList spl)
	{
		if (spl instanceof ISharedConnector) {
			ISharedConnector sc = (ISharedConnector) spl;
			ISharedBackshell sbs = sc.getBackshell();
			IConnector conn = ((IConnector) pl.getConnectivity());
			if (sbs != null && conn.getBackshell() == null) {
				IBackshell bs = FactoryMgr.getCablePropertiedFactory().createBackshell();
				bs.setSharedPinList(sbs);
				conn.setBackshell(bs);
			}
		}
	}

	protected IConnector getLogicConnector()
	{
		if (getSubType() == INLINE_JACK_CONNECTOR) {
			return getLogicJack();
		}
		else {
			return getLogicPlug();
		}
	}

	private IConnector getLogicJack()
	{
		IConnectivity connectivity = getLogicModel().getDesign().getConnectivity();
		assert connectivity != null;
		IConnector jack = (IConnector) connectivity.findSharedPinList(m_sharedJack);
		if (jack == null) {
			jack = FactoryMgr.getCablePropertiedFactory()
					.createInlineJackConnector(FactoryMgr.getCommonFactory().createUID());
			connectivity.addConnector(jack);
			jack.setSharedPinList(m_sharedJack);
			LogicObjectLockFinder.tryEdit(jack);
		}
		return jack;
	}

	private IConnector getLogicPlug()
	{
		IConnectivity connectivity = getLogicModel().getDesign().getConnectivity();
		assert connectivity != null;
		IConnector plug = (IConnector) connectivity.findSharedPinList(m_sharedPlug);
		if (plug == null) {
			plug = FactoryMgr.getCablePropertiedFactory()
					.createInlinePlugConnector(FactoryMgr.getCommonFactory().createUID());
			connectivity.addConnector(plug);
			plug.setSharedPinList(m_sharedPlug);
			LogicObjectLockFinder.tryEdit(plug);
		}
		return plug;
	}

	protected void generateConnGraphics(IPinList schem_conn, int pinspacing, int generationMode, SizeHelper sizeH
	)
	{
		// Note: does not currently pass generationMode down. Probably should, though that level of control is
		// only really needed when library parts are bound on creation, i.e. for interconnect connectors.
		AddSharedHelper.generateConnectorGraphics(schem_conn, getSubType(), sizeH, getLocalModel());
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

	protected boolean shouldAddPins()
	{
		// Don't auto add pins if the ctrl button is held.  We manage that here.. not the generic CreateConnectorAction.
		return false;
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

	public void mouseReleased(MouseEvent e)
	{
		if (getState() == STATE_PARAM) {
			if (m_goingToTerminate) {
				setFeedbackText(null);
				updateFeedback(e.getSource());
				cleanupTrans();
				final List<ISmartPoint> connectorPointList = getPointList();
				boolean success = createConnector(connectorPointList);

				Collection<IPinProxy> pinsToAdd = getSelectedSharedPins();
				if (success && !pinsToAdd.isEmpty()) {
					//dts0100526682: the isReference flag must be passed to the AddPinActionHelper to not reserve the non-reusable pins to a design.
					boolean isReference = m_addPinListDialog.getReference();
					m_addPinActionHelper.setIsReference(isReference);
					m_addPinActionHelper.setPlaceAsStack(m_addPinListDialog.getPlaceAsStack());
					m_addPinActionHelper.setPlaceAsGroup(m_addPinListDialog.getPlaceAsGroup());

					// Set delegate AddPinAction so that pins can be added interactively.
					m_addPinActionHelper.setUp(m_schemPlug, pinsToAdd);
					setState(STATE_PINS);
				}
				else {
					getController().getActionMgr().terminateActiveAction(success);
				}
			}
		}
		else if (getState() == STATE_GENERATE) {
			getController().getActionMgr().terminateActiveAction(m_sharedAutoGenDynamic != null);
		}
		else if (getState() == STATE_PINS) {
			// In add pins phase
			m_addPinActionHelper.mouseReleased(e);
		}
		updateStatusbarText();
	}

	protected boolean createConnector(@Nullable List<ISmartPoint> connectorPointList)
	{
		boolean success = (connectorPointList != null);
		if (success) {
			List<IPinList> pair = constructInlineConnectorPair(connectorPointList);
			if (pair == null || pair.size() != 2) {
				success = false;
			}
			else {
				m_schemJack.addAttachedObject(m_schemPlug);
				m_schemPlug.addAttachedObject(m_schemJack);
			}
		}

		// We created stuff before onTerminate() was called - cache what's in the
		// CreationDeletionHelper and clear it out.
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		m_preemies = new HashSet<>(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
		cdh.clearNewObjects();

		return success;
	}

	@NotNull protected Collection<IPinProxy> getSelectedSharedPins()
	{

		ISharedConnector sharedConnector = (ISharedConnector) m_addPinListDialog.getSharedPinList();
		Collection<IPinProxy> pinsToAdd = new ArrayList<>();
		if (sharedConnector.getType() == PinListTypeEnum.TypeInlineJack) {
			for (IPinProxy usedPinProxy : m_addPinListDialog.getUsedPins()) {

				ISharedPin sharedPin = usedPinProxy.getSharedPin();

				if (sharedPin != null) {
					ISharedPin matedPin = sharedPin.getMatePin();
					if (matedPin != null) {
						pinsToAdd.add(new PinProxy(matedPin));
					}
				}
			}
		}
		else {
			for (IPinProxy usedPinProxy : m_addPinListDialog.getUsedPins()) {
				ISharedPin sharedPin = usedPinProxy.getSharedPin();
				if (sharedPin != null) {
					pinsToAdd.add(usedPinProxy);
				}
			}
		}
		return pinsToAdd;
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

	/**
	 * Set the status text for this action
	 */
	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_PARAM:
				return ResourceMgr.getString(AddSharedInlineConnectorAction.class,
						"AddSharedInlineConnectorAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
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
			if (uidObj instanceof ISharedConnector) {
				ISharedConnector sharedPinlist = (ISharedConnector) uidObj;
				if (sharedPinlist.getType().equals(PinListTypeEnum.TypeInlinePlug) ||
						sharedPinlist.getType().equals(PinListTypeEnum.TypeInlineJack)) {
					return sharedPinlist;
				}
			}
		}
		return null;
	}

	/**
	 * Override this method if you want validation to be delayed past the 'onPostTerminate()' method call.
	 *
	 * @return true
	 */
	public boolean isPostTerminateValidationRequired()
	{
		return true;
	}
}