(function(){
    "use strict";

    require(['views/navigationPanelView', "models/selectedSystem"], function (navPanelView, selectedSystem)
    {
        describe("navigationPanelViewTest", function (){
            beforeEach(function () {
                stubs.currentPackage.set("id", "projectId");
                selectedSystem.set("systemId", "testSystemId");

                navPanelView.$el.append("<ol class=\"tree\">\n" +
                        "    <li>\n" +
                        "        <label title=\"0-HIGH-LEVEL\" for=\"0-HIGH-LEVEL\">0-HIGH-LEVEL</label>\n" +
                        "        <input type=\"checkbox\" id=\"0-HIGH-LEVEL\">\n" +
                        "        <ol>\n" +
                        "            <li>\n" +
                        "                <label title=\"0.1 - System Interactive High-level Process\" for=\"0.1 - System Interactive High-level Process\">0.1 - System Interactive High-level Process</label>\n" +
                        "                <input type=\"checkbox\" id=\"0.1 - System Interactive High-level Process\">\n" +
                        "                <ol>\n" +
                        "                    <li data-id=\"testSystemId\" base-id=\"testSystemId\" class=\"file listItem\"><span>0.1 - System Interactive High-level Process</span><span class=\"popUp\"></span></li>\n" +
                        "                </ol>\n" +
                        "            </li>\n" +
                        "            <li>\n" +
                        "                <label title=\"0.2 - Platform Interactive High-level Process\" for=\"0.2 - Platform Interactive High-level Process\">0.2 - Platform Interactive High-level Process</label>\n" +
                        "                <input type=\"checkbox\" id=\"0.2 - Platform Interactive High-level Process\">\n" +
                        "                <ol>\n" +
                        "                    <li data-id=\"UIDe61099-16a31487574-daa05d62f658526afb247f2833a04a69\" base-id=\"UIDe61099-16a31487574-daa05d62f658526afb247f2833a04a69\" class=\"file listItem\"><span>0.2 - Platform Interactive High-level Process</span><span class=\"popUp\"></span></li>\n" +
                        "                </ol>\n" +
                        "            </li>\n" +
                        "            <li>\n" +
                        "                <label title=\"0.3 - Automotive Generative High-level Process\" for=\"0.3 - Automotive Generative High-level Process\">0.3 - Automotive Generative High-level Process</label>\n" +
                        "                <input type=\"checkbox\" id=\"0.3 - Automotive Generative High-level Process\">\n" +
                        "                <ol>\n" +
                        "                    <li data-id=\"UIDe61099-16a31486b12-daa05d62f658526afb247f2833a04a69\" base-id=\"UIDe61099-16a31486b12-daa05d62f658526afb247f2833a04a69\" class=\"file listItem\"><span>0.3 - Automotive Generative High-level Process</span><span class=\"popUp\"></span></li>\n" +
                        "                </ol>\n" +
                        "            </li>\n" +
                        "            <li>\n" +
                        "                <label title=\"0.4 - First-Pass Generative High-level Process\" for=\"0.4 - First-Pass Generative High-level Process\">0.4 - First-Pass Generative High-level Process</label>\n" +
                        "                <input type=\"checkbox\" id=\"0.4 - First-Pass Generative High-level Process\">\n" +
                        "                <ol>\n" +
                        "                    <li data-id=\"UIDe61099-16a3148baae-daa05d62f658526afb247f2833a04a69\" base-id=\"UIDe61099-16a3148baae-daa05d62f658526afb247f2833a04a69\" class=\"file listItem\"><span>0.4 - First-Pass Generative High-level Process</span><span class=\"popUp\"></span></li>\n" +
                        "                </ol>\n" +
                        "            </li>\n" +
                        "            <li>\n" +
                        "                <label title=\"TEMPLATE Process\" for=\"TEMPLATE Process\">TEMPLATE Process</label>\n" +
                        "                <input type=\"checkbox\" id=\"TEMPLATE Process\">\n" +
                        "                <ol>\n" +
                        "                    <li data-id=\"UIDe61099-16a31488d30-daa05d62f658526afb247f2833a04a69\" base-id=\"UIDe61099-16a31488d30-daa05d62f658526afb247f2833a04a69\" class=\"file listItem\"><span>TEMPLATE Process</span><span class=\"popUp\"></span></li>\n" +
                        "                </ol>\n" +
                        "            </li>\n" +
                        "        </ol>\n" +
                        "    </li>\n" +
                        "</ol>");
            });

            afterEach(function () {

            })

            it("Panel with tree view should not call expand panel", function(){
                var flag = false;
                var expandFirstPanelCalled = false;
                var origExpandFirstPanel = navPanelView.expandFirstPanel;
                navPanelView.expandFirstPanel = function(){
                    // this function should not be called
                    var expandFirstPanelCalled = true;
                };

                runs(function(){
                    navPanelView.highlightElement();
                    setTimeout(function(){
                        flag = true;
                    }, 900)
                });

                waitsFor(function(){
                    return flag;
                }, 1000);

                runs(function(){
                    expect(expandFirstPanelCalled).toBeFalsy();
                });

                navPanelView.expandFirstPanel=origExpandFirstPanel;
            });

            it("should be able to dehighlight selected item", function () {
                navPanelView.dehighlightSelectedElement();
                navPanelView.dehighlightSelectedElement({id: "testId"});
            });

            it("should be able to expand first panel", function () {
                navPanelView.expandFirstPanel();
            });

            it("should be able to render", function () {
                var origGetNavigationPanelOrder = mentor.publisher.dataLoader.getNavigationPanelOrder,
                    origSetElement = navPanelView.setElement;
                    mentor.publisher.config.navHidden=true
                    ;
                mentor.publisher.dataLoader.getNavigationPanelOrder = function () {return [{title: "testTitle1"}]};
                navPanelView.setElement = function () {};

                spyOn(navPanelView, "setElement");
                navPanelView.render();
                expect(navPanelView.setElement).toHaveBeenCalled();

                mentor.publisher.dataLoader.getNavigationPanelOrder = origGetNavigationPanelOrder;
                navPanelView.setElement = origSetElement;
            });

            it("should be able to expand first panel", function () {
                navPanelView.model.set("visible", false);
                navPanelView.toggleVisibility();
                expect(navPanelView.model.get("visible")).toBeTruthy();

                navPanelView.model.set("visible", true);
                navPanelView.toggleVisibility();
                expect(navPanelView.model.get("visible")).toBeFalsy();
            });

            it("should be able to expand first panel", function () {
                navPanelView.onWindowResize();
                expect(navPanelView.model.get("width")).toBe("21%");
            });

        });
    }, function () {
        describe("navigationPanelViewTest loading failed", function(){
            it("should have loaded the required module", function(){
                expect(true).toBeFalsy();
            });
        });
    });
})();