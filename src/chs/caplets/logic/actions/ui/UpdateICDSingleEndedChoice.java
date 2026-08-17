/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caplets.logic.icd.ICDInterconnectPreprocessor;
import chs.caplets.logic.icd.ICDSingleEndedConnectStrategy;
import chs.caplets.logic.icd.PlacingPinRouteInfo;
import chs.caplets.logic.icd.SchemDeviceICDPinInfo;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import chs.utilities.CommonUtils;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to compute user's choice in generation of single ended conductors in Update ICD action
 */
public class UpdateICDSingleEndedChoice extends ICDSingleEndedChoice
{

	@NotNull private Set<Map.Entry<IPinList, IDeviceICD>> entries;
	@NotNull private ISchemDiagram diagram;
	@Nullable private ILogicDesign design;

	public UpdateICDSingleEndedChoice(@NotNull Set<Map.Entry<IPinList, IDeviceICD>> entries,
			@NotNull ISchemDiagram diagram, @Nullable ILogicDesign design, @Nullable IProject project)
	{
		super(project);
		this.entries = entries;
		this.diagram = diagram;
		this.design = design;

	}

	@Override @NotNull protected String getPrefKey()
	{
		return "UpdateICD.userChoice";
	}

	@NotNull protected String getMessageResourceKey()
	{
		return "UpdateICDAction.userChoice";
	}

	protected boolean shouldGetChoiceFromUser()
	{
		SetMap<IDeviceICD, IPinList> icdToPinlists = new SetMap<>();
		for (Map.Entry<IPinList, IDeviceICD> entry : entries) {
			icdToPinlists.add(entry.getValue(), entry.getKey());
		}
		for (IDeviceICD deviceICD : icdToPinlists.keySet()) {
			Set<IPinList> pinLists = icdToPinlists.get(deviceICD);
			for (IPinList pinList : pinLists) {
				if (areThereMissingSignalsOnPinlist(deviceICD, pinList)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean areThereMissingSignalsOnPinlist(@NotNull IDeviceICD deviceICD, @NotNull IPinList pinList)
	{
		SchemDeviceICDPinInfo icdPinInfo = new SchemDeviceICDPinInfo(pinList);
		List<PlacingPinRouteInfo> pinRouteInfos =
				ICDInterconnectPreprocessor.constructPlacingPinRouteInfo(icdPinInfo, deviceICD,
						CommonUtils.getNoFilter());
		for (PlacingPinRouteInfo pinRouteInfo : pinRouteInfos) {
			for (IICDAssociatedSignal associatedSignal : pinRouteInfo.getAssociatedSignals()) {
				boolean present = ICDSingleEndedConnectStrategy.isSignalAlreadyPresentOnTheDiagram(diagram,
						pinRouteInfo.getPlacingPin(), associatedSignal, isWiringAbstraction(),
						getCableConductorType());
				if (!present) {
					return true;
				}
			}
		}
		return false;
	}

	@NotNull private Class<? extends IConductor> getCableConductorType()
	{
		return isWiringAbstraction() ? IWireConductor.class : INetConductor.class;
	}

	private boolean isWiringAbstraction()
	{
		if (design != null) {
			IDesignAbstraction designAbstraction = design.getDesignAbstraction();
			return designAbstraction != null && designAbstraction.getAllowAutoCreation();
		}
		return false;
	}
}
