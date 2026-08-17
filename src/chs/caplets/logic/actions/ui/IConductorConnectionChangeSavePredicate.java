package chs.caplets.logic.actions.ui;

import chs.cof.logical.ILogicDesign;
import chs.common.IDesignDescriptor;

import java.util.Collection;

public interface IConductorConnectionChangeSavePredicate
{

	boolean shouldSaveForeignDesigns();

	boolean isCurrentDesign(IDesignDescriptor designDescriptor);

	Collection<ILogicDesign> getOpenedDesignsToBeSaved();

	void doPostSave();
}
