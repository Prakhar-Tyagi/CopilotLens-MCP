package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Feb 3, 2010 Time: 6:00:48 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})
public class CreateInternalLinkDiodeActionUI extends ActionUI
{

	/**
	 * Constructor for the CreateInternalLinkDiodeActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public CreateInternalLinkDiodeActionUI(ICaplet caplet)

	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_diode_link.gif");
		Integer iMnemonic = new Integer(java.awt.event.KeyEvent.VK_D);
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, ResourceMgr.getString(CreateInternalLinkDiodeActionUI.class,
				"CreateInternalLinkDiodeActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(CreateInternalLinkDiodeActionUI.class,
						"CreateInternalLinkDiodeActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(CreateInternalLinkDiodeActionUI.class,
				"CreateInternalLinkDiodeActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/app/ico_diode_link_disabled.gif");
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return CreateInternalLinkDiodeAction.class.getName();
	}
}
