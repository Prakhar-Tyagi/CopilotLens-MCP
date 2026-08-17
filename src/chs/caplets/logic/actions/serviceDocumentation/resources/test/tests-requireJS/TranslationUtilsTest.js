/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["TranslationUtils"],
        function (TranslationUtils)
        {
            "use strict";

            describe("TranslationUtilsTest", function ()
            {

                var language,
                        dictionary,
                        oldLanguageDataLoader;

                beforeEach(function ()
                {
                    oldLanguageDataLoader = mentor.publisher.languageDataLoader;
                    mentor.publisher.languageDataLoader = {
                        getLanguageDictionary: function ()
                        {
                            return dictionary;
                        },

                        getCurrentLanguage: function ()
                        {
                            return language;
                        }
                    }
                });

                it("should translate a div with unmarked quick code", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>abc</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                    expect($(element).html()).toBe('alpha beta gamma');
                });

                it("should not translate a div with unmarked quick code", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>abc</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                    expect($(element).html()).toBe('abc');
                });

                it("should only translate text(without curly braces) within an element and should retain any html elements",
                        function ()
                        {
                            dictionary = {
                                translations: {
                                    abc: "alpha beta gamma"
                                }
                            };
                            language = "en";

                            var element = $(
                                    "<td id='target'><span class='clickable-column' id='object-UID'></span>abc</td>");

                            TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                            expect($(element).html()).toBe(
                                    '<span class="clickable-column" id="object-UID"></span>alpha beta gamma');
                        });

                it("should only translate text(with curly braces) within an element and should retain any html elements",
                        function ()
                        {
                            dictionary = {
                                translations: {
                                    abc: "alpha beta gamma"
                                }
                            };
                            language = "en";

                            var element = $(
                                    "<td id='target'><span class='clickable-column' id='object-UID'></span>{abc}</td>");

                            TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                            expect($(element).html()).toBe(
                                    '<span class="clickable-column" id="object-UID"></span>alpha beta gamma');
                        });

                it("should handle nested elements", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma",
                            efg: "detala epsilon zeta"
                        }
                    };
                    language = "en";

                    var element = $(
                            "<td id='target'><span class='clickable-column' id='object-UID'>{efg}</span>{abc}</td>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                    expect($(element).html()).toBe(
                            '<span class="clickable-column" id="object-UID">detala epsilon zeta</span>alpha beta gamma');
                });

                it("should not translated nested html elements when text is not marked for translation", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma",
                            efg: "detala epsilon zeta"
                        }
                    };
                    language = "en";

                    var element = $(
                            "<td id='target'><span class='clickable-column' id='object-UID'>efg</span>abc</td>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                    expect($(element).html()).toBe('<span class="clickable-column" id="object-UID">efg</span>abc');
                });

                it("should translate a div with marked quick code", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>{abc}</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                    expect($(element).html()).toBe('alpha beta gamma');
                });

                it("should translate a div with marked quick code when asked to mark them as such", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>{abc}</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                    expect($(element).html()).toBe('alpha beta gamma');
                });

                it("should translate a div with marked quick code and static text", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>{abc}, T20 & F50</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                    expect($(element).text()).toBe('alpha beta gamma, T20 & F50');
                });

                it("should translate a div with marked quick code containing &", function ()
                {
                    dictionary = {
                        translations: {
                            "ab&c": "alpha beta & gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>{ab&c}</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element);

                    expect($(element).text()).toBe('alpha beta & gamma');
                });

                it("should translate a div with unmarked quick code with no translation", function ()
                {
                    dictionary = {
                        translations: {
                            "ab&c": "alpha beta & gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'>abc</div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                    expect($(element).text()).toBe('abc');
                });

                it("should translate header of a report even if quick code contains html mark up", function ()
                {
                    dictionary = {
                        translations: {
                            abc: "alpha beta gamma"
                        }
                    };
                    language = "en";

                    var element = $("<div id='target'><b>abc</b></div>");

                    TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                    expect($(element).text()).toBe('alpha beta gamma');
                });

                it("should not translate or change the header if translation code is not avaialble for it",
                        function ()
                        {
                            dictionary = {
                                translations: {
                                    abc: "alpha beta gamma"
                                }
                            };
                            language = "en";

                            var element = $("<div id='target'><b>abc1</b></div>");

                            TranslationUtils.replaceLanguageCodeWithTranslatedText(element, true);

                            expect($(element).text()).toBe('abc1');
                        });

                afterEach(function ()
                {
                    mentor.publisher.languageDataLoader = oldLanguageDataLoader;
                });

            });
        });