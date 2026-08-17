/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2019-2025 Siemens
 */

package chs.caplets.logic.actions.shared.autoshare;

import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.shared.AbstractSharePinListActionHelper;
import chs.caplets.logic.actions.shared.IPinListShareContextProvider;
import chs.caplets.logic.actions.shared.IShareActionChangeReporter;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.IPinListShareContext;
import chs.utility.helpers.IPinListShareHelper;
import chs.utility.helpers.IShareActionChange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoSharePinListActionHelper extends AbstractSharePinListActionHelper
{

	@NotNull protected ILogicDesign mLogicDesign;
	@Nullable private IPinListShareContextProvider mAutoShareContextProvider;
	@NotNull protected IMessageReporterWithContext mMessageReporter;
	@NotNull protected AutoShareParams m_params;

	protected AutoSharePinListActionHelper(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, false);
		mMessageReporter = reporter;
		mLogicDesign = design;
		m_params = params;
	}

	private void sendMessage(@NotNull PromptSeverity severity, @NotNull String message,
			@NotNull IMessageContext context)
	{
		mMessageReporter.report(severity, message, context);
	}

	@Override protected IActionEnum postSetup(@Nullable String dialogTitle)
	{
		final boolean status = performAutoSharePinlist();
		if (status) {
			return IActionEnum.eCompleted;
		}
		return IActionEnum.eCanceled;
	}

	private boolean performAutoSharePinlist()
	{
		final AutoSharePinlistView autoSharePinlistView = createAutoSharePinlistView();
		if (autoSharePinlistView != null) {
			mAutoShareContextProvider = autoSharePinlistView;
			return autoSharePinlistView.execute();
		}
		return false;
	}

	@Nullable protected AutoSharePinlistView createAutoSharePinlistView()
	{
		return new AutoSharePinlistView(cablePinList, m_pinList, mLogicDesign, mMessageReporter, isBulkPromotion(),
				m_params);
	}

	@Nullable @Override protected IPinListShareContextProvider getPinlistShareContextProvider()
	{
		return mAutoShareContextProvider;
	}

	protected void reportSymbolNotAvailable()
	{
		sendMessage(PromptSeverity.ERROR, ResourceMgr.getString(AutoSharePinListActionHelper.class,
				"AutoSharePinListActionHelper.NoSymDefDialog.Message.text"), getMessageContext());
	}

	protected void reportSymbolOutOfDate()
	{
		sendMessage(PromptSeverity.ERROR, ResourceMgr
						.getString(AutoSharePinListActionHelper.class, "AutoSharePinListActionHelper.NotUptoDate.Message.text"),
				getMessageContext());
	}

	protected void reportDuplicatePins()
	{
		sendMessage(PromptSeverity.ERROR, ResourceMgr.getString(AutoSharePinListActionHelper.class,
				"AutoSharePinListActionHelper.DuplicatePins.Message.text"), getMessageContext());
	}

	protected void reportCompositeSymbolOutOfDate()
	{
		sendMessage(PromptSeverity.ERROR, ResourceMgr.getString(AutoSharePinListActionHelper.class,
				"AutoSharePinListActionHelper.OutOfDateChildren.Message.text"), getMessageContext());
	}

	protected void reportError(@NotNull IPinListShareHelper.ErrorCode errorCode)
	{
		final String errorMessage = errorCode == IPinListShareHelper.ErrorCode.DeviceFootprintMismatch ? ResourceMgr
				.getString(AutoSharePinListActionHelper.class,
						"AutoSharePinListActionHelper.DeviceFootprintMismatch.text") : errorCode.getErrorMessage();

		sendMessage(PromptSeverity.ERROR, errorMessage, getMessageContext());
	}

	@NotNull protected IMessageContext getMessageContext()
	{
		return determineMessageContext(m_pinList, cablePinList);
	}

	@NotNull public static IMessageContext determineMessageContext(@Nullable IPinList schemPL,
			@Nullable chs.cof.logical.cable.IPinList cablePL)
	{
		final IUIDObject contextObject = schemPL != null ? schemPL : cablePL;
		final IUIDObject mate = getMate(schemPL, cablePL);
		if (contextObject != null) {
			if (mate != null) {
				return IMessageContext.createContext(contextObject, mate);
			}
			return IMessageContext.createContext(contextObject);
		}
		return IMessageContext.UndeterminedContext;
	}

	@Nullable
	private static IUIDObject getMate(@Nullable IPinList schemPL, @Nullable chs.cof.logical.cable.IPinList cablePL)
	{
		if (cablePL instanceof IGenericInlineConnector) {
			if (schemPL != null) {
				return schemPL.getAttachedObjects(IPinList.EXCLUDE_MODULAR).getNext();
			}
			return ((IGenericInlineConnector) cablePL).getMate();
		}
		return null;
	}

	@NotNull @Override protected Runnable getConflictResolver(@NotNull ISharedObjectModificationObserver observer,
			@NotNull IPinListShareContext pinListShareContext)
	{
		return () -> {
		};
	}

	@Override public void cleanup()
	{
		super.cleanup();
		mAutoShareContextProvider = null;
	}

	@NotNull @Override
	protected IShareActionChangeReporter getShareActionChangeReporter(@NotNull IPinListShareContext pinListShareContext)
	{
		return new AutoShareActionChangeReporter();
	}

	private class AutoShareActionChangeReporter implements IShareActionChangeReporter
	{

		@Override public void reportChanges()
		{

		}

		@Override public void notify(@NotNull IShareActionChange change)
		{
			IMessageContext context = getMessageContext();
			IConnectivity connectivity = mLogicDesign.getConnectivity();
			ISharedPinList sharedPinList =
					mAutoShareContextProvider == null ? null : mAutoShareContextProvider.getSharedPinList();
			if (connectivity != null && sharedPinList != null) {
				chs.cof.logical.cable.IPinList sharedInstance = connectivity.findSharedPinList(sharedPinList);
				context = sharedInstance == null ? context : IMessageContext.createContext(sharedInstance);
			}
			sendMessage(PromptSeverity.INFORMATION, getResourceMessage(change), context);
		}

		@NotNull private String getResourceMessage(@NotNull IShareActionChange change)
		{
			switch (change.getReason()) {
				case DEVICE_CONNECTOR_RENAMED_DUE_TO_PART_MISMATCH:
					return ResourceMgr.getString(AutoSharePinListActionHelper.class,
							"AutoSharePinListActionHelper.DeviceConnector.renamedDueToPartMismatch.text",
							change.getOldValue(), change.getNewValue());
				case DEVICE_CONNECTOR_RENAMED_DUE_TO_DEVICE_PIN_MAPPED_TO_MULTIPLE_CONNECTORS:
					return ResourceMgr.getString(AutoSharePinListActionHelper.class,
							"AutoSharePinListActionHelper.DeviceConnector.renamedDueToDevicePinMappedToMultipleConnectors.text",
							change.getOldValue(), change.getNewValue());
				case DEVICE_CONNECTOR_RENAMED_DUE_TO_CAVITY_MISMATCH:
					return ResourceMgr.getString(AutoSharePinListActionHelper.class,
							"AutoSharePinListActionHelper.DeviceConnector.renamedDueToCavityMismatch.text",
							change.getOldValue(), change.getNewValue());
			}
			return StringUtils.BLANK;
		}
	}
}
