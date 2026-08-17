package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.commands.AssociateConnectorCommand;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedConnector;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.logic.LogicObjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;

public class AssociateConnectorAction extends ControllerActionRT implements ICtxMenuProvider
{

	private ICapletModel m_model;
	private IConnector m_connector;

	public AssociateConnectorAction(ICapletController controller)
	{
		super(controller);
		m_model = controller.getCapletModel();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			if (!(m_model instanceof ILogicModel)) {
				return true;
			}
			final ICapletController capletController = getController();
			if (capletController == null) {
				return false;
			}

			Frame owner = capletController.getCaplet().getFIB().getWindowMgr().getDialogFrame();
			AssociateConnectorCommand m_command =
					new AssociateConnectorCommand(((ILogicModel) m_model).getDiagram(), m_connector);
			if (m_command.getCommandStatus() != AssociateConnectorCommand.COMMAND_STATUS.OK) {
				showFailureMessage(m_command);
				return false;
			}

			String title = ResourceMgr.getString(AssociateConnectorAction.class,
					"AssociateConnectorAction.dialog.title");
			AssociateConnectorDialog.DIALOG_RESULT result =
					AssociateConnectorDialog.showDialog(owner, title, m_command);
			if (result == AssociateConnectorDialog.DIALOG_RESULT.OK) {
				final boolean bStatus = m_command.execute();
				if (!bStatus) {
					showFailureMessage(m_command);
					return bStatus;
				}
				if (m_connector.isShared()) {
					capletController.getUndoableContainer().endEdit();
					capletController.clearUndoQueue();
				}
				return bStatus;
			}

			if (result == AssociateConnectorDialog.DIALOG_RESULT.NOSHOW) {
				showNoAssociationFound();
			}
			//		Clear select sets so we can delete (possibly selectted) schem propertied text.
			capletController.getSelectMgr().getPreSelections().clear();
			capletController.getSelectMgr().getCurrentSelections().clear();
		}

		return true;
	}

	private boolean showFailureMessage(AssociateConnectorCommand m_command)
	{
		if (m_command.getCommandStatus() == AssociateConnectorCommand.COMMAND_STATUS.SHAREDCONNECTOR_FROZEN) {
			showFrozenErrorMessage();
			return true;
		}
		if (m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.SHAREDCONNECTOR_ALREADYASSOCIATED) {
			showConnectorAlreadyAssociatedError();
			return true;
		}
		if (m_command.getCommandStatus() == AssociateConnectorCommand.COMMAND_STATUS.SHAREDCONNECTOR_DELETED) {
			showConnectorNotAvailableError();
			return true;
		}
		if (m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.SHAREDCONNECTOR_LOCKFAILURE) {
			showLockError();
			return true;
		}
		if (m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.SHAREDPOSITION_NOTAVAILABLE) {
			showPositionNotAvailableMessage();
			return true;
		}
		if (m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.SHAREDCONNECTOR_DOMAIN_ACCESS_FAILURE) {
			showDomainFailureMessage();
			return true;
		}
		if (m_command.getValidConnectors().isEmpty() || m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.TARGETCONNECTORS_NOTFOUND) {
			showNoAssociationFound();
			return true;
		}
		// Already displayed error message
		return m_command.getCommandStatus() ==
				AssociateConnectorCommand.COMMAND_STATUS.CANNOT_BREAK_REVISION;
	}

	private void showDomainFailureMessage()
	{
		String resourceKeyRoot = "AssociateConnectorAction.domainCheckFailure";
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(AssociateConnectorAction.class, resourceKeyRoot);
		Message.show(PromptSeverity.ERROR, content);
	}

	private void showConnectorNotAvailableError()
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.deletederror.Guidance"));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorAction.class, "AssociateConnectorAction.error.headline"),
				ResourceMgr.getString(AssociateConnectorAction.class,
						"AssociateConnectorAction.deletederror.Body"), actionLabel);
	}

	private void showConnectorAlreadyAssociatedError()
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.alreadyassociated.Guidance"));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorAction.class, "AssociateConnectorAction.error.headline"),
				ResourceMgr.getString(AssociateConnectorAction.class,
						"AssociateConnectorAction.alreadyassociated.Body"), actionLabel);
	}

	private void showLockError()
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.lockerror.Guidance"));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorAction.class, "AssociateConnectorAction.error.headline"),
				ResourceMgr.getString(AssociateConnectorAction.class,
						"AssociateConnectorAction.lockerror.Body"), actionLabel);
	}

	private void showPositionNotAvailableMessage()
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.lockerror.Guidance"));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.error.headline"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.positionNotAvailable.error"), actionLabel);
	}

	private void showNoAssociationFound()
	{
		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();
		MessageHelper.showInformationMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorDialog.class, "AssociateConnectorDialog.modular.label"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorDialog.association.notfound.msg"));
	}

	private void showFrozenErrorMessage()
	{
		Frame owner = CAFUtils.getInstance().getWindowMgr().getDialogFrame();
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr.getString(AssociateConnectorAction.class,
				"AssociateConnectorAction.frozenerror.Guidance"));

		MessageHelper.showErrorMessage(owner,
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.dialog.title"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.error.headline"),
				ResourceMgr.getString(AssociateConnectorDialog.class,
						"AssociateConnectorAction.frozenerror.body"), actionLabel);
	}


	protected ICapletModel getModel()
	{
		return m_model;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isSelectionsValid(selections)) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public String getActionUIClass()
	{
		return AssociateConnectorActionUI.class.getName();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
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

	private boolean isSelectionsValid(SelectSet selections)
	{
		m_connector = getSelectedConnector(selections);
		if (m_connector == null) {
			return false;
		}

		if (!LogicObjectUtils.isValidPositionContainer(m_connector)) {
			return false;
		}

		if (m_connector.getLibraryObject() == null) {
			return false;
		}

		if (m_connector.getOccupiedPosition() != null) {
			return false; // Already associated
		}

		ISharedConnector sharedConnector = (ISharedConnector) m_connector.getSharedObject();
		if (sharedConnector != null) {
			if (sharedConnector.getOccupiedPosition() != null) {
				return false;
			}
			if (sharedConnector.isFrozen()) {
				return false;
			}
		}
		return true;
	}

	public boolean isEnabled()
	{
		SelectSet selections = getController().getSelectMgr().getCurrentSelections();
		SelectSet selectSet = new SelectSet();
		selectSet.add(selections);

		boolean isEditable = getController().getCapletModel().isEditable();
		if (isEditable && isSelectionsValid(selectSet)) {
			return super.isEnabled();
		}

		return false;
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}
}
