/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, afterEach, mentor*/
(function ()
{
    "use strict";
    var context, spy = mentor.publisher.popoutHandler, stubs, Model = Backbone.Model.extend({

    }), Collection = Backbone.Collection.extend(), fileDisplayHandler = {
        display: function (content)
        {
            this.content = content;
        }
    }, View = function (collection)
    {
        return Backbone.View.extend();
    };

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
    };
    context = createContext(stubs);

    context(['views/zoomToolBarView', "ZoomToolBarModel"],
            function (zoomToolBarView, ZoomToolBarModel)
            {
                var zoomBar, is_touch_device;

                beforeEach(function ()
                {
                    $('body').append($('<div id="zoomToolBar"></div>'));
                     is_touch_device = window.is_touch_device;
                    zoomToolBarView.container = "#zoomToolBar";
                    zoomToolBarView.templateHTML =
                            '<div class="diagramControl toolbar background splitPanel_bottomToolBar_mouseEnter" style="display: block"><div class="zoomout-button component-button"></div><div class="zoomin-button component-button"></div><div class="slider"><div class="sliderNotch component-button" style="left: 10px;"></div></div><div class="component-label" style="">100%</div><div class="zoomall-button component-button"></div></div>';
                    zoomBar = new zoomToolBarView({el: "#zoomToolBar"});
                    window.is_touch_device = function ()
                    {
                        return false;
                    };
                });

                afterEach(function ()
                {
                    $("#zoomToolBar").remove();
                    window.is_touch_device = is_touch_device;
                });

                describe("zoomToolBarViewTest", function ()
                {$('body').on
                    it("should be able to load zoomToolBarView Module", function ()
                    {
                        expect(zoomToolBarView).toBeDefined();
                    });

                    it("should be able to render zoomToolBar View ", function ()
                    {

                        zoomBar.render();

                        zoomBar.hide();
                        expect($('.diagramControl', zoomBar.$el).css("display")).toBe("none");

                        zoomBar.show();
                        expect($('.diagramControl', zoomBar.$el).css("display")).toBe("block");

                        zoomBar.moveZoomSlider(50, 50);
                        expect($('.component-label', zoomBar.$el).html()).toBe("100%");

                        $('body').on("click", function (event)
                        {
                            zoomBar.zoomIn(event);
                        });
                        window.resizeSvg = function ()
                        {

                        };

                        $('body').trigger("click");
                        expect($('.component-label', zoomBar.$el).html()).toBe("100%");

                        $('body').off('click');

                        $('body').on("click", function (event)
                        {
                            zoomBar.zoomOut(event);

                        });

                        $('body').trigger("click");
                        expect($('.component-label', zoomBar.$el).html()).toBe("100%");

                    });

                    it("should be able to show tool tip", function () {
                        var origShowToolTipFromEvent = mentor.publisher.toolTip.showToolTipFromEvent;
                        mentor.publisher.toolTip.showToolTipFromEvent = function () {};
                        spyOn(mentor.publisher.toolTip, "showToolTipFromEvent");
                        zoomBar.showToolTip({});
                        expect(mentor.publisher.toolTip.showToolTipFromEvent).toHaveBeenCalled();
                        mentor.publisher.toolTip.showToolTipFromEvent = origShowToolTipFromEvent;
                    });

                    it("should be able to remove tool tip", function () {
                        spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
                        zoomBar.removeToolTip({});
                        expect( mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
                    });

                    it("should be able to show dynamic tool tip", function () {
                        var origShowToolTipFromEvent = mentor.publisher.toolTip.showToolTipFromEvent,
                            evt1= {
                                currentTarget: $('<div customtooltip-0="ZoomToFit"/>'),
                                altKey: true
                            },
                            evt2= {
                                currentTarget: $('<div customtooltip-0="ZoomToDefault"/>'),
                                altKey: false
                            }
                        ;
                        mentor.publisher.toolTip.showToolTipFromEvent = function () {};
                        spyOn(mentor.publisher.toolTip, "showToolTipFromEvent");
                        zoomBar.showDynamicToolTip(evt1);
                        zoomBar.showDynamicToolTip(evt2);
                        expect(mentor.publisher.toolTip.showToolTipFromEvent).toHaveBeenCalled();
                        mentor.publisher.toolTip.showToolTipFromEvent = origShowToolTipFromEvent;
                    });


                    it("should be able to zoom selected object", function () {
                        var origZoomViews = window.crossHighlightHandler.zoomViews;
                        window.crossHighlightHandler.zoomViews = function () {};
                        spyOn(window.crossHighlightHandler, "zoomViews");
                        zoomBar.zoomSelectedObject();
                        expect(window.crossHighlightHandler.zoomViews).toHaveBeenCalled();
                        window.crossHighlightHandler.zoomViews = origZoomViews;
                    });

                    it("should be able to set zoom level definition notch position", function () {
                        spyOn(zoomBar, "setNotchPosition");
                        zoomBar.setZoomLevelDefinitionNotchPosition();
                        expect(zoomBar.setNotchPosition).toHaveBeenCalled();
                    });

                    it("should be able to fit zoom", function () {
                        zoomBar.zoomFit({altKey: true});
                        var evt = {
                            altKey: false
                        };
                        zoomBar.options.handler = {
                            svgContainerId: "testSvgContainerId"
                        };
                        zoomBar.zoomFit(evt);
                        expect(ZoomToolBarModel.get(zoomBar.el.id).get("currentZoomLevel")).toBe(100);
                    });
                });
            });
})();



