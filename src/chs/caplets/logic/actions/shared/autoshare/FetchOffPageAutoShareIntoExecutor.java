package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.BaseShareActionHelper;
import chs.caplets.logic.actions.shared.BaseShareActionOperands;
import chs.caplets.logic.actions.shared.IShareActionHelper;
import chs.caplets.logic.actions.shared.IShareIntoActionHelper;
import chs.caplets.logic.actions.shared.IShareOperandStrategy;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.project.IProject;
import chs.common.INamedUIDObject;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.Pair;
import chs.utility.IMessageContext;
import chs.utility.IMessageReporterWithContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FetchOffPageAutoShareIntoExecutor extends BaseFetchOffPageAutoShareExecutor
{

	@NotNull protected ISharedObject mSharedObject;
	private boolean mIsNewlyCreatedObject;
	@NotNull protected Map<ILogicObject, ISharedObject> m_multicoreHierarchyMap = Collections.emptyMap();

	public FetchOffPageAutoShareIntoExecutor(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull ISharedObject sharedObject,
			@NotNull IMessageReporterWithContext reporter, @NotNull AutoShareParams params)
	{
		super(project, design, diagram, reporter, params);
		mSharedObject = sharedObject;
	}

	public FetchOffPageAutoShareIntoExecutor(@NotNull IProject project, @NotNull ILogicDesign design,
			@Nullable ISchemDiagram diagram, @NotNull ISharedObject sharedObject,
			@NotNull IMessageReporterWithContext reporter, @NotNull AutoShareParams params, boolean isNewlyCreatedObj)
	{
		this(project, design, diagram, sharedObject, reporter, params);
		mIsNewlyCreatedObject = isNewlyCreatedObj;
	}

	@Override public IShareOperandStrategy getShareOperandStrategy()
	{
		return new FetchOffAutoShareIntoOperandStrategy();
	}

	@Nullable @Override
	protected Pair<INamedUIDObject, IShareActionHelper> determineActionHelper(@NotNull BaseShareActionOperands operands)
	{
		final IShareIntoActionHelper pinListHelper =
				new FetchOffPageAutoShareIntoPinlistActionHelper(getProject(), getDesign(), getDiagram(),
						getMessageReporter(), m_params);
		final IShareIntoActionHelper conductorHelper =
				new AutoShareIntoConductorActionHelper(getDesign(), getDiagram(), getMessageReporter(), true, mIsNewlyCreatedObject);
		final IShareIntoActionHelper highwayHelper =
				new AutoShareIntoHighwayActionHelper(getDesign(), getDiagram(), getMessageReporter(), true, mIsNewlyCreatedObject);
		final IShareIntoActionHelper singleLineHelper =
				new AutoShareIntoSingleLineActionHelper(getDesign(), getDiagram(), getMessageReporter(), true, mIsNewlyCreatedObject);
		final IShareIntoActionHelper conductorGroupHelper =
				new AutoShareIntoConductorGroupActionHelper(getDesign(), getMessageReporter(), m_multicoreHierarchyMap,
						true, mIsNewlyCreatedObject);
		final List<IShareIntoActionHelper> shareIntoHelpers =
				CollectionUtils.createListNoNulls(pinListHelper, conductorHelper, highwayHelper, singleLineHelper,
						conductorGroupHelper);

		for (IShareIntoActionHelper shareIntoHelper : shareIntoHelpers) {
			if (shareIntoHelper.acceptSharedObject(mSharedObject)) {
				break;
			}
		}
		return BaseShareActionHelper
				.determineActionHelper(operands, pinListHelper, conductorGroupHelper, conductorHelper, highwayHelper,
						singleLineHelper);
	}

	@Override protected void postSuccessfulShare(@Nullable IUIDObject target, @Nullable ILogicObject logicObject)
	{

	}

	@NotNull @Override
	protected IMessageContext getMessageContext(@Nullable IUIDObject target, @Nullable ILogicObject logicObject)
	{
		return IMessageContext.UndeterminedContext;
	}

	public void setMulticoreHierarchyMap(
			@NotNull Map<ILogicObject, ISharedObject> multicoreHierarchyMap)
	{
		m_multicoreHierarchyMap = multicoreHierarchyMap;
	}

	@Override protected void checkAndNotifyFrozen(@Nullable String sharedObjectName, @Nullable IUIDObject target,
			@Nullable ILogicObject logicObject)
	{

	}
}
