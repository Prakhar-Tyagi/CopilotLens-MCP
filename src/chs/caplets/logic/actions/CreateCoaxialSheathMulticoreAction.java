package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.SheathTypeAttrVal;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IProjectPreferenceMgr;
import org.jetbrains.annotations.NotNull;

public class CreateCoaxialSheathMulticoreAction extends QuickAddMulticoreAction
{

	public CreateCoaxialSheathMulticoreAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return CreateCoaxialSheathMulticoreActionUI.class.getName();
	}

	@Override public String getSheathType()
	{
		IProjectPreferenceMgr currentProjectPreferences = CAFUtils.getInstance().getCurrentProjectPreferences();
		String defaultShieldIndicator = IndicatorHelper.getDefaultShieldIndicatorType();
		if (currentProjectPreferences != null) {
			String shortcutForCoaxialSheath = currentProjectPreferences.getShortcutForCoaxialSheath();
			return IndicatorHelper.isMulticoreShieldIndicator(shortcutForCoaxialSheath) ? shortcutForCoaxialSheath :
					defaultShieldIndicator;
		}
		return defaultShieldIndicator;
	}

	@Override protected boolean isShieldedIndicator()
	{
		return false;
	}

	@Override protected String getSheathGroupType()
	{
		return SheathTypeAttrVal.SHEATH_TYPE_SHEATH.toString();
	}
}
