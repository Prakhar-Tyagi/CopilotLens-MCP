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
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 *  Validates the import configuration file.
 */
public class ConfigFileValidator
{

	@NotNull
	private final SysMLErrorReporter sysMLErrorReporter;

	public ConfigFileValidator(@NotNull SysMLErrorReporter sysMLErrorReporter)
	{
		this.sysMLErrorReporter = sysMLErrorReporter;
	}

	/**
	 * Validates the import configuration file.
	 *
	 * @param configFile the configuration file to validate
	 * @return true if the file is valid, false otherwise
	 */
	protected boolean validate(@Nullable File configFile, @NotNull File xsdFile)
	{
		if (configFile == null || !configFile.exists()) {
			sysMLErrorReporter.handleInvalidImportConfigFilePath();
			return false;
		}
		XMLConfigSchemaValidator
				xsdValidator = new XMLConfigSchemaValidator(xsdFile, configFile, sysMLErrorReporter, new SysmlValidatorErrorHandler());
		return xsdValidator.validate();
	}
}
