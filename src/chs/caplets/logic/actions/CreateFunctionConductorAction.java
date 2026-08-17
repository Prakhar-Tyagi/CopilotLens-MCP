package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IFunctionConductor;
import chs.images.CHSImages;
import org.jetbrains.annotations.NotNull;

public class CreateFunctionConductorAction extends CreateFunctionBaseConductorAction
{


	/**
	 * Constructor for the CreateConductorAction object
	 *
	 * @param controller Description of the Parameter
	 */
	public CreateFunctionConductorAction(ICapletController controller)
	{
		super(controller);
	}

	public String getActionUIClass()
	{
		return CreateFunctionConductorActionUI.class.getName();
	}

	@NotNull protected Class<IFunctionConductor> getConductorType()
	{
		return IFunctionConductor.class;
	}

	@NotNull protected String getCursorImage()
	{
		return CHSImages.FUNCTIONCODUCTOR_ADD_CURSOR;
	}

}


