package chs.caplets.logic.actions.icdbrowser;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.browser.ICDBrowserActionHelper;
import chs.caplets.logic.actions.AbstractAddDeviceFromLibraryPartAction;
import chs.caplets.logic.actions.AddICDWithSymbolAction;
import chs.caplets.logic.actions.AddLibraryPartWithSymbolAction;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import org.jetbrains.annotations.Nullable;

public class AddDeviceFromICDAction extends AbstractAddDeviceFromLibraryPartAction
{

	public AddDeviceFromICDAction(ICapletController controller)
	{
		super(controller);
	}

	@Nullable protected ILibraryPartSelection getPartSelection()
	{
		return ICDBrowserActionHelper.getSelectedBrowserPart();
	}

	public String getActionUIClass()
	{
		return AddDeviceFromICDActionUI.class.getName();
	}

	@Override protected AddLibraryPartWithSymbolAction getAddWithSymbolAction(ILibraryPartSelection libraryPart)
	{
		IICDSelection partSelection = (IICDSelection) getPartSelection();
		assert partSelection != null;
		return new AddICDWithSymbolAction(getController(), libraryPart, partSelection);
	}

	@Nullable @Override protected ILibraryPartSelection pickLibraryPart()
	{
			return getPartSelection();
	}
}
