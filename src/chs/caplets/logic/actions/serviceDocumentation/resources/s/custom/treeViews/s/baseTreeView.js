/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

define([
    'jquery',
    'underscore',
    "fileDisplayHandler",
    "ListView",
    "treeViewHelper"
], function ($, _, fileDisplayHandler, listView, treeViewHelper)
{
    "use strict";

    return function (designs)
    {
        return listView(designs).extend({

            events: {
                "click .listItem": "listItemClicked",
                "mouseover .listItem": "mouseover",
                "mouseout .listItem": "mouseout",
                "click .titlebar": "headerClicked",
                "click .collapseAll": "collapseAll",
                "click .popUp": "popOut",
                "click .expand-tree": "expandTree",
                "click .collapse-tree": "collapseTree"
            },

            headerClicked: function (evt)
            {
                this.expanded = this.expanded ? false : true;
                $("ol.tree", this.$el).toggle();
                evt.stopPropagation();
            },

            expandTree: function (evt)
            {
                $(evt.target).siblings('input[type=checkbox]').prop("checked", true);
                $(evt.target).siblings('ol').find('input[type=checkbox]').prop("checked", true);
                evt.stopPropagation();
            },

            collapseTree: function (evt)
            {
                $(evt.target).siblings('input[type=checkbox]').prop("checked", false);
                $(evt.target).siblings('ol').find('input[type=checkbox]').prop("checked", false);
                evt.stopPropagation();
            },

            mouseout: function (event)
            {
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                    event);
                this.hideCollapseAll(event);
            },

            mouseover: function (event)
            {
                var clickedSystem, firstDiagram, id;
                id = $(event.currentTarget).attr('data-id');
                clickedSystem = designs.get(id);
                event.detail = clickedSystem;
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                    event);
                this.showCollapseAll(event);
            },

            clicked: function (evt)
            {
                var id = $(evt.currentTarget).attr('data-id');
                var content = this.getContent(id);
                fileDisplayHandler.display(content);
            },

            render: function (expand, header)
            {
                //initialize systems
                mentor.publisher.project.getSystems();
                header = header || true;
                expand = expand || false;
                if (!this.getData().getModels() || this.getData().getModels().length <= 0) {
                    return;
                }
                this.setElement(this.container);

                var treeViewHelperOptions = {
                    idString: this.getModelIdString(),
                    fileLabelSupplierFn: this.getFileLabel,
                    folderLabelSupplierFn: _.identity,
                    desginFolderProviderFn: this.getDesingfolders,
                    folderSeparatorFn: this.getFolderDelimiter,
                    showFolderCount: this.showFolderCount || false,
                    showFoldersFirst: this.showFoldersFirst || false,
                };
                var systemAsTreeHTML = treeViewHelper.createTreeView(this.getData().getModels(), treeViewHelperOptions);

                this.templateHTML = this.templateHTML || "<div></div>";
                var template = _.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize(this.title) || this.title,
                    items: this.getData().getModels(),
                    header: header,
                    expand: expand,
                    systemAsTreeHTML: systemAsTreeHTML
                });
                this.$el.append(template);
                this.highlightPreviousSelection();
                this.amItTheFirstPanel();
                return this;
            }
        });
    };
});