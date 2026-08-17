/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.aws.ui.handlers.sysmltcsoa.ISysMLSoaHandler;
import chs.aws.ui.handlers.sysmltcsoa.progressDialog.SysMLSOAImportProgress;
import chs.aws.ui.handlers.sysmltcsoa.progressDialog.SysMLSOAProgressConstants;
import chs.bridges.adaptors.tcmbse.ContainerTypes;
import chs.bridges.adaptors.tcmbse.FunctionDesignInfo;
import chs.bridges.adaptors.tcmbse.IFilteringConfig;
import chs.bridges.adaptors.tcmbse.IImportConfig;
import chs.bridges.adaptors.tcmbse.ISysMLProjectNode;
import chs.bridges.adaptors.tcmbse.ITranslationConfig;
import chs.bridges.adaptors.tcmbse.TCMbseLoggerFactory;
import chs.bridges.adaptors.tcmbse.TranslationUtility;
import chs.bridges.adaptors.tcmbse.diagramgenerator.TCFunctionalDiagramProcessor;
import chs.bridges.adaptors.tcmbse.filtering.InputJSONBuilder;
import chs.bridges.adaptors.tcmbse.translator.SysMLModelTranslator;
import chs.bridges.adaptors.tcmbse.translator.TranslationResult;
import chs.caf.CAFUtils;
import chs.caf.cafmain.actions.capture.sysml.graph.IUsedProjectGraphBuilder;
import chs.caf.cafmain.actions.capture.sysml.graph.SysMLGraphUtils;
import chs.caf.cafmain.actions.capture.sysml.graph.UsedProjectGraphBuilder;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.project.IProject;
import chs.utilities.Environment;
import chs.utilities.FileUtils;
import chs.utility.DesignsImported;
import com.mentor.capital.logging.ILogger;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Imports sysml model from teamcenter
 */
public class SysMLSoaModelImportService
{

	public static final String TOOL_NAME = "CapitalSystems";
	private final String tempFolderPathAndName;
	private ILogger logger;
	private final SysMLErrorReporter sysMLErrorReporter;
	private final ISysMLSoaHandler soaHandler;

	public SysMLSoaModelImportService(@NotNull ISysMLSoaHandler soaHandler,
			@NotNull SysMLErrorReporter sysMLErrorReporter)
	{
		this.soaHandler = soaHandler;
		this.sysMLErrorReporter = sysMLErrorReporter;
		tempFolderPathAndName = Environment.getTemp() + File.separator;
		logger = TCMbseLoggerFactory.getLogger();
	}

	public void importSysMLModel(@NotNull String selectedObjDetails, @NotNull SysMLSOAImportProgress progress,
			@NotNull PropertyChangeSupport pcs, @NotNull IImportConfig importConfig)
	{
		progress.increment(SysMLSOAProgressConstants.IMPORTING_JSON);
		logger.debug("[1/5] Retrieving System Model Data from Teamcenter");

		IUsedProjectGraphBuilder usedProjectGraphBuilder = new UsedProjectGraphBuilder();

		long bt = System.currentTimeMillis();
		Map<String, List<String>> usedProjectsMap = soaHandler.getUsedProjects(selectedObjDetails);
		logger.debug("Time taken by usedProjects = " + (System.currentTimeMillis() - bt));

		ISysMLProjectNode mainProjectNode =
				usedProjectGraphBuilder.buildUsedProjectGraph(selectedObjDetails, usedProjectsMap::get);

		SysMLGraphUtils sysMLGraphUtils = new SysMLGraphUtils();
		boolean isValid = sysMLGraphUtils.validateGraph(mainProjectNode);

		if (!isValid) {
			sysMLErrorReporter.handleGraphValidationFailure();
			return;
		}

		Function<String, String> importCollectionsFunction =
				(inputJsonString) -> soaHandler.importCollections(inputJsonString, TOOL_NAME, pcs,
						sysMLErrorReporter::logAndShowErrorMessageInOutputTabWithPrompt,
						sysMLErrorReporter::reportCancellation);

			bt = System.currentTimeMillis();
			boolean isSuccess =
					populateJsons(mainProjectNode, importCollectionsFunction, importConfig.getFilteringConfig());
			logger.debug("Time taken by import collections = " + (System.currentTimeMillis() - bt));
			if (!isSuccess) {
				return;
			}
        Map<String, Set<String>> derivedTypes = soaHandler.getDerivedTypes(TOOL_NAME);

        Map<IFunctionLogicDesign, FunctionDesignInfo> functionalDesignMap = getFunctionDesignMap();
		String outputFileName = tempFolderPathAndName + "Output" + System.currentTimeMillis() + ".xml";
		try {
			ITranslationConfig translationConfig = importConfig.getTranslationConfig();
			SysMLModelTranslator sysMLModelTranslator =
					new SysMLModelTranslator(outputFileName, translationConfig, derivedTypes);
			TranslationResult
					translationResult =
					sysMLModelTranslator.translateSysMLModel(mainProjectNode, progress, functionalDesignMap);

			if (progress.isCancelled()) {
				sysMLErrorReporter.reportCancellation("Import was cancelled by the user");
				return;
			}
			if (!translationResult.isTranslationSuccessful()) {
				sysMLErrorReporter.handleTranslationFailure();
				return;
			}
			IProject currentProject = CAFUtils.getInstance().getCurrentProject();
			if (currentProject == null) {
				sysMLErrorReporter.logAndShowErrorMessageInOutputTabWithPrompt("No project is open");
				return;
			}
			SysMLFunctionalDesignImporter sysMLFunctionalDesignImporter =
					new SysMLFunctionalDesignImporter(sysMLErrorReporter);
			List<DesignsImported> designsImported = sysMLFunctionalDesignImporter.importDesignsFromFile(outputFileName);
			logger.debug(designsImported.isEmpty() ? "Import completed unsuccessfully" : "Import completed successfully");
			TCFunctionalDiagramProcessor diagramProcessor =
					new TCFunctionalDiagramProcessor(translationResult.getTranslationContext(),
							mainProjectNode, ContainerTypes.FUNCTION_DESIGN_DIAGRAM);
			SysMLFunctionalDiagramImporter sysMLFunctionalDiagramImporter =
					new SysMLFunctionalDiagramImporter(diagramProcessor, currentProject,
							importConfig.getDiagramConfig());
			sysMLFunctionalDiagramImporter.importDiagrams(designsImported);
		}
		finally {
			if (FileUtils.fileExists(outputFileName)) {
				new File(outputFileName).delete();
			}
		}
	}

	private boolean populateJsons(@NotNull ISysMLProjectNode projectNode, @NotNull Function<String, String> importCollectionsFunction,
							   @NotNull IFilteringConfig filteringConfig)
	{
		return TranslationUtility.traverse(projectNode, node -> node.getJson() != null, node ->
				retrieveAndSetJson(node, importCollectionsFunction, filteringConfig));
	}

	private boolean retrieveAndSetJson(ISysMLProjectNode projectNode,
			Function<String, String> importCollectionsFunction, IFilteringConfig filteringConfig)
	{
		InputJSONBuilder jsonBuilder = getInputJSONBuilder(projectNode, filteringConfig);
		String inputJsonString = jsonBuilder.build();
		if (inputJsonString == null) {
			return false;
		}
		String json = importCollectionsFunction.apply(inputJsonString);
		if (json.isBlank()) {
			return false;
		}
		projectNode.setJson(json);
		return true;
	}

	@NotNull protected InputJSONBuilder getInputJSONBuilder(@NotNull ISysMLProjectNode projectNode, @NotNull IFilteringConfig filteringConfig)
	{
		return new InputJSONBuilder(projectNode.getProjectId(), TOOL_NAME, logger, filteringConfig);
	}

	@NotNull
	protected Map<IFunctionLogicDesign, FunctionDesignInfo> getFunctionDesignMap()
	{
		FunctionalDesignBaseIdProcessor functionalDesign = new FunctionalDesignBaseIdProcessor();
		Map<IFunctionLogicDesign, FunctionDesignInfo> functionalDesignMap =
				functionalDesign.getFunctionalDesignBaseIds();
		return functionalDesignMap;
	}
}
