package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.common.IExtent;
import chs.ctf.caf.utils.IPinProxy;
import chs.services.dynamicgfx.DynamicRingTerminalRotationIndicator;
import chs.services.dynamicgfx.DynamicRotationIndicator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AddParameterizedRingTerminalAction extends AddParameterizedPinListAction
{

	public AddParameterizedRingTerminalAction(ICapletController controller, IPinList pinlist, List<IAbstractPin> pins,
			boolean autogenerate, boolean reference, List<IPinProxy> pinProxies)
	{
		super(controller, pinlist, pins, autogenerate, reference, false, false, pinProxies);
	}

	@Override public void constrainExtent(IExtent constExtent)
	{
		super.constrainExtent(constExtent);
		constrainExtentByMaxPinCount(constExtent, 1);
	}

	@NotNull @Override protected DynamicRotationIndicator createRotationIndicator()
	{
		DynamicRotationIndicator indicator = new DynamicRingTerminalRotationIndicator(getIndicateBothEdges());
		setRotationIndicator(indicator);
		return indicator;
	}

	@Override protected boolean shouldAutoGeneratePins()
	{
		return true;
	}
}
