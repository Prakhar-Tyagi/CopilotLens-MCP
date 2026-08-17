package chs.caplets.shared;

import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.cof.project.IProject;
import org.jetbrains.annotations.NotNull;

public class CreateNewLogicDiagramDelegate extends CreateNewDiagramDelegate
{

	public CreateNewLogicDiagramDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass,
			@NotNull ICaplet caplet, String designXMLTag, boolean updateXrefOnReadOnly, int drawGridSpacing)
	{
		super(lifeCycleListener, resourceClass, caplet, designXMLTag, updateXrefOnReadOnly, drawGridSpacing);
	}

	@NotNull @Override protected OpenDiagramDelegate getOpenDiagramDelegate(IProject project)
	{
		return new OpenNewLogicDiagramDelegate(mLifeCycleListener, mCaplet, getResourceClass(), mDesignTagXML,
				mUpdateXRefOnReadOnly, mGridSpace);
	}
}
