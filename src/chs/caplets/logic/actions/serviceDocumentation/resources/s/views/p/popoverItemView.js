/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define("PopoverItemView", [
    'jquery',
    'underscore',
    'backbone',
    "models/selectedSystem"
], function ($, _, Backbone, selectedSystem)
{
    "use strict";
    var PopoverItemSection = Backbone.View.extend({

        doNotLoadOnStart: true,

        initialize: function ()
        {
            if (this.getData()) {
                this.getData().on("reset", this.render, this);
            }
        },

        removeView: function ()
        {

        },
        getWindowObj: function ()
        {
            if (window.opener && window.opener.mentor) {
                return window.opener;
            }
            else {
                return window
            }
            //return window.opener || window;
        },

        getDataId: function (event)
        {
            return $(event.target).parent().attr('data-id');
        },
        openPopout: function (url)
        {
            mentor.publisher.popoutHandler.openPopout(url);
        },

        createURL: function (content)
        {
        },

        popOut: function (event)
        {
            var cid = this.getDataId(event), content;
            content = this.getData().get(cid);
            if (!content) {
                return;
            }
            this.openPopout(this.createURL(content));
            event.stopPropagation();
        },

        //this will be overriden in the derivative views
        getData: function ()
        {
            return [];
        },

        //this will be overriden in the derivative views
        getTitle: function ()
        {
            return "";
        },

        getClassName: function ()
        {
            return "";
        },

        events: {},

        shouldShowPopup: function ()
        {
            return true;
        },

        filter: function (items)
        {
            var index, diagramIDs, getDiagramIds;

            _.each(items, function (item) {
                if (!item.get) {
                    return;
                }

                getDiagramIds = item.get("getDiagramIds");
                if (!item.get("getDiagramIds")) {
                    return;
                }

                diagramIDs = getDiagramIds();

                item.isActive = "";
                if (diagramIDs && (diagramIDs.indexOf(selectedSystem.get("diagramId")) < 0)) {
                    item.isActive = "panelitem_hide";
                }
            });

            return items;
        },

        isExpanded: function ()
        {
            return this.getData().expand;
        },

        removeToolTip: function (event)
        {
            this.generateEvent(event, mentor.publisher.events.REMOVE_TOOL_TIP);
        },

        generateEvent: function (event, eventName)
        {
            mentor.publisher.eventDispatcher.dispatchEvent(eventName,
                event);
        },

        getTooltipContent: function (content)
        {
            return content;
        },

        showToolTip: function (event)
        {
            var clickedItem, firstDiagram, id;
            id = $(event.currentTarget).attr('data-id');
            clickedItem = this.getData().get(id);
            event.detail = this.getTooltipContent(clickedItem);
            this.generateEvent(event, mentor.publisher.events.SHOW_TOOL_TIP);
        },

        getItemContent: function ()
        {

        },

        displayContent: function (content)
        {
            this.getWindowObj().mentor.publisher.detailLayoutManager.resetContentPanel();
            this.getWindowObj().mentor.publisher.fileDisplayHandler.display(content);
        },

        displaySelectedItem: function (itemId)
        {
            var content = this.getItemContent(itemId);
            if (content) {
                this.displayContent(content);
            }
        },

        popoverItemClicked: function (event)
        {
            var cid = $(event.currentTarget).attr('data-id');
            this.displaySelectedItem(cid);
        },

        getModel: function (models)
        {
            models.sort((a, b) => {
                const getA = typeof a.get === 'function';
                const getB = typeof b.get === 'function';

                if (getA && getB && a.get("mainText") != undefined && b.get("mainText") != undefined) {

                    const textA = a.get("mainText");
                    const textB = b.get("mainText");
                    return textA.localeCompare(textB);
                }
            });
            return {
                title: mentor.publisher.languageTranslator.localize(this.getTitle()),
                showTitle: this.getTitle() !== '',
                items: this.filter(models),
                className: this.getClassName(),
                showPopup: this.shouldShowPopup(),
                expand: this.isExpanded(),
                totalItems: this.getTotalItems()
            };
        },

        getTotalItems : function(){
            this.totalItems
        },

        shouldProcessDataBeforeDisplay: function ()
        {
            return false;
        },

        processDataBeforeRender: function (models)
        {

        },

        shouldRenderForEmptyCollection: function()
        {
            return false;
        },

        render: function ()
        {
            var models, template;
            $('.' + this.getClassName(), this.$el).remove();
            if (this.getData()) {
                models = this.getData().getModels ? this.getData().getModels() : this.getData().models;
                if (this.shouldProcessDataBeforeDisplay()) {
                    this.processDataBeforeRender(models);
                    //this.processXrefToShowCorrectSystemName(models);
                }

                if (!this.shouldRenderForEmptyCollection() &&
                        (!this.filter(models) || this.filter(models.length) === 0)) {
                    return;
                }
                if (!this.totalItems) {
                    this.totalItems = models && models.length;
                }
            }
            this.setElement(this.container);
            $(this.container).parent().css("visibility", "visible");
            if (this.templateHTML && models) {
                var opts = this.getModel(models) || {};
                opts.showRenderConnectivityBtn = false;
                template = _.template(this.templateHTML)(opts);

                this.setRenderedTemplateInElement(template);
            }
            return this;
        },

        setRenderedTemplateInElement: function (renderedTemplate) {
            this.$el.append(renderedTemplate);
        }

    });

    return PopoverItemSection;
});
