/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, setInterval, clearInterval, $, mentor, resizeViewer, Backbone*/
define(["ListGroupView", "models/selectedSystem", "currentPackage", "models/navPanelModel"],
    function (ListGroupView, selectedItem, currentPackage, navigationPanelModel)
    {
        "use strict";

        var hidePanel,
            showPanel;

        hidePanel = function (panelView)
        {
            panelView.model.set("width", $("#detail").css("left"));

            $("#navigation").hide();
            $("#detailNavigationResizeBar").hide();
            $("#detail").css("left", "0");

            panelView.model.set("visible", false);

            resizeViewer();
        };

        showPanel = function (panelView)
        {
            var width;

            width = panelView.model.get("width");

            $("#navigation").show();
            $("#detailNavigationResizeBar").show();
            $("#detail").css("left", "");
            $("#detail").css("left", width);

            panelView.model.set("visible", true);

            resizeViewer();
        };

        var NavigationPanelView = ListGroupView.extend({
            initialize: function ()
            {
                selectedItem.on("change:id", this.highlightElement, this);
                selectedItem.on("change:clearNavigationPanelSelection", this.dehighlightSelectedElement, this);
                selectedItem.on("scrollNavigationPanelToTheSelectedElement", this.scrollToTheSelectedElement, this);
				navigationPanelModel.on("scrollNavigationPanelToTheSelectedElement", this.scrollToTheSelectedElement, this);

                this.model = new Backbone.Model({
                    visible: true,
                    width: "21%"
                });
            },
            events: {
                "click #platform-grouped-list .titlebar": "toggleSection"
            },
            findElementById: function (elementId)
            {
                var selectedElement = $('.listItem[data-id="' + elementId + '"]', this.$el);
                if (selectedElement.length <= 0) {
                    //in case of dynamic mode(configuration filtering) , the base id is the one which matches
                    //not the primary id. primary id is combination of system id and configuration which is unique
                    //while base id is the system id which is not unique in the dynamic mode
                    selectedElement = $('.listItem[base-id="' + elementId + '"]', this.$el);
                }
                return {elementId: elementId, selectedElement: selectedElement};
            }, findElementToHighlight: function ()
            {
                var elementId = selectedItem.get("id") ||
                    selectedItem.get("systemId");
                return this.findElementById(elementId);
            },
            scrollToTheSelectedElement: function ()
            {
                var __ret = this.findElementToHighlight(), selectedElement;
                selectedElement = __ret.selectedElement;
                setTimeout(function ()
                {
                    var offset = $(selectedElement).offset();
                    if (offset) {
                        $('#platform-grouped-list>div').animate({
                            scrollTop: (offset.top - $(selectedElement).width())
                        }, 100);
                    }
                });
            },
            dehighlightSelectedElement: function (options)
            {
                if (options) {
                    var id = options.id;
                    if (id) {
                        var elementToDehightlight = this.findElementById(id);
                        $(elementToDehightlight.selectedElement).removeClass("highlight");
                    }
                }
                else {
                    $(".highlight").removeClass("highlight");
                    selectedItem.set("id", "", {silent: true});
                    selectedItem.set("selectedElement", "", {silent: true});
                }
            }, highlightElement: function ()
            {
                var __ret = this.findElementToHighlight(), elementId, selectedElement;
                elementId = __ret.elementId;
                selectedElement = __ret.selectedElement;
                if (selectedElement.length <= 0) {
                    return false;
                }
                if (!navigationPanelModel.get("supportMultipleHighlight")) {
                    $(".highlight").removeClass("highlight");
                }
                var that = this;
                if(this.highlightTimeoutId) {
                    clearTimeout(this.highlightTimeoutId)
                }
                this.highlightTimeoutId = setTimeout(function(){
                    $(selectedElement).addClass("highlight");
                    selectedItem.set("selectedElement", elementId, {silent: true});
                    if (!that.isPanelTreeView(selectedElement)) {
                        that.expandFirstPanel(selectedElement);
                    } else {
                        that.expandTreePanel(selectedElement);
                    }
                    that.highlightTimeoutId = '';
                }, 800);
            },
            isPanelTreeView: function(selectedElement)
            {
                var treeList = $(selectedElement).closest("ol.tree");
                return treeList.length > 0;
            },
            expandFirstPanel: function (selectedElement)
            {
                var section = $(selectedElement).closest(".listPanel");
                if ($(".list-content", section).length > 0) {
                    $(".list-content", section).toggle(true);
                } else {
                    $(".listItem", section).toggle(true);
                }
            },

            expandTreePanel: function(selectedElement)
            {
                var section = $(selectedElement).closest(".listPanel");
                $('ol.tree', section).show();
                $(selectedElement).parents('ol').children('li').children('input[type="checkbox"]').prop("checked", true);
            },

            render: function ()
            {
                var navigationPanelOrderXML = mentor.publisher.dataLoader.getNavigationPanelOrder(currentPackage.id),
                    index,
                    panel,
                    firstPanel = true;
                this.setElement(this.container);
                for (index in navigationPanelOrderXML) {
                    if (navigationPanelOrderXML.hasOwnProperty(index) && navigationPanelOrderXML[index].title) {
                        panel = $("<div class='listPanel " +
                        navigationPanelOrderXML[index].title.replace(/ /g, '_') + " auto-list'></div>");
                        /**
                         * add data tag to indicate it is first panel
                         */
                        if (firstPanel) {
                            firstPanel = false;
                            $(panel).attr("data-firstPanel", "true");
                        }
                        $("#platform-grouped-list>div").append(panel);
                    }
                }
               
                if (mentor.publisher.config.navHidden) {
                    hidePanel(this);
                }

            },

            hidePanel: function() {
                hidePanel(this);
            },

            showPanel: function() {
                showPanel(this);
            },

            toggleVisibility: function ()
            {
                this.model.get("visible") ? hidePanel(this) : showPanel(this);
                setTimeout(function ()
                {
                    mentor.publisher.detailLayoutManager.reLayoutOnWindowResize();
                }, 100);
            },

            onWindowResize: function (event)
            {
                var width;

                width = "21%";

                this.model.set("width", width);

                $("#detailNavigationResizeBar").css('left', width);

                $("#navigation").width(width);
                if ($("#navigation").is(':visible')) {
                    $("#detail").css('left', width);
                }

                resizeViewer();
            }

        });

        return new NavigationPanelView();
    });