package chs.caplets.shared.actions;

import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

@ApplicationSpecification(includeIn = {Application.ArtisanFunction, Application.ArtisanArchitect})
public class FunctionAutoRouteAction extends AbstractAutoRouteAction
{

	public FunctionAutoRouteAction(IFIB fib)
	{
		super(fib);
	}

	protected void initMenu()
	{
		putValue(NAME, ResourceMgr.getString(FunctionAutoRouteAction.class, "FunctionAutoRouteAction.AutoRouting.Title"));
		putValue(SHORT_DESCRIPTION, getValue(NAME));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(FunctionAutoRouteAction.class, "FunctionAutoRouteAction.AutoRouting.Desc"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK));
	}
}
