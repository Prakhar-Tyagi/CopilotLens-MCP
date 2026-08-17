/*
 * Copyright 2002-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.creation.DeleteActionDecorationChecker;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.DeleteContext;
import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.helpers.DeletabilityChecker;
import chs.caplets.logic.helpers.IDeletabilityChecker;
import chs.cof.draw.IGfxObject;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.ISingleLineOwned;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.ILogicSegment;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignSharedUsage;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.IDeleteContext;
import chs.system.UIDMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class DeleteAction extends chs.caplets.shared.actions.DeleteAction
{

	public static final String DELIMITER = ",";
	protected boolean deleteConnectivity = true;

	public DeleteAction(ICapletController controller)
	{
		super(controller);
	}

	@Override @NotNull public String getActionUIClass()
	{
		return DeleteActionUI.class.getName();
	}

	protected boolean isDeletable(IUIDObject obj, @NotNull SelectSet sset)
	{
		ILogicModel logicModel = ((ILogicModel) m_model);
		ISchemDiagram currentDiagram = logicModel.getDiagram();
		if (deleteConnectivity && logicModel.getDesign() instanceof ILayoutLogicDesign && !isDeleteableInLayoutDesign(obj)) {
			return false;
		}
		if (!super.isDeletable(obj, sset)) {
			return false;
		}

		if (obj instanceof IMulticore) {
			IDeletabilityChecker deletabilityChecker = new DeletabilityChecker();
			return deletabilityChecker.canDeleteMulticore((IMulticore) obj, sset, currentDiagram);
		}

		return true;
	}

	private boolean isDeleteableInLayoutDesign(IUIDObject obj)
	{
		if(obj instanceof ILogicObject) {
			return obj instanceof ILogicOtherComponent;
		}
		if(obj instanceof IConnectivityRef) {
			return ((IConnectivityRef) obj).getConnectivity() instanceof ILogicOtherComponent;
		}
		return true;
	}

	/**
	 * Overridden here to handle optional delete of connectivity
	 *
	 * @return the result of the delete
	 */
	protected boolean editModel()
	{
		return editModel(deleteConnectivity);
	}

	protected boolean editModel(boolean deleteConn)
	{
		ISchemDiagram currentDiagram = ((ILogicModel) m_model).getDiagram();
		SelectSet toBeDeleted = getSelectionsToBeDeleted();
		SelectSet hiddenDecorations = DeleteActionDecorationChecker.preProcessDeleteSetForText(toBeDeleted);
		DeleteContext deleteContext = DeleteHelper.getInstance().delete(currentDiagram, toBeDeleted, deleteConn);
		handleFailures(deleteContext);
		//cdh.isGoingToDelete is a linear search. and this is causing
		//performance issue. so trying to build a lookup cache.
		Set<IUID> candidateUIDsMarkedForDeletion = new HashSet<>();
		CreationDeletionHelper cdh = CreationDeletionHelper.getTheCreationHelper();
		cdh.processOnDeletionObjects(objectsMarkedForDeletion -> {
			while (objectsMarkedForDeletion.hasNext()) {
				IUIDObject objMarkedForDel = objectsMarkedForDeletion.next();
				candidateUIDsMarkedForDeletion.add(objMarkedForDel.getUID());
			}
			return Void.TYPE;
		});
		SelectedUIDObjectIterator iter = hiddenDecorations.getSelectedUIDObjects();
		while (iter.hasNext()) {
			IUIDObject uidObject = iter.getNext();
			if (uidObject instanceof IAttributeText) {
				Collection<IUID> uids = findObjectsForText((IAttributeText) uidObject);
				boolean objectAddedForDelete = false;
				for (IUID uid : uids) {
					IUIDObject textOwner = UIDMgr.getObject(uid);
					if (!(textOwner instanceof IDiagramObject) ||
							candidateUIDsMarkedForDeletion.contains(textOwner.getUID())) {
						objectAddedForDelete = true;
					}
				}
				if (!objectAddedForDelete) {
					((IGfxObject) uidObject).setMarkedVisible(true);
				}
			}
		}
		return true;
	}

	private void handleFailures(@Nullable DeleteContext deleteContext)
	{
		if (deleteContext == null) {
			return;
		}
		handleAssemblies(deleteContext);
	}

	private void handleAssemblies(@NotNull DeleteContext deleteContext)
	{
		if (!CAFUtils.getInstance().hasWindowMgr() || CAFUtils.getInstance().getOutputWindow()==null) {
			return;
		}

		String paneName = ResourceMgr.getString(DeleteAction.class, "DeleteAction.Outputpane.Name");
		IOutputWindow outputWindow = CAFUtils.getInstance().getOutputWindow();
		if(outputWindow.getPane(paneName)!=null){
			outputWindow.clearPane(paneName);
		}

		List<IAssembly> assemblies = collectAssemblies(deleteContext);
		if (assemblies.isEmpty()) {
			return;
		}
		outputWindow.addComponentPane(paneName);

		for (IAssembly assembly : assemblies) {
			IDeleteContext.IDeletionFailedReason failedReason = deleteContext.getFailedObjects().get(assembly);
			if (failedReason == IDeleteContext.IDeletionFailedReason.HAS_OTHER_USAGES) {
				ILogicDesign design = deleteContext.getDesign();
				Map<String, IUID> diagramToSchemObjectMap = new HashMap<>();
				for (IDesignSharedUsage usage : design.getDesignWideUsageMgr().getUsages(assembly)) {
					diagramToSchemObjectMap.putIfAbsent(usage.getDiagram().getName(), usage.getDiagramObjectUID());
				}

				List<String> diagramNames = new ArrayList<>(diagramToSchemObjectMap.keySet());
				Collections.sort(diagramNames, new AlphaNumComparator<String>(true, true, true));

				StringJoiner sj = new StringJoiner(DELIMITER);
				for (String diagramName : diagramNames) {
					sj.add(HTMLHelper.link(design.getUID(), diagramToSchemObjectMap.get(diagramName), diagramName));
				}
				notifyFailure(assembly, sj.toString());
			}
		}
		CAFUtils.getInstance().getOutputWindow().setActivePane(paneName);
	}

	@NotNull private List<IAssembly> collectAssemblies(@NotNull DeleteContext deleteContext)
	{
		List<IAssembly> assemblies =
				deleteContext.getFailedObjects().keySet().stream().filter(object -> object instanceof IAssembly)
						.map(obj -> (IAssembly) obj).collect(
						Collectors.toList());

		Collections.sort(assemblies, new Comparator<IAssembly>()
		{
			@Override public int compare(IAssembly o1, IAssembly o2)
			{
				return new AlphaNumComparator<String>(true, true, true).compare(o1.getName(), o2.getName());
			}
		});
		return assemblies;
	}

	private void notifyFailure(@NotNull IAssembly assembly, @NotNull String outputMessage)
	{
		if (outputMessage.isEmpty()) {
			return;
		}
		CAFUtils.getInstance().getOutputWindow().sendMessage(ResourceMgr
				.getString(DeleteAction.class, "DeleteAction.Assemmbly.Failure.text", assembly.getName(),
						outputMessage), ResourceMgr.getString(DeleteAction.class, "DeleteAction.Outputpane.Name"), true);
	}


	@NotNull protected SelectSet getSelectionsToBeDeleted()
	{
		return getController().getSelectMgr().getPreSelections();
	}

	@NotNull private Collection<IUID> findObjectsForText(@NotNull IAttributeText text)
	{
		Collection<IUID> diagramObjectsToCheckForLock = new LinkedHashSet<>();

		IDiagramObject parent = text.getParent();
		if (parent != null) {
			diagramObjectsToCheckForLock.add(parent.getUID());
		}
		if (parent instanceof IAbstractSchemPin) {
			IAbstractSchemPin pin = (IAbstractSchemPin) parent;
			if (pin.getParent() != null) {
				diagramObjectsToCheckForLock.add(pin.getParent().getUID());
			}
		}
		else if (parent instanceof ILogicSegment) {
			ILogicSegment logicSegment = (ILogicSegment) parent;
			diagramObjectsToCheckForLock.add(logicSegment.getParent().getUID());
		}
		return diagramObjectsToCheckForLock;
	}

	@Override public void performTaskOnKeyStrokeWhileDisabled()
	{
		SelectSet sset = getPreSelections();
		if(!isDeletable(sset)){
			reportNonDeletableObjectMessage();
		}

	}

	private void reportNonDeletableObjectMessage()
	{
		String message = ResourceMgr
				.getString(DeleteAction.class, "DeleteAction.NonDeleteableObjectInSelection");
		CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(message);
	}
}
