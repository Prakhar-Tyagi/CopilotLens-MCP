package chs.caplets.shared;

import chs.caf.caplet.ICapletLifecycle;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.utility.IUserAccessNotifier;
import chs.utility.task.ITask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public abstract class EditLifecycleDelegate extends BaseLifecycleDelegate
{
	@NotNull private final FunctionalPermissionEnum mEditDesignPermission;

	protected EditLifecycleDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, @NotNull FunctionalPermissionEnum editDesignPermission)
	{
		super(lifeCycleListener, resourceClass);
		mEditDesignPermission = editDesignPermission;
	}

	private FunctionalPermissionEnum getEditDesignPermission()
	{
		return mEditDesignPermission;
	}

    protected boolean isAllowedToEdit(ILogicDesign design, IUserAccessNotifier notifier,
            @NotNull Consumer<IUserAccessNotifier> domainErrorMsgDisplayer)
    {
        //FEAT00011755 -check if the design domain is available for this user
        if (!DesignCapletLifecycleHelper.ValidateDesignAccess(design, false, notifier)) {
            domainErrorMsgDisplayer.accept(notifier);
            return false;
        }
        //dts0101079840
        final FunctionalPermissionEnum editDesignPermission = getEditDesignPermission();
        return checkPermission(editDesignPermission);
    }

	protected void saveDesignAndWaitUntilComplete(IProject project, ILogicDesign design,
			@Nullable List<IBaseDiagram> restrictDiagrams, boolean runDRCs)
	{
		ITask saveTask = ((ICapletLifecycle) mLifeCycleListener).save(project, design, restrictDiagrams, true, runDRCs);

		if (saveTask != null) {
			// Wait until the save is finished.
			saveTask.getTaskWorker().get();
		}
	}
}
