package chs.caplets.logic.actions.shared;

import chs.utility.ValidateDuplicateSharedNames;
import chs.caplets.logic.actions.ui.MCNode;
import chs.caplets.logic.actions.ui.MCSharedMatcher;
import chs.caplets.logic.actions.ui.MCSharedNode;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.project.IOptionExpression;
import chs.cof.project.IOptionedObject;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUIDObject;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.property.IProperty;
import chs.utilities.ui.property.IStringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Map;

/**
 * A Controller class to manage SharedMulticoreDialog
 */
public class MulticoreSharedController
{

	private SharedMulticoreModel m_sharedMulticoreModel;
	private IDesign m_design;
	private boolean strictMatch = true;

	public enum AssociationResult
	{

		NO_ERROR, STRUCTURE_NOT_MATCHED, ALREADY_ASSIGNED, PARENTS_NOT_MATCHED
	}

	public static class ResultOfAssociationCheck
	{

		protected AssociationResult m_result = AssociationResult.NO_ERROR;

		ResultOfAssociationCheck()
		{
		}

		protected void setResult(AssociationResult result)
		{
			m_result = result;
		}

		@NotNull public AssociationResult getResult()
		{
			return m_result;
		}
	}

	private static final Map<AssociationResult, String> tooltipOnAssociationButton =
			new EnumMap<AssociationResult, String>(AssociationResult.class);

	static {
		tooltipOnAssociationButton.put(AssociationResult.NO_ERROR, "MulticoreMapPanel.associate.tooltip");
		tooltipOnAssociationButton
				.put(AssociationResult.STRUCTURE_NOT_MATCHED, "MulticoreMapPanel.associate.incompatiblemulticores");
		tooltipOnAssociationButton
				.put(AssociationResult.ALREADY_ASSIGNED, "MulticoreMapPanel.associate.alreadyAssigned");
		tooltipOnAssociationButton
				.put(AssociationResult.PARENTS_NOT_MATCHED, "MulticoreMapPanel.associate.parentsNotMatched");

	}

	public MulticoreSharedController(SharedMulticoreModel sharedMulticoreModel, IDesign design)
	{
		m_sharedMulticoreModel = sharedMulticoreModel;
		m_design = design;
	}

	@Nullable public ISharedMulticore getSharedMulticore()
	{
		return m_sharedMulticoreModel.getSharedMulticore();
	}

	@Nullable public String getSharedMulticoreName()
	{
		return m_sharedMulticoreModel.getSharedMulticoreName();
	}

	@Nullable public String getSharedMulticoreRevision()
	{
		return m_sharedMulticoreModel.getSharedMulticoreRevision();
	}

	public boolean isSharedMulticoreNameGenerated()
	{
		return m_sharedMulticoreModel.isSharedMulticoreNameGenerated();
	}

	public boolean isStrictMatch()
	{
		return strictMatch;
	}

	public void setStricMatch(boolean isStrictMatch)
	{
		strictMatch = isStrictMatch;
	}

	public boolean allowAssociate(ResultOfAssociationCheck result, @Nullable MCNode mcNode,
			@Nullable MCSharedNode sharedMCNode)
	{
		if (mcNode != null && !mcNode.isAssigned() && sharedMCNode != null && !sharedMCNode.isAssigned()) {
			if (isMatched(mcNode, sharedMCNode)) {
				if (canParentBeAssociated(mcNode, sharedMCNode)) {
					result.setResult(AssociationResult.NO_ERROR);
					return true;
				}
				result.setResult(AssociationResult.PARENTS_NOT_MATCHED);
				return false;
			}
			result.setResult(AssociationResult.STRUCTURE_NOT_MATCHED);

			return false;
		}
		result.setResult(AssociationResult.ALREADY_ASSIGNED);
		return false;
	}

	private boolean canParentBeAssociated(@NotNull MCNode mcNode,
			@NotNull MCSharedNode sharedMCNode)
	{
		MCNode parentMcNode = mcNode.getParentMcNode();
		MCSharedNode parentMcSharedNode = sharedMCNode.getParentMcSharedNode();
		boolean isParentsMatched = true;
		// Check entire hierarcy if the parent can be matched
		while (parentMcNode != null && parentMcSharedNode != null && !parentMcNode.isRoot() &&
				!parentMcSharedNode.isRoot()) {
			if (isMatched(parentMcNode, parentMcSharedNode)) {
				parentMcSharedNode = parentMcSharedNode.getParentMcSharedNode();
				parentMcNode = parentMcNode.getParentMcNode();
			}
			else {
				isParentsMatched = false;
				break;
			}
		}
		return isParentsMatched;
	}

	public boolean allowUnassociate(@Nullable MCNode mcNode)
	{
		return (mcNode != null && mcNode.isAssigned());
	}

	@NotNull public String getToolTipTextForAssocButton(AssociationResult result)
	{
		String tooltip = tooltipOnAssociationButton.get(result);
		if (tooltip == null) {
			tooltip = "MulticoreMapPanel.associate.tooltip";
		}
		return ResourceMgr.getString(MulticoreMapPanel.class, tooltip);
	}

	public boolean autoAssignMulticores(MCNode localMulticoreNode, MCSharedNode sharedMulticoreNode)
	{
		MCSharedMatcher mcSharedMatcher = new MCSharedMatcher();
		if (mcSharedMatcher.isMatched(isStrictMatch(), localMulticoreNode, sharedMulticoreNode)) {
			Map<MCNode, MCSharedNode> autoMapper =
					mcSharedMatcher
							.getMulticoreMappingByWeight(localMulticoreNode, sharedMulticoreNode, isStrictMatch());
			localMulticoreNode.setSharedProxy(sharedMulticoreNode);
			for (Map.Entry<MCNode, MCSharedNode> matchEntry : autoMapper.entrySet()) {
				matchEntry.getKey().setSharedProxy(matchEntry.getValue());
			}
			return true;
		}
		return false;
	}

	private boolean isMatched(MCNode mcNode, MCSharedNode mcSharedNode)
	{
		MCSharedMatcher mcSharedMatcher = new MCSharedMatcher();

		return mcSharedMatcher.isMatched(isStrictMatch(), mcNode, mcSharedNode);
	}

	public boolean isAllowUnassociateAll(MCNode mcroot)
	{
		MCNode mcNode = (MCNode) mcroot.getFirstChild();
		for (Enumeration<?> enumerator = mcNode.depthFirstEnumeration(); enumerator.hasMoreElements(); ) {
			MCNode node = (MCNode) enumerator.nextElement();
			if (node.isAssigned()) {
				return true;
			}
		}
		return false;
	}

	public boolean isAllowAssociateAll(MCNode mcroot, MCSharedNode sharedMCroot)
	{
		MCNode mcNode = (MCNode) mcroot.getFirstChild();
		MCSharedNode sharedMCNode = (MCSharedNode) sharedMCroot.getFirstChild();
		return !mcNode.isAssigned() && !sharedMCNode.isAssigned();
	}

	public void associateAll(MCNode rootMCNode, MCSharedNode rootShareMCNode)
	{
		if (!rootMCNode.isAssigned() && rootShareMCNode != null && !rootShareMCNode.isAssigned()) {
			autoAssignMulticores(rootMCNode, rootShareMCNode);
		}
	}

	public void associate(@Nullable MCNode mcNode, @Nullable MCSharedNode sharedMCNode)
	{
		if (mcNode != null && !mcNode.isAssigned() && sharedMCNode != null && !sharedMCNode.isAssigned()) {
			boolean assigned = autoAssignMulticores(mcNode, sharedMCNode);
			if (assigned) {
				MCNode parentMcNode = mcNode.getParentMcNode();
				if (parentMcNode != null && !parentMcNode.isAssigned()) {
					if (parentMcNode.getUnassignedChildCount() == 0) {
						MCSharedNode parentMcSharedNode = sharedMCNode.getParentMcSharedNode();
						associate(parentMcNode, parentMcSharedNode);
					}
				}
			}
		}
	}

	public void disassociate(@Nullable MCNode mcNode, @Nullable MCSharedNode sharedMCNode)
	{
		if (mcNode != null && mcNode.isAssigned() && sharedMCNode != null && sharedMCNode.isAssigned()) {
			mcNode.removeAssociationAllChildren();
			unAssignParentMulticores(mcNode, sharedMCNode);
		}
	}

	public void disassociateAll(MCNode rootMCNode)
	{
		rootMCNode.removeAssociationAllChildren();
	}

	private void unAssignParentMulticores(@NotNull MCNode mcNode, @NotNull MCSharedNode sharedMCNode)
	{
		mcNode.removeAssociation();
		MCNode parentMcNode = mcNode.getParentMcNode();
		MCSharedNode parentMcSharedNode = sharedMCNode.getParentMcSharedNode();
		// Un-Assiging any children should Un-Assign its parents in hierarchy
		if (parentMcNode != null && parentMcNode.isAssigned() && parentMcSharedNode != null) {
			unAssignParentMulticores(parentMcNode, parentMcSharedNode);
		}
	}

	public void reset(MCNode localMCNode)
	{
		if (localMCNode != null && localMCNode.isAssigned()) {
			localMCNode.removeAssociationAllChildren();
		}
	}

	@Nullable public MCNode getRootMCProxy()
	{
		return m_sharedMulticoreModel.getRootProxy();
	}

	@Nullable public String validateName(IProperty property, IMulticore multicore)
	{
		String name = ((IStringProperty) property).getValue();
		@SuppressWarnings("MagicNumber") StringBuffer errmsg = new StringBuffer(32);
		IReadOnlyNamedObject namedObject = getNamedObject(multicore);
		boolean valid = validateName(name, errmsg);

		//duplicate name
		if (valid) {
			ValidateDuplicateSharedNames findDuplicateShared = new ValidateDuplicateSharedNames(m_design);
			//check if it's a valid duplicate name
			StringBuilder reasonForFailure = new StringBuilder();
			if (namedObject != null) {
				if (findDuplicateShared.isDuplicateName(StringUtils.trim(name), namedObject, m_design.getProject())) {
					checkOptionExpression(namedObject, reasonForFailure);
					if (reasonForFailure.toString().isEmpty()) {
						reasonForFailure.append("SelectSharedPanel.nameused.text");
					}
					//in all cases, register a warning
					property.addNote(
							ResourceMgr.getString(SelectSharedPanel.class, reasonForFailure.toString()),
							IProperty.NoteTagLevel.Warning);
					return ResourceMgr.getString(SelectSharedPanel.class, reasonForFailure.toString());
				}
				else {
					return null;
				}
			}
			else {
				return null;
			}
		}
		else {
			return errmsg.toString();
		}
	}

	@NotNull public static NameValidationResult validateName(String name, IMulticore multicore, @NotNull ILogicDesign design)
	{
		@SuppressWarnings("MagicNumber") StringBuffer errmsg = new StringBuffer(32);
		IReadOnlyNamedObject namedObject = getNamedObject(multicore);
		boolean valid = validateName(name, errmsg);

		//duplicate name
		if (valid) {
			ValidateDuplicateSharedNames findDuplicateShared = new ValidateDuplicateSharedNames(design);
			//check if it's a valid duplicate name
			StringBuilder reasonForFailure = new StringBuilder();
			if (namedObject != null) {
				if (findDuplicateShared.isDuplicateName(StringUtils.trim(name), namedObject, design.getProject())) {
					checkOptionExpression(namedObject, reasonForFailure);
					return reasonForFailure.toString().isEmpty() ? NameValidationResult.DuplicateName : NameValidationResult.DuplicateNameAndOptionExpression;
				}
				else {
					return NameValidationResult.Valid;
				}
			}
			else {
				return NameValidationResult.Valid;
			}
		}
		else {
			return NameValidationResult.Invalid;
		}
	}

	public enum NameValidationResult
	{
		Valid,
		Invalid,
		DuplicateName,
		DuplicateNameAndOptionExpression
	}

	private static void checkOptionExpression(IReadOnlyNamedObject namedObject, StringBuilder reasonForFailure)
	{
		if (namedObject instanceof IOptionedObject) {
			IOptionedObject optionedObject = (IOptionedObject) namedObject;
			IOptionExpression opExpression = optionedObject.getOptionExpression();
			if (opExpression != null && opExpression.getExpression() != null &&
					!opExpression.getExpression().isEmpty()) {
				reasonForFailure.append("SelectSharedPanel.nameandoptionexpressionalreadyused.text");
			}
		}
	}

	private static boolean validateName(String name, StringBuffer errmsg)
	{
		boolean ok = true;

		if (name == null || name.trim().isEmpty()) {
			if (errmsg.length() != 0) {
				errmsg.append('\n');
			}
			errmsg.append(ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.InvalidName.text"));
			ok = false;
		}

		return ok;
	}

	@Nullable
	protected static IReadOnlyNamedObject getNamedObject(IUIDObject iUIDObject)
	{
		IReadOnlyNamedObject namedObject = null;
		if (iUIDObject instanceof IRepresentedObject) {
			IRepresentedObject repObj = (IRepresentedObject) iUIDObject;
			IUIDObject logicObj = repObj.getRawConnectivity();
			if (logicObj instanceof IIndexedNamedObject) {
				namedObject = (IReadOnlyNamedObject) logicObj;
			}
		}
		else if (iUIDObject instanceof IReadOnlyNamedObject) {
			// This change for Topology - has several non-graphical named objects
			namedObject = (IReadOnlyNamedObject) iUIDObject;
		}
		return namedObject;
	}

	public void setRootMCNode(MCNode localMCNode)
	{
		m_sharedMulticoreModel.setRootMCNode(localMCNode);
	}

	public boolean mappingDone()
	{
		return entireMCStructureAssigned();
	}

	private boolean entireMCStructureAssigned()
	{
		MCNode rootMCProxy = getRootMCProxy();
		if (rootMCProxy != null) {
			for (Enumeration<TreeNode> enumerator = rootMCProxy.breadthFirstEnumeration();
					enumerator.hasMoreElements(); ) {
				MCNode node = (MCNode) enumerator.nextElement();
				if (!node.isAssigned()) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean onCompletion()
	{
		ISharedMulticore sharedMulticore = getSharedMulticore();
		if (sharedMulticore != null) {
			sharedMulticore.unlock();
		}
		return true;
	}

	public void onCancel()
	{
		ISharedMulticore sharedMulticore = getSharedMulticore();
		if (sharedMulticore != null) {
			sharedMulticore.unlock();
		}
	}
}
