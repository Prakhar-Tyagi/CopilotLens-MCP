/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("configPopupBtnControlsTemplateTest", function () {
    var templateUnderTest;
    var compiledHtml;
    beforeEach(function() {
        $.ajax("/base/s/templates/p/c/configPopupBtnControls.html", {async: false}).done(function(template) {
            templateUnderTest = template;
            compiledHtml = _.template(templateUnderTest)({
                configFilterName: "Config Name Test"
            });
        });
    });

    it("Template is defined", function() {
        expect(compiledHtml).toBeDefined();
        expect(typeof compiledHtml).toBe('string');
    });

    it("Config Name text field has character limit of 255", function() {
        var maxlength = $(compiledHtml).find('input#config-filter-name').attr('maxlength');
        expect(maxlength).toBe('255');
    });

    it("autocomplete is off on Config Name text field", function() {
        var autocomplete = $(compiledHtml).find('input#config-filter-name').attr('autocomplete');
        expect(autocomplete).toBe('off');
    });

    it("Config Name is shown properly when config name contains special characters", function() {
        var html = _.template(templateUnderTest)({
            configFilterName: "Config Name Test <script>alert(1)</script>"
        });
        var configNameWithSpecialCharaters = $(html).find('input#config-filter-name').val();
        expect(configNameWithSpecialCharaters).toBe('Config Name Test <script>alert(1)</script>');
    });
});


