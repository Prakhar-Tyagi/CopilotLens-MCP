package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Event;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA. User: vkhatri Date: 8 Sep, 2012 Time: 12:43:54 PM To change this template use File |
 * Settings | File Templates.
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture,
				Application.CapitalEssentialsDesign, Application.SvcDoc, Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED
)
@ImmersedAction(actionId = "CAPITAL_SLICE_ACTION",
		label = "Slice",
		tooltip = "Slice objects(Ctrl+Shift+S)",
		icon = "ico_slice_enabled",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class SliceActionUI extends ActionUI implements ISelectListener
{

	public SliceActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public String getActionClass()
	{
		return SliceAction.class.getName();
	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(SliceActionUI.class, "SliceActionUI.putValue.action.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(SliceActionUI.class, "SliceActionUI.putValue.action.text_1"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(SliceActionUI.class, "SliceActionUI.putValue.action.text_2"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_slice_enabled.png"));
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK + Event.SHIFT_MASK));
		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public void selectionChanged(SelectEvent e)
	{
		updateUI();
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_slice_disabled.png");
	}
}
