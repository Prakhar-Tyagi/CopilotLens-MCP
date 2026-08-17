package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.helper.PinMappingHandler;
import chs.caplets.logic.actions.shared.helper.PortMappingHandler;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingConstants;

public class MapPanelForFunction extends MapPanel
{

	public MapPanelForFunction(EditSharedPinListModel emodel, ILogicDesign des)
	{
		super(emodel, des);
	}

	@NotNull @Override protected PinMappingHandler createHandler(EditSharedPinListModel emodel, ILogicDesign des)
	{
		return new PortMappingHandler(emodel, des);
	}

	@NotNull protected String getAssociateTooltip()
	{
		return "MapPanel.associate.port.tooltip";
	}

	@NotNull protected String getUnAvailableTooltip()
	{
		return "MapPanel.unavailable.port.tooltip";
	}

	@NotNull protected String getAutoAssociateWarningHeader()
	{
		return "MapPanel.autoassociate.port.warningheader";
	}

	@NotNull protected String getAutoAssociateWarningTitle()
	{
		return "MapPanel.autoassociate.port.warningtitle";
	}

	@NotNull protected String getShowUnavailableTooltip()
	{
		return "MapPanel.showUnavailable.port.tooltip";
	}

	@NotNull protected String getShowUnavailableText()
	{
		return "MapPanel.showUnavailable.port.text";
	}

	@NotNull protected String getUnassociateAllTooltip()
	{
		return "MapPanel.unassociateAll.port.tooltip";
	}

	@NotNull protected String getUnassociateTooltip()
	{
		return "MapPanel.unassociate.port.tooltip";
	}

	@NotNull protected String getAutoAssociateTooltip()
	{
		return "MapPanel.autoassociate.port.tooltip";
	}

	@NotNull @Override
	protected SharedPinListAddRemoveButtons getAddRemoveButtons(EditSharedPinListModel emodel, ILogicDesign des)
	{
		return new SharedFunctionAddRemoveButtons(toList, des, SwingConstants.HORIZONTAL, emodel,
				mHandler.getPinNameToCountMap());
	}
}
