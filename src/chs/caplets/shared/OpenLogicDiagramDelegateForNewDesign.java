package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OpenLogicDiagramDelegateForNewDesign extends OpenDiagramDelegate
{

	public OpenLogicDiagramDelegateForNewDesign(@NotNull ILifeCycleChangeListener lifeCycleListener,
			ICaplet caplet, Class<? extends DesignCapletLifecycleHelper> resourceClass, String designTagXML,
			boolean updateXRefOnReadOnly, int gridSpace)
	{
		super(lifeCycleListener, caplet, resourceClass, designTagXML, updateXRefOnReadOnly, gridSpace);
	}

	@Override protected boolean processDesignLocking(@NotNull IProject project, @Nullable List<?> context,
			@NotNull ILogicDesign design, ISchemDiagram diagram)
	{
		Boolean doOpenReadOnly = checkDesignLockingForTopoMU(project, context, design, diagram);
		if (doOpenReadOnly != null) {
			return doOpenReadOnly;
		}
		return design.lock();
	}

	@Nullable @Override public Model createNewSchematicModel(@Nullable List<?> context, @NotNull IProject project,
			@NotNull ILogicDesign design, @NotNull ISchemDiagram diagram)
	{
		if (design.isEditable() && !design.isLocked()) {
			if (!processDesignLocking(project, context, design, diagram)) {
				return null;
			}
		}

		CAFUtils.getInstance().getOutputWindow()
				.sendDebugMessage("New Caplet Instance " + mCaplet.getName() + " created", true);
	//	CAFUtils.getInstance().getOutputWindow().createCommentsTab("New Comments tab begin created",true,design.getFullName());

		CAFUtils.getInstance().getScanningLock().obtainScanningLock();
		try {
			return mLifeCycleListener.createModel(project, design, diagram, null);
		}
		finally {
			CAFUtils.getInstance().getScanningLock().releaseScanningLock();
		}
	}

	@Nullable @Override public ICapletModel openDiagram(@Nullable List<?> context, @NotNull IProject project,
			@NotNull ISchemDiagram diagram, boolean showAltBuildListDlg)
	{
		ILogicDesign design = diagram.getDesign();
		assert design != null;

		Model model = createNewSchematicModel(context, project, design, diagram);
		if (model != null) {
			if (!createView(model, project, design, diagram, showAltBuildListDlg)) {
				return null;
			}
		}
		return model;
	}
}
