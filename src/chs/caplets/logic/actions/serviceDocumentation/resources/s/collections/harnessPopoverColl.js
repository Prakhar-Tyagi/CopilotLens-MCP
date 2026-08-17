/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define(["PopoverItem", "models/selectedSystem"],
    function (PopoverItem, selectedSystem)
    {
        "use strict";
        var HarnessColl = PopoverItem.extend({
            model: Backbone.Model.extend({idAttribute: "cid"}),
            applyFilter: true,
            getHarnessLayouts: function (harnesses)
            {
                var HarnessLayoutsColl = Backbone.Collection.extend({}), har, that = this;
                this.reset();

                harnesses.forEach(function (harness)
                {
                    that.addIfUnopened(harness);
                });
                return this;

            },
            addIfUnopened: function (harness)
            {
                if (this.isUnopened(harness, "harnessLayoutDiagram") &&
                        this.isUnopened(harness, "oldDesignRevision") &&
                        this.isUnopened(harness, "newDesignRevision")) {
                    var harLayout = this.setMainText(harLayout, harness);
                    if (harLayout) {
                        this.add(harLayout);
                    }
                }
            },
            isUnopened: function (harness, key)
            {
                var openedDiagram = selectedSystem.get(key);
                return this.excludeOpenDiagrams(openedDiagram, harness);
            },
            setMainTextForDiagramAsSystemsFlow: function (harLayout, harness)
            {
                harLayout.set("mainText", harness.mainText);
            },
            appendDiagramName: function (harLayout, harness)
            {
                harLayout.set("mainText", harLayout.get("mainText") + ":" + harness.mainText);
            },
            isDiagramAsSystemsFlow: function ()
            {
                return getWindowObj().diagramAsSystemsObjectFactoryImpl;
            },
            getSelectedHarnessLayouts: function ()
            {
                return require("harnessLayouts");
            },
            setMainText: function (harLayout, harness)
            {
                if (!harness.id) {
					return;
				}
                harLayout = (typeof diagramAsSystemsFlow !== 'undefined') ?
                        this.getSelectedHarnessLayouts().get(harness.diagramId).clone() :
                        this.getSelectedHarnessLayouts().get(harness.id).clone();
                harLayout.idAttribute = "diagramId";
                harLayout.set("diagramId", harness.diagramId);
                harLayout.set("objectId", harness.objectuId);
                if (this.isDiagramAsSystemsFlow()) {
                    this.setMainTextForDiagramAsSystemsFlow(harLayout, harness);
                }
                else {
                    this.appendDiagramName(harLayout, harness);
                }
                return harLayout;
            }, isDiagramAlreadyOpened: function (harness, openedDiagram)
            {
                return harness.diagramId !== openedDiagram.get("id");
            }, excludeOpenDiagrams: function (openedDiagram, harness)
            {
                return !openedDiagram || this.isDiagramAlreadyOpened(harness, openedDiagram);
            },

            fetch: function (model, triggerEvent)
            {
                this.getData(model);
                this.trigger("reset");
                return this.models;
            },

            initialize: function ()
            {
                PopoverItem.prototype.initialize.call(this);
            },
            getData: function (designObject)
            {
                return this.getHarnessLayouts(designObject.getHarnessLayouts ? designObject.getHarnessLayouts() : []);
            }
        });
        return new HarnessColl();
    });