package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caplets.logic.actions.UnplaceActionUI;
import org.jetbrains.annotations.NotNull;

/**
 * Action UI class for PublisherUnPlaceAction
 */
@ApplicationSpecification(
		includeIn = {Application.SvcDoc})
public class PublisherUnPlaceActionUI extends UnplaceActionUI
{

	public PublisherUnPlaceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	@Override @NotNull public String getActionClass()
	{
		return PublisherUnPlaceAction.class.getName();
	}
}
