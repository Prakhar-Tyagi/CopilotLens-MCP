package chs.caplets.logic.actions.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelectMgr;
import org.jetbrains.annotations.NotNull;

public class ReplaceSharedCompositeSymbolFunctionAction extends ReplaceSharedCompositeSymbolAction
{

	public ReplaceSharedCompositeSymbolFunctionAction(ICapletController controller,
			ISpecialSelectMgr sharedSelectMgr)
	{
		super(controller, sharedSelectMgr);
	}

	@NotNull @Override protected String getNonSymbolUsageKey()
	{
		return "ReplaceSharedCompositeSymbolFunctionAction.nonSymbolUsage";
	}

	@NotNull @Override protected String getNoValidSharedDeviceFoundKey()
	{
		return "ReplaceSharedCompositeSymbolAction.noValidSharedFunctionsFound.text";
	}

	@NotNull @Override protected String getUnableToAccessSymbolKey()
	{
		return "ReplaceSharedCompositeSymbolAction.unableToAccessFunctionSymbol.text";
	}

	/**
	 * Gets the ActionUIClass attribute of the ReplaceSharedCompositeSymbolAction object
	 *
	 * @return The ActionUIClass value
	 */
	public String getActionUIClass()
	{
		return ReplaceSharedCompositeSymbolFunctionActionUI.class.getName();
	}
}
