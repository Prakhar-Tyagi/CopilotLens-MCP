package chs.caplets.logic.helpers.tabulareditor;

import chs.caf.caplet.helpers.TabularEditActionHelper;
import chs.caf.caplet.helpers.tabulareditor.LogicTabularEditorObjectNameDerivation;
import chs.caf.caplet.helpers.tabulareditor.TabularEditor;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemSector;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * @author chandras on 02-08-2017.
 */
public class LogicTabularEditActionHelper extends TabularEditActionHelper
{

	public LogicTabularEditActionHelper(@NotNull Supplier<TabularEditor> tabularEditorSupplier)
	{
		super(tabularEditorSupplier, new LogicTabularEditorObjectNameDerivation());
	}

	@Override public boolean isEnabled(@NotNull SelectSet selectSet)
	{
		return m_nameDerivation.namedConnectivityObjects(selectSet)
				.filter(selectedObject -> !(selectedObject instanceof ILogicDesign))
				.filter(selectedObject -> !(selectedObject instanceof ISchemSector))
				.count() > 0;
	}
}
