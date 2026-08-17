package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.helpers.tabulareditor.ObjectFilterMenuItemProvider;
import chs.caf.caplet.helpers.tabulareditor.TablePane;
import chs.caf.caplet.helpers.tabulareditor.TabularEditor;
import chs.caf.caplet.helpers.tabulareditor.TabularEditorDialog;
import chs.caf.caplet.helpers.tabulareditor.TabularEditorPane;
import chs.caf.caplet.helpers.tabulareditor.TabularSelection;
import org.jetbrains.annotations.NotNull;

public class LogicTabularEditorDialog extends TabularEditorDialog
{

	public LogicTabularEditorDialog(@NotNull TabularSelection selectedObjects, TabularEditor tabularEditor)
	{
		super(selectedObjects, tabularEditor);
	}

	@Override protected CreatePaneResult getTabularEditorPane()
	{
		TablePane tablePane = createTablePane();
		ObjectFilterMenuItemProvider provider =
				new ObjectFilterMenuItemProvider(tabularEditor.getTableObjectClass(), tablePane::predicatesUpdated,
						tablePane::validateNumberOfPropertiesOnTablePane);
		if (tablePane.validateNumberOfPropertiesOnTablePane(provider.getPredicate())) {
			tablePane.predicatesUpdated(provider.getPredicate());
			return new CreatePaneResult(new TabularEditorPane(provider, tablePane, true), null);
		}
		else {
			return new CreatePaneResult(null, CreatePaneResult.DisableReason.PROPERTIESEXCEEDSLIMIT);
		}
	}

	@NotNull protected TablePane createTablePane()
	{
		return new LogicTablePane(selectedObjects, tabularEditor, true);
	}
}
