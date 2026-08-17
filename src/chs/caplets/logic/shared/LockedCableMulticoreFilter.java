package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.ShareAction;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.images.CHSImageLoader;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class LockedCableMulticoreFilter implements IObjectUIFilterOption
{

	private static final Icon LOCK_ICON = CHSImageLoader.loadImageIcon("chs/images/general/ico_locked_record.gif");

	private final IMulticore m_sourceCableMC;

	public LockedCableMulticoreFilter(@NotNull IMulticore sourceCableMC)
	{
		m_sourceCableMC = sourceCableMC;
	}

	@Nullable @Override public Icon getIcon(@NotNull Object obj)
	{
		return LOCK_ICON;
	}

	@Override public boolean filterIn(@NotNull Object obj)
	{
		return doFilter(obj);
	}

	@NotNull @Override public String getDescription(@NotNull Object obj)
	{
		return ResourceMgr.getString(ShareAction.class, "ShareAction.DisableReason.Lock.text");
	}

	public boolean selected(@NotNull Object obj)
	{
		return doFilter(obj);
	}

	private boolean doFilter(@NotNull Object obj)
	{
		ISharedMulticore sharedMulticore = CommonUtils.cast(obj, ISharedMulticore.class);
		if (sharedMulticore == null) {
			return true;
		}
		ILogicDesign logicDesign = m_sourceCableMC.getLogicDesign();
		if (logicDesign == null) {
			return true;
		}
		IConnectivity connectivity = logicDesign.getConnectivity();
		if (connectivity == null) {
			return true;
		}
		IMulticore multicore = connectivity.findSharedMulticore(sharedMulticore);
		if (multicore == null) {
			return true;
		}
		return !LogicObjectLockFinder.isLogicObjectLockedInOtherSession(multicore);
	}
}
