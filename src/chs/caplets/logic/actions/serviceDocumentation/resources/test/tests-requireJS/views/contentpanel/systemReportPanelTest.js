/**
 * Created with IntelliJ IDEA.
 * User: paresaan
 * Date: 5/16/13
 * Time: 9:51 PM
 * To change this template use File | Settings | File Templates.
 */

require(["views/contentpanel/systemReportPanel"], function (systemReportPanel) {

    describe("systemReportPanelTest", function () {
        var org_render, viewRendered, harnessRendered, org_globalRender_method;
        beforeEach(function () {
            viewRendered = false;
            var Model = Backbone.Model.extend({});
            systemReportPanel.currentPackage = new Model();
            systemReportPanel.selectedSystem = new Model();
            org_render = systemReportPanel.render;
            systemReportPanel.render = function () {
                viewRendered = true;
            };
            org_globalRender_method = systemReportPanel.renderGlobalReport;
            systemReportPanel.renderGlobalReport = function () {
                harnessRendered = true;
            };
            systemReportPanel.initialize();

        });

        it("should be able to load systemReport module", function () {
            expect(systemReportPanel).toBeDefined();
        });

        it("it should re render when report id is changed", function () {
            systemReportPanel.selectedSystem.set("reportId", "newId");
            expect(viewRendered).toBeTruthy();
        });

        it("it should re render when vin is changed", function () {
            systemReportPanel.currentPackage.set("vin", "newVin");
            expect(viewRendered).toBeTruthy();
        });

        it("it should re render when language is changed", function () {
            systemReportPanel.currentPackage.set("language", "fr");
            expect(viewRendered).toBeTruthy();
        });

        it("it should re render when option expression  is changed", function () {
            systemReportPanel.selectedSystem.set("optionExpression", "op1");
            expect(viewRendered).toBeTruthy();
        });

        it("it should be able to render harness report", function () {
            systemReportPanel.selectedSystem.set("harness", "h1");
            expect(harnessRendered).toBeTruthy();
        });

        it("it should set view in report's container", function ()
        {
            var htmlStr = '<div>report</div>';

            systemReportPanel.$el.html(htmlStr);
            expect(systemReportPanel.$el.html()).toBe(htmlStr);
        });

    });

}, function (err)
{
    describe("systemReportPanelTest failed", function ()
    {
        it("should be able to load module", function ()
        {
            expect(err).toBeUndefined();
        });

    });
});
