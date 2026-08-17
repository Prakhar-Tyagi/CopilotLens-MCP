/*
 * Copyright 2015 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.common.IHarnessAttributeProvider;
import chs.common.INamedObject;
import chs.common.IUID;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.helpers.SharedConductorGroupHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class HarnessAttributeUpdater
{

	private void syncMulticore(@NotNull IMulticore rootMC)
	{
		if (rootMC.getParent() != null) {
			return;
		}

		final Set<IHarnessAttributeProvider> innerCores = new HashSet<IHarnessAttributeProvider>();
		innerCores.addAll(rootMC.getAllConductorsInHierarchy(true));
		innerCores.addAll(rootMC.getAllMulticoresInHierarchy());

		updateHarness(rootMC, innerCores);
	}

	private void syncMulticore(ISharedMulticore rootMC)
	{
		if (rootMC.getParent() != null) {
			return;
		}

		final Set<IHarnessAttributeProvider> innerCores = new HashSet<IHarnessAttributeProvider>();
		for (IUID uid : SharedConductorGroupHelper.findAllDependents(rootMC)) {
			IHarnessAttributeProvider harnessProvider = UIDMgr.getObjectOfType(uid, IHarnessAttributeProvider.class);
			if (harnessProvider != null) {
				innerCores.add(harnessProvider);
			}
		}

		updateHarness(rootMC, innerCores);
	}

	private void updateHarness(IHarnessAttributeProvider rootAttributeProvider,
			Set<IHarnessAttributeProvider> innerCores)
	{
		final String oldHarnessAttr = rootAttributeProvider.getHarness();

		Map<INamedObject, String> oldHarnessAttribs = new HashMap<INamedObject, String>(1);
		if (rootAttributeProvider instanceof INamedObject) {
			oldHarnessAttribs.put((INamedObject)rootAttributeProvider, oldHarnessAttr);
		}

		String newHarnessAttrib = null;
		boolean bUpdateHarness = false;
		if (StringUtils.isBlank(oldHarnessAttr)) {
			for (IHarnessAttributeProvider innerCore : innerCores) {
				final String harness = innerCore.getHarness();
				if (innerCore instanceof INamedObject) {
					oldHarnessAttribs.put((INamedObject) innerCore, trimAttrib(harness));
				}
				if (!StringUtils.isBlank(harness)) {
					if (StringUtils.isBlank(newHarnessAttrib)) {
						bUpdateHarness = true;
						newHarnessAttrib = harness;
					}
					else if (!harness.equalsIgnoreCase(newHarnessAttrib)) {
						bUpdateHarness = true;
						newHarnessAttrib = "";
					}
				}
			}
		}
		else {
			for (IHarnessAttributeProvider innerCore : innerCores) {
				final String harness = innerCore.getHarness();
				if (innerCore instanceof INamedObject) {
					oldHarnessAttribs.put((INamedObject) innerCore, trimAttrib(harness));
				}
				if (StringUtils.isBlank(harness) || !harness.equalsIgnoreCase(oldHarnessAttr)) {
					bUpdateHarness = true;
				}
			}
			newHarnessAttrib = oldHarnessAttr;
		}

		if (bUpdateHarness) {
			final String newHarness = trimAttrib(newHarnessAttrib);
			showMessages(oldHarnessAttribs, newHarness);
			rootAttributeProvider.setHarness(newHarness);
		}
	}

	protected void showMessages(Map<INamedObject, String> oldHarnessAttribs, String newHarness)
	{
		for (Map.Entry<INamedObject, String> entry : oldHarnessAttribs.entrySet()) {
			String outputMsg = null;
			final String oldHarness = trimAttrib(entry.getValue());
			final String objectLink = HTMLHelper.link(entry.getKey());
			if (oldHarness.isEmpty()) {
				if (!newHarness.isEmpty()) {
					outputMsg = getMessageString("HarnessAttributeUpdater.attribute.added", objectLink, newHarness);
				}
			}
			else {
				if (newHarness.isEmpty()) {
					outputMsg = getMessageString("HarnessAttributeUpdater.attribute.removed", objectLink, oldHarness);
				}
				else if (!oldHarness.equalsIgnoreCase(newHarness)) {
					outputMsg = getMessageString("HarnessAttributeUpdater.attribute.modified", objectLink, oldHarness,
							newHarness);
				}
			}

			if (outputMsg != null) {
				CAFUtils.getInstance().getOutputWindow().sendApplicationMessage(outputMsg);
			}
		}
	}

	private String getMessageString(String resourceKey, Object... values)
	{
		return ResourceMgr.getString(HarnessAttributeUpdater.class, resourceKey, values);
	}

	private String trimAttrib(@Nullable String harnessAttr)
	{
		return StringUtils.trimWhitespace(harnessAttr, StringUtils.TrimStyle.ALL);
	}

	private void syncUnsharedMulticores(Set<IMulticore> multicores)
	{
		for (IMulticore multicore : multicores) {
			syncMulticore(multicore);
		}
	}

	private void syncSharedMulticores(Set<ISharedMulticore> sharedMulticores)
	{
		for (ISharedMulticore multicore : sharedMulticores) {
			syncMulticore(multicore);
		}
	}

	public void syncMulticores(Set<IMulticore> unsharedRootMulticores, Set<ISharedMulticore> sharedRootMulticores)
	{
		syncUnsharedMulticores(unsharedRootMulticores);
		syncSharedMulticores(sharedRootMulticores);
	}
}