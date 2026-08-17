/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2023 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.UndoableContainerIdler;
import chs.caplets.logic.Model;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGrid;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IAbstractPinIterator;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.IInlinePlugConnector;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.CreationUtils;
import chs.cofUtils.parameterized.AddPinHelper;
import chs.common.IUIDObject;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import chs.services.dynamicgfx.ISmartPoint;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.logic.LogicUtils;
import chs.utility.preferences.PreferenceSetHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Adds an instance of an existing inline to a diagram.
 * <p>
 * This is a "sub action" designed to be constructed and used by another action, rather than called via CAF
 */
public class PlaceInlineConnectorAction extends CreateInlineConnectorAction
{

	protected AddPinActionHelper m_addPinActionHelper;  // delegate for adding individual pins
	private Set<IUIDObject> m_preemies;
	private IInlineJackConnector inlineJack;
	private IInlinePlugConnector inlinePlug;
	private List<IAbstractPin> pins;
	private boolean asReference;
	private boolean autogenerate;
	private boolean placeAsGroup;
	private boolean placeAsStack;
	private IGenericInlineConnector operand;
	private Collection<IPinProxy> pinProxies;
	private boolean pinsAdded;


	public PlaceInlineConnectorAction(ICapletController controller, IGenericInlineConnector inlineHalf,
			List<IAbstractPin> pins, boolean autogenerate, boolean reference, boolean groupPlacement,boolean stackPlacement,
			Collection<IPinProxy> pinProxies)
	{
		super(controller);
		this.pins = pins;
		this.autogenerate = autogenerate;
		asReference = reference;
		placeAsGroup = groupPlacement;
		placeAsStack = stackPlacement;
		this.pinProxies = pinProxies;
		pinsAdded = !pinProxies.isEmpty();
		operand = inlineHalf;

		// here we know there won't be multiple mates - we'll assert below anyway
		if (inlineHalf instanceof IInlineJackConnector) {
			inlineJack = (IInlineJackConnector) inlineHalf;
			inlinePlug = (IInlinePlugConnector) inlineJack.getMate();
		}
		else if (inlineHalf instanceof IInlinePlugConnector) {
			inlinePlug = (IInlinePlugConnector) inlineHalf;
			inlineJack = (IInlineJackConnector) inlinePlug.getMate();
		}
		else {
			assert false;
		}
		assert inlineJack != null;
		assert inlinePlug != null;
		assert inlineJack.getMates().size() == 1;
		assert inlinePlug.getMates().size() == 1;
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		m_addPinActionHelper=new AddPinActionHelper(this,true,true);
		m_preemies = new HashSet<>();
		autogenerate = shouldAutoGeneratePins();
		ISchemDiagram diagram = (ISchemDiagram) getModel().getSheet();
		m_grid = diagram.getGrid();
		if(autogenerate){
			setState(STATE_GENERATE);
		}else{
			setState(STATE_PARAM);
		}
		IActionEnum state = super.onActivate(e);

		if (getState() == STATE_GENERATE) {
			DynamicRotationIndicator indicator = getRotationIndicator();
			indicator.hide();
		}
		return state;
	}

