package chs.caplets.logic.icd;

import chs.cof.logical.schem.IPin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SignalPathTree
{
	private IPin node;
	private Collection<SignalPathTree> children;
	private SignalPathTree parent;
	private int depth;
	//private List<IPin> pathFromRoot;

	SignalPathTree(@Nullable IPin pin) {
		node = pin;
		children = new ArrayList<>();
	}

	@Nullable public IPin getPin() {
		return node;
	}

	public Collection<SignalPathTree> getChildren()
	{
		return children;
	}

	public SignalPathTree getParent() {
		return parent;
	}

	public void setParent(SignalPathTree parentNode) {
		parent = parentNode;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depthToBeSet) {
		depth = depthToBeSet;
		readjustDepthOfChildren();
	}

	public void readjustDepthOfChildren() {
		for (SignalPathTree child : children) {
			child.setDepth(depth + 1);
		}
	}

	public void addChild(SignalPathTree childNode) {
		children.add(childNode);
		childNode.setParent(this);
		childNode.setDepth(depth + 1);
	}

	public List<SignalPathTree> getLeafNodes() {
		List<SignalPathTree> leafNodes = new ArrayList<>();
		if (children.isEmpty()) {
			leafNodes.add(this);
		}
		else {
			for (SignalPathTree childNode : children) {
				leafNodes.addAll(childNode.getLeafNodes());
			}
		}
		return leafNodes;
	}

	public List<SignalPathTree> getAllNodes() {
		List<SignalPathTree> allNodes = new ArrayList<>();
		allNodes.add(this);
		for (SignalPathTree child : children) {
			allNodes.addAll(child.getAllNodes());
		}
		return allNodes;
	}
}
