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

/**
 * Created by IntelliJ IDEA. User: momostafa Date: Jul 30, 2009 Time: 3:44:39 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class AddInternalPinActionUI extends ActionUI implements ISelectListener
{

	public AddInternalPinActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_internalpin.png");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_N);
		KeyStroke accel = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, 0);

		putValue(NAME, ResourceMgr.getString(AddInternalPinActionUI.class, "AddInternalPinActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(AddInternalPinActionUI.class, "AddInternalPinActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(AddInternalPinActionUI.class, "AddInternalPinActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(ACCELERATOR_KEY, accel);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_internalpin_disabled.png");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return AddInternalPinAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}
}
