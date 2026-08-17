/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define([], function ()
{
    var p = mentor.publisher;
    function getOptionExp(content)
    {
        return content.optionExpression ? content.optionExpression : "";
    }

    function getSystemId(content, selectedSystem)
    {
        return content.systemId || selectedSystem.get("idAttribute") || content.id;
    }

    function getFirstDiagramToShow(system)
    {
        var diagrams = system.getDiagrams();
        var firstDiaOrReport;
        for (var i in diagrams) {
            var diagram = diagrams[i];
            var opExp = diagram.getOptionExpression();
            var vinOptions = mentor.publisher.filter.vinOptions;
            var opFilter = new OptionExpressionFilter();
            if (opFilter.evaluteOptionsAgainstOptionExpressions(opExp, vinOptions)) {
                firstDiaOrReport = diagram;
                break;
            }
        }
        if (!diagram) {
            var reports = system.getReports();
            if (reports && reports.length > 0) {
                firstDiaOrReport = reports[0];
                if (firstDiaOrReport) {
                    firstDiaOrReport.reportId = firstDiaOrReport.id;
                }
            }
        }
        return firstDiaOrReport;
    }

    return {
        highlightObject: function (content) {
            p.eventDispatcher.dispatchEvent(p.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                objectId: content.objectId,
                systemId: content.systemId
            });
        }, openDiagram: function (selectedSystem, preDiagramId, preOptionExpression, content) {
            if (selectedSystem.get("diagramId") !== preDiagramId) {
                selectedSystem.trigger("change:diagramId");
            }
            else if (selectedSystem.get("optionExpression") !== preOptionExpression) {
                selectedSystem.trigger("change:optionExpression");
            }
            else {
                this.highlightObject(content);
            }
        }, openSystem: function (content,
                selectedSystem,
                resetViewIfSystemIdChanged,
                resolveDynamicConfigurationMode)
        {
            var preOptionExpression,
                    preDiagramId,
                    preSystemId,
                    diagramName,
                    diagram,
                    systemModel,
                    system,
                    optionExpression = getOptionExp(content),
                    idAttribute = getSystemId(content, selectedSystem);
            system = mentor.publisher.project.getObjectById(idAttribute);
            if (!system || !system.getObjectById) {
                system = mentor.publisher.project.getObjectById(content.systemId);
            }
            if (content.diagramId) {
                diagram = system.getObjectById(content.diagramId);
            }
            else {
                diagram = getFirstDiagramToShow(system);
                if (diagram.path && diagram.path.indexOf(".svg") < 0) {
                    diagram.type = mentor.publisher.contentType.SYSTEM_REPORT;
                    mentor.publisher.fileDisplayHandler.display(diagram);
                    return diagram;
                }
            }
            var designMainText = system.mainText;
            if (system.withoutTranslation) {
                designMainText = system.withoutTranslation.mainText;
            }
            diagramName = designMainText + (diagram.getName() ? ", " + diagram.getName() : "");
            var computeTitle = function (lang) {
                return Utils.handleTranslation(designMainText,true) +
                        (diagram.getName() ? ", " + Utils.handleTranslation(diagram.getName(),true) : "");
            };
            preDiagramId = selectedSystem.get("diagramId");
            preOptionExpression = selectedSystem.get("optionExpression");
            preSystemId = selectedSystem.get("systemId");
            //the system id that is passed should be system id and not id in case of dynamic mode
            //in case of normal mode, the content id and system id would be the same, in which case it should be fine
            // to pass any one of it as system id
            selectedSystem.set({
                        systemId: system.systemId,
                        diagramId: diagram.id,
                        path: diagram.path,
                        type: mentor.publisher.contentType.SYSTEM_SVG,
                        title: diagramName,
                        optionExpression: optionExpression,
                        objectId: content.objectId,
                        computeTitle: computeTitle
                    },
                    {silent: true});

            var newSystem = resetViewIfSystemIdChanged(preSystemId);
            if (!newSystem) {
                mentor.publisher.fileDisplayHandler.display(content);
                return;
            }
            this.openDiagram(selectedSystem, preDiagramId, preOptionExpression, content);

            if (!(window.opener && window.opener.mentor) &&
                    resolveDynamicConfigurationMode(system, optionExpression)) {
                return;
            }

            return system;

        },
        getFirstDiagramOrReportToOpen: function (systemId)
        {
            var system = mentor.publisher.project.getObjectById(systemId);
            return getFirstDiagramToShow(system);
        }
    }

});