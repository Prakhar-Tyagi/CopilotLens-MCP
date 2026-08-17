/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.capture;

import chs.aws.ui.handlers.sysmltcsoa.ISysMLSoaHandler;
import chs.caf.cafmain.actions.capture.sysml.ISysMLSoaModelImporter;
import chs.caf.cafmain.actions.capture.sysml.SysMLSoaModelImporter;
import chs.cof.logical.ISysMLImportFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for SysML related features
 */
public class SysMLImportFactory implements ISysMLImportFactory
{

	@NotNull public ISysMLSoaModelImporter constructSysMLSoaImporter(ISysMLSoaHandler handler)
	{
		return new SysMLSoaModelImporter(handler);
	}
}
