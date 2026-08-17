package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.CapitalSymbolDesigner})
public class AddPortActionUI extends ActionUI implements ISelectListener
{

	public AddPortActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_port_active.gif");
		putValue(NAME, ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(AddPortActionUI.class, "AddPortActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, KeyEvent.VK_T);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, 0));

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_port_inactive.gif");
	}

	@Override public String getActionClass()
	{
		return AddPortAction.class.getName();
	}

	@Override public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
