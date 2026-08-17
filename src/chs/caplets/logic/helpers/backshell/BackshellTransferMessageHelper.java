/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.helpers.backshell;

import chs.system.FactoryMgr;
import chs.utility.helpers.LogTabType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects and produces output messages for backshell termination pin transfers between
 * device-side connectors and harness connectors.
 */
public class BackshellTransferMessageHelper
{

	/**
	 * When set, {@link #flushMessages()} routes messages through this consumer instead
	 * of writing directly to the output window. This is needed in GHC, as it manages its own output lifecycle
	 * and wouldotherwise clear the output tab after these messages have been written.
	 */
	@Nullable private  Consumer<String> m_externalMessageConsumer = null;

	/**
	 * Sets an external message consumer. While set, all {@code BackshellTransferMessageHelper}
	 * instances will route their flushed messages through this consumer.
	 *
	 * @param consumer the message consumer to set
	 */
	public void setExternalMessageConsumer(@NotNull Consumer<String> consumer)
	{
		m_externalMessageConsumer = consumer;
	}

	/**
	 * Clears the external message consumer. Must be called in a {@code finally} block
	 * after the outer flow completes.
	 */
	public void clearExternalMessageConsumer()
	{
		m_externalMessageConsumer = null;
	}

	private final List<String> m_messages = new ArrayList<>();

	/**
	 * Adds a pre-built message string directly to the collection.
	 *
	 * @param message the formatted message to record
	 */
	public void addMessage(@NotNull BackshellTransferReporter.Message message)
	{
		String builtMessage = message.backshellTransferResult()
				.buildMessage(message.device(), message.deviceConnector(), message.targetConnector());
		if (builtMessage != null) {
			m_messages.add(builtMessage);
		}
	}

	/**
	 * Flushes all collected messages. If an external message consumer is active,
	 * messages are routed through it; otherwise they are sent directly to the output window.
	 */
	public void flushMessages()
	{
		for (String message : m_messages) {
			if (m_externalMessageConsumer != null) {
				m_externalMessageConsumer.accept(message);
			}
			else {
				FactoryMgr.getCAFUtils().sendMessage(message, LogTabType.TAB_HCONN.getLabel(), true);
			}
		}
		m_messages.clear();
	}

	/**
	 * Clears all collected messages.
	 */
	public void clear()
	{
		m_messages.clear();
	}

	public void addMessagesFromReport(@NotNull BackshellTransferReporter mReport)
	{
		for (BackshellTransferReporter.Message message : mReport.getMessages()) {
			addMessage(message);
		}
		mReport.clear();
	}
}

