package chs.caplets.logic.actions.shared;

import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

public class ReusePanelForFunction extends ReusePanel
{

	public ReusePanelForFunction(EditSharedPinListModel emodel, ILogicDesign theDesign)
	{
		super(emodel, theDesign);
	}

	@NotNull protected String getAddAllPinReservationErrorGiaidance()
	{
		return "ReusePanel.addAll.portReservation.error.guidance";
	}

	@NotNull protected String getAddAllPinReservationErrorImplication()
	{
		return "ReusePanel.addAll.portReservation.error.implication";
	}

	@NotNull protected String getAddAllPinReservationErrorMessage()
	{
		return "ReusePanel.addAll.portReservation.error.message";
	}

	@NotNull protected String getAddAllPinReservationErrorContext()
	{
		return "ReusePanel.addAll.portReservation.error.context";
	}

	@NotNull protected String getAddAllPinReservationError()
	{
		return "ReusePanel.addAll.portReservation.error";
	}

	@NotNull protected String getAddPinReservationErrorGuaidance()
	{
		return "ReusePanel.add.portReservation.error.guidance";
	}

	@NotNull protected String getAddPinReservationErrorImplication()
	{
		return "ReusePanel.add.portReservation.error.implications";
	}

	@NotNull protected String getAddPinReservationErrorContextMessage()
	{
		return "ReusePanel.add.portReservation.error.message";
	}

	@NotNull protected String getAddPinReservationErrorContext()
	{
		return "ReusePanel.add.portReservation.error.context";
	}

	@NotNull protected String getAddPinReservationError()
	{
		return "ReusePanel.add.portReservation.error";
	}

	@NotNull protected String getAddTooltip()
	{
		return "ReusePanel.addPort.tooltip";
	}

	@NotNull protected String getAddAllTooltip()
	{
		return "ReusePanel.addallPort.tooltip";
	}

	@NotNull protected String getRemoveTooltip()
	{
		return "ReusePanel.removePort.tooltip";
	}

	@NotNull protected String getRemoveDisabledTooltip()
	{
		return "ReusePanel.removePort.disabled.tooltip";
	}

	@NotNull protected String getRemoveAllTooltip()
	{
		return "ReusePanel.removeallPorts.tooltip";
	}

	@NotNull protected String getRemoveAllDisabledTooltip()
	{
		return "ReusePanel.removeallPorts.disabled.tooltip";
	}

	@NotNull protected String getReusablePinsText()
	{
		return "ReusePanel.reusableports.text";
	}
}
