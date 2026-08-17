/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caf.caplet.helpers.tabulareditor.ObjectFilterMenuItemProvider;
import chs.common.IAttributePropertyProvider;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Custom filter menu pane provider
 */
public abstract class BatchShareCustomFilterProvider
{

	private final Function<Predicate<IAttributePropertyProvider>, Boolean> setFilterPredicate;

	private Map<IBatchShareCustomFilter, Boolean> filtersMap = new HashMap<>();

	protected BatchShareCustomFilterProvider(
			Function<Predicate<IAttributePropertyProvider>, Boolean> setFilterPredicate)
	{
		this.setFilterPredicate = setFilterPredicate;
	}

	public void registerFilter(IBatchShareCustomFilter filter)
	{
		filtersMap.put(filter, false);
	}

	/**
	 * @return custom filter pane
	 */
	@NotNull public Pane constructFilterPane()
	{
		FlowPane pane = new FlowPane();
		pane.setAlignment(Pos.BASELINE_RIGHT);
		pane.setMaxWidth(ObjectFilterMenuItemProvider.MAX_WIDTH);
		pane.setPrefWidth(ObjectFilterMenuItemProvider.MAX_WIDTH);
		filtersMap.forEach((filter, isEnabled) -> {
			ToggleButton button = new ToggleButton();
			button.getStyleClass().add("filter-toggle-button");
			button.setSelected(isEnabled);
			button.setId(filter.getId() + "Filter");
			button.setGraphic(new ImageView(filter.getImage()));
			button.setTooltip(new Tooltip(filter.getTooltipString()));
			button.setMinSize(ObjectFilterMenuItemProvider.BUTTON_SIZE, ObjectFilterMenuItemProvider.BUTTON_SIZE);
			button.setMaxSize(ObjectFilterMenuItemProvider.BUTTON_SIZE, ObjectFilterMenuItemProvider.BUTTON_SIZE);
			button.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
				filtersMap.put(filter, t1);
				setFilterPredicate.apply(t -> true);
			});
			pane.getChildren().add(button);
		});
		return pane;
	}

	@NotNull public Predicate<IBatchShareRow> getPredicate()
	{
		AtomicReference<Predicate<IBatchShareRow>> p = new AtomicReference<>(t -> true);
		filtersMap.forEach((filter, isEnabled) -> {
			if (isEnabled) {
				p.set(p.get().and(filter.getPredicate()));
			}
		});
		return p.get();
	}

	@NotNull public Set<String> getFilterIds()
	{
		return filtersMap.keySet().stream().filter(t -> filtersMap.get(t)).map(s -> s.getId())
				.collect(Collectors.toSet());
	}

	public void clearAndResetFilterSelection(Set<String> ids)
	{
		filtersMap.keySet().forEach(filter -> {
			if (ids.contains(filter.getId())) {
				filtersMap.put(filter, true);
			}
			else {
				filtersMap.put(filter, false);
			}
		});
	}
}
