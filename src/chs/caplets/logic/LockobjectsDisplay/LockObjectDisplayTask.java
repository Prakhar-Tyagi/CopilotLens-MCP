package chs.caplets.logic.LockobjectsDisplay;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.IWindowMgr;
import chs.caf.caplet.IActionable;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.ViewChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.helpers.browser.LockedTreeNodeDimmer;
import chs.capitalmanager.appserver.IUserSession;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IGfxView;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.IOtherUserLockedObjectsListener;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUID;
import chs.system.FactoryMgr;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LockObjectDisplayTask implements IDisplayContextListener
{

	private ScheduledExecutorService service;

	private static long frequencyOfExecutionInMillis = 5000;

	private static LockObjectDisplayTask instance = null;
	private Map<IDesign, LockObjectDisplayDataForDesign> designData =
			new HashMap<IDesign, LockObjectDisplayDataForDesign>();

	protected LockObjectDisplayTask()
	{

	}

	public static LockObjectDisplayTask getInstance()
	{
		if (instance == null) {
			instance = new LockObjectDisplayTask();
			IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
			if (userSession != null) {

				FactoryMgr.getDrawFactory().setGfxDimmerable(true);
				IWindowMgr windowManager = CAFUtils.getInstance().getWindowMgr();
				if (windowManager != null) {
					windowManager.addDisplayContextListener(instance);
				}
			}
		}
		return instance;
	}

	public void addDesign(@NotNull LockedTreeNodeDimmer treeNodeDimmer, @NotNull ILogicDesign design)
	{
		if (design instanceof IPrivilegedCOGManagedLockableChildrenContainer) {
			LockObjectDisplayDataForDesign lockObjectDisplayDataForDesign =
					new LockObjectDisplayDataForDesign(treeNodeDimmer,
							((IPrivilegedCOGManagedLockableChildrenContainer) design).getLockingListener(),
							getTaskSceduler());
			designData.put(design, lockObjectDisplayDataForDesign);
		}
	}

	@Override public void windowChanged(WindowChangeEvent wce)
	{
		reset();
		IDesign design = getDesignToWorkOn(wce);
		IBaseDiagram diagram = getDiagramToWorkOn(wce);

		if (design != null && diagram != null && diagram.isEditable()) {

			LockObjectDisplayDataForDesign lockObjectDisplayDataForDesign = designData.get(design);
			if (lockObjectDisplayDataForDesign != null) {
				lockObjectDisplayDataForDesign.setDiagram(diagram);
				createService(design);
			}
		}
	}

	public void createService(IDesign design)
	{
		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleWithFixedDelay(
				new FindLockedLogicObjects(design.getUID(),
						constructLockObjectHandler(design)),
				0,
				frequencyOfExecutionInMillis,
				TimeUnit.MILLISECONDS
		);
	}

	public void removeDesign(IDesign design)
	{
		reset();
		LockObjectDisplayDataForDesign dataForDesign = designData.get(design);
		if (dataForDesign != null) {
			dataForDesign.reset();
		}
		designData.remove(design);
	}

	@Override public void postWindowChanged(WindowChangeEvent wce)
	{

	}

	@Override public void viewChanged(ViewChangeEvent vce)
	{

	}

	@Nullable private ILogicDesign getDesignToWorkOn(WindowChangeEvent wce)
	{
		ICAFWindow newWindow = wce.getNewWindow();
		if (newWindow instanceof ICapletWindow && (((ICapletWindow) newWindow).getController() != null)) {
			ICapletModel capletModel = ((ICapletWindow) newWindow).getController().getCapletModel();
			if (capletModel instanceof ILogicModel) {

				return ((ILogicModel) capletModel).getDesign();
			}
		}
		return null;
	}

	@Nullable private IBaseDiagram getDiagramToWorkOn(WindowChangeEvent wce)
	{
		ICAFWindow newWindow = wce.getNewWindow();
		if (newWindow instanceof ICapletWindow) {
			IGfxView view = (IGfxView) ((ICapletWindow) newWindow).getCurrentView();
			//dts0101356456
			if(view != null) {
				ISheet sheet = view.getSheet();
				if (sheet instanceof IBaseDiagram) {
					return (IBaseDiagram) sheet;
				}
			}
		}
		return null;
	}

	private void reset()
	{
		if (service != null) {
			service.shutdownNow();
			service = null;
		}
	}

	protected Consumer<Map<IUID, LockedTreeNodeDimmer.LockDetail>> constructLockObjectHandler(
			@NotNull IDesign design)
	{
		return new Consumer<Map<IUID, LockedTreeNodeDimmer.LockDetail>>()
		{
			@Override public void accept(Map<IUID, LockedTreeNodeDimmer.LockDetail> t)
			{

				LockObjectDisplayDataForDesign lockObjectDisplayDataForDesign = designData.get(design);
				if (lockObjectDisplayDataForDesign != null) {

					lockObjectDisplayDataForDesign.updateCurrentLockDetails(t);
				}
			}
		};
	}

	protected static class RepaintTask implements Runnable
	{

		private LockedTreeNodeDimmer lockedTreeNodeDimmer;

		private Map<IUID, LockedTreeNodeDimmer.LockDetail> lockUIDs;

		private IOtherUserLockedObjectsListener lockListener;

		RepaintTask(Map<IUID, LockedTreeNodeDimmer.LockDetail> lockedUIDs,
				LockedTreeNodeDimmer lockedTreeNodeDimmer, IOtherUserLockedObjectsListener listener)
		{
			lockUIDs = lockedUIDs;
			this.lockedTreeNodeDimmer = lockedTreeNodeDimmer;
			this.lockListener = listener;
		}

		@Override public void run()
		{

			lockedTreeNodeDimmer.setUIDs(lockUIDs);
			lockListener.lockedInOtherSessions(lockUIDs.keySet());
			ICapletController capletController = CAFUtils.getInstance().getActiveCapletController();
			if (capletController != null) {
				IActionable browserTree = capletController.getActionableBrowser("Diagram");
				if (browserTree instanceof Component) {
					((Component) browserTree).repaint();
				}
			}
		}
	}

	private static class LockObjectDisplayDataForDesign
	{

		private LockedTreeNodeDimmer treeNodeDimmer;
		private IBaseDiagram diagram;
		private IOtherUserLockedObjectsListener lockListener;

		private Map<IUID, LockedTreeNodeDimmer.LockDetail> currentlyLockedLogicObjects = null;
		private Consumer<Runnable> taskScheduler;

		LockObjectDisplayDataForDesign(LockedTreeNodeDimmer treeNodeDimmer,
				IOtherUserLockedObjectsListener otherUserLockListener, Consumer<Runnable> taskScheduler)
		{

			this.treeNodeDimmer = treeNodeDimmer;
			lockListener = otherUserLockListener;
			this.taskScheduler = taskScheduler;
		}

		boolean isThereAnyChangeInLockStatus(Collection<IUID> lockedObjects)
		{
			return currentlyLockedLogicObjects == null ||
					!(currentlyLockedLogicObjects.keySet().containsAll(lockedObjects) &&
							lockedObjects.containsAll(currentlyLockedLogicObjects.keySet()));
		}

		void setDiagram(IBaseDiagram diagram)
		{
			this.diagram = diagram;
		}

		Map<IUID, LockedTreeNodeDimmer.LockDetail> getCurrentlyLockedLogicObjects()
		{
			Map<IUID, LockedTreeNodeDimmer.LockDetail> currentValue =
					currentlyLockedLogicObjects == null ? Collections.emptyMap() : currentlyLockedLogicObjects;
			return new HashMap<>(currentValue);
		}

		void updateCurrentLockDetails(Map<IUID, LockedTreeNodeDimmer.LockDetail> lockedObjects)
		{
			if (isThereAnyChangeInLockStatus(lockedObjects.keySet())) {
				currentlyLockedLogicObjects = new HashMap<IUID, LockedTreeNodeDimmer.LockDetail>(lockedObjects);

				RepaintTask repaintTask =
						new RepaintTask(getCurrentlyLockedLogicObjects(), treeNodeDimmer, lockListener);
				LockObjectVisitorTask lockObjectVisitorTask =
						new LockObjectVisitorTask(getCurrentlyLockedLogicObjects().keySet(), diagram);
				taskScheduler.accept(lockObjectVisitorTask);
				taskScheduler.accept(repaintTask);
			}
		}

		void reset()
		{
			lockListener.lockedInOtherSessions(Collections.emptySet());
		}
	}

	protected Consumer<Runnable> getTaskSceduler()
	{
		return new Consumer<Runnable>()
		{

			@Override public void accept(Runnable runnable)
			{
				try {
					SwingUtilities.invokeAndWait(runnable);
				}
				catch (InterruptedException ignored) {

				}
				catch (InvocationTargetException ignored) {

				}
			}
		};
	}
}