/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, window, $, mentor*/
define([
    'jquery',
    'underscore',
    'backbone',
    "currentPackage",
    "fileDisplayHandler",
    "ListView",
    "SectionCollection",
    "ComponentLoader"
], function ($,
             underscore,
             Backbone,
             selectedPackage,
             fileDisplayHandler,
             listView,
             BaseCollection,
             ComponentLoader) {
    "use strict";
    var shown = false, View = Backbone.View.extend({

        getData: function () {
            return mentor.publisher.project.getCustomData() || [];
        },

        initialize: function () {
            selectedPackage.on("change:id", this.render.bind(this));
        },

        getPanelData: function (panelType) {
            return mentor.publisher.project.getData(panelType);
        },

        loadViewForCustomData: function (panel, panelDataCollection) {

            var panelView = ComponentLoader.getComponentViewByName(panel), that = this;
            if (panelView) {
                panelView.render();
            } else {
                that.useDefaultView(panel, panelDataCollection);
            }
        },

        useDefaultView: function (panel, panelDataCollection) {
            var customView, cssClass, CustomView;
            cssClass = panel.replace(/ /g, "_");
            CustomView = listView(panelDataCollection).extend({
                title: panel,
                genKey: panelDataCollection.genKey,
                cssClass: cssClass,
                getData: function () {
                    return panelDataCollection;
                },
                getTitle: function ()
                {
                    if(this.genKey && this.genKey != "") {
                        var titleText = mentor.publisher.languageTranslator.localize(this.genKey);
                        if(titleText !== this.genKey) {
                            return titleText;
                        }
                    }
                    return mentor.publisher.languageTranslator.localize(this.title) || this.title;
                },
                clicked: function (evt) {
                    var id = $(evt.currentTarget).attr('data-id'),
                    content = this.getData().get(id);
                    if (content) {
                        content.mainText = content.get("mainText");
                        content.path = content.get("path");
                        content.reset = true;

                        fileDisplayHandler.display(content);
                    }
                },

                popOut: function (event) {
                    var id = $(event.target).parent().attr('data-id'),
                        selectedElement = this.getData().get(id),
                        p = mentor.publisher,
                        type;

                    if (!selectedElement) {
                        return;
                    }

                    type = selectedElement.get("type") || selectedElement.type ||
                        mentor.publisher.contentType.CUSTOM_VIEW;
                    //if the type is not known, the pop-out cannot work.
                    if (!type) {
                        return;
                    }
                    if (!shown) {
                        shown = true;
                        setTimeout(function () {
                            p.popoutHandler.openPopout(p.popoutHandler.createURL({
                                type: type,
                                mainText: selectedElement.get("mainText"),
                                path: selectedElement.get("path"),
                                projectId: selectedPackage.get("id")
                            }));
                            shown = false;
                        }.bind(this), 1000);
                    }

                    event.stopPropagation();
                },

                beforeViewRender: function () {
                    $("." + cssClass + "> div").remove();
                },
            });
            customView = new CustomView();
            customView.templateHTML = this.templateHTML;
            customView.container = this.container + ">." + cssClass;
            customView.render();
            return customView; // mainly for testing
        },
        createCustomPanel: function (panel, genKey) {
            var paneldata = this.getPanelData(panel),
                PanelDataCollection = BaseCollection.extend({
                    getData: function () {
                        return paneldata;
                    },
                    getIdToFilter: function (item) {
                        if (item.has('idForSearch')) {
                            return item.get('idForSearch');
                        }
                        return item.get("id");
                    }
                }),
                panelDataCollection = new PanelDataCollection(),
                CustomView,
                customView;

            panelDataCollection.category = mentor.publisher.languageTranslator.localize(panel);
            panelDataCollection.set(paneldata);
            panelDataCollection.genKey = genKey;

            this.loadViewForCustomData(panel, panelDataCollection);

        },
        panelAlreadyCreated: function (type) {
            /**
             * if there is alreay a panel for the given type then return true
             */
            return $("." + type.replace(/ /g, "_") + ">div").length > 0;
        },

        render: function () {
            var index, customPanel, panels = this.getData(), isThereAReportWithSameType;
            if (panels.length > 0) {
                for (index in panels) {
                    if (panels.hasOwnProperty(index)) {
                        isThereAReportWithSameType = this.panelAlreadyCreated(panels[index].type);
                        /**
                         * There is already a report with the type, so dont show this again
                         */
                        if (!isThereAReportWithSameType) {
                            this.createCustomPanel(panels[index].type, panels[index].genKey || "");
                        }
                    }

                }

            }
        }

    });

    return new View();

});
