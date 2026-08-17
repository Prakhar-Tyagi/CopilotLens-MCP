/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("LocationViewObjectPopoverModel",
        ["backbone", "PopoverModel", "SignalTracerModel", "XRefsCollection", "XRefsCollectionItem", "XRefsViewItem",
            "text!templates/p/basicListItemTemplate.html"],
        function (Backbone, PopoverModel, signalTracerModel, xrefCollection, XRefsCollectionItem, XRefsViewItem,
                xrefsTemplate) {
            "use strict";
            var LocationViewObjectPopoverModel = PopoverModel.extend({
                isValidEvent: function (data) {
                    return !signalTracerModel.altClickRender;
                },
                isDynamicNavigationActive: function () {
                    var windowObj = window;
                    if (window.opener && window.opener.mentor) {
                        windowObj = window.opener;
                    }
                    return windowObj.mentor.publisher.configurationsManager.isDynamicNavigationActive();
                },
                getCurrentProject: function () {
                    return mentor.publisher.project;
                },
                updateSignalTracerModel: function (firstTwoDObject) {
                    var firstDesignObject, objectId, systemId;
                    objectId = firstTwoDObject.objectId || firstTwoDObject.get("objectId");
                    systemId = firstTwoDObject.systemId || firstTwoDObject.get("systemId");
                    firstDesignObject =
                            this.getCurrentProject().loadObjectData(systemId, objectId);
                    if (firstDesignObject.getSignalTraceFiles) {
                        signalTracerModel.update(firstDesignObject.getSignalTraceFiles(), objectId,
                                systemId);
                    }
                },
                loadData: function (data) {
                    var xrefs, firstTwoDObject, firstDesignObject, objectId, systemId, selectedObjects = data.matches;
                    xrefs = flattenMapValues(selectedObjects);
                    if (xrefs && xrefs.length > 0) {
                        firstTwoDObject = xrefs[0] || {};
                        this.updateSignalTracerModel(firstTwoDObject);
                    }
                    this.set("showFilter", true);
                    this.set("showXrefBuilderButton", this.isDynamicNavigationActive());
                    this.doFetch('', [], xrefCollection);
                    data.getName = function () {
                        //if there are multiple matches, show the title as this
                        if (selectedObjects.size > 1) {
                            return mentor.publisher.languageTranslator.localize("multipleLinks.label");
                        }
                        return data.name;
                    };
                    return data;
                },
                loadCollections: function (model) {
                    var selectedObjects = model.matches;
                    if (selectedObjects.size == 1) {
                        var iterator = selectedObjects.entries();
                        var next = iterator.next();
                        var name = next.value[0];
                        var xrefsModel = next.value[1];
                        this.doFetch(name, xrefsModel, xrefCollection);
                    }
                    else {
                        var fetcher = this;
                        var linkIndex = 0;
                        selectedObjects
                                .forEach(function (value, key) {
                                    var XRefCollItem = XRefsCollectionItem.extend({});
                                    var coll = new XRefCollItem();
                                    var XrefItem = XRefsViewItem(coll).extend({});
                                    var view = new XrefItem({
                                        el: '#popover-grouped-list',
                                    });
                                    view.title = key;
                                    view.className = view.className + '-' + (linkIndex++);
                                    view.container = '#popover-grouped-list';
                                    view.templateHTML = xrefsTemplate;
                                    fetcher.doFetch(key, value, coll);
                                });
                    }
                },
                doFetch: function (match, modelData, xrefsColl) {
                    if (mentor.publisher.config['collapseAllPanelsInObjectPopover'] === 'true') {
                        xrefsColl.expand = false;
                    }
                    xrefsColl.fetch({
                        getName: function () {
                            return match;
                        },
                        getCrossReferences: function () {
                            return {
                                listItems: modelData
                            };
                        }
                    });
                }
            }), locationViewObjectPopoverModel, call = function (evt) {
                locationViewObjectPopoverModel.loadPopoverData(evt);
            };
            locationViewObjectPopoverModel = new LocationViewObjectPopoverModel();
            mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.SHOW_POP_OVER_2D_VIEW, call);
            return _.extend(locationViewObjectPopoverModel, Backbone.Events);
        });

