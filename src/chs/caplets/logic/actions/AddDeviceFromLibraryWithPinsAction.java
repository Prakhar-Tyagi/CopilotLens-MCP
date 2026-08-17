package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.parts.partselector.ILibraryPartSelection;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;

/**
 * @author aaluri
 */
public class AddDeviceFromLibraryWithPinsAction extends AbstractAddDeviceFromLibraryPartAction
{

	public AddDeviceFromLibraryWithPinsAction(ICapletController controller)
	{
		super(controller);
	}

	@Override
	public String getActionUIClass()
	{
		return AddDeviceFromLibraryWithPinsActionUI.class.getName();
	}

	@Nullable
	@Override
	protected ILibraryPartSelection getPartSelection()
	{
		return PartBrowserActionHelper.getSelectedBrowserPart();
	}

	protected IActionEnum activateAddWithSymbol(ActionEvent e, ILibraryPartSelection libraryPart)
	{
		AddDeviceWithPinsFromLibrary action = new AddDeviceWithPinsFromLibrary(getController(), libraryPart);
		subAction = action;
		return action.onActivate(e);
	}
}