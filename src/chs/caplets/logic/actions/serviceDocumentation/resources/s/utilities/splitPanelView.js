/*global Msg, mentor*/
var splitPanelView = (function (p)
{
    "use strict";
    function isNotNull(variable)
    {
        return typeof (variable) !== "undefined" && variable !== null;
    }

    function isNonEmpty(variable)
    {
        return isNotNull(variable) && variable.trim() !== "";
    }

    return {
        titleToDisplay: null,
        getTitleToDisplay: function ()
        {

            return splitPanelView.titleToDisplay;
        },
        getFaceViewWindowTitle: function (faceViewName, uid, systemID)
        {
            var designObject = p.objectDataLoader.load(systemID, uid,
                    p.project.getId()), windowTitleConfigData, windowTitle;
            this.handleObjectData(designObject);
            windowTitle = this.getPathInformation(mentor.publisher.languageTranslator.localize('ConnectorsTitle'),
                    faceViewName,
                    mentor.publisher.languageTranslator.localize(splitPanelView.getConnectorViewTitle()));
            windowTitleConfigData = p.dataLoader.getWindowTitleConfigData();

            if (windowTitleConfigData.showPathForFaceViews === "true") {
                return this.addSpaceBetPathAndConnectorInfo(windowTitle, this.titleToDisplay);
            }
            return this.titleToDisplay;

        },
        addSpaceBetPathAndConnectorInfo: function (pathInfo, connInfo)
        {
            return "<span class='faceViewPathInfo'>" + pathInfo +
                    "</span><span class='faceViewConnInfo'>" + connInfo + "</span>";
        },

        getTwoDWindowTitle: function (twoSVGName)
        {
            var windowTitleConfigData = p.dataLoader.getWindowTitleConfigData();
            if (windowTitleConfigData.showPathFor2dViews === "true") {
                return this.getPathInformation(mentor.publisher.languageTranslator.localize('locationViews'),
                        twoSVGName, null);
            }
            return twoSVGName;
        },
        getPathInformation: function (leftNavigationTitle, objectName, attributePanelTitle)
        {
            return leftNavigationTitle + "/" + objectName +
                    (isNonEmpty(attributePanelTitle) === true ? "/" + attributePanelTitle : "");
        },
        handleObjectData: function (objectData)
        {
            if (isNotNull(objectData)) {
                this.createWindowTitle(objectData);
            }
        },
        createWindowTitle: function (objectData)
        {
            var windowTitleConfigData = p.dataLoader.getWindowTitleConfigData(), delimiter, attributesToDisplayOnWindow, attributeAssociativeMap;
            delimiter = windowTitleConfigData.delimiter;
            attributesToDisplayOnWindow = windowTitleConfigData.attributeNames;
            attributeAssociativeMap = this.createAttibuteMap(objectData.getAttributes().listItems);
            this.titleToDisplay =
                    this.createTitle(attributesToDisplayOnWindow, attributeAssociativeMap, delimiter);
            return this.titleToDisplay;
        },

        createAttibuteMap: function (attributeArray)
        {
            var attributes = {}, attribute, length = attributeArray.length || [], i;
            for (i = 0; i < length; i = i + 1) {
                attribute = attributeArray[i];
                attributes[attribute.name.toLowerCase().replace(" ", "")] = attribute.value;
            }
            return attributes;
        },

        createTitle: function (attributesToDiaplay, attributes, delimiter)
        {
            var attributeName, attributeValue, title = '', length, i;
            length = attributesToDiaplay.length || [];
            for (i = 0; i < length; i = i + 1) {
                attributeValue = attributes[attributesToDiaplay[i].trim().toLowerCase().replace(" ", "")];
                if (isNonEmpty(attributeValue)) {
                    if (title !== '') {
                        title = title + delimiter;
                    }
                    title = title + attributeValue;
                }
            }
            return title;
        },

        getConnectorViewTitle: function ()
        {
            if (mentor.publisher.config["show-multiple-connector-views"]) {
                return "ConnectorViewsTitle";
            }
            else {
                return "FaceViewTitle";
            }
        }
    };

}(mentor.publisher));