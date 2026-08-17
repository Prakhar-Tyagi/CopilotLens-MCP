package chs.caplets.logic.actions.shared;

import chs.caf.caplet.action.IActionEnum;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ISingleLine;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.cof.logical.shared.ISharedHighway;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cofUtils.logical.concurrency.ShareConcurrencyHelper;
import chs.common.IUID;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.PortHelper;
import chs.utility.Replicator;
import chs.utility.helpers.PropertyHelper;
import chs.utility.helpers.SingleLineHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author: Balaraju Kadukuntla
 * @Date: Feb 26, 2010 11:35:35 AM
 */
public class UnshareHighwayActionHelper extends UnshareSegmentContainerActionHelper implements IShareActionHelper
{

	protected IHighway m_highway;
	private Collection<IHighwaySchematic> m_highwaySchematics;
	private IUID m_sharedHighwayUid;

	public UnshareHighwayActionHelper(IDesign theDesign)
	{
		super(theDesign);
	}

	@NotNull public IActionEnum setup(@NotNull BaseShareActionOperands operands, @Nullable String dialogTitle,
			@Nullable ISchemDiagram diagram)
	{
		m_highway = operands.getHighway();
		if (m_highway == null) {
			return IActionEnum.eCanceled;
		}

		m_sharedHighwayUid = m_highway.getSharedObjectUID();
		m_highwaySchematics = operands.getHighwayRepresentations();

		if (!promptRenameLocalObject(m_highway)) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

	public static boolean unshareHighways(IHighway highway,
			Collection<IHighwaySchematic> schemHighways, String newName, String shortDesc)
	{
		ILogicDesign design = highway.getLogicDesign();
		assert design != null;

		IDesignWideUsageMgr dwum = design.getDesignWideUsageMgr();
		Replicator replicator = new Replicator(Replicator.COPY);

		// If all usages are getting unshared, then make the connectivity local
		// If not, then we can only cope with single schem selection and we create a new connectivity in that case
		ISharedHighway sharedHighway = highway.getSharedHighway();
		boolean allUnshared = dwum.getDesignSharedUsageCount(highway) == schemHighways.size();

		String failureMsg = ResourceMgr.getString(UnshareHighwayActionHelper.class,
				"UnshareConductorActionHelper.UnshareFailureInMU.Message.text");
		if (!ShareConcurrencyHelper.attemptLockToUnshare(highway, schemHighways, allUnshared, design, failureMsg)) {
			return false;
		}

		if (allUnshared) {
			// we will be using this connectivity for the unshared conductors, just set the shared to null
			highway.setSharedHighway(null);

			if(SingleLineHelper.isSingleLineHighway(highway)){
				removeSharedMulticoreReference(highway, replicator);
			}

			// transfer of properties etc must be done *after* nulling the shared ref
			replicator.replicateCopyableObject(sharedHighway, highway);
		}
		else {
			if (schemHighways.size() > 1) {
				// limitation - we can't handle this case yet
				assert false : "Unshare action should not be enabled for this case";
				return false;
			}
		}

		IHighway unsharedHighway = highway;
		for (IHighwaySchematic schemHighway : schemHighways) {

			if (allUnshared) {
				// Unassign ported m_highway will not replicate the connectivity, since we are re-using it.
				// It will remove the m_design usage
				PortHelper.unassignPortedHighway(schemHighway, false);

				// update all proptexts to refer to any new ones put on the connectivity by the replicator
				PropertyHelper.updatePropertyTexts(schemHighway, highway);
			}
			else {
				// if we're in this branch, it should only be for selection of a single instance
				assert schemHighways.size() == 1;

				// If there are multiple instances of this shared object then we need to leave the connectivity where it
				// is (i.e. refering back to the shared obj) and replicate connectivity so we have a local one.
				// Happily this unassign function does all that for us.  It's wonderful like that.
				PortHelper.unassignPortedHighway(schemHighway, true);
				unsharedHighway = schemHighway.getConnectivity();
				if (SingleLineHelper.isSingleLineSchematic(schemHighway) && SingleLineHelper.isSingleLine(highway)) {
					UnshareSingleLineActionHelper.updateEnds(schemHighway, (ISingleLine) highway);
				}
				assert unsharedHighway != highway;

				// for some unknown reason the replicator doesn't set the name for us
				// if the user chose something else it will be set below
				unsharedHighway.setName(sharedHighway.getName());
			}
		}

		if (newName != null) {
			unsharedHighway.setName(newName);
		}
		if (shortDesc != null) {
			unsharedHighway.setShortDescription(shortDesc);
		}
		return true;
	}

	private static void removeSharedMulticoreReference(@NotNull IHighway highway,@NotNull Replicator replicator)
	{
		ISingleLine singleLine = CommonUtils.cast(highway, ISingleLine.class);
		if (singleLine != null) {
			singleLine.getSingleLineMulticores().stream().forEach(multicore -> {
				ISharedMulticore sharedMulticore = multicore.getSharedMulticore();
				if(sharedMulticore != null){
					//remove shared multicore reference
					multicore.setSharedMulticore(null);

					//copy data from shared multicore to multicore
					replicator.replicateCopyableObject(sharedMulticore, multicore);
				}
			});
		}
	}

	public boolean doEdit()
	{
		return unshareHighways(m_highway, m_highwaySchematics, m_newName, shortDescription);
	}

	public void cleanup()
	{
		m_highway = null;
		m_sharedHighwayUid = null;
		if (m_highwaySchematics != null) {
			m_highwaySchematics.clear();
		}
	}

	public boolean isNewSharedObject()
	{
		return false;
	}

	@Override @Nullable public IUID getSharedObjectUID()
	{
		return m_sharedHighwayUid;
	}

	@Override public boolean isShareInto()
	{
		return false;
	}
}
