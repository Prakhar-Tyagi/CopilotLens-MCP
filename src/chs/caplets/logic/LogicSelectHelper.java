/*
 * Copyright 2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.action.IEventDistributor;
import chs.caf.caplet.helpers.MoveActionTimer;
import chs.caf.caplet.helpers.SelectHelper;
import chs.caf.caplet.selection.ISelectClient;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caf.caplet.selection.SelectionUtils;
import chs.caplets.UndoDisableForSharedObjectSave;
import chs.caplets.logic.actions.CreateStackPinAction;
import chs.caplets.logic.actions.MoveFunctionPortAction;
import chs.caplets.logic.actions.MovePinAction;
import chs.caplets.shared.actions.MoveConnectorAction;
import chs.cof.logical.cable.IPlugConnector;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cofUtils.logical.concurrency.GeneralLogicConcurrencyActionInfo;
import chs.cofUtils.logical.concurrency.LogicConcurrencyController;
import chs.cog.IPersistenceSession;
import chs.system.FactoryMgr;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;

public class LogicSelectHelper extends SelectHelper
{

	private MoveActionTimer movePinActionTimer = null; //Timer for starting MovePinAction after a long click
	private IAbstractSchemPin m_LastClickedPin;
	private IPinList m_LastConnectorPinList;

	public static void setLastclickedPoint(@NotNull Point lastclickedPoint)
	{
		m_lastclickedPoint = lastclickedPoint;
	}

	private static Point m_lastclickedPoint = null;
	private MovePinActionListener listener;

	public LogicSelectHelper(ICapletController controller,
			SelectSet selectSet,
			IEventDistributor distributor,
			ISelectClient selectClient)
	{
		super(controller, selectSet, distributor, selectClient);

		initializeMovePinTimer();
		m_LastClickedPin = null;
		m_LastConnectorPinList = null;
	}

	private void initializeMovePinTimer()
	{
		setListener(new MovePinActionListener(() -> otherRegisteredActionStarted()));
		movePinActionTimer = new MoveActionTimer(getListener());
	}

	@Nullable private IAbstractSchemPin getPinUnderMousePointer(MouseEvent e, SelectSet selections)
	{

		List<Selection> selectionsList = filterSelections(IAbstractSchemPin.class, selections);

		if (!selectionsList.isEmpty()) {
			Selection s = selectionsList.iterator().next();
			return (IAbstractSchemPin) s.getObject();
		}
		return null;
	}

	@NotNull private List<Selection> filterSelections(
			Class<?> className, SelectSet selections)
	{
		SelectionFilter pinFitler = new SelectionFilter();
		pinFitler.addOnlyClass(className);
		return selections.getFilteredSelections(pinFitler);
	}

	@Override public void mouseDragged(MouseEvent e)
	{
		super.mouseDragged(e);
		setLastclickedPoint(e.getPoint());
	}

	public void mousePressed(MouseEvent e)
	{
		super.mousePressed(e);

		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
			m_lastclickedPoint = e.getPoint();
		}

		//Start the MovePinAction Timer if the mouse was pressed on a Pin

		ICapletView eventView = (ICapletView) e.getSource();
		SelectSet selections = eventView.OnSelectPoint(e);
		if (m_mouseDownEvent != null && m_mouseDownEvent.isShiftDown() && m_mouseDownEvent.isControlDown()) {
			m_LastConnectorPinList = getPlugUnderMousePointer(e, selections);
			if (m_LastConnectorPinList != null) {
				movePinActionTimer.startTimer();
			}
			return;
		}
		IAbstractSchemPin clickedPin = getPinUnderMousePointer(e, selections);
		if (clickedPin != null) {
			//should filter on certain modifiers
			movePinActionTimer.startTimer();
			m_LastClickedPin = clickedPin;
		}
	}

	@Nullable private IPinList getPlugUnderMousePointer(MouseEvent e, SelectSet selections)
	{
		List<Selection> list = filterSelections(IPinList.class, selections);
		IPinList pinList = list.stream()
				.map(sel -> (IPinList) sel.getObject())
				.filter(pl -> pl.getConnectivity() instanceof IPlugConnector)
				.findFirst()
				.orElse(null);
		return pinList;
	}

	public void mouseReleased(MouseEvent e)
	{
		movePinActionTimer.stop();

		GeneralLogicConcurrencyActionInfo actionId = new GeneralLogicConcurrencyActionInfo("Select/Move/Stretch", true);
		UndoDisableForSharedObjectSave undoDisabler = new UndoDisableForSharedObjectSave();
		IPersistenceSession persistenceSession = FactoryMgr.getCHSSystem().getPersistenceSession();
		try {
			LogicConcurrencyController.getInstance().actionStarted(actionId);
			if (persistenceSession != null) {
				persistenceSession.addListener(undoDisabler);
			}
			super.mouseReleased(e);
		}
		finally {
			LogicConcurrencyController.getInstance().actionEnded(actionId);
			if (persistenceSession != null) {
				undoDisabler.clearUndo();
				persistenceSession.removeListener(undoDisabler);
			}
		}
	}

	@Override protected void removeSelections(SelectSet removeSet)
	{
		// first remove the selections - this will do additional handling like removing associated schem objects, when segment is deselected etc..
		super.removeSelections(removeSet);
		super.removeSelections(
				SelectionUtils.getAdditionalObjectsToRemoveFromSelection(removeSet, getSelectSet(), getController()));
	}

	public static Point getLastClickedPoint()
	{
		return m_lastclickedPoint;
	}

	public MovePinActionListener getListener()
	{
		return listener;
	}

	public void setListener(MovePinActionListener listener)
	{
		this.listener = listener;
	}

	protected class MovePinActionListener implements ActionListener
	{

		private Runnable callbackForMoveStart;

		MovePinActionListener(Runnable callbackForMoveStart)
		{
			this.callbackForMoveStart = callbackForMoveStart;
		}

		public void actionPerformed(ActionEvent e)
		{
			movePinActionTimer.stop();
			// check the delay between the mouse down event and now, if this exceeds the time limit
			// then do not start the movePinAction. This can happen if a mouse down event caught by another
			// handler, invokes a dialog (ex. Pin Properties Dialog) and we get the event after the dialog is dismissed
			if (movePinActionTimer.shouldStartMove()) {
				if (m_mouseDownEvent != null && m_mouseDownEvent.isShiftDown()) {
					callbackForMoveStart.run();
					startMoveConnectorAction();
				}
				else {
					startMovePinAction();
				}
				//cancel the drag only when successfully invoked the action. otherwise transient
				//gfx is not being cleared. ideally we should not blindly cancel the drag.
				IActionMgr actionMgr = getController().getActionMgr();
				if (actionMgr != null && actionMgr.getActiveAction() != actionMgr.getBaseAction()) {
					cancelDrag();
					disableDragging();
				}
			}
		}

		private void startMovePinAction()
		{
			//Add the moving Pin to the selection so the movePinAction can be enabled
			//Reset selection before starting movingPins if Ctrl is not down
			SelectHelper.selectObject(getController(), m_LastClickedPin,
					!(m_mouseDownEvent != null && m_mouseDownEvent.isControlDown()));

			ICaplet logicCaplet = getController().getCaplet();
			ActionEvent actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
			String actionName = null;
			final ICaplet.Type capletType = logicCaplet.getType();
			if (capletType == ICaplet.Type.ARTISAN_FUNCTION) {
				actionName = MoveFunctionPortAction.class.getName();
			}
			else if (capletType != ICaplet.Type.LAYOUT) {
				actionName = MovePinAction.class.getName();
				if (m_mouseDownEvent != null && m_mouseDownEvent.isControlDown()) {
					actionName = CreateStackPinAction.class.getName();
				}
			}
			if (!StringUtils.isBlank(actionName)) {
				logicCaplet.getActionDispatcher().actionPerformed(actionEvent, actionName);
			}
		}

		private void startMoveConnectorAction()
		{
			if (m_LastConnectorPinList != null) {
				ActionEvent actionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
				String actionName = MoveConnectorAction.class.getName();
				ICaplet logicCaplet = getController().getCaplet();
				logicCaplet.getActionDispatcher().actionPerformed(actionEvent, actionName);
			}
		}
	}
}