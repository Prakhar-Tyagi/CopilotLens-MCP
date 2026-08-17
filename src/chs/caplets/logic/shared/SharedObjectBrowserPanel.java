/*
 * Copyright 2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.AppAction;
import chs.caf.IAppActionMgr;
import chs.caf.cafmain.actions.RefreshSharedObjectsAction;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.browser.IBrowserUpdatableUI;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.logic.actions.shared.FreezeSharedObjectsActionUI;
import chs.cof.logical.IDesign;
import chs.cof.project.IProject;
import chs.ctf.ui.form.CTFLabel;
import chs.utilities.ResourceMgr;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Copyright 2008 Mentor Graphics Corporation. All Rights Reserved. THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR GRAPHICS CORPORATION OR ITS LICENSORS AND IS SUBJECT TO LICENSE TERMS.
 * <p/>
 * Created: Mar 3, 2008 Author: Mohammed El-Morsy.
 */
public class SharedObjectBrowserPanel extends JPanel implements IBrowserUpdatableUI
{

	private ICapletController m_capletController;
	private SharedObjectBrowserClient sharedClient;
	private SharedObjectBrowserTree m_sharedView;
	private JToolBar refreshToolbar;
	private boolean toolbarCreated;
	private JPanel shareCliToolbar;
	private JPanel svjsp;

	public SharedObjectBrowserPanel(IDesign design, ICapletController capletController)
	{
		m_capletController = capletController;
		sharedClient = new SharedObjectBrowserClient(m_capletController);
		m_sharedView = new SharedObjectBrowserTree(sharedClient, "SharedBrowser");

		IProject project = design.getProject();
		if (project != null) {
			project.getSharedConductorMgr().addChangeListener(m_sharedView);
			project.getSharedPinListMgr().addChangeListener(m_sharedView);
		}

		toolbarCreated = false;
	}

	public void initialize()
	{
		if (toolbarCreated) {
			return;
		}
		// Construct the base panel that everything sits on... use the GridBadLayout so everything looks super splendid
		setLayout(new GridBagLayout());

		// First create the toolbar at the top of the tab
		GridBagConstraints gridBagConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 0, 4), 0, 0
		);
		shareCliToolbar = sharedClient.buildToolbar();
		add(shareCliToolbar, gridBagConstraints);

		// Now create and add the refresh thing-a-me-bob
		JLabel refreshLabel = new CTFLabel();
		refreshLabel.setText(
				ResourceMgr.getStringForLabel(SharedObjectBrowserPanel.class, "SharedObjectBrowserPanel.Refresh.text"));
		gridBagConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE,
				GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 0, 4), 0, 0
		);

		// Now the refresh button
		ActionContainer refreshSharedObjectTb = new ActionContainer("LogicRefreshSharedObject");
		if (m_capletController != null
				&& m_capletController.getCaplet() != null
				&& m_capletController.getCaplet().getFIB() != null) {
			//noinspection ConstantConditions
			IAppActionMgr appActionMgr = m_capletController.getCaplet().getFIB().getAppActionMgr();

			AppAction appAction = appActionMgr.getAction(RefreshSharedObjectsAction.class.getName());
			if (appAction != null) {
				refreshSharedObjectTb
						.add(new ActionEntry(appAction));
			}
		}
		refreshToolbar = ResourceHolder.createToolBar((String) refreshSharedObjectTb.getValue(Action.NAME),
				refreshSharedObjectTb.getMembers(), null, m_capletController);
		refreshToolbar.setBorder(null);
		if (refreshToolbar.getComponentCount() > 0){
			add(refreshLabel, gridBagConstraints);
		}
		gridBagConstraints = new GridBagConstraints(GridBagConstraints.RELATIVE,
				GridBagConstraints.REMAINDER, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 0, 5), 0, 0
		);
		add(refreshToolbar, gridBagConstraints);

		// Now add the pre-constructed tree view to the tab
		gridBagConstraints = new GridBagConstraints(0, 1, 4, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 5, 5, 5), 0, 0
		);
		svjsp = m_sharedView.buildContentPanel(null);
		add(svjsp, gridBagConstraints);

		toolbarCreated = true;
	}

	protected void paintComponent(Graphics g)
	{
		m_sharedView.startDisplayOfTree();
		super.paintComponent(g);
	}


	public void updateBrowsableUI()
	{
		ICaplet caplet = m_capletController.getCaplet();
		if (caplet != null) {
			IActionUI action = caplet.getActionUI(FreezeSharedObjectsActionUI.class.getName());
			if (action != null) {
				action.updateUI();
			}
		}
	}

	public SharedObjectBrowserTree getSharedView()
	{
		return m_sharedView;
	}

	public void destroy()
	{
		if (shareCliToolbar != null) {
			shareCliToolbar.removeAll();
		}
		if (svjsp != null) {
			svjsp.removeAll();
		}

		removeAll();
		if (m_sharedView != null) {
			m_sharedView.destroy();
			m_sharedView = null;
			if (refreshToolbar != null) {
				refreshToolbar.removeAll();
				refreshToolbar = null;
			}
		}
	}
}
