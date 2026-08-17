package chs.caplets.logic.actions.shared.helper;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPin;
import chs.ctf.caf.utils.IPinProxy;
import chs.ctf.caf.utils.PinProxy;
import chs.utilities.ui.SortedListModel;
import chs.utility.helpers.NamedObjectComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PinReuseHandler extends BaseSharePinlistHandler
{

	@NotNull private final SortedListModel<IPinProxy> tiedList;
	@NotNull private final SortedListModel<IPinProxy> reusableList;
	@NotNull private final Set<IPinProxy> alreadyReusableProxies;

	public PinReuseHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design)
	{
		this(model, design, null, false);
	}

	public PinReuseHandler(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@Nullable IShareMessageContextReporter reporter, boolean isBulkShare)
	{
		super(model, design, reporter, isBulkShare);
		addPinChangeListener((e) -> onPinChange());
		addSharedChangeListener((e) -> onSharedPinlistChange());
		alreadyReusableProxies = new HashSet<IPinProxy>();
		// Can't generify the name object comparator because the static object can't be typed (I think)
		//noinspection unchecked
		tiedList = new SortedListModel<IPinProxy>(NamedObjectComparator.caseInsensitiveComparator());
		reusableList = model.getReusableProxies();
	}

	@NotNull public SortedListModel<IPinProxy> getTiedList()
	{
		return tiedList;
	}

	@NotNull public SortedListModel<IPinProxy> getReusableList()
	{
		return reusableList;
	}

	public void removeFromReusable(@NotNull List<IPinProxy> toMakeNonReusable)
	{
		// Add the proxies to be added to respective lists and then add/remove to list model in one go
		// This avoids performance overhead of ProxyList.fireChangeEvent being called for every add/remove
		reusableList.removeAll(toMakeNonReusable);
		tiedList.addAll(toMakeNonReusable);
	}

	public void removeAllFromReusable()
	{
		List<IPinProxy> allObjects = new ArrayList<IPinProxy>(reusableList);
		removeFromReusable(allObjects);
	}

	/**
	 * This function will determine if the pin to be made reusable is already used on a design in the project. If it is
	 * then various conditions apply to making it reusable.  If all pass then the proxy is added to the reusable list.
	 *
	 * @param s1 Array fo pin proxies
	 */
	public void makePinsReusable(@NotNull List<IPinProxy> s1)
	{
		// Add the proxies to be added to respective lists and then add/remove to list model in one go
		// This avoids performance overhead of ProxyList.fireChangeEvent being called for every add/remove
		List<IPinProxy> reUsePinProxies = new ArrayList<IPinProxy>(s1);
		if (!reUsePinProxies.isEmpty()) {
			reusableList.addAll(reUsePinProxies);
			tiedList.removeAll(reUsePinProxies);
		}
	}

	public void makeAllPinsReusable()
	{
		makePinsReusable(tiedList);
	}

	public boolean allowAdd(@NotNull List<IPinProxy> selected)
	{
		boolean allow = !selected.isEmpty();
		for (int i = 0; i < selected.size() && allow; i++) {
			PinProxy pp = (PinProxy) selected.get(i);
			if (!alreadyReusableProxies.contains(pp)) {
				ISharedPin spin = pp.getSharedPin();
				if (spin != null) {
					allow = allowAdd(spin);
				}
			}
		}
		return allow;
	}

	public boolean allowRemove(@NotNull List<IPinProxy> selected)
	{
		boolean allow = !selected.isEmpty();
		for (int i = 0; i < selected.size() && allow; i++) {
			PinProxy pp = (PinProxy) selected.get(i);
			if (alreadyReusableProxies.contains(pp)) {
				allow = allowRemove(pp.getSharedPin());
			}
		}
		return allow;
	}

	public boolean allowRemove(@Nullable ISharedPin spin)
	{
		return getModel().allowRemove(spin);
	}

	public boolean allowAdd(@NotNull ISharedPin spin)
	{
		return !spin.isReusable();
	}

	public boolean allowAddAll()
	{
		boolean allow = tiedList.getSize() > 0;
		for (IPinProxy pp : tiedList) {
			if (!allow) {
				break;
			}
			if (!alreadyReusableProxies.contains(pp)) {
				ISharedPin spin = pp.getSharedPin();
				if (spin != null) {
					allow = allowAdd(spin);
				}
			}
		}
		return allow;
	}

	public boolean allowRemoveAll()
	{
		if (reusableList.getSize() == 0) {
			return false;
		}
		for (IPinProxy pp : reusableList) {
			if (alreadyReusableProxies.contains(pp)) {
				if (!allowRemove(pp.getSharedPin())) {
					return false;
				}
			}
		}
		return true;
	}

	private void onPinChange()
	{
		// Get rid of any pins that were removed.
		for (Iterator<IPinProxy> pitr = tiedList.iterator(); pitr.hasNext(); ) {
			IPinProxy ppp = pitr.next();
			if (!getProxies().contains(ppp)) {
				pitr.remove();
			}
		}
		for (Iterator<IPinProxy> pitr = reusableList.iterator(); pitr.hasNext(); ) {
			IPinProxy ppp = pitr.next();
			if (!getProxies().contains(ppp)) {
				pitr.remove();
			}
		}

		// Put any added pins on the non-reusable side
		for (IPinProxy ppp : getProxies()) {
			if (!tiedList.contains(ppp) && !reusableList.contains(ppp)) {
				tiedList.add(ppp);
			}
		}
	}

	public void onSharedPinlistChange()
	{
		tiedList.clear();
		reusableList.clear();
		alreadyReusableProxies.clear();
		// Add the proxies to be added to respective lists and then add/remove to list model in one go
		// This avoids performance overhead of ProxyList.fireChangeEvent being called for every add/remove
		List<IPinProxy> proxiesTied = new ArrayList<IPinProxy>();
		List<IPinProxy> proxiesReuse = new ArrayList<IPinProxy>();
		for (IPinProxy proxy : getProxies()) {
			if (proxy.getSharedPin() != null && proxy.getSharedPin().isReusable()) {
				proxiesReuse.add(proxy);
				alreadyReusableProxies.add(proxy);
			}
			else {
				proxiesTied.add(proxy);
			}
		}
		if (!proxiesReuse.isEmpty()) {
			reusableList.addAll(proxiesReuse);
			proxiesReuse.clear();
		}
		if (!proxiesTied.isEmpty()) {
			tiedList.addAll(proxiesTied);
			proxiesTied.clear();
		}
	}

	public void init()
	{
		onSharedPinlistChange();
	}

	public boolean isAlreadyReusable(@NotNull IPinProxy pp)
	{
		return alreadyReusableProxies.contains(pp);
	}
}
