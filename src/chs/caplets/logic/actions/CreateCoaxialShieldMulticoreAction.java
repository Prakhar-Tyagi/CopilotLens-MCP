package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.SheathTypeAttrVal;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IProjectPreferenceMgr;
import org.jetbrains.annotations.NotNull;

public class CreateCoaxialShieldMulticoreAction extends QuickAddMulticoreAction
{

	public CreateCoaxialShieldMulticoreAction(
			@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getActionUIClass()
	{
		return CreateCoaxialShieldMulticoreActionUI.class.getName();
	}

	@Override public String getSheathType()
	{
		CAFUtils CAFUtilsInstance = CAFUtils.getInstance();
		IProjectPreferenceMgr currentProjectPreferences = CAFUtilsInstance.getCurrentProjectPreferences();
		String defaultShieldIndicator = IndicatorHelper.getDefaultShieldIndicatorType();
		if (currentProjectPreferences != null) {
			String shortcutForCoaxialShield = currentProjectPreferences.getShortcutForCoaxialShield();
			return IndicatorHelper.isMulticoreShieldIndicator(shortcutForCoaxialShield) ? shortcutForCoaxialShield : defaultShieldIndicator;
		}
		return defaultShieldIndicator;
	}

	@Override protected boolean isShieldedIndicator()
	{
		return true;
	}

	@Override protected String getSheathGroupType()
	{
		return SheathTypeAttrVal.SHEATH_TYPE_SHEATH.toString();
	}
}
