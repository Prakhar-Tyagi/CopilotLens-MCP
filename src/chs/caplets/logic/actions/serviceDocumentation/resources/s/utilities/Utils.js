/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/**
 * @Fileoverview This class contains the Utility methods , that are used
 * for common purpose.
 */
jQuery.extend({
    Utils: function () {
        this.isNotHTTP = function () {
            return ("" + window.location).indexOf("http") !== 0;
        };

        this.matchAndTranformObjectTitlePattern = function (inStr, subStrTransformer) {
            var startCharOfProp = '[';
            var endCharOfProp = ']';
            var stack = [];
            var chars = inStr.split("");
            var attributesIndexes = [];
            var pair = [];
            for (var i = 0; i < chars.length; i++) {
                var currChar = chars[i];
                if (currChar === startCharOfProp) {
                    if (stack.length === 0) {
                        pair = [];
                        pair.push(i);
                    }
                    stack.push(currChar)
                }
                else if (currChar === endCharOfProp) {
                    var currLength = stack.length;
                    if (stack[currLength - 1] === startCharOfProp) {
                        stack.pop();
                        if (stack.length === 0) {
                            pair.push(i);
                            attributesIndexes.push(pair);
                        }
                    }
                    else {
                        stack.push(currChar)
                    }
                }
            }
            var substrinsToTranform = [];
            attributesIndexes.forEach(function (pair) {
                substrinsToTranform.push(inStr.substring(pair[0], pair[1] + 1));
            });
            substrinsToTranform.forEach(function (subString) {
                inStr = inStr.replace(subString, subStrTransformer(subString, startCharOfProp, endCharOfProp));
            })
            return inStr;
        };

        this.prepareFilePath = function (url) {
            //convert backward slashes to forward '/'
            if(url != undefined) {
                url = url.replace(/\\/g, '/');
                url = decodeURI(url);
                url = encodeURI(url);
                return this.processUrlBeforeRequest(url);
            }else{
                return "";
            }
        };

        this.processUrlBeforeRequest = function (url) {
            var effSetter = require("filehandlers/effectivitySetter");
            var urlWithSubfolderPathAndEffectivity = effSetter.addEffectivitAndZipLocationInURLAsParameters(url);
            return effSetter.distinguishZippedContent(urlWithSubfolderPathAndEffectivity);
        };

        this.getTime = function () {
            return ((new Date()).getMilliseconds());
        };

        /**
         * This method provide name space
         */
        this.namespace = function () {
            var o, d;
            $.each(arguments, function (i) {
                d = arguments[1].split(".");
                o = window[d[0]] = window[d[0]] || {};
                d = d.slice(1);
                $.each(d, function (i) {
                    o = o[d[i]] = o[d[i]] || {};
                });
            });
            return o;
        };

        /**
         * It sorts the give array
         * @param targetArray  Given array
         * @return Array
         */
        this.sortArrayData = function (targetArray) {
            if (typeof(targetArray) !== 'undefined') {
                targetArray.sort(function (a, b) {
                    var nameA = a.name.toLowerCase();
                    var nameB = b.name.toLowerCase();
                    if (nameA < nameB) {
                        return -1;
                    }
                    if (nameA > nameB) {
                        return 1;
                    }
                    return 0;
                });
            }
        };

        /**
         *  For removing the an element from am array for given index
         * @param targetArray  Given array
         * @param index  index number
         * @return Array
         */
        this.removeByIndex = function (targetArray, index) {
            return targetArray.splice(index, 1);
        };

        /**
         * This method returns the value for the given parameter from the URL.
         * @param parameterName
         */
        this.getUrlParameter = function (parameterName) {
            return this.getParameterValueFromURL(parameterName, this.getURL());
        };

        this.getParameterValueFromURL = function (parameterName, url) {
            parameterName = parameterName.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
            var regexS = "[\\?&]" + parameterName + "=([^&#]*)";
            var regex = new RegExp(regexS);
            var results = regex.exec(url);

            //If nothing is found, return nothing
            if (results === null) {
                return "";
            }

            //Otherwise return the second match (the value)
            //decode it because , the this wil already be encoded.
            //while loading any panel, this will again be encoded.
            //so unless we decode it here and bring it back to normal state,
            //it will be encoded twice which will screwup.
            return decodeURI(results[1]);
        };

        this.sortByGivenOrder = function (arrayToBeSorted, order, keyGetter) {
            return arrayToBeSorted.sort(function (a, b) {
                return order && order.indexOf(keyGetter(a)) > order.indexOf(keyGetter(b)) ? 1 : -1;
            });
        }

        this.sort = function (a, b, key) {
            key = key || "nameAttr";

            if (!a || !b || !key || !a[key] || !b[key]) {
                return 0;
            }

            return Utils.alphaNumericCompareFn(a[key], b[key]);
        };

        this.alphaNumericCompareFn = function (a, b) {
            function chunkify(t)
            {
                var tz = [], x = 0, y = -1, n = 0, i, j;

                while (i = (j = t.charAt(x++)).charCodeAt(0)) {
                    var m = (i == 46 || (i >= 48 && i <= 57));
                    if (m !== n) {
                        tz[++y] = "";
                        n = m;
                    }
                    tz[y] += j;
                }
                return tz;
            }

            var aa = chunkify(a.toLowerCase());
            var bb = chunkify(b.toLowerCase());

            for (var x = 0; aa[x] && bb[x]; x++) {
                if (aa[x] !== bb[x]) {
                    var c = Number(aa[x]), d = Number(bb[x]);
                    if (c == aa[x] && d == bb[x]) {
                        return c - d;
                    }
                    else {
                        return (aa[x] > bb[x]) ? 1 : -1;
                    }
                }
            }
            return bb.length - aa.length;
        }

        this.getURL = function () {
            return document.location.href;
        };

        /**
         * This method reads a Cookie.
         * @param name of the cookie
         */

        this.readCookie = function (name) {
            // TODO: Added this check for testing. Need to check why window.cookieHandler is undefined in Karma flow.
            if (window.cookieHandler) {
                return window.cookieHandler.readCookie(name);
            }
            return '';
        };

        /**
         * This method writes a Cookie.
         * @param name of the cookie
         * @param value value of the cookie
         * @param days lifetime of the cookie
         */

        this.createCookie = function (name, value, days) {
            window.cookieHandler.createCookie(name, value, days);
        };

        this.getCookiesDuration = function () {
            let duration = window.mentor && window.mentor.publisher.serverConfig ?
                    +window.mentor.publisher.serverConfig['cookies-duration'] : 365;
            if (isNaN(duration) || duration == 0) {
                duration = 365;
            }
            return duration;
        };

        this.getLocationSpecificCookieName = function (name) {
            var path = window.location.pathname;
            var lastSlash = path.lastIndexOf("/");
            if (lastSlash !== -1) {
                path = path.slice(0, lastSlash)
            }

            return path + "##" + name;
        };

        this.getDataLoader = function (dataLoader) {
            if (window.mentor && mentor.publisher.languageDataLoader) {
                return mentor.publisher.languageDataLoader;
            }

            if (window.opener && window.opener.mentor) {
                return window.opener.mentor.publisher.languageDataLoader;
            }

            return mentor.publisher.languageDataLoader;
        };

        this.stripOffTranslationMarkers = function (rawValue, translatedVal) {
            if (rawValue && this.isTranslatableQuickCode(rawValue) && translatedVal === rawValue) {
                return rawValue.replace(/{/g, "").replace(/}/g, "");
            }
            else {
                return translatedVal;
            }
        }

        this.handleTranslation = function (rawValue, returnRawValue=false) {
            var regexForQuickCode = /^.*{.*}.*$/;
            if (regexForQuickCode.test(rawValue)){
                return this.translate(rawValue);
            }
            return returnRawValue ? rawValue : this.translatePlainText(rawValue);
        };


        this.translate = function (rawValue) {
            var dataLoader, langDictionary, currentLangChoiceString;
            dataLoader = this.getDataLoader(dataLoader);
            langDictionary = dataLoader.getLanguageDictionary();
            currentLangChoiceString = dataLoader.getCurrentLanguage();
            var translatedVal = this.translateText(langDictionary, rawValue, currentLangChoiceString);
            return this.stripOffTranslationMarkers(rawValue, translatedVal);
        };

        this.translatePlainText = function (text) {
            var quickCode = '{' + text + '}', translatedValue;
            text = text || "";
            if (!text.trim()) {
                return text;
            }
            translatedValue = this.translate(quickCode);
            if (translatedValue !== quickCode) {
                return translatedValue;
            }
            return text;
        };

        this.isTranslatableQuickCode = function (text) {
            var regexForQuickCode = /^{.*}$/;
            text = text || "";

            return regexForQuickCode.test(text);

        };

        /**
         * Convert string items if found in the dictionary .
         * @param langDictionary the loaded language dictionary
         * @param someOldNodeValue - the old note value before conversion
         * @param currentLanguageStr - the language string we are currently using
         */

        this.translateText = function (langDictionary, someOldNodeValue, currentLanguageStr) {
            // ACE bug fix for 787111 - if no language is selected
            if (!currentLanguageStr) {
                return someOldNodeValue;
            }

            var ESCAPE_CHAR = '\x1E';

            var QUICKCODE_START = '{';
            var QUICKCODE_END = '}';

            var hasCurly = someOldNodeValue ? someOldNodeValue.indexOf(QUICKCODE_START) : -1;
            if (-1 === hasCurly) {
                return someOldNodeValue;  // no change
            }

            var isTranslated = false;
            var newStringValue = "";
            var qcChars = "";
            var prevCharIsEscape = false;
            var numbofChars = someOldNodeValue.length;

            for (var i = 0; i < numbofChars; i++) {
                var c = someOldNodeValue.charAt(i);
                if (i !== 0) // not tested on first iteratoration but is set false above
                {
                    var pc = someOldNodeValue.charAt(i - 1);
                    prevCharIsEscape = (pc === ESCAPE_CHAR);
                }

                switch (c) {
                    case QUICKCODE_START:
                        if (!prevCharIsEscape) {
                            isTranslated = true;
                        }
                        break;
                    case QUICKCODE_END:
                        if (prevCharIsEscape) {
                            break;
                        }
                        else {
                            var translation;
                            if (langDictionary && langDictionary.translations) {
                                translation = langDictionary.translations[qcChars];
                            }
                            if (translation === undefined) {
                                newStringValue = newStringValue + QUICKCODE_START + qcChars + QUICKCODE_END;
                            } else {
                                newStringValue = newStringValue + translation;
                            }
                            isTranslated = false;
                            qcChars = '';
                        }
                        break;
                    default:
                        if (isTranslated) {
                            qcChars = qcChars + c;
                        }
                        else {
                            newStringValue = newStringValue + c;
                        }
                }
            }
            /**
             * if there is no translation for a quick code in a selected language then display quick code only
             * ,similiar behaviour is in capital publisher
             */
            if (newStringValue == "") {
                /**
                 * no translation found then return the quick code to display
                 */
                return someOldNodeValue;
            }
            return newStringValue;
        };

        this.getDiagnosticFolderDelimiter = function () {
            return mentor.publisher.config["diagnostic-hierarchy-level-delimiter"];
        }

        this.translateDiagnosticFolder = function (folder) {
            var delimiter = this.getDiagnosticFolderDelimiter();
            var translatedFolder = folder.split(delimiter).map(function (component) {
                return Utils.translate(component);
            }).join(delimiter);
            return translatedFolder;
        };

        this.notNull = function (obj) {
            return !!(obj != null && typeof(obj) != 'undefined');
        };
        this.is_mozilla = function () {
            return navigator.userAgent.match(/Firefox/i);
        };

        this.is_msie = function () {
            return navigator.userAgent.match(/msie/i) || navigator.userAgent.match(/Trident/i);
        };

        this.isEdge = function () {
            return navigator.userAgent.match(/Edge/i);
        };

        this.is_safari = function () {
            var is_safari = navigator.userAgent.match(/Safari/i);
            var isChromiumOrChrome = navigator.userAgent.match(/Chromium|Chrome/i);
            return is_safari && !isChromiumOrChrome;
        };

        this.is_mobile_device = function () {
            return navigator.userAgent.match(/Mobile|mini|Fennec|Android|iP(ad|od|hone)/i)
        }

        /*
         Fidn the index based on value object of array
         */
        this.findIndexOfObject = function (array, objectToBeSearched) {
            var ctr = "";
            if (!Utils.notNull(array) || !objectToBeSearched) {
                return "";
            }
            for (var i = 0; i < array.length; i++) {
                // use === to check for Matches. ie., identical (===), ;
                var arrayObject = array[i];
                var passedObject = objectToBeSearched;

                if (Utils.notNull(array[i].value)) {
                    arrayObject = array[i].value;
                }
                if (Utils.notNull(objectToBeSearched.value)) {
                    passedObject = objectToBeSearched.value;
                }
                if (arrayObject.trim() === passedObject.trim()) {
                    ctr = i;
                    break;
                }
            }
            return ctr;
        };

        this.compareArrayContents = function (arr1, arr2) {
            var result = true;
            for (var index = 0; index < arr1.length; index++) {
                var isMatched = false;
                var findInArr2 = this.findIndexOfObject(arr2, arr1[index]);//Finds the index based on value of
                                                                           // arr1[index]
                if (findInArr2 !== "" && arr1[index].name === arr2[findInArr2].name) {//Just check the name as well
                    isMatched = true;
                }
                result = result && isMatched;
            }
            return result;//
        };

        this.stripX = function (value) {
            var str = value;
            if (value) {
                str = str.replace(/\&amp;/g, '&');
                str = str.replace(/\&lt;/g, '<');
            }
            else {
                str = str.replace(/\&/g, '&amp;');
                str = str.replace(/</g, '&lt;');
            }
            return str;
        };

        this.getUrlVars = function () {
            var vars = [];
            var hash = [];
            var hashes = [];
            hashes = this.getURL().slice(this.getURL().indexOf('?') + 1).split('&');
            for (var i = 0; i < hashes.length; i++) {
                hash = hashes[i].split('=');
                vars.push(hash[0]);
                vars[hash[0]] = hash[1];
            }
            return vars;
        };

        this.htmlEncode = function (text) {
            var divElement,
                    textElement;

            divElement = document.createElement('div');
            textElement = document.createTextNode(text);

            divElement.appendChild(textElement);

            return divElement.innerHTML;
        };

        this.htmlDecode = function (html) {
            var divElement;

            divElement = document.createElement('div');
            divElement.innerHTML = html;

            return divElement.textContent;
        };
        this.isPopoutWindow = function () {
            return window.opener && window.opener.mentor;
        };
        // it will filter out the language suffix if available
        this.getIntroductionFileName = function (introductionFileName, selectedLang) {
            if (introductionFileName) {
                const lastIndexOf = introductionFileName.lastIndexOf("_");
                // if it contains underscore "_"
                if(lastIndexOf >= 0) {
                    const lang = introductionFileName.substring(lastIndexOf + 1, introductionFileName.length);
                    const availableLanguageCode = mentor.publisher.languageDataLoader.getKnownLanguageCodes();
                    // validate language code
                    if (availableLanguageCode.indexOf(lang) !== -1) {
                        return introductionFileName.substring(0, lastIndexOf) + (selectedLang ? '_' + selectedLang : '');
                    }
                    return introductionFileName;
                }
                return introductionFileName;
            }
            return "";
        };
        this.resetUrlParams = function () {
            if (window.location.href.indexOf("?") > 0) {
                const urlObj = new URL(window.location.href);
                urlObj.search = '';
                window.history.pushState({}, document.title, urlObj.toString());
            }
        }
    }
});

