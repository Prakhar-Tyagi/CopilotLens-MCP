/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.IDiagramConfig;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCDiagramInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.ITCDiagramProcessor;
import chs.bridges.adaptors.tcmbse.diagramgenerator.StyleSetInfo;
import chs.bridges.adaptors.tcmbse.diagramgenerator.TCDiagramGenerationInfo;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.common.IDesignContainer;
import chs.utilities.suite.DesignType;
import chs.utility.DesignsImported;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Imports sysml functional diagrams
 */
public class SysMLFunctionalDiagramImporter
{

	private IProject project;
	private ITCDiagramProcessor diagramProcessor;
	@NotNull private IDiagramConfig diagramConfig;

	public SysMLFunctionalDiagramImporter(ITCDiagramProcessor diagramProcessor, IProject project,
			@NotNull IDiagramConfig diagramConfig)
	{

		this.project = project;
		this.diagramProcessor = diagramProcessor;
		this.diagramConfig = diagramConfig;
	}

	public void importDiagrams(List<DesignsImported> designsImported)
	{

		StyleSetHandler styleSetHandler = new StyleSetHandler();
		StyleSetInfo styleSetInfo = styleSetHandler.processStyleSets(diagramConfig.getStyleSetName());
		designsImported.forEach(designImported -> {
			IDesignContainer design = project.getDesignMgr()
					.getDesignByNameAndRevisionAndShortDescription(designImported.getDesignName(),
							designImported.getRevision(), designImported.getShortDescription(), DesignType.FUNCTIONS);
			if (design != null && design instanceof ILogicDesign) {
				ITCDiagramInfo diagramInfo = diagramProcessor.getDiagramInfo((ILogicDesign) design);
				if (diagramInfo != null && !diagramInfo.getSVGDiagramInfo().isEmpty()) {
					TCDiagramGenerationInfo tcDiagramGenerationInfo =
							new TCDiagramGenerationInfo(diagramInfo, diagramConfig);
					TCDiagramGenerator diagramGenerator = getDiagramGenerator(tcDiagramGenerationInfo, styleSetInfo);
					diagramGenerator.generate((ILogicDesign) design);
				}
			}
		});
	}

	@NotNull protected TCDiagramGenerator getDiagramGenerator(TCDiagramGenerationInfo tcDiagramGenerationInfo, @NotNull StyleSetInfo styleSetInfo)
	{
		return new TCDiagramGenerator(tcDiagramGenerationInfo, project, styleSetInfo);
	}
}
