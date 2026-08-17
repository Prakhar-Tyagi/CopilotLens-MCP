package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.SheathTypeAttrVal;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IProjectPreferenceMgr;
import org.jetbrains.annotations.NotNull;

public class CreateTwistedSheathMulticoreAction extends QuickAddMulticoreAction
{

	public CreateTwistedSheathMulticoreAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return CreateTwistedSheathMulticoreActionUI.class.getName();
	}

	@Override public String getSheathType()
	{
		IProjectPreferenceMgr currentProjectPreferences = CAFUtils.getInstance().getCurrentProjectPreferences();
		String defaultTwistIndicator = IndicatorHelper.getDefaultTwistIndicatorType();
		if (currentProjectPreferences != null) {
			String shortcutForTwistedSheath = currentProjectPreferences.getShortcutForTwistedSheath();
			return IndicatorHelper.isTwistIndicator(shortcutForTwistedSheath) ? shortcutForTwistedSheath :
					defaultTwistIndicator;
		}
		return defaultTwistIndicator;
	}

	@Override protected boolean isShieldedIndicator()
	{
		return false;
	}

	@Override protected String getSheathGroupType()
	{
		return SheathTypeAttrVal.SHEATH_TYPE_TWISTED.toString();
	}
}
