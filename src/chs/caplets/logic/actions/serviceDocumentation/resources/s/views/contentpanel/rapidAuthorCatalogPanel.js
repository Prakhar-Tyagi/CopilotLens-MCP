/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
define(["backbone", "underscore", "currentPackage", "views/contentpanel/toolbar/contentToolBar",
        "ra3DModel"],
    function (Backbone, _, currentPackage, Toolbar, ra3DModel) {
        var p = mentor.publisher;
        var interactive_xml = ".interactivity.xml";
        var SELECTION_MODE_ITEM = "item";
        var SELECTION_MODE_ROW = "row";
        var RapidAuthorCatalogPanel = Backbone.View.extend({
            events: {
                "click .popOutBtn": "showPopout",
                "click .closeBtn": "close"
            },

            getSelectedItems: function () {
                return this.model.get("selectedItems");
            },
            setSelectedItems: function (items) {
                this.model.set("selectedItems", items);
            },
            clearSelectedItems: function () {
                this.model.set("selectedItems", []);
            },
            addSelectedItem: function (item) {
                if (!this.model.get("selectedItems")) {
                    this.clearSelectedItems();
                }
                this.model.get("selectedItems").push(item);
            },
            hasSelection: function () {
                return this.getSelectedItems() && this.getSelectedItems().length;
            },

            getPath: function () {
                return this.model.get("path");
            },

            getContentType: function () {
                return this.model.get("type");
            },

            getModelName: function () {
                // return this.modelName;
                return this.model.get("modelName");
            },
            setModelName: function (name) {
                this.model.set("modelName", name);
            },

            getModelPath: function () {
                return this.model.get("modelPath");
            },
            setModelPath: function (path) {
                this.model.set("modelPath", path);
            },

            getModelUrl: function () {
                return this.model.get("modelUrl");
            },
            setModelUrl: function (url) {
                this.model.set("modelUrl", url);
            },

            getMainText: function () {
                return !this.model ? "" : this.model.get("mainText");
            },

            initialize: function () {
                currentPackage.on("change:projectId", this.close, this);
            },

            getPagePathname: function () {
                return window.location.pathname;
            },

            openObjectThreeD: function (content) {
                if (this.model) {
                    this.removeEventHandlers();
                }

                this.model = content;

                var pathname = this.getPagePathname();
                //make pathname absolute
                if (pathname.length === 0 || pathname.charAt(0) !== '/') {
                    pathname = '/' + pathname;
                }
                var pathEnd = pathname.length;

                var path = this.getPath().replace(/\\/g, '/');

                //The trailing "ipc" is specific to Catalog views
                var rapidAuthorIpc = 'RapidAuthor/ipc';
                var tailLength = rapidAuthorIpc.length + 1;

                if (path.indexOf(interactive_xml) === -1) {
                    this.setModelName(path.substring(path.lastIndexOf(rapidAuthorIpc) + tailLength));
                } else {
                    var lastIndex = path.lastIndexOf(rapidAuthorIpc);
                    var nextIndex = path.indexOf('/', lastIndex + tailLength);
                    this.setModelName(path.substring(lastIndex + tailLength, nextIndex));
                }
                if (path.match("^" + pathname) === null) {
                    pathname = pathname.replace("index.html", "").replace("index1.html", "").replace("popout.html", "");
                    pathEnd = pathname.length;
                    if (path.match("^" + pathname) === null) {
                        pathEnd = 0;
                        //try without leading /
                        if (path.match("^" + pathname.substr(1)) !== null) {
                            pathEnd = pathname.length - 1;
                        }
                    }
                }

                this.setModelPath(
                    path.substring(pathEnd, path.indexOf(this.getModelName()) + this.getModelName().length));

                this.setModelUrl(Utils.prepareFilePath(
                    pathname +
                    this.getModelPath() + "/" +
                    this.getModelName() +
                    interactive_xml));
                this.render();
            },

            removeEventHandlers: function () {
                this.undelegateEvents();
                if (p.solo) {
                    p.solo.removeAllListeners();
                }
            },

            close: function () {
                this.$el.html("");
                this.model = undefined;
                ra3DModel.sheetViews = null;
                this.removeEventHandlers();
                p.detailLayoutManager.refreshContentToolbars();
            },

            loadSheetViews: function (sheets) {
                ra3DModel.sheetViews = sheets;
            },

            showPopout: function (event) {
                var data = this.model;
                var objectId = data.get("objectId") || "";
                var modified = objectId.replace(/\//g, "___");
                p.popoutHandler.openPopout("popout.html#/ra3DXML/" +
                    data.get("mainText") + "/" +
                    currentPackage.get("id").replace("\\", "/") + "/" +
                    modified +
                    this.getModelUrl().replace("\\", "/"));
            },

            /**
             * Handler for Cortona 3D Item Selected events
             * @param modelItem The selected RA Model Item Name
             */
            selectMatchedItem: function (dx, dy, itemName) {
                var that=this;
                if (itemName) {
                    this.clearIPCTable();

                    //remember selection
                    this.setSelectedItems([itemName]);
                    var objDataArray = ra3DModel.getSystemDataForRapidAuthorItemInstance(itemName,
                        this.getModelPath());

                    var objectDataMap = new Map();

                    if (objDataArray.length > 0) {
                        objectDataMap.set(itemName, objDataArray);
                        this.displayRapidAuthorAttribs(dx, dy, objectDataMap, itemName, SELECTION_MODE_ITEM);
                    }
                    else {
                        p.eventDispatcher.dispatchEvent(p.events.CLOSE_POPOVER);
                    }
                    setTimeout(function () {
                        that.highlightItems([itemName]);
                    }, 200);
                    window.crossHighlightHandler.crossHighlightInRapidAuthorViews([itemName],
                        document, true);
                }
            },

            /**
             * Handler for Cortona Row Selected events
             * @param modelItem The selected RA Model Item
             */
            selectMatchedRow: function (dx, dy, modelItem) {
                if (modelItem) {
                    this.clearIPCTable();

                    //remember selection
                    this.setSelectedItems(modelItem.objectNames);

                    var objDataArray = ra3DModel.getSystemDataForRapidAuthorItem(modelItem, this.getModelPath());

                    var objectDataMap = new Map();

                    if (objDataArray.length > 0) {
                        objectDataMap.set(modelItem.part.metadata.DFP, objDataArray);
                        this.displayRapidAuthorAttribs(dx, dy, objectDataMap, modelItem.part.metadata.DFP, SELECTION_MODE_ROW);
                    }
                    var that = this;
                    setTimeout(function () {
                        that.highlightItems(modelItem.objectNames);
                    }, 200);

                    window.crossHighlightHandler.crossHighlightInRapidAuthorViews(modelItem.objectNames,
                            document, true);
                }
            },

            enable3DViewSelectionToolbar: function () {
                this.$el.find('object').contents().find('#toolbar-part-selection').removeClass('disabled');
                this.$el.find('object').contents().find('#toolbar-part-selection > input:disabled').prop('disabled', false);
            },

            selectInIPC: function (itemName, recursiveCounter = 0) {
                var index = this.objectToRow(itemName);
                if (index !== -1) {
                    if(p.solo.app.ipc.getCurrentSheetItems) {
                        //remember selection
                        this.setSelectedItems([itemName]);

                        //ensure selection toolbar is enabled
                        //this.enable3DViewSelectionToolbar();

                        p.solo.app.ipc.selectItem(index);
                        p.solo.dispatch('catalog.didSelectItem', index);
                        p.solo.dispatch('catalog.didChangeSelection', p.solo.app.ipc.selectedItems);
                    }
                    else {
                        if(recursiveCounter < 10) {
                            var that = this;
                            setTimeout(function () {
                                that.selectInIPC(itemName, recursiveCounter + 1);
                            }, 400);
                        }
                    }
                }
            },

            /**
             * Highlight capital related objects open in other windows
             * @param item The RA Model Item value
             */
            doHoverItem: function (item) {
                var that = this;
                setTimeout(function() {
                    //Don't X-highlight if IPC has selection
                    if (item && !that.hasSelection()) {
                        //get the related Capital System Paths to this model item
                        var systemPaths = ra3DModel.getSystemDataForRapidAuthorItem(item, that.getModelPath());
                        if (systemPaths && systemPaths.length > 0) {
                            //clear previous
                            that.clearCrossHighlight();

                            p.eventDispatcher.dispatchEvent(
                                p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                                    objectId: systemPaths[0]["objectId"],
                                    systemId: systemPaths[0]["systemId"]
                                });
                        }
                    }
                    else if (item) {
                        //remove hover from selected row
                        var row = that.$el.find('object').contents().find('#dpl-table tr[id="row' + item.row + '"]').removeClass('hover');
                    }
                },200);
            },

            doHoverObject: function(itemName,handle){
                var that = this;
                setTimeout(function() {
                    //Don't X-highlight if IPC has selection
                    if (itemName && !that.hasSelection()) {
                        //get the related Capital System Paths to this model item
                        var systemPaths = ra3DModel.getSystemDataForRapidAuthorItemInstance(itemName,
                                that.getModelPath());
                        if (systemPaths && systemPaths.length > 0) {
                            //clear previous
                            that.clearCrossHighlight();
                            p.solo.app.setHoveredObjects(handle);

                            p.eventDispatcher.dispatchEvent(
                                    p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                                        objectId: systemPaths[0]["objectId"],
                                        systemId: systemPaths[0]["systemId"]
                                    });
                        }
                    }
                    else if (itemName) {
                        //remove hover from selected row
                        var row = that.$el.find('object').contents().find('#dpl-table tr[id="row' + itemName.row + '"]').removeClass('hover');
                    }
                },200);

            },

            onLoad: function (view3d) {

                if (view3d.get("objectId") && '__objectId__' !== view3d.get("objectId")) {

                    //select in IPC
                    var that = this;
                    setTimeout(function () {
                        var name = view3d.get("objectId");
                        that.zoomToFitObjectNames([name]);
                        that.selectInIPC(name);
                    }, 500);
                } else {
                    var selectedSystem = require("models/selectedSystem");
                    var connUID = selectedSystem.get("objectId");
                    if (connUID) {
                        var raItemNames = ra3DModel.getItemNamesForSystemId(connUID, this.getModelPath());
                        var uniqueNames = _.uniq(raItemNames);
                        if(uniqueNames.length) {
                            view3d.set("objectId", uniqueNames[0]);
                            this.onLoad(view3d);
                        }
                    }
                }
            },

            /**
             * Respond to Cross Zoom event
             * @param connUID The capital ConnUID of the source object
             */
            zoomObjects: function (connUID) {
                var raItemNames = ra3DModel.getItemNamesForSystemId(connUID, this.getModelPath());
                var uniqueNames = _.uniq(raItemNames);
                this.zoomTo3dModel(uniqueNames);
            },

            getValidHandles: function (itemNames, sheetId) {
                var that=this;
                var handles = _.filter(_.uniq(_.map(itemNames, function (name) {
                    //ensure any linked view (sheet) is selected
                    var handle = p.solo.app.getObjectWithName(name);
                    if (handle) {
                        //lookup sheet if object in this model
                        if (!sheetId) {
                            sheetId = that.getSheetForItemName(name);
                        }
                    }
                    return handle;
                })), function (handle) {
                    //not in this model
                    return handle !== 0;
                });
                return {sheetId, handles};
            },

            zoomToFitObjectNames: function (itemNames) {
                that = this;

                var handles = this.deriveHandles(itemNames,true);

                //Select and Zoom
                setTimeout(function () {
                    p.solo.app.setSelectedObjects(handles[0], true);
                    p.solo.app.fitObjectsInView(handles[0], true);
                }, 200);
            },

            //get the sheet the item is specified for or the first sheet if not set
            getSheetForItemName: function (name) {
                var defaultSheet = null;
                var requiredSheet = null;
                var dplRow = p.solo.app.ipc.interactivity.getRowByObjectName(name);
                _.each(ra3DModel.sheetViews, function (sheet, id, list) {
                    if (!defaultSheet) {
                        defaultSheet = sheet.id;
                    }
                    if (!requiredSheet && sheet.items && sheet.items.includes(dplRow)) {
                        requiredSheet = sheet.id;
                    }
                });
                return requiredSheet || defaultSheet;
            },

            displayRapidAuthorAttribs: function (dx, dy, objDataMap, itemName, selectedMode) {
                var windowOffset = this.$el.offset();
                var viewerPortOffset = $('#ramodel').offset();
                var finalOffset = selectedMode == SELECTION_MODE_ROW ? windowOffset : viewerPortOffset;
                var offsetX = dx + finalOffset.left + 50;
                var offsetY = dy + finalOffset.top + 50;
                var uidToHighlight = flattenMapValues(objDataMap)[0].connUID;
                display2DViewsAttributes(itemName, offsetX, offsetY, uidToHighlight, objDataMap);
            },

            setToolbar: function (toolbar, title) {
                toolbar = new Toolbar();
                this.$el.append(toolbar.render({
                    type: p.contentType.CUSTOM_VIEW,
                    title: title,
                    allowsPrinting: false
                }).$el);
            },

            render: function () {
                var that = this, containerId = this.container;
                var template, title, toolbar,
                        objectId;

                if (this.model && this.getModelUrl()) {

                    var effSetter = require("filehandlers/effectivitySetter");
                    effSetter.setEffectivityInCookies();

                    this.setElement(containerId);

                    title = this.model.get("mainText");
                    objectId = this.model.get("objectId");

                    p.contentArea.closeExistingPanel(
                            {type: p.contentType.CUSTOM_VIEW},
                            this);

                    this.setToolbar(toolbar, title);

                    if (!this.templateHTML) {
                        this.templateHTML = require("text!templates/cp/RA3DTemplate.html");
                    }
                    template = _.template(this.templateHTML)({
                        path: this.getModelUrl(),
                        memory: Utils.is_mobile_device() ? 128 : 256, //C3D recommended setting of 128 to avoid OOM on mobile devices
                        title: title,
                        objectId: objectId,
                        type: this.getContentType()
                    });

                    this.$el.append(template);

                    setTimeout(function () {
                        p.contentArea.layoutContentPanel({
                            type: that.getContentType(),
                            title: title,
                            mainText: title,
                            id: that.model.get("id"),
                            objectId: that.objectId,
                            path: that.getModelUrl()
                        });
                    }, 100);

                }
                return this;
            },

            layoutContentPanel: function () {
                this.onLoad(this.model);
            },

            /**
             * Respond to Cross Highlight event
             * @param raItemNames An array of Rapid Author item object (node) names
             */
            highlightItems: function (raItemNames) {
                var uniqueNames = _.uniq(raItemNames);

                this.clearSelectionInIPC(uniqueNames);
                this.highlight3dModel(uniqueNames);
                this.selectRowInIPC(uniqueNames);
            },

            clearSelectionInIPC: function (uniqueNames) {

                // Clear current selection if different otherwise leave IPC alone as popup may be showing
                if (this.hasSelection() && !_.intersection(this.getSelectedItems(), uniqueNames)) {
                    this.clearIPCTable();
                }
            },

            selectRowInIPC: function (itemNames) {
                //Select in IPC
                var that = this;
                _.each(itemNames, function (itemName, i, list) {
                    that.selectInIPC(itemName);
                });
            },

            zoomTo3dModel: function (uniqueNames) {

                this.clearSelectionInIPC(uniqueNames);
                this.zoomToFitObjectNames(uniqueNames);
                this.selectRowInIPC(uniqueNames);
            },

            highlight3dModel: function (itemNames) {

                var handles = this.deriveHandles(itemNames,false);

                setTimeout(function () {
                    p.solo.app.setSelectedObjects(handles, true);
                }, 200);

            },

            deriveHandles: function (itemNames,isZoom) {
                var sheetId;

                //derive handle from name returning unique set and filtering any names not in this model
                var result = this.getValidHandles(itemNames, sheetId);
                sheetId = result.sheetId;
                if (sheetId && sheetId !== p.solo.app.ipc.currentSheetInfo.id && isZoom) {
                    p.solo.app.ipc.setCurrentSheet(sheetId);
                }
                return result.handles;
            },

            clearCrossHighlight: function () {
                p.eventDispatcher.dispatchEvent(
                        p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                            objectId: null,
                            systemId: null
                        });
            },

            clearIPCTable: function () {
                if (this.hasSelection) {
                    //sync other windows
                    this.clearCrossHighlight();
                }
                this.clearSelectedItems();

                p.eventDispatcher.dispatchEvent(p.events.CLOSE_POPOVER);
            },

            clearHighlighting: function () {
                //Clear 3D selections
                p.solo.app.setSelectedObjects([], true);

                //Clear IPC selections
                this.clearIPCTable();
            },

            objectToRow: function (name) {
                var row = p.solo.app.ipc.interactivity.getRowByObjectName(name);
                return p.solo.app.ipc.interactivity.getIndexByRow(row);
            },

            /**
             * Mirror zoom in related diagrams and other open 3D views
             */
            zoomRelatedDiagrams: function (item) {
                if (item) {
                    //get the related Capital System Paths to this model item
                    var systemPaths = ra3DModel.getSystemDataForRapidAuthorItem(item,
                        this.getModelPath());
                    if (systemPaths) {
                        if (systemPaths.length > 0) {
                            if (window.crossHighlightHandler &&
                                window.crossHighlightHandler.hasOwnProperty("zoomViews")) {
                                window.mentor.publisher.selectedSystem.set("objectId", systemPaths[0].objectId,
                                                                              {silent: true})
                                window.crossHighlightHandler.zoomViews();
                            }
                        }
                    }
                    setTimeout(function () {
                        window.crossHighlightHandler.zoomItemInRapidAuthorViews(item);
                    }, 200);
                }
            },

            fitRelatedDiagram:function (item,lastSelected3DObject) {
                var that=this;
                if (item) {
                    //get the related Capital System Paths to this model item
                    var systemPaths = ra3DModel.getSystemDataForRapidAuthorItem(item,
                            this.getModelPath());
                    if (systemPaths) {
                        if (systemPaths.length > 0) {
                            if (window.crossHighlightHandler &&
                                    window.crossHighlightHandler.hasOwnProperty("zoomViews")) {
                                window.crossHighlightHandler.zoomViews();
                            }
                        }
                    }
                    setTimeout(function () {
                       // window.crossHighlightHandler.zoomItemInRapidAuthorViews(item);
                        that.zoomTo3dModel([lastSelected3DObject]);
                    }, 200);
                }
            },

            isVisible: function () {
                return this.$('#RA3DViewLoadArea').filter(':visible').length;
            }
        });
        return new RapidAuthorCatalogPanel();
    }
);

