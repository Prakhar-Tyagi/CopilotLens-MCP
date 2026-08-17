package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.SheathTypeAttrVal;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IProjectPreferenceMgr;
import org.jetbrains.annotations.NotNull;

public class CreateOverbraidwithAccelAction extends QuickAddMulticoreAction
{

	public CreateOverbraidwithAccelAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override public String getSheathType()
	{
		IProjectPreferenceMgr currentProjectPreferences = CAFUtils.getInstance().getCurrentProjectPreferences();
		String defaultOverbraidIndicator = IndicatorHelper.getDefaultOverbraidIndicatorType();
		if (currentProjectPreferences != null) {
			String shortcutForOverbraidSheath = currentProjectPreferences.getShortcutForOverbraidSheath();
			return IndicatorHelper.isOverbraidShieldIndicator(shortcutForOverbraidSheath) ? shortcutForOverbraidSheath :
					defaultOverbraidIndicator;
		}
		return defaultOverbraidIndicator;
	}

	@Override protected boolean isShieldedIndicator()
	{
		return true;
	}

	@Override protected String getSheathGroupType()
	{
		return SheathTypeAttrVal.SHEATH_TYPE_SHEATH.toString();
	}

	@Override public String getActionUIClass()
	{
		return CreateOverbraidSheathActionUI.class.getName();
	}

	@Override public void setEditTypeForContext(CreateMulticoreContext createContext)
	{
		createContext.setEditType(COFTypeEnum.Overbraid);
	}
}
