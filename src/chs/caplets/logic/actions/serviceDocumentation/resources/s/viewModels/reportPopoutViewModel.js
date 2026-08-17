/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["currentPackage"], function(currentPackage){
    var createCapitalReportVM, createGlobalHarnessReportVM, createSystemReportVM, createGenericReportVM;

    createCapitalReportVM =function(report) {
        return {
            mainText: report.get("mainText"),
            path: report.get("path"),
            projectId: currentPackage.get("id"),
            type: mentor.publisher.contentType.CAPITAL_REPORT
        };
    };

    createGlobalHarnessReportVM =function(report) {
        return {
            mainText: report.get("mainText"),
            projectId: currentPackage.get("id"),
            type: mentor.publisher.contentType.HARNESS
        };
    };

    createSystemReportVM =function(model) {
        return {
            systemId: model.get("systemId"),
            reportId: model.get("reportId"),
            projectId: currentPackage.get("id"),
            type: mentor.publisher.contentType.SYSTEM_REPORT
        };
    };

    createGenericReportVM =function(model) {
        return {
            systemId: model.get("systemId"),
            path: model.get("reportPath"),
            projectId: currentPackage.get("id"),
            mainText: model.get("reportTitle"),
            type: mentor.publisher.contentType.OBJECT_REPORT
        };
    };

    return function (model)
    {
        var report = model.get("harness");
        if (report && report.get("type") === mentor.publisher.contentType.CAPITAL_REPORT) {
            return createCapitalReportVM(report);
        }
        else if (report) {
            return createGlobalHarnessReportVM(report);
        }
        else if (model.get("reportId")) {
            return createSystemReportVM(model);
        }
        else {
            return createGenericReportVM(model);
        }
    };
});