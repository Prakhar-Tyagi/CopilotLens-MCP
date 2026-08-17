package chs.caplets.shared;

import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.validation.ValidationException;
import org.jetbrains.annotations.Nullable;

public interface ILifeCycleChangeListener
{

	boolean projectDeleted(IProject project);

	void designRemotelyDeleted(IProject project, ILogicDesign design);

	void diagramValidationFailed(ValidationException exception, @Nullable ILogicDesign design);

	@Nullable Model findModel(ILogicDesign design);

	Model createModel(IProject project, ILogicDesign design, ISchemDiagram diagram, @Nullable Model model);

	void removeDiagramFromModel(ISchemDiagram diagram);

	void diagramDeleted(@Nullable Model model, ISchemDiagram diagram);

	void designModified(ILogicDesign design, boolean designAlreadyLocked, boolean releaseLevelChanged);
}
