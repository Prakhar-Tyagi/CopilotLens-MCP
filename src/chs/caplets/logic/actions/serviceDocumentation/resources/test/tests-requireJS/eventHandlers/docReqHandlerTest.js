/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("docReqHandlerTest", function(){

    var p = mentor.publisher, contentToCheckIfSameSystemIsOpened, contentToDisplay, fakeNavPanel = _.extend({}, Backbone.Events);;
    beforeEach(function(){
        p.crossReferenceHandler.setContentArea({
            closeAllSplitPanelsIfNewSystemIsOpened:function(content) {
                contentToCheckIfSameSystemIsOpened = content;
            }
        });
        p.crossReferenceHandler.setDocumentDisplayHandler({
            display : function(content) {
                contentToDisplay = content;
            }
        })
        p.crossReferenceHandler.setNavPanelModel(fakeNavPanel)
    });
    it("should close all existing systems when new system is opened from system cross reference panel", function(){
        p.crossReferenceHandler.display({
            attributes : {
                systemId : "testSystemId",
                diagramId : "testDiagramId"
            }
        });
        expect(contentToCheckIfSameSystemIsOpened.systemId).toBe("testSystemId");
    });

    it("should navigate to selected system when cross ref is clicked from popover", function(){

        p.crossReferenceHandler.display({
            attributes : {
                systemId : "testSystemId",
                diagramId : "testDiagramId"
            }
        });
        expect(contentToDisplay.systemId).toBe("testSystemId");
    });

    it("should show selected system when a cross ref entery is clicked from popver", function(){
        var scrollToHighlightedObj = false;
        fakeNavPanel.on("scrollNavigationPanelToTheSelectedElement", function(){
            scrollToHighlightedObj = true;
        });
        p.crossReferenceHandler.display({
            attributes : {
                systemId : "testSystemId",
                diagramId : "testDiagramId"
            }
        });
        expect(scrollToHighlightedObj).toBeTruthy();
    });

    it("should hide presentation and open file buttons when pdf handler is attached", function(){
        var isWaiting = true;
        var doc$ = $("<div><div id='openFile'/></div>");
        runs(function() {
            p.pdfTextClickHandler.attachEventHandler(doc$, ".unknown");
            setTimeout(function() {
                isWaiting = false;
            }, 700);
        });

        waitsFor(function() {
            return !isWaiting;
        }, 2000);

        runs(function() {
            expect($('#openFile', doc$).css('display')).toBe('none');
        });
    });

    it("should not unset content type when url is created", function(){
        var attributes, url;
        attributes = {
            systemId : "testSystemId",
            diagramId : "testDiagramId",
            objectId: "testObjectId",
            type: mentor.publisher.contentType.OBJECT_CROSS_REF,
        };
        url = p.crossReferenceHandler.createURL({
            attributes: attributes,
        });
        expect(url).toBe("popout.html#/system/testSystemId/testDiagramId/id1/testObjectId");
        expect(attributes.type).toNotBe(undefined);
    });

    it("should fetch first cross reference", function(){

        var result = p.crossReferenceHandler.fetchFirstCrossReference({
            content : {
                systemId : "testSystemId",
                objectId : "testObjectId"
            }
        });
        expect(result).toBe("");
    });

    it("should be able to get the document request handler factor based on type", function () {
        expect(p.documentRequestHandlerFactory.get("OBJECT_CROSS_REF")).toEqual(p.crossReferenceHandler);
        expect(p.documentRequestHandlerFactory.get("oldDesignRevision")).toEqual(p.illustratorDesignReferenceHandler);
        expect(p.documentRequestHandlerFactory.get("design-object")).toEqual(p.objectPopoverDisplayHandler);
        expect(p.documentRequestHandlerFactory.get("pdf-textt")).toEqual(p.pdfTextClickHandler);
    });

    it("should be able to check if it is in open diagrams", function () {
        var origLoadObjectData = mentor.publisher.project.loadObjectData,
            designObject={
                getCrossReferences: function (opts) {
                    return {
                        listItems: ['listItem1', 'listItem2']
                    };
                }
            }
        ;
        mentor.publisher.project.loadObjectData = function () {
            return designObject;
        };

        expect(p.crossReferenceHandler.isInOpenDiagrams({})).toBeTruthy();

        mentor.publisher.project.loadObjectData = origLoadObjectData;
    });

    it("should be able to get first active system", function () {
        expect(p.crossReferenceHandler.firstActiveSystem(['activeSystem1', 'activeSystem2'])).toBe('activeSystem1');
    });

    it("should be able to create url", function () {
        var origFetchFirstCrossReference = p.crossReferenceHandler.fetchFirstCrossReference,
            origGetId = p.project.getId
        ;
        p.crossReferenceHandler.fetchFirstCrossReference=function () {
            return {
                systemId: 'testSystemId',
                objectId: 'testObjectId',
                diagramId: 'testDiagramId',
            }
        };
        p.project.getId = function () {return 'testProjectId'};

        expect(p.crossReferenceHandler.createURL({
            attributes:{
                systemId: 'systemId',
                objectId: 'objectId'
            }
        })).toEqual("popout.html#/system/testSystemId/testDiagramId/testProjectId/testObjectId");

        this.fetchFirstCrossReference = origFetchFirstCrossReference;
        p.project.getId = origGetId;
    });

    it("should be able to display the object Popover and load tool tip", function () {
        p.objectPopoverDisplayHandler.loadToolTip({}, {});
        p.objectPopoverDisplayHandler.display({}, {});
    });

    it("should be able to load object", function () {
        var origLoadObjectData = p.project.loadObjectData;
        ;
        p.project.loadObjectData = function () {
            return {
                designName: 'testDesignName',
                designId: 'testDesignId',
            }
        }

        expect(p.crossReferenceHandler.loadObject({
            objectId: 'testObjectId',
            systemId: 'testSystemId',
        })).toEqual({ designName : 'testDesignName', designId : 'testDesignId' });
        p.project.loadObjectData=origLoadObjectData;
    });

    it("should be able to load tool tip", function () {
        p.crossReferenceHandler.loadToolTip({id: "testId"});
    });

    it('should be able to concatenate adjacent text from same line', function () {
        var target = $("<div id='target'>Sample Text</div>");
        var evt = {
            target:target
        };
        $('body').append(target);
        $('body').append($('<div id="sibling">Sample Text Post</div>'));
        var origGetUIDsForName=p.pdfTextClickHandler.getUIDsForName;

        p.pdfTextClickHandler.getUIDsForName=function () {};

        spyOn(p.pdfTextClickHandler, 'getUIDsForName')
        p.pdfTextClickHandler.concatenateAdjacentTextFromSameLine(evt);
        expect(p.pdfTextClickHandler.getUIDsForName).toHaveBeenCalled();

        p.pdfTextClickHandler.getUIDsForName=origGetUIDsForName;
    });

    it('should be able to get matching right content', function () {
        var target = $("<div id='target'>Sample Text</div>");
        var evt = {
            target:target
        };
        $('body').append(target);
        $('body').append($('<div id="sibling">Sample Text Post</div>'));
        var origGetUIDsForName=p.pdfTextClickHandler.getUIDsForName;

        spyOn(p.pdfTextClickHandler, 'getUIDsForName')
        p.pdfTextClickHandler.getMatchingRightContent(evt);
        expect(p.pdfTextClickHandler.getUIDsForName).toHaveBeenCalled();

        p.pdfTextClickHandler.getUIDsForName=origGetUIDsForName;
    });

    it("should be able to show popover", function () {
        var isWaiting = true,
            content=[
                {
                    title: "testTitle",
                    relatedObjects: [
                        {
                            objectConnUID: 'testObjectConnUID1'
                        }
                    ]
                }
            ],
            origAddSection=p.designObjectPopover.addSection;
        runs(function() {
            p.designObjectPopover.addSection=function () {};
            p.pdfTextClickHandler.showPopover("showTitle", content, 0, 0, false);
            setTimeout(function() {
                isWaiting = false;
            }, 101);
        });

        waitsFor(function() {
            return !isWaiting;
        }, 101);

        runs(function() {
            p.designObjectPopover.addSection=origAddSection;
        });
    });

    it("should be able to show multiple matches", function () {
        p.pdfTextClickHandler.showMultipleMatches();
        var content=[
                {
                    name: "testName",
                    relatedDocuments: [
                        {
                            documentUID: "testDocumentUID"

                        },
                    ],
                    relatedObjects: [
                        {
                            objectConnUID: 'testObjectConnUID1',
                            svgPath: 'testPath\testSvgPath1.svg',
                            type: p.contentType.NEW_DESIGN_REVISION,
                            filter: function () {},
                        },
                        {
                            objectConnUID: 'testObjectConnUID1',
                            svgPath: 'testPath\testSvgPath1.svg',
                            type: p.contentType.OLD_DESIGN_REVISION,
                            filter: function () {},
                        },
                    ]
                }
            ],
            origShowPopover=p.pdfTextClickHandler.showPopover,
            origDispatchEvent=p.eventDispatcher.dispatchEvent
        ;
        p.pdfTextClickHandler.showPopover=function () {};
        p.eventDispatcher.dispatchEvent=function () {};

        p.pdfTextClickHandler.showMultipleMatches(content, 0, 0);

        p.pdfTextClickHandler.showPopover=origShowPopover;
        p.eventDispatcher.dispatchEvent=origDispatchEvent;
    });

    it('should be able to call appropriate functions on content click', function () {
        p.pdfTextClickHandler.onContentClick();
        var content1=[{
                relatedObjects: [{objectConnUID: 'testObjectConnUID1'}]
            }],
            content2=[{
                relatedObjects: [{objectConnUID: 'testObjectConnUID1'}, {objectConnUID: 'testObjectConnUID2'}]
            }]
            origHighlightMatches=p.pdfTextClickHandler.highlightMatches,
            origShowMultipleMatches=p.pdfTextClickHandler.showMultipleMatches
        ;
        p.pdfTextClickHandler.highlightMatches=function () {};
        p.pdfTextClickHandler.showMultipleMatches=function () {};
        spyOn(p.pdfTextClickHandler, 'displayObjectPopover');
        spyOn(p.pdfTextClickHandler, 'showMultipleMatches');

        p.pdfTextClickHandler.onContentClick(content1, 0, 0, false);
        p.pdfTextClickHandler.onContentClick(content1, 0, 0, true);
        expect(p.pdfTextClickHandler.displayObjectPopover).toHaveBeenCalled();

        p.pdfTextClickHandler.onContentClick(content2, 0, 0, true);
        expect(p.pdfTextClickHandler.showMultipleMatches).toHaveBeenCalled();

        p.pdfTextClickHandler.highlightMatches=origHighlightMatches;
        p.pdfTextClickHandler.showMultipleMatches=origShowMultipleMatches;
    });

    it("should be able to call appropriate functions on click", function () {
        var origGetUIDsForName = p.pdfTextClickHandler.getUIDsForName,
            origConcatenateAdjacentTextFromSameLine = p.pdfTextClickHandler.concatenateAdjacentTextFromSameLine,
            origGetMatchingRightContent = p.pdfTextClickHandler.getMatchingRightContent,
            origOnContentClick = p.pdfTextClickHandler.onContentClick;

        p.pdfTextClickHandler.getUIDsForName = function () {};
        p.pdfTextClickHandler.concatenateAdjacentTextFromSameLine = function () {};
        p.pdfTextClickHandler.getMatchingRightContent = function () {};
        p.pdfTextClickHandler.onContentClick = function () {};

        spyOn(p.pdfTextClickHandler, "getUIDsForName");
        spyOn(p.pdfTextClickHandler, "onContentClick");

        p.pdfTextClickHandler.onClick();

        expect(p.pdfTextClickHandler.getUIDsForName).toHaveBeenCalled();
        expect(p.pdfTextClickHandler.onContentClick).toHaveBeenCalled();

        p.pdfTextClickHandler.getUIDsForName = origGetUIDsForName;
        p.pdfTextClickHandler.concatenateAdjacentTextFromSameLine = origConcatenateAdjacentTextFromSameLine;
        p.pdfTextClickHandler.getMatchingRightContent = origGetMatchingRightContent;
        p.pdfTextClickHandler.onContentClick = origOnContentClick;

    });

    it("should be able to highlight matches", function () {
        p.pdfTextClickHandler.highlightMatches();

        var content = [{
                relatedObjects: [
                    {
                        objectConnUID: "testObjectConnUID"
                    }
                ]
            }];

        spyOn(p.eventDispatcher, "dispatchEvent");
        p.pdfTextClickHandler.highlightMatches(content);
        expect(p.eventDispatcher.dispatchEvent).toHaveBeenCalled();
    });

    it("should be able to zoom matches", function () {

        var isWaiting = true;
        var origLoadFile = p.xmlLoader.loadFile,
            origZoomViews= window.crossHighlightHandler.zoomViews
        ;
        p.xmlLoader.loadFile=function () {return {data:{autoZoomOnClick:true}};};
        window.crossHighlightHandler.zoomViews=function () {};
        runs(function() {
            p.pdfTextClickHandler.zoomMatches({});
            setTimeout(function() {
                isWaiting = false;
            }, 501);
        });

        waitsFor(function() {
            return !isWaiting;
        }, 501);

        p.xmlLoader.loadFile=origLoadFile;
        window.crossHighlightHandler.zoomViews=origZoomViews;
    });

    it("should be able to load tool tip in illustrator design reference handler", function () {
        p.illustratorDesignReferenceHandler.loadToolTip({});
    });

    it("should be able to create url in illustrator design reference handler", function () {
        expect(p.illustratorDesignReferenceHandler.createURL({
            attributes: {
                systemID: "testSystemID",
                objectConnUID: "testObjectConnUID",
                diagramUID: "testDiagramUID",
                type: "testType"

            }
        })).toBe('');
    });

    it("should be able to get bucket", function () {
        expect(mentor.publisher.nameToUIDMap.getBucket(101)).toBe(1);
    });

    it("should be able to replace invalid char", function () {
        expect(mentor.publisher.nameToUIDMap.replaceInvalidChar('sampleText;<>')).toBe('sampleText___');
    });

    it("should be able to get name to UID file path", function () {
        expect(mentor.publisher.nameToUIDMap.getNameToUIDFilePath('sampleText')).toBe('id1/object-names-to-uid-map/63/sampleText.json');
    });

    it("should be able to get UIDs", function () {
        expect(mentor.publisher.nameToUIDMap.getUIDsFor("sampleText")).toBe(null);
    });

});

