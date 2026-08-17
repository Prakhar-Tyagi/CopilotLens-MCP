/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */
/*global Utils, mentor*/
//$.namespace("mentor.publisher.popoutHandler");
/**
 * The 'events' object conatins events that can be generated and consumed in publisher viewer.
 * each event may expect some data in order to complete itself.
 *
 * How to attach aa event listerner for an event type :
 *
 * publisher.mentor.eventDispatcher.attachEventListener(p.events.VIN_FILTER_APPLIED, callbackFunction);
 *
 * How to trigger aa event type :
 *
 * mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.VIN_FILTER_APPLIED, data);
 *
 *
 *
 * @type {Object}
 */
mentor.publisher.events = {
    /**
     * The event is generated when VIN filter is applied to navigation panel.
     * generated event conatins 'vinOptions'
     */
    VIN_FILTER_APPLIED: "vinFilterApplied",
    /**
     * The event is generated when configurayin filter is applied to navigation panel.
     */
    CONFIGURATION_FILTER_APPLIED: "configurationFilterApplied",
    ITEM_CLICKED_IN_DYNAMIC_MODE: "clickedindynamicmode",
    /**
     * The event is generated when language is change in the viewer
     */
    LANGUAGE_FILTER_APPLIED: "languageFilterApplied",
    /**
     * it opens a location view diagram
     * when generated it contains path of location view SVG
     */
    OPEN_RENDEREDSIGNAL: "openRenderedSignal",
    ZOOM_TRIGGERED: "zoom_triggered",
    GROUND_PATH_TRACE: "tracegroundpath",
    RESET: "RESET",
    /**
     * This event is used to display design object's information, such as attributes, xrefs, 2d location views etc., in
     * a popover. the event requires following data to perform its task
     * {systemId : "designId", id:"objectUID"}
     *
     */
    OPEN_OBJECT_POPUP: "OPEN_OBJECT_POPUP",
    CLOSE_POPOVER: "CLOSE_POPOVER",
    CLOSE_CONFIG_POPOVER: "CLOSE_CONFIG_POPOVER",
    DETACH_CLOSE_POPOVER_LISTENER: "DETACH_CLOSE_POPOVER_LISTENER",
    ATTACH_CLOSE_POPOVER_LISTENER: "ATTACH_CLOSE_POPOVER_LISTENER",
    APPLY_CONFIGURATION_FILTER_ON_POPOVER: "applyconfigurationfilteronpopover",
    OPEN_LANGUAGES_POPUP: "OPEN_LANGUAGES_POPUP",
    OPEN_PRINT_POPUP: "OPEN_PRINT_POPUP",
    SHOW_PANELS_TO_PRINT_POPUP: "SHOW_PANELS_TO_PRINT_POPUP",
    SHOW_VIN_LOGIN_POPUP: "SHOW_VIN_LOGIN_POPUP",
    ALT_CLICK_TRIGGERED: "ALT_CLICK_TRIGGERED",
    CLICKED_IN_SIGNAL_TRACE_VIEW: "CLICKED_IN_SIGNAL_TRACE_VIEW",
    UPDATE_SIGNAL_TRACER: "UPDATE_SIGNAL_TRACER",
    HIGHLIGHT_TRACED_SIGNAL: "HIGHLIGHT_TRACED_SIGNAL",
    SHOW_POP_OVER_2D_VIEW: "SHOW_POP_OVER_2D_VIEW",
    HIGHLIGHT_OBJECT_ACROSS_WINDOWS: "HIGHLIGHT_OBJECT_ACROSS_WINDOWS",
    SHOW_TOOL_TIP: "SHOW_TOOL_TIP",
    REMOVE_TOOL_TIP: "REMOVE_TOOL_TIP",
    RESIZE_SVG: "RESIZE_SVG",
    REPOSITION_SVG_SLIDER: "REPOSITION_SVG_SLIDER",
    SHOW_SLIDER: "SHOW_SLIDER",
    ANY: "any",
    CLOSE_MODAL: "CLOSE_MODAL"
};

mentor.publisher.contentType = {
    GLOBAL_REPORT: "projectReport",
    SYSTEM_REPORT: "systemReport",
    SYSTEM_SVG: "systemSVG",
    CUSTOM_VIEW: "customView",
    CONNECTORS: "connectors",
    DEVICES: "devices",
    WIRES: "wires",
    SPLICES: "splices",
    MULTICORES: "multicores",
    SIGNALS: "signals",
    SIGNAL: "signal",
    LOCATION_VIEWS: "locationviews",
    FAULT_CODE: "faultcode",
    HARNESS: "harness",
    CONNECTOR_FACE_VIEW: "connectorFaceView",
    THREE_D_XML: "3dXML",
    JT_3D: "JT",
    JT_3D_MODEL: "JT 3D Model",
    RA_3D: "RA",
    RA_3D_MODEL: "Rapid Author 3D Catalog Model",
    RENDERED_SVG: "RENDERED_SVG",
    INLINES: "inlines",
    GROUNDS: "grounds",
    GLOBAL_GROUND_REPORT: "globalreports",
    CAPITAL_REPORT: "capitalreport",
    DIAGNOSTIC: "diagnostic",
    HARNESS_LAYOUT_DIAGRAM: "harnessLayoutDiagram",
    HARNESS_LAYOUT_REPORT: "harnessLayoutReport",
    OBJECT_CROSS_REF: "OBJECT_CROSS_REF",
    DESIGN_OBJECT: "design-object",
    PDF_OBJECT: "pdf-textt",
    OBJECT_REPORT: "systemObjectReport",
    OLD_DESIGN_REVISION: "oldDesignRevision",
    NEW_DESIGN_REVISION: "newDesignRevision",
    TROUBLESHOOT: "troubleshoot",
    COMMON_FAULT_CODE: "commonFaultCodes",
    FAULT_OBJECT_TABLE: "faultObjectTable"
};

mentor.publisher.documentCategory = {
    INFORMATION: "introduction-page",
    WIRES: "Wires",
    NETS: "Nets",
    CONNECTORS: "connectors",
    DEVICES: "devices",
    SPLICES: "splices",
    MULTICORES: "multicores",
    INLINES: "inlines",
    GROUNDS: "grounds",
    LOCATION_VIEWS: "location-views",
    FAULT_CODE: "faultcode",
    HARNESS: "harness",
    DIAGNOSTICS: "diagnostics",
    GLOBAL_GROUND_REPORT: "globalreports",
    CAPITAL_REPORT: "capitalreport",
    SYSTEMS: "systems",
    HARNESS_LAYOUT_DIAGRAM: "harness-layouts",
    REPORTS: "reports",
    DIAGRAMS: "diagrams",
    TROUBLESHOOT: "troubleshoot",
    COMMON_FAULT_CODE: "commonFaultCodes",
    CUSTOM_VIEW: "customView",
    THREE_D_VIEW: "threeDView"
};

mentor.publisher.toolBarElementCSSSelectors = {
    relatedDataBtn: ".related-data-button",
    reportBtn: ".reports-button",
    diagramBtn: ".diagrams-button",
    toolBar: ".toolbar",
    regenerateBtn: ".regenerateBtn",
    toolBarHeader: ".component-label",
    closeBtn: ".closeBtn",
    maximizeBtn: ".maximizeBtn",
    restoreBtn: ".restoreBtn",
    printBtn: ".printBtn",
    languageBtn: ".languageBtn",
    expandCollapseNavPanel: ".collapseExpandNavigation",
    popOutBtn: ".popOutBtn"
};
