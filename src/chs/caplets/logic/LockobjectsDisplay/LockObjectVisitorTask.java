package chs.caplets.logic.LockobjectsDisplay;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IWindowMgr;
import chs.cof.drawplus.IBaseDiagram;
import chs.common.IUID;
import chs.utility.helpers.LogicObjectDimmer;

import java.util.Collection;
import java.util.LinkedHashSet;

class LockObjectVisitorTask implements Runnable
{

	private IBaseDiagram diagram;
	private Collection<IUID> lockedObjects;

	LockObjectVisitorTask(Collection<IUID> lockedObjects, IBaseDiagram diagram)
	{
		this.lockedObjects = lockedObjects;
		this.diagram = diagram;
	}

	public void run()
	{

		IBaseDiagram activeDiagram = CAFUtils.getInstance().getActiveDiagram();

		if (diagram == activeDiagram) {

			LogicObjectDimmer.getInstance().setLockedObjects(new LinkedHashSet<>(lockedObjects));
			IWindowMgr windowMgr = CAFUtils.getInstance().getWindowMgr();
			ICAFWindow currentWindow = windowMgr != null ? windowMgr.getCurrentWindow() : null;
			if (currentWindow != null) {
				currentWindow.getContainer().repaint();
			}
		}
	}
}
