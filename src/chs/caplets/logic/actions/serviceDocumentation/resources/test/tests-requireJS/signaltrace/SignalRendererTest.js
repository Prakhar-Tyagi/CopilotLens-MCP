/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("SignalRendererTest", function ()
{
    it("should create non cacheable regenerate URL", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        expect(signalRenderer.getRegenerateURL().indexOf("render?regenerate=true&rand=")).toBe(0);
        expect($.isNumeric(signalRenderer.getRegenerateURL().replace("render?regenerate=true&rand=", ""))).toBeTruthy();
    });

    it("should signal file loaded", async function() {
        var signalRenderer = new WebBasedSignalRenderer();
        spyOn(signalRenderer, "renderSVG");
        await signalRenderer.signalFileLoaded('filePath', 'connID', true, 'designID', {hookupConnectOntoMulticore: ''});
        expect(signalRenderer.renderSVG).toHaveBeenCalled();
    });

    it("should destroy session", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        spyOn($, 'ajax');
        signalRenderer.destroySession();
        expect($.ajax).toHaveBeenCalled();
    });

    it("should be able to delete rendered file", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        spyOn($, 'ajax');
        signalRenderer.deleteRenderedFile();
        expect($.ajax).toHaveBeenCalled();
    });

    it("should flush rendered data", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        signalRenderer.flushRenderedData();
        expect(signalRenderer.flush).toBeTruthy();
    });

    it("should check the prototype", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        expect(signalRenderer.doCheck()).toBeTruthy();
    });

    it("should regenerate SVG", function() {
        var signalRenderer = new WebBasedSignalRenderer();
        spyOn(signalRenderer, "getRegenerateURL").andCallThrough();
        signalRenderer.regenerateSVG();
        expect(signalRenderer.getRegenerateURL).toHaveBeenCalled();
    });

    it("should test signalRenderer methods", function() {
        //The following functions are empty, hence no assertions are present
        var signalRenderer = new SignalRenderer();
        signalRenderer.doCheck();
        signalRenderer.signalFileLoaded();
        signalRenderer.regenerateSVG();
        signalRenderer.deleteRenderedFile();
        signalRenderer.flushRenderedData();
        signalRenderer.destroySession();
    });
});
