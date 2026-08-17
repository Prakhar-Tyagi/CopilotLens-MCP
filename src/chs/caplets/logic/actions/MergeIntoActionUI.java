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
import chs.images.CHSImages;
import chs.utilities.ResourceMgr;

import javax.swing.Icon;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 10-Mar-2010 Time: 13:56:33
 */
@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalEssentialsDesign,
		Application.ArtisanFunction, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
@ImmersedAction(actionId = "CAPITAL_MERGE_INTO_ACTION",
		label = "Merge",
		tooltip = "Merge two objects...",
		icon = "ico_merge_active",
		buttonStyle = "SMALL_IMAGE_AND_TEXT")
public class MergeIntoActionUI extends ActionUI implements ISelectListener
{

	public MergeIntoActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void selectionChanged(SelectEvent e)
	{

	}

	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getStringForMenu(MergeIntoActionUI.class, "MergeIntoAction.name.text"));
		putValue(SHORT_DESCRIPTION,
				ResourceMgr.getStringForMenu(MergeIntoActionUI.class, "MergeIntoAction.sdesc.text"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getStringForMenu(MergeIntoActionUI.class, "MergeIntoAction.ldesc.text"));

		Icon icon = CHSImageLoader.loadImageIcon(CHSImages.MERGE_INTO_ICON);
		putValue(SMALL_ICON, icon);
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

	public String getActionClass()
	{
		return MergeIntoAction.class.getName();
	}

	@Override public Icon getInactiveIcon()
	{
		return CHSImageLoader.loadImageIcon(CHSImages.MERGE_INTO_DISABLED_ICON);
	}
}
