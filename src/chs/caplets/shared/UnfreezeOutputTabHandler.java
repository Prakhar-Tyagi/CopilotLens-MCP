/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.shared;

import chs.caplets.logic.actions.shared.IUnfreezeStatusMessage;
import chs.caplets.logic.actions.shared.UnfreezeFeedbackColumn;
import chs.caplets.logic.actions.shared.UnfreezeMessage;
import chs.caplets.logic.actions.shared.UnfreezeMessageWrapper;
import chs.caplets.logic.actions.shared.UnfreezeStatusMessage;
import chs.caplets.logic.actions.shared.UnfreezeStatusWindowAssistant;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IUnfreezeOutputTabHandler;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.shared.IBaseDesignSharedUsage;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.common.IUID;
import chs.utilities.AlphaNumComparator;
import chs.utilities.DomainInaccessibleSharedObjectDesignHandledRuntimeException;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class UnfreezeOutputTabHandler implements IUnfreezeOutputTabHandler
{

	private final UnfreezeStatusWindowAssistant assistant;
	protected final Map<IUID, IUnfreezeStatusMessage> unfreezeStatusMessageMap = new HashMap<>();

	public UnfreezeOutputTabHandler(boolean activeWindow)
	{
		String tabName = ResourceMgr.getString(EditDesignDelegate.class, "Unfreeze.tabName");
		assistant =
				new UnfreezeStatusWindowAssistant(tabName, UnfreezeFeedbackColumn.Severity.toString(), activeWindow);
	}

	@Override
	public void showMessages(@NotNull Set<UnfreezeMessageWrapper> unfreezeMessages, @NotNull ILogicDesign design)
	{
		if (unfreezeMessages.isEmpty()) {
			return;
		}
		Set<IUnfreezeStatusMessage> messagesToAdd = new HashSet<>(unfreezeMessages.size());
		Set<IUnfreezeStatusMessage> messagesToRemove = new HashSet<>(unfreezeMessages.size());

		collectMessages(unfreezeMessages, design, messagesToAdd, messagesToRemove);

		assistant.removeStatusMessage(messagesToRemove);
		assistant.addStatusMessages(getSortedCollection(messagesToAdd));
	}

	private void collectMessages(@NotNull Set<UnfreezeMessageWrapper> unfreezeMessages, @NotNull ILogicDesign design,
			@NotNull Set<IUnfreezeStatusMessage> messagesToAdd, @NotNull Set<IUnfreezeStatusMessage> messagesToRemove)
	{
		for (UnfreezeMessageWrapper unfreezeMessage : unfreezeMessages) {
			IUID sharedObjectUID = unfreezeMessage.getSharedObject().getUID();
			//collect old message for this shared object(if present), which is to be deleted from fxTable
			if (unfreezeStatusMessageMap.containsKey(sharedObjectUID)) {
				messagesToRemove.add(unfreezeStatusMessageMap.get(sharedObjectUID));
			}

			//create & collect new messages for this shared object, which is to be added to the fxTable
			UnfreezeStatusMessage message = buildMessage(design, unfreezeMessage);
			messagesToAdd.add(message);
			unfreezeStatusMessageMap.put(sharedObjectUID, message);
		}
	}

	@NotNull
	private Collection<IUnfreezeStatusMessage> getSortedCollection(@NotNull Set<IUnfreezeStatusMessage> messages)
	{
		return messages.stream().sorted((msg1, msg2) -> {
			int result = Integer.compare(msg1.getStatus().getSortIndex(), msg2.getStatus().getSortIndex());
			if (result == 0) {
				result = AlphaNumComparator.compare(msg1.getMessage(), msg2.getMessage(), false);
			}
			if (result == 0) {
				result = AlphaNumComparator.compare(msg1.getObjectDetailText(), msg2.getObjectDetailText(), false);
			}
			return result;
		}).collect(Collectors.toList());
	}

	@NotNull
	protected UnfreezeStatusMessage buildMessage(@NotNull ILogicDesign design, @NotNull
			UnfreezeMessageWrapper unfreezeMessage)
	{
		String msg = ResourceMgr.getString(EditDesignDelegate.class, unfreezeMessage.getMessage().getResourceKey(),
				unfreezeMessage.getObjectName());
		ILogicDesign designToNavigate = design;
		UnfreezeMessage message = unfreezeMessage.getMessage();
		if (message == UnfreezeMessage.NonEditableDesignsFailure) {
			Set<ILogicDesign> nonEditableDesigns = unfreezeMessage.getAssociatedDesigns();
			if (nonEditableDesigns != null) {
				Optional<ILogicDesign> logicDesign = nonEditableDesigns.stream().min((o1, o2) -> o1.compareTo(o2));
				if (logicDesign.isPresent()) {
					designToNavigate = logicDesign.get();
				}
			}
		}
		else if (message == UnfreezeMessage.DomainRestrictedDesignFailure) {
			designToNavigate = null;
		}
		IUID designUID = designToNavigate != null ? designToNavigate.getUID() : null;
		IUID logicObjectUID = null;
		if (designToNavigate != null) {
			logicObjectUID =
					getLogicObjectUIDCorrespondingToSharedObject(designToNavigate, unfreezeMessage.getSharedObject());
		}
		UnfreezeStatusMessage statusMessage = new UnfreezeStatusMessage(message.getStatus(), msg,
				unfreezeMessage.getObjectName(), designUID, logicObjectUID);
		return statusMessage;
	}

	@Nullable private IUID getLogicObjectUIDCorrespondingToSharedObject(@NotNull ILogicDesign design,
			@NotNull ISharedObject sharedObject)
	{
		if (sharedObject instanceof ISharedMulticore) {
			for (IBaseDesignSharedUsage multicoreUsage : design.getSharedUsageMgr().getMulticoreUsages()) {
				ISharedObject sharedMC = multicoreUsage.getSharedObject();
				if (sharedMC == sharedObject) {
					return multicoreUsage.getLogicObjectUID();
				}
			}
		}
		else {
			Iterator<IDesignSharedUsage> iterator = design.getSharedUsageMgr().getUsages(sharedObject).iterator();
			if (iterator.hasNext()) {
				return iterator.next().getLogicObjectUID();
			}
		}
		IConnectivity connectivity = getConnectivity(design);
		if (connectivity != null) {
			ILogicObject logicObject = connectivity.findLogicObjectForShared(sharedObject);
			if (logicObject != null) {
				return logicObject.getUID();
			}
		}
		return null;
	}

	@Nullable private IConnectivity getConnectivity(@NotNull ILogicDesign design)
	{
		try {
			return design.getConnectivity();
		}
		catch (DomainInaccessibleSharedObjectDesignHandledRuntimeException ignored) {
			return null;
		}
	}
}
