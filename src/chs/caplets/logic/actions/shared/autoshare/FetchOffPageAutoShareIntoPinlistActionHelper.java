package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FetchOffPageAutoShareIntoPinlistActionHelper extends AutoShareIntoPinListActionHelper
{

	protected FetchOffPageAutoShareIntoPinlistActionHelper(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
	}

	@Nullable @Override protected AutoSharePinlistView createAutoSharePinlistView()
	{
		if (mSharedPinlist != null) {
			return new FetchOffPageAutoShareIntoPinlistView(mSharedPinlist, cablePinList, m_pinList, mLogicDesign,
					mMessageReporter, isBulkPromotion(), m_params);
		}
		return null;
	}

	@Override protected boolean isBulkPromotion()
	{
		return true;
	}

	@NotNull @Override protected IMessageContext getMessageContext()
	{
		return IMessageContext.UndeterminedContext;
	}
}
