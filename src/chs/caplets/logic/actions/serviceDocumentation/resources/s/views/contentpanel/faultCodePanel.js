/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer, $, setTimeout, require*/
mentor.publisher.faultCodeTableGenerator = (function (p)
{
    "use strict";
    return {
        getObjects: function (faultCodeData)
        {
            var faultCodeObjects = p.filter.applyFilter(p.dataLoader.loadFaultCodeById(faultCodeData.get('id') ||
                faultCodeData.id)) ||
                [];
            return p.configurationsBasedOtherFilter.applyFilter(faultCodeObjects);
        }
    };

}(mentor.publisher));

define(["backbone", "underscore", "jquery", "models/selectedSystem", "views/contentpanel/toolbar/contentToolBar",
        "currentPackage"],
    function (Backbone, underscore, $, selectedSystem, Toolbar, currentPackage)
    {
        "use strict";

        var faultCodeInformationModel = Backbone.Model.extend(), FaultCodeObjects, faultCodeObjects, FaultCodePanelView;
        FaultCodeObjects = Backbone.Collection.extend({
            model: faultCodeInformationModel
        });
        faultCodeObjects = new FaultCodeObjects();

        FaultCodePanelView = Backbone.View.extend({
            events: {
                "click tr": "clicked",
                "click .closeBtn": "close"
            },

            close: function ()
            {
                this.selectedSystem.set("faultCode", "", {silent: true});
                this.undelegateEvents();
                this.$el.html('');
            },
            currentPackage: currentPackage,

            selectedSystem: selectedSystem,

            highlightObject: function (event)
            {
                var data = event.detail, objectId = data.objectId;
                $(".selected", this.$el).each(function ()
                {
                    $(this).removeClass("selected");
                });
                $("tr[data-id='" + data.cid + "']>td", this.$el).each(
                    function ()
                    {
                        $(this).addClass("selected");
                    }
                );
            },

            clicked: function (event)
            {
                var p = mentor.publisher, cid = $(event.currentTarget).attr("data-id"), faultCodeInfo, data = {};
                faultCodeInfo = faultCodeObjects.get(cid);
                if ($("td", $(event.currentTarget)).hasClass("selected")) {
                    faultCodeInfo.id = faultCodeInfo.get("objectId");
                    faultCodeInfo.x = event.clientX;
                    faultCodeInfo.y = event.clientY;
                    faultCodeInfo.objectId = faultCodeInfo.get("objectId");
                    faultCodeInfo.systemId = faultCodeInfo.get("systemId");
                    event.detail = faultCodeInfo;
                    p.eventDispatcher.dispatchEvent(p.events.OPEN_OBJECT_POPUP,
                        faultCodeInfo);

                }
                else {
                    faultCodeInfo.id = faultCodeInfo.get("systemId");
                    faultCodeInfo.systemId = faultCodeInfo.id;
                    faultCodeInfo.type = mentor.publisher.contentType.SYSTEM_SVG;
                    faultCodeInfo.idToHighlight = faultCodeInfo.get("objectId");
                    faultCodeInfo.objectId = faultCodeInfo.get("objectId");
                    faultCodeInfo.diagramName = faultCodeInfo.get("diagramName");
                    faultCodeInfo.diagramName = faultCodeInfo.get("diagramName");
                    faultCodeInfo.diagramId = faultCodeInfo.get("diagramId");
                    faultCodeInfo.path = mentor.publisher.project.getId() + "/" + faultCodeInfo.get("path");
                    faultCodeInfo.reset = false;
                    //this may get overridden for diagrams as system case
                    faultCodeInfo.id = getIdToHighlight(faultCodeInfo);
                    selectedSystem.set("objectId", faultCodeInfo.idToHighlight, {silent: true});
                    event.detail = faultCodeInfo;
                    this.displayFaultCode(faultCodeInfo, event);
                    $(".selected", this.$el).removeClass("selected");
                    $("td", $(event.currentTarget)).addClass("selected");
                    crossHighlightHandler.initCrossHighlight(faultCodeInfo.get("objectId"));
                }
                event.stopPropagation();
            },

            displayFaultCode: function (faultCodeInfo, event)
            {
                require(["fileDisplayHandler"], function (fileDisplayHandler)
                {
                    fileDisplayHandler.display(faultCodeInfo);
                });
            },

            initialize: function ()
            {
                var that = this;
                this.currentPackage.on("change:language", this.render, this);
                currentPackage.on("change:vin", this.render, this);
                this.selectedSystem.on("change:optionExpression", this.render, this);
                //when fault code is changed then re-render the view
                this.selectedSystem.on("change:faultCode", this.render, this);

            },

            clearContainer: function ()
            {
                this.setElement(this.container);
                this.$el.html('');
            },

            getTitle: function ()
            {
                var title = this.selectedSystem.get("faultCode").get("mainText"), subTitle = this.selectedSystem.get("faultCode").get("subText") ||
                    "";
                return title + (subTitle.trim() ? ", " + subTitle : "");
            },

            render: function ()
            {
                var toolbar, template, that = this;
                if (this.selectedSystem.get("faultCode") && this.selectedSystem.get("faultCode").get("mainText")) {
                    this.clearContainer();
                    mentor.publisher.contentArea.closeExistingPanel({
                            type: mentor.publisher.contentType.FAULT_CODE,
                            systemId: that.selectedSystem.get("systemId")
                        },
                        that);

                    faultCodeObjects.reset();
                    faultCodeObjects.set(mentor.publisher.faultCodeTableGenerator.getObjects(this.selectedSystem.get("faultCode")));

                    //compile template
                    template = underscore.template(this.templateHTML)({
                        items: faultCodeObjects.models,
                        title: this.selectedSystem.get("faultCode").get("mainText") +
                                ", " +
                                this.selectedSystem.get("faultCode").get("subText")
                    });

                    //create toolbar
                    toolbar = new Toolbar();
                    this.$el.append(toolbar.render({
                        type: mentor.publisher.contentType.FAULT_CODE,
                        title: this.getTitle()
                    }).$el);

                    //add content
                    this.$el.append(template);
                    setTimeout(function ()
                    {
                        //re layout content panel
                        mentor.publisher.contentArea.layoutContentPanel({
                            type: mentor.publisher.contentType.FAULT_CODE,
                            title: that.getTitle(),
                            id: that.selectedSystem.get("faultCode").get("id"),
                            systemId: that.selectedSystem.get("systemId")
                        });
                        $(".popOutBtn", that.$el).hide();
                    }, 10);
                    return this;
                }
            }
        });

        return new FaultCodePanelView();
    });


