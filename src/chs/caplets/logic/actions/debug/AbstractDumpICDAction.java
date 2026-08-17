package chs.caplets.logic.actions.debug;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @author chandras on 06-02-2017.
 */
abstract class AbstractDumpICDAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected AbstractDumpICDAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		assert actionUI != null;
		container.add(new ActionEntry(actionUI));
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	protected void dumpData(String paneName, List<String> dumpData)
	{
		IOutputWindow outputWindow = getOutputWindow();
		outputWindow.clearPane(paneName);
		for (String msg : dumpData) {
			outputWindow.sendMessage(HTMLHelper.convertToHTML(msg), paneName, true);
		}
	}

	protected IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	protected String getTabString(int count)
	{
		StringBuilder tab = new StringBuilder();
		for (int i = 0; i < count; i++) {
			tab.append(".......");
		}
		return tab.toString();
	}
}
