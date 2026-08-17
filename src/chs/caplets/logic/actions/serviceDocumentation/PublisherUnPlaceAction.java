package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.caplet.ICapletController;
import org.jetbrains.annotations.NotNull;

/**
 * Delete Graphical Instance action - Delete the selected objects without deleting the connectivity
 */
public class PublisherUnPlaceAction extends PublisherDeleteAction
{

	public PublisherUnPlaceAction(ICapletController controller)
	{
		super(controller);
		deleteConnectivity = false;
	}

	@Override @NotNull public String getActionUIClass()
	{
		return PublisherUnPlaceActionUI.class.getName();
	}
}
