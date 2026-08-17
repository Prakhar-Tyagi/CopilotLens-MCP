
require(["collections/harnessPopoverColl", "models/selectedSystem"],
    function (harnessColl, selectedSystem)
    {
        var Model = Backbone.Model.extend({});

        function createTestData()
        {
            var harness = {};
            harness.objectids = [];
            harness.objectids.push("id");
            harness.objectId = "id";
            harness.logicObjectId = "logic_id";
            harness.id = "id";
            harness.diagramId = "diagramId";
            harness.mainText = "diagramName";
            harness.path = "test.svg";
            return harness;
        }

        describe("harnessCollTest", function ()
        {
            beforeEach(function ()
            {
                window.diagramAsSystemsObjectFactoryImpl = false;
                harnessColl.getSelectedHarnessLayouts = function ()
                {
                    return {
                        get: function ()
                        {
                            return {
                                clone: function ()
                                {
                                    var har = new Model();
                                    har.set(createTestData());
                                    return har;
                                }
                            }
                        }
                    };
                }
                harnessColl.isDiagramAlreadyOpened = function(){
                    return true;
                };
            });
            it("should load successfully", function ()
            {
                expect(harnessColl).toBeDefined();

            });
            it("fetch harness diagram from design object data", function ()
            {
                var harnesses = harnessColl.fetch({
                    getHarnessLayouts: function ()
                    {
                        var harnesses = [];
                        var harness = createTestData();
                        harness.mainText = "systemName";
                        harnesses.push(harness);
                        return harnesses;
                    }
                })
                expect(harnesses.length).toBe(1);
                expect(harnesses[0].get("mainText")).toBe("diagramName:systemName");

            });
            it("diagramAsSystem flow should return harness layout", function () {
                diagramAsSystemsFlow = true;
                var harness = createTestData();
                var layout = harnessColl.setMainText(layout, harness);
                expect(layout !== null).toBe(true);
            });
            it("fetch harness diagram from design object data for diagrams as system flow", function ()
            {
                window.diagramAsSystemsObjectFactoryImpl = true;
                var harnesses = harnessColl.fetch({
                    getHarnessLayouts: function ()
                    {
                        var harnesses = [];
                        var harness = createTestData();
                        harnesses.push(harness);
                        return harnesses;
                    }
                })
                expect(harnesses.length).toBe(1);
                expect(harnesses[0].get("mainText")).toBe("diagramName");

            });
        });
    }, function (err)
    {
        describe("harnessCollTestFailed", function ()
        {
            it("harnessColl test Failed", function ()
            {
                expect(err).toBeUndefined();

            });
        });
    });
