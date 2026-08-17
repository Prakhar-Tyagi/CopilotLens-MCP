/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*
 This class is used to show tool tips
 */
/*global mentor, Utils, $, window*/
mentor.publisher.languageDataLoader = (function (p) {
    "use strict";
    var getLangString, getNewChoiceNumber, load, langDictionaryLoaded, isLoaded, getLangKeyFromDictionary,
        loadLangDictionary, dictionary, isDefaultLanguageLoaded = false, getViewerLanguage, langDictionaryFailedToLoad,
        fetchTranslations, shouldFetchTranslations, packageId = null;

    //private function
    getLangString = function (eLang, orderedLangList) {
        return orderedLangList[eLang].langKey + " : " + orderedLangList[eLang].language;
        //  orderedLangList[eLang].country;
        //  orderedLangList[eLang].nation;
    };
    //private function
    getNewChoiceNumber = function (choiceStr, orderedLangList) {
        var s = orderedLangList.length, eLang, itemstr;
        for (eLang = 0; eLang < s; eLang = eLang + 1) {
            itemstr = getLangString(eLang, orderedLangList);
            if (itemstr === choiceStr) {
                return eLang;
            }
        }
        return 0;
    };

    load = function () {
        if (isLoaded) {
            return;
        }

        var url = `unzipped/data/langDictionary`;
        var sessionContext = require('PackagesInSession');
        var packagesInSession = sessionContext.get("packages");

        if (packageId === null) {
            var packageInfo = getWindowObj().mentor.publisher.packectInfo;
            packageId = packageInfo !== undefined ? packageInfo.packageId : null;
        }

        if (packagesInSession.length === 1) {
            packageId = packagesInSession.at(0).get('id').replace("data/", "").replace("data\\", "");
        }

        if (packageId) {
            url += `?packageId=${packageId}`;
        }

        $.ajax({
            url: url,
            type: 'GET',
            async: false,
            dataType: 'json',
            success: function (response, textStatus, xhr) {
                langDictionaryLoaded(response, textStatus);
            },
            error: function (xhr, textStatus, errorThrown) {
                require(['views/component/ModalDialog'], function (ModalDialog) {
                    var errorMsg;
                    if (xhr.responseJSON === "INVALID_FORMAT") {
                        errorMsg = "LangDictionary.InvalidFormat";
                    } else if (xhr.responseJSON === "SERVER_MEMORY_INSUFFICIENT") {
                        errorMsg = "LangDictionary.ServerError";
                    } else {
                        return;
                    }
                    var translator = mentor.publisher.languageTranslator;
                    var modalDialog = new ModalDialog({
                        title: translator.localize("LangDictionary.ErrorTitle"),
                        message: translator.localize(errorMsg + ".ErrorMessage"),
                        implication: translator.localize("LangDictionary.ErrorImplication"),
                        guidance: translator.localize(errorMsg + ".ErrorGuidance"),
                        primaryButton: translator.localize("Close"),
                        dialogFlag: mentor.publisher.modalDialogFlag.ERROR,
                        onConfirmFn: function () {}.bind(this),
                        onCancelFn: function () {}.bind(this)
                    });
                    modalDialog.show();
                });
                langDictionaryFailedToLoad(null, textStatus);
            }
        });
    };

    langDictionaryLoaded = function (langDictionary, textStatus) {
        try {
            var viewerLanguage;
            if (textStatus === 'success' && langDictionary) {
                loadLangDictionary({
                    textValue: textStatus,
                    langDictionary: langDictionary
                });
                /**
                 * load default language based on user prefrences
                 */
                if (!isDefaultLanguageLoaded) {
                    viewerLanguage = getLangKeyFromDictionary(getViewerLanguage(), langDictionary);
                    isDefaultLanguageLoaded = true;
                    if (viewerLanguage !== -1) {
                        langDictionary.currentLangChoice = viewerLanguage;
                        var langKey = langDictionary.orderedLangList[viewerLanguage].langKey;
                        fetchTranslations(langKey);
                    }
                }
            } else {
                langDictionaryFailedToLoad(null, textStatus);
            }
        } catch (erc) {
            langDictionaryFailedToLoad(null, textStatus);
        }
    };

    getLangKeyFromDictionary = function (viwerLanguage, langDictionary) {
        var index, curLang;
        for (index = 0; index < langDictionary.orderedLangList.length; index = index + 1) {
            curLang = langDictionary.orderedLangList[index];
            if (curLang && curLang.langKey.toLowerCase() === viwerLanguage.toLowerCase()) {
                return index;
            }
        }
        return -1;
    };

    loadLangDictionary = function (aLangDictionaryDetails) {
        if (aLangDictionaryDetails.textValue === 'success') {
            dictionary = aLangDictionaryDetails.langDictionary;
        }
        isLoaded = true;
    };

    // This is a private method
    langDictionaryFailedToLoad = function (data, textStatus) {
        loadLangDictionary({textValue : textStatus, packageDataArray : data });
    };

    getViewerLanguage = function () {
        var languageCode, locationName;
        var preferences = require("preferences");
        if (Utils.notNull(Utils.getUrlVars().lang)) {
            languageCode = Utils.getUrlVars().lang;
        } else {
            locationName = window.navigator.userLanguage || window.navigator.language;
            languageCode = Utils.notNull(locationName) ? locationName.split('-')[0] : 'en';

        }
        if (window.opener && window.opener.mentor) {
            languageCode = window.opener.mentor.publisher.LanguageFilteredProject.getCurrentLanguage();
        } else if (preferences) {
            languageCode = preferences.get("language");
        }
        if (!languageCode) {
            languageCode = 'en';
        }
        return languageCode;
    };

    fetchTranslations = function (langKey) {
        var url = `unzipped/data/translations?language=${langKey}`;
        var sessionContext = require('PackagesInSession');
        var packagesInSession = sessionContext.get("packages");

        if (packageId === null) {
            var packageInfo = getWindowObj().mentor.publisher.packectInfo;
            packageId = packageInfo !== undefined ? packageInfo.packageId : null;
        }

        if (packagesInSession.length === 1) {
            packageId = packagesInSession.at(0).get('id').replace("data/", "").replace("data\\", "");
        }

        if (!packageId) {
            return;
        }

        url += `&packageId=${packageId}`;

        $.ajax({
            url: url,
            type: 'GET',
            async: false,
            dataType: 'json',
            success: function (response, textStatus, xhr) {
                dictionary.translations = response;
            },
            error: function (error, textStatus, errorThrown) {
                console.error('Error fetching translations for ' + langKey + ':', error);
            }
        });
    };

    shouldFetchTranslations = function (dictionary, choice) {
        if (!dictionary) {
            return false;
        }
        const isLanguageChanged = dictionary.currentLangChoice !== choice;
        const missingTranslations = dictionary.translations === undefined;
        return isLanguageChanged || missingTranslations;
    }

    return {
        getCurLangChoice : function () {
            load();
            var langDictionary = dictionary || { currentLangChoice : ""}, langNumb;
            langNumb = langDictionary.currentLangChoice;
            return langNumb;
        },
        getCurrentLanguage : function () {
            var langNumb = this.getCurLangChoice(), curLang;
            curLang = dictionary && dictionary.orderedLangList ? dictionary.orderedLangList[langNumb] : undefined;
            // ACE bug fix for 787111 - if no language is selected
            if (curLang) {
                return curLang.langKey;
            }
            return '';
        },
        getOrderedLangList : function () {
            load();
            var ans = [], langDictionary, orderedLangList, s, eLang, item, itemstr;
            langDictionary = dictionary || {orderedLangList : []};
            orderedLangList = langDictionary.orderedLangList;
            s = orderedLangList.length;
            for (eLang = 0; eLang < s; eLang = eLang + 1) {
                itemstr = getLangString(eLang, orderedLangList);
                //  orderedLangList[eLang].country;
                //  orderedLangList[eLang].nation;
                ans.push({
                    code: orderedLangList[eLang].langKey,
                    language: orderedLangList[eLang].language,
                    "name" : itemstr
                });
            }

            return ans;
        },
		resetDefaultLanguageChoice: function() {
			isDefaultLanguageLoaded = '';
		},
        getKnownLanguageCodes : function () {
            var orderedLangList = (dictionary && dictionary.orderedLangList) || [];
            return _.map(orderedLangList, function (entry) {
                return entry.langKey;
            });
        },
        hasLanguageChanged : function (choiceStr) {
            var langDictionary = dictionary, newChoiceNumber;
            newChoiceNumber = getNewChoiceNumber(choiceStr, langDictionary.orderedLangList);
            if (shouldFetchTranslations(langDictionary, newChoiceNumber)) {
                langDictionary.currentLangChoice = newChoiceNumber;
                var newLangKey = langDictionary.orderedLangList[newChoiceNumber].langKey;
                fetchTranslations(newLangKey);
                return true;
            }
            return false;
        },
        getLanguageDictionary : function () {
            load();
            return dictionary;
        },
        getViewerLanguage : function () {
            return getViewerLanguage();
        },
        setCurrentLanguageChoice : function (choice) {
            if (shouldFetchTranslations(dictionary, choice)) {
                var newLangKey = dictionary.orderedLangList[choice].langKey;
                fetchTranslations(newLangKey);
            }
            dictionary.currentLangChoice = choice;
        },
        reset : function () {
            isLoaded = false;
        },
        setPackageId : function (value) {
            packageId = value;
        },
        getPackageId : function () {
            return packageId;
        }
    };

}(mentor.publisher));