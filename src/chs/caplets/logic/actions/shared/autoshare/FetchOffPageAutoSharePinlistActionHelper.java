package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.utility.IMessageReporterWithContext;
import chs.utility.helpers.IPinListShareContext;
import chs.utility.helpers.PinListShareContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FetchOffPageAutoSharePinlistActionHelper extends AutoSharePinListActionHelper
{

	protected FetchOffPageAutoSharePinlistActionHelper(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
	}

	@Nullable @Override protected AutoSharePinlistView createAutoSharePinlistView()
	{
		return new FetchOffPageAutoSharePinlistView(cablePinList, m_pinList, mLogicDesign, mMessageReporter,
				isBulkPromotion(), m_params);
	}

	@Override protected boolean isBulkPromotion()
	{
		return true;
	}

	@NotNull @Override protected IPinListShareContext getPinListShareContext()
	{
		return new PinListShareContext(isBulkPromotion()){
			@Override public boolean usePinReservationPreferenceSetting()
			{
				return m_params.usePinReservationPreferenceSetting();
			}
		};
	}
}
