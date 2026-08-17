/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2025 Siemens
 */

package chs.caplets.logic.actions.ui;

import chs.caf.CAFUtils;
import chs.cof.logical.shared.ISharedObjectModificationObserver;
import chs.cof.logical.cable.ILogicObject;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

/**
 * @author chandras on 09-03-2018.
 */
public class ShareIntoFacetConflictResolutionDialog extends CAFOkCancelDialog
{

	private static final int DIALOG_WIDTH = 800;
	private static final int DIALOG_HEIGHT = 550;
	private static final int DIALOG_MIN_WIDTH = 800;
	private static final int DIALOG_MIN_HEIGHT = 550;
	private IShareIntoFacetConflictResolutionController m_controller;
	private IFacetConflictResolutionModel m_model;
	@NotNull private Optional<ISharedObjectModificationObserver> m_SharedObjectModificationObserver = Optional.empty();

	public ShareIntoFacetConflictResolutionDialog(@NotNull ISharedObjectModificationObserver observer,
												  @NotNull IShareIntoFacetConflictResolutionController controller,
												  @NotNull IFacetConflictResolutionModel model)
	{
		super(CAFUtils.getInstance().getDialogFrame(),
				ResourceMgr.getString(ShareIntoFacetConflictResolutionDialog.class,
						"ShareIntoFacetConflictResolutionDialog.title"), true);
		m_controller = controller;
		m_model = model;
		setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
		setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
		m_SharedObjectModificationObserver = Optional.of(observer);
	}

	public void process(@NotNull ILogicObject target)
	{
		FacetConflictResolutionPanel<ILogicObject> conflictResolutionPanel =
				new FacetConflictResolutionPanel<ILogicObject>(this, m_model);
		m_controller.register(conflictResolutionPanel);

		m_controller.targetChanged(target);

		if (!m_model.getTopNodes().isEmpty()) {
			getContentPane().add(conflictResolutionPanel);
			JButton okButton = getOkButton();
			okButton.setText(ResourceMgr.getString(ShareIntoFacetConflictResolutionDialog.class,
					"ShareIntoFacetConflictResolutionDialog.apply"));
			okButton.setToolTipText(ResourceMgr.getString(ShareIntoFacetConflictResolutionDialog.class,
					"ShareIntoFacetConflictResolutionDialog.apply.tooltip"));
			okButton.addActionListener(new ActionListener()
			{

				public void actionPerformed(ActionEvent e)
				{
					m_model.apply();
					setCancelled(false);
					setVisible(false);
					dispose();
					m_SharedObjectModificationObserver.ifPresent(observer -> observer.setModified());
				}
			});

			JButton cancelButton = getCancelButton();
			cancelButton.setText(ResourceMgr.getString(ShareIntoFacetConflictResolutionDialog.class,
					"ShareIntoFacetConflictResolutionDialog.doNothing"));
			cancelButton.setToolTipText(ResourceMgr.getString(ShareIntoFacetConflictResolutionDialog.class,
					"ShareIntoFacetConflictResolutionDialog.doNothing.tooltip"));
			cancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setCancelled(true);
					setVisible(false);
					dispose();
				}
			});

			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			pack();

			showDialog();
		}
		m_controller.unregister(conflictResolutionPanel);
	}

	protected void showDialog(){
		setVisible(true);
	}
}
