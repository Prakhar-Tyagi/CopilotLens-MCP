/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("popupTemplateTest", function ()
{

    it("Should be able to render template correctly", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<style type="text/css">	#detail {		top: 0;		left: 0;		width: 100%;		overflow: hidden;	}</style><div id="applicationArea" class="iesdApplication auto-application-area">	<div id="detail" class="styled auto-detail">	</div></div><span class="tooltip auto-tooltip"></span><div id="modal-container"></div></div>';

        $.ajax("/base/s/templates/popupTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html)();
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

