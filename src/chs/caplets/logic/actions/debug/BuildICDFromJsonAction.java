/*
* Copyright 2017-2018 Mentor Graphics Corporation
* All Rights Reserved
*
* THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
* INFORMATION WHICH IS THE PROPERTY OF MENTOR
* GRAPHICS CORPORATION OR ITS LICENSORS AND IS
* SUBJECT TO LICENSE TERMS.
*/

package chs.caplets.logic.actions.debug;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.icd.IICD;
import chs.cof.icd.ISystemICDMgr;
import chs.ctf.caf.ui.CAFOkCancelDialog;
import chs.system.FactoryMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.BasicUIFactory;
import chs.utility.debug.json.importer.icd.ICDJsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pbhawsar on 17-02-2017
 */
public class BuildICDFromJsonAction extends ControllerActionRT implements ICtxMenuProvider
{

	@Nullable private GenerateICDFromJsonDialog mDialog;
	@Nullable private ISystemICDMgr mICDMgr;

	public BuildICDFromJsonAction(@NotNull ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		mICDMgr = FactoryMgr.getSystemFactory().getCHSSystem().getSystemData().getICDMgr();
		return IActionEnum.eCompleted;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		mDialog = createGenerateICDDialog("Generate ICD", true);
		mDialog.setVisible(true);
		return true;
	}

	@NotNull protected GenerateICDFromJsonDialog createGenerateICDDialog(String title, boolean modal)
	{
		return new GenerateICDFromJsonDialog(title, modal);
	}

	@Override public boolean onPostTerminate(boolean onTerminateResult)
	{
		if (mDialog != null) {
			mDialog.dispose();
		}
		mDialog = null;
		return true;
	}

	@Override public String getActionUIClass()
	{
		return BuildICDFromJsonActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		assert actionUI != null;
		container.add(new ActionEntry(actionUI));
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	private void buildICDsFromJson(@NotNull String filePath)
	{
		List<IICD> createdICDsHolder = new ArrayList<>();
		final String importLog = ICDJsonBuilder.buildICDsFromJson(filePath, createdICDsHolder, mICDMgr);
		if (mDialog != null) {
			mDialog.setLog(importLog);
		}
	}

	protected class GenerateICDFromJsonDialog extends CAFOkCancelDialog
	{

		private JTextArea logArea;
		private JFileChooser fileChooser;
		private JButton openJsonFileButton;
		private JButton generateICDsButton;

		protected GenerateICDFromJsonDialog(String title, boolean modal)
		{
			super(CAFUtils.getInstance().getWindowMgr().getDialogFrame(), title, modal);
			initUI();
		}

		private void setLog(@NotNull String log)
		{
			logArea.setText(log);
		}

		@SuppressWarnings({"OverlyLongMethod", "MagicNumber"}) private void initUI()
		{
			getOkButton().addActionListener(e -> setVisible(false));
			getCancelButton().addActionListener(e -> setVisible(false));

			setSize(new Dimension(900, 600));

			fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileNameExtensionFilter(".json files", "json"));

			final JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());
			logArea = new JTextArea();
			logArea.setEditable(false);
			JScrollPane logScrollPane = new JScrollPane(logArea);

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.weightx = 3.0;
			gbc.weighty = 3.0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.ipadx = 4;
			gbc.ipady = 4;
			gbc.insets = new Insets(4, 4, 4, 4);
			mainPanel.add(logScrollPane, gbc);
			openJsonFileButton = BasicUIFactory.getInstance().createSiemensCustomJButton();
			openJsonFileButton.setHorizontalTextPosition(4);
			openJsonFileButton.setText("Open Json file");
			openJsonFileButton.setVerticalAlignment(1);
			openJsonFileButton.setVerticalTextPosition(1);
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.VERTICAL;
			gbc.insets = new Insets(4, 4, 4, 4);
			mainPanel.add(openJsonFileButton, gbc);
			generateICDsButton = BasicUIFactory.getInstance().createSiemensCustomJButton();
			generateICDsButton.setText("Generate ICDs");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(4, 4, 4, 4);
			mainPanel.add(generateICDsButton, gbc);
			JTextArea filePath = new JTextArea();
			filePath.setEditable(false);
			JScrollPane filePathScrollPane = new JScrollPane(filePath);
			filePathScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(4, 4, 4, 4);
			mainPanel.add(filePathScrollPane, gbc);
			final JLabel label1 = new JLabel();
			label1.setText("Log");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.ipadx = 10;
			gbc.ipady = 20;
			gbc.insets = new Insets(0, 30, 0, 0);
			mainPanel.add(label1, gbc);

			openJsonFileButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int returnVal = selectJsonFile(mainPanel);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fileChooser.getSelectedFile();
						if (file.exists()) {
							filePath.setText(file.getAbsolutePath());
						}
					}
				}
			});

			generateICDsButton.addActionListener(new ActionListener()
			{
				@Override public void actionPerformed(ActionEvent e)
				{
					final String jsonFilePath = filePath.getText();
					if (StringUtils.isBlank(jsonFilePath)) {
						return;
					}
					buildICDsFromJson(jsonFilePath);
					generateICDsButton.setEnabled(false);
				}
			});

			filePath.setName("filePath");
			logArea.setName("logArea");
			openJsonFileButton.setName("openJsonFileButton");
			generateICDsButton.setName("generateICDsButton");

			add(mainPanel);
		}

		protected int selectJsonFile(JPanel mainPanel)
		{
			return fileChooser.showOpenDialog(mainPanel);
		}

		protected JFileChooser getFileChooser()
		{
			return fileChooser;
		}

		protected JButton getOpenJsonFileButton()
		{
			return openJsonFileButton;
		}

		protected JButton getGenerateICDsButton()
		{
			return generateICDsButton;
		}

		protected String getLog()
		{
			return logArea.getText();
		}
	}
}
