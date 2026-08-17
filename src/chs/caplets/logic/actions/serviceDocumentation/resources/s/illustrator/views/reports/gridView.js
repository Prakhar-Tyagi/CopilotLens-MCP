/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, Backbone, _, define, $*/
define([], function ()
{
    "use strict";
    return function (model, templates, eventHandlers)
    {
        model = model || {};
        var p = mentor.publisher, GridView = Backbone.View.extend({
            model: model,
            container: model.get('container'),
            sortable: model.get('sortable'),
            searchable: model.get('searchable'),
            events: {
                "click th": "onColumnHeaderClick",
                "keyup input": "onSearchTextEnter",
                "mouseup input": "clearFilter",
                "click td": "onCellClick",
                "click input": "removeTextPlaceHolder",
                "focusout input": "addTextPlaceHolder",
            },
            removeTextPlaceHolder: function (evt)
            {
                var ele = $(evt.target);
                if ($(ele).hasClass("placeHolderText")) {
                    $(ele).val("");
                    $(ele).removeClass("placeHolderText");
                }
            },

            addTextPlaceHolder: function (evt)
            {
                var ele = $(evt.target);
                if ($(ele).val().trim() === "") {
                    this.addPlaceHolderText(ele);
                }
            },
            addPlaceHolderText: function (element)
            {
                $(element).val(mentor.publisher.languageTranslator.localize('Search'));
                $(element).addClass("placeHolderText");
            },
            getTargetElement: function (e)
            {
                return $(e.target);
            }, clearFilter: function (e)
            {
                var searchInput = this.getTargetElement(e), oldValue = searchInput.val();

                if (oldValue === "") {
                    return;
                }
                var that = this;
                setTimeout(function ()
                {
                    var newValue = searchInput.val();

                    if (newValue === "") {
                        that.onSearchTextEnter(e);
                    }
                }, 1);
            },
            onCellClick: function (e)
            {
                if (eventHandlers && eventHandlers.onCellClick) {
                    eventHandlers.onCellClick($(e.currentTarget), e);
                }
            },
            sorting: {
                col: "",
                order: "&#9650;",
                desc: "&#9650;",
                asc: "&#9660;"
            },
            filtering: {},

            onSearchTextEnter: function (e)
            {
                var colName = this.getTargetElement(e).attr('data-col');
                this.filtering[colName] = this.getTargetElement(e).val();
                //this.filtering['searchAllCols'] = false;
                this.renderTableBody(true);
            },
            refreshVisibleIndices: function () {
                var visibleIndices = this.getVisibleIndices(this.filtering);
                var config = p.xmlLoader.loadFile("config.json", false, true, "json");
                var rowsPerPage = (config && config.data && config.data.rowsPerTablePage) || 100;
                this.visibleData = {
                    indices: visibleIndices,
                    pageCount: 1 + Math.floor(visibleIndices.length / rowsPerPage),
                    currentPage: 1,
                    getRangeStart: function () {
                        return this.currentPage * rowsPerPage - rowsPerPage;
                    },
                    getRangeEnd: function () {
                        return _.min([this.indices.length, this.currentPage * rowsPerPage]);
                    }
                };
            },
            renderTableBody: function (shouldRefetchVisibleIndices)
            {
                if (shouldRefetchVisibleIndices) {
                    this.refreshVisibleIndices();
                }
                var innerTableTemplate = _.template(templates.get('innerTable'));
                var renderData = {
                    data: model.getTableData(),
                    visibleData: this.visibleData,
                    templateFn: innerTableTemplate
                };
                var compiledHTML = _.template(templates.get('table'))(renderData);
                $("tbody", this.$el).replaceWith($(compiledHTML));
                this.$(".prev-page").on("click", this.onPrevPageClick.bind(this));
                this.$(".next-page").on("click", this.onNextPageClick.bind(this));
            },
            removeExistingFiltering: function ()
            {
                $(".sort-order", this.$el).html("");
            },
            setFiltering: function (e, orderAsc)
            {
                $("span[class='sort-order']", $(e.currentTarget)).html(orderAsc);
            },
            getSortOrder: function (colName, asc, dsc, col, order)
            {
                var orderAsc = asc;
                if (col === colName) {
                    orderAsc = (order && order === asc) ? dsc : asc;
                }
                return orderAsc;
            },
            setSortingColumnAndOrder: function (colName)
            {
                var order = this.getSortOrder(colName, this.sorting.asc, this.sorting.desc, this.sorting.col,
                        this.sorting.order);
                this.sorting.col = colName;
                this.sorting.order = order;
                return order;
            },
            onColumnHeaderClick: function (e)
            {
                if (!this.sortable) {
                    return;
                }
                var colName = $(e.currentTarget).attr('data-col');
                this.removeExistingFiltering();
                var order = this.setSortingColumnAndOrder(colName);
                this.setFiltering(e, order);
                this.renderTableBody(true);
            },

            sort: function (visibleIndices)
            {
                if (!this.sorting.col) {
                    this.sorting.col = this.data.cols[0];
                }
                return model.sort(visibleIndices, this.sorting.col, this.sorting.order === this.sorting.asc);
            },
            filter: function (visibleIndices, filteringTextPerColumn)
            {
                return model.filterByColumns(visibleIndices, filteringTextPerColumn);
            },
            getVisibleIndices: function (filteringTextPerColumn)
            {
                var tableData = model.getTableData();
                if (!tableData.items || tableData.items.length == 0) {
                    return [];
                }

                var visibleIndices = tableData.items.map(function (val, idx) {
                    return idx;
                });
                if (this.searchable) {
                    visibleIndices = this.filter(visibleIndices, filteringTextPerColumn);
                }
                if (this.sortable) {
                    visibleIndices = this.sort(visibleIndices);
                }
                return visibleIndices;
            },
            getTableContainerRenderData: function () {
                var renderData = {};
                renderData.hasItems = model.getTableData().items && model.getTableData().items.length > 0;
                renderData.cols = model.getTableData().cols;
                renderData.filtering = this.filtering;
                renderData.sorting = this.sorting;
                renderData.sorting.image = this.sorting[this.sorting.order + ""];
                renderData.sortable = this.sortable;
                renderData.searchable = this.searchable;
                renderData.sorting.col = (model.getTableData().layout.attributes)["sorted-by"];
                return renderData;
            },
            render: function ()
            {
                this.setElement(this.container);
                var compiledHTML = _.template(templates.get('tableContainer'))(this.getTableContainerRenderData());
                this.$el.html(compiledHTML);
                setTimeout(function () {
                    this.renderTableBody(true);
                }.bind(this), 1);
            },
            onPrevPageClick: function ()
            {
                this.visibleData.currentPage -= 1;
                this.renderTableBody(false);
            },
            onNextPageClick: function ()
            {
                this.visibleData.currentPage += 1;
                this.renderTableBody(false);
            },
            data: model.get('tableData')
        });

        return new GridView();
    };

});