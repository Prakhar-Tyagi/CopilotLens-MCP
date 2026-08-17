/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for handling XML validation errors.
 */
public interface IXMLValidationErrorHandler
{
	void handleMissingXSD();
	void handleValidationFailure(@NotNull Exception exception);
}
