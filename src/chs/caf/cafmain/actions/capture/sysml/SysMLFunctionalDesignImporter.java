/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2025 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.caf.CAFUtils;
import chs.cof.project.IProject;
import chs.common.ILockable;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.utility.DesignsImported;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Imports functional designs into current project
 */
public class SysMLFunctionalDesignImporter
{

	@Nullable private IProject currentProject;
	private final SysMLErrorReporter sysMLErrorReporter;

	public SysMLFunctionalDesignImporter(SysMLErrorReporter sysMLErrorReporter)
	{
		this.sysMLErrorReporter = sysMLErrorReporter;
		currentProject = CAFUtils.getInstance().getCurrentProject();
	}

	@NotNull public List<DesignsImported> importDesignsFromFile(@NotNull String fileName)
	{
		if (currentProject == null) {
			sysMLErrorReporter.logAndShowErrorMessageInOutputTabWithPrompt("project is null");
			return Collections.emptyList();
		}

		List<ILockable> objectsToLock = getObjectsToLock();
		try {
			if (!lockObjects(objectsToLock)) {
				sysMLErrorReporter.logAndShowErrorMessageInOutputTabWithPrompt("Object lock not obtained");
				return Collections.emptyList();
			}

			List<DesignsImported> designsImported = performImportDesigns(fileName);
			return designsImported != null ? designsImported : Collections.emptyList();
		}
		catch (MultipleDesignsImporter.DesignXmlValidationException e) {
			sysMLErrorReporter.logAndShowErrorMessageInOutputTabWithPrompt("XML Validation Error");
		}
		finally {
			unlock(objectsToLock);
		}
		return Collections.emptyList();
	}

	private void unlock(List<ILockable> objectsToLock)
	{
		for (ILockable lockable : objectsToLock) {
			(new LockUpdateHelper(lockable)).unlock();
		}
	}

	@Nullable private List<DesignsImported> performImportDesigns(@NotNull String fileName)
			throws MultipleDesignsImporter.DesignXmlValidationException
	{
		assert currentProject != null;
		MultipleDesignsImporter importer =
				new MultipleDesignsImporter(currentProject, fileName,
						CAFUtils.getInstance().getWindowMgr().getDialogFrame(), sysMLErrorReporter.getReporter());
		List<DesignsImported> designsImported = importer.importDesigns();
		return designsImported;
	}

	protected boolean lockObjects(@NotNull List<ILockable> objectsToLock)
	{
		for (ILockable lockable : objectsToLock) {
			LockUpdateHelper helper = new LockUpdateHelper(lockable);
			if (!helper.lock()) {
				return false;
			}
		}
		return true;
	}

	@NotNull private List<ILockable> getObjectsToLock()
	{
		assert currentProject != null;
		List<ILockable> objectsToLock = new ArrayList<>();
		objectsToLock.add(currentProject.getFolderMgr());
		objectsToLock.add(currentProject.getDesignMgr());
		return objectsToLock;
	}
}
