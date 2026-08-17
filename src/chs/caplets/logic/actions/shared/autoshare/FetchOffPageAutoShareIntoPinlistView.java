package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FetchOffPageAutoShareIntoPinlistView extends AutoShareIntoPinlistView
{

	public FetchOffPageAutoShareIntoPinlistView(
			@NotNull ISharedPinList spl,
			@Nullable IPinList cpl,
			@Nullable chs.cof.logical.schem.IPinList pl,
			@NotNull ILogicDesign design,
			@NotNull IMessageReporterWithContext reporter, boolean isBulkShare,
			@NotNull AutoShareParams params)
	{
		super(spl, cpl, pl, design, reporter, isBulkShare, params);
	}

	protected boolean extendedPinMatch()
	{
		return false;
	}

	@NotNull @Override
	protected IMessageContext getMessageContext(@Nullable chs.cof.logical.schem.IPinList schemPinlist,
			@Nullable IPinList cablePinlist)
	{
		return IMessageContext.UndeterminedContext;
	}
}
