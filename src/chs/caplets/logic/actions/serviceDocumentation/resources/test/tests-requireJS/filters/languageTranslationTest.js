/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 16/10/12
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
/*global assertEquals, mentor, DOMParser, applyLanguageFilter, languageDictionary*/
describe("languageTranslationTest", function() {
    /*testProject : '',*/
    beforeEach(function() {
        "use strict";
        var domParser;
        domParser = new DOMParser();
        /*this.prevproject = mentor.publisher.project;*/
        var testProject = {
            getId : function () {
                return "someId";
            },
            getSystems : function () {
                return [
                    {
                        getId : function () {
                            return "sysId2";
                        },
                        getOptionExpression : function () {
                            return "op1";
                        },
                        getReports : function () {
                            return [];
                        },
                        getDiagrams : function () {
                            return [];
                        },
                        mainText : "{CIRD}",
                        subText : "{shortDes}",
                        getShortDescription : function () {
                            return '{shortDes}';
                        },
                        getToolTips : function () {
                            return [
                                {
                                    getName : function () {
                                        return "name";
                                    },
                                    getValue : function () {
                                        return '{CIRD}';
                                    }
                                }
                            ];
                        }
                    },
                    {
                        getId : function () {
                            return "sysId1";
                        },
                        getOptionExpression : function () {
                            return "op2";
                        },
                        getReports : function () {
                            return [];
                        },
                        getDiagrams : function () {
                            return [];
                        },
                        getShortDescription : function () {
                            return '{shortDes}';
                        },
                        mainText : "{CIRD}"
                    }
                ];
            }

        };
        mentor.publisher.pathResolver = {
            getLanguageDictionaryFilePath : function () {
                return "path";
            },
            getGlobalLanguageDictionaryFilePath: function ()
            {
                return this.getLanguageDictionaryFilePath();
            }
        };
        mentor.publisher.LanguageFilteredProject.setProject(testProject);
        var xmlLoader = {loadGlobalFile : function () {
            return {data : domParser.parseFromString(languageDictionary, "application/xml"), ajaxStatus : "success"};
        }};

        mentor.publisher.languageDataLoader.setXMLLoader(xmlLoader);
        this.unalteredLanguageDataLoader = mentor.publisher.languageDataLoader;
        this.unalteredUrlParams = mentor.publisher.urlParams;

    });

    afterEach(function () {
        "use strict";
        mentor.publisher.languageDataLoader = this.unalteredLanguageDataLoader;
        mentor.publisher.urlParams = this.unalteredUrlParams;
        mentor.publisher.LanguageFilteredProject.setProject(mentor.publisher.project);
    });

    it("test systems get translated when new language is selected.", function() {
        "use strict";
        mentor.publisher.languageDataLoader.reset();
        var langDic = mentor.publisher.languageDataLoader.getLanguageDictionary(), systems;
        mentor.publisher.LanguageFilteredProject.setCurrentLanguage("DE");

        systems = mentor.publisher.LanguageFilteredProject.getSystems();
        // assertEquals("Systems did not get translated when new language is selected", systems[0].mainText, "german");
        // assertEquals("Systems did not get translated when new language is selected", systems[0].subText,
        //     "shortDesgerman");
        // assertEquals("Systems did not get translated when new language is selected", systems[0].getShortDescription(),
        //     "shortDesgerman");
        // assertEquals("Systems did not get translated when new language is selected",
        //     systems[0].getToolTips()[0].getValue(), "german");

        expect(systems[0].mainText).toBe("german");
        expect(systems[0].subText).toBe("shortDesgerman");
        expect(systems[0].getShortDescription()).toBe("shortDesgerman");
        expect(systems[0].getToolTips()[0].getValue()).toBe("german");
    });

    it("test get localized name strips the language code if current.", function() {
        "use strict";

        mentor.publisher.languageDataLoader = {
            getCurrentLanguage: function () { return "En"; }
        };

        // assertEquals("file_en is to localized to file", "file",
        //         mentor.publisher.LanguageFilteredProject.getLocalizedName("file_en"));
        expect(mentor.publisher.LanguageFilteredProject.getLocalizedName("file_en")).toBe("file");

        // assertEquals("file_de is to localized to file", "file_de",
        //         mentor.publisher.LanguageFilteredProject.getLocalizedName("file_de"));
        expect(mentor.publisher.LanguageFilteredProject.getLocalizedName("file_de")).toBe("file_de");
    });

    it("test filter information pages will filter non current localized files.", function() {
        "use strict";

        mentor.publisher.languageDataLoader = {
            getCurrentLanguage: function () { return "EN"; },
            getKnownLanguageCodes: function () { return ["EN", "de", "fr"]; }
        };
        mentor.publisher.urlParams.ignoreInfoTranslation = false;

        var source = _.map(["file_en", "file_de", "file_undocumented", "file_a_b", "1.pdf"], function (value) {
            return { mainText: value };
        });

        var filtered = mentor.publisher.LanguageFilteredProject.filterInformationPages(source);

        var result = _.map(filtered, function (value) {
            return value.mainText;
        });

        // assertEquals("information files are not filtered correctly.",
        //         ["file_en", "file_undocumented", "file_a_b", "1.pdf"], result);
        expect(result).toBE(["file_en", "file_undocumented", "file_a_b", "1.pdf"]);
    });
});
