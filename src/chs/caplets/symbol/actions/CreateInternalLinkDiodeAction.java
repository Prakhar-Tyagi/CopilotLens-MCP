package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Feb 3, 2010 Time: 8:02:07 PM To change this template use File |
 * Settings | File Templates.
 */
public class CreateInternalLinkDiodeAction extends CreateInternalLinkAction
{

	/**
	 * Constructor for the CreateLineAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateInternalLinkDiodeAction(ICapletController controller)
	{
		super(controller, "Diode");
	}

	public String getActionUIClass()
	{
		return CreateInternalLinkDiodeActionUI.class.getName();
	}
}
