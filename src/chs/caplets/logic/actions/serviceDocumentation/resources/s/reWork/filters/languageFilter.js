/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global Utils, mentor, extend, $, TextWrapper*/
mentor.publisher.LanguageFilteredProject = (function ()
{
    "use strict";
    var currentLanguage, project, translateAttributedObject, translateToolTips, getTranslatedToolTip, translateAttributedObjects, getTranslatedObject, getDictionary;
    //todo do translation, use Utils.translate wherever needed ->check, its done
    translateAttributedObjects = function (attrs)
    {
        var translatedObjects = [], k = 0;
        if (attrs) {
            for (k = 0; k < attrs.length; k = k + 1) {
                translatedObjects.push(translateAttributedObject(attrs[k]));
            }
        }
        return translatedObjects;
    };
    translateAttributedObject = function (attributedObject)
    {
        var sd, toolTips, mainText, subText, temp = mentor.publisher.object(attributedObject);

        if (typeof temp.getShortDescription === "function") {
            sd = temp.getShortDescription();
            temp.getShortDescription = function ()
            {
                return Utils.translateText(getDictionary(), sd, currentLanguage);
            };
        }

        if (typeof temp.getToolTips === "function") {
            toolTips = temp.getToolTips();
            temp.getToolTips = function ()
            {
                return translateToolTips(toolTips);
            };
        }

        if (temp.mainText) {
            mainText = temp.mainText;
            temp.mainText = Utils.translateText(getDictionary(), mainText, currentLanguage);
        }

        if (temp.subText) {
            subText = temp.subText;
            temp.subText = Utils.translateText(getDictionary(), subText, currentLanguage);
        }

        if (temp.type === "diagnostic" && temp.folder) {
            temp.folder = Utils.translateDiagnosticFolder(temp.folder);
        }

        temp.withoutTranslation = mentor.publisher.object(attributedObject);
        return temp;

    };
    translateToolTips = function (toolTips)
    {
        var k, newTT = [];
        for (k = 0; k < toolTips.length; k = k + 1) {
            newTT.push(getTranslatedToolTip(toolTips[k]));
        }
        return newTT;
    };
    getTranslatedToolTip = function (tooltip)
    {
        return {
            getName: function ()
            {
                return tooltip.getName();
            },
            getValue: function ()
            {
                return Utils.translateText(getDictionary(), tooltip.getValue(), currentLanguage);
            }
        };
    };
    getDictionary = function ()
    {
        return mentor.publisher.languageDataLoader.getLanguageDictionary();
    };
    getTranslatedObject = function (object)
    {
        return translateAttributedObject(object);
    };
    return {
        /**
         * Translates a quick code i.e. text with opening and closing curly brace, {text}
         * if there is no translation in dictionary, it returns plain text without the curly braces.
         *
         * @param quickcode
         * @returns {string}
         */
        translateQuickCode: function (quickcode)
        {
            var translatedValue = '';
            quickcode = quickcode || "";

            translatedValue = Utils.translateText(getDictionary(), quickcode.trim(), currentLanguage);
            if (translatedValue === quickcode.trim()) {
                translatedValue = quickcode.replace("{", "").replace("}", "");
            }

            return translatedValue;
        },
        filterInformationPages : function (filteredObjs) {
            var filteredIntro = [], index, curLang = mentor.publisher.languageDataLoader.getCurrentLanguage() ||
                    "", regex, knownLanguageCodes, discardableLanguageCodes;

            knownLanguageCodes = mentor.publisher.languageDataLoader.getKnownLanguageCodes() || [];
            discardableLanguageCodes = _.without(knownLanguageCodes, curLang);

            if (!_.isEmpty(discardableLanguageCodes)) {
                regex = new RegExp("_(" + discardableLanguageCodes.join("|") + ")$", "i");
            }

            _.each(filteredObjs, function (value) {
                if (!mentor.publisher.urlParams.ignoreInfoTranslation && value.mainText && value.mainText.lastIndexOf("_") >= 0) {

                    if (regex && regex.test(value.mainText)) {
                        return;
                    }
                }

                filteredIntro.push(value);
            });

            return filteredIntro;
        },
        getLocalizedName : function (name) {
            var currentLanguage = mentor.publisher.languageDataLoader.getCurrentLanguage(),
                    regex;

            if (name.lastIndexOf("_") < 0 || _.isEmpty(currentLanguage)) {
                return name;
            }

            regex = new RegExp("_" + currentLanguage + "$", "i");
            if (!regex.test(name)) {
                return name;
            }

            return name.substring(0, name.lastIndexOf("_"));
        },
        setCurrentLanguage: function (language)
        {
            currentLanguage = language;
        },
        getCurrentLanguage: function ()
        {
            return currentLanguage;
        },
        applyFilter: function (objectsWithAttributes)
        {
            var k, translatedObjects = [];
            if (objectsWithAttributes && objectsWithAttributes.length > 0) {
                for (k = 0; k < objectsWithAttributes.length; k = k + 1) {
                    translatedObjects.push(getTranslatedObject(objectsWithAttributes[k]));
                }
            }
            return translatedObjects;
        },
        getDiagrams: function (type)
        {
            var diagrams = project.getDiagrams(type), p = this;
            return p.applyFilter(diagrams);
        },
        setProject: function (projectData)
        {
            project = projectData;
        },
        getProject: function ()
        {
            return project;
        },
        getSystems: function ()
        {
            var systems = project.getSystems(), p = this;
            return p.applyFilter(systems);
        },
        getObjects: function (type, loadAllObjects)
        {
            var objects = project.getObjects(type, loadAllObjects), p = this;
            return p.applyFilter(objects);
        },
        getObjectById: function (objectId)
        {
            var objects = [], originalObject, translatedObject;
            originalObject = project.getObjectById(objectId);
            objects.push(originalObject);
            translatedObject = this.applyFilter(objects)[0];
            return translatedObject;
        },
        loadObjectData: function (systemId, objectUid)
        {
            var objects = [], originalObject, translatedObject;
            originalObject = project.loadObjectData(systemId, objectUid);
            objects.push(originalObject);
            translatedObject = this.applyFilter(objects)[0];
            return translatedObject;
        },
        getId: function ()
        {
            return project.getId();
        },
        getReports: function (type)
        {
            var reps = project.getReports(type), p = this;
            return p.applyFilter(reps);
        },
        getInformation: function ()
        {
            return project.getReports('introduction-page');
        },
        createListGroups: function ()
        {
            return project.createListGroups();
        },
        getData: function (type, systemId, diagramId)
        {
            return project.getData(type, systemId, diagramId);
        },
        getFirstSection: function ()
        {
            var firstSection = project.getFirstSection(), translatedListItems = [
            ], temp = mentor.publisher.object(firstSection);
            translatedListItems = this.applyFilter(firstSection.listItems());
            temp.listItems = function ()
            {
                return translatedListItems;
            };
            return temp;
        },
        get: function (name)
        {
            var objects = [], originalObject, translatedObject;
            originalObject = project.get(name);
            if (originalObject && originalObject.length > 0) {
                objects = originalObject;
                translatedObject = this.applyFilter(objects);
            }
            else if (originalObject) {
                objects.push(originalObject);
                translatedObject = this.applyFilter(objects)[0];
            }
            return translatedObject;
        },
        getByType: function (type)
        {
            var objects = [], originalObject, translatedObject = objects;
            originalObject = project.getByType(type);
            if (originalObject && originalObject.length > 0) {
                objects = originalObject;
                translatedObject = this.applyFilter(objects);
            }
            else if (originalObject && !$.isArray(originalObject)) {
                objects.push(originalObject);
                translatedObject = this.applyFilter(objects)[0];
            }
            return translatedObject;
        },
        getCustomData: function ()
        {
            return  project.getCustomData();
        }
    };
}());

function applyLanguageFilter(doNotApply)
{
    "use strict";
    var originalProject;
    if (!doNotApply) {
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.LANGUAGE_FILTER_APPLIED,
            {lang: mentor.publisher.LanguageFilteredProject.getCurrentLanguage()});
    }

}



