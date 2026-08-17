/*
 * Copyright 2004-2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.shared;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.helpers.CAFUndoMgr;
import chs.caf.caplet.helpers.CHSUndoableEdit;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.undo.UndoableEdit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author chandras on 22-12-2016.
 */
public class CAFSharedModularConnectCleaner extends SharedModularConnectCleaner
{

	private Model m_model;
	@NotNull private List<UndoableEdit> editsList;

	public CAFSharedModularConnectCleaner(@NotNull Model model)
	{
		super(model.getDesign());
		m_model = model;
		editsList = Collections.emptyList();
	}

	@Override public void beforeCleanup()
	{
		super.beforeCleanup();
		ICapletController controller = m_model.getController();
		if (controller != null) {
			CAFUndoMgr cum = controller.getUndoableContainer().getUndoManager();
			editsList = CollectionUtils.createListNoNulls(cum.getEdits());
		}
	}

	protected boolean doSyncDesign(@NotNull ILogicDesign design, @NotNull IConnectivity connectivity,
			@NotNull Set<IUID> modifiedObjs, @NotNull Set<ISchemDiagram> changedDiagrams)
	{
		return syncConnectivityFromShared(connectivity, modifiedObjs) || verifyInUndoQueue(modifiedObjs);
	}

	@Override public void afterCleanup()
	{
		editsList.clear();
		super.afterCleanup();
	}

	/**
	 * Determines if the list of undoable edits in the undo container resulted in deleting a logic object which points
	 * to any of the given modified shared pinlists. If so, we may have to clear the undo queue. This is avoid the case
	 * where an undo is performed after refreshing shared pinlists & this may result in cable-shared out-of-sync
	 *
	 * @param modifiedSharedPinlists - List of modified shared pinlists
	 *
	 * @return true if any of these undoable edits involves deleting a logic object which points to any of the modified
	 * shared pinlists TODO FEAT15651 - should we worry about REDO queue?
	 */
	private boolean verifyInUndoQueue(Set<IUID> modifiedSharedPinlists)
	{
		for (UndoableEdit edit : editsList) {
			if (edit instanceof CHSUndoableEdit) {
				for (IUIDObject deletedObject : ((CHSUndoableEdit) edit).getDeletedObjects()) {
					if (deletedObject instanceof IUndoableObject && deletedObject instanceof ILogicObject) {
						IUndoableObject actualObject =
								((CHSUndoableEdit) edit).getObjectFromUndo((IUndoableObject) deletedObject);
						if (actualObject instanceof ILogicObject && modifiedSharedPinlists
								.contains(((ILogicObject) actualObject).getSharedObjectUID())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
