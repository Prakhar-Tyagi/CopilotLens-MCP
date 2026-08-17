package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IFunctionMessage;
import chs.images.CHSImages;
import org.jetbrains.annotations.NotNull;

public class CreateFunctionMessageAction extends CreateFunctionBaseConductorAction {



    /**
     * Constructor for the CreateConductorAction object
     *
     * @param controller Description of the Parameter
     */
    public CreateFunctionMessageAction(ICapletController controller) {
        super(controller);
    }

    public String getActionUIClass() {
        return CreateFunctionMessageActionUI.class.getName();
    }

    @NotNull protected Class<IFunctionMessage> getConductorType()
	{
		return IFunctionMessage.class;
	}

	@NotNull
	@Override protected String getCursorImage()
	{
		return CHSImages.FUNCTIONMESSAGE_ADD_CURSOR;
	}
}



