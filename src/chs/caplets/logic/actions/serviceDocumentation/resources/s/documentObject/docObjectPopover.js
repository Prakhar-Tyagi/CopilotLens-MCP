/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function (p)
{
    "use strict";
    var filterModel, PopoverItems, createPanelCollection, createSectionCollection, createSectionView, PanelView, constructSection, createPopoverPanelView;

    p.documentObjectSection = {
        getDocumentEventHandler: function ()
        {
            return p.documentRequestHandlerFactory;
        },
        createDocumentObjectSection: function (viewName, viewData, config)
        {
            var events = {}, documentEventHandler = this.getDocumentEventHandler();
            events["click ." + viewName + ">.listItem"] = "popoverItemClicked";
            events["mouseover ." + viewName + ">.listItem"] = "showToolTip";
            events["mouseout ." + viewName + ">.listItem"] = "removeToolTip";
            events["click ." + viewName + ">.listItem>.popUp"] = "popOut";
            /* events["keyup #relateddata_filter"] = "textEntered";
             events["click #relateddata_filter"] = "removeTextPlaceHolder";
             events["focusout #relateddata_filter"] = "addTextPlaceHolder";
             */
            config = config || {};
            return {
                container: "#popover-grouped-list",
                templateHTML: config.htmlTemplate,

                getData: function ()
                {
                    if (filterModel && filterModel.get("searchText")) {
                        return {
                            models: this.getTextSearchComponent()(viewData).filterByText(filterModel.get("searchText")),
                            expand: true
                        };
                    }
                    else {
                        return viewData;
                    }

                },
                getTextSearchComponent: function ()
                {
                    return require("textSearch");
                },
                getTitle: function ()
                {
                    return viewName;
                },
                shouldShowPopup: function ()
                {
                    return config.showpopoutInSectionItem;
                },
                getClassName: function ()
                {
                    return viewName;
                },

                getTooltipContent: function (content)
                {
                    var tooltips = content.tooltips || "";
                    if (!tooltips) {
                        var type = content.get("type") || "";
                        var docRequestHandler = documentEventHandler.get(type);
                        if (docRequestHandler) {
                            return docRequestHandler.loadToolTip(content);
                        }
                    }
                    return tooltips;
                },
                events: events,
                popoverItemClicked: function (event)
                {
                    var type, cid = $(event.currentTarget).attr('data-id'), clickedItem;
                    clickedItem = this.getItemContent(cid);
                    if (!clickedItem) {
                        return;
                    }
                    if (config.onMouseClick) {
                        config.onMouseClick(event, clickedItem);
                    }
                    else {
                        type = clickedItem.get("type") || "";
                        if (type) {
                            var docRequestHandler = documentEventHandler.get(type);
                            if (docRequestHandler) {
                                docRequestHandler.display(clickedItem, event);
                            }
                        }

                    }
                },
                popOut: function (event)
                {
                    var type, cid = this.getDataId(event), clickedItem;
                    clickedItem = this.getItemContent(cid);
                    if (!clickedItem) {
                        return;
                    }
                    if (config.onPopout) {
                        config.onPopout(event, clickedItem);
                    }
                    else {

                        type = clickedItem.get("type") || "";
                        if (type) {
                            var docRequestHandler = documentEventHandler.get(type);
                            if (docRequestHandler) {
                                var url = docRequestHandler.createURL(clickedItem, event);
                                if (url) {

                                    this.openPopout(url);
                                }
                            }
                        }

                    }
                    event.stopPropagation();
                },

                getItemContent: function (itemId)
                {
                    var itemClicked;
                    viewData.models.forEach(function (item)
                    {
                        if (itemId === item.id) {
                            itemClicked = item;
                            return false;
                        }

                    });
                    return itemClicked;
                }

            };
        },
        instantiateDocumentObjectSectionGrp: function (PopoverView, popoverTemplate, config) {
            var popoverWithoutSections, PopoverWithoutSections = PopoverView.extend({
                container: "body",
                initialize: function () {
                    PopoverWithoutSections.__super__.initialize();
                },
                events: {
                    "keyup #relateddata_filter>input": "textEntered",
                    "click #relateddata_filter>input": "removeTextPlaceHolder",
                    "focusout #relateddata_filter>input": "addTextPlaceHolder"
                },
                getFilterInput: function (evt)
                {
                    return $(evt.target);

                },
                render: function ()
                {
                    var that = this;
                    this.setElement(this.container);
                    $('#detailPopup', this.$el).remove();

                    var showFilter = config.enableFilter || false;
                    var coordinates = this.getCoordinates(config.x, config.y);
                    var template = _.template(popoverTemplate)({
                        title: mentor.publisher.languageTranslator.localize(config.title) || config.title,
                        show: true,
                        x: coordinates.x,
                        y: coordinates.y,
                        height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                                mentor.publisher.constants.popOverHeightWithoutFilter,
                        showFilter: showFilter,
                        showXrefBuilderButton: false,
                        showRenderConnectivityBtn: that.isSignalTracerAvailable(),
                        renderConnectivityBtnToolTip: that.getRenderConnectivityBtnToolTip()
                    });
                    this.$el.append(template);
                    setTimeout(function ()
                    {
                        $('#detailPopup', that.$el).css("visibility", "visible");
                    }, 100);

                    return this;
                },
                getPopoverFilter: function ()
                {
                    return config.popoverFilterModel;
                },
                triggerFilter: function (filterText)
                {
                    if (filterText === 'Filter') {
                        //todo can this condition lead to issues?
                        filterText = '';
                    }
                    config.popoverFilterModel.set("searchText", filterText);

                }
            });
            popoverWithoutSections = new PopoverWithoutSections();
            popoverWithoutSections.render();
        },
        createDocumentObjectSectionGrp: function (title, config) {
            config = config || {};
            config.title = title;
            var that = this;
            require(["PopoverView", "text!templates/p/popoverTemplate.html"],
                    function (PopoverView, popoverTemplate) {
                        that.instantiateDocumentObjectSectionGrp(PopoverView, popoverTemplate, config)
                    });

        }

    };
    p.popoverPanels = [];
    p.popoverPanels.push("bundles");
    p.popoverPanels.push("wires");

    createPanelCollection = function (panelName, config)
    {
        var PanelCollection;
        config = config || {};
        PopoverItems = require("PopoverItem");
        PanelCollection = PopoverItems.extend({
            expand: config.expand,
            getData: function (designObject)
            {
                return p.filter.applyFilter(designObject.get ? designObject.get(panelName) : []);
            },
            getPopoverFilterModel: function ()
            {
                return filterModel;
            }
        });
        return new PanelCollection();

    };
    createSectionCollection = function (panelData)
    {
        var PanelCollection;
        PopoverItems = p.designObjectPopover.getPopoverItemCollection();
        PanelCollection = PopoverItems.extend({
            getData: function (designObject)
            {
                return p.designObjectPopover.getFilter().applyFilter(panelData || []);
            }
        });
        return new PanelCollection();

    };

    createSectionView = function (panelName, panelCollection, panelTemplate, designObject, config)
    {
        var View;
        config = config || {};
        panelCollection = panelCollection || createPanelCollection(panelName, config);
        panelTemplate = panelTemplate || "templates/p/popoverPanelTemplate.html";
        PanelView = p.designObjectPopover.getPopoverItemView();

        p.designObjectPopover.loadHTMLTemplateAndRenderView("s/" + panelTemplate, function (htmlTemplate)
        {
            var view;
            config.htmlTemplate = htmlTemplate;
            config.showpopoutInSectionItem = config.showPopoutBtn;
            View = PanelView.extend(p.documentObjectSection.createDocumentObjectSection(panelName, panelCollection,
                config));

            view = new View();
            panelCollection.expand = config.expand;
            filterModel.on("change:searchText", function ()
            {
                view.render();
            });
            panelCollection.on("reset", function ()
            {
                view.render()
            });

            panelCollection.fetch(designObject);

            view.render();
        }, config);

        return panelCollection;
    };

    constructSection = function (panelName, sectionData, designObject, config)
    {
        var componentLoader, panelCollection, panelPopoverView, panelHTMLTemplate = "templates/p/popoverPanelTemplate.html";
        componentLoader = p.designObjectPopover.getComponentLoader();
        panelPopoverView = componentLoader.getComponentViewByName(panelName);
        if (!(panelPopoverView && panelPopoverView.view)) {

            if (panelPopoverView && panelPopoverView.template) {
                panelHTMLTemplate = panelPopoverView.template;
            }
            return createSectionView(panelName, sectionData, panelHTMLTemplate, designObject, config);
        }

    };

    p.designObjectPopover = {
        createDesignObjectSection: function (popoverSectionName, designObject, config)
        {
            var Backbone = require("backbone");
            filterModel = new (Backbone.Model.extend({}))();
            return {
                title: popoverSectionName,
                collection: constructSection(popoverSectionName, "", designObject, config)
            };
        },
        addSection: function (sectionName, sectionData, popoverFilterModel, config)
        {
            filterModel = popoverFilterModel;
            config = config || {
                expand: true,
                showPopoutBtn: true,
                async: true
            };
            return constructSection(sectionName, createSectionCollection(sectionData), "", config);

        },
        showPopover: function (title, x, y, enableFilter, popoverFilterModel)
        {
            return p.documentObjectSection.createDocumentObjectSectionGrp(title, {
                x: x,
                y: y,
                enableFilter: enableFilter,
                popoverFilterModel: popoverFilterModel
            });
        }, getComponentLoader: function ()
        {
            return require("ComponentLoader");
        }, loadHTMLTemplateAndRenderView: function (htmlTemplate, callBack, config)
        {

            $.ajax({
                url: htmlTemplate,
                async: config.async,
                success: function (tpl)
                {
                    callBack(tpl);
                },
                dataType: "html"
            });

        },
        getPopoverItemCollection: function ()
        {
            return require("PopoverItem");
        },
        getPopoverItemView: function ()
        {
            return require("PopoverItemView");
        }, getFilter: function ()
        {
            return p.filter;
        }
    };

    /*}(mentor.publisher));

     (function (p)
     {*/

}(mentor.publisher));