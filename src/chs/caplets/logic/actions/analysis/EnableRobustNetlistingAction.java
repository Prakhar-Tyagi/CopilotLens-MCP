/*
 * Copyright 2007-2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.IAnalysisNetlistScope;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.analysis.SubsystemBaseAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caplets.logic.analysis.LogicAnalysisServices;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cofUtils.logical.concurrency.LogicConcurrencyHelper;
import chs.common.IDesignContainer;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.logic.ILogicModel;
import com.mentor.capital.ui.IToggleAction;

import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.prefs.Preferences;

/**
 * * Called when the user attempts to cut some object(s) * * The method is undoable, becuase it makes changes to the
 * caplet model
 */
@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class EnableRobustNetlistingAction extends AppAction implements IToggleAction
{

	/**
	 * The icon to be shown when the action is enabled
	 */
	protected Icon enabledIcon;

	/**
	 * The icon to be shown when the action is disabled
	 */
	protected Icon disabledIcon;

	protected boolean isSelected;

	/**
	 * The name of the action when enabled
	 */
	String enabledDesc;

	/**
	 * The name of the action when disabled
	 */
	String disabledDesc;

	/**
	 * THe preferences key for the enabled state
	 */
	private static final String ROBUST_ENABLED_KEY = "RobustEnabled";
	private static final String SYSTEM_PROP_ROBUST_ENABLED = "analysis.robust.netlisting.enabled";

	/**
	 * Construct a new instance of the action using the given FIB
	 *
	 * @param fib, the framework interface broker
	 */
	public EnableRobustNetlistingAction(IFIB fib)
	{
		super(fib);
		disabledDesc = ResourceMgr.getString(EnableRobustNetlistingAction.class,
				"EnableRobustNetlistingAction.String.shortDesc.disabled");
		enabledDesc = ResourceMgr
				.getString(EnableRobustNetlistingAction.class, "EnableRobustNetlistingAction.String.shortDesc.enabled");
		String name =
				ResourceMgr.getString(EnableRobustNetlistingAction.class, "EnableRobustNetlistingAction.String.name");
		String longDesc =
				ResourceMgr
						.getString(EnableRobustNetlistingAction.class, "EnableRobustNetlistingAction.String.longDesc");

		enabledIcon = CHSImageLoader.loadImageIcon("chs/images/app/as_robust_netlisting_enabled.png");
		disabledIcon = CHSImageLoader.loadImageIcon("chs/images/app/as_robust_netlisting_disabled.png");

		Preferences preferences = Preferences.userNodeForPackage(EnableRobustNetlistingAction.class);
		isSelected = preferences.getBoolean(ROBUST_ENABLED_KEY, false);
		System.setProperty(SYSTEM_PROP_ROBUST_ENABLED, String.valueOf(isSelected));

		Integer iMnemonic = KeyEvent.VK_R;
		putValue(MNEMONIC_KEY, iMnemonic);
		putValue(NAME, name);
		putValue(SHORT_DESCRIPTION, isSelected ? enabledDesc : disabledDesc);
		putValue(LONG_DESCRIPTION, longDesc);
		putValue(SMALL_ICON, isSelected ? enabledIcon : disabledIcon);
	}

	/**
	 * * Use the analysis interface to fire off the launcher.
	 */
	public void actionPerformed(ActionEvent e)
	{
		//AbstractButton button = (AbstractButton) ae.getSource();

		if (CAFUtils.getInstance().getActiveActionMgr().getActiveAction() instanceof SubsystemBaseAction) {
			MessageHelper.showInformationMessage(CAFUtils.getInstance().getDialogFrame(),
					ResourceMgr.getString(EnableRobustNetlistingAction.class,
							"EnableRobustNetlistingAction.String.disabled.title"),
					ResourceMgr.getString(EnableRobustNetlistingAction.class,
							"EnableRobustNetlistingAction.String.disabled.message"));
		}
		else {
			isSelected = !isSelected;
			System.setProperty(SYSTEM_PROP_ROBUST_ENABLED, isSelected ? "true" : "false");
			putValue(SMALL_ICON, isSelected ? enabledIcon : disabledIcon);
			putValue(NAME, isSelected ? enabledDesc : disabledDesc);
			putValue(SHORT_DESCRIPTION, isSelected ? enabledDesc : disabledDesc);
			firePropertyChange(SHORT_DESCRIPTION, getValue(SHORT_DESCRIPTION),
					""); // don't care about the old val.
		}
		CAFUtils.getInstance().tickleUI(this.getFIB());
	}

	public void updateUI()
	{
		isSelected = "true".equals(System.getProperty(SYSTEM_PROP_ROBUST_ENABLED, "false"));
		putValue(SMALL_ICON, isSelected ? enabledIcon : disabledIcon);
		putValue(NAME, isSelected ? enabledDesc : disabledDesc);
		putValue(SHORT_DESCRIPTION, isSelected ? enabledDesc : disabledDesc);
		this.firePropertyChange(SHORT_DESCRIPTION, getValue(SHORT_DESCRIPTION), "");
		setEnabled(determineEnabledState());
	}

	public boolean determineEnabledState()
	{
		IDesignContainer design = FactoryMgr.getCAFUtils().getActiveDesignContainer();
		ILogicDesign logicDesign = design instanceof ILogicDesign ? (ILogicDesign) design : null;
		IProject project = logicDesign != null ? logicDesign.getProject() : null;
		if (LogicConcurrencyHelper.isLogicInMultiUserMode(project)) {
			ICapletController activeCapletController = getCAFUtils().getActiveCapletController();
			ICapletModel model = activeCapletController != null ? activeCapletController.getCapletModel() : null;
			if (model instanceof ILogicModel && !model.isEditable()) {
				return false;
			}
		}
		IAnalysisNetlistScope scope = LogicAnalysisServices.getCurrentAnalysisNetlistScope();
		boolean enabledT = true;
		if (scope != null && LogicAnalysisServices.isAnalysisActive(scope.getUid())) {
			enabledT = false;
		}

		return enabledT && AnalysisServices.isActionSupportedInMUMode(logicDesign);
	}

	public boolean isEnabled()
	{
		updateUI(); // ensure we're showing the correct status...

		return determineEnabledState();
	}

	public static void storeRobustEnabledState()
	{
		Preferences preferences = Preferences.userNodeForPackage(EnableRobustNetlistingAction.class);
		preferences.putBoolean(ROBUST_ENABLED_KEY,
				Boolean.parseBoolean(System.getProperty(SYSTEM_PROP_ROBUST_ENABLED, "false")));
	}

	@Override public boolean isOn()
	{
		return isSelected;
	}
}
