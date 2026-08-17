/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caf.cafmain.actions.capture.sysml;

import chs.bridges.adaptors.tcmbse.FunctionDesignInfo;
import chs.caf.CAFUtils;
import chs.cof.logical.IFunctionLogicDesign;
import chs.cof.logical.IFunctionLogicDesignIterator;
import chs.cof.project.IProject;
import chs.common.attr.IAttribute;
import chs.common.IPrivilegedDesignMgr;
import com.mentor.chs.api.IXAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * processes the functional design base id
 */
public class FunctionalDesignBaseIdProcessor
{

	private Map<IFunctionLogicDesign, FunctionDesignInfo> functionDesignMap;

	private IProject project;

	public FunctionalDesignBaseIdProcessor()
	{
		project = getProject();
		functionDesignMap = new HashMap<>();
	}

	@NotNull private IProject getProject()
	{
		IProject currentProject = CAFUtils.getInstance().getCurrentProject();
		assert currentProject != null;
		return currentProject;
	}

	@NotNull protected Map<IFunctionLogicDesign, FunctionDesignInfo> getFunctionalDesignBaseIds()
	{
		IFunctionLogicDesignIterator functionDesigns =
				((IPrivilegedDesignMgr)project.getDesignMgr()).getFunctionDesigns();
		while (functionDesigns.hasNext()) {
			IFunctionLogicDesign functionDesign = functionDesigns.next();
			IAttribute functionDesignName = functionDesign.getAttribute(IXAttributes.Name);
			IAttribute functionDesignRevision = functionDesign.getAttribute(IXAttributes.Revision);
			assert functionDesignName != null;
			assert functionDesignRevision != null;
			FunctionDesignInfo functionDesignInfo = getFunctionDesignInfo(functionDesign);
			functionDesignMap.put(functionDesign, functionDesignInfo);
		}

		return functionDesignMap;
	}

	@NotNull private static FunctionDesignInfo getFunctionDesignInfo(IFunctionLogicDesign functionDesign)
	{
		FunctionDesignInfo functionDesignInfo = new FunctionDesignInfo();
		functionDesignInfo.setName(functionDesign.getName());
		functionDesignInfo.setBaseId(functionDesign.getBaseId().getString());
		functionDesignInfo.setRevisionId(functionDesign.getRevision());
		return functionDesignInfo;
	}
}