function isChrome()
{
    var isChromium = window.chrome,
            winNav = window.navigator,
            vendorName = winNav.vendor,
            isOpera = winNav.userAgent.indexOf("OPR") > -1,
            isIEedge = winNav.userAgent.indexOf("Edge") > -1,
            isIOSChrome = winNav.userAgent.match("CriOS");

    if (isIOSChrome) {
        return true;
    }
    else if (
            isChromium !== null &&
            typeof isChromium !== "undefined" &&
            vendorName === "Google Inc." &&
            isOpera === false &&
            isIEedge === false
    ) {
        return true;
    }
    else {
        return false;
    }
}

function getWindowObj()
{
    if (window.opener && window.opener.mentor) {
        return window.opener;
    }
    return window;
}

Utils = new $.Utils();

function displayAttributes(schematicUID, uid, x, y, systemId)
{
    "use strict";
    return {
        showPopUpPanel: function (evt) {
            evt = evt || {};
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP,
                    {
                        id: uid, x: evt.clientX || x, y: evt.clientY ||
                    y, schemUID: schematicUID, systemId: systemId
                    });

            if (evt) {
                mentor.publisher.stopEventFlow(evt);
            }

        }
    };
}

function flattenMapValues(mapObject)
{
    "use strict";
    var allValues = new Array();
    mapObject.forEach(function (value, key) {
        allValues = allValues.concat(value);
    });
    return allValues;
    //Array.from(selectedObjects.values()).flat();
}

function display2DViewsAttributes(name, x, y, uidToHighlight, model, callback)
{
    "use strict";
    var twoD, noOFSystems = 0, index,
            data = {name: name, x: x, y: y, uidToHighlight: uidToHighlight, matches: model},
            firstTwoDObject, sharedUIDToObjectMap = {}, xrefs = flattenMapValues(model);
    /**
     * Check if all the instance are of same shared object
     */
    for (index in xrefs) {
        if (xrefs.hasOwnProperty(index)) {
            twoD = xrefs[index];
            if (!sharedUIDToObjectMap[twoD.sharedUID]) {
                sharedUIDToObjectMap[twoD.sharedUID] = twoD;
                noOFSystems = noOFSystems + 1;
            }
        }
    }
    if (xrefs && xrefs.length > 0) {
        require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter) {
            multipleDocumentRouter.save(true, xrefs[0].objectId);
        });
    }
    if ((xrefs && xrefs.length === 1) || noOFSystems === 1) {
        if (callback && callback.showDesignObjectPopover) {
            callback.showDesignObjectPopover(data);

        }
        else {
            firstTwoDObject = xrefs[0];
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP,
                    {id: firstTwoDObject.objectId, x: x, y: y, systemId: firstTwoDObject.systemId});
        }
    }
    else {
        if (callback && callback.showLinks) {
            callback.showLinks(data)
        }
        else {
            // // TODO: DOCS-8856 - This is temporary fix.
            // // Removing systems with no diagrams is not a ideal fix.
            // // This needs to be relooked when we fix 2d location poppover for harness.
            //
            // //for harness diagrams
            var filteredMap = new Map();
            data.matches.forEach(function (value, key) {
                var filteredSystems = value.filter(function (ele) {
                    return !!ele.diagramName;
                });
                if (filteredSystems.length != 0) {
                    filteredMap.set(key, filteredSystems);
                }
            });
            if (filteredMap.size == 0) {
                firstTwoDObject = xrefs[0];
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.OPEN_OBJECT_POPUP,
                        {id: firstTwoDObject.objectId, x: x, y: y, systemId: firstTwoDObject.systemId});
            }
            else {
                data.matches = filteredMap;
                mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.SHOW_POP_OVER_2D_VIEW, data);
            }
        }
    }
}

function displayJTAttributes(x, y, oPsid, partName, modelName)
{
    "use strict";
    var objDataArray, uidToHighlight, multipleObjectsData = new Map();;
    require(["jt3DModel"], function (jtCache) {
        objDataArray = jtCache.getSystemsForJTPart(oPsid, partName, modelName);
        if (objDataArray.length > 0) {
            uidToHighlight = objDataArray[0].connUID;
            multipleObjectsData.set(partName, objDataArray);
            display2DViewsAttributes(partName, x, y, uidToHighlight, multipleObjectsData);
        }
    });
}

var xml3dPlayerReady = function (viewerId, eventCategory, eventName, eventSender, eventParameters) {
    if (eventCategory == "Application") {
        APPLICATION = eventSender;
        if (eventName == "Ready") {
            mapEWVIA[viewerId] = eventSender;
            mapSelection[viewerId] = eventSender.ActiveEditor.Selection;
            if (typeof(mapReadyCallback[viewerId]) != 'undefined') {
                //mapReadyCallback[viewerId](viewerId);
            }
        }
    }
    else if (eventCategory == "Selection") {
        if (typeof(mapSelectionCallback[viewerId]) != 'undefined') {
            mapSelectionCallback[viewerId](eventCategory, eventName, eventSender, eventParameters);
        }
        else {
            setSelectionCallback(viewerId, onSelect);
        }

        if (eventName == "Add") {
            var currentElem = eventParameters;
            var selCount = currentElem.Count;
            if (selCount > 0) {
                for (i = 1; i <= selCount; i++) {
                    var selItem = currentElem.Item(i);
                    crossHighlightFrom3DXml(selItem.DisplayName);
                }
            }
        }

    }
    else if (eventCategory == "Mouse") {
        //alert("Mouse");
        if (typeof(mapMouseCallback[viewerId]) != 'undefined') {
            mapMouseCallback[viewerId](eventCategory, eventName, eventSender, eventParameters);
        }
    }
    else if (eventCategory == "Keyboard") {
        //alert("Keyboard");
        if (typeof(mapKeyboardCallback[viewerId]) != 'undefined') {
            mapKeyboardCallback[viewerId](eventCategory, eventName, eventSender, eventParameters);
        }
    }
    setTimeout(zoomInObject, 2);
};

var zoomInObject = function () {
    if (packageModel.get("partNumber") !== '') {
        try {
            select("xml3d", packageModel.get("partNumber"));
            reframeOnSelection("xml3d");
        }
        catch (error) {
        }
    }
};

var crossHighlightFrom3DXml = function (objectName) {
    var index = objectName.indexOf("("), currentFolder;
    objectName = objectName.substring(index + 1, objectName.length - 1);
    var activeProject = mentor.publisher.project.getId();
    currentFolder = crossHighlightFrom3DXml.systemId;
    //todo
    //INDIAEDIT--START . All designs are dumped in "Systems" folder and "Xml3Ds" has "X"
    mentor.publisher.dataLoader.get3dXmlId(activeProject, currentFolder, objectName, function (xmlIdData) {
        for (var index in xmlIdData['dataArray']) {
            window.crossHighlightHandler.initCrossHighlight(xmlIdData['dataArray'][index].id);
        }

    });
};
var removeSelection = function (partNumber) {
    if ((typeof(partNumber) !== 'undefined') && (partNumber !== null) && (partNumber.length > 0)) {
        unselect("xml3d", partNumber);
        reframe("xml3d");
    }
};

var onSelect = function (eventCategory, eventName, eventSender, eventParameters) {
};

var TranslationUtils = function ($) {
    "use strict";

    var replaceLanguageCodeWithTranslatedText,
            translateHTMLContent,
            translateNodeContent,
            translateNodesContent,
            translateSVGContent;

    /**
     * Translates quick code to its translation in current language
     *
     * When createCode is false, the @param element should contain text in following form
     * <div>{nodeText}</div> and it will be translated to <div>translated value</div>
     *
     * And when @param createCode is true, the node value need not be wrapped around curly braces.
     *
     * Also, if the @param element contains nested text elements then it wont be translated.
     * e.g. <div><span>{nodeText1}</span>{nodeText2}</div> will not get translated.
     *
     * But if there is no text in nested elements then it will get translated.
     * e.g.  columns in faceview cavity chart has following html
     *<div><span class="clickable-column" id="UID"></span>{columnText}</div>,
     * In this case, columnText will get translated
     *
     *
     */
    replaceLanguageCodeWithTranslatedText = function (element, createCode) {
        var dataLoader = mentor.publisher.languageDataLoader,
                langDictionary = dataLoader.getLanguageDictionary(),
                currentLangChoiceString = dataLoader.getCurrentLanguage(),
                rawVal = $(element).html() || "",
                translatedVal,
                id,
                textToTranslate;

        textToTranslate = rawVal;
        var curlyBracesAdded = false;
        /**
         * this check will add '{' and '}' to a text of the text does not contain a '{'
                 * i.e. 'testString' will become '{testString}' and '{code}'  will remain '{}'
                 */
        if (createCode && rawVal.indexOf("{") < 0) {
            var htmlDecode = Utils.htmlDecode(rawVal.trim());
            if(htmlDecode.trim().length !== 0) {
                textToTranslate = '{' + htmlDecode + '}';
                curlyBracesAdded = true;
            }
        }

        translatedVal = textToTranslate.replace(/\{[^\}]+\}/ig, function (match) {
            var decodedText = Utils.htmlDecode(match);
            var translation = Utils.translateText(langDictionary, decodedText, currentLangChoiceString);
            translation = Utils.stripOffTranslationMarkers(decodedText, translation);

            return Utils.htmlEncode(translation);
        });
        if (curlyBracesAdded) {
            translatedVal = rawVal.replace(htmlDecode, translatedVal);
            if (rawVal !== translatedVal) {
                $(element).html(translatedVal);
            }
        }
        else {
            if (translatedVal != textToTranslate) {
                $(element).html(translatedVal);
            }
        }
    };

    translateHTMLContent = function (root, options) {
        var dataTranslation;

        options = options || {};
        options.fallback = options.fallback || function () {
        };

        options.completion = options.completion || function () {

        };

        dataTranslation = $(root).attr('data-translation');
        if (!dataTranslation) {
            $('body', $(root)).children().each(function () {
                dataTranslation = $(this).attr('data-translation');
                if (dataTranslation) {
                    return false;
                }
            });
        }

        if (dataTranslation === 'marker-based') {
            var nodes = $('.translatable', root);
            translateNodesContent(nodes, {
                nodeContent: function (node, content) {
                    if (content) {
                        $(node).html(content);
                    }

                    return $(node).html();
                }
            });
        }
        else {
            options.fallback();
        }

        options.completion();
    };

    translateSVGContent = function (root, isCustomContent) {
        var dataTranslation,
                nodes;

        dataTranslation = $(root).attr('data-translation');
        if (dataTranslation === 'marker-based') {
            nodes = $('text[class~="translatable"], tspan[class~="translatable"]', root);
        }
        else {
            if (isCustomContent) {
                return;
            }

            nodes = $('text', root);
        }

        translateNodesContent(nodes, {
            nodeContent: function (node, content) {
                if (content) {
                    try {
                        var styler = new Styler(node, content, $('#viewport', root), root);
                        styler.applyStyle();
                    }
                    catch (e) {
                        node.textContent = content;
                    }
                }

                return node.textContent;
            }
        });
    };

    translateNodesContent = function (nodes, options) {
        $(nodes).each(function () {
            translateNodeContent(this, options);
        });
    };

    translateNodeContent = function (node, options) {
        var nodeContent = options.nodeContent(node) || "";
        var translatedContent = nodeContent;

        var quickCodes = nodeContent.match(/{[^}]+}/g), qcIndex, quickCode;
        if (quickCodes) {
            for (qcIndex in quickCodes) {
                if (quickCodes.hasOwnProperty(qcIndex)) {
                    quickCode = quickCodes[qcIndex];
                    var translation = Utils.translate(quickCode);
                    if (translation) {
                        translatedContent = translatedContent.replace(quickCode, translation);
                    }
                }
            }
        }

        if (translatedContent !== nodeContent) {
            options.nodeContent(node, translatedContent);
        }
    };

    return {
        replaceLanguageCodeWithTranslatedText: replaceLanguageCodeWithTranslatedText,
        translateHTMLContent: translateHTMLContent,
        translateSVGContent: translateSVGContent
    };
};

if (!String.prototype.endsWith) {
    String.prototype.endsWith = function (searchString, position) {
        var subjectString = this.toString();
        if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position ||
                position > subjectString.length) {
            position = subjectString.length;
        }
        position -= searchString.length;
        var lastIndex = subjectString.indexOf(searchString, position);
        return lastIndex !== -1 && lastIndex === position;
    };
}

function updateClientType(message, clientType)
{
    return message.replace(/{app}/, clientType ? clientType : "").replace(/\s+/g, " ");
}