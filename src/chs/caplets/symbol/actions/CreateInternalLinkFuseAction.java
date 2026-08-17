package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: Feb 3, 2010 Time: 5:25:11 PM To change this template use File |
 * Settings | File Templates.
 */
public class CreateInternalLinkFuseAction extends CreateInternalLinkAction
{

	/**
	 * Constructor for the CreateLineAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateInternalLinkFuseAction(ICapletController controller)
	{
		super(controller, "Fusing");
	}

	public String getActionUIClass()
	{
		return CreateInternalLinkFuseActionUI.class.getName();
	}
}
