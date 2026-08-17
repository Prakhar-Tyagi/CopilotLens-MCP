/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("TwoDSVGEventHandlerTest", function () {
    beforeEach(function () {
        $('body').append('<div id="svg-container">' +
                '<svg id="twoDSVG" xmlns="http://www.w3.org/2000/svg" width="100px" height="50px">' +
                '<g id="1">' +
                '   <desc>chs.cof.logical.schem.CAFPinList schem1 conn1</desc>>' +
                '   <hotspot>P1</hotspot>' +
                '   <hotspot>P2</hotspot>' +
                '   <text x="10" y="20" stroke-width="1">ILC1</text>' +
                '</g>' +
                '<g id="2">' +
                '   <text x="100" y="200" stroke-width="1">ILC2</text>' +
                '</g>' +
                '<g id="3">' +
                '   <desc>chs.cof.logical.schem.CAFPinList schem2 conn2</desc>>' +
                '   <hotspot>P23</hotspot>' +
                '   <hotspot>P31</hotspot>' +
                '   <text x="10" y="20" stroke-width="1">ILC112</text>' +
                '   <circle x="100" y="200" r="10" stroke-width="1"></circle>' +
                '</g>' +
                '<g id="4">' +
                '   <desc>chs.cof.logical.schem.CAFPinList schem3 conn3</desc>>' +
                '   <text x="100" y="200" stroke-width="1">ILC221</text>' +
                '   <circle x="100" y="200" r="2" stroke-width="1"></circle>' +
                '</g>' +
                '<text x="300" y="400" stroke-width="1">SP1</text>' +
                '</svg>' +
                '</div>')
    });

    function init(twoDSVGEventHandler)
    {
        var paths = new Map();
        paths.set("P1", [{id: "1", diagramName: "dia1"}]);
        paths.set("P2", [{id: "2", diagramName: "dia2"}]);
        paths.set("ILC1", [{id: "3", diagramName: "dia3"}]);
        paths.set("ILC2", [{id: "4", diagramName: "dia4"}]);
        paths.set("P23", [{id: "5", diagramName: "dia5"}]);
        paths.set("P31", [{id: "6", diagramName: "dia6"}]);
        paths.set("ILC112", [{id: "7", diagramName: "dia7"}]);
        paths.set("P100", [{id: "9", diagramName: "dia9"}]);
        paths.set("SP1", [{id: "10", diagramName: "dia10"}]);
        mentor.publisher.colors = {};
        mentor.publisher.constants.redColorMsg = "red";
        mentor.publisher.colors['red'] = 'red';
        twoDSVGEventHandler.root = document.getElementById("twoDSVG");
        twoDSVGEventHandler.hotSpotText = "ILC1";
        twoDSVGEventHandler.svgContainerId = "svg-container";
        twoDSVGEventHandler.getSystemPaths = function (textValue) {
            return paths.get(textValue);
        };
        twoDSVGEventHandler.getSVGDocument = function () {
            return this.root;
        };
    };

    function getSVGElement()
    {
        return $('body').find('div').find('svg');
    }

    function validateCrossHighlightBehaviour(twoDSVGEventHandler, uid, goldenHighElesLen)
    {
        var eleToHighlight, highlighColor, callCount = 0, actualName,
                actualModel, origFn1, origFn2, actualHighElesLen, displayFn;
        origFn1 = mentor.publisher.locationViews.doesObjectHasLinksWithin2dView;
        origFn2 = mentor.publisher.locationViews.getLocationViewByObjectId;
        displayFn = window.crossHighlightHandler.flushZoomedViews;
        try {
            mentor.publisher.locationViews.doesObjectHasLinksWithin2dView = function (txt) {
                return txt;
            };
            mentor.publisher.locationViews.getLocationViewByObjectId = function (txt) {
                return txt == "conn3" ? 'SP1' : null;
            };
            window.crossHighlightHandler.flushZoomedViews = function(){};
            twoDSVGEventHandler.setHighLightMap = function (highEles) {
                actualHighElesLen = highEles.length;
            };
            twoDSVGEventHandler.highlightUid(uid, 'red', false);
        }
        catch (e) {
        }
        finally {
            mentor.publisher.locationViews.doesObjectHasLinksWithin2dView = origFn1;
            mentor.publisher.locationViews.getLocationViewByObjectId = origFn2;
            window.crossHighlightHandler.flushZoomedViews = displayFn;
        }
        expect(actualHighElesLen).toEqual(goldenHighElesLen);
    };

    function validateClickBehaviour(twoDSVGEventHandler, element, goldenHighElesLen, textToMatch, ss)
    {
        var eleToHighlight, highlighColor, callCount = 0, actualName,
                actualModel, origFn1, origFn2, highElesLen, displayFn;
        displayFn = display2DViewsAttributes;
        origFn1 = mentor.publisher.locationViews.doesObjectHasLinksWithin2dView;
        origFn2 = mentor.publisher.locationViews.getLocationViewByObjectId;
        try {
            mentor.publisher.locationViews.doesObjectHasLinksWithin2dView = function (txt) {
                return txt;
            };
            mentor.publisher.locationViews.getLocationViewByObjectId = function (txt) {
                return txt == "conn3" ? 'SP1' : null;
            };
            twoDSVGEventHandler.setHighLightMap = function (highEles) {
                highElesLen = highEles.length;
            };
            display2DViewsAttributes = function (name, x, y, uidToHighlight, model, callback) {
                actualName = name;
                actualModel = model;
            };
            twoDSVGEventHandler.doHandleEvent("click", 0, 0, false, element);
        }
        catch (e) {
        }
        finally {
            mentor.publisher.locationViews.doesObjectHasLinksWithin2dView = origFn1;
            mentor.publisher.locationViews.getLocationViewByObjectId = origFn2;
            display2DViewsAttributes = displayFn;
        }
        expect(highElesLen).toEqual(goldenHighElesLen);
        expect(actualName).toEqual(textToMatch);
        ss.forEach(function (s) {
            expect(actualModel.get(s)).toBeDefined();
        });
    };

    it("Click on text element should give matches for text and hotspots", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        var element = getSVGElement().find('#1').find('text')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 2, 'P1,P2,ILC1', ['P1', 'P2', 'ILC1']);
    });

    it("Click on text element without other hotspots should still show matches for text", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        var element = getSVGElement().find('#2').find('text')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 1, 'ILC2', ['ILC2']);
    });

    it("Click on circle or text element should give matches for text and hotspots", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        var element = getSVGElement().find('#3').find('text')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 3, 'P23,P31,ILC112', ['P23', 'P31', 'ILC112']);
        element = getSVGElement().find('#3').find('circle')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 3, 'P23,P31', ['P23', 'P31']);
    });

    it("Click on text element should give matches based on description", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        var element = getSVGElement().find('#4').find('text')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 3, 'SP1', ['SP1']);
        element = getSVGElement().find('#4').find('circle')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 3, 'SP1', ['SP1']);
    });

    it("Click on text element outside g element should show matches for text", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        var element = getSVGElement().children('text')[0];
        validateClickBehaviour(twoDSVGEventHandler, element, 1, 'SP1', ['SP1']);
    });

    it("Highlight UID based on UID match", function () {
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        init(twoDSVGEventHandler);
        validateCrossHighlightBehaviour(twoDSVGEventHandler, "conn1", 2);
        validateCrossHighlightBehaviour(twoDSVGEventHandler, "conn3", 4);
    });

    it("should be able to hightlight element on mouse hover", function(){
        var twoDSVGEventHandler = new TwoDSVGEventHandler();
        twoDSVGEventHandler.doPan='pan';
        init(twoDSVGEventHandler);
        twoDSVGEventHandler.mouseHoverHighLight('sampleDesc', {});

        twoDSVGEventHandler.doPan='noPan';
        var event1={
                target: {
                    nodeName: 'div',
                    id: 'testID',
                    tagName: 'g',
                    parentNode: {
                        id: 'viewport',
                        nodeName: 'div'
                    },
                    getElementsByTagName: function () {return []},
                },
                stopPropagation: function () {},
                type: 'testEventType'
            };
        init(twoDSVGEventHandler);
        spyOn(window.crossHighlightHandler, 'flushZoomedViews').andCallThrough();
        twoDSVGEventHandler.mouseHoverHighLight('sampleDesc', event1);
        expect(window.crossHighlightHandler.flushZoomedViews).toHaveBeenCalled();

        var event2={
                target: {
                    nodeName: 'span',
                    id: 'testID',
                    tagName: 'g',
                    parentNode: {
                        id: 'viewport',
                        nodeName: 'div',
                        parentNode: {
                            nodeName: 'div',
                            id: 'root',
                            tagName: 'g',
                            getElementsByTagName: function () {return []},
                        }
                    },
                    getElementsByTagName: function () {return []},
                },
                stopPropagation: function () {},
                type: 'mouseenter'
            };
        init(twoDSVGEventHandler);
        spyOn(twoDSVGEventHandler, 'doHandleEvent').andCallThrough();
        twoDSVGEventHandler.mouseHoverHighLight('sampleDesc', event2);
        expect(twoDSVGEventHandler.doHandleEvent).toHaveBeenCalled();
    });

    afterEach(function () {
        $("#twoDSVG").remove();
    });

});
