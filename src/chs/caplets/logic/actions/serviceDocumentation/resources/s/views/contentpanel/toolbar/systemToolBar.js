/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer, Backbone, require, diagramAsSystemsObjectFactoryImpl*/
define(
    [
        "backbone",
        "underscore",
        "jquery",
        "DiagramsPopoverModel",
        "RelatedDataPopoverModel",
        "ReportsPopoverModel",
        "preferences",
        "views/navigationPanelView",
        "currentPackage",
        "views/appNameAndLogo/appNameAndLogoView"
    ],
    function (Backbone, _, $, diagramsModel, relatedDataModel, reportsModel, preferences, navigationPanelView,
            currentPackage, appNameAndLogoView) {
        "use strict";

        var SystemButtons, p = mentor.publisher, Model = Backbone.Model.extend({}), model = new Model();
        SystemButtons = Backbone.View.extend({
            el: "<div></div>",
            model: model,
            faceViewSymbolHandler: "",

            events: {
                "click .navigation-panel-toggle": "toggleNavigationPanel",
                "click .diagrams-button": "showDiagrams",
                "click .reports-button": "showReports",
                "click .related-data-button": "showObjects",
                "click .face-view-button": "showFaceViews",
            },

            toggleNavigationPanel: function (event) {
                navigationPanelView.toggleVisibility();
                mentor.publisher.stopEventFlow(event);
            },

            showDiagrams: function (event)
            {
                mentor.publisher.systemData.showDiagrams(event.clientX, event.clientY, mentor.publisher.selectedSystem.get("systemId"),
                    diagramsModel);
                event.stopPropagation();
            },

            showReports: function (event)
            {
                mentor.publisher.systemData.showReports(event.clientX, event.clientY, mentor.publisher.selectedSystem.get("systemId"),
                    reportsModel);
                event.stopPropagation();
            },

            showObjects: function (event)
            {
                mentor.publisher.systemData.showReferences(event.clientX, event.clientY, mentor.publisher.selectedSystem.get("systemId"),
                        relatedDataModel);
                event.stopPropagation();
            },

            showFaceViews: function (evt) {
                if (this.faceViewSymbolHandler && this.faceViewSymbolHandler.showFaceViews) {
                    this.faceViewSymbolHandler.showFaceViews(evt);
                }
            },
            getCurrentProject: function () {
                return mentor.publisher.project;
            },

            enableFaceViewsNavigation: function (config) {
                this.faceViewSymbolHandler = config.faceViewSymbolHandler;
                this.$(".face-view-button").show();
            },
            initialize: function ()
            {
                var that = this;

                preferences.on("change:language", this.localizeButtons, this);
            },
            translateToolbarContent: function ()
            {
                var translator;
                translator = mentor.publisher.languageTranslator;

                this.$(".diagrams-button").html(translator.localize("DiagramsButtonTitle"));
                this.$(".diagrams-button").prop("title", translator.localize("DiagramsButtonTooltip"));
                this.$(".reports-button").html(translator.localize("ReportsButtonTitle"));
                this.$(".reports-button").prop("title", translator.localize("ReportsButtonToolTip"));
                this.$(".related-data-button").html(translator.localize('RelatedDataButtonTitle'));
                this.$(".related-data-button").prop("title", translator.localize("RelatedDataButtonToolTip"));

                this.$(".face-view-button").html(translator.localize('FaceViewButtonTitle'));
                this.$(".face-view-button").prop("title", translator.localize("FaceViewButtonToolTip"));

                this.$(".navigation-panel-toggle").prop("title", translator.localize("NavigationPanelToggleToolTip"));

                var translatedTitle = Utils.translate(this.title);
                if (this.computeTitle) {
                    var currentLang = currentPackage.get("language");
                    translatedTitle = this.computeTitle(currentLang);
                }
                this.$(".component-label").html(translatedTitle);
            },
            localizeButtons: function ()
            {
                setTimeout(this.translateToolbarContent.bind(this), 100);
            },

            isDocumentTypeActive: function (options)
            {
                if (this.$('.diagrams-button').length == 0) {
                    return false;
                }

                var system = mentor.publisher.project.getObjectById(mentor.publisher.selectedSystem.get("systemId")), noOfDiagrams = system.getDiagrams &&
                    system.getDiagrams().length;
                //for index1.html flow where show each diagram as system in navigation panel , diagram button should
                // not be displayed
                if (diagramAsSystemsObjectFactoryImpl) {
                    return false;
                }
                return (system.getDiagrams && noOfDiagrams > 1) ||
                    (noOfDiagrams === 1 && !mentor.publisher.selectedSystem.get("diagramId"));
            },

            isReportsBtnActive: function (options)
            {
                if (this.$('.reports-button').length == 0) {
                    return false;
                }

                var system = mentor.publisher.project.getObjectById(mentor.publisher.selectedSystem.get("systemId")),
                        noOfReports = system.getReports &&
                                system.getReports().length;
                return (system.getReports && noOfReports > 1) ||
                        (noOfReports === 1 && !mentor.publisher.selectedSystem.get("reportId"));
            },

            isObjectsBtnActive: function (options) {
                return this.$('.related-data-button').length != 0;
            },
            render: function (options) {
                var template;
                if (!options) {
                    return;
                }

                this.title = options.title || "";
                this.computeTitle = options.computeTitle;
                this.$el.append(_.template(SystemButtons.templateHTML)(options));

                //hide face view symbol, it will be enabled when there are multiple symbols available
                $(".face-view-button", this.$el).hide();

                if (!options.isSystem) {
                    this.$('.diagrams-button').remove();
                    this.$('.reports-button').remove();
                    this.$('.related-data-button').remove();
                }
                else {
                    this.model.set(options);
                }
                appNameAndLogoView.updateApplicationNameAndLogo(this);

                if (!this.isDocumentTypeActive(options)) {
                    $(".diagrams-button", this.$el).hide();
                }

                if (!this.isObjectsBtnActive(options)) {
                    $(".related-data-button", this.$el).hide();
                }

                if (!this.isReportsBtnActive(options)) {
                    $(".reports-button", this.$el).hide();
                }
                this.localizeButtons();
                this.delegateEvents();
                return this;
            }
        });

        return SystemButtons;
    }
);

mentor.publisher.defaultEventHandler = function (events)
{
    "use strict";

    return {
        panelItemClicked: function (event)
        {
            var eventsToGenerate = events ||
                [], index, length = eventsToGenerate.length, data = event.detail;
            data.x = event.clientX;
            data.y = event.clientY;
            if ($(event.target).hasClass(mentor.publisher.listItemConstants.popoutClass)) {
                mentor.publisher.popoutHandler.openPopoutWindow({data: data, events: eventsToGenerate});
            }
            else {
                for (index = 0; index < length; index = index + 1) {
                    mentor.publisher.eventDispatcher.dispatchEvent(eventsToGenerate[index],
                        event.detail);
                }
            }
            /* highlightListElement(event);*/
            event.stopPropagation();
        },
        mouseover: function (event)
        {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                event);
        },
        mouseleave: function (event)
        {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP,
                event);
        },
        mousemove: function (event)
        {
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_TOOL_TIP,
                event);
        }
    };
};

mentor.publisher.relatedDataEventHandler = function ()
{
    "use strict";
    var p = mentor.publisher,
        events = [p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS],
        that = p.defaultEventHandler(events), eventHandler = p.object(that), highlightListElement = function (evt)
        {
            $("#detailPopup .popoutListItemSelected").removeClass("popoutListItemSelected");
            $(evt.target).addClass("popoutListItemSelected");
        };
    eventHandler.panelItemClicked = function (event)
    {
        that.panelItemClicked(event);
        highlightListElement(event);
    };
    return eventHandler;
};

mentor.publisher.systemData = (function (p)
{
    "use strict";
    var populatePopoverContent, customDataview = [], createPopover, counter = 0, xLoc, yLoc, config, setLocation, inactivateElementsNotInCurrDiagram, createObjectsListGroup, identifyActiveObjectsInCurrentDiagram;
    return {
        showDiagrams: function (x, y, systemId, model)
        {
            var diagrams = p.project.getObjectById(systemId).getDiagrams(), gListArray = [], diagramListModel, listGroup;
            model.load(x, y, diagrams);
        },
        showReports: function (x, y, systemId, model)
        {
            var reports = p.project.getObjectById(systemId).getReports(), gListArray = [], reportListModel, listGroup;
            model.load(x, y, reports);
        },
        showReferences: function (x, y, systemId, model)
        {
            var system = p.project.getObjectById(systemId), connectors, gListArray = [], devices, signals, highlightSelectedElement, timeoutVar, listGroupModel, listGroup, eventHandler = mentor.publisher.relatedDataEventHandler(), customList =
                [], order = p.dataLoader.getRelatedDataPanelOrder(p.project.getId(),
                systemId), i, diagramGeneratorsDataOrder, diagramId = mentor.publisher.selectedSystem.get("diagramId");
            model.load(x, y, system);
            customDataview = [];

            this.showCustomData(p.project.getId(), system, order);
            require(["PopoverFilterModel"],
                function (PopoverFilterModel)
                {
                    PopoverFilterModel.on("change:searchText", function (panelView)
                    {
                        var index;
                        for (index in customDataview) {
                            if (customDataview.hasOwnProperty(index)) {
                                customDataview[index].render();
                            }
                        }

                    }, this);
                });

            //show the diagram level custom panels
            if (diagramId) {
                //the config file which has the order is found at the diagram level
                diagramGeneratorsDataOrder =
                    p.dataLoader.getRelatedDataPanelOrder(p.project.getId(), systemId, diagramId);
                this.showDiagramLevelCustomData(p.project.getId(), systemId, diagramId, diagramGeneratorsDataOrder);
            }
        },
        createCustomDataPanel: function (customDataPanel, title)
        {

            require(["views/p/relatedData/customDataView", "PopoverItem"],
                function (CustomDataView, PopoverItem)
                {
                    var view, localCounter, customDataCollection, CustomDataCollection = PopoverItem.extend(
                        {
                            applyFilter: true,
                            getData: function (data)
                            {
                                return this;
                            }
                        }
                    );
                    view = new CustomDataView();

                    customDataCollection = new CustomDataCollection();
                    customDataCollection.set(mentor.publisher.LanguageFilteredProject.applyFilter(customDataPanel));

                    counter = counter + 1;
                    localCounter = counter;
                    view.getClassName = function ()
                    {
                        return "customData" + localCounter;
                    };
                    view.collection = customDataCollection;
                    view.container = CustomDataView.container;
                    view.templateHTML = CustomDataView.templateHTML;
                    view.getTitle = function ()
                    {
                        return title;
                    };
                    view.render();
                    customDataview.push(view);
                });

        },
        showCustomData: function (projectId, system, panelsInOrder)
        {
            var systemId = system.getId(), index, panel, panelData;
            for (index in panelsInOrder) {
                if (panelsInOrder.hasOwnProperty(index)) {
                    panel = panelsInOrder[index];
                    panelData = p.project.getData(panel, systemId);
                    this.createCustomDataPanel(panelData, panel);
                }
            }

        },
        showDiagramLevelCustomData: function (projectId, systemId, diagramId, panelsInOrder)
        {
            var index, panel, panelData;
            for (index in panelsInOrder) {
                if (panelsInOrder.hasOwnProperty(index)) {
                    panel = panelsInOrder[index];
                    panelData = p.project.getData(panel, systemId, diagramId);
                    this.createCustomDataPanel(panelData, panel);
                }
            }
        }
    };
}(mentor.publisher));
