/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.IImportConfig;
import chs.bridges.adaptors.tcmbse.ITranslationConfig;
import chs.bridges.adaptors.tcmbse.SysMLImportException;
import chs.bridges.adaptors.tcmbse.TCMbseLoggerFactory;
import chs.bridges.adaptors.tcmbse.config.AllowedStereotypesCombinationProvider;
import chs.bridges.adaptors.tcmbse.config.DiagramConfigBuilder;
import chs.bridges.adaptors.tcmbse.config.FilteringConfigBuilder;
import chs.bridges.adaptors.tcmbse.config.ImportConfig;
import chs.bridges.adaptors.tcmbse.config.ImportConfigParser;
import chs.bridges.adaptors.tcmbse.config.ImportTagsProvider;
import chs.bridges.adaptors.tcmbse.config.TranslationConfigProvider;
import chs.bridges.adaptors.tcmbse.translator.FileTranslationConfigBuilder;
import com.mentor.capital.logging.ILogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Factory class for creating import configurations from a configuration file.
 */
public class ImportConfigFactory
{

	@NotNull
	private final ILogger logger;

	public ImportConfigFactory()
	{
		logger = TCMbseLoggerFactory.getLogger();
	}

	/**
	 * Creates an import configuration from the specified configuration file.
	 *
	 * @param configFile the configuration file
	 * @return the import configuration, or null if the configuration file is invalid
	 */
	@Nullable
	protected IImportConfig createImportConfig(@NotNull File configFile)
	{
		try {
			ImportConfigParser importConfigParser = getImportConfigParser(configFile);

			AllowedStereotypesCombinationProvider stereotypesCombinationProvider =
					new AllowedStereotypesCombinationProvider(importConfigParser);
			ImportTagsProvider importTagsProvider = new ImportTagsProvider(importConfigParser);
			TranslationConfigProvider translationConfigProvider = new TranslationConfigProvider(importConfigParser);
			ITranslationConfig translationConfig =
					new FileTranslationConfigBuilder(stereotypesCombinationProvider, importTagsProvider,
							translationConfigProvider).build();
			return new ImportConfig(translationConfig,
					new FilteringConfigBuilder(stereotypesCombinationProvider).build(),
					new DiagramConfigBuilder(importConfigParser).build());
		}
		catch (SysMLImportException e) {
			logger.error("Failed to parse import configuration file: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Parses the specified configuration file.
	 *
	 * @param configFile the configuration file
	 * @return the parser
	 * @throws SysMLImportException if the configuration file cannot be parsed
	 */
	@NotNull
	private ImportConfigParser getImportConfigParser(@NotNull File configFile) throws SysMLImportException
	{
		ImportConfigParser parser = new ImportConfigParser();
		parser.parse(configFile);
		return parser;
	}
}
