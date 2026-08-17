package chs.caplets.logic.actions.shared;

import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 29 Apr, 2013 Time: 12:40:17 PM To change this template use File |
 * Settings | File Templates.
 */
public class ConnectorUnshareHelper extends GenericPinListUnshareHelper
{

	public ConnectorUnshareHelper(IDesign theDesign, @Nullable ISchemDiagram diagram)
	{
		super(theDesign, diagram);
	}

	@Override protected void unshareThisPinList()
	{
		super.unshareThisPinList();
		// If this is a connector, it may have backshells and terminations, handle them.
		unshareBackShells(cablePinList);
	}

	protected Set<ILogicObject> getLockableCableObjects()
	{
		Set<ILogicObject> lockables = new HashSet<>();
		lockables.addAll(super.getLockableCableObjects());
		IBackshell backshell = getBackshell(cablePinList);
		if (backshell != null) {
			lockables.add(backshell);
		}
		return lockables;
	}
}
