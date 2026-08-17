package chs.caplets.logic.actions.ui;

import chs.cof.logical.cable.IConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.common.graph.MaximumMatchingInBipartiteGraphFinder;
import chs.utility.algorithm.IAssignmentAlgorithm;
import chs.utility.algorithm.IAssignmentWeight;
import chs.utility.algorithm.OptimalAssignmentImpl;
import chs.view.connectivity.comparator.ComparatorContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public class MCSharedMatcher
{

	private boolean mstrictMatch;
	private static final int NAME_ATTRIBUTE_MATCH_WEIGHT = 10000;
	private static final int MAT_ATTRIBUTE_MATCH_WEIGHT = 1000;
	private static final int SPEC_ATTRIBUTE_MATCH_WEIGHT = 100;
	private static final int COLOR_ATTRIBUTE_MATCH_WEIGHT = 10;
	public static final MCNodeComparator comparator = new MCNodeComparator();

	private static final Comparator<MulticoreMatchWeight>
			MULTICORE_MATCH_WEIGHT_COPARATOR =
			new Comparator<MulticoreMatchWeight>()
			{
				public int compare(
						MulticoreMatchWeight o1, MulticoreMatchWeight o2)
				{
					int wg1 = o1.getWeight();
					int wg2 = o2.getWeight();
					if (wg1 > wg2) {
						return -1;
					}
					if (wg1 < wg2) {
						return 1;
					}
					return 0;
				}
			};

	public MCSharedMatcher()
	{

	}

	public boolean isMatched(boolean strictMatch, MCNode mcNode, MCSharedNode mcSharedNode)
	{
		mstrictMatch = strictMatch;
		if ((mcNode.isOverbraid() && !mcSharedNode.isOverbraid()) ||
				(!mcNode.isOverbraid() && mcSharedNode.isOverbraid())) {
			return false;
		}
		if (mcNode.getLevel() != mcSharedNode.getLevel()) {
			return false;
		}
		if (mcNode.hasLibraryRef() &&
				(!mcSharedNode.hasLibraryRef() ||
						!(mcSharedNode.getLibraryRef() == mcNode.getLibraryRef()))) {
			return false;
		}
		if (!mcNode.isOverbraid()) {
			if (mcNode.getSheathType() != null &&
					(mcSharedNode.getSheathType() == null ||
							!mcSharedNode.getSheathType().equals(mcNode.getSheathType()))) {
				return false;
			}
		}
		if (!mcNode.getConductorRole().equals(mcSharedNode.getConductorRole())) {
			return false;
		}
		if (!isSiblingAssignedToSameSharedSibling(mcNode, mcSharedNode)) {
			return false;
		}
		int refChildCount = mcSharedNode.getUnassignedChildCount();
		boolean isStructureMatching = mstrictMatch ? refChildCount == mcNode.getUnassignedChildCount() :
				refChildCount >= mcNode.getUnassignedChildCount();
		if (isStructureMatching) {
			if (refChildCount == 0) {   // No more to compare they are match
				return true;
			}
			List<MCNode> mcChildren = getSortedMulticoreChilds(mcNode);
			List<MCSharedNode> sharedMCChildren = getSortedSharedMulticoreChilds(mcSharedNode);

			if (!mcChildren.isEmpty()) {
				MaximumMatchingInBipartiteGraphFinder<MCNode, MCSharedNode> matcher =
						getStructureMatchFinder();
				return allObjectsHaveValidMatchings(matcher, mcChildren, sharedMCChildren);
			}
			return true;
		}
		else {
			return false;
		}
	}

	@NotNull private List<MCNode> getSortedMulticoreChilds(@NotNull MCNode mcNode)
	{
		List<MCNode> mcChildren = new ArrayList<>();
		for (Enumeration<MCNode> enumerator = mcNode.unassignedChildren(); enumerator.hasMoreElements(); ) {
			mcChildren.add(enumerator.nextElement());
		}
		Collections.sort(mcChildren, comparator);
		return mcChildren;
	}

	@NotNull private List<MCSharedNode> getSortedSharedMulticoreChilds(MCSharedNode mcSharedNode)
	{
		List<MCSharedNode> sharedMCProxy = new ArrayList<>();
		for (Enumeration<MCSharedNode> enumerator = mcSharedNode.unassignedChildren();
				enumerator.hasMoreElements(); ) {
			sharedMCProxy.add(enumerator.nextElement());
		}
		Collections.sort(sharedMCProxy, comparator);
		return sharedMCProxy;
	}

	private boolean allObjectsHaveValidMatchings(
			MaximumMatchingInBipartiteGraphFinder<MCNode, MCSharedNode> matcher,
			List<MCNode> logicObjects, List<MCSharedNode> sharedObjects)
	{
		removeAlreadyMatching(sharedObjects, logicObjects);
		return allObjectsCanBeAssignable(matcher, logicObjects, sharedObjects);
	}

	private boolean allObjectsCanBeAssignable(
			MaximumMatchingInBipartiteGraphFinder<MCNode, MCSharedNode> matcher,
			@NotNull List<MCNode> mcNodeList, @NotNull List<MCSharedNode> mcSharedNodeList)
	{
		if (mcNodeList.isEmpty()) {
			return true;
		}
		if (mcNodeList.size() > mcSharedNodeList.size()) {
			return false;
		}
		return matcher.hasMatchings(mcNodeList, mcSharedNodeList);
	}

	private void removeAlreadyMatching(@NotNull List<MCSharedNode> sharedProxies,
			@NotNull List<MCNode> logicObjects)
	{
		Set<MCNode> alreadyAssigned = new HashSet<>();
		for (MCNode child : logicObjects) {
			final MCSharedNode sharedProxy = child.getSharedProxy();
			if (sharedProxy != null) {
				if (sharedProxies.remove(sharedProxy)) {
					alreadyAssigned.add(child);
				}
			}
		}
		logicObjects.removeAll(alreadyAssigned);
	}

	@NotNull
	private MaximumMatchingInBipartiteGraphFinder<MCNode, MCSharedNode> getStructureMatchFinder()
	{
		return new MaximumMatchingInBipartiteGraphFinder<>(getStructureMatcher());
	}

	@NotNull
	private BiPredicate<MCNode, MCSharedNode> getStructureMatcher()
	{
		return (left, right) -> (isMatched(mstrictMatch, left, right));
	}

	@NotNull public Map<MCNode, MCSharedNode> getMulticoreMappingByWeight(MCNode mcNode,
			MCSharedNode mcSharedNode, boolean isStrictMatch)
	{
		mstrictMatch = isStrictMatch;
		List<MCNode> mcNodeChildren = getSortedMulticoreChilds(mcNode);
		List<MCSharedNode> sharedMcChildren = getSortedSharedMulticoreChilds(mcSharedNode);

		IAssignmentAlgorithm<MCNode, MCSharedNode> assigner =
				new OptimalAssignmentImpl<MCNode, MCSharedNode>();
		Map<MCNode, MCSharedNode> assignment =
				assigner.getAssignment(mcNodeChildren, sharedMcChildren, getWeightAssignment(),
						ComparatorContext.COMPARATOR_DEFAULT_CONTEXT);
		Map<MCNode, MCSharedNode> allMatchings = new HashMap<>(assignment);
		for (Map.Entry<MCNode, MCSharedNode> matchEntry : assignment.entrySet()) {
			allMatchings.putAll(getChildrenMap(matchEntry.getKey(), matchEntry.getValue()));
		}
		return allMatchings;
	}

	@NotNull private Map<MCNode, MCSharedNode> getChildrenMap(
			MCNode source, MCSharedNode target)
	{
		return getMulticoreMappingByWeight(source, target, mstrictMatch);
	}

	@NotNull private IAssignmentWeight<MCNode, MCSharedNode> getWeightAssignment()
	{
		return new IAssignmentWeight<MCNode, MCSharedNode>()
		{
			@Override
			public float getWeight(MCNode object1, MCSharedNode object2)
			{
				if (isMatched(mstrictMatch, object1, object2)) {
					return getMatchWeight(object1, object2);
				}
				return Integer.MIN_VALUE;
			}

			@Override public double[] getWeightForRow(@NotNull MCNode object1,
					@NotNull Map<Integer, MCSharedNode> secondObjectsMap, @NotNull ComparatorContext context)
			{
				int sizeObject2 = secondObjectsMap.size();
				double[] weights = new double[sizeObject2];
				for (int i = 0; i < sizeObject2; i++) {
					weights[i] = getWeight(object1, secondObjectsMap.get(i));
				}
				return weights;
			}
		};
	}

	private int getAttributeMatchWeight(@NotNull MCNode logicMCNode, @NotNull MCSharedNode mcSharedNode)
	{
		if (logicMCNode.getName().equals(mcSharedNode.getName())) {
			return NAME_ATTRIBUTE_MATCH_WEIGHT;
		}
		if ((!logicMCNode.getAttributes().isEmpty() && !mcSharedNode.getAttributes().isEmpty())) {
			if (logicMCNode.getRef() instanceof IConductor && mcSharedNode.getRef() instanceof ISharedConductor) {
				int matchWeight = 0;
				IConductor conductor = (IConductor) logicMCNode.getRef();
				ISharedConductor sharedConductor = (ISharedConductor) mcSharedNode.getRef();

				String materialCode = conductor.getMaterialCode();
				String sharedConductorMaterialCode = sharedConductor.getMaterialCode();
				if (materialCode != null && !materialCode.trim().isEmpty() &&
						sharedConductorMaterialCode != null &&
						!sharedConductorMaterialCode.trim().isEmpty() && materialCode
						.equalsIgnoreCase(sharedConductorMaterialCode)) {
					matchWeight = MAT_ATTRIBUTE_MATCH_WEIGHT;
				}
				String wireSpecification = conductor.getWireSpecification();
				String sharedConductorWireSpecification = sharedConductor.getWireSpecification();
				if (wireSpecification != null &&
						!wireSpecification.trim().isEmpty() &&
						sharedConductorWireSpecification != null &&
						!sharedConductorWireSpecification.trim().isEmpty() && wireSpecification
						.equalsIgnoreCase(sharedConductorWireSpecification)) {
					matchWeight += SPEC_ATTRIBUTE_MATCH_WEIGHT;
				}
				String wireColor = conductor.getWireColor();
				String sharedConductorWireColor = sharedConductor.getWireColor();
				if (wireColor != null && !wireColor.trim().isEmpty() &&
						sharedConductorWireColor != null && !sharedConductorWireColor.trim().isEmpty() &&
						wireColor.equalsIgnoreCase(sharedConductorWireColor)) {
					matchWeight += COLOR_ATTRIBUTE_MATCH_WEIGHT;
				}

				return matchWeight;
			}
		}
		if (logicMCNode.getUnassignedChildCount() == mcSharedNode.getUnassignedChildCount()) {
			return 1;
		}
		return 0;
	}

	private int getMatchWeight(@NotNull MCNode logicMCNode, @NotNull MCSharedNode mcSharedNode)
	{
		int matchWeight = getAttributeMatchWeight(logicMCNode, mcSharedNode);

		List<MCNode> childMCNodes = new ArrayList<MCNode>();
		for (Enumeration<MCNode> enumerator = logicMCNode.unassignedChildren(); enumerator.hasMoreElements(); ) {
			childMCNodes.add(enumerator.nextElement());
		}
		Collections.sort(childMCNodes, comparator);

		List<MCSharedNode> childMCSharedNodes = new ArrayList<MCSharedNode>();
		for (Enumeration<MCSharedNode> enumerator = mcSharedNode.unassignedChildren(); enumerator.hasMoreElements(); ) {
			childMCSharedNodes.add(enumerator.nextElement());
		}
		Collections.sort(childMCSharedNodes, comparator);

		Map<MCNode, List<MulticoreMatchWeight>> multicoreMatchWtMap =
				new HashMap<MCNode, List<MulticoreMatchWeight>>();
		processChild(childMCNodes, childMCSharedNodes, multicoreMatchWtMap);

		List<MulticoreMatchWeight> multMatchWeightList = new ArrayList<MulticoreMatchWeight>();
		for (MCNode mcNode : childMCNodes) {
			if (multicoreMatchWtMap.containsKey(mcNode)) {
				multMatchWeightList.addAll(multicoreMatchWtMap.get(mcNode));
			}
		}

		Collections.sort(multMatchWeightList, MULTICORE_MATCH_WEIGHT_COPARATOR);

		boolean hasElements = !multMatchWeightList.isEmpty();
		Set<MulticoreMatchWeight> matchMCList = new HashSet<MulticoreMatchWeight>();
		while (hasElements) {
			MulticoreMatchWeight matchSharedMCNode = multMatchWeightList.get(0);
			MCNode matchMCNode = matchSharedMCNode.getMCNode();

			List<MulticoreMatchWeight> removeMatchWt = new ArrayList<MulticoreMatchWeight>();
			for (MulticoreMatchWeight multicoreMatchWt : multMatchWeightList) {
				if (multicoreMatchWt.getMCSharedNode() == matchSharedMCNode.getMCSharedNode()) {
					multicoreMatchWtMap.get(multicoreMatchWt.getMCNode()).remove(multicoreMatchWt);
					removeMatchWt.add(multicoreMatchWt);
				}
			}
			removeMatchWt.addAll(multicoreMatchWtMap.get(matchMCNode));
			multMatchWeightList.removeAll(removeMatchWt);
			matchMCList.add(matchSharedMCNode);
			hasElements = !multMatchWeightList.isEmpty();
		}
		for (MulticoreMatchWeight sharedMCNodeWt : matchMCList) {
			matchWeight += sharedMCNodeWt.getWeight();
		}
		return matchWeight;
	}

	private void processChild(@NotNull List<MCNode> childMCNodes, List<MCSharedNode> childSharedMCNodes,
			Map<MCNode, List<MulticoreMatchWeight>> multicoreMatchWtMap)
	{
		for (MCNode childMCNode : childMCNodes) {
			boolean hasMatch = false;
			for (MCSharedNode childSharedMCNode : childSharedMCNodes) {
				if (isMatched(mstrictMatch, childMCNode, childSharedMCNode)) {
					// If the multicore has match with shared multicore, create multicore match entry update the map
					List<MulticoreMatchWeight> sharedMCMatchMCList = multicoreMatchWtMap.get(childMCNode);
					if (sharedMCMatchMCList == null) {
						sharedMCMatchMCList = new ArrayList<MulticoreMatchWeight>();
						multicoreMatchWtMap.put(childMCNode, sharedMCMatchMCList);
					}
					boolean containsMatch = false;
					for (MulticoreMatchWeight multicoreMatch : sharedMCMatchMCList) {
						if (multicoreMatch.getMCSharedNode() == childSharedMCNode &&
								multicoreMatch.getMCNode() == childMCNode) {
							containsMatch = true;
							break;
						}
					}
					if (!containsMatch) {
						int weight = getMatchWeight(childMCNode, childSharedMCNode);
						sharedMCMatchMCList.add(new MulticoreMatchWeight(childSharedMCNode, childMCNode, weight));
					}
					hasMatch = true;
				}
			}
			assert hasMatch : "Error in matching multicore structures";
		}
	}

	private static boolean isSiblingAssignedToSameSharedSibling(@NotNull MCNode mcNode,
			@NotNull MCSharedNode sharedNode)
	{

		MCNode parentNode = mcNode.getParentMcNode();
		MCSharedNode selectedSharedParentNode = sharedNode.getParentMcSharedNode();
		if (parentNode != null && selectedSharedParentNode != null) {
			MCNode assginedChildNode = null;
			for (Enumeration<?> enumerator = parentNode.children(); enumerator.hasMoreElements(); ) {
				MCNode node = (MCNode) enumerator.nextElement();
				if (node.isAssigned()) {
					assginedChildNode = node;
					break;
				}
			}
			if (assginedChildNode != null && assginedChildNode.getSharedProxy() != null) {
				MCSharedNode otherSharedParentNode = assginedChildNode.getSharedProxy().getParentMcSharedNode();
				return otherSharedParentNode == selectedSharedParentNode;
			}
		}
		return true;
	}

	public static class MulticoreMatchWeight
	{

		private MCSharedNode m_mcSharedNode;
		private MCNode m_mcNode;
		private int m_weight;

		MulticoreMatchWeight(MCSharedNode mcSharedNode, MCNode mcNode, int wt)
		{
			m_mcSharedNode = mcSharedNode;
			m_mcNode = mcNode;
			m_weight = wt;
		}

		public MCSharedNode getMCSharedNode()
		{
			return m_mcSharedNode;
		}

		public MCNode getMCNode()
		{
			return m_mcNode;
		}

		public int getWeight()
		{
			return m_weight;
		}
	}
}