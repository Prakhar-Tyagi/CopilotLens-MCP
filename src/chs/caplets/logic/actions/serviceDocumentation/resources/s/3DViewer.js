// 3DViewer.js
// Copyright Dassault Systemes

/*PRIVATE*/
var PLUGIN_CLSID = "5ABD296B-F8A0-436C-B2F7-B19170C43D28";
/*PRIVATE*/
var PLUGIN_MIME_TYPE = "application/x-3dxmlplugin";

// A map to identify objects parameters
/*PRIVATE*/
var ID2PARAMS = new Object();

/* Contianer height*/
var containerHeight = null;
/* Contianer width*/
var containerWIdth = null;

var threeContainerId = null;
var obejctId = null;
var tableId = null;

// Check if the browser is Firefox
// @return true if the browser is Firefox
/*PRIVATE*/
function isFirefox() {
    if (typeof InstallTrigger != "undefined") {
        return true;
    }
    else {
        return false;
    }
}

// Get the url of an image
// @param imgName the image name
// @return the url of the image name, located in image subfolder
/*PRIVATE*/
function getImageUrl(imgName) {
    var url = document.URL;
    var lastBackslash = url.lastIndexOf("\\");
    if (lastBackslash > 0) {
        // IE case with file://
        url = url.substr(0, lastBackslash);
        url = url + "\\img\\" + imgName;
    }
    else {
        var lastSlash = url.lastIndexOf("/");
        url = url.substr(0, lastSlash);
        url = url + "/img/" + imgName;
    }
    return Utils.prepareFilePath(url);
}

// Callback method on viewer ready
// @param viewerId the id of the main viewer
/*PRIVATE*/
function onViewerReady(viewerId) {
    var params = parent.ID2PARAMS[viewerId];
    if (params != undefined) {
        if (params["ID_TOOLBAR_ADVANCED_VIEW"] == undefined) {
            setCustomCommandCallback(viewerId, onCustomCommand);
            params["ID_TOOLBAR_ADVANCED_VIEW"] = createToolbar(viewerId, "Advanced View");
            params["ID_COMMAND_ADVANCED_VIEW_PART"] =
                createCommand(viewerId, params["ID_TOOLBAR_ADVANCED_VIEW"], "check", "Part Viewer", "Show Part Viewer",
                    "Display the part viewer", getImageUrl("PartViewer.png"));
            params["ID_COMMAND_ADVANCED_VIEW_DRAWING"] =
                createCommand(viewerId, params["ID_TOOLBAR_ADVANCED_VIEW"], "check", "Drawing Viewer",
                    "Show Drawing Viewer", "Display the drawing viewer", getImageUrl("DrawingViewer.png"));
            params["ID_COMMAND_ADVANCED_VIEW_MAGNIFIER"] =
                createCommand(viewerId, params["ID_TOOLBAR_ADVANCED_VIEW"], "check", "Magnifier", "Show Magnifier",
                    "Display the magnifier", getImageUrl("Magnifier.png"));
        }
    }
}

// Callback method on custom command
// @param viewerId the id of the main viewer
// @param eventCategory the event category
// @param eventName the event name
// @param eventSender the event sender
// @param eventParameters the event parameters
/*PRIVATE*/
function onCustomCommand(viewerId, eventCategory, eventName, eventSender, eventParameters) {
    var params = parent.ID2PARAMS[viewerId];
    if (params != undefined && eventParameters.Count > 0) {
        var commandId = eventParameters.Item(1);
        if (commandId == params["ID_COMMAND_ADVANCED_VIEW_PART"]) {
            if (eventName == "check") {
                params["DISPLAY_PARTVIEWER"] = true;
            }
            else {
                params["DISPLAY_PARTVIEWER"] = false;
            }
        }
        else if (commandId == params["ID_COMMAND_ADVANCED_VIEW_DRAWING"]) {
            if (eventName == "check") {
                params["DISPLAY_DRAWINGVIEWER"] = true;
            }
            else {
                params["DISPLAY_DRAWINGVIEWER"] = false;
            }
        }
        else if (commandId == params["ID_COMMAND_ADVANCED_VIEW_MAGNIFIER"]) {
            if (eventName == "check") {
                params["DISPLAY_MAGNIFIER"] = true;
            }
            else {
                params["DISPLAY_MAGNIFIER"] = false;
            }
        }
        updateViewerLayout(viewerId);
    }
}

// Called when the layout needs to be refreshed
// @param viewerId the id of the main viewer
/*PRIVATE*/
function updateViewerLayout(viewerId) {
    var params = parent.ID2PARAMS[viewerId];
    if (params != undefined) {
        if (params["DISPLAY_PARTVIEWER"] || params["DISPLAY_DRAWINGVIEWER"] || params["DISPLAY_MAGNIFIER"]) {
            document.getElementById("ds_viewer_" + viewerId).style.width = "66%";
        }
        else {
            document.getElementById("ds_viewer_" + viewerId).style.width = "100%";
        }

        //document.getElementById("ds_viewer_" + viewerId).style.width = containerWidth;
        var nSV = 0;
        if (params["DISPLAY_PARTVIEWER"]) {
            nSV++;
        }
        if (params["DISPLAY_DRAWINGVIEWER"]) {
            nSV++;
        }
        if (params["DISPLAY_MAGNIFIER"]) {
            nSV++;
        }
        var svHeight = (nSV == 3 ? "33%" : (nSV == 2 ? "50%" : "100%"));

        var ds_row_part = document.getElementById("ds_row_part_" + viewerId);
        var ds_part = document.getElementById("ds_part_" + viewerId);
        if (params["DISPLAY_PARTVIEWER"]) {
            if (ds_part == undefined) {
                ds_part = ds_row_part.insertCell(-1);
                ds_part.id = "ds_part_" + viewerId;
                ds_part.style.height = svHeight;
                ds_part.style.border = "1px";
                ds_part.style.borderColor = "#CFCFE6";
                ds_part.style.borderStyle = "solid";
                ds_part.style.padding = 0;

                createPartViewer(ds_part.id, viewerId);
                setSelectionSynchronizationON(ds_part.id);
                setViewpointSynchronizationON(ds_part.id);
            }
            else {
                ds_part.style.height = svHeight;
            }
        }
        else if (ds_part != undefined) {
            ds_row_part.deleteCell(-1);
        }

        var ds_row_drawing = document.getElementById("ds_row_drawing_" + viewerId);
        var ds_drawing = document.getElementById("ds_drawing_" + viewerId);
        if (params["DISPLAY_DRAWINGVIEWER"]) {
            if (ds_drawing == undefined) {
                ds_drawing = ds_row_drawing.insertCell(-1);
                ds_drawing.id = "ds_drawing_" + viewerId;
                ds_drawing.style.height = svHeight;
                ds_drawing.style.border = "1px";
                ds_drawing.style.borderColor = "#CFCFE6";
                ds_drawing.style.borderStyle = "solid";
                ds_drawing.style.padding = 0;

                createDrawingViewer(ds_drawing.id, viewerId);
                setLabelsOFF(ds_drawing.id);
            }
            else {
                ds_drawing.style.height = svHeight;
            }
        }
        else if (ds_drawing != undefined) {
            ds_row_drawing.deleteCell(-1);
        }

        var ds_row_magnifier = document.getElementById("ds_row_magnifier_" + viewerId);
        var ds_magnifier = document.getElementById("ds_magnifier_" + viewerId);
        if (params["DISPLAY_MAGNIFIER"]) {
            if (ds_magnifier == undefined) {
                ds_magnifier = ds_row_magnifier.insertCell(-1);
                ds_magnifier.id = "ds_magnifier_" + viewerId;
                ds_magnifier.style.height = svHeight;
                ds_magnifier.style.border = "1px";
                ds_magnifier.style.borderColor = "#CFCFE6";
                ds_magnifier.style.borderStyle = "solid";
                ds_magnifier.style.padding = 0;

                activateMagnifier(ds_magnifier.id, viewerId);
            }
            else {
                ds_magnifier.style.height = svHeight;
            }
        }
        else if (ds_magnifier != undefined) {
            deactivateMagnifier(ds_magnifier.id, viewerId);
            ds_row_magnifier.deleteCell(-1);
        }
    }
}

// Create a simple viewer in the browser
// @param containerId the id of the container which will contains the viewer object
// @param viewerId the id of the viewer to be created
// @param documentUrl the url of the document to load in the viewer
function createViewer(containerId, viewerId, documentUrl) {
    var params = ID2PARAMS[viewerId];
    if (params == undefined) {
        params = new Object();
        ID2PARAMS[viewerId] = params;
    }

    var useScripting = params["USE_SCRIPTING"];

    var containerObject = document.getElementById(containerId);

    var param = document.createElement("param");
    param.name = "DocumentFile";
    param.value = documentUrl;

    if (isFirefox()) {
        // Firefox case
        var viewerObject = document.createElement("object");
        viewerObject.id = viewerId;
        obejctId = viewerId;
        viewerObject.width = containerWidth;
        viewerObject.height = containerHeight;
        viewerObject.appendChild(param);
        viewerObject.type = PLUGIN_MIME_TYPE;
        containerObject.appendChild(viewerObject);
    }
    else {
        // Internet Explorer case
        var objectNode = document.createElement("object");
        objectNode.appendChild(param); // parameters must be appended before setting the classid
        objectNode.id = viewerId;
        obejctId = viewerId;
        objectNode.width = containerWidth;
        objectNode.height = containerHeight;
        objectNode.classid = "clsid:" + PLUGIN_CLSID;
        containerObject.appendChild(objectNode);
    }

    if (useScripting) {
        createScriptingBehavior(containerObject, viewerId);
    }
}

// Create an advanced viewer in the browser
// @param containerId the id of the container which will contains the viewer object
// @param viewerId the id of the viewer to be created
// @param documentUrl the url of the document to load in the viewer
function createAdvancedViewer(containerId, viewerId, documentUrl) {
    containerHeight = $($('#' + containerId).parent()).height();
    containerWidth = $($('#' + containerId).parent()).width();

    threeContainerId = containerId;
    var params = ID2PARAMS[viewerId];

    if (params == undefined) {
        params = new Object();
        ID2PARAMS[viewerId] = params;
    }

    ID2PARAMS[viewerId]["DISPLAY_PARTVIEWER"] = false;
    ID2PARAMS[viewerId]["DISPLAY_DRAWINGVIEWER"] = false;
    ID2PARAMS[viewerId]["DISPLAY_MAGNIFIER"] = false;

    var container = document.getElementById(containerId);
    if (container != undefined) {
        var ds_table = document.createElement("table");
        ds_table.id = "ds_table_" + viewerId;
        tableId = ds_table.id;
        ds_table.cellspacing = 0;
        ds_table.cellpadding = 0;
        ds_table.style.width = containerWidth;
        ds_table.style.height = containerHeight;
        ds_table.style.border = 0;
        ds_table.style.borderCollapse = "collapse";

        var ds_row_part = ds_table.insertRow(-1);
        ds_row_part.id = "ds_row_part_" + viewerId;

        var ds_row_drawing = ds_table.insertRow(-1);
        ds_row_drawing.id = "ds_row_drawing_" + viewerId;

        var ds_row_magnifier = ds_table.insertRow(-1);
        ds_row_magnifier.id = "ds_row_magnifier_" + viewerId;

        var ds_viewer = ds_row_part.insertCell(-1);
        ds_viewer.id = "ds_viewer_" + viewerId;
        ds_viewer.rowSpan = "3";
        ds_viewer.style.width = '100%';
        ds_viewer.style.border = "0px";
        ds_viewer.style.borderColor = "#CFCFE6";
        ds_viewer.style.borderStyle = "solid";
        ds_viewer.style.padding = 0;

        container.appendChild(ds_table);

        ID2PARAMS[viewerId]["USE_SCRIPTING"] = true;
        createViewer(ds_viewer.id, viewerId, documentUrl);

        setReadyCallback(viewerId, onViewerReady);
    }
}


