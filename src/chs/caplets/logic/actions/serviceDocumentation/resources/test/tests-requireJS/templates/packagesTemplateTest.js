/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("packagesTemplateTest", function ()
{

    it("Should be able to render template correctly", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<div class="components flex-parent">    <div class="flex-child flex-growable">        <div class="component-button auto-projects-button navigation_btn" title="Back" id="back_button">?        </div>        <div class="component-button auto-projects-button navigation_btn" title="Forward" id="forward_button">?        </div>        <img src="images/CapitalServiceExplorer.png" class="ApplicationLogoInHeader">        <span class="ApplicationNameInHeader">            Capital Service Explorer        </span>    </div>    <div class="flex-child flex-fixed">        <img src="images/home.png" srcset="images/home@2x.png 2x,         images/home@3x.png 3x" class="home-1 auto-projects-button" id="project_button">        <img src="images/export-excel-small.png" class="save-button auto-save-button" style="display: none">    </div></div>';
        $.ajax("/base/s/templates/packagesTemplate.html", {async: false}).done(
                function (html) {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html)({range: null});
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "")).toEqual(goldenExpectedCompiledTemplate);
                }).fail(function () {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

