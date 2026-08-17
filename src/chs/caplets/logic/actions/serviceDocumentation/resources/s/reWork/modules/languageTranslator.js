/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global mentor, Utils, $,window, applyLanguageFilter*/
mentor.publisher.languageTranslator = (function () {
    "use strict";
    var getLanguageResourceFile, loadResourceFile, resourcesData, loadDefaultResources, parseResourceFile, isLoaded,
            load, translateToLanguage, languageButtonClickHandler, init, initialLanguage;

    getLanguageResourceFile = function (languageCode) {
        var filename, code;

        code = languageCode && languageCode.toLowerCase();
        if (!code || (code === 'en')) {
            filename = 'resources/resources.properties';
        } else {
            filename = 'resources/resources_' + code + '.properties';
        }

        return filename;
    };

    loadResourceFile = function (filename) {
        resourcesData = [];
        $.ajax({async : false, url : filename,
            success : function (data, textStatus, XMLHttpRequest) {
                if (!data) {
                    loadDefaultResources();
                    return;
                }
                var lines = data.split("\n");
                parseResourceFile(lines);
            },
            error : function (data, textStatus, XMLHttpRequest) {
                loadDefaultResources();
            }, dataType : "text"});
    };

    loadDefaultResources = function () {
        $.ajax({
            async : false, 
            url : 'resources/resources.properties',
            success : function (data, textStatus, XMLHttpRequest) {
                var lines = data.split("\n");
                parseResourceFile(lines);
            },
            error : function (data, textStatus, XMLHttpRequest) {
                loadDefaultResources();
            }, 
            dataType : "text"
        });
    };

    parseResourceFile = function (lines) {
        var count, value;
        for (count = 0; count < lines.length; count = count + 1) {
            if (lines[count].split('=').length === 2) {
                value = lines[count].split('=')[1].trim();
                if (isNaN(value)) {
                    resourcesData[lines[count].split('=')[0].trim()] = value;
                } else {
                    /**
                     * workaround ..number value causes viewer to fail while loading
                     */
                    resourcesData[lines[count].split('=')[0].trim()] = value + " ";
                }

            }
        }
        isLoaded = true;
    };

    load = function (language) {
        var resourceFileName = getLanguageResourceFile(language);
        loadResourceFile(resourceFileName);
    };

    translateToLanguage = function (language, doNotApply) {
        //var dataLoader = mentor.publisher.languageDataLoader;
        load(language);
        mentor.publisher.LanguageFilteredProject.setCurrentLanguage(language);
        applyLanguageFilter(doNotApply);
    };

    init = function (lang) {
        var dataLoader = mentor.publisher.languageDataLoader, lanList = [
        ], choice, viewerLanguage, k, langCodeToShow, choiceNum = 0;
        lanList = dataLoader.getOrderedLangList();
        viewerLanguage = lang || dataLoader.getViewerLanguage();
        //the initial language should be based on the following
        //todo if the cookie is set , then go for the cookie??i do not think so
        //viewer language -> URL or browser language
        //otherwise , the first language in the list
        if (lanList && lanList.length > 0) {
            try {
                for (k = 0; k < lanList.length; k = k + 1) {
                    if (lanList[k].name.split(" : ")[0].toUpperCase() === viewerLanguage.toUpperCase()) {
                        langCodeToShow = lanList[k].name.split(" : ")[0];
                        choiceNum = k;
                        break;
                    }
                }
                if (!langCodeToShow) {
                    langCodeToShow = lanList[0].name.split(" : ")[0];
                    if (!window.langCodeMsgShown) {
                        alert(mentor.publisher.languageTranslator.localize("AlertViewNotFound").format(viewerLanguage.toUpperCase(), langCodeToShow));
                        window.langCodeMsgShown = true;
                    }
                }
                $('.languageBtn').html(langCodeToShow);
                //added this here, the lang button is not loaded initially, so this is used later
                initialLanguage = langCodeToShow;
                mentor.publisher.toolTip.changeToolTipTextOnButton($('.languageBtn'),
                        mentor.publisher.languageTranslator.localize('Language') + langCodeToShow);
                load(langCodeToShow);
            }
            catch (e) {
            }
            //choice = $('.languageBtn').html();
            dataLoader.setCurrentLanguageChoice(choiceNum);
            mentor.publisher.languageTranslator.translate(langCodeToShow);
        }
    };


    languageButtonClickHandler = function (event) {
        var languageListItems = [], index, langMenuLength, langMenu = [], langLoader;
        langLoader = mentor.publisher.languageDataLoader;
        langMenu = langLoader.getOrderedLangList();
        langMenuLength = langMenu.length;
        for (index = 0; index < langMenuLength; index = index + 1) {
            languageListItems.push({mainText : langMenu[index].name, id : langMenu[index].name});
        }
        var currentTarget = event.currentTarget;
        var clientY = currentTarget ? (currentTarget.offsetTop + currentTarget.offsetHeight) : event.clientY;
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_LANGUAGES_POPUP, {
            x: event.clientX || x,
            y: clientY || y,
            models: languageListItems
        });
        //createLanguagePopover(event.clientX, event.clientY, languageListItems);
        event.stopPropagation();
    };

    return {
        initialize : function (lang) {
            //Reset the LanguageFilteredProject object, it may contain reference to previous project
            // this happens when a new project is loaded
            init(lang);
        },
        translate : function (language, doNotApply) {
            if (!language) {
                language = $('.languageBtn').html();
            }
            translateToLanguage(language, doNotApply);
        },
        loadResources: load,
        clickHandler : languageButtonClickHandler,
        localize: function (message) {
            if (resourcesData && resourcesData[message]) {
                return resourcesData[message].replace(/\\u[\dA-F]{4}/gi,
                        function (match) {
                            return String.fromCharCode(parseInt(match.replace(/\\u/g, ''), 16));
                        });
            }
            else {
                return message;
            }
        },
        currentLanguage : function () {
            //todo correct this one
            return "currentLang";
        },
        isLoaded : function(){
            return isLoaded;
        }
    };

}());
