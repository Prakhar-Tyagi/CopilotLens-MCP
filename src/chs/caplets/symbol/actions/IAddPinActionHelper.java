package chs.caplets.symbol.actions;

public interface IAddPinActionHelper
{

	boolean isEnabled();

	String getActionUIClass();

	boolean isValidToPerformAction();

	String getStatusbarText();
}
