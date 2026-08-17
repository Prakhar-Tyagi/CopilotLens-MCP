package chs.caplets.logic.icd;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IMulticore;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IProperty;
import chs.common.IReadOnlyValue;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utility.ICDUtils;
import chs.utility.Replicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class ICDMulticore
{

	private String mName;

	private IConnectivity mConnectivity;

	private ICDUtils.MC_INDICATOR_TYPE mType;

	public static final String SOURCE_CABLE_NAME = "Source Cable Name";

	public ICDMulticore(String name, String type, IConnectivity connectivity)
	{
		mName = name;
		mConnectivity = connectivity;
		mType = ICDUtils.determineIndicatorType(type);
	}

	public String getName()
	{
		return mName;
	}

	@NotNull public IMulticore createMulticore(@Nullable IMulticore childMulticore)
	{
		IMulticore multicore = FactoryMgr.getCablePropertiedFactory().createMulticore(FactoryMgr.createUID());
		String indicatorType = IndicatorHelper.getDefaultShieldIndicatorType();
		if (ICDUtils.MC_INDICATOR_TYPE.TWISTED.equals(mType)) {
			indicatorType = IndicatorHelper.getDefaultTwistIndicatorType();
		}

		ILogicDesign logicDesign = CommonUtils.cast(mConnectivity.getDesign(), ILogicDesign.class);
		assert logicDesign != null;
		mConnectivity.addMulticore(multicore);
		Replicator.ensureShieldBodyOnLogicMulticore(logicDesign, multicore, indicatorType);
		multicore.setSheathType(IndicatorHelper.getSheathTypeForIndicator(multicore.getShieldBody()));

		IReadOnlyValue value = FactoryMgr.getCommonFactory().createdStringValue(getName());
		IProperty property = FactoryMgr.getCommonFactory().constructProperty(SOURCE_CABLE_NAME, value, false, multicore);
		multicore.addProperty(property);

		if (childMulticore != null) {
			multicore.addMulticore(childMulticore);
		}
		return multicore;
	}
}
