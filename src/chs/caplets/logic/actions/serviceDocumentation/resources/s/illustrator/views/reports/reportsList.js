/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global LoadMask */
define(["ListView",
            "fileDisplayHandler",
            "SectionCollection",
            "illustrator/collection/reports/reportsList",
            "currentPackage",
            "models/selectedSystem"],
        function (listView,
                fileDisplayHandler,
                BaseCollection,
                reportsList,
                currentPackage,
                selectedSystem) {

            var p = mentor.publisher;
            var CustomView = listView(reportsList).extend({
                title: "Reports",
                cssClass: "Reports",
                doNotLoadOnStart: false,
                expanded: true,

                getClickedData: function (evt, id, config) {
                    config = config || {};
                    var domquerylib = config.domquerylib || $;
                    id = id || domquerylib(evt.currentTarget).attr('data-id');
                    var id = id, content;
                    content = (config.report && config.report(id)) || reportsList.get(id);
                    content.mainText = content.get("mainText");
                    content.path = content.get("path");
                    content.type = "ChangeReport";
                    return content;
                },
                clicked: function (evt, config) {
                    config = config || {};
                    var fdh = config.fileDisplayHandler || fileDisplayHandler;
                    var content = this.getClickedData(evt);
                    fdh.display(content);
                },

                popOut: function (evt, config) {
                    config = config || {};
                    var domquerylib = config.domquerylib || $;
                    evt.stopPropagation();
                    var id = domquerylib(evt.target).parent().attr('data-id');
                    var content = this.getClickedData(evt, id, config);
                    content.type = content.type + "Popout"
                    this.openPopout(content, config);
                },
                openPopout: function (content, config) {
                    config = config || {};
                    var popuphandler = config.popuphandler || p.popoutHandler;
                    var packageid = config.id || currentPackage.get("id") || "";
                    var url = "popout.html#/document/ChangeReport/" +
                            content.mainText + "/" +
                            packageid.replace("\\", "/") + "/" +
                            "searchText" + (currentPackage && currentPackage.get("searchText") ? currentPackage.get("searchText") : "") + "/" +
                            content.path;

                    popuphandler.openPopout(url);
                },
                renderReport: function (result, moduleloader, content) {
                    if (result.data) {
                        moduleloader(["illustrator/views/reports/changeReport"],
                                function (componentChangeReport) {
                                    content.data = result.data;
                                    new componentChangeReport("#splitter3", content).render();
                                    LoadMask.removeLoadMask();
                                });
                    }
                },

                renderCustomReport: function (content, moduleloader) {
                    moduleloader(["illustrator/views/reports/customReport"],
                            function (customReport) {
                                customReport("#splitter3", content).render();
                            }
                    );
                },

                openReport: function (content, config) {
                    config = config || {};

                    var that = this;
                    var dataloader = config.dataloader || p.xmlLoader;
                    var moduleloader = config.moduleloader || require;

                    if (isJsonFile(content.path)) {
                        LoadMask.addLoadMask("changeReport");
                        setTimeout(function () {
                            var report = dataloader.loadFile(content.path, false, false, "json");
                            that.renderReport(report, moduleloader, content);
                        }, 100);
                    }
                    else {
                        this.renderCustomReport(content, moduleloader);
                    }
                }
            });

            var view = new CustomView();
            fileDisplayHandler.addFileHandler("ChangeReport", view.openReport.bind(view));
            fileDisplayHandler.addFileHandler("ChangeReportPopout", view.openPopout.bind(view));
            return view;
        }
);