/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global Backbone, mentor, _, require, define, $*/
define(["text!illustrator/templates/change-report.html",
            "models/selectedSystem",
            "views/contentpanel/toolbar/contentToolBar",
            "currentPackage",
            "models/selectedSystem"],
        function (htmlTemplate,
                currentState,
                Toolbar,
                currentPackage,
                selectedSystem)
        {
            "use strict";
            return function (container, tableData)
            {
                var p = mentor.publisher,
                        view,
                        ChangeReportView = Backbone.View.extend({
                    container: container,
                    summaryTableTemplates: ["text!illustrator/templates/summary-container.html",
                        "text!illustrator/templates/table.html",
                        "text!illustrator/templates/summary-inner.html"],
                    sectionTableTemplates: ["text!illustrator/templates/table-container.html",
                        "text!illustrator/templates/table.html",
                        "text!illustrator/templates/inner-table.html"],
                    modifiedSectionTemplates: ["text!illustrator/templates/table-container.html",
                        "text!illustrator/templates/modified-objs-table.html",
                        "text!illustrator/templates/inner-table.html"],

                    tableView: "illustrator/views/reports/gridView",
                    tableModel: "illustrator/models/reports/gridModel",
                    summaryTableModel: "SummaryGridModel",
                    data: tableData.data,
                    title: tableData.mainText,
                    reportId: tableData.id,
                    tableEventHandler: {
                        objectInteractionInterface: {
                            showPopover: function (uid, x, y)
                            {
                                p.eventDispatcher.dispatchEvent(p.events.OPEN_OBJECT_POPUP,
                                        {
                                            id: uid,
                                            x: x,
                                            y: y,
                                            systemId: ""
                                        });

                            },
                            highlightObject: function (uid)
                            {
                                p.eventDispatcher.dispatchEvent(p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                                    objectId: uid,
                                    systemId: ""
                                });
                            },
                            objectHighlighter: window.crossHighlightHandler,
                            dataLoader: p.xmlLoader,
                            zoomToObject: function (waitBeforeInvoke)
                            {
                                var that = this;
                                setTimeout(function ()
                                {
                                    var config = that.dataLoader.loadFile("config.json", false, true, "json");
                                    if (config && config.data) {
                                        config = config.data['autoZoomOnClick'];
                                    }
                                    if (config) {
                                        that.objectHighlighter.zoomViews();
                                    }

                                }, waitBeforeInvoke);

                            }
                        },
                        onCellClick: function (cellEle, e)
                        {
                            var xref,
                                    content,
                                    OLD_DESIGN_REVISION = p.contentType.OLD_DESIGN_REVISION,
                                    NEW_DESIGN_REVISION = p.contentType.NEW_DESIGN_REVISION;

                            var uid = $(cellEle).attr('data-objectId');
                            var layoutId = $(cellEle).attr('data-layoutId');
                            var isHighlighted = $(cellEle).attr('data-highlighted');
                            var that = this;
                            if (uid) {
                                content = {
                                    systemId: layoutId,
                                    objectId: uid
                                };
                                if (layoutId && !(isHighlighted === "true") && !p.crossReferenceHandler.isInOpenDiagrams(content)) {
                                    xref = p.crossReferenceHandler.fetchFirstCrossReference(content);
                                    if (xref) {
                                        var type = (xref.illustratorDesignType == 'old') ? OLD_DESIGN_REVISION :
                                                NEW_DESIGN_REVISION;
                                        var content = {
                                            listItemId: xref.id,
                                            layoutId: xref.id,
                                            id: xref.diagramId,
                                            objectId: uid,
                                            reset: false,
                                            type: type,
                                            group: p.documentCategory.DIAGRAMS,
                                            doNotSaveAsHistory: true,
                                            avoidRenderingIfOpen: true
                                        };
                                        view.moduleLoader(["fileDisplayHandler"],
                                                function (fileDisplayHandler)
                                                {
                                                    fileDisplayHandler.display(content);
                                                    that.objectInteractionInterface.zoomToObject(1500);
                                                });
                                    }
                                }

                                if (isHighlighted === "true" || e.ctrlKey) {
                                    this.objectInteractionInterface.showPopover(uid, e.pageX, e.pageY);
                                }
                                this.objectInteractionInterface.highlightObject(uid);
                                this.objectInteractionInterface.zoomToObject(1000);
                                e.stopPropagation();
                            }
                        }
                    },
                    initialize: function ()
                    {
                        currentPackage.on("change:language", this.render, this);
                        currentPackage.on("change:searchText", this.render, this);
                        currentPackage.on("change:id", this.render, this);
                        currentState.on("change:objectId", this.highlightCells, this);
                    },

                    events: {
                        "click .section-header": "collapseTable",
                        "click .popOutBtn ": "openPopout",
                        "click .closeBtn": "close",
                        "click .restoreBtn": "setReportSectionHeight",
                        "click .maximizeBtn": "setReportSectionHeight"
                    },
                    openPopout: function ()
                    {
                        var content = {
                            mainText: tableData.mainText,
                            path: tableData.path,
                            searchText: currentPackage.get("searchText"),
                            type: tableData.type + "Popout"
                        };
                        this.moduleLoader(["fileDisplayHandler"], function (fileDisplayHandler)
                        {
                            fileDisplayHandler.display(content);
                        });

                    },
                    collapseTable: function (e)
                    {
                        var ele = $(e.currentTarget).parent();
                        $(".expand-section", ele).toggle();
                        $(".collapse-section", ele).toggle();
                        $(".change-table", ele).animate({
                            height: "toggle"
                        }, {
                            duration: 150
                        });
                        e.stopPropagation();
                    },
                    close: function ()
                    {
                        selectedSystem.trigger("change:clearNavigationPanelSelection", {id: this.reportId});
                        this.undelegateEvents();
                        this.$el.html('');
                    },
                    htmlTemplate: htmlTemplate,
                    renderToolBar: function (toolbar)
                    {
                        this.$el.append(
                                toolbar.render({title: this.title, type: p.contentType.CUSTOM_VIEW}).$el);
                    }, render: function ()
                    {
                        this.undelegateEvents();
                        this.$el.html('');
                        this.setElement(this.container);
                        p.contentArea.closeExistingPanel({type: p.contentType.CUSTOM_VIEW}, this);
                        var toolbar = new Toolbar();
                        var html = _.template(this.htmlTemplate)({
                            sections: this.data
                        });
                        this.renderToolBar(toolbar);
                        this.$el.append(html);
                        p.contentArea.layoutContentPanel(
                                {type: p.contentType.CUSTOM_VIEW, title: this.title, id: this.reportId});
                        setTimeout(this.loadTables.bind(this), 1000);
                    },
                    registerRemoveHighlightedRowsEvent: function () {
                        $(document).on("click", function (event) {
                            if (!$(event.target).closest('tbody').length) {
                                $("td[data-highlighted='true']", this.$el).removeAttr("style data-highlighted");
                                require(["currentPackage", "models/selectedSystem"],
                                function (currentPackage, selectedSystem) {
                                        selectedSystem.set("objectId", "");
                                });
                            }
                        });
                    },
                    highlightCells: function ()
                    {
                        //todo remove hardcoding
                        var dataId = currentState.get("objectId");
                        if (dataId) {
                            $("td[data-highlighted='true']", this.$el).attr("style", "");
                            $("td[data-highlighted='true']", this.$el).attr("data-highlighted", "");
                            var tdsWithDataId = $(".grid-ui td[data-objectId=" + dataId + "]", this.$el);
                            $(tdsWithDataId).each(function ()
                            {
                                $(this).attr("style", "background-color:#EEEE99;");
                                $(this).attr("data-highlighted", "true");
                            });
                        }
                    },

                    getTablesData: function ()
                    {
                        return this.data;
                    }, loadTables: function ()
                    {
                        _.each(this.getTablesData(), function (module)
                        {
                            //todo code smell?
                            switch (module.layout.type) {
                                case 'table':
                                    this.loadSectionTable(module);
                                    break;

                                case 'table-with-pairs':
                                    this.loadSectionTableWithPairs(module);
                                    break;

                                case 'summary':
                                    this.loadSummaryTable(module);
                                    break;

                                default:
                            }
                        }, this);

                        setTimeout(function ()
                        {
                            this.setReportSectionHeight();
                            this.setResizeCallbacks();
                            this.registerRemoveHighlightedRowsEvent();
                            this.highlightCells();
                        }.bind(this), 1000);
                    },
                    moduleLoader: require,
                    loadTable: function (templates, view, model, tableData, tableEventHandler)
                    {
                        var modules = templates.concat([view, model]);
                        this.moduleLoader(modules,
                                function (tableContainer, table, innerTable, TableView, TableDataModel)
                                {
                                    var tableDataModel = new TableDataModel({
                                        tableData: tableData
                                    });

                                    if (tableData.layout.attributes.searchable != undefined) {
                                        tableDataModel.set('searchable', tableData.layout.attributes.searchable);
                                    }

                                    if (tableData.layout.attributes.sortable != undefined) {
                                        tableDataModel.set('sortable', tableData.layout.attributes.sortable);
                                    }

                                    var gridTemplates = new Backbone.Model({
                                        tableContainer: tableContainer,
                                        table: table,
                                        innerTable: innerTable
                                    });
                                    var view = new TableView(tableDataModel, gridTemplates, tableEventHandler);
                                    if (currentPackage.hasChanged("searchText") || !currentPackage.get("searchText")) {
                                        var tData = tableDataModel.get('tableData');
                                        if (tData) {

                                            var cols = tableDataModel.get('tableData').cols;
                                            if (cols && cols.length > 0) {

                                                cols.forEach(function (key, index)
                                                {
                                                    view.filtering[key] = "";
                                                });
                                                view.filtering['searchAllCols'] = currentPackage.get("searchText");
                                            }
                                        }
                                    }
                                    view.render();
                                    // delete view.filtering['searchAllCols'];
                                });
                    },
                    loadSummaryTable: function (summary)
                    {
                        this.loadTable(this.summaryTableTemplates,
                                this.tableView,
                                this.summaryTableModel,
                                summary);
                    },
                    setResizeCallbacks: function ()
                    {
                        var that = this;
                        var onCloseBtnClick = function (evt)
                        {
                            if ($(evt.target).hasClass("closeBtn")) {
                                that.setReportSectionHeight();
                            }

                        };
                        $("#detail").off("click", onCloseBtnClick);
                        $("#detail").on("click", onCloseBtnClick);
                        selectedSystem.off("harnessDiagramOpened", this.setReportSectionHeight);
                        selectedSystem.on("harnessDiagramOpened", this.setReportSectionHeight);

                        $(window).off("resize", this.setReportSectionHeight);
                        $(window).on("resize", this.setReportSectionHeight);
                        $("#horizontalResizebar").off("mousemove", this.setReportSectionHeight);
                        $("#detailNavigationResizeBar").off("mousemove", this.setReportSectionHeight);
                        $("#detail").off("mousemove", this.setReportSectionHeight);
                        $("#detail").on("mousemove", this.setReportSectionHeight);
                        $("#horizontalResizebar").on("mousemove", this.setReportSectionHeight);
                        $("#detailNavigationResizeBar").on("mousemove", this.setReportSectionHeight);
                        // $("#horizontalResizebar").on("mouseup", this.setReportSectionHeight);
                    },
                    loadSectionTable: function (tableSection)
                    {
                        this.loadTable(this.sectionTableTemplates,
                                this.tableView,
                                this.tableModel,
                                tableSection,
                                this.tableEventHandler);

                    },
                    loadSectionTableWithPairs: function (tableSection)
                    {
                        this.loadTable(this.modifiedSectionTemplates,
                                this.tableView,
                                "illustrator/models/reports/modifiedObjects",
                                tableSection,
                                this.tableEventHandler);

                    },
                    setReportSectionHeight: function ()
                    {

                        var totalHeight = $("#splitter3").height();
                        var margin = $("#changeReport").css("margin-top");
                        if (margin) {
                            var toolBarHeight = margin.replace("px", "");
                            var reportSectionHeight = totalHeight - toolBarHeight;
                            $("#changeReport").css("max-height", reportSectionHeight + "px");
                        }

                    }
                });
                view = new ChangeReportView();
                return view;
            };

        });