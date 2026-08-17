package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectionUtils;
import chs.cof.logical.schem.IPinList;
import chs.system.FactoryMgr;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class AbstractAddPinAction extends ControllerActionRT
		implements ICtxMenuProvider, MouseListener, MouseMotionListener, KeyListener
{

	protected AddPinActionHelper m_addPinActionPresenter;
	protected AddPinActionModel m_addPinActionModel;
	private String ctxCommand;

	protected AbstractAddPinAction(ICapletController controller)
	{
		super(controller);
	}

	public boolean onTerminate(boolean successful)
	{
		boolean itWorked = false;
		if (m_addPinActionModel != null) {
			itWorked = m_addPinActionPresenter.execute(successful);
		}

		cleanUp();

		return itWorked;
	}

	// Enabled if there are any IParameterized objects selected.
	public boolean isEnabled()
	{
		if (!getController().getCapletModel().isEditable()) {// eg. read-only model
			return false;
		}
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		//
		// If we are in a transaction boundary, we MUST check to see if there were shared objects selected.
		//
		if (FactoryMgr.getSystemFactory().getCAFUtils().isWithinTransactionBoundary()) {
			if (SelectionUtils.isSharedObjectSelected(selections)) {
				return false;
			}
		}
		IPinList operand = getOperand(selections);
		if (operand == null) {
			return false;
		}

		if (LogicObjectLockFinder.isLogicObjectLockedInOtherSession(operand.getConnectivity())) {
			return false;
		}
		return super.isEnabled();
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperand(selections) != null) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (ctxCommand == null || !ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(getActionUI(), ctxCommand));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@NotNull protected abstract AddPinActionModel setupPinActionModel(@NotNull IPinList function);

	protected abstract IPinList getOperand(SelectSet selections);

	public void mouseEntered(MouseEvent e)
	{
		m_addPinActionPresenter.mouseEntered(e);
	}

	public void mouseExited(MouseEvent e)
	{
		m_addPinActionPresenter.mouseExited(e);
	}

	public void mousePressed(MouseEvent e)
	{
		m_addPinActionPresenter.mousePressed(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		m_addPinActionPresenter.mouseReleased(e);
	}

	public void mouseDragged(MouseEvent e)
	{
		m_addPinActionPresenter.mouseDragged(e);
		updateStatusbarText();
	}

	public void mouseClicked(MouseEvent e)
	{
		m_addPinActionPresenter.mouseClicked(e);
	}

	public void mouseMoved(MouseEvent e)
	{
		m_addPinActionPresenter.mouseMoved(e);
		updateStatusbarText();
	}

	public void keyPressed(KeyEvent e)
	{
		m_addPinActionPresenter.keyPressed(e);
	}

	public void keyReleased(KeyEvent e)
	{

	}

	public void keyTyped(KeyEvent e)
	{
	}

	public String getStatusbarText()
	{
		return m_addPinActionPresenter.getStatusbarText();
	}

	protected void cleanUp()
	{
		if (m_addPinActionModel != null) {
			m_addPinActionModel.cleanUp();
			m_addPinActionModel = null;
		}
	}
}
