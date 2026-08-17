package chs.caplets.logic.actions.shared;

import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IFunctionMessage;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.ui.MessageHelper;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.helpers.SharedFunctionMessageHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShareFunctionMessageActionHelper extends ShareConductorActionHelper
{

	public ShareFunctionMessageActionHelper(@NotNull ILogicDesign design, @Nullable ISchemDiagram diagram)
	{
		super(design, diagram);
		setShareHelper(new SharedFunctionMessageHelper());
	}

	@Override
	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		if (!(operands.getLogicObject() instanceof IFunctionMessage)) {
			return IActionEnum.eCanceled;
		}
		return super.setup(operands, dialogTitle, diagram);
	}

	@Override protected int handleDuplicateName(@NotNull String name, @NotNull String objectType)
	{
		Message.show(PromptSeverity.INFORMATION, ShareFunctionMessageActionHelper.class,
				"ShareFunctionMessageActionHelper.ShareIntoFailure");
		return MessageHelper.RESULT_ALL;
	}
}
