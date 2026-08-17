/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare;

import org.jetbrains.annotations.NotNull;

public interface IInlineConnectorInfo extends IObjectInfo
{
	void setMatedConnector(@NotNull IInlineConnectorInfo matedConnector);

	@NotNull IInlineConnectorInfo getMatedConnector();

	boolean isJack();
}
