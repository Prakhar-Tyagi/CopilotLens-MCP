/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2026 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletView;
import chs.caplets.shared.actions.SymDeviceTemporaryPlaceHolderCreationHelper;
import chs.cof.draw.IGfxContext;
import chs.cof.draw.IGriddable;
import chs.cof.draw.ISheet;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cofUtils.parameterized.BSPinPlacementConstraints;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.services.gfx.GfxView;
import chs.utility.DiagramHelper;
import chs.utility.helpers.DSCWithBackshellPlaceholderHelper;
import chs.utility.helpers.PinPlaceholderProviderForSymbolledDeviceInMove;
import org.jetbrains.annotations.NotNull;

/**
 * Builder for BSPinPlacementConstraints used in pin placement during schematic generation/ re-generation.
 * <p>
 * This builder allows for optional addition of temporary placeholders for devices with symbols,
 * which is necessary to ensure correct pin placement when such devices are present.
 */
public class BSPinPlacementConstraintsBuilder
{

	private final IPinList pinList;
	private final ISheet sheet;
	private final GeneratorParameters generatorParams;
	private final SymDeviceTemporaryPlaceHolderCreationHelper symDevPlaceHolderHelper;

	public BSPinPlacementConstraintsBuilder(IPinList pinList, ISheet sheet)
	{
		this.pinList = pinList;
		this.sheet = sheet;

		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		GfxView gfxView = (GfxView) view;
		ISchemDiagram diagram = (ISchemDiagram) gfxView.getDiagram();
		generatorParams = DiagramHelper.createGeneratorParameters(diagram);
		symDevPlaceHolderHelper =
				new SymDeviceTemporaryPlaceHolderCreationHelper(
						PinPlaceholderProviderForSymbolledDeviceInMove::getDeviceWithSymbol);
	}

	@NotNull
	public BSPinPlacementConstraintsBuilder addTempPlaceHolderForDevicesWithSymbols()
	{
		symDevPlaceHolderHelper.addTempPlaceHolderForDevicesWithSymbols(
				getSchemDeviceForPlaceholderCreation(),
				((IGriddable) sheet).getGrid(),
				generatorParams);

		return this;
	}

	private IPinList getSchemDeviceForPlaceholderCreation()
	{
		if (pinList.getConnectivity() instanceof IDeviceConnector) {
			DSCWithBackshellPlaceholderHelper helper = new DSCWithBackshellPlaceholderHelper(pinList);
			return helper.getPinListForPlaceholderCreation();
		}
		return pinList;
	}

	@NotNull
	public BSPinPlacementConstraints build(String pinname, boolean includeBoundaryExtensions, IGfxContext context)
	{
		return new BSPinPlacementConstraints(pinname, pinList, generatorParams.getSpacing(), includeBoundaryExtensions,
				sheet, context);
	}
}