package chs.caplets.symbol.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.PropertiesActionUI;

/**
 * Created by IntelliJ IDEA. User: momostafa Date: Aug 5, 2009 Time: 8:57:02 AM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalSymbolDesigner, Application.CapitalSymbolForCapture, Application.CapitalEssentialsSymbolDesigner,
				Application.XSCSymbol, Application.SEElectricalSymbol})

public class SymbolPropertiesActionUI extends PropertiesActionUI
{

	public SymbolPropertiesActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Return our matching ActionRT class
	 */
	public String getActionClass()
	{
		return SymbolPropertiesAction.class.getName();
	}
}
