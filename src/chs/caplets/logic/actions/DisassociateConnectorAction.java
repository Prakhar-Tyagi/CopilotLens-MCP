package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.capitalmanager.appserver.IUserSession;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInternalPositionsContainer;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConnector;
import chs.cof.logical.shared.ISharedInternalPosition;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.ui.LockInfoDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 8 Apr, 2013
 */

public class DisassociateConnectorAction extends ControllerActionRT implements ICtxMenuProvider
{

	private IConnector m_connector;

	public DisassociateConnectorAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		boolean bStatus = false;
		if (successful) {
			bStatus = editModel();
		}
		return bStatus;
	}

	protected boolean editModel()
	{
		boolean bStatus = false;
		final IProject project = m_connector.getProject();
		ISharedConnector sharedConnector = (ISharedConnector) m_connector.getSharedObject();
		if (sharedConnector == null) {
			m_connector.setOccupiedPosition(null);
			bStatus = true;
		}
		else {
			Set<IUID> impactedSharedConnectors = new HashSet<IUID>();
			try {
				sharedConnector.refresh();
				if (!sharedConnector.lock()) {
					IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
					if (userSession != null) {
						LockInfoDialog.showLockInfoDialog(null, sharedConnector, userSession);
					}
					return bStatus;
				}

				ISharedConnector parentSharedConnector = getParentSharedConnector(sharedConnector);
				if (parentSharedConnector == null) {
					return bStatus;
				}

				if (sharedConnector.isFrozen()) {
					showConnectorFrozenErrorMessage();
					return bStatus;
				}
				if (!parentSharedConnector.isEditable()) {
					showDomainFailureMessage();
					return false;
				}

				impactedSharedConnectors.add(sharedConnector.getUID());
				impactedSharedConnectors.add(parentSharedConnector.getUID());
				sharedConnector.setOccupiedPosition(null);
				ISharedObjectMgr sharedObjectMgr = project.getSharedPinListMgr();
				sharedObjectMgr.fireChangeEvent(impactedSharedConnectors);
				parentSharedConnector.save(); // Equivalent to saving and unlocking sharedConnector
				getController().getUndoableContainer().endEdit();
				getController().getUndoableContainer().clear();
				bStatus = true;
			}
			finally {
				if (sharedConnector.isLocked()) {
					sharedConnector.unlock();
				}
			}
		}
		return bStatus;
	}

	private void showConnectorFrozenErrorMessage()
	{
		JLabel actionLabel = new JLabel();
		Font newLabelFont =
				actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(DisassociateConnectorAction.class,
				"DisassociateConnectorAction.frozenerror.Guidance"));

		MessageHelper.showErrorMessage(null,
				ResourceMgr.getString(DisassociateConnectorAction.class, "DisassociateConnectorAction.dialog.title"),
				ResourceMgr.getString(DisassociateConnectorAction.class,
						"DisassociateConnectorAction.frozenerror.headline"),
				ResourceMgr
						.getString(DisassociateConnectorAction.class, "DisassociateConnectorAction.frozenerror.body"),
				actionLabel);
	}
	protected void showDomainFailureMessage()
	{
		String resourceKeyRoot = "DisassociateConnectorAction.domainCheckFailure";
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(DisassociateConnectorAction.class, resourceKeyRoot);
		Message.show(PromptSeverity.ERROR, content);
	}

	@Nullable private ISharedConnector getParentSharedConnector(ISharedConnector sharedConnector)
	{
		ISharedInternalPosition occupiedPosition = sharedConnector.getOccupiedPosition();
		if (occupiedPosition != null) {
			IInternalPositionsContainer positionContainer = occupiedPosition.getInternalPositionContainer();
			if (positionContainer instanceof ISharedConnector) {
				return (ISharedConnector) positionContainer;
			}
		}
		return null;
	}

	@Override public String getActionUIClass()
	{
		return DisassociateConnectorActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isSelectionValid(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	public boolean isEnabled()
	{
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		SelectSet selectSet = new SelectSet();
		selectSet.add(selections);

		boolean isEditable = getController().getCapletModel().isEditable();
		return isEditable && isSelectionValid(selectSet) && super.isEnabled();
	}

	private boolean isSelectionValid(SelectSet selections)
	{
		m_connector = getSelectedConnector(selections);
		if (m_connector == null) {
			return false;
		}

		if (!m_connector.isPartAssigned()) {
			// Not a library part
			return false;
		}

		if (m_connector.getOccupiedPosition() == null) {
			// this is not a child connector
			return false;
		}

		return !isConnectorFrozen();
	}

	private boolean isConnectorFrozen()
	{
		ISharedObject sharedObject = m_connector.getSharedObject();
		return sharedObject != null && sharedObject.isFrozen();
	}

	@Nullable private IConnector getSelectedConnector(SelectSet selections)
	{
		IConnector connector = null;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();

			ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(obj);
			if (logicObject instanceof IConnector) {
				if (connector == null || connector.equals(logicObject)) {
					connector = (IConnector) logicObject;
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}
		return connector;
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}
}