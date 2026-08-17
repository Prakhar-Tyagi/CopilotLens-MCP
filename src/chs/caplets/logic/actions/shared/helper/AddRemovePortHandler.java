package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListModel;
import java.util.Map;

public class AddRemovePortHandler extends AddRemovePinHandler
{

	public AddRemovePortHandler(
			@NotNull EditSharedPinListModel model,
			@NotNull ILogicDesign design,
			@Nullable Map<String, Integer> pinNameToCountMap,
			ListModel<IPinProxy> proxyListModel)
	{
		super(model, design, pinNameToCountMap, proxyListModel);
	}


	@NotNull protected String getRemoveButtonNotAllowedToRemoveFrozenTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.notAllowedToRemoveFrozen.tooltip";
	}

	@NotNull protected String getRemoveButtonWithLibraryPartTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.withLibraryPart.tooltip";
	}

	@NotNull protected String getRemoveButtonWithSymbolTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.withSymbol.tooltip";
	}

	@NotNull protected String getRemoveButtonDisabeledTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.disabled.tooltip";
	}

	@NotNull protected String getRemoveButtonDisabeledDueToTransientUsageTooltip()
	{
		return "SharedFunctionAddRemoveButtons.removeButton.disabled.mayBeUnplaced.tooltip";
	}

	@NotNull protected String getRenameButtonNameAlreadyExistsErrorText()
	{
		return "SharedFunctionAddRemoveButtons.renameButton.NameExistsError";
	}

}
