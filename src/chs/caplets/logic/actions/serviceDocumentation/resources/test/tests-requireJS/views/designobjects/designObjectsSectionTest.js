/**
 * Created by mukumar on 09-02-2017.
 */
require(["views/designobjects/designObjectsSection"], function (DesignObjectsSectionFactory)
{
    describe("designObjectsSection tests", function ()
    {
        it("should be able to load", function ()
        {
            expect(DesignObjectsSectionFactory).toBeDefined();
        });
        it("should be a viewFactory", function ()
        {
            expect(DesignObjectsSectionFactory.isViewFactory).toBe(true);
        });

        function createAndTestViewForObject(type, title, category)
        {
            var view = DesignObjectsSectionFactory(type);
            expect(view.title).toBe(title);
            expect(view.type).toBe(type);
            expect(view.getData().type).toBe(type);
            expect(view.getData().category).toBe(category);
        }

        it("should be a able to create view for wires", function ()
        {
            var type = "wires";
            var title = "WiresTitle";
            var category = "Wires";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for connectors", function ()
        {
            var type = "connectors";
            var title = "ConnectorTitle";
            var category = "connectors";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for devices", function ()
        {
            var type = "devices";
            var title = "DevicesTitle";
            var category = "devices";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for splices", function ()
        {
            var type = "splices";
            var title = "SplicesTitle";
            var category = "splices";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for multicores", function ()
        {
            var type = "multicores";
            var title = "MulticoresTitle";
            var category = "multicores";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for grounds", function ()
        {
            var type = "grounds";
            var title = "GroundsTitle";
            var category = "grounds";
            createAndTestViewForObject(type, title, category);
        });

        it("should be a able to create view for inlines", function ()
        {
            var type = "inlines";
            var title = "InlinesTitle";
            var category = "inlines";
            createAndTestViewForObject(type, title, category);
        });

    });
}, function ()
{
    describe("designObjectsSection tests", function ()
    {
        it("failed to load", function ()
        {
            expect(true).toBeFalsy();
        });

    });
});
