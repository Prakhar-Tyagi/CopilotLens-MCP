package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.BaseShareActionHelper;
import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.IShareActionHelper;
import chs.caplets.logic.actions.shared.IShareOperandStrategy;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.project.IProject;
import chs.common.IDesignAbstraction;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import chs.utility.SharedObjectAbstractionMatcher;
import chs.utility.helpers.ReferenceHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FetchOffPageAutoShareExecutor extends BaseFetchOffPageAutoShareExecutor
{

	@NotNull private IShareActionHelper m_pinListHelper;
	@NotNull private IShareActionHelper m_condGroupHelper;
	@NotNull private IShareActionHelper m_conductorHelper;
	@NotNull private IShareActionHelper m_highwayHelper;
	@NotNull private IShareActionHelper m_singleLineHelper;

	@NotNull private IShareActionHelper m_functionMessageHelper;

	public FetchOffPageAutoShareExecutor(@NotNull IProject project,
			@NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram,
			@NotNull IMessageReporterWithContext reporter,
			@NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
		m_pinListHelper = new FetchOffPageAutoSharePinlistActionHelper(project, design, diagram, reporter, params);
		m_condGroupHelper = new AutoShareConductorGroupActionHelper(design, reporter, true);
		m_conductorHelper = new AutoShareConductorActionHelper(design, diagram, reporter, true);
		m_highwayHelper = new AutoShareHighwayActionHelper(design, diagram, reporter, true);
		m_singleLineHelper = new AutoShareSingleLineActionHelper(design, diagram, reporter, true);
		m_functionMessageHelper = new AutoShareFunctionMessageActionHelper(design, diagram, reporter, true);

	}

	@Override protected IShareOperandStrategy getShareOperandStrategy()
	{
		return new FetchOffAutoShareOperandStrategy();
	}

	@Nullable
	protected Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(@NotNull BaseShareActionOperands operands)
	{
		return BaseShareActionHelper
				.determineActionHelper(operands, m_pinListHelper, m_condGroupHelper, m_conductorHelper,
						m_highwayHelper, m_singleLineHelper, m_functionMessageHelper, m_conductorHelper);
	}

	@Override protected void postSuccessfulShare(@Nullable IUIDObject target, @Nullable ILogicObject logicObject)
	{
		final String objectName = logicObject != null ? logicObject.getName() : StringUtils.EMPTY_STRING;
		IDesignAbstraction designAbstraction = logicObject != null ?
				SharedObjectAbstractionMatcher.getDesignAbstraction(logicObject.getSharedObject()) : null;

		final String successMsg;
		if(designAbstraction != null){
			successMsg = ResourceMgr
					.getString(FetchOffPageAutoShareExecutor.class,
							"FetchOffPageAutoShareExecutor.onSuccess.withAbstraction.msg",
							objectName, designAbstraction.getName(), getDesign().getName());
		}
		else {
			successMsg = ResourceMgr
					.getString(FetchOffPageAutoShareExecutor.class, "FetchOffPageAutoShareExecutor.onSuccess.msg",
							objectName, getDesign().getName());
		}
		sendMessage(PromptSeverity.INFORMATION, successMsg, getMessageContext(target, logicObject));
	}

	@NotNull @Override protected List<IUIDObject> extendToShareableObjects(@NotNull IUIDObject uidObjectToShare)
	{
		final ILogicObject logicObject = ReferenceHelper.reduceToLogicObject(uidObjectToShare);
		if (logicObject instanceof IConductor) {
			final IConductor cableConductor = (IConductor) logicObject;
			final IMulticore rootMulticore = cableConductor.getRootMulticore();
			if (rootMulticore != null) {
				return Arrays.asList(rootMulticore);
			}
		}
		else if (logicObject instanceof IMulticore) {
			final IMulticore multicore = (IMulticore) logicObject;
			return Arrays.asList(multicore.getRootMulticore());
		}
		return super.extendToShareableObjects(uidObjectToShare);
	}

	@NotNull @Override
	protected IMessageContext getMessageContext(@Nullable IUIDObject target, @Nullable ILogicObject logicObject)
	{
		final IUIDObject contextObject = target != null ? target : logicObject;
		return contextObject != null ? IMessageContext.createContext(contextObject) :
				IMessageContext.UndeterminedContext;
	}
}
