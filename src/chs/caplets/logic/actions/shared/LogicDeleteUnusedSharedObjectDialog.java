/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.ctf.ui.form.AbstractEntryDialogStatusListener;
import chs.ctf.ui.form.AbstractEntryPanel;
import chs.ctf.ui.form.EntryPanelController;
import chs.ctf.ui.form.FormMode;
import chs.ctf.ui.form.ObjectDeletedException;
import chs.ctf.ui.form.shareddeletion.SharedDeletionModel;
import chs.ctf.ui.form.shareddeletion.SharedDeletionTablePanel;
import chs.dataservices.CapitalDataServices;
import chs.utilities.CHS_unwind_error;
import chs.utilities.IBoundaryTransactionMarshaller;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.UtilsHelper;
import com.mentor.capital.profiling.LogKeyProvider;
import com.mentor.capital.profiling.Profiler;
import com.mentor.capital.profiling.ProfilingKeyRegistry;
import com.mentor.capital.profiling.ProfilingService;

import java.awt.BorderLayout;
import java.awt.Frame;
/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */

/**
 * Created by jamesmw User: jamesmw Date: 27-Jun-2007 Time: 11:34:38
 */
public class LogicDeleteUnusedSharedObjectDialog extends AbstractEntryDialogStatusListener
{

	protected SharedDeletionModel model;
	private SharedDeletionTablePanel m_tablepanel;

	public LogicDeleteUnusedSharedObjectDialog(Frame frame, SharedDeletionModel sharedDeleteModel)
	{
		super(frame, FormMode.MODE_READ_WRITE);
		setName("DeleteUnusedSharedObjectsDialog");
		model = sharedDeleteModel;
		setTitle(ResourceMgr.getString(LogicDeleteUnusedSharedObjectDialog.class,
				"LogicDeleteUnusedSharedObjectDialog.title"));
		try {
			// add split pane to content pane
			m_mainPanel.setLayout(new BorderLayout(5, 5));
			model.load(this);
			EntryPanelController sharedDeletionPanelController = new EntryPanelController();
			m_tablepanel = getSharedDeletionTablePanel(frame, sharedDeletionPanelController);
			m_mainPanel.add(m_tablepanel, BorderLayout.CENTER);
			addPanel(m_tablepanel);

			setModel(model);
			addListeners();
		}
		catch (ObjectDeletedException ode) {
			CapitalDataServices.logError("Error detected loading SharedConductorDialog " + ode.getMessage());
			ode.printStackTrace();
		}
	}

	protected SharedDeletionTablePanel getSharedDeletionTablePanel(Frame frame,
			EntryPanelController sharedDeletionPanelController)
	{
		return new SharedDeletionTablePanel(frame, sharedDeletionPanelController, m_formMode, model);
	}

	/**
	 * Executes the delete of the selected shared objects
	 *
	 * @throws CHS_unwind_error
	 */
	protected void saveChanges() throws CHS_unwind_error
	{
		try {
			// save the model state
			model.mark();
			m_tablepanel.entryToModel(AbstractEntryPanel.INITIAL_SELECTION);
			IBoundaryTransactionMarshaller btm = UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller();
			try {
				btm.enterTransactionBoundary(this, IBoundaryTransactionMarshaller.Nesting.MAIN);
				model.save();
			}
			finally {
				UtilsHelper.getCHSSystem().getBoundaryTransactionMarshaller().exitTransactionBoundary(this, true);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new CHS_unwind_error(e);
		}
	}

	public int showDialog()
	{
		Profiler openDialogProfiler =
				ProfilingService.createAndStartProfiler(ProfilingKeyRegistry.GENERAL_PROFILER_KEY);
		pack();
		centerWindow();
		stopAndLogProfiler(openDialogProfiler);
		show();
		return getExitStatus();
	}

	private static void stopAndLogProfiler(Profiler profiler)
	{
		profiler.stop();
		if (profiler.isEnabled()) {
			profiler.log(new LogKeyProvider()
			{
				@Override public String getKey()
				{
					return "Open Delete Unused Shared Objects dialog :";
				}
			});
		}
	}

	/**
	 * Free of the object - if any locks or other resources have been taken then release them.
	 * <p/>
	 * This is usually called immediatley before the window this dialog is disposed of.
	 */
	public void releaseObject() throws ObjectDeletedException
	{
		model.release(this);
		model.reset();
	}
}
