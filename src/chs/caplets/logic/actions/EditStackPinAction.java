/*
 * Copyright 2011-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.drawplus.ISecondaryRepresentation;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cofUtils.logical.concurrency.ILogicConcurrencyActionContextForErrorReport;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IUIDObject;
import chs.ctf.caf.ui.CreationType;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.ui.PinPlaceOptionsParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.IDynamicGfxService;
import chs.utilities.ResourceMgr;
import chs.utility.DiagramHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.PinListHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditStackPinAction extends ControllerActionRT implements ICtxMenuProvider, MouseListener,
		MouseMotionListener, KeyListener
{

	private ISchemStackPin m_stackPin = null;

	protected GeneratorParameters m_genParams;
	//protected List<PinPlacementConstraints> m_constraints = new ArrayList<PinPlacementConstraints>();
	protected IDynamicGfxService m_dynamics;

	protected EditStackPinActionHelper m_editStackedPinActionHelper;

	public EditStackPinAction(ICapletController controller)
	{
		super(controller);
		setupActionHelper();
	}

	protected void setupActionHelper()
	{
		m_editStackedPinActionHelper = new EditStackPinActionHelper(this, false, true);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_stackPin = getOperand(getController().getSelectMgr().getPreSelections());

		boolean setup = false;
		if (m_stackPin != null) {
			setup = showDialog(new PinPlaceOptionsParams(CreationType.FOR_STACK_PIN));
		}

		if (!setup) {
			// dialog cancelled
			m_editStackedPinActionHelper.cleanUp();
			return IActionEnum.eCanceled;
		}

		if (!m_editStackedPinActionHelper
				.lockObjects(stackPin -> getLockErrorPrefix(), getActionContextForLockErrorReporting())) {
			return IActionEnum.eCanceled;
		}

		if (m_editStackedPinActionHelper.isDeleteAction()) {
			return IActionEnum.eCompleted;
		}

		return IActionEnum.eActivated;
	}

	private ILogicConcurrencyActionContextForErrorReport getActionContextForLockErrorReporting()
	{
		return new ILogicConcurrencyActionContextForErrorReport()
		{
			@Override public String getContext()
			{
				return getActionDisplayName();
			}

			@Override public String getObjectImplications()
			{
				return ResourceMgr.getString(CreateStackPinAction.class, "StackPinAction.lockerror.implecations");
			}
		};
	}

	private String getActionDisplayName()
	{
		return (String) getActionUI().getValue(Action.NAME);
	}

	String getLockErrorPrefix()
	{
		return ResourceMgr.getString(AddToStackPinAction.class, "EditStackPinAction.unableToUnstack",
				getActionDisplayName());
	}

	protected boolean showDialog(@NotNull IPlacementOptionParams params)
	{
		EditStackPinDialog dlg = new EditStackPinDialog(CAFUtils.getInstance().getDialogFrame(), m_stackPin, params);
		EditStackPinDialog.Result result = dlg.selectPins();
		if (result != EditStackPinDialog.Result.CANCEL) {
			return setupSelection(dlg.getPins(), result == EditStackPinDialog.Result.DELETE, dlg.getPlaceAsGroup());
		}
		return false;
	}

	protected boolean setupSelection(List<IPinProxy> selectedPins, boolean forDelete, boolean placeAsGroup)
	{
		IPinList pinList = (IPinList) m_stackPin.getParent();
		if (pinList == null) {
			return false;
		}
		if (!forDelete) {
			m_editStackedPinActionHelper.setPlaceAsGroup(placeAsGroup);
		}
		return m_editStackedPinActionHelper.setupStackForEdit(pinList, selectedPins, m_stackPin, forDelete);
	}

	@Nullable private ISchemStackPin getOperand(SelectSet selectSet)
	{
		for (SelectedUIDObjectIterator iter = selectSet.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof ISchemStackPin) {
				ISchemStackPin stackPin = (ISchemStackPin) uidObj;
				if (stackPin.getNumPins() < 1) { // Skip empty stack pins
					continue;
				}
				return stackPin;
			}
		}
		return null;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		boolean itWorked = true;
		if (successful) {
			IPinList pinList = (IPinList) m_stackPin.getParent();
			assert pinList != null : diagnosticMessage("Schematic pinlist is null", true);
			chs.cof.logical.cable.IPinList cablePL = pinList.getConnectivity();
			ISchemDiagram diagram = DiagramHelper.getDiagram(m_stackPin);
			assert (diagram != null);
			m_editStackedPinActionHelper
					.editStackPin(pinList, diagram, objectSet -> m_editStackedPinActionHelper.lockObjects(
							stackPin -> getLockErrorPrefix(), getActionContextForLockErrorReporting(), objectSet));

			//regenerates pinlist after editing it
			if (cablePL != null) {
				Collection<ISecondaryRepresentation> reps = cablePL.getAssociateRepresentations();
				if (reps != null) {
					for (ISecondaryRepresentation secRep : reps) {
						secRep.regenerateDiagramObject();
					}
				}
				else {
					itWorked = false;
					assert false : diagnosticMessage("Secondary representations are null", true);
				}
			}
			else {
				itWorked = false;
				assert false : diagnosticMessage("Connectivity pinlist is null", true);
			}
		}
		m_editStackedPinActionHelper.cleanUp();

		return itWorked;
	}

	private String diagnosticMessage(String msg, boolean successful)
	{
		return msg + " on EditStackPinAction.onTerminate(" + successful + ") : ";
	}

	@Override public boolean isEnabled()
	{
		return hasOperands(getController().getSelectMgr().getPreSelections()) &&
				getController().getCapletModel().isEditable() && super.isEnabled();
	}

	private boolean isLockable(Set<ILogicObject> lockableObjects)
	{
		for (ILogicObject logicObject : lockableObjects) {
			if (LogicObjectLockFinder.isLogicObjectLockedInOtherSession(logicObject)) {
				return false;
			}
		}
		return true;
	}

	protected boolean hasOperands(SelectSet sset)
	{
		ISchemStackPin stackPin = null;
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!(obj instanceof ISchemStackPin)) {
				continue;
			}
			ISchemStackPin tempstack = (ISchemStackPin) obj;
			if (tempstack.getNumPins() < 1) { // Skip empty stack pins
				continue;
			}

			if (stackPin == null) {
				stackPin = tempstack;
			}
			else if (stackPin != tempstack) {
				return false;
			}

			// All bets are off if the pinlist is not on the active diagram
			if (!isOnAcitiveDiagram(stackPin)) {
				return false;
			}
		}
		return okToUnstack(stackPin);
	}

	private boolean okToUnstack(@Nullable ISchemStackPin stackPin)
	{
		IPinList pinlist = stackPin != null ? (IPinList) stackPin.getParent() : null;
		if (pinlist == null || !PinListHelper.isEditableHarnessConnector(pinlist)) {
			return false;
		}
		return isLockable(getLockableObjects(stackPin));
	}

	private Set<ILogicObject> getLockableObjects(ISchemStackPin stackPin)
	{
		IPinList pinlist = stackPin != null ? (IPinList) stackPin.getParent() : null;
		if (stackPin != null && pinlist != null) {
			Set<ILogicObject> lockableObjects = new HashSet<>();
			lockableObjects.add(pinlist.getConnectivity());

			ISchemStackPin matedStack = ConnectionHelper.getConnectedStackedPin(stackPin);
			if (matedStack != null) {
				lockableObjects.add(pinlist.getConnectivity());
			}
			return lockableObjects;
		}
		return Collections.emptySet();
	}

	private boolean isOnAcitiveDiagram(ISchemStackPin stackPin)
	{
		IPinList pinlist = (IPinList) stackPin.getParent();
		if (pinlist != null) {
			return DiagramHelper.getDiagram(pinlist) == CAFUtils.getInstance().getActiveDiagram();
		}
		return false;
	}

	protected boolean hasOperands2(SelectSet sset)
	{
		IPinList pinlist = null;

		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			if (!(obj instanceof ISchemStackPin)) {
				continue;
			}
			ISchemStackPin stackPin = (ISchemStackPin) obj;
			if (stackPin.getNumPins() < 1) { // Skip empty stack pins
				continue;
			}

			if (pinlist == null) {
				pinlist = (IPinList) stackPin.getParent();
				assert pinlist != null;
				// All bets are off if the pinlist is not on the active diagram
				if (DiagramHelper.getDiagram(pinlist) != CAFUtils.getInstance().getActiveDiagram()) {
					return false;
				}
			}
			else if (pinlist != stackPin.getParent()) {
				return false;
			}
		}

		if (pinlist != null) {
			return PinListHelper.isEditableHarnessConnector(pinlist);
		}
		return false;
	}

	@Override public String getActionUIClass()
	{
		return EditStackPinActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (hasOperands(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	public void mouseEntered(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseEntered(e);
	}

	public void mouseExited(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseExited(e);
	}

	public void mousePressed(MouseEvent e)
	{
		m_editStackedPinActionHelper.mousePressed(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseReleased(e);
	}

	public void mouseDragged(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseDragged(e);
	}

	public void mouseClicked(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseClicked(e);
	}

	public void mouseMoved(MouseEvent e)
	{
		m_editStackedPinActionHelper.mouseMoved(e);
	}

	public String getStatusbarText()
	{
		return m_editStackedPinActionHelper.getStatusbarText();
	}

	@Override public void keyTyped(KeyEvent e)
	{

	}

	@Override public void keyPressed(KeyEvent e)
	{
		m_editStackedPinActionHelper.keyPressed(e);
	}

	@Override public void keyReleased(KeyEvent e)
	{

	}
}