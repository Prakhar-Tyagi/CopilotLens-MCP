/**
 * Created by mukumar on 09-02-2017.
 */
require(["views/designobjects/designObjectsView"], function (desObjFactory)
{
    describe("designObjectsView module tests", function ()
    {
        var objUnderTest, viewData;
        beforeEach(function ()
        {
            objUnderTest = desObjFactory(viewData);
        });
        it("should be able to load", function ()
        {
            expect(desObjFactory).toBeDefined();
        });
        function mockGetDomElement(itemName, result)
        {
            var event = {
                target: "dummyTarger"
            };
            objUnderTest.getDomElement = function (elementSel)
            {
                expect(elementSel).toBe(event.target);
                return {
                    hasClass: function (className)
                    {
                        if (className === itemName) {
                            return result;
                        }

                    }
                }
            };
            return event;
        }

        it("isValidEvent should return false when 'next' is clicked", function ()
        {
            var event = mockGetDomElement("next", true);
            var isVal = objUnderTest.isValidEvent(event);
            expect(isVal).toBe(false);
        });

        it("getDataIdOfClickedElement should return id of current element", function ()
        {
            var event = {
                target: "tEle",
                currentTarget: "currentTarget"
            };
            objUnderTest.getDomElement = function (sel)
            {
                expect(sel).toBe(event.currentTarget);
                return {
                    attr: function (attr)
                    {
                        expect(attr).toBe('data-id');
                        return "testDataId";
                    }
                }
            };
            var id = objUnderTest.getDataIdOfClickedElement(event);
            expect(id).toBe("testDataId");
        });

        it("isValidEvent should return false when 'previous' is clicked", function ()
        {
            var event = mockGetDomElement("previous", true);
            var isVal = objUnderTest.isValidEvent(event);
            expect(isVal).toBe(false);
        });

        it("isValidEvent should return true when 'previous' or 'next' is not clicked", function ()
        {
            var event = mockGetDomElement("object", false);
            var isVal = objUnderTest.isValidEvent(event);
            expect(isVal).toBe(true);
        });
        it("getSelectecObject should be able to return clicked object", function ()
        {
            objUnderTest.getData = function ()
            {
                return {
                    get: function (id)
                    {
                        expect(id).toBe("obj1");
                        return {objectId: "obj1"};
                    }
                }
            };
            var selectedObj = objUnderTest.getSelectecObject("obj1");
            expect(selectedObj.objectId).toBe('obj1');
        });

        it("should be an instance of paginationView", function ()
        {
            expect(typeof  objUnderTest.showNextPage).toBe('function');
            expect(typeof  objUnderTest.showPreviousPage).toBe('function');
            expect(typeof  objUnderTest.paginate).toBe('function');
        });

        it("should be able to respond context changes", function ()
        {
            expect(typeof  objUnderTest.optionFilterApplied).toBe('function');
            expect(typeof  objUnderTest.packageChanged).toBe('function');
            expect(typeof  objUnderTest.languageChanged).toBe('function');
            expect(typeof  objUnderTest.searchTextApplied).toBe('function');
            expect(typeof  objUnderTest.resetAndReRenderView).toBe('function');
        });
        it("isViewDataAvailable should return true", function ()
        {
            expect(objUnderTest.isViewDataAvailable()).toBe(true);
        });

        function createTestEvent()
        {
            return {
                currentTarget: "currentTarget",
                target: "target",
                clientX: 5,
                clientY: 6,
                stopPropagation: function ()
                {
                    this.eventPropagationStopped = true;
                }
            };
        }

        function getDataIfOfClickedElement(evt)
        {
            objUnderTest.getDataIdOfClickedElement = function (event)
            {
                expect(event).toBe(evt);
                return "testId";
            }
        }

        it("should be able to show object popover when an item is clicked", function ()
        {
            var evt = createTestEvent(), popovershown, objectHighlighted;
            objUnderTest.isValidEvent = function (event)
            {
                expect(event).toBe(evt);
                return true;
            };
            getDataIfOfClickedElement(evt);
            objUnderTest.showObjectPopoverForObject = function (id, coordinates)
            {
                expect(id).toBe("testId")
                expect(coordinates.x).toBe(5)
                expect(coordinates.y).toBe(6)
                popovershown = true;
            };
            objUnderTest.highlightObject = function (id)
            {
                expect(id).toBe("testId")

                objectHighlighted = true;
            };
            objUnderTest.clicked(evt);
            expect(popovershown).toBeTruthy();
            expect(objectHighlighted).toBeTruthy();
            expect(evt.eventPropagationStopped).toBeTruthy();

        });

        it("highlightObject should be able to generate correct event to highlight object", function ()
        {
            var objHighlighted;
            var isWaiting = true;
            var config = {
                eventName: "highlightObject",
                eventDispatcher: {
                    eventDispatcher: {
                        dispatchEvent: function (eventName, payload)
                        {
                            expect(eventName).toBe(config.eventName);
                            expect(payload.objectId).toBe("testId")
                            objHighlighted = true;
                        }
                    }
                }
            };
            runs(function() {
                objUnderTest.highlightObject("testId", config);
                setTimeout(function() {
                    isWaiting = false;
                }, 100);
            });

            waitsFor(function() {
                return !isWaiting;
            }, 2000);

            runs(function() {
                expect(objHighlighted).toBe(true);
            });
        });

        it("should be not show object popover when an invalid item is clicked", function ()
        {
            var evt = createTestEvent(), popovershown;
            objUnderTest.isValidEvent = function (event)
            {
                expect(event).toBe(evt);
                return false;
            };
            getDataIfOfClickedElement(evt);
            objUnderTest.showObjectPopoverForObject = function (id, coordinates)
            {
                expect(id).toBe("testId")
                expect(coordinates.x).toBe(5)
                expect(coordinates.y).toBe(6)
                popovershown = true;
            };
            objUnderTest.clicked(evt);
            expect(popovershown).toBeUndefined();
            expect(evt.eventPropagationStopped).toBeTruthy();

        });
        it("showObjectPopoverForObject should save object highlight in history and show popover", function ()
        {
            var saved, popovershown, objId = "obj1", coor = {x: 1, y: 2};
            objUnderTest.saveCurrentObjectSelectionInHistory = function (id)
            {
                expect(id).toBe(objId);
                saved = true;
            };
            objUnderTest.showObjectPopover = function (id, coordinates)
            {
                expect(id).toBe(objId);
                expect(coordinates).toBe(coor);
                popovershown = true;
            };
            objUnderTest.showObjectPopoverForObject(objId, coor);
            expect(saved).toBe(true);
            expect(popovershown).toBe(true);
        });
        it("saveCurrentObjectSelectionInHistory should save object in browser history", function ()
        {
            var id = "testId", config = {
                moduleLoader: function (modules, callback)
                {
                    expect(modules[0]).toBe('routers/multipleDocumentRouter');
                    callback(documentObjectStorage);
                }
            }, documentObjectStorage = {
                save: function (flag, id)
                {
                    this.flag = flag;
                    this.id = id;
                }
            };
            objUnderTest.saveCurrentObjectSelectionInHistory(id, config);
            expect(documentObjectStorage.id).toBe('testId');
            expect(documentObjectStorage.flag).toBe(true);
        });
        it("showObjectPopover should generate an event to show object popover", function ()
        {
            var id = "obj1";
            var coor = {x: 1, y: 2};
            var config = {
                eventName: mentor.publisher.events.OPEN_OBJECT_POPUP,
                eventDispatcher: {
                    dispatchEvent: function (eventName, payload)
                    {
                        this.eventName = eventName;
                        expect(payload.systemId).toBe("testSystemId");
                        expect(payload.id).toBe("obj1");
                        expect(payload.x).toBe(1);
                        expect(payload.y).toBe(2);
                    }
                }
            };
            objUnderTest.getSelectecObject = function ()
            {
                return {
                    get: function (id)
                    {
                        expect(id).toBe("systemId");
                        return "testSystemId";
                    }
                }
            };
            objUnderTest.showObjectPopover(id, coor, config);
            expect(config.eventName).toBe(mentor.publisher.events.OPEN_OBJECT_POPUP);

        });
    });
}, function ()
{
    describe("designObjectsView module tests", function ()
    {
        it("failed to load", function ()
        {
            expect(true).toBeFalsy();
        });
    });
});
