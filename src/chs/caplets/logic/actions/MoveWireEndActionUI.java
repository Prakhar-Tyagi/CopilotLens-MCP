package chs.caplets.logic.actions;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ActionUI;
import chs.caf.caplet.selection.ISelectListener;
import chs.caf.caplet.selection.SelectEvent;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_SHORTCUT_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_MOVE_WIRE_END_ACTION",
		label = "Move End",
		tooltip = "Move End",
		icon = "move_end",
		buttonStyle = "MEDIUM_IMAGE_AND_TEXT")
public class MoveWireEndActionUI extends ActionUI implements ISelectListener
{

	/**
	 * Constructor for the MoveWireEndActionUI object
	 *
	 * @param caplet Description of Parameter
	 */
	public MoveWireEndActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Description of the Method
	 */
	public void setupUI()
	{
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_move_wire_end.png");

		putValue(NAME, ResourceMgr.getString(MoveWireEndActionUI.class, "MoveWireEndActionUI.name.decl"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getString(MoveWireEndActionUI.class, "MoveWireEndActionUI.shortDesc.decl"));
		putValue(LONG_DESCRIPTION,
				ResourceMgr.getString(MoveWireEndActionUI.class, "MoveWireEndActionUI.longDesc.decl"));
		putValue(SMALL_ICON, icon);

		// Add ourselves as a select listener on the AppActionMgr so
		// we can update our UI when selection states change.
		getFIB().getAppActionMgr().addSelectListener(this, true);
	}

	public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon("chs/images/general/ico_move_wire_end_disabled.png");
	}

	public boolean isEnabled()
	{
		if (ActionRT.isDesignUnderConcurrentEdit()) {
			IAction action = getAction();
			if (action != null) {
				action.setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			}
			return false;
		}
		return super.isEnabled();
	}

	/**
	 * The Id that uniquely identifides this Action
	 *
	 * @return The ActionClass value
	 */
	public String getActionClass()
	{
		return MoveWireEndAction.class.getName();
	}

	public void selectionChanged(SelectEvent e)
	{
		boolean bEnable = false;

		setEnabled(bEnable);
	}
}
