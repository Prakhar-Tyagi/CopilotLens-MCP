/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("DesignObjectsView", [
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "models/selectedSystem"
], function ($, underscore, Backbone, selectedPackage, selectedSystem)
{
    "use strict";
    return function (objects)
    {
        return Backbone.View.extend({
            title: "DesignObjs",
            itemsPerPage: 50,
            page: 1,
            shouldFilterForDiagram: false,
            highlightInLeftPanel: false,

            events: {
                "click .next": "showNextPage",
                "click .previous": "showPreviousPage",
                "click .titlebar": "headerClicked",
                "click .listItem": "listItemClicked",
                "mouseover .listItem": "mouseover",
                "mouseout .listItem": "mouseout",
                "click .collapseAll": "collapseAll",
                "click .popUp": "popOut"
            },

            getTotalPages: function ()
            {
                var items = this.getData() || {}, objs, length, totalLength;
                objs = (items.getModels && items.getModels()) || [];
                length = objs.length;
                totalLength = length <= this.itemsPerPage ? 1 :
                        (length % this.itemsPerPage === 0 ? length / this.itemsPerPage :
                        (length / this.itemsPerPage) + 1);
                return parseInt(totalLength, 10);
            },

            filterForDiagram: function (items)
            {
                var index;
                for (index in items) {
                    if (items.hasOwnProperty(index)) {
                        if (items[index].get("getDiagramIds")) {
                            if (items[index].get("getDiagramIds")()) {
                                if (items[index].get("getDiagramIds")().indexOf(selectedSystem.get("diagramId")) >= 0) {
                                    items[index].isActive = "";
                                }
                                else {
                                    items[index].isActive = "panelitem_hide";
                                }
                            }
                        }
                    }
                }
                return items;
            },

            getItems: function (items)
            {
                var itemsToDiaplay;
                if (this.shouldFilterForDiagram) {
                    items = this.filterForDiagram(items);
                }
                var totalItems = (items ||
                []).length, totalPages = this.getTotalPages(), itemsToDisplay, rem, start, end;
                start = (this.page - 1) * this.itemsPerPage;
                if (this.page === totalPages) {
                    end = totalItems;
                }
                else if (this.page < totalPages) {
                    end = this.page * this.itemsPerPage;
                }
                else {
                    return [];
                }
                itemsToDiaplay = items.slice(start, end);
                if (!this.getData().partiallyLoaded && itemsToDiaplay && itemsToDiaplay.length > 0) {
                    itemsToDiaplay[0].set("totalObjects", totalItems);
                }
                return itemsToDiaplay;
            },

            hideCollapseAll: function (evt)
            {
                $(".collapseAll", $(evt.currentTarget)).css("visibility", "hidden");
            },

            showCollapseAll: function (evt)
            {
                $(".collapseAll", $(evt.currentTarget)).css("visibility", "visible");
            },

            initialize: function ()
            {
                selectedPackage.on("change:searchText", this.showObjectsThatMatchesSearchText, this);
                selectedPackage.on("change:id", this.resetState, this);
                this.getData().on("reset", this.reRender, this);
                selectedPackage.on("collapseAll", this.collapseAll, this);
                selectedPackage.on("expandAll", this.expandAll, this);
                this.init();
            },

            showObjectsThatMatchesSearchText: function ()
            {
                this.resetState();
                this.reRender();
            },

            collapseAll: function (evt)
            {
                if (evt) {
                    if (this.expanded || $(evt.target).hasClass("collapseAll")) {
                        $(".titlebar", $(this.$el)).trigger("click");
                    }
                    if (evt) {
                        evt.stopPropagation();
                    }
                }
            },

            expandAll: function ()
            {
                if (!this.expanded) {
                    $(".titlebar", $(this.$el)).trigger("click");
                }
            },

            resetState: function ()
            {
                this.page = 1;
            },

            init: function ()
            {

            },

            highlightPreviousSelection: function ()
            {
                var selectedElement;
                if (selectedSystem.get("selectedElement")) {
                    selectedElement = $(".listItem[data-id='" + selectedSystem.get("selectedElement") + "']", this.$el);
                    selectedElement = selectedElement.length <= 0 ?
                            $(".listItem[base-id='" + selectedSystem.get("selectedElement") + "']", this.$el) :
                            selectedElement;
                    if (selectedElement.length > 0) {
                        $(selectedElement).first().addClass("highlight");
                    }
                }
            },

            popOut: function (event)
            {
                event.stopPropagation();
            },

            highlightOnClick: function (event)
            {
                return $(event.currentTarget).hasClass("highlight");
            },

            listItemClicked: function (event)
            {
                if ((this.highlightOnClick(event) && this.isContentTypeOpen()) ||
                        $(event.currentTarget).hasClass("next") || $(event.currentTarget).hasClass("previous") ) {
                    event.stopPropagation();
                    return;
                }
                this.clicked(event);
            },

            isContentTypeOpen: function () {
                return mentor.publisher.detailLayoutManager.isContentActive(this.getContentType());
            },

            getContentType: function () {
                return "";
            },

            mouseout: function (event)
            {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                        event);
                this.hideCollapseAll(event);
            },

            mouseover: function (event)
            {
                var selectedObject, firstDiagram, id;
                id = $(event.currentTarget).attr('data-id');
                selectedObject = this.getData().get(id);
                event.detail = selectedObject;
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                        event);
                this.showCollapseAll(event);
            },

            getData: function ()
            {
                return objects;
            },

            clicked: function (event)
            {
                var id = $(event.currentTarget).attr("data-id"), designObject;
                if (!$(event.target).hasClass("next") && !$(event.target).hasClass("previous")) {
                    designObject = this.getData().get(id);
                    require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter)
                    {
                        multipleDocumentRouter.save(true, id);
                    });
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP,
                            {
                                x: event.clientX,
                                y: event.clientY,
                                id: id,
                                systemId: designObject.get("systemId"),
                                callBack: function ()
                                {
                                }
                            });

                }
                event.stopPropagation();
            },

            highlightObject: function (event, collection)
            {
                var cid = $(event.currentTarget).attr('data-id'), object, content;
                object = collection.get(cid);
                if (object) {
                    mentor.publisher.eventDispatcher.dispatchEvent(
                            mentor.publisher.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS,
                            object.attributes);
                }
            },

            headerClicked: function (event)
            {
                this.expanded = this.expanded ? false : true;
            },

            showNextPage: function (event)
            {
                var that = this, loadAllObjects = false;
                if (this.getData().partiallyLoaded && ((this.page + 1) * this.itemsPerPage) > this.getData().length) {
                    alertMsg.showMessageWithLoadingImage(
                            getLoadingMessage(mentor.publisher.languageTranslator.localize('NotReadyPleaseWait')),
                            "loading");
                    loadAllObjects = true;
                }
                setTimeout(function ()
                {
                    if (loadAllObjects) {
                        selectedPackage.trigger("loadAllObjects");
                        alertMsg.removeAlertMsg();
                    }

                    that.page = that.page + 1;
                    that.paginate();
                    event.stopPropagation();
                }, 100);
            },

            showPreviousPage: function (event)
            {
                this.page = this.page - 1;
                this.paginate();
                event.stopPropagation();
            },

            reRender: function ()
            {
                this.paginate(true);
            },

            paginate: function (header)
            {
                if (this.templateHTML) {
                    header = header || false;
                    var template = this.renderItems(
                            {
                                header: header,
                                expand: this.expanded
                            }
                    );
                    if (template) {
                        $(this.$el).first().append(template.trim());
                    }
                }
            },

            removeItems: function ()
            {
                $(this.$el).children().each(function ()
                {
                    $(this).remove();
                });
            },

            renderItems: function (options)
            {
                if (this.templateHTML && this.getData()) {
                    this.setElement(this.container);
                    this.removeItems();

                    return underscore.template(this.templateHTML)({
                        page: this.page,
                        totalPages: this.getTotalPages(),
                        title: this.getTitle(),
                        items: this.getItems(this.getData().getModels()),
                        expand: options.expand,
                        totalItems: this.getData().getModels().length
                    });
                    if (this.highlightInLeftPanel) {
                        this.highlightPreviousSelection();
                    }
                }
            },

            amItTheFirstPanel: function ()
            {
                if ($(this.$el).attr("data-firstPanel") === "true") {
                    this.expanded = true;
                    $(this.$el).removeAttr("data-firstPanel");
                }
            },

            render: function ()
            {
                if (this.templateHTML) {
                    var template = this.renderItems(
                            {
                                header: true,
                                expand: false
                            }
                    );
                    this.$el.append(template);
                    this.amItTheFirstPanel();
                }
                return this;
            },

            getTitle: function ()
            {
                return mentor.publisher.languageTranslator.localize(this.title) || this.title;
            }
        });
    };
});
