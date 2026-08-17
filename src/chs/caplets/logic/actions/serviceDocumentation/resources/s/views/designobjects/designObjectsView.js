/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "models/selectedSystem",
    "filters/documentContentBasedFilter",
    "views/component/PaginatedListView",
    "views/component/ContextChangeSensitiveView",
    "ListView"
], function ($,
        underscore,
        Backbone,
        selectedPackage,
        selectedSystem,
        documentContentBasedFilter,
        PaginatedListView,
        ContextChangeSensitiveView,
        ListView)
{
    "use strict";
    var p = mentor.publisher;
    return function (objects)
    {
        var viewConfiguration = {
            applyOptionFilter: true,
            applyPackageChange: true,
            applyLanguageChange: true,
            applySearchFilter: true
        };
        var Objects = PaginatedListView(objects).extend(
                _.extend(ContextChangeSensitiveView(viewConfiguration), {
                    title: "DesignObjs",
                    delegate: this,
                    isViewDataAvailable: function ()
                    {
                        return true;
                    },
                    getDataIdOfClickedElement: function (event)
                    {
                        var element = this.getDomElement(event.currentTarget);
                        return element.attr("data-id");
                    },
                    clicked: function (event)
                    {
                        if (this.isValidEvent(event)) {
                            var targetObjectId = this.getDataIdOfClickedElement(event);
                            var targetCoordinates = {x: event.clientX, y: event.clientY};
                            this.showObjectPopoverForObject(targetObjectId, targetCoordinates);
                            this.highlightObject(targetObjectId);
                        }
                        event.stopPropagation();
                    },
                    getDomElement: function (elementSelector)
                    {
                        return $(elementSelector);
                    },
                    isValidEvent: function (event)
                    {
                        //when next... or previous... click, it will paginate and will not show any popover
                        //so ignore these clicks for popover
                        var element = this.getDomElement(event.target);
                        return !element.hasClass("next") && !element.hasClass("previous");
                    },
                    showObjectPopoverForObject: function (id, targetCoordinates)
                    {
                        this.saveCurrentObjectSelectionInHistory(id);
                        this.showObjectPopover(id, targetCoordinates);

                    },
                    highlightObject: function (id, config)
                    {
                        config = config || {};
                        var p = config.eventDispatcher || mentor.publisher;
                        var eventName = config.eventName || p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS;
                        setTimeout(function ()
                        {
                            p.eventDispatcher.dispatchEvent(eventName, {objectId: id});
                        }, 100);

                    },
                    saveCurrentObjectSelectionInHistory: function (id, config)
                    {
                        config = config || {};
                        var modLoader = config.moduleLoader || require;
                        modLoader(["routers/multipleDocumentRouter"], function (multipleDocumentRouter)
                        {
                            multipleDocumentRouter.save(true, id);
                        });
                    },

                    showObjectPopover: function (id, coordinates, config)
                    {
                        config = config || {};
                        var eventDispatcher = config.eventDispatcher || p.eventDispatcher;
                        var eventName = config.eventName || p.events.OPEN_OBJECT_POPUP;
                        var designObject = this.getSelectecObject(id);
                        eventDispatcher.dispatchEvent(
                                eventName,
                                {
                                    x: coordinates.x,
                                    y: coordinates.y,
                                    id: id,
                                    systemId: designObject.get("systemId"),
                                    callBack: function ()
                                    {
                                    }
                                });
                    },
                    getSelectecObject: function (id)
                    {
                        return this.getData().get(id);
                    }
                }));
        return new Objects();
    };
});
