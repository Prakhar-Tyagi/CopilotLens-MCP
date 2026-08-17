package chs.caplets.logic.actions;

import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.ResourceMgr;

import java.util.function.BiConsumer;

public class ManageConnectorChange
{

	private IPinProxy current;
	private IPinProxy changed;

	public ManageConnectorChange(IPinProxy current, IPinProxy changed)
	{
		this.current = current;
		this.changed = changed;
	}

	public void applyIfChanged(BiConsumer<IPinProxy, IPinProxy> handleChange)
	{
		if (current != changed) {
			handleChange.accept(current, changed);
		}
	}

	public String getChange()
	{
		if (current != changed) {
			return ResourceMgr.getString(ManageConnectorChange.class,"ManageConnectorChange.tooltip.change",current.getName(),changed.getName());
		}
		else {
			return null;
		}
	}
}
