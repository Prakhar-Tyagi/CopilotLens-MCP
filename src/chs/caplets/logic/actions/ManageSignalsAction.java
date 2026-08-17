package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.ICtxMenuProvider;
import chs.caf.IFIB;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.capitalmanager.appserver.IUserSession;
import chs.caplets.logic.actions.ui.ManageSignalsDialog;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.common.attr.IAttributeTypes;
import chs.common.attr.custom.IReadOnlyCustomAttribute;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.FactoryMgr;
import chs.utilities.HybridSet;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ManageSignalsAction extends ControllerActionRT implements ICtxMenuProvider
{

	@Nullable private IFunctionMessage m_Message = null;
	@Nullable private ManageSignalsDialog dialog;

	public ManageSignalsAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(@Nullable ActionEvent e)
	{
		m_Message = getSingleIFunctionMessage(getController().getSelectMgr().getPreSelections());
		if (m_Message == null) {
			return IActionEnum.eCanceled;
		}
		if (!isActionAllowed()) {
			return IActionEnum.eCanceled;
		}
		if (m_Message.getSharedConductor() != null) {
			Message.show(PromptSeverity.WARNING, ManageSignalsAction.class,
					"ManageSignalsAction.ActionInvokedOnSharedMessage");
		}
		IFIB fib = getController().getCaplet().getFIB();
		Frame owner = fib.getWindowMgr().getDialogFrame();
		dialog =
				new ManageSignalsDialog(owner, m_Message.getName(), true, m_Message);
		boolean success = dialog.showDialog();
		return success ? IActionEnum.eCompleted : IActionEnum.eCanceled;
	}

	private boolean isActionAllowed()
	{
		if (m_Message != null && m_Message.getSharedConductor() == null) {
			return true;
		}
		return isActionAllowedWithShared();
	}

	protected IUserSession getUserSession()
	{
		return FactoryMgr.getSystemFactory().getCHSSystem().getUserSession();
	}

	private boolean isActionAllowedWithShared()
	{
		assert m_Message != null;
		if (m_Message.getSharedConductor() != null && m_Message.getSharedConductor().isFrozen()) {
			return false;
		}
		return lockAndRefreshSharedFunctionMessage();
	}

	private boolean lockAndRefreshSharedFunctionMessage()
	{
		assert m_Message != null;
		if (!lockSharedFunctionMessage()) {
			return false;
		}
		m_Message.getSharedConductor().refresh();
		fireSharedMessageChangeEvent();
		return true;
	}

	protected boolean lockSharedFunctionMessage()
	{
		assert m_Message != null;
		return LockUpdateHelper.lock(m_Message.getSharedConductor());
	}

	private void fireSharedMessageChangeEvent()
	{
		if (m_Message != null) {
			Set<ISharedObject> updatedObjectSet = new HybridSet<>();
			updatedObjectSet.add(m_Message.getSharedConductor());
			LogicUtils.fireChangeEvent(updatedObjectSet);
		}
	}

	@Override public boolean isEnabled()
	{
		IFunctionMessage functionMessage = getSingleIFunctionMessage(getController().getSelectMgr().getPreSelections());
		if (functionMessage == null) {
			return false;
		}
		if (functionMessage.getSharedConductor() != null) {
			return !functionMessage.getSharedConductor().isFrozen();
		}
		return super.isEnabled() && isModelEditable();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			if (dialog != null) {
				dialog.apply();
			}
		}
		else {
			if (dialog != null) {
				dialog.cleanup();
			}
		}
		cleanup();
		return successful;
	}

	private void cleanup()
	{
		assert m_Message != null : "Empty message found";
		if (m_Message.getSharedConductor() != null) {
			m_Message.getSharedConductor().unlock();
			fireSharedMessageChangeEvent();
			fireModelChangeEvent();
		}
		m_Message = null;
		dialog = null;
	}

	protected void fireModelChangeEvent()
	{
		if (m_Message == null) {
			return;
		}
		HashSet<IUID> changedUIDs = new HashSet<IUID>();
		changedUIDs.add(m_Message.getUID());
		ICapletModel model = getCapletModel();
		model.notifyPreModelChange(new ModelChangeEvent(model, changedUIDs));
		model.notifyModelChange(new ModelChangeEvent(model, changedUIDs));
	}

	@Override public String getActionUIClass()
	{
		return ManageSignalsActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getSingleIFunctionMessage(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Nullable IFunctionMessage getSingleIFunctionMessage(SelectSet selection)
	{
		Set<ILogicObject> iLogicObject =
				selection.getSelectedUIDS()
						.stream()
						.map(ReferenceHelper::reduceToLogicObject)
						.collect(Collectors.toSet());
		if (iLogicObject.size() == 1) {
			ILogicObject obj = iLogicObject.iterator().next();
			if (obj instanceof IFunctionMessage) {
				IFunctionMessage msg = (IFunctionMessage) obj;
				IReadOnlyCustomAttribute dictionaryMessageNameAttr =
						msg.getStoredCustomAttribute(IAttributeTypes.DICTIONARY_MESSAGE_NAME);
				return (dictionaryMessageNameAttr != null && dictionaryMessageNameAttr.getValue() != null) ? msg : null;
			}
		}
		return null;
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Nullable public ManageSignalsDialog getDialog()
	{
		return dialog;
	}
}