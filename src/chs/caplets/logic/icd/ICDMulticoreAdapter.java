package chs.caplets.logic.icd;

import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.utilities.CommonUtils;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ICDMulticoreAdapter
{

	private IMulticore mMulticore;
	private ISharedMulticore mSharedMulticore;

	public ICDMulticoreAdapter(@NotNull IMulticore multicore)
	{
		init();
		mMulticore = multicore;
		mSharedMulticore = mMulticore.getSharedMulticore();
	}

	public ICDMulticoreAdapter(ISharedMulticore sharedMulticore)
	{
		init();
		mSharedMulticore = sharedMulticore;
	}

	private void init()
	{
		mMulticore = null;
		mSharedMulticore = null;
	}

	@NotNull public String getSourceName()
	{
		return getSourceName(getAbstractMulticore());
	}

	@NotNull private String getSourceName(IAbstractMulticore abstractMulticore)
	{
		IPropertiedObject multicore = CommonUtils.cast(abstractMulticore, IPropertiedObject.class);
		if (multicore != null) {
			IProperty sourceProp = multicore.findPropertyByName(ICDMulticore.SOURCE_CABLE_NAME);
			if (sourceProp != null) {
				String sourceName = sourceProp.getAsString();
				if (sourceName != null && !StringUtils.isBlank(sourceName)) {
					return sourceName;
				}
			}
		}
		return StringUtils.EMPTY_STRING;
	}

	private IAbstractMulticore getAbstractMulticore()
	{
		if (mMulticore != null) {
			return mMulticore;
		}
		return mSharedMulticore;
	}

	public IMulticore getMulticore()
	{
		return mMulticore;
	}

	@Nullable public ISharedMulticore getSharedMulticore()
	{
		return mSharedMulticore;
	}

}
