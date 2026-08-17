/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.TCMbseLoggerFactory;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ISVGDiagramInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCDiagramGenerationInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCDiagramInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCElementAdaptor;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCElementAdaptorFactory;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITcDiagramGenerator;
import chs.bridges.adaptors.tcmbse.diagramgenerator.StyleSetInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.TCElementAdaptorFactory;
import chs.caf.CAFUtils;
import chs.caf.ICAFProjectMgr;
import chs.caf.cafmain.actions.CAFCommandHelper;
import chs.caf.cafmain.actions.CAVALLog;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.actions.CAVALFeedback;
import chs.caplets.logic.actions.CAVALServiceFactory;
import chs.caplets.logic.actions.DiagramDeleter;
import chs.caplets.logic.actions.ICAVALLog;
import chs.caplets.logic.actions.ICAVALServiceFactory;
import chs.caplets.logic.actions.pagination.ICAVALPaginationDefinition;
import chs.caplets.logic.actions.pagination.INewPaginationDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.SharedObjectMgr;
import chs.cof.project.IProject;
import chs.common.IDesignUnloadHelper;
import chs.common.IProperty;
import chs.common.ValueTypeEnum;
import chs.common.graph.data.OGDPreferenceContext;
import chs.system.FactoryMgr;
import chs.utilities.AppInfo;
import chs.utilities.CAVALError;
import chs.utility.logic.DesignHelper;
import chs.utility.ui.progress.ProgressGroup;
import chs.view.schem.IMessageLoggerForSymbolCreation;
import chs.view.schem.MessageLoggerForSymbolCreationUsingReplaceAction;
import chs.view.utils.ICAVALService;
import chs.view.utils.ICAVALServiceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Teamcenter Diagram Generator
 */
public class TCDiagramGenerator implements ITcDiagramGenerator
{

	private final StyleSetInfo styleSetInfo;
	private ITCDiagramGenerationInfo diagramGenerationInfo;
	private final IProject project;
	private ITCElementAdaptorFactory<ILogicObject> tcElementAdaptorFactory = TCElementAdaptorFactory.getInstance();

	public TCDiagramGenerator(ITCDiagramGenerationInfo diagramGenerationInfo, IProject project, @NotNull StyleSetInfo styleSetInfo)
	{
		this.diagramGenerationInfo = diagramGenerationInfo;
		this.project = project;
		this.styleSetInfo = styleSetInfo;
	}

	@Override public void generate(ILogicDesign design)
	{
		// true if design is loaded by diagram generation
		boolean shouldUnload = DesignHelper.isFullySkeleton(design);
		try {
			ITCDiagramInfo tcDiagramInfo = diagramGenerationInfo.getDiagramInfo();
			ICAVALPaginationDefinition paginationDefinition = createPaginationDefinition(design, tcDiagramInfo);
			if (paginationDefinition == null) {
				return;
			}
			design.lock();
			OGDPreferenceContext context = new OGDPreferenceContext(false);
			context.setDiagramGeneratorPlugin(diagramGenerationInfo.getPlugin());
			context.setCavalPreferenceSet(styleSetInfo.getStyleSet());
			ICAVALServiceResult serviceResult = generateDiagram(paginationDefinition, context);
			List<ISchemDiagram> generatedDiagrams = serviceResult.getGeneratedDiagrams();
			applyStyleSetToDiagrams(generatedDiagrams);
			if (!styleSetInfo.canConfiguredStyleSetBeApplied()) {
				String styleSetName = diagramGenerationInfo.getDiagramConfig().getStyleSetName();
				assert styleSetName != null;
				assignStyleSetToDiagrams(generatedDiagrams, styleSetName);
			}
			saveDesign(design);
		}
		finally {
			design.unlock();
			if (shouldUnload) {
				IDesignUnloadHelper designUnloadHelper = FactoryMgr.getCommonFactory().createDesignUnloadHelper();
				designUnloadHelper.unloadDesignChildren(design, false);
			}
		}
	}

	private void applyStyleSetToDiagrams(@NotNull List<ISchemDiagram> generatedDiagrams)
	{
		generatedDiagrams.forEach(diagram -> diagram.regenerateDiagramObject());
	}

	private void assignStyleSetToDiagrams(@NotNull List<ISchemDiagram> generatedDiagrams, @NotNull String styleSetName)
	{
		generatedDiagrams.forEach(diagram -> diagram.setPreferenceSetName(styleSetName));
	}

	protected void saveDesign(ILogicDesign design)
	{
		CAFCommandHelper cafCommandHelper = createCAFCommandHelper();
		try {
			cafCommandHelper.saveDesign(design);
		}
		catch (UserSessionException e) {
			TCMbseLoggerFactory.getLogger().error("Failed during save, " + e.getMessage());
		}
		ICAFProjectMgr projectMgr = CAFUtils.getInstance().getCAFProjectMgr();
		if (projectMgr != null) {
			projectMgr.projectChanged(project);
		}
		SharedObjectMgr.fireChangeEventForManagers(project.getSharedConductorMgr(),
				project.getSharedPinListMgr());
	}

	@NotNull protected CAFCommandHelper createCAFCommandHelper()
	{
		CAFCommandHelper cafCommandHelper = new CAFCommandHelper();
		return cafCommandHelper;
	}

	@NotNull protected ICAVALServiceResult generateDiagram(ICAVALPaginationDefinition paginationDefinition,
			OGDPreferenceContext context)
	{
		ICAVALServiceFactory factory = CAVALServiceFactory.getInstance();
		ICAVALService cavalService = factory.createCAVALService(createFeedbackObject(), DiagramDeleter::execute);
		IMessageLoggerForSymbolCreation messageLogger = new MessageLoggerForSymbolCreationUsingReplaceAction();
		return cavalService.generateDiagrams(paginationDefinition, context, messageLogger);
	}

	@NotNull protected ICAVALLog getCAVALLog()
	{
		return new CAVALLog(CAFUtils.getInstance().getOutputWindow(), AppInfo.getAppInfo().getApplicationTitle());
	}

	@Nullable protected ICAVALPaginationDefinition createPaginationDefinition(ILogicDesign design,
			ITCDiagramInfo tcDiagramInfo)
	{
		Map<String, ISVGDiagramInfo> svgDiagramInfo = tcDiagramInfo.getSVGDiagramInfo();
		IConnectivity connectivity = design.getConnectivity();
		if (connectivity == null || svgDiagramInfo.isEmpty()) {
			return null;
		}
		ICAVALServiceFactory factory = CAVALServiceFactory.getInstance();
		ICAVALPaginationDefinition paginationDefinition = factory.createPaginationDefinitionFor(design);
		svgDiagramInfo.forEach((diagramName, tcSvgDiagramInfo) -> {
			INewPaginationDiagram distributionDiagram = paginationDefinition.createDistributionDiagram(diagramName);
			connectivity.getFunctions().stream()
					.filter(function -> svgElementInfoPresent(tcSvgDiagramInfo,
							tcElementAdaptorFactory::createPartPropertyAdaptor, function))
					.forEach(func -> {
						distributionDiagram.addPinList(func);
						func.getPins().stream()
								.filter(pin -> svgElementInfoPresent(tcSvgDiagramInfo,
										tcElementAdaptorFactory::createPortAdaptor, pin))
								.forEach(distributionDiagram::addPin);
					});
			connectivity.getConductors().stream()
					.filter(conductor -> svgElementInfoPresent(tcSvgDiagramInfo,
							tcElementAdaptorFactory::createConnectorAdaptor, conductor))
					.forEach(distributionDiagram::addConductor);
			ISVGDiagramInfo svgInfoForDiagram = tcDiagramInfo.getSVGDiagramInfo(diagramName);
			assert svgInfoForDiagram != null;
			svgInfoForDiagram.getPropertyEntries(diagramName).forEach(propertyEntry -> {
				IProperty property = FactoryMgr.getCommonFactory()
						.constructProperty(propertyEntry.getPropertyName(), ValueTypeEnum.TypeString,
								propertyEntry.getPropertyValue(), propertyEntry.isEditable(), distributionDiagram);
				distributionDiagram.addProperty(property);
			});
		});
		return paginationDefinition;
	}

	private boolean svgElementInfoPresent(ISVGDiagramInfo tcSvgDiagramInfo,
			@NotNull Function<ILogicObject, ITCElementAdaptor> tcElementAdaptor, ILogicObject object)
	{
		ITCElementAdaptor elementAdaptor = tcElementAdaptor.apply(object);
		return elementAdaptor != null && tcSvgDiagramInfo.getSVGElementInfo(elementAdaptor) != null;
	}

	@NotNull
	private CAVALFeedback createFeedbackObject()
	{
		ProgressGroup progress = new ProgressGroup("test");
		CAVALError errorReporter = new CAVALError();
		ICAVALLog log = getCAVALLog();
		CAVALFeedback feedback = CAVALServiceFactory.getInstance().createFeedback(progress, errorReporter, log);
		return feedback;
	}
}
