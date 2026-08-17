/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.IImportConfig;
import chs.cof.project.IProject;
import chs.common.IProperty;
import chs.utilities.Environment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Builder for creating import configuration from a file.
 */
public class FileImportConfigBuilder implements IImportConfigBuilder
{

	@NotNull
	private IProject currentProject;
	@NotNull
	private final ConfigFileValidator configFileValidator;
	@NotNull
	private final ImportConfigFactory importConfigFactory;

	private static final String SYSML_IMPORT_CONFIGURATION =
			"SysML to Teamcenter to Capital Systems Import Configuration";

	public FileImportConfigBuilder(@NotNull IProject currentProject, @NotNull SysMLErrorReporter sysMLErrorReporter)
	{
		this.currentProject = currentProject;
		configFileValidator = new ConfigFileValidator(sysMLErrorReporter);
		importConfigFactory = new ImportConfigFactory();
	}

	@Nullable
	@Override
	public IImportConfig build()
	{
		File configFile = getConfigFile();
		if (!configFileValidator.validate(configFile, getXSDFile())) {
			return null;
		}
		assert configFile != null;
		return importConfigFactory.createImportConfig(configFile);
	}

	/**
	 * Retrieves the configuration file from the given import configuration project property.
	 *
	 * @return the configuration file, or null if the file name is invalid or project property does not exist
	 */
	@Nullable
	protected File getConfigFile()
	{
		IProperty importConfigProperty = currentProject.findPropertyByName(SYSML_IMPORT_CONFIGURATION);
		String fileName = (importConfigProperty != null) ? importConfigProperty.getAsString() : null;
		return (fileName != null && !fileName.trim().isEmpty()) ? new File(fileName.trim()) : null;
	}

	@NotNull
	public File getXSDFile()
	{
		File xsdFile = new File(Environment.getRoot() + File.separator + "dtd" + File.separator +
				"bridgesSysMLToTeamcenterToSystems.xsd");
		return xsdFile;
	}
}
