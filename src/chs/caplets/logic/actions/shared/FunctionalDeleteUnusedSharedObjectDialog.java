package chs.caplets.logic.actions.shared;

import chs.ctf.ui.form.EntryPanelController;
import chs.ctf.ui.form.shareddeletion.FunctionalSharedDeletionTablePanel;
import chs.ctf.ui.form.shareddeletion.SharedDeletionModel;
import chs.ctf.ui.form.shareddeletion.SharedDeletionTablePanel;

import java.awt.Frame;

public class FunctionalDeleteUnusedSharedObjectDialog extends LogicDeleteUnusedSharedObjectDialog
{

	public FunctionalDeleteUnusedSharedObjectDialog(Frame frame, SharedDeletionModel sharedDeleteModel)
	{
		super(frame, sharedDeleteModel);
	}

	protected SharedDeletionTablePanel getSharedDeletionTablePanel(Frame frame,
			EntryPanelController sharedDeletionPanelController)
	{
		return new FunctionalSharedDeletionTablePanel(frame, sharedDeletionPanelController, m_formMode, model);
	}
}
