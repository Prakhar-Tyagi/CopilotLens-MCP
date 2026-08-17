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

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ModelChangeEvent;
import chs.caplets.logic.Model;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IPrivilegedDesignMgr;
import chs.utilities.CommonUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * @author chandras on 23-12-2016.
 */
public class CAFDesignSharedObjectCleanupAdaptor extends AbstractDesignSharedObjectCleanupAdaptor
{

	@Nullable private Model m_model;

	public CAFDesignSharedObjectCleanupAdaptor(@NotNull Model model, ISharedObjectDesignCleaner... listeners)
	{
		super(listeners);
		m_model = model;
		final ILogicDesign design = model.getDesign();
		IProject project = design.getProject();
		assert project != null;
		IPrivilegedDesignMgr designMgr = CommonUtils.cast(project.getDesignMgr(), IPrivilegedDesignMgr.class);
		assert designMgr != null;
		designMgr.registerCustomSharedObjectChangeSyncHandler(design, this);
	}

	public void destroy()
	{
		m_model = null;
	}

	@Override protected void stateChangeEnded(boolean stateChanged, @NotNull Set<ISchemDiagram> changedDiagrams)
	{
		if (stateChanged) {
			clearUndoQueue();
			notifyModelChange();
		}
		invalidateView(changedDiagrams);
	}

	private void invalidateView(@NotNull Set<ISchemDiagram> changedDiagrams)
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			if (changedDiagrams.contains(CAFUtils.getInstance().getActiveDiagram())) {
				view.invalidate(IViewInvalidationEnum.eFull);
			}
		}
	}

	private void notifyModelChange()
	{
		if (m_model == null) {
			return;
		}
		// Notify model changes to refresh design browser tree structure
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			ICapletModel capletModel = view.getCapletModel();
			if (capletModel == m_model) {
				boolean bModelModified = m_model.isModified();
				capletModel.notifyModelChange(new ModelChangeEvent(capletModel, Collections.emptyList()));
				m_model.setModified(bModelModified);
			}
		}
	}

	private void clearUndoQueue()
	{
		if (m_model != null) {
			m_model.getController().clearUndoQueue();
		}
	}
}
