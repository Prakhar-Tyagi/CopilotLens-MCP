package chs.caplets.logic.actions;

import chs.caf.IFIB;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.cafmain.actions.icd.task.SaveDesignAsICDTask;
import chs.caf.cafmain.actions.task.AppTaskAction;
import chs.images.CHSImageLoader;
import chs.task.AbstractNonDeterministicFEMTask;
import chs.task.IFEMTask;
import chs.task.annotations.TaskInfo;
import chs.utilities.Environment;
import chs.utilities.WrappingRuntimeException;
import chs.utility.helpers.LogHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner})
public class ICDImportTaskSubmitAction extends AppTaskAction
{

	public ICDImportTaskSubmitAction(IFIB fib)
	{
		super(fib, TaskInfo.XSDICDImportTask);
		putValue(NAME, "Import ICDs from Logic designs");
		putValue(SHORT_DESCRIPTION, "Import ICDs from Logic designs in the background");
		putValue(LONG_DESCRIPTION, "Import ICDs from Logic designs in the background");
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif"));
		putValue(MNEMONIC_KEY, (int) 'X');
	}

	@Override protected boolean isLocal()
	{
		return true;
	}

	@Override protected boolean runWithCurrentApp()
	{
		return false;
	}

	@Override protected boolean prepareTaskInput(ActionEvent ae)
	{
		return true;
	}

	@SuppressWarnings("OverlyBroadCatchBlock") @Override protected IFEMTask getTask()
	{
		AbstractNonDeterministicFEMTask task = new SaveDesignAsICDTask();
		String inputFilePath = getInputXMLFilePath();
		try (FileInputStream fis = getInputStream(inputFilePath)) {
			task.getParameters().fromXML(fis);
		}
		catch (IOException e) {
			LogHelper.appMsgSafe("FEM input parameter file does not exist. Please add the file at " + inputFilePath);
			LogHelper.appMsgSafe(
					"For more details about input file, please check confluence page https://ies-iesd-conf.ies.mentorg.com/pages/viewpage.action?pageId=129173804");
			throw new WrappingRuntimeException(e);
		}
		return task;
	}

	@NotNull protected String getInputXMLFilePath()
	{
		return String.join(File.separator, Environment.getRoot(),
				"doc", "webservice", "data", "SaveLogicDesignsAsICD.xml");
	}

	@NotNull private FileInputStream getInputStream(String inputFilePath) throws FileNotFoundException
	{
		return new FileInputStream(inputFilePath);
	}

	@Override public void updateUI()
	{

	}
}
