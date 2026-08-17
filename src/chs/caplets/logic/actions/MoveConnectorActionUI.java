package chs.caplets.logic.actions;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caplets.shared.actions.MoveConnectorAction;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.SEElectricalDesign})
public class MoveConnectorActionUI extends ActionUI
{

	public MoveConnectorActionUI(@NotNull ICaplet caplet)
	{
		super(caplet);
	}

	@Override public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(MoveConnectorActionUI.class, "MoveConnectorActionUI.action.name"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(MoveConnectorActionUI.class, "MoveConnectorActionUI.action.shortdescription"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(MoveConnectorActionUI.class, "MoveConnectorActionUI.action.longdescription"));
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK);
		putValue(ACCELERATOR_KEY, accel);
	}

	@Override public String getActionClass()
	{
		return MoveConnectorAction.class.getName();
	}
}
