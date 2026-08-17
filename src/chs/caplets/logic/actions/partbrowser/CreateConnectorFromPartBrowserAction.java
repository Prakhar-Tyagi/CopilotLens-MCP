package chs.caplets.logic.actions.partbrowser;

import chs.caf.cafmain.actions.partbrowser.PartBrowserAction;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.parts.ILibraryBaseConnector;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

abstract class CreateConnectorFromPartBrowserAction extends PartBrowserAction
{

	CreateConnectorFromPartBrowserAction(String name, String shortDesc, String longDesc, int mnemonic,
			Icon icon)
	{
		super(name, shortDesc, longDesc, mnemonic, icon);
	}

	public boolean isApplicable(ILibraryObject libObj)
	{
		return (libObj instanceof ILibraryBaseConnector);
	}

	public boolean isEnabled()
	{
		if (super.isEnabled()) {
			ILibraryBaseConnector libraryConnector = getSelectedPart();
			return libraryConnector != null;
		}
		return false;
	}

	@Nullable private ILibraryBaseConnector getSelectedPart()
	{
		ILibraryPartSelection part = getPartSelection();
		if (part != null && part.getSelectedObject() instanceof ILibraryBaseConnector) {
			return (ILibraryBaseConnector) part.getSelectedObject();
		}
		return null;
	}

	@Nullable protected ILibraryPartSelection getPartSelection()
	{
		return PartBrowserActionHelper.getCurrentSelectedBrowserPart();
	}
}
