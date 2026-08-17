/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, define, require*/
define(
        "harnessLayoutBarHandler",
        ["views/contentpanel/DefaultBarHandler", "models/selectedSystem"],
        function (DefaultBarHandler, selectedSystem)
        {
            "use strict";

            var HarnessLayoutBarHandler = DefaultBarHandler.extend({
                onDiagramsButtonClick: function (event)
                {
                    this.createOptionsAndShowPopover(event, "diagrams",
                            this.contentType);
                },
                contentType: mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM,
                setContentType: function (type)
                {
                    this.contentType = type;
                },

                setDataId: function (id)
                {
                    this.dataId = id;
                },

                getPopoverView: function ()
                {
                    return "views/p/hld/HarnessLayoutDiagramsPopover";
                },
                createOptionsAndShowPopover: function (event, documentGroup, documentType, popoverTitle)
                {
                    documentType = documentType || mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM;
                    documentGroup = documentGroup || "diagrams";
                    var dataId = this.dataId;
                    var options = {
                        el: "",
                        preferredX: event.clientX,
                        preferredY: event.clientY,
                        getDocumentGroup: function ()
                        {
                            return documentGroup;
                        }, getDocumentType: function ()
                        {
                            return documentType;
                        }, getActiveDocumentForDocumentSet: function ()
                        {
                            return selectedSystem.get(documentType) || {};
                        },
                        getDocumentSetId: function ()
                        {
                            return dataId;
                        },
                        getDocumentCSSClass: function ()
                        {
                            return "harness-layout-" + documentGroup;
                        },
                        getDocumentName: function ()
                        {
                            return documentType;
                        },
                        popoverTitle: popoverTitle
                    };

                    this.showPopover(this.getPopoverView(), event, options);
                    return (function (event, documentGroup, documentType)
                    {

                    }(event, documentGroup, documentType));
                },
                onReportsButtonClick: function (event)
                {
                    this.createOptionsAndShowPopover(event, "reports",
                            mentor.publisher.contentType.HARNESS_LAYOUT_REPORT, "ReportsPopoverViewTitle");
                },
                amdLoader: require,

                getOptions: function (options, event)
                {
                    options = {
                        preferredX: event.clientX,
                        preferredY: event.clientY
                    };
                    return options;
                }, showPopover: function (type, event, options)
                {
                    var popover;

                    options = options || this.getOptions(options, event);

                    this.amdLoader([type], function (Popover)
                    {
                        popover = new Popover();
                        popover.render(options);
                    });
                },
            });

            return HarnessLayoutBarHandler;
        }
);