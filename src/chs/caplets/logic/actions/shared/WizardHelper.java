/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 24, 2005 Time: 1:09:56 PM
 */
public class WizardHelper extends ComponentAdapter
{

	private List components;
	private List checker;
	private Component OKButton;
	private JTabbedPane holder;

	public WizardHelper()
	{
		components = new ArrayList();
		checker = new ArrayList();
	}

	public void addComponent(Component c, WizardCheck wc)
	{
		components.add(c);
		checker.add(wc);
		c.addComponentListener(this);
	}

	public void componentShown(ComponentEvent e)
	{
		Component c = e.getComponent();
		int idx = components.indexOf(c);
		WizardCheck wc = (WizardCheck) checker.get(idx);
		wc.wizardEnabled(true);
	}

	public void setHolder(JTabbedPane h)
	{
		holder = h;
	}

	public void setOKButton(Component c)
	{
		OKButton = c;
	}

	public void check()
	{
		boolean hitBadOne = false;
		for (int idx = 0; idx < checker.size(); idx++) {
			WizardCheck wc = (WizardCheck) checker.get(idx);
			if (hitBadOne) {
				Component c = (Component) components.get(idx);
				if (holder.getTabCount() > idx) {
					holder.setEnabledAt(idx, false);
				}
			}
			else {
				if (holder.getTabCount() > idx) {
					holder.setEnabledAt(idx, true);
				}
				if (!wc.allInfoAdded()) {
					hitBadOne = true;
				}
			}
		}
		OKButton.setEnabled(!hitBadOne);
	}
}
