/*
 * Copyright 2006-2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.serviceDocumentation;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.DeleteAction;
import chs.caplets.logic.actions.serviceDocumentation.delete.DeletableSelectionsProvider;
import chs.caplets.logic.actions.serviceDocumentation.delete.IDeletableSelectionHelper;
import chs.common.IUID;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PublisherDeleteAction extends DeleteAction
{

	public PublisherDeleteAction(ICapletController controller)
	{
		super(controller);
	}

	@NotNull @Override public String getActionUIClass()
	{
		return DeleteActionUI.class.getName();
	}

	/**
	 * if atleast one of the object in the selection set is 'deletable' as per the definition of publisher, then this
	 * returns true
	 *
	 * @param sset selected set of objects
	 *
	 * @return true if atleast one object in selection is 'deletable'
	 */
	protected boolean isDeletable(@NotNull SelectSet sset)
	{
		SelectSet selectSet = getDeletableSelectSet(sset);
		if (selectSet.isEmpty()) {
			return false;
		}
		return super.isDeletable(selectSet);
	}

//	@Override protected boolean isDeletable(IUIDObject obj, @NotNull SelectSet sset)
//	{
///*		if ((obj instanceof ICompoundDiagramObject)) {
//			return false;
//		}
//		return true;*/
//		if ((obj instanceof IPropertiedGfxGroup) || (obj instanceof ITableGraphic) ||
//				(obj instanceof IPropertiedGraphic) || (obj instanceof ICommentSymbol)
//				|| (obj instanceof IDiagramText)) {
//			return true;
//		}
///*		else if (obj instanceof IDiagramText) {
//			return !(obj instanceof IXRefText);
//		}*/
//		return false;
//	}

	@NotNull protected SelectSet getSelectionsToBeDeleted()
	{
		SelectSet allSelections = super.getSelectionsToBeDeleted();
		Set<IUIDObject> selectionObjects = getSelections(allSelections, true, true);
		SelectSet selectSet = new SelectSet();
		selectionObjects.forEach(o -> selectSet.add(o));
		return selectSet;
	}

	@NotNull private SelectSet getDeletableSelectSet(@NotNull SelectSet sset)
	{
		if (sset.isEmpty()) {
			return new SelectSet();
		}
		SelectSet selectSet = new SelectSet();
		Set<IUIDObject> deletables = getSelections(sset, false, false);
		deletables.forEach(o -> selectSet.add(o));
		return selectSet;
	}

	/**
	 * if all the objects in selection are non-connectivity objects then include all of them in the select set for
	 * delete in publisher
	 * <p>
	 * else if any one of the following statements is true about the selected object, then the object is included in the
	 * select set for delete in publisher
	 * <p>
	 * object is deletable as per logic and non-connectivity object and does not have the parent object part of the
	 * selection
	 * <p>
	 * object is deletable as per logic and the connectivity object and is fetched and not the last instance in the
	 * project and the selection does not contain all the instances of the object then include it in selection. For such
	 * object, also include other child non-connectivity objects part of the original selection in the select set for
	 * publisher delete
	 * <p>
	 * else return empty selection set
	 *
	 * @param sset the selection set from the view
	 * @param checkForConnectivityChanges check if the deletion of object selections can cause connectivity changes,
	 * this is passed as false in the isEnabled flow, because of performance reasons
	 * @param shouldLogMessages whether to log messages
	 *
	 * @return all the objects which are true per the above defined behaviour
	 */
	private Set<IUIDObject> getSelections(@NotNull SelectSet sset, boolean checkForConnectivityChanges,
			boolean shouldLogMessages)
	{
		Set<IUID> allSelections = new HashSet<>(sset.getSelectedUIDS());
		Set<IUIDObject> selectedUIDObjects = allSelections
				.stream()
				.map(IUID::getObject)
				.collect(Collectors.toSet());
		DeletableSelectionsProvider provider =
				getDeletableSelectionsProvider(checkForConnectivityChanges, shouldLogMessages);
		return provider.getSelectionsToDelete(selectedUIDObjects, sset);
	}

	@NotNull private DeletableSelectionsProvider getDeletableSelectionsProvider(boolean checkForConnectivityChanges,
			boolean logMessages)
	{
		return new DeletableSelectionsProvider(checkForConnectivityChanges, logMessages, getDeletableSelectionHelper(),
				getStreamForDebugging());
	}

	@Nullable protected OutputStream getStreamForDebugging()
	{
		return null;
	}

	@NotNull private IDeletableSelectionHelper getDeletableSelectionHelper()
	{
		return new IDeletableSelectionHelper()
		{
			@Override public boolean isDeletableAsPerLogic(IUIDObject obj, @NotNull SelectSet sset)
			{
				return PublisherDeleteAction.super.isDeletable(obj, sset);
			}
		};
	}
}
