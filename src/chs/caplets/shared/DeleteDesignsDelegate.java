package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cog.ICOGLockable;
import chs.cog.IPersistenceSession;
import chs.cog.PersistenceLockFailureCheckedException;
import chs.ctf.deletedesign.DeleteDesignHelper;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.SharedPinListFilters;
import com.mentor.capital.profiling.Profiler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DeleteDesignsDelegate extends BaseLifecycleDelegate
{

	protected DeleteDesignsDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass)
	{
		super(lifeCycleListener, resourceClass);
	}

	public boolean deleteDesigns(DeleteDesignHelper deleteDesignHelper, List<? extends ILogicDesign> designList)
	{
		if (designList.isEmpty()) {
			return true;
		}
		// IN-2545
		// add profiling hook only for the case where the design will be deleted
		Profiler profiler = startProfiling();
		IProject project = deleteDesignHelper.getProject();
		boolean designsDeleted = deleteDesign(deleteDesignHelper, project, designList);
		if (designsDeleted) {
			for (ILogicDesign design : designList) {
				List<Object> childContext = new ArrayList<Object>();
				childContext.add(project);
				childContext.add(design);
				CAFUtils.getInstance().getAutoRecovery().discardDesignRecoveryInfo(design);
				CAFUtils.getInstance().getFIB().getProjectMgr().projectChildDeleted(project, childContext);
				design.unload();
			}
		}
		stopAndLogProfiler(profiler, "Delete Design :");
		return designsDeleted;
	}

	private boolean deleteDesign(DeleteDesignHelper deleteDesignHelper, IProject project,
			List<? extends ILogicDesign> designList)
	{

		// DR 369094: design deletion can trigger updates to shared pinlists when pins reserved to the design
		// being deleted are unreserved:
		// * We must lock and refresh such pinlists to prevent overwrite of any other users edits.
		// * We guard against another user creating a shared pinlist while we're deletign by locking
		//   ISharedPinListMgr.
		// * We guard against another user adding a reservation on the design while we're deleting it by locking
		//   all shared pinlists, not just those that currently have a pin reserved to the design being deleted.
		//   Note that we do this one at a time which will be slow for projects with a very large number of shared
		//   pinlists. May need to find a way of optimising this.

		//dts0100924756
		Set<ICOGLockable> lockableObjects = new LinkedHashSet<ICOGLockable>();
		IPersistenceSession persistenceSession = UtilsHelper.getPersistenceSession();
		boolean designsDeleted = false;

		try {
			lockableObjects.add(project.getSharedPinListMgr());

			// dts0100924756
			Collection<? extends ISharedPinList> sharedPinLists =  SharedPinListFilters.getAllForDesigns(project, designList);
			lockableObjects.addAll(sharedPinLists);

			try {
				persistenceSession.batchAtomicLock(lockableObjects);
			}
			catch (PersistenceLockFailureCheckedException ignore) {
				deleteDesignHelper.collectErrors(ResourceMgr.getString(BaseLifecycle.class,
						"BaseLifecycle.acquireLock.error.message"));
				return false;
			}
			//
			for (ILogicDesign design : designList) {
				// Close any open diagram windows for the design.
				closeWindowsForDesign(design);

				// Remove the design and all its diagrams from here.
				Model model = getModel(design);
				if (model != null) {
					((BaseLifecycle) mLifeCycleListener).destroyModel(model);
				}
			}
			designsDeleted = deleteDesignHelper.deleteAllDesigns(null, designList, false);
		}

		finally {
			persistenceSession.batchUnlock(lockableObjects);
		}

		return designsDeleted;
	}

	protected void closeWindowsForDesign(ILogicDesign design)
	{
		List<IBaseDiagram> diagrams = design.getBaseDiagrams();

		for (IBaseDiagram diagram : diagrams) {
			((DesignCapletLifecycleHelper) mLifeCycleListener).closeWindowsForDiagram(diagram);
		}
	}
}
