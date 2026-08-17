package chs.caplets.logic.merge;

import chs.cof.helpers.SegmentContainerHelper;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 19-Mar-2010 Time: 16:37:39
 */
public abstract class SegmentContainerMerger extends Merger
{

	protected SegmentContainerMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
	}

	@Override protected void postSchematicMerge(IConnectivityRef schemSourceObject)
	{
		super.postSchematicMerge(schemSourceObject);
	}

	@Override
	protected void postMergingComplete()
	{
		processSchematicsFor(getTargetLogicObject(), new ISchematicProcessor()
		{

			/**
			 * Finds all representations for this logic object, and updates their port graphics.<br>
			 * And unloads any diagrams that were loaded by this process
			 *
			 * @param schemObject To regenerate chevrons for its representations
			 */
			public void process(IConnectivityRef schemObject)
			{
				SegmentContainerHelper.updatePortGfx(schemObject);
			}
		});
		super.postMergingComplete();
	}
}
