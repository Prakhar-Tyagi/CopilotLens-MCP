package chs.caplets.logic.actions.shared;

import chs.caplets.logic.actions.shared.helper.AddRemovePinHandler;
import chs.caplets.logic.actions.shared.helper.AddRemovePortHandler;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.naming.INameMgr;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;
import java.util.Map;

public class SharedFunctionAddRemoveButtons extends SharedPinListAddRemoveButtons
{

	public SharedFunctionAddRemoveButtons(JList<IPinProxy> proxyList,
			ILogicDesign design, int orientation,
			EditSharedPinListModel esplModel,
			@NotNull Map<String, Integer> pinNameToCountMap)
	{
		super(proxyList, design, orientation, esplModel, pinNameToCountMap);
	}


	@Override @NotNull protected AddRemovePinHandler getAddRemovePinHandler(JList<IPinProxy> proxyList, ILogicDesign design,
			EditSharedPinListModel esplModel, @NotNull Map<String, Integer> pinNameToCountMap)
	{
		return new AddRemovePortHandler(esplModel, design, pinNameToCountMap, proxyList.getModel());
	}

	@NotNull @Override protected String getHelpIDName()
	{
		return SharedFunctionAddRemoveButtons.class.getName();
	}

	@NotNull @Override protected String getDialogTitle()
	{
		return "SharedFunctionAddRemoveButtons.AddDialog.title";
	}

	@NotNull @Override protected String getPinListPrefixText()
	{
		return "SharedFunctionAddRemoveButtons.prefix.text";
	}

	@NotNull @Override protected String getPinNameLabelText()
	{
		return "SharedFunctionAddRemoveButtons.fullname.text";
	}

	@NotNull @Override protected String getPinCountText()
	{
		return "SharedFunctionAddRemoveButtons.count.text";
	}

	protected String getPrefixString()
	{
		return mHandler.getProject().getNameMgr().getObjectPrefix(INameMgr.FUNCTIONPIN).getString();
	}

	@NotNull @Override protected String getRemaneButtonTooltipForRenameForFrozenNotAllowed()
	{
		return "SharedFunctionAddRemoveButtons.renameButton.notAllowedToRenameFrozen";
	}

	@NotNull @Override protected String getTooltipForRenameButtonWhenNoObjectSelected()
	{
		return "SharedFunctionAddRemoveButtons.renameButton.notOneSelected";
	}

	@NotNull @Override protected String getTooltipForRenameButton()
	{
		return "SharedFunctionAddRemoveButtons.renameButton.tooltip";
	}

	@NotNull @Override protected String getRenamePinDialogTitle()
	{
		return "SharedFunctionAddRemoveButtons.renameButton.dialogTitle";
	}


	@NotNull @Override protected String getRemoveButtonTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.tooltip";
	}

	@NotNull @Override protected String getAddPinsButtonTooltip()
	{
		return "SharedFunctionAddRemoveButtons.addButton.tooltip";
	}

	@Override public String getName()
	{
		return "SharedFunctionAddRemoveButtons";
	}
}
