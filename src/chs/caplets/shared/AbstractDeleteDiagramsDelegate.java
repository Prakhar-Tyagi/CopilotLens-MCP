package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.IUndoableContainer;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.DesignCapletLifecycleHelper;
import chs.caf.caplet.helpers.OpenDiagramBorderUpdater;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.cof.security.FunctionalPermissionEnum;
import chs.common.IUID;
import chs.ctf.deletedesign.DeleteDesignHelper;
import chs.utilities.BuildInfo;
import chs.utilities.IXMLTags;
import chs.utilities.ResourceMgr;
import chs.utility.persist.DataStorageHelper;
import chs.utility.persist.ServerUpdateHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDeleteDiagramsDelegate extends BaseLifecycleDelegate
{

	@NotNull protected ICaplet mCaplet;
	private FunctionalPermissionEnum mEditDesignPermission;

	protected AbstractDeleteDiagramsDelegate(@NotNull ILifeCycleChangeListener lifeCycleListener,
			Class<? extends DesignCapletLifecycleHelper> resourceClass, @NotNull ICaplet caplet,
			FunctionalPermissionEnum editDesignPermission)
	{
		super(lifeCycleListener, resourceClass);
		mCaplet = caplet;
		mEditDesignPermission = editDesignPermission;
	}

	public boolean deleteDiagrams(@NotNull DeleteDesignHelper deleteDesignHelper,
			List<? extends ISchemDiagram> diagramList)
	{
		if (diagramList.isEmpty()) {
			return false;
		}
		IProject project = CAFUtils.getInstance().getFIB().getProjectMgr().getCurrentProject();

		// Delete all diagrams selected before proceeding with deletion of multiple designs
		Map<ILogicDesign, List<ISchemDiagram>> digramDeletionMap =
				new LinkedHashMap<ILogicDesign, List<ISchemDiagram>>();
		for (ISchemDiagram diagram : diagramList) {
			ILogicDesign design = diagram.getDesign();
			if (!digramDeletionMap.containsKey(design)) {
				List<ISchemDiagram> diagramForDesignList = new ArrayList<ISchemDiagram>();
				diagramForDesignList.add(diagram);
				digramDeletionMap.put(design, diagramForDesignList);
			}
			else {
				digramDeletionMap.get(design).add(diagram);
			}
		}
		boolean isDeleted = false;
		for (ILogicDesign designInMap : digramDeletionMap.keySet()) {
			List<ISchemDiagram> diagramListInMap = digramDeletionMap.get(designInMap);

			isDeleted = deleteDiagrams(deleteDesignHelper, project, diagramListInMap, designInMap);

			if (!isDeleted) {
				deleteDesignHelper.collectErrors(designInMap,
						ResourceMgr.getString(getResourceClass(),
								"BaseLifecycle.cannotDelete.error.diagram.message"));
				return false;
			}
			else {
				List<Object> clearDiagramContext = new ArrayList<Object>();
				for (ISchemDiagram diagram : diagramListInMap) {
					clearDiagramContext.add(project);
					clearDiagramContext.add(diagram);
					CAFUtils.getInstance().getFIB().getProjectMgr()
							.projectChildDeleted(project, clearDiagramContext);
				}
			}
		}
		return isDeleted;
	}

	protected abstract boolean deleteDiagrams(DeleteDesignHelper deleteDesignHelper, IProject project,
			List<ISchemDiagram> diagramListInMap, ILogicDesign designInMap);

	private FunctionalPermissionEnum getEditDesignPermission()
	{
		return mEditDesignPermission;
	}

	protected boolean isEditAllowed(IDesign d, String op)
	{

		if (!d.isEditable()) {
			JOptionPane.showMessageDialog(CAFUtils.getInstance().getWindowMgr().getDialogFrame(),
					ResourceMgr.getString(getResourceClass(), "Lifecycle.CannotEdit.Message.text", d.getName()),
					op, JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	protected boolean checkEditDesignPermission()
	{
		final FunctionalPermissionEnum editDesignPermission = getEditDesignPermission();
		if (editDesignPermission == null) {
			return true;
		}

		return checkPermission(editDesignPermission);
	}

	/**
	 * Delete the diagram directly from the database.
	 * <p>
	 *
	 * @param diagram Diagram to delete
	 *
	 * @return true if did delete
	 */
	protected boolean deleteDiagramFromDB(@NotNull ISchemDiagram diagram)
	{
		if (!(BuildInfo.getBuildInfo().areQAExtensionsEnabled() && CAFUtils.getInstance().getData() == null)) {
			String request = DataStorageHelper.getDeleteRequest(IXMLTags.DIAGRAM, diagram.getUID().getString());
			return ServerUpdateHelper.updateServerData(request);
		}
		return true;
	}

	protected void updateOpenDiagramBorder()
	{
		new OpenDiagramBorderUpdater(mCaplet).updateOpenDiagramBorder();
	}

	@Nullable protected IBaseDiagram getActiveDiagram()
	{
		return CAFUtils.getInstance().getActiveDiagram();
	}

	protected void clearDiagram(ISchemDiagram diagram, ILogicDesign design)
	{
		// Cleanup browser tree helper listners on the model
		mLifeCycleListener.removeDiagramFromModel(diagram);

		// Remove the diagram from the design before updating the usages
		design.removeDiagram(diagram);

		// Unload must be after  remove diagram
		diagram.unload();
	}

	protected void cleanUpModel(ISchemDiagram diagram, ILogicDesign design, @Nullable Model model,
			@Nullable IBaseDiagram preDeleteActiveDiagram, @Nullable ISchemDiagram oldCurrentDiagram)
	{
		// Clear the undo queue for the design
		if (model != null) {
			//TODO: 14953 - Check if commented lines are accidently added.
			/*Collection<IUID> emptyList = Collections.emptyList();
			model.notifyModelChange(new ModelChangeEvent(model, emptyList));*/

			IUndoableContainer uc = model.getController().getUndoableContainer();
			if (uc != null) {
				uc.clear();
			}

			// set the diagram of the model if required dts0101158673
			IBaseDiagram baseDiagram = getActiveDiagram();
			if (baseDiagram instanceof ISchemDiagram && design == baseDiagram.getDesignContainer()) {
				model.setCurrentDiagram((ISchemDiagram) baseDiagram);
			}
			else {
				if (model.getDiagram() == null) {
					// see if we have got a valid diagram to set back and is not the diagram we have deleted just now
					if (oldCurrentDiagram != null && oldCurrentDiagram != diagram) {
						model.setCurrentDiagram(oldCurrentDiagram);
					}
				}
			}

			//if there is no change in window we are not getting model change event fired.
			//which causes a stale state of browser tree. trying to fire the same event
			//in this special case.
			if (preDeleteActiveDiagram != null && preDeleteActiveDiagram == baseDiagram) {
				// send a model change event so that the browser tree will
				// be notified that the diagram has been deleted and hence the content in it
				Collection<IUID> emptyList = Collections.emptyList();
				model.notifyModelChange(new ModelChangeEvent(model, emptyList));
			}
		}
	}
}
