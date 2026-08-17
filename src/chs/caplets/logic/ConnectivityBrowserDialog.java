/*
 * Copyright 2007-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.IWindowMgr;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletWindow;
import chs.caf.caplet.IDisplayContextListener;
import chs.caf.caplet.ViewChangeEvent;
import chs.caf.caplet.WindowChangeEvent;
import chs.caf.caplet.helpers.browser.ConnectivityBrowserTreeHelper;
import chs.utilities.CommonUtils;
import chs.utilities.ui.MessageHelper;
import chs.utility.ui.OkCancelDialog;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ConnectivityBrowserDialog extends OkCancelDialog implements IDisplayContextListener
{

	private static final int MAIN_PANEL_WIDTH = 400;
	private static final int MAIN_PANEL_HEIGHT = 500;

	private JPanel mMainPanel = null;
	private JScrollPane mScrolledTree = null;
	private ConnectivityBrowserTreeHelper mTree = null;

	public ConnectivityBrowserDialog(Frame owner, String title)
	{
		super(owner, title, false);
		if (!buildDialog(true, null)) {
			MessageHelper.showErrorMessage(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), "Error",
					"Make the logical design the active Caplet prior to invoking the Connectivity Browser");
		}
	}

	private boolean buildDialog(boolean fullBuild, ICapletController capletController)
	{
		assert mMainPanel == null && mScrolledTree == null && mTree == null;

		mMainPanel = new JPanel(new BorderLayout());
		mMainPanel.setMinimumSize(new Dimension(MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT));
		mMainPanel.setPreferredSize(new Dimension(MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT));
		getContentPane().add(mMainPanel, FlowLayout.LEFT);

		// We're checking that the Caplet Model is chs.caplets.logic.Model since the tree only supports Logic
		ICapletController activeCapletController =
				(capletController != null) ? capletController : CAFUtils.getInstance().getActiveCapletController();
		if ((activeCapletController != null) &&
				(CommonUtils.cast(activeCapletController.getCapletModel(), Model.class) != null)) {
			final ConnectivityBrowserClient cbClient = new ConnectivityBrowserClient(activeCapletController);
			mTree = new ConnectivityBrowserTreeHelper(activeCapletController.getCapletModel(), cbClient,
					"ConnectivityBrowser");

			JPanel warningPanel = new JPanel(new BorderLayout());
			warningPanel.setBorder(BorderFactory.createTitledBorder(""));
			warningPanel.add(new TextBlock(
					"<font color=\"#FF0000\" face=\"Arial,Helvetica\"><center>Connectivity Browser may cause objects to load on<br>demand which may change Capital behaviour.</center></font>"),
					BorderLayout.CENTER);
			mMainPanel.add(warningPanel, BorderLayout.NORTH);
			mScrolledTree = new JScrollPane(mTree);
			mMainPanel.add(mScrolledTree, BorderLayout.CENTER);
		}
		else {
			JPanel warningPanel = new JPanel(new BorderLayout());
			warningPanel.setBorder(BorderFactory.createTitledBorder(""));
			warningPanel.add(new TextBlock(
					"<font color=\"#FF0000\" face=\"Arial,Helvetica\"><center>Please activate a CLogic or CDesign view!</center></font>"),
					BorderLayout.CENTER);
			warningPanel.setMinimumSize(new Dimension(MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT));
			warningPanel.setPreferredSize(new Dimension(MAIN_PANEL_WIDTH, MAIN_PANEL_HEIGHT));
			mMainPanel.add(warningPanel, BorderLayout.CENTER);
		}

		boolean createdListeningDialog = false;
		if (fullBuild) {
			setResizable(true);
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			ActionListener destroyDialogActionHandler = new ActionListener()
			{
				public void actionPerformed(ActionEvent ae)
				{
					destroyDialog();
				}
			};
			getCancelButton().addActionListener(destroyDialogActionHandler);
			getOkButton().addActionListener(destroyDialogActionHandler);
			IWindowMgr windowManager = CAFUtils.getInstance().getWindowMgr();
			if (windowManager != null) {
				windowManager.addDisplayContextListener(this);
			}
			createdListeningDialog = true;
		}

		pack();
		setVisible(true);

		return createdListeningDialog;
	}

	@SuppressWarnings({"MethodOnlyUsedFromInnerClass"}) private void destroyDialog()
	{
		IWindowMgr windowManager = CAFUtils.getInstance().getWindowMgr();
		if (windowManager != null) {
			windowManager.removeDisplayContextListener(this);
		}
		if (mTree != null) {
			mTree.destroy(); // Stop selection listening
		}
		dispose();
	}

	private void removeTree()
	{
		// Destroy existing Components, we may be 'refreshing' the dialog
		if (mTree != null) {
			mTree.destroy();
			mTree = null;
		}
		if (mMainPanel != null) {
			if (mScrolledTree != null) {
				mMainPanel.remove(mScrolledTree);
				mScrolledTree = null;
			}
			getContentPane().remove(mMainPanel);
			mMainPanel = null;
		}
	}

	public void windowChanged(WindowChangeEvent vce)
	{
		removeTree();
		ICapletWindow capletWindow = CommonUtils.cast(vce.getNewWindow(), ICapletWindow.class);
		if (capletWindow != null) {
			buildDialog(false, capletWindow.getController());
		}
	}

	public void postWindowChanged(WindowChangeEvent wce)
	{
	}

	public void viewChanged(ViewChangeEvent vce)
	{
	}

	private static class TextBlock extends JPanel
	{

		private TextBlock(String bodyText)
		{
			JEditorPane p = new JEditorPane("text/html", "");
			p.setEditorKit(new HTMLEditorKit());
			p.setText("<html> <head></head> <body>" + bodyText + "</body></html>");
			p.setEditable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setLayout(new BorderLayout());
			add(p, BorderLayout.CENTER);
		}
	}
}

