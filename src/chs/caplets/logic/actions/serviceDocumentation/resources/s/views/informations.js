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
    "collections/informations",
    "fileDisplayHandler",
    "ListView"
], function ($, underscore, Backbone, selectedPackage, informations, fileDisplayHandler, listView)
{
    "use strict";
    var Info = listView(informations).extend({

        getData: function ()
        {
            return informations;
        },
        title: "introduction-page",
        cssClass: "Informations",
        openPopout: function (id, projectId)
        {
            mentor.publisher.popoutHandler.openPopout("popout.html#/information/" + encodeURIComponent(id) + "/" +
                    projectId);
        }, popOut: function (event)
        {
            var id = $(event.target).parent().attr('data-id');
            this.openPopout(id, selectedPackage.get("id").replace("\\", "/"));
            event.stopPropagation();
        },
        clicked: function (event)
        {
            var information, firstDiagram, id, selectedElement;
            id = $(event.currentTarget).attr('data-id');
            information = informations.get(id);
            fileDisplayHandler.display({
                id: id,
                reset: true,
                type: mentor.publisher.contentType.CUSTOM_VIEW
            });
            this.expanded = true;

        }

    });
    var infoView = new Info();
    fileDisplayHandler.addFileHandler("popout-" + mentor.publisher.documentCategory.INFORMATION, function (content)
    {
        var infoId = content.id;
        var projectId = content.projectId;
        infoView.openPopout(infoId, projectId);
    });

    return infoView;
});
