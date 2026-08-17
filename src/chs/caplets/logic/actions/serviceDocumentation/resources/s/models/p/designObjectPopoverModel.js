/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("DesignObjectPopoverModel",
    ["PopoverModel", "SignalTracerModel", "models/selectedSystem", "AttributesCollection",
        "ConnectorFaceviewsCollection", "CustomDataCollection", "ThreeDViewCollection", "TwoDLocationCollection",
        "XRefsCollection", "XRefActiveConfigModel", "collections/p/groundPathCollection",
        "collections/harnessPopoverColl"],
    function (PopoverModel, signalTracerModel, selectedSystem, attributes, faceViews, customCollection, threeDViews,
        twoDViews, xrefs, xRefActiveConfigModel, groundPathCollection, harnessLayoutCollection)
    {
        "use strict";
        var p = mentor.publisher, DesignObjectPopoverModel = PopoverModel.extend({
            popoverPanels: [],
            addPopoverPanel: function (popoverPanel)
            {
                this.popoverPanels.push(popoverPanel);
            },
            initialize: function ()
            {
                PopoverModel.prototype.initialize.call(this);
                //DesignObjectPopoverModel.__super__.initialize();
                xRefActiveConfigModel.on("change:config", this.fetchXrefCollection, this);
                //console.log("Initializing view of DesignObjectPopoverModel");
            },
            isValidEvent: function (data)
            {
                var systemId = (data && data.systemId) || selectedSystem.get("systemId") || (data && data.id);
                return systemId;
            },
            isDynamicNavigationActive: function ()
            {
                var windowObj = window;
                if (window.opener && window.opener.mentor) {
                    windowObj = window.opener;
                }
                return windowObj.mentor.publisher.configurationsManager.isDynamicNavigationActive();
            },
            getCurrentProject: function ()
            {
                return mentor.publisher.project;
            },
            loadData: function (data)
            {
                var designObject, systemId = data.systemId || selectedSystem.get("systemId");
                designObject = this.getCurrentProject().loadObjectData(systemId, data.id);
                if (designObject && designObject.getSignalTraceFiles) {
                    signalTracerModel.update(designObject.getSignalTraceFiles(), data.id, systemId);
                }
                this.set("showFilter", this.isDynamicNavigationActive());
                this.set("showXrefBuilderButton",
                    this.isDynamicNavigationActive());
                return designObject;
            },
            getPopoverOrder: function ()
            {
                return mentor.publisher.dataLoader.getPopoverOrder();
            },

            loadCollections: function (model)
            {
                var popoverPanelsOrderMap = this.getPopoverOrder(), index, type = model.getType ?
                    model.getType() : "", popoverPanelsOrder;

                // DeviceConnector dont have popover order, so adding Connector as a fallback to it.
                if (type === "DeviceConnector"){
                    type = "Connector";
                }

                if (popoverPanelsOrderMap) {
                    popoverPanelsOrder = popoverPanelsOrderMap[type];
                }
                var noOfItems = 0, length = popoverPanelsOrder ? popoverPanelsOrder.length :
                    0, coll, collItems, firstPanelFound;
                if (popoverPanelsOrder) {
                    //todo the text in the popover panel order is not same as the i8n text, should both be the same?
                    //todo the below code does not adhere to open/closed principle, need to find a better way to handle
                    // this
                    for (index = 0; index < length; index++) {
                        if (popoverPanelsOrder[index] === "Attributes") {
                            coll = attributes;
                        }
                        else if (popoverPanelsOrder[index] === "Links") {
                            coll = xrefs;
                        }
                        else if (popoverPanelsOrder[index] === "FaceViews") {
                            coll = faceViews;
                        }
                        else if (popoverPanelsOrder[index] === "CustomData") {
                            coll = customCollection;
                        }
                        else if (popoverPanelsOrder[index] === "LocationView") {
                            coll = threeDViews;
                        }
                        else if (popoverPanelsOrder[index] === "TwodViews") {
                            coll = twoDViews;
                        }
                        else if (popoverPanelsOrder[index] === "ShowGroundAndPowerSignal") {
                            coll = groundPathCollection;
                        }
                        else if (popoverPanelsOrder[index] === "HarnessLayout") {
                            coll = harnessLayoutCollection;
                        }
                        else {
                            var expand = false, totalWires;
                            if (noOfItems === 0) {
                                expand = true;
                                firstPanelFound = true;
                            }
                            var sectionColl = p.designObjectPopover.createDesignObjectSection(popoverPanelsOrder[index],
                                model, {showPopoutBtn: false, expand: expand, async: false});
                            if (sectionColl && sectionColl.collection) {
                                var sectionCollItems = sectionColl.collection.fetch(model) || [];
                                noOfItems += sectionCollItems.length;
                            }

                        }
                        if (coll) {
                            //if the firstpanel is already found, then make the rest of the panels not expanded
                            if (firstPanelFound ||
                                mentor.publisher.config['collapseAllPanelsInObjectPopover'] === 'true') {
                                coll.expand = false;
                            }

                            collItems = (coll.fetch(model) || []);
                            //except for the first item in the popover, rest should not be shown expanded
                            if (!firstPanelFound && collItems.length > 0) {
                                firstPanelFound = true;
                            }
                            //at the end set it back to true
                            coll.expand = true;
                            noOfItems = collItems.length + noOfItems;

                        }
                        coll = null;
                    }
                }
                else {
                    //todo why the order will not be define
                    //if the order is not given, then show in default order
                    noOfItems = (attributes.fetch(model) || []).length + noOfItems;
                    //attributes.expand = false;
                    noOfItems = (xrefs.fetch(model) || []).length + noOfItems;
                    noOfItems = (faceViews.fetch(model) || []).length + noOfItems;
                    noOfItems = (customCollection.fetch(model) || []).length + noOfItems;
                    noOfItems = (threeDViews.fetch(model) || []).length + noOfItems;
                    noOfItems = (twoDViews.fetch(model) || []).length + noOfItems;
                    noOfItems = (groundPathCollection.fetch(model) || []).length + noOfItems;
                    noOfItems = (harnessLayoutCollection.fetch(model) || []).length + noOfItems;
                }
                if (!noOfItems) {
                    //The signal tracer get reset on click of an object
                    //and once object data is loaded, the signal tracer model is updated
                    //now if there are no items to show, we SHOULD NOT reset the signal tracer model.
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER,
                        {doNotResetSignalTracer: true});
                }

            },
            fetchXrefCollection: function ()
            {
                xrefs.fetch(this._internalModel);
            }
        }), designObjectPopoverModel, call = function (evt)
        {
            if (evt.detail && evt.detail.id && evt.detail.systemId) {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                    objectId: evt.detail.id,
                    systemId: evt.detail.systemId
                });
            }
            designObjectPopoverModel.loadPopoverData(evt);
        };
        designObjectPopoverModel = new DesignObjectPopoverModel();
        mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.OPEN_OBJECT_POPUP, call);
        return _.extend(designObjectPopoverModel, Backbone.Events);
    });

