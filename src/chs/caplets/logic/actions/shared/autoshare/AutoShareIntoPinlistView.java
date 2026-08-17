package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoShareIntoPinlistView extends AutoSharePinlistView
{

	@NotNull private ISharedPinList mSharedPinlist;

	public AutoShareIntoPinlistView(@NotNull ISharedPinList spl, @Nullable IPinList cpl,
			@Nullable chs.cof.logical.schem.IPinList pl, @NotNull ILogicDesign design, @NotNull
			IMessageReporterWithContext reporter, boolean isBulkShare, @NotNull AutoShareParams params)
	{
		super(cpl, pl, design, reporter, isBulkShare, params);
		mSharedPinlist = spl;
	}

	@Override public boolean execute()
	{
		if (mSelectSharedView != null) {
			final boolean shareInto = mSelectSharedView.performShareInto(mSharedPinlist);
			if (!shareInto) {
				cancel();
				return false;
			}
			if (allowReuse()) {
				makePinsReusable();
			}
			if (mMapView != null) {
				mMapView.associateAll();
			}
			return super.execute();
		}
		return false;
	}

	private boolean makePinsReusable()
	{
		if (mReuseView != null) {
			mReuseView.makeAllPinsReusable();
		}
		return true; // For splice
	}

	protected boolean allowReuse()
	{
		return m_params.doMakePinsReusable();
	}

	@Override protected boolean isShareInto()
	{
		return true;
	}
}
