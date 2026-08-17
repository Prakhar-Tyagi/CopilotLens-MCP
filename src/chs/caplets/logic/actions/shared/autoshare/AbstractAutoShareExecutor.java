package chs.caplets.logic.actions.shared.autoshare;

import chs.caf.CAFUtils;
import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.shared.BaseShareActionHelper;
import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.IShareActionHelper;
import chs.caplets.logic.actions.shared.IShareOperandStrategy;
import chs.caplets.logic.actions.shared.OperandShareabilityStatus;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.utilities.IAuditTrailLogger;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.audit.AuditableEventType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractAutoShareExecutor
{

	@Nullable private IShareActionHelper m_helper;
	@NotNull private final IProject mProject;
	@NotNull private final ILogicDesign mDesign;
	@Nullable private final ISchemDiagram mDiagram;
	@NotNull private IMessageReporterWithContext mMessageReporter;
	@Nullable private String mNewlySharedObjName;
	@Nullable private String mNewlySharedObjUid;
	@Nullable private Consumer<String> mAuditObjUIDConsumer;
	@NotNull protected AutoShareParams m_params;

	protected AbstractAutoShareExecutor(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		mMessageReporter = reporter;
		mProject = project;
		mDesign = design;
		mDiagram = diagram;
		m_params = params;
	}

	@NotNull protected IProject getProject()
	{
		return mProject;
	}

	@NotNull protected ILogicDesign getDesign()
	{
		return mDesign;
	}

	@Nullable protected ISchemDiagram getDiagram()
	{
		return mDiagram;
	}

	@NotNull protected IMessageReporterWithContext getMessageReporter()
	{
		return mMessageReporter;
	}

	public void setAuditObjUIDConsumer(@Nullable Consumer<String> auditObjUIDConsumer)
	{
		mAuditObjUIDConsumer = auditObjUIDConsumer;
	}

	private boolean shouldAllowExecute(@NotNull BaseShareActionOperands operands)
	{
		return !shouldDisableShareForUnplaced(operands);
	}

	@NotNull private BaseShareActionOperands getShareOperands(@NotNull IUIDObject uidObjectToShare)
	{
		List<IUIDObject> extendedList = extendToShareableObjects(uidObjectToShare);
		return BaseShareActionHelper.getShareOperands(extendedList, getShareOperandStrategy());
	}

	protected abstract IShareOperandStrategy getShareOperandStrategy();

	@Nullable protected abstract Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(
			@NotNull BaseShareActionOperands operands);

	public boolean execute(@NotNull IUIDObject uidObjectToShare)
	{
		BaseShareActionOperands operands = getShareOperands(uidObjectToShare);
		final IShareOperandStrategy shareOperandStrategy = getShareOperandStrategy();
		final OperandShareabilityStatus shareabilityStatus = operands.getShareabilityStatus();
		if (shareOperandStrategy.isSkippable(shareabilityStatus)) {
			return true; // No object to share. Not a failure condition
		}
		if (shareOperandStrategy.isError(shareabilityStatus)) {
			logShareabilityErrorMessage(shareabilityStatus, operands);
			return false;
		}
		final boolean allowed = shouldAllowExecute(operands);
		if (!allowed) {
			return false;
		}
		try {
			return doExecute(operands);
		}
		finally {
			cleanUp();
			reset();
		}
	}

	private void logShareabilityErrorMessage(@NotNull OperandShareabilityStatus shareabilityStatus,
			@NotNull BaseShareActionOperands operands)
	{
		final String message = shareabilityStatus.getMessage(operands.getLogicObject());
		if (message == null) {
			return;
		}
		sendMessage(PromptSeverity.ERROR, message, getMessageContext(operands.getTarget(), operands.getLogicObject()));
	}

	private boolean doExecute(@NotNull BaseShareActionOperands operands)
	{
		final Pair<INamedUIDObject, IShareActionHelper> actionHelperPair = determineActionHelper(operands);

		if (actionHelperPair == null || actionHelperPair.getSecond() == null) {
			return false;
		}
		mNewlySharedObjName = actionHelperPair.getFirst().getName();
		mNewlySharedObjUid = actionHelperPair.getFirst().getUID().getString();
		checkAndNotifyFrozen(mNewlySharedObjName, operands.getTarget(), operands.getLogicObject());
		m_helper = actionHelperPair.getSecond();
		final boolean setupResult = m_helper.setup(operands, null, mDiagram) == IActionEnum.eCompleted;
		boolean editSuccessful = setupResult && doEdit(m_helper);
		if (editSuccessful) {
			if (mNewlySharedObjName != null && mNewlySharedObjUid != null) {
				final int evtType = m_helper.isNewSharedObject() ? AuditableEventType.SHARED_OBJECT_ADDED :
						AuditableEventType.SHARED_OBJECT_MODIFIED;
				storeAuditLog(evtType, mNewlySharedObjName, mNewlySharedObjUid);
			}
		}
		if (editSuccessful) {
			postSuccessfulShare(operands.getTarget(), operands.getLogicObject());
		}
		return editSuccessful;
	}

	protected boolean doEdit(@NotNull IShareActionHelper helper)
	{
		return helper.doEdit();
	}

	protected abstract void postSuccessfulShare(@Nullable IUIDObject target, @Nullable ILogicObject logicObject);

	private void storeAuditLog(int evtType, @NotNull String sharedObjName, @NotNull String sharedObjUID)
	{
		final String projectUid = mProject.getUID().getString();
		final String sharedStr = ResourceMgr.getString(AuditableEventType.class, "AuditableEventType.SHARED");
		final IAuditTrailLogger auditLogger = CAFUtils.getInstance().getAuditLogger();
		if (isBulkShare()) {
			auditLogger.storeEvent(evtType, sharedStr, projectUid,
					BaseShareActionHelper.getNewlySharedObjectName(sharedObjName, sharedObjUID), sharedObjUID);
			if (mAuditObjUIDConsumer != null) {
				mAuditObjUIDConsumer.accept(sharedObjUID);
			}
		}
		else {
			auditLogger.postEvent(evtType, sharedStr, projectUid,
					BaseShareActionHelper.getNewlySharedObjectName(sharedObjName, sharedObjUID), sharedObjUID);
		}
	}

	protected boolean isBulkShare()
	{
		return false;
	}

	@NotNull protected List<IUIDObject> extendToShareableObjects(@NotNull IUIDObject uidObjectToShare)
	{
		final List<IUIDObject> extendedList = new ArrayList<>();
		if (uidObjectToShare instanceof IPinList) {
			final chs.cof.logical.cable.IPinList cablePinlist = ((IPinList) uidObjectToShare).getConnectivity();
			if (cablePinlist instanceof IGenericInlineConnector) {
				extendedList.addAll(((IPinList) uidObjectToShare).getAttachedPinListObjects(IPinList.EXCLUDE_MODULAR));
			}
		}
		else if (uidObjectToShare instanceof IGenericInlineConnector) {
			extendedList.addAll(((IConnector) uidObjectToShare).getMates());
		}
		extendedList.add(uidObjectToShare);
		return extendedList;
	}

	private void cleanUp()
	{
		if (m_helper != null) {
			m_helper.cleanup();
		}
	}

	private void reset()
	{
		m_helper = null;
		mNewlySharedObjName = null;
		mNewlySharedObjUid = null;
	}

	protected void sendMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		mMessageReporter.report(severity, message, context);
	}

	private boolean shouldDisableShareForUnplaced(@NotNull BaseShareActionOperands operands)
	{
		final OperandShareabilityStatus shareabilityStatus =
				BaseShareActionHelper
						.getUnplacedObjectShareabilityStatus(operands, isShareOfUnplacedObjectsDisabled(), true);
		if (getShareOperandStrategy().isShareable(shareabilityStatus)) {
			return false;
		}
		logShareabilityErrorMessage(shareabilityStatus, operands);
		return true;
	}

	protected boolean isShareOfUnplacedObjectsDisabled()
	{
		return !m_params.doShareUnplacedObjects();
	}

	protected void checkAndNotifyFrozen(@Nullable String sharedObjectName, @Nullable IUIDObject target,
			@Nullable ILogicObject logicObject)
	{
		final String name = sharedObjectName != null ? sharedObjectName : StringUtils.EMPTY_STRING;
		if (BaseShareActionHelper.isFrozenSharedObjectsRequired(mDesign)) {
			final String message = ResourceMgr
					.getString(AbstractAutoShareExecutor.class, "AbstractAutoShareExecutor.WillFreeze.Message.text",
							name);
			sendMessage(PromptSeverity.INFORMATION, message, getMessageContext(target, logicObject));
		}
	}

	@NotNull protected abstract IMessageContext getMessageContext(@Nullable IUIDObject target,
			@Nullable ILogicObject logicObject);
}
