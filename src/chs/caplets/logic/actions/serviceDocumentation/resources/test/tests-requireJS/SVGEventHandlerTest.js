/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("SVGEventHandlerTest", function() {
    var svgEventHandler, origCrosshighligher, origHotSpotTextFinder, origEventHandler,
        origColors, origDisplayAttributes, origSelectedSystem, origAlertMsg;
    beforeEach(function() {
        "use strict";

        var svg = $('<svg  height="100%" width="100%" style="cursor: default;"> ' +
                '<g id="viewport" transform="matrix(0.018257280811667442,0,0,0.018257280811667442,66.34995380473265,215.20794717879107)">' +
                '<g id="desc"><desc>chs.cof.logical.schem.CAFShieldBody UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac60a-9e788023cd93580da11edf9eaf55278e UID25b55b-1316adac609-9e788023cd93580da11edf9eaf55278e</desc><path class="C" d="M10534,11354v-1579"/><path class="C" d="M10534,7262 C10362,7262 10223,7401 10223,7573 "/><path class="C" d="M10845,7573 C10845,7401 10706,7262 10534,7262 "/><path class="C" d="M10223,9464 C10223,9635 10362,9775 10534,9775 "/><path class="C" d="M10534,9775 C10706,9775 10845,9635 10845,9464 "/><path class="C" d="M10223,7573v1891"/><path class="C" d="M10845,7573v1891"/></g></g>');
        origCrosshighligher = window.crossHighlightHandler;
        origHotSpotTextFinder = window.HotSpotTextFinder;
        origEventHandler = mentor.publisher.eventDispatcher;
        origColors = mentor.publisher.colors;
        mentor.publisher.colors = {};
        origDisplayAttributes = window.displayAttributes;
        origSelectedSystem = mentor.publisher.selectedSystem;

        mentor.publisher.selectedSystem = {
            id: 'test',
            get: function () {
                return this.id;
            },
            set: function (newId) {
                this.id = newId;
            }
        };
        window.displayAttributes = function (schematicUID, connectivityUID, x, y) {
            that.displayAttributes = JSON.stringify([schematicUID, connectivityUID]);
            return {
                showPopUpPanel: function () {
                    that.popupShown = true;
                }
            };
        };
        mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg] = 'orange';
        origAlertMsg = window.alertMsg;

        window.alertMsg = {
            removeAlertMsg: function () {
                this.removeAlertMsgCalled = true;
            }
        };
        window.crossHighlightHandler = {
            flushZoomedViews: function () {
                this.isCalled = true;
            },
            initCrossHighlight: function () {
                this.initCrossHighlight = true;
            },
            handlerHighlightedElementsMap: []
        };

        mentor.publisher.eventDispatcher = {
            dispatchEvent: function (event) {
                this.generatedEvent = event;
            }
        };
        svgEventHandler = new SVGEventHandler();
        var bodyElem = $('body');
        $(bodyElem).attr("id", "svgContainer");
        svgEventHandler.svgContainerId = "svgContainer";
        $(bodyElem).append(svg);
        $(bodyElem).append($("<object></object>"));

        window.HotSpotTextFinder = {
            findTopGElement: function () {
                return {
                    getBBox: function () {

                    }
                };
            }
        };
    });

    afterEach(function () {
        "use strict";
        window.crossHighlightHandler = origCrosshighligher;
        mentor.publisher.eventDispatcher = origEventHandler;
        mentor.publisher.colors = origColors;
        window.alertMsg = origAlertMsg;
        window.displayAttributes = origDisplayAttributes;
        mentor.publisher.selectedSystem = origSelectedSystem;
        window.HotSpotTextFinder = origHotSpotTextFinder;
    });

    it("test SVG Event Handler should load correctly", function () {
        expect(svgEventHandler).toBeDefined();
    });

    it("test SVG event handler should get initialized properly", function () {
        "use strict";
        svgEventHandler.init($("svg").first()[0])
        expect(svgEventHandler.root).toBeDefined();
    });

    it("test click on SVG should close popover", function () {
        "use strict";
        var gElem = $('svg>g');
        var origPressed = svgEventHandler.pressed;
        var pressedCalled = false;
        svgEventHandler.pressed = function () {
            pressedCalled = true;
        };
        $(gElem).on("click", function (event) {
            svgEventHandler.mouseClickHandler(event);
        });
        $(gElem).trigger("click");

        expect(mentor.publisher.eventDispatcher.generatedEvent).toBe(mentor.publisher.events.CLOSE_POPOVER);
        expect(pressedCalled).toBe(true);

        svgEventHandler.pressed = origPressed;
    });

    it("test handleHighlightEvents should highlight on mouse enter", function () {
        "use strict";
        var wireColor, origHighlightObject;
        origHighlightObject = svgEventHandler.highlightObject;
        svgEventHandler.highlightObject = function (connectivityUID, color, isRenderedSVG) {
            wireColor = color;
        };
        var gEle = $('svg>g');
        $(gEle).on("mouseenter", function (event) {
            svgEventHandler.handleHighlightEvents(event, "mouseenter", "className, schemUID connectivityUID");
        });
        $(gEle).trigger("mouseenter");

        expect(wireColor).toBe(mentor.publisher.colors[mentor.publisher.constants.orangeColorMsg]);

        svgEventHandler.highlightObject = origHighlightObject;
    });

    it("test mouseMoveHanlder should pan when SVG is dragged", function () {
        "use strict";
        var changedColor, origHighlightObject, origGetFitScale, origIsValidElement;
        origHighlightObject=svgEventHandler.highlightObject;
        origGetFitScale = svgEventHandler.getFitScale;
        origIsValidElement = svgEventHandler.isValidElement;

        svgEventHandler.root = $("svg").first()[0];
        svgEventHandler.getGElementToFocus = function () {
            return $('g')[0];
        };

        svgEventHandler.getFitScale = function () {
            return {};
        };
        svgEventHandler.mouseout = '';

        svgEventHandler.highlightObject = function (connectivityUID, color, isRenderedSVG) {
            changedColor = color;
        };
        svgEventHandler.style = {};
        svgEventHandler.isValidElement = function () {return 'sampleDesc';};

        svgEventHandler.mouseDownHandler({
            pageX: 1, pageY: 1, target: {
                tagName: 'svg'
            }
        });
        svgEventHandler.mouseMoveHanlder({
            pageX: 2, pageY: 2, target: {
                tagName: 'svg'
            }
        });

        expect(svgEventHandler.doPan).toBe(true);

        svgEventHandler.mouseClickHandler({
            pageX: 2, pageY: 2, target: {
                tagName: 'svg'
            }
        });

        expect(svgEventHandler.doPan).toBeFalsy();

        expect(changedColor).toBeUndefined();

        svgEventHandler.highlightObject = origHighlightObject;
        svgEventHandler.getFitScale = origGetFitScale;
        svgEventHandler.isValidElement = origIsValidElement;
    });

    it("test mouse leave handler and mouse enter handler", function () {
        "use strict";
        var changedColor;

        svgEventHandler.root = $("svg").first()[0];
        svgEventHandler.style = {};

        var evt = {
            target: {
                timer: 10,
            },
            data: {
                svgHandle: {
                    mouseMoveAndEnter: false,
                    isValidEvent: function () {return true;},
                    resetAttributesAndStack: function () {},
                    mouseout: false,
                }
            }
        };
        svgEventHandler.mouseEnterHandler(evt);
        expect(evt.data.svgHandle.mouseMoveAndEnter).toBe(true);
        svgEventHandler.mouseLeaveHandler(evt);
        expect(evt.data.svgHandle.mouseMoveAndEnter).toBe(false);
    });

    it("test resetAttributes reset nodes when svg empty area is clicked", function () {
        var setColor, node = {
            getAttributeNS: function () {
                return "black";
            },
            setAttributeNS: function (ns, attribute, newValue) {
                setColor = newValue;
            }
        };
        svgEventHandler.resetableSetAttribute(node, "color", "red");
        expect(setColor).toBe("red");

        svgEventHandler.resetAttributes();
        expect(setColor).toBe("black");

    });

    it("test getConnectivityUid method fetchs IDs from source properties for Rendered SVG", function () {
        var connecivityId = svgEventHandler.getConnectivityId(
                "chs.cof.logical.schem.PinList UID44bbc6-1631aacf6fb-14b576cc06b9c637217db3b9cfc0fc5b UID40c928-1631aacfc9d-14b576cc06b9c637217db3b9cfc0fc5b UID40c928-1631aacfca0-14b576cc06b9c637217db3b9cfc0fc5b sourceDesignUID:UID0d3c33-162f60afa37-14b576cc06b9c637217db3b9cfc0fc5b sourceObjectUID:UID0d3c33-162f60afa8a-14b576cc06b9c637217db3b9cfc0fc5b");
        expect(connecivityId).toBe("UID0d3c33-162f60afa8a-14b576cc06b9c637217db3b9cfc0fc5b");
    });

    it("test handleHighlightEvents should initialze object highlighting across all windows", function () {
        "use strict";
        var wireColor, origHighlightObject, origOpenPopup, origHighlightOnMouseClick, event={
            pageX: 1,
            pageY: 1,
            target: {
                tagName: 'svg'
            }
        };

        svgEventHandler.root = $("svg").first()[0];
        origHighlightObject = svgEventHandler.highlightObject;
        origOpenPopup = svgEventHandler.openPopup;
        origHighlightOnMouseClick = svgEventHandler.highlightOnMouseClick;

        mentor.publisher.selectedSystem.set=function () {};
        svgEventHandler.openPopup= function () {};
        svgEventHandler.highlightOnMouseClick=function () {};
        svgEventHandler.highlightObject = function (connectivityUID, color, isRenderedSVG) {
            wireColor = color;
        };

        spyOn(svgEventHandler, 'highlightOnMouseClick').andCallThrough();
        svgEventHandler.handleHighlightEvents(event, "click", "className, schemUID connectivityUID");
        expect(svgEventHandler.highlightOnMouseClick).toHaveBeenCalled();

        spyOn(svgEventHandler, 'openPopup').andCallThrough();
        svgEventHandler.handleHighlightEvents(event, "dblclick", "className, schemUID connectivityUID");
        expect(svgEventHandler.openPopup).toHaveBeenCalled();

        svgEventHandler.highlightObject = origHighlightObject;
        svgEventHandler.openPopup = origOpenPopup;
        svgEventHandler.highlightOnMouseClick = origHighlightOnMouseClick;
    });

    it("test dohighlight method sould be able to search elements in SVG with UID", function () {
        var svgGElemenet;
        svgEventHandler.root = $("svg").first()[0];
        var origSetHighLightMap = svgEventHandler.setHighLightMap;
        var setHighlightCalled = false;
        svgEventHandler.setHighLightMap = function (highObjsArray) {
            setHighlightCalled = true;
            expect(highObjsArray.length).toBe(1);
        }
        svgEventHandler.setHighLightMap = origSetHighLightMap;
    });

    it("test highlightObject should be able to higlight an elemnt in SVG", function () {
        var svgGElemenet, matrix, highlightedObj;
        svgEventHandler.root = $("svg").first()[0];
        var origHighlightUid = svgEventHandler.highlightUid;
        var uidhighlighted = false;
        svgEventHandler.highlightUid = function (uid, color) {

            uidhighlighted = true;
            expect(uid).toBe("UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e");
            expect(color).toBe("red");

        }
        svgEventHandler.getSVGContainer = function () {
            return $('body')[0];
        };

        svgEventHandler.doHighlighting = function (uid, color) {
            return highlightedObj = uid;
        };

        window.crossHighlightHandler.handlerHighlightedElementsMap.push({
            handler: svgEventHandler, highlightedElementArray: []
        });
        svgEventHandler.highlightObject("UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e", "red", false);
        highlightedObj = "";
        svgEventHandler.highlightUids(["UID25b55b-1316adac60b-9e788023cd93580da11edf9eaf55278e"], "red");
        svgEventHandler.highlightUid = origHighlightUid;
    });

    it("test handleHighlightEvents on rendered SVG should initialze object highlighting across all windows", function () {
        "use strict";
        var wireColor, origGetFitScale, origHighlightObject, origHighlightObject, origOpenPopup, origHighlightOnMouseClick, event={
            pageX: 1,
            pageY: 1,
            target: {
                tagName: 'svg'
            }
        };
        svgEventHandler.root = $("svg").first()[0];
        svgEventHandler.getGElementToFocus = function () {
            return $('g')[0];
        };
        origGetFitScale = svgEventHandler.getFitScale;
        origHighlightObject = svgEventHandler.highlightObject;
        origOpenPopup = svgEventHandler.openPopup;
        origHighlightOnMouseClick = svgEventHandler.highlightOnMouseClick;

        mentor.publisher.selectedSystem.set=function () {};
        svgEventHandler.openPopup= function () {};
        svgEventHandler.highlightOnMouseClick=function () {};

        svgEventHandler.getFitScale = function () {
            return {};
        };

        svgEventHandler.highlightObject = function (connectivityUID, color, isRenderedSVG) {
            wireColor = color;

        };

        spyOn(svgEventHandler, 'highlightOnMouseClick').andCallThrough();
        svgEventHandler.handleHighlightEvents(event, "click",
                "className, schemUID connectivityUID sourceDesignUID:designID sourceObjectUID:sourceObjectUID");
        expect(svgEventHandler.highlightOnMouseClick).toHaveBeenCalled();

        svgEventHandler.highlightObject = origHighlightObject;
        svgEventHandler.getFitScale = origGetFitScale;
        svgEventHandler.openPopup = origOpenPopup;
        svgEventHandler.highlightOnMouseClick = origHighlightOnMouseClick;
    });

    it("test if description tag exists", function () {
        expect(svgEventHandler.hasDescriptionTag('Desc')).toBeTruthy();
        expect(svgEventHandler.hasDescriptionTag('sampleTag')).toBeFalsy();
    });

    it("test if it is a valid element", function () {
        var evt = {
            target: {
                tagName: 'desc',
                parentNode: {
                    firstChild: 'desc'
                }
            },
        };
        expect(svgEventHandler.isValidElement(evt)).toBeFalsy();
    });

    it("test reset object highlighting", function () {
        var origSet=mentor.publisher.selectedSystem.set;
        mentor.publisher.selectedSystem.set=function () {};

        spyOn(window.crossHighlightHandler, 'flushZoomedViews').andCallThrough();
        svgEventHandler.resetObjectHighlighting();
        expect(window.crossHighlightHandler.flushZoomedViews).toHaveBeenCalled();

        mentor.publisher.selectedSystem.set=origSet;
    });

    it("test highlightWholeSignalInRenderedSVG", function () {
        var origGetSignalDataForHighlightInRenderedSVG=mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG;
        mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG=function (signalName, callback) {
            callback({signalName: signalName, textValue: 'success', dataArray: {objArray: []}});
        };
        svgEventHandler.signalDataArray = [];

        spyOn(mentor.publisher.dataLoader, 'getSignalDataForHighlightInRenderedSVG').andCallThrough();
        svgEventHandler.highlightWholeSignalInRenderedSVG('testSignalName', 'testColor');
        expect(mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG).toHaveBeenCalled();

        mentor.publisher.dataLoader.getSignalDataForHighlightInRenderedSVG=origGetSignalDataForHighlightInRenderedSVG;
    });

    it("test convertData", function () {
        var data = {name: 'testName', value: 'testValue'};
        expect(svgEventHandler.convertData(data)).toEqual({name : 'testName', value : 'testValue' });
    });

    it("test showToolTipOnMouseover", function () {
        var objData = {
            getAttr: function () {
                return 'testAttr';
            }
        }, evt = {
            pageX: 1,
            pageY: 1,
        };
        spyOn(mentor.publisher.toolTip, 'showToolTipForName').andCallThrough();
        svgEventHandler.showToolTipOnMouseover(objData, evt);
        expect(mentor.publisher.toolTip.showToolTipForName).toHaveBeenCalled();
    });

    it("test removeToolTipOnMouseleave", function () {
        var data = 'testData';
        spyOn(mentor.publisher.toolTip, 'removeToolTip').andCallThrough();
        svgEventHandler.removeToolTipOnMouseleave(data);
        expect(mentor.publisher.toolTip.removeToolTip).toHaveBeenCalled();
    });

    it("test highlightWholeSignal", function () {
        var origGetSignalObjects=mentor.publisher.dataLoader.getSignalObjects;
        mentor.publisher.dataLoader.getSignalObjects=function (signalName, systemID) {};

        spyOn(mentor.publisher.dataLoader, 'getSignalObjects').andCallThrough();
        svgEventHandler.highlightWholeSignal('testSignalName', 'testColor', 'testSystemID');
        expect(mentor.publisher.dataLoader.getSignalObjects).toHaveBeenCalled();

        mentor.publisher.dataLoader.getSignalObjects=origGetSignalObjects;
    });

    it("test openPopup", function () {
        spyOn(mentor.publisher.eventDispatcher, 'dispatchEvent');
        svgEventHandler.openPopup(0, 0, 'test description', true);
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("test highlightOnMouseClick", function () {
        spyOn(svgEventHandler, 'resetAttributesAndStackForEmptyClick').andCallThrough();
        svgEventHandler.highlightOnMouseClick('test description');
        expect(svgEventHandler.resetAttributesAndStackForEmptyClick).toHaveBeenCalled();
    });

    it("test doHighlighting", function () {
        svgEventHandler.root = $("svg").first()[0];
        expect(svgEventHandler.doHighlighting('testUID', 'testColor')).toEqual([]);
    });

    it("test setNewStyle", function () {
        var origGetComputedStyle=window.getComputedStyle;
        var origResetableSetAttribute=svgEventHandler.resetableSetAttribute;
        svgEventHandler.resetableSetAttribute=function () {};
        window.getComputedStyle=function () {return {getPropertyValue: function (props) {return '100px'}};}

        svgEventHandler.setNewStyle($('svg'), 'testColor', true, 'testUID');
        expect($('svg').attr('style')).toEqual("cursor: default;");

        window.getComputedStyle=origGetComputedStyle;
        svgEventHandler.resetableSetAttribute=origResetableSetAttribute;
    });

    it("test setHighLightMap", function () {
        svgEventHandler.svgTransformModel=new (Backbone.Model.extend())();
        spyOn(mentor.publisher.eventDispatcher, 'dispatchEvent');
        svgEventHandler.setHighLightMap([], true);
        expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("test longPressHandler", function () {
        var evt = {};
        var origIsValidElement = svgEventHandler.isValidElement;
        var origMouseHoverHighlight = svgEventHandler.mouseHoverHighlight;
        svgEventHandler.isValidElement = function (evt) {
            return true;
        };
        svgEventHandler.mouseHoverHighlight = function (descValue, evt) {};

        spyOn(svgEventHandler, 'isValidElement').andCallThrough();
        svgEventHandler.longPressHandler(evt);
        expect(svgEventHandler.isValidElement).toHaveBeenCalled();

        svgEventHandler.isValidElement = origIsValidElement;
        svgEventHandler.mouseHoverHighlight = origMouseHoverHighlight;
    });

    it("test pressed", function () {
        var origSet=mentor.publisher.selectedSystem.set;
        mentor.publisher.selectedSystem.set=function () {};
        var evt = {
            target: {
                nodeName: 'testNode',
                tagName: 'desc',
                parentNode: {
                    firstChild: 'desc'
                }
            },
            type: 'testType',
            stopPropagation: function () {},
        };
        svgEventHandler.pressed(evt);

        mentor.publisher.selectedSystem.set=origSet;
    });

    it("test showHiddenGraphicsForElement", function () {
        var origShowHiddenGraphics=mentor.publisher.objectDataLoader.loadRefernceIdsIfAny
        spyOn(mentor.publisher.objectDataLoader, 'loadRefernceIdsIfAny');
        svgEventHandler.showHiddenGraphicsForElement('testConnUID');
        expect(mentor.publisher.objectDataLoader.loadRefernceIdsIfAny).toHaveBeenCalled();
        mentor.publisher.objectDataLoader.loadRefernceIdsIfAny=origShowHiddenGraphics;
    });

});