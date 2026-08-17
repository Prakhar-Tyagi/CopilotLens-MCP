/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 16/10/12
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
/*global assertEquals, mentor, languageDictionary, DOMParser, TestCase*/
describe("languageDataLoaderTest", function(){
    "use strict";
    
    var mockLanguageDictionary;
    var originalAjax;
    
    beforeEach(function () {
        // Mock language dictionary
        mockLanguageDictionary = {
            orderedLangList: [
                { langKey: "EN", language: "English" },
                { langKey: "DE", language: "German" },
                { langKey: "FR", language: "French" }
            ],
            currentLangChoice: 0
        };
        
        // Mock jQuery.ajax
        originalAjax = jQuery.ajax;
        jQuery.ajax = function(options) {
            if (options.success) {
                options.success({
                    success: true,
                    data: mockLanguageDictionary
                });
            }
        };
        
        mentor.publisher.languageDataLoader.reset();
        mentor.publisher.languageDataLoader.resetDefaultLanguageChoice();
    });
    
    afterEach(function () {
        if (originalAjax) {
            jQuery.ajax = originalAjax;
        }
        mentor.publisher.languageDataLoader.reset();
    });
    
    it("should load language dictionary on first call", function () {
        var langList = mentor.publisher.languageDataLoader.getOrderedLangList();
        expect(langList.length).toBe(3);
        expect(langList[0].code).toBe("EN");
        expect(langList[0].language).toBe("English");
    });
    
    it("should return current language code", function () {
        var currentLang = mentor.publisher.languageDataLoader.getCurrentLanguage();
        expect(currentLang).toBe("EN");
    });
    
    it("should return current language choice index", function () {
        var langIndex = mentor.publisher.languageDataLoader.getCurLangChoice();
        expect(langIndex).toBe(0);
    });
    
    it("should return false when language hasn't changed", function () {
        var hasChanged = mentor.publisher.languageDataLoader.hasLanguageChanged("EN");
        expect(hasChanged).toBe(false);
    });
    
    it("should detect language change", function (done) {
        var initialLang = mentor.publisher.languageDataLoader.getCurrentLanguage();
        expect(initialLang).toBe("EN");
        
        mentor.publisher.languageDataLoader.setCurrentLanguageChoice(1);

        var hasChanged = mentor.publisher.languageDataLoader.hasLanguageChanged("EN");
        expect(hasChanged).toBe(true);
    });

    it("should return all known language codes", function () {
        var codes = mentor.publisher.languageDataLoader.getKnownLanguageCodes();
        expect(codes.length).toBe(3);
        expect(codes).toContain("EN");
        expect(codes).toContain("DE");
        expect(codes).toContain("FR");
    });
    
    it("should return complete language dictionary", function () {
        var dict = mentor.publisher.languageDataLoader.getLanguageDictionary();
        expect(dict).toBeDefined();
        expect(dict.orderedLangList.length).toBe(3);
    });
    
    it("should reset loader state", function () {
        mentor.publisher.languageDataLoader.reset();
        var dict = mentor.publisher.languageDataLoader.getLanguageDictionary();
        expect(dict).toBeDefined();
    });
});