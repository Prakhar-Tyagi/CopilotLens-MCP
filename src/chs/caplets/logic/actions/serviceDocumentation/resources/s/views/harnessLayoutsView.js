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
    'backbone',
    "currentPackage",
    "DesignObjectsView",
    "fileDisplayHandler",
    "harnessLayouts",
    "treeViewHelper",
    "text!templates/paginatedListGroup.html",
    "text!templates/treeView.html"
], function ($,
    Backbone,
    selectedPackage,
    listView,
    fileDisplayHandler,
    harnessLayouts,
    treeViewHelper,
    flatViewTemplate,
    treeViewTemplate)
{
    "use strict";
    var paginatedView = listView(harnessLayouts);
    var p = mentor.publisher;
    var translator = mentor.publisher.languageTranslator;

    var harnessLayoutsView = paginatedView.extend({
        title: "HarnessLayouts",
        cssClass: "harness-layouts",
        expanded: false,

        events: {
            "click .next": "showNextPage",
            "click .previous": "showPreviousPage",
            "click .titlebar": "headerClicked",
            "click .listItem": "listItemClicked",
            "mouseover .listItem": "mouseover",
            "mouseout .listItem": "mouseout",
            "click .collapseAll": "collapseAll",
            "click .popUp": "popOut",
            "click .expand-tree": "expandTree",
            "click .collapse-tree": "collapseTree"
        },

        getContentType: function ()
        {
            return mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM;
        },

        headerClicked: function (event)
        {
            this.expanded = this.expanded ? false : true;
            if (this.isTreeViewApplicable()) {
                $("ol.tree", this.$el).toggle();
            }
            else {
                var section = $(event.currentTarget).parent();
                var objs = $(".listItem", section);
                $.each(objs, function (index)
                {
                    $(objs[index]).toggle();
                });
            }
            event.stopPropagation();
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

        popOut: function (event)
        {
            var layout,
                layoutId = $(event.currentTarget).parent().attr('data-id'),
                itemId,
                layoutToOpen,
                projectId;

            event.stopPropagation();

            layout = harnessLayouts.get(layoutId);
            layoutToOpen = layout.getDiagramOrReportToOpen(layoutId);
            var defaultRoute = this.getType(layoutToOpen, event);
            projectId = selectedPackage.get("id").replace("\\", "/");
            itemId = layoutToOpen.id;

            mentor.publisher.popoutHandler.openPopout(
                "popout.html#/" + defaultRoute.toLowerCase() + "/" + layoutId + "/" + itemId + "/" + projectId
            );
        },

        getType: function (layoutToOpen, event)
        {
            var defaultRoute = "harnesslayoutdiagram";
            if (layoutToOpen.group === p.documentCategory.REPORTS) {
                defaultRoute = "harnesslayoutreport";
            }
            return defaultRoute;
        },

        clicked: function (event)
        {
            var content = {},
                harnessLayout;

            harnessLayout = this.getLayoutFromEvent(event);
            if (harnessLayout) {

                content = harnessLayout.getContent();
                fileDisplayHandler.display(content);
            }
        },

        mouseout: function (event)
        {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                event);

            this.hideCollapseAll(event);
        },

        mouseover: function (event)
        {
            event.detail = this.getLayoutFromEvent(event);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                event);

            this.showCollapseAll(event);
        },

        getLayoutFromEvent: function (event)
        {
            var cid = $(event.currentTarget).attr('data-id');
            return harnessLayouts.get(cid);
        },

        render: function ()
        {
            if (this.isTreeViewApplicable()) {
                this.templateHTML = treeViewTemplate;
                this.renderTreeView();
            }
            else {
                this.templateHTML = flatViewTemplate;
                this.renderFlatView();
            }
            return this;
        },

        isTreeViewApplicable: function ()
        {
            var treeViewEnabled = p.harnessTreeViewEnabled;
            return treeViewEnabled && this.isHarnessPathDataAvailable()
        },

        isHarnessPathDataAvailable: function ()
        {
            return _.every(harnessLayouts.models, function (design)
            {
                return design.has('folders');
            });
        },

        reRender: function ()
        {
            this.removeItems();
            this.render();
        },

        renderFlatView: function ()
        {
            var template = this.renderItems({
                header: true,
                expand: this.expanded
            });
            this.$el.append(template);
            this.amItTheFirstPanel();
        },

        // Tree View Related functions
        renderTreeView: function (expand, header)
        {
            header = header || true;
            expand = expand || false;
            if (!this.getData().getModels() || this.getData().getModels().length <= 0) {
                return;
            }
            this.setElement(this.container);

            var treeViewHelperOptions = {
                idString: this.getModelIdString(),
                fileLabelSupplierFn: this.getFileLabel,
                folderLabelSupplierFn: this.getFolderDisplayLabel,
                desginFolderProviderFn: this.getDesingfolders
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
        },

        getFolderDisplayLabel: function (folderNodeText)
        {
            var translatableStrings = ["Derivatives", "Functional Modules", 'Production Modules'];
            var stringIndex = translatableStrings.indexOf(folderNodeText);

            if (stringIndex === -1) {
                return folderNodeText;
            }

            switch (stringIndex) {
                case 0:
                    return translator.localize("derivatives.label");
                case 1:
                    return translator.localize("fm.modules.label");
                case 2:
                    return translator.localize("pm.modules.label");
                default:
            }
        },

        getFileLabel: function (fileObj)
        {
            function getToolTip(list, prop)
            {
                var ttObject = list.filter(function (item)
                {
                    return item.name === prop;
                });
                return ttObject && (ttObject.length > 0) && ttObject[0].value;
            }

            function getDesignSpec(designTooltips)
            {
                var designSpecText = getToolTip(designTooltips, "Part Number") + ":" +
                    getToolTip(designTooltips, "Revision");
                var designShortDesc = Utils.handleTranslation(getToolTip(designTooltips, "Description"));
                if (designShortDesc) {
                    designSpecText = designSpecText + ":" + designShortDesc;
                }
                return designSpecText;
            }

            var tooltips = fileObj.get('tooltips');
            var fileLabel;

            if (getWindowObj().diagramAsSystemsObjectFactoryImpl) {
                fileLabel =
                    fileObj.get("mainText") + " (" + fileObj.get("subText") + ":" + getDesignSpec(tooltips) + ")";
            }
            else {
                fileLabel = fileObj.get("mainText") + ":" + getDesignSpec(tooltips);
            }

            return fileLabel;
        },

        getDesingfolders: function (design)
        {
            return design.get('folders');
        },

        getModelIdString: function ()
        {
            return "id";
        }
    });

    return new harnessLayoutsView();
});