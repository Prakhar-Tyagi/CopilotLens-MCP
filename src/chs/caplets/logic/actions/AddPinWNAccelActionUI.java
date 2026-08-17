package chs.caplets.logic.actions;

import chs.caf.caplet.ICaplet;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

public class AddPinWNAccelActionUI extends AddPinActionUI
{

	public AddPinWNAccelActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * /** Description of the Method
	 */
	public void setupUI()
	{
		KeyStroke accel1 = KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.ALT_MASK);

		putValue(ACCELERATOR_KEY, accel1);
	}

	@Nullable public Icon getInactiveIcon()
	{
		return null;
	}

//	/**
//	 * The Id that uniquely identifides this Action
//	 *
//	 * @return The ActionClass value

	//	 */

	public String getActionClass()
	{
		return AddPinWNAccelAction.class.getName();
	}
}
