/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define("ListView", [
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "models/selectedSystem"
], function ($, underscore, Backbone, selectedPackage, selectedSystem)
{
    "use strict";
    return function (collection)
    {
        return Backbone.View.extend({

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
            popOut: function (event)
            {
                event.stopPropagation();
            },
            hideCollapseAll: function (evt)
            {
                $(".collapseAll", $(evt.currentTarget)).css("visibility", "hidden");
            },
            showCollapseAll: function (evt)
            {
                $(".collapseAll", $(evt.currentTarget)).css("visibility", "visible");
            },

            listItemClicked: function (event)
            {
                if ($(event.currentTarget).hasClass("highlight") && this.isContentTypeOpen()) {
                    event.stopPropagation();
                    return;
                }
                this.clicked(event);
                event.stopPropagation();
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
                var selectedObject, id;
                id = $(event.currentTarget).attr('data-id');
                selectedObject = this.getData().get(id);
                event.detail = selectedObject;
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                        event);
                this.showCollapseAll(event);
            },

            clicked: function ()
            {
            },
            headerClicked: function ()
            {
                this.expanded = this.expanded ? false : true;
            },
            initialize: function ()
            {
                selectedPackage.on("change:searchText", this.reRender, this);
                this.getData().on("reset", this.reRender, this);
                selectedPackage.on("collapseAll", this.collapseAll, this);
                selectedPackage.on("expandAll", this.expandAll, this);
                this.init();
            },
            collapseAll: function (evt)
            {
                if (this.isListViewExpanded()) {
                    $(".titlebar", $(this.$el)).trigger("click");
                }
                if (evt) {
                    evt.stopPropagation();
                }
            },
            expandAll: function ()
            {
                if (!this.isListViewExpanded()) {
                    $(".titlebar", $(this.$el)).trigger("click");
                }
            },
            init: function ()
            {

            },

            isListViewExpanded: function ()
            {
                return $(".listItem", this.$el).is(':visible');
            },

            highlightPreviousSelection: function ()
            {
                var selectedElement;
                if (selectedSystem.get("selectedElement")) {
                    selectedElement = $('.listItem[data-id="' + selectedSystem.get("selectedElement") + '"]', this.$el);
                    selectedElement = selectedElement.length <= 0 ?
                            $('.listItem[base-id="' + selectedSystem.get("selectedElement") + '"]', this.$el) :
                            selectedElement;
                    if (selectedElement.length > 0) {
                        $(selectedElement).first().addClass("highlight");
                    }
                }
            },

            getTitle: function ()
            {
                return mentor.publisher.languageTranslator.localize(this.title) || this.title;
            },

            beforeViewRender: function ()
            {
                //initialize systems
                mentor.publisher.project.getSystems();
            }, isViewDataAvailable: function ()
            {
                return this.getData().getModels() && this.getData().getModels().length > 0;
            },
            fetchData: function (config)
            {
                var header = this.header || true;
                var expand = this.expanded;
                var models = this.getData().getModels();
                models.sort((a, b) => {
                    const textA = a.get("mainText");
                    const textB = b.get("mainText");
                    return textA.localeCompare(textB);
                });
                config.success({
                    title: this.getTitle(),
                    items: models,
                    header: header,
                    expand: expand
                });
            },
            createHTMLAndAppendToContainer: function (dataForViewTemplate)
            {
                var template = underscore.template(this.templateHTML)(dataForViewTemplate);
                this.$el.append(template);
            },
            viewDidRender: function ()
            {
                this.highlightPreviousSelection();
                this.amItTheFirstPanel();
            },
            render: function ()
            {
                this.beforeViewRender();
                if (this.isViewDataAvailable()) {
                    this.setElement(this.container);
                    this.fetchData({
                        error: function ()
                        {

                        },
                        success: function (data)
                        {
                            this.createHTMLAndAppendToContainer(data);
                            this.viewDidRender();
                        }.bind(this)
                    });
                    return this;
                }
            },
            amItTheFirstPanel: function ()
            {
                if ($(this.$el).attr("data-firstPanel") === "true") {
                    this.expanded = true;
                    $(this.$el).removeAttr("data-firstPanel");
                }
            },
            getData: function ()
            {
                return collection;
            },
            reRender: function ()
            {
                this.removeItems();
                this.render();
                return this;
            },

            removeItems: function ()
            {
                $(this.$el).children().each(function ()
                {
                    $(this).remove();
                });
            }

        });
    };
});

