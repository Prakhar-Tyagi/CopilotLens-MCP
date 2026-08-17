/*
 * Copyright 2006-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic.actions.rules;

import chs.caf.action.immersed.ImmersedAction;
import chs.caf.cafmain.actions.SetAttributesAndPropertiesByRuleActionUI;
import chs.caf.caplet.ICaplet;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.annotations.Application;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;

@ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalCapture, Application.CapitalArchitect,
				Application.SvcDoc, Application.ArtisanFunction})
@ImmersedAction(actionId = "CAPITAL_LOGIC_SET_ATTRIBUTES_AND_PROPERTIES_BY_RULE_ACTION",
		label = "Apply Constraints",
		tooltip = "Apply Constraints",
		icon = "set_attributes_and_properties_by_rule")
public class LogicSetAttributesAndPropertiesByRuleActionUI extends SetAttributesAndPropertiesByRuleActionUI
{

	public LogicSetAttributesAndPropertiesByRuleActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	/**
	 * Overriden here because this action is performed via "Apply Constraints" in Logic.
	 * <p/>
	 * In future, there will be multiple constraints in Logic and we'll need a separate action for "Apply Constraints".
	 * <p/>
	 * For now we can kludge it by just renaming this action, since it is the only constraint to apply for logic in 7.2.
	 */
	public void setupUI()
	{
		putValue(NAME, ResourceMgr.getString(LogicSetAttributesAndPropertiesByRuleAction.class,
				"LogicSetAttributesAndPropertiesByRuleAction.name"));
		putValue(SHORT_DESCRIPTION, ResourceMgr.getString(LogicSetAttributesAndPropertiesByRuleAction.class,
				"LogicSetAttributesAndPropertiesByRuleAction.shortDescription"));
		putValue(LONG_DESCRIPTION, ResourceMgr.getString(LogicSetAttributesAndPropertiesByRuleAction.class,
				"LogicSetAttributesAndPropertiesByRuleAction.longDescription"));
		putValue(MNEMONIC_KEY,
				Integer.valueOf((int) ResourceMgr.getMnemonic(LogicSetAttributesAndPropertiesByRuleAction.class,
						"LogicSetAttributesAndPropertiesByRuleAction.mnemonic")));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/ico_transparent.gif"));
	}

	public String getActionClass()
	{
		return LogicSetAttributesAndPropertiesByRuleAction.class.getName();
	}
}
