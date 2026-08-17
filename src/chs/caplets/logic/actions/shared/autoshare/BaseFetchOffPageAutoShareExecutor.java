package chs.caplets.logic.actions.shared.autoshare;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseFetchOffPageAutoShareExecutor extends AbstractAutoShareExecutor
{

	protected BaseFetchOffPageAutoShareExecutor(@NotNull IProject project,
			@NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
	}

	@Override protected boolean isBulkShare()
	{
		return true;
	}
}
