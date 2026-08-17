package chs.caplets.logic;

import chs.caf.ActionContainer;
import org.jetbrains.annotations.NotNull;

public interface ISharedObjectToolbarProvider
{

	@NotNull ActionContainer getSharedToolbar();
}
