
require(["harnessLayoutBarHandler", "models/selectedSystem"],
        function (HLayoutBarHandler, selectedSystem)
    {
        var harnessLayoutBarHandler = new HLayoutBarHandler();
        harnessLayoutBarHandler.amdLoader = function (deps, callback) {
            callback(DummyPopup);
        }
        var popupdata, DummyPopup = Backbone.View.extend({
            render: function (data)
            {
                popupdata = data;
            }
        });

        describe("harnessLayoutBarHandlerTest", function ()
        {
            define("dummyView", function ()
            {
                return DummyPopup;
            });
            var orgGetPopoverView=harnessLayoutBarHandler.getPopoverView;
            beforeEach(function ()
            {
                popupdata = false;
                harnessLayoutBarHandler.getPopoverView = function ()
                {

                    return "dummyView";
                };
                selectedSystem.set(mentor.publisher.contentType.HARNESS_LAYOUT_REPORT, {
                    dataName: "reports"
                }, {silent: true});
                selectedSystem.set(mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM, {
                    dataName: "diagrams"
                }, {silent: true});
            });
            it("should load succcessfully", function ()
            {
                expect(harnessLayoutBarHandler).toBeDefined();

            });
            it("should show reports popover when report btn is clicked", function ()
            {
                harnessLayoutBarHandler.onReportsButtonClick({
                    clienX: "text_x",
                    clienY: "text_y"
                });
                expect(popupdata.getDocumentGroup()).toBe("reports");
                expect(popupdata.getDocumentType()).toBe(mentor.publisher.contentType.HARNESS_LAYOUT_REPORT);
                expect(popupdata.popoverTitle).toBe("ReportsPopoverViewTitle");
                expect(popupdata.getActiveDocumentForDocumentSet().dataName).toBe("reports");
            });
            it("should show diagrams popover when diagram btn is clicked", function ()
            {
                harnessLayoutBarHandler.onDiagramsButtonClick({
                    clienX: "text_x",
                    clienY: "text_y"
                });
                expect(popupdata.getDocumentGroup()).toBe("diagrams");
                expect(popupdata.getDocumentType()).toBe(mentor.publisher.contentType.HARNESS_LAYOUT_DIAGRAM);
                expect(popupdata.getActiveDocumentForDocumentSet().dataName).toBe("diagrams");
            });
            it("should set content type", function ()
            {
                harnessLayoutBarHandler.setContentType("TestType");
                expect(harnessLayoutBarHandler.contentType).toBe("TestType");
            });
            it("should set data ID", function ()
            {
                harnessLayoutBarHandler.setDataId("TestID");
                expect(harnessLayoutBarHandler.dataId).toBe("TestID");
            });
            it("should getPopoverView", function ()
            {
                harnessLayoutBarHandler.getPopoverView=orgGetPopoverView;
                expect(harnessLayoutBarHandler.getPopoverView()).toBe("views/p/hld/HarnessLayoutDiagramsPopover");
            });
            it("should set options", function ()
            {
                var event = {
                    clientX: 'testX',
                    clientY: 'testY'
                };
                var result=harnessLayoutBarHandler.getOptions({}, event);
                var expectedResult= {
                    "preferredX": "testX",
                    "preferredY": "testY"
                };
                expect(JSON.stringify((result))).toBe(JSON.stringify(expectedResult));
            });
        });

    }, function (err)
    {
        describe("harnessLayoutBarHandlerTestFailed", function ()
        {
            it("harnessLayoutBarHandlerTest Failed", function ()
            {
                expect(err).toBeUndefined();

            });
        });
    });
