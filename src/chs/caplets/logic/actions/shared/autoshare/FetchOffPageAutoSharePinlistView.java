package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FetchOffPageAutoSharePinlistView extends AutoSharePinlistView
{

	public FetchOffPageAutoSharePinlistView(@Nullable IPinList cpl, @Nullable chs.cof.logical.schem.IPinList pl,
			@NotNull ILogicDesign design, @NotNull IMessageReporterWithContext reporter, boolean isBulkShare,
			@NotNull AutoShareParams params)
	{
		super(cpl, pl, design, reporter, isBulkShare, params);
	}

	@Override public boolean execute()
	{
		if (mReuseView != null) {
			mReuseView.makeAllPinsReusable();
		}
		return super.execute();
	}

	protected boolean extendedPinMatch()
	{
		return false;
	}
}
