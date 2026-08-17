package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.naming.INameMgr;
import chs.ctf.caf.utils.PinProxy;
import chs.ctf.caf.utils.PortProxy;
import chs.utilities.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PortMappingHandler extends PinMappingHandler
{

	public PortMappingHandler(
			@NotNull EditSharedPinListModel model,
			@NotNull ILogicDesign design)
	{
		super(model, design);
	}

	@NotNull @Override protected PinProxy createPinProxy(String name)
	{
		return new PortProxy(name, true);
	}

	@NotNull @Override protected String getPinPrefix()
	{
		final IProject project = getDesign().getProject();
		return project != null ? project.getNameMgr().getObjectPrefix(INameMgr.FUNCTIONPIN).getString() :
				StringUtils.EMPTY_STRING;
	}
}