	@Override public boolean onTerminate(boolean successful)
	{
		boolean pinCreationCancelled = false;
		boolean actionSuccess = successful;
		try {
			setFeedbackText(null);
			updateFeedback(CAFUtils.getInstance().getActiveCapletView());

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

	private boolean cleanUp(boolean successful, boolean pinCreationCancelled, boolean actionSuccess)
	{
		CAFUtils.getInstance().setTempUndoableContainer(UndoableContainerIdler.instance());
		if (pinCreationCancelled) {
			for (IUIDObject obj : m_addPinActionHelper.getCreatedPinObjects()) {
				UIDMgr.removeObject(obj.getUID());
			}
		}
		boolean ok = actionSuccess;
		if (pinsAdded && successful) {
			boolean cleanUpSuccess = false;
			try {
				m_addPinActionHelper.cleanUp(true);
				cleanUpSuccess = true;
			}
			finally {
				ok &= cleanUpSuccess;
			}
		}
		else {
			m_addPinActionHelper.cleanUp(false);
			getLocalModel().getDynamicGfxService().removeAllDynamicGfx();
			getLocalModel().getDynamicGfxService().removeAllTransientGfx();
		}

		setState(STATE_LOITER);
		CAFUtils.getInstance().clearTempUndoableContainer();
		if (pinCreationCancelled) {
			List<IUIDObject> toDelObjList = LogicUtils.getObjectsToBeDeleted(m_preemies);
			for (IUIDObject object : toDelObjList) {
				CreationDeletionHelper.getTheCreationHelper().addDeletionObject(object);
			}
			getController().getUndoableContainer().startEdit("Add Inline Connector -Cancel");
			CreationDeletionHelper.getTheCreationHelper().processObjects();
			getController().getUndoableContainer().endEdit();
		}
		return ok;
	}
	@Override protected boolean createParamObject(boolean actionSuccess)
	{
		if (autogenerate) {
			super.createParamObject(actionSuccess);
		}
		if (m_schemPlug != null && m_schemJack != null) {
			Model model = getLocalModel();
			ISchemDiagram diag = model.getDiagram();
			if (getState() != STATE_GENERATE) {

				m_addPinActionHelper.setIsReference(asReference);
				m_addPinActionHelper.addPins(isOperandPlug() ? m_schemPlug : m_schemJack,
						isOperandPlug() ? m_schemJack : m_schemPlug, diag);
			}

			connectGfxObjectToModel(m_schemPlug);
			connectGfxObjectToModel(m_schemJack);

			m_schemJack.regenerateDiagramObject();
			m_schemPlug.regenerateDiagramObject();

			PreferenceSetHelper.applyStyleSet(m_schemJack.getObjectsForStyling(), diag, true);
			PreferenceSetHelper.applyStyleSet(m_schemPlug.getObjectsForStyling(), diag, true);

		}
		return true;
	}

	@Override public void mouseExited(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.mouseExited(e);
		}
		else {
			super.mouseExited(e);
		}
	}

	@Override public void keyPressed(KeyEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.keyPressed(e);
		}
		else {
			super.keyPressed(e);
		}
	}
	@Override public void mousePressed(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.mousePressed(e);
		}
		else {
			super.mousePressed(e);
		}
		updateStatusbarText();
	}

	@Override public void mouseEntered(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.mouseEntered(e);
		}
		else {
			super.mouseEntered(e);
		}
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			updateStatusbarText();
			m_addPinActionHelper.mouseClicked(e);
		}
		else {
			super.mouseClicked(e);
		}
		updateStatusbarText();
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.mouseMoved(e);
		}
		else {
			super.mouseMoved(e);
		}
	}

	@Override public void mouseDragged(MouseEvent e)
	{
		if (getState() == STATE_PINS && m_addPinActionHelper != null) {
			m_addPinActionHelper.mouseDragged(e);
		}
		else {
			super.mouseDragged(e);
		}
	}
	public String getStatusbarText()
	{
		switch (getState()) {
			case STATE_PARAM:
				return ResourceMgr.getString(PlaceInlineConnectorAction.class,
						"PlaceInlineConnectorAction.StatusBar.text");
			case STATE_PINS:
				return m_addPinActionHelper.getStatusbarText();
			default:
				return null;
		}
	}

	@Override public void mouseReleased(MouseEvent e)
	{
		if (getState() == STATE_PARAM) {
			if (m_goingToTerminate) {
				setFeedbackText(null);
				updateFeedback(e.getSource());
				cleanupTrans();
				final List<ISmartPoint> connectorPointList = getPointList();
				boolean success = createConnector(connectorPointList);
				if (success && !pinProxies.isEmpty()) {
					m_addPinActionHelper.setIsReference(asReference);
					m_addPinActionHelper.setPlaceAsGroup(placeAsGroup);
					m_addPinActionHelper.setPlaceAsStack(placeAsStack);
					m_addPinActionHelper.setUp(isOperandPlug() ? m_schemPlug : m_schemJack, pinProxies);
					setState(STATE_PINS);
				}
				else {
					getController().getActionMgr().terminateActiveAction(success);
				}
			}
		}
		else if (getState() == STATE_PINS) {
			m_addPinActionHelper.mouseReleased(e);
		}
		else if (getState() == STATE_GENERATE) {
			super.mouseReleased(e);
		}
		updateStatusbarText();
	}

	public boolean isOperandPlug(){
		return operand instanceof IInlinePlugConnector;
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
		m_preemies = new HashSet<IUIDObject>(CollectionUtils.createList(cdh.getNewObjectsToProcess()));
		cdh.clearNewObjects();
		return success;
	}
	protected boolean shouldAutoGeneratePins()
	{
		return autogenerate;
	}



	@Override protected List<IPin> addPins(int width, int height, int verticalOffset, IGrid grid, IPinList schem_conn,
			IConnector connector, boolean topdown)
	{
		// but for interconnect inlines, the autogenerate is not working and we have to add the existing pin ourselves
		if (connector instanceof IInterconnectObject) {
			IAbstractPinIterator it = connector.getPins();
			IAbstractPin icxPin = it.hasNext() ? it.getNext() : null;
			if (icxPin != null) {
				int ypos = topdown ? height : 0;
				IPin pin = AddPinHelper.generatePin(schem_conn, connector, (m_pinsOnLeft) ? 0 : width,
						ypos + verticalOffset, grid, icxPin, PreferenceSetHelper.getStyleSet(getDiagram()));

				return Collections.singletonList(pin);
			}
			assert false : "Interconnect inline without a pin?";
		}

		// NO-OP in all other cases because selected pins are added by the autogenerate later
		return Collections.emptyList();
	}

	/**
	 * Overridden here to use the existing inline halves in the connectivity rather than creating new connectivity
	 * objects
	 *
	 * @return The connectivity plug/jack half of the inline that is getting placed
	 */
	@Override protected IConnector getLogicConnector()
	{
		IConnector connector = null;
		if (getSubType() == INLINE_JACK_CONNECTOR) {
			connector = inlineJack;
		}
		else if (getSubType() == INLINE_PLUG_CONNECTOR) {
			connector = inlinePlug;
		}
		assert connector != null; // should never be null for this action
		return connector;
	}

	protected List<IPinList> constructInlineConnectorPair(List<ISmartPoint> point_list)
	{
		// decision on whether the instance will be home is based on usages of the Logic object before this one
		boolean home = !LogicUtils.hasUsage(inlinePlug);

		// Overridden here to set the home condition according to the usual pinlist rules - true for first instance, false otherwise
		List<IPinList> pair = super.constructInlineConnectorPair(point_list);
		if (inlinePlug != null) {
			ILogicDesign design = inlinePlug.getLogicDesign();
			if (design != null) {
				for (IPinList pl : pair) {
					if (pl != null) {
						pl.setHome(home);
					}
				}
			}
		}

		return pair;
	}

	@Override protected void preActionChanges()
	{
		// ensure that both halves are big eno%ugh for any pins that we're adding
		// this is attempted in the autogenerate of pins but does not work for inlines for some unknown reason
		// attempts to debug this resulted in hours of hell in the ConnectionHelper quagmire - hence the untidy workaround here
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();
		ensureSize(diagram);
	}

	@Override protected boolean postActionChanges()
	{
		// currently we only ever autogenerate the specified pins in this action
		ISchemDiagram diagram = (ISchemDiagram) getController().getCapletModel().getModelRoot();
		if(autogenerate) {
			List<IPinProxy> allPinProxies = new ArrayList<>(pinProxies);
			allPinProxies.addAll(PinProxy.create(pins));
			if (!(inlinePlug instanceof IInterconnectObject)) {
				AddPinListActionHelper actionHelper = new AddPinListActionHelper(this);
				actionHelper.setup(allPinProxies, autogenerate, asReference, false, placeAsGroup);
				if (autogenerate && m_schemPlug != null) {
					actionHelper.createMissingCablePinsFromProxies(m_schemPlug.getConnectivity());
				}
				allPinProxies.forEach(aPinProxy -> {
					IAbstractPin givenPin = aPinProxy.getCablePin();
					Collection<IAbstractPin> connectedPins = givenPin.getConnectedPins();
					IAbstractPin matePin = !connectedPins.isEmpty() ? connectedPins.iterator().next() : null;
					if (matePin == null) {
						matePin = CreationUtils.createPin(m_schemJack.getConnectivity());
						matePin.setConnectedPin(givenPin);
						m_schemJack.getConnectivity().addPin(matePin);
					}
				});

				actionHelper.addPins(diagram, m_schemPlug, m_schemJack);
			}
		}

		if (diagram != null) {
			Collection<ILogicObject> toBeLockedForAssisted = new HashSet<>();
			getConnectedConductors(toBeLockedForAssisted, m_schemJack);
			getConnectedConductors(toBeLockedForAssisted, m_schemPlug);
			ILogicDesign design = diagram.getDesign();
			if (design != null) {
				LogicObjectLockFinder.tryEdit(design, toBeLockedForAssisted);
				createConnections(diagram, m_schemJack);
				createConnections(diagram, m_schemPlug);
			}
		}
		return true;
	}

	private void createConnections(ISchemDiagram diagram, IPinList schemConnector)
	{
		if (schemConnector != null) {
			ObjectConnectionsGetter.createConnectionSchematics(schemConnector, diagram);
			PreferenceSetHelper.applyStyleSet(schemConnector.getObjectsForStyling(), diagram, true);
		}
	}

	private void getConnectedConductors(Collection<ILogicObject> toBeLockedForAssisted, IPinList schemConnector)
	{
		if (schemConnector != null) {
			for (IAbstractPin abstractPin : schemConnector.getCablePins(false)) {
				toBeLockedForAssisted.addAll(abstractPin.getConductorsAsSet());
			}
		}
	}

	@Override protected void clearAction(boolean actionSuccess)
	{
		super.clearAction(actionSuccess);
		inlineJack = null;
		inlinePlug = null;
		if (pins != null) {
			pins.clear();
			pins = null;
		}
		if (pinProxies != null) {
			pinProxies.clear();
			pinProxies = null;
		}
	}

	@Override protected void connectGfxObjectToModel(IGfxObject newObject)
	{
		if (!asReference) {
			if (autogenerate) {
				if (pins.isEmpty() && pinProxies.isEmpty()) {
					super.connectGfxObjectToModel(newObject);
				}
			}
			else if (pins.isEmpty() || pinProxies.isEmpty()) {
				super.connectGfxObjectToModel(newObject);
			}
		}
	}

	private void ensureSize(ISchemDiagram diagram)
	{
		List<IPinProxy> allPinProxies = new ArrayList<>(pinProxies);
		if (pins != null) {
			allPinProxies.addAll(PinProxy.create(pins));
		}
		if (allPinProxies.size() > 1 && m_point_list != null && m_point_list.size() == 3) {

			// the inline halves must be at least this height to fit the pins
			int spacing = diagram.getGrid().getGridSpacing();
			int minSize = spacing * allPinProxies.size() + spacing;

			// adjust the points clicked if required
			Iterator<ISmartPoint> it = m_point_list.iterator();
			Point pt1 = it.next().getAbsoluteLocation();
			it.next(); // not needed
			Point pt3 = it.next().getAbsoluteLocation();
			double y1 = pt1.getY();
			double y3 = pt3.getY();
			double height = Math.abs(y3 - y1);
			if (height < minSize) {
				if (y3 > y1) {
					pt3.setLocation(pt3.getX(), y1 + minSize);
				}
				else {
					pt1.setLocation(pt1.getX(), y3 + minSize);
				}
			}
			assert (double) minSize <= Math.abs(pt3.getY() - pt1.getY());
		}
	}
}
