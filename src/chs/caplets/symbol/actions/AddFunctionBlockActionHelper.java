package chs.caplets.symbol.actions;

import chs.cof.symbol.IBlock;
import chs.utilities.ResourceMgr;

import javax.swing.JLabel;
import java.awt.Font;
import java.util.List;

public class AddFunctionBlockActionHelper extends AddBlockActionHelper
{

	public AddFunctionBlockActionHelper(IBlock m_block)
	{
		super(m_block);
	}


	@Override protected void CreateAndShowDuplicatePinNamesWarningMsg(List<String> duplicatePinNames)
	{
		JLabel actionLabel = new JLabel();
		Font newLabelFont = actionLabel.getFont().deriveFont(Font.ITALIC, actionLabel.getFont().getSize());
		actionLabel.setFont(newLabelFont);
		actionLabel.setText(ResourceMgr
				.getString(AddBlockInstanceAction.class, "AddFunctionBlockActionHelper.DuplicatePinNames.action"));

		String title =
				ResourceMgr.getString(AddBlockInstanceAction.class, "AddBlockActionHelper.DuplicatePinNames.title");
		String heading = ResourceMgr
				.getString(AddBlockInstanceAction.class, "AddFunctionBlockActionHelper.DuplicatePinNames.body");
		String message = ResourceMgr
				.getString(AddBlockInstanceAction.class, "AddFunctionBlockActionHelper.DuplicatePinNames.text",
						m_compositeBlock.getName(), duplicatePinNames.toString());
		showWarningMessage(actionLabel, title, heading, message);
	}
}
