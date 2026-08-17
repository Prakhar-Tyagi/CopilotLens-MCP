package chs.caplets.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OpenNewLogicDiagramDelegate extends OpenDiagramDelegate
{

	public OpenNewLogicDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			ICaplet caplet, Class<? extends DesignCapletLifecycleHelper> resourceClass, String designTagXML,
			boolean updateXRefOnReadOnly, int gridSpace)
	{
		super(lifeCycleListener, caplet, resourceClass, designTagXML, updateXRefOnReadOnly, gridSpace);
	}

	@Override @Nullable public Model createNewSchematicModel(@Nullable List<?> context, @NotNull IProject project,
			@NotNull ILogicDesign design, @NotNull ISchemDiagram diagram)
	{
		Model model = mLifeCycleListener.findModel(design);
		return createModel(project, design, diagram, mCaplet, model);
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		return design.isLocked();
	}
}
