/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.TCMbseLoggerFactory;
import chs.caf.CAFUtils;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.IMessageReporter;
import com.mentor.capital.logging.ILogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Error reporter for SysML
 */
public class SysMLErrorReporter implements IXMLValidationErrorHandler
{

	private final String linkedLoggedFile;
	private final ILogger logger;
	private IMessageReporter reporter;
	private static final String SYSML_IMPORT_CONFIGURATION = "SysML to Teamcenter to Capital Systems Import Configuration";
	private static final String SYSML_IMPORT_CONFIGURATION_XSD_WITH_PATH = "\"bridgesSysMLToTeamcenterToSystems.xsd\" to \"&lt;CAPITAL_HOME&gt;/dtd\"";

	public SysMLErrorReporter()
	{
		linkedLoggedFile = new TCMbseLogFileFilter().getLinkedLogFile();
		logger = TCMbseLoggerFactory.getLogger();
		reporter = (severity, message) -> CAFUtils.getInstance().getOutputWindow()
				.sendMessage(message, AppInfo.getAppInfo().getApplicationTitle(), true, true);
	}

	public void logAndShowErrorMessageInOutputTabWithPrompt(String message)
	{
		logger.error(message);
		reportInOutputTab(PromptSeverity.INFORMATION,
				getResourceString("SysMLSoaImporter.backendTermination.failure", linkedLoggedFile));
		showErrorDialog("ImportSysMLSoaModelAction.translationFailure", null, null);
	}

	public void handleTranslationFailure()
	{
		logAndShowErrorMessageInOutputTabWithPrompt("TC SysML model Translation failure");
	}

	public void handleGraphValidationFailure()
	{
		logAndShowErrorMessageInOutputTabWithPrompt("TC Project dependency order validation failure");
	}


	@NotNull public IMessageReporter getReporter()
	{
		return reporter;
	}

	public void reportInOutputTab(PromptSeverity severity, String message)
	{
		reporter.report(severity, message);
	}

	@NotNull
	private String getResourceString(String resourceKey, Object... args)
	{
		return ResourceMgr.getString(SysMLSoaModelImporter.class, resourceKey, args);
	}

	private void showErrorDialog(String resourceKeyRoot, @Nullable String guidanceParameter, @Nullable String implicationParameter, Object... messageParameters)
	{
		ResourceBasedMessageContent resourceBasedMessageContent =
				new ResourceBasedMessageContent(ImportSysMLModelAction.class, resourceKeyRoot);
		if (guidanceParameter != null) {
			resourceBasedMessageContent.setGuidanceParameters(guidanceParameter);
		}
		if (implicationParameter != null) {
			resourceBasedMessageContent.setImplicationsParameters(implicationParameter);
		}
		if (messageParameters.length > 0) {
			resourceBasedMessageContent.setMessageParameters(messageParameters);
		}
		Message.show(PromptSeverity.ERROR, resourceBasedMessageContent);
	}

	public void reportCancellation(String message)
	{
		logger.info(message);
		reportInOutputTab(PromptSeverity.INFORMATION, getResourceString("SysMLSoaImporter.backendTermination.user.cancelled"));
	}

	/**
	 * Handles the case when the import configuration file path is invalid.
	 * Logs the information and shows an error dialog to the user.
	 */
	public void handleInvalidImportConfigFilePath()
	{
		showErrorDialog("FileImportConfigBuilder.invalidConfigFilePath", null, null, SYSML_IMPORT_CONFIGURATION);
	}

	/**
	 * Handles the validation failure of the import configuration file.
	 * Logs the error and shows an error dialog to the user.
	 *
	 * @param exception the exception that occurred during validation
	 */
	@Override public void handleValidationFailure(@NotNull Exception exception)
	{
		logger.info(ResourceMgr.getString(FileImportConfigBuilder.class,
				"FileImportConfigBuilder.importConfig.invalid.message", SYSML_IMPORT_CONFIGURATION + " XML"));
		logger.error(exception);
		reportInOutputTab(PromptSeverity.INFORMATION, ResourceMgr.getString(FileImportConfigBuilder.class,
				"FileImportConfigBuilder.importConfig.invalid.failure", SYSML_IMPORT_CONFIGURATION + " XML",
				linkedLoggedFile));
		showErrorDialog("FileImportConfigBuilder.importConfig.invalid", null, null,
				SYSML_IMPORT_CONFIGURATION + " XML");
	}

	/**
	 * Handles the case when the import configuration XSD file is missing.
	 * Logs the information and shows an error dialog to the user.
	 */
	@Override public void handleMissingXSD()
	{
		showErrorDialog("FileImportConfigBuilder.importConfigXSD.missing", SYSML_IMPORT_CONFIGURATION_XSD_WITH_PATH,
				SYSML_IMPORT_CONFIGURATION, SYSML_IMPORT_CONFIGURATION);
	}
}
