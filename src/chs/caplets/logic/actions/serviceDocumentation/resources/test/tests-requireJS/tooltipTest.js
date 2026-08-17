/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("toolTipTest", function ()
{

    beforeEach(function ()
    {
        $('body').html('<div class="tooltip"></div>');
    });

    it("should be able to translate the name of system tool tip", function ()
    {
        var pre = Utils.translate;
        Utils.translate = function ()
        {
            return "translatedValue";
        };
        var tooltips = [];
        tooltips.push(
                {
                    getName: function ()
                    {
                        return "tooltipName";
                    },
                    getValue: function ()
                    {
                        return "tooltipValue";
                    }}
        );
        mentor.publisher.toolTip.showToolTip(tooltips,
                1,
                1,
                $('body'))
        ;

        expect($(".tooltipName").html()).toBe("translatedValue:");

    });

    it("should be able to show tool tip for name", function () {
        mentor.publisher.toolTip.showToolTipForName("testName", 1, 1, $('body'));
        expect($(".tooltipName").html()).toBe("testName");
    });

    it("should be able to remove tool tip", function () {
        var tooltips = [];
        tooltips.push(
        {
            getName: function ()
            {
                return "tooltipName";
            },
            getValue: function ()
            {
                return "tooltipValue";
            }}
        );
        mentor.publisher.toolTip.showToolTip(tooltips,
                1,
                1,
                $('body'))
        ;
        var temp=$(".tooltipName").html();
        expect($(".tooltipName").html()).toBe('translatedValue:');
        mentor.publisher.toolTip.removeToolTip($('body'));
        expect($(".tooltipName").html()).toBeUndefined();
    });

    it("should be able to register on button", function () {
        var container ={
                on: function (eventName, eventHandler) {eventHandler({detail:{}})},
            }
        ;
        spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
        mentor.publisher.toolTip.registerOnButton(container);
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("should be able to register tool tip based on length", function () {
        var container={
                on: function (event, eventHandler) {eventHandler({detail:{}})},
                0:{
                    offsetWidth: 10,
                    scrollWidth: 0,
                },
            }
        ;
        spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
        mentor.publisher.toolTip.registerToolTipBasedOnLength(container);
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("should be able to add custom css", function () {
        mentor.publisher.toolTip.addCustomCSS({"color":"red", "width":"100px"});
    });

    it("should be able to show tooltip from event", function () {
        var evt={
            currentTarget:'testTarget',
            detail:{},
        };
        spyOn(mentor.publisher.eventDispatcher, "dispatchEvent").andCallThrough();
        mentor.publisher.toolTip.showToolTipFromEvent(evt);
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("Some Text is going out of box whenever hovered", function () {
        var tooltips = [{
            getName: function () {
                return "Tooltip Name";
            },
            getValue: function () {
                return "This is a long text";
            }
        }];
        mentor.publisher.toolTip.showToolTip(tooltips, 1, 1, $('body'));
        var tooltipValueSpans = $('.tooltipValue');
        var element = tooltipValueSpans.eq(0);
        if (element.length > 0) {
            var styleAttribute = element.attr('style');
        }
        expect(styleAttribute).toBe('white-space: pre-wrap');
    });

    afterEach(function () {
        $(".tooltip").remove();
    });

})
;
