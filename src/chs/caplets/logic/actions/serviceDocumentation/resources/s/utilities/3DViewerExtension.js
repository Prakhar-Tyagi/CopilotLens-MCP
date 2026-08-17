// 3DViewerExtension.js
// Copyright Dassault Systemes

// The map which associates a viewer id to its scripting object
/*PRIVATE*/
var mapEWVIA = new Object();
// The map which associates a viewer id to its selection object
/*PRIVATE*/
var mapSelection = new Object();
// The map which associates a part viewer id to its scripting object
/*PRIVATE*/
var mapPartViewer = new Object();
// The map which associates a drawing viewer id to its scripting object
/*PRIVATE*/
var mapDrawingViewer = new Object();

// The map which associated a viewer id to its ready callback function
/*PRIVATE*/
var mapReadyCallback = new Object();
// The map which associated a viewer id to its selection callback function
/*PRIVATE*/
var mapSelectionCallback = new Object();
// The map which associated a viewer id to its mouse callback function
/*PRIVATE*/
var mapMousedCallback = new Object();
// The map which associated a viewer id to its keyboard callback function
/*PRIVATE*/
var mapKeyboardCallback = new Object();
// The map which associated a viewer id to its custom command callback function
/*PRIVATE*/
var mapCustomCommandCallback = new Object();

// Create the viewer extension with scripting capabilities
// @param containerObject the DOM container object
// @param viewerId the id of the viewer to script
/*PRIVATE*/
function createScriptingBehavior(containerObject, viewerId) {
    var scriptEvent = document.createElement("script");
    scriptEvent.type = "text/javascript";
    if (isFirefox()) {
        scriptEvent.text = "function " + viewerId +
            "_OnEvent(EventCategory, EventName, EventSender, EventParameters) { onViewerEvent('" + viewerId +
            "', EventCategory, EventName, EventSender, EventParameters); }";
    }
    else {
        scriptEvent.text = "function " + viewerId +
            "::OnEvent(EventCategory, EventName, EventSender, EventParameters) { onViewerEvent('" + viewerId +
            "', EventCategory, EventName, EventSender, EventParameters); }";
    }
    containerObject.appendChild(scriptEvent);
}

// Callback method on viewer event
// @param viewerId the id of the viewer who is scripted
// @param eventCategory the event category
// @param eventName the event name
// @param eventSender the event sender
// @param eventParameters the event parameters
/*PRIVATE*/
function onViewerEvent(viewerId, eventCategory, eventName, eventSender, eventParameters) {
    if (eventCategory == "Application") {
        if (eventName == "Ready") {
            mapEWVIA[viewerId] = eventSender;
            mapSelection[viewerId] = eventSender.ActiveEditor.Selection;
            if (mapReadyCallback[viewerId] != undefined) {
                mapReadyCallback[viewerId](viewerId);
            }
        }
    }
    else if (eventCategory == "Selection") {
        if (mapSelectionCallback[viewerId] != undefined) {
            mapSelectionCallback[viewerId](viewerId, eventCategory, eventName, eventSender, eventParameters);
        }
    }
    else if (eventCategory == "Mouse") {
        if (mapMouseCallback[viewerId] != undefined) {
            mapMouseCallback[viewerId](viewerId, eventCategory, eventName, eventSender, eventParameters);
        }
    }
    else if (eventCategory == "Keyboard") {
        if (mapKeyboardCallback[viewerId] != undefined) {
            mapKeyboardCallback[viewerId](viewerId, eventCategory, eventName, eventSender, eventParameters);
        }
    }
    else if (eventCategory == "Command") {
        if (mapCustomCommandCallback[viewerId] != undefined) {
            mapCustomCommandCallback[viewerId](viewerId, eventCategory, eventName, eventSender, eventParameters);
        }
    }
}

// Set the ready callback on a viewer
// @param viewerId the id of the viewer
// @param functionCB the callback
function setReadyCallback(viewerId, functionCB) {
    mapReadyCallback[viewerId] = functionCB;
}

// Set the selection callback on a viewer
// @param viewerId the id of the viewer
// @param functionCB the callback
function setSelectionCallback(viewerId, functionCB) {
    mapSelectionCallback[viewerId] = functionCB;
}

// Set the mouse callback on a viewer
// @param viewerId the id of the viewer
// @param functionCB the callback
function setMouseCallback(viewerId, functionCB) {
    mapMouseCallback[viewerId] = functionCB;
}

// Set the keyboard callback on a viewer
// @param viewerId the id of the viewer
// @param functionCB the callback
function setKeyboardCallback(viewerId, functionCB) {
    mapKeyboardCallback[viewerId] = functionCB;
}

// Set the custom command callback on a viewer
// @param viewerId the id of the viewer
// @param functionCB the callback
function setCustomCommandCallback(viewerId, functionCB) {
    mapCustomCommandCallback[viewerId] = functionCB;
}

// Create a sub viewer instance in the browser
// @param containerId the id of the container which will contain the sub viewer
// @param viewerId the id of the viewer for which a sub viewer is created
// @param viewerType the sub viewer type, may be "PARTVIEWER", "DRAWINGVIEWER", "MAGNIFIER"
/*PRIVATE*/
function createSubViewer(containerId, viewerId, viewerType) {
    var nodeId = containerId + "_" + viewerType;
    var node = document.getElementById(nodeId);
    if (node == undefined) {
        var container = document.getElementById(containerId);

        var param = document.createElement("param");
        if (PLUGIN_MIME_TYPE == "application/x-3dxmlplugin") {
            param.name = "DocumentFile";
        }
        else {
            param.name = "PLMProperties";
        }
        param.value = viewerType;

        node = document.createElement("object");
        node.appendChild(param); // In IE, parameters must be appended before setting the type
        node.id = nodeId;
        node.width = "100%";
        node.height = "100%";
        node.type = PLUGIN_MIME_TYPE;
        //node.classid = "clsid:" + PLUGIN_CLSID;
        container.appendChild(node);
    }

    if (viewerType == "PARTVIEWER") {
        mapPartViewer[nodeId] = mapEWVIA[viewerId].GetPartViewer(node);
    }
    else if (viewerType == "DRAWINGVIEWER") {
        mapDrawingViewer[nodeId] = mapEWVIA[viewerId].GetDrawingViewer(node);
    }
    else if (viewerType == "MAGNIFIER") {
        mapEWVIA[viewerId].ActivateMagnifier(node);
    }
}

// Create a part viewer instance in the browser
// @param containerId the id of the container which will contain the part viewer
// @param viewerId the id of the viewer for which a part viewer is created
function createPartViewer(containerId, viewerId) {
    createSubViewer(containerId, viewerId, "PARTVIEWER");
}

// Create a drawing viewer instance in the browser
// @param containerId the id of the container which will contain the drawing viewer
// @param viewerId the id of the viewer for which a drawing viewer is created
function createDrawingViewer(containerId, viewerId) {
    createSubViewer(containerId, viewerId, "DRAWINGVIEWER");
}

// Activate a magnifier instance in the browser
// @param containerId the id of the container which will contain the magnifier
// @param viewerId the id of the viewer for which a magnifier is created
function activateMagnifier(containerId, viewerId) {
    createSubViewer(containerId, viewerId, "MAGNIFIER");
}

// Deactivate a magnifier instance in the browser
// @param containerId the id of the container which will contain the magnifier
// @param viewerId the id of the viewer for which a magnifier is created
function deactivateMagnifier(containerId, viewerId) {
    var node = document.getElementById(containerId + "_MAGNIFIER");
    if (node != undefined && mapEWVIA[viewerId] != undefined) {
        mapEWVIA[viewerId].DeactivateMagnifier(node);
    }
}

// Activate the synchronization on selection behavior of a part viewer
// @param idPartViewer the id of the container which contains the part viewer
function setSelectionSynchronizationON(idPartViewer) {
    var part_viewer = mapPartViewer[idPartViewer + "_PARTVIEWER"];
    if (part_viewer != undefined) {
        part_viewer.SelectionSynchronization = true;
    }
}

// Deactivate the synchronization on selection behavior of a part viewer
// @param idPartViewer the id of the container which contains the part viewer
function setSelectionSynchronizationOFF(idPartViewer) {
    var part_viewer = mapPartViewer[idPartViewer + "_PARTVIEWER"];
    if (part_viewer != undefined) {
        part_viewer.SelectionSynchronization = false;
    }
}

// Activate the synchronization on viewpoint behavior of a part viewer
// @param idPartViewer the id of the container which contains the part viewer
function setViewpointSynchronizationON(idPartViewer) {
    var part_viewer = mapPartViewer[idPartViewer + "_PARTVIEWER"];
    if (part_viewer != undefined) {
        part_viewer.ViewpointSynchronization = true;
    }
}

// Deactivate the synchronization on viewpoint behavior of a part viewer
// @param idPartViewer the id of the container which contains the part viewer
function setViewpointSynchronizationOFF(idPartViewer) {
    var part_viewer = mapPartViewer[idPartViewer + "_PARTVIEWER"];
    if (part_viewer != undefined) {
        part_viewer.ViewpointSynchronization = false;
    }
}

// Activate a predefined view in the main viewer or in a drawing viewer
// @param id the id of the viewer or the id of the container which contains the drawing viewer
// @param viewName the predefined view name ("Iso", "Front", "Back", "Left", "Right", "Top", "Bottom")
function setPredefinedView(id, viewName) {
    if (mapEWVIA[id] != undefined) {
        var service = mapEWVIA[id].GetSessionService("PlayerService");
        service.SetPredefinedView(viewName);
    }
    else {
        var drawing_viewer = mapDrawingViewer[id + "_DRAWINGVIEWER"];
        if (drawing_viewer != undefined) {
            drawing_viewer.SetPredefinedView(viewName);
        }
    }
}

// Set the background color in a drawing viewer
// @param idDrawingViewer the id of the container which contains the drawing viewer
// @param colorValue the color value (as a string like "0xFF7777")
function setBackgroundColor(idDrawingViewer, colorValue) {
    var drawing_viewer = mapDrawingViewer[idDrawingViewer + "_DRAWINGVIEWER"];
    if (drawing_viewer != undefined) {
        drawing_viewer.SetBackgroundColour(parseInt(colorValue));
    }
}

// Set the explode factor in a drawing viewer
// @param idDrawingViewer the id of the container which contains the drawing viewer
// @param explodeFactor the explode factor
function setExplodeFactor(idDrawingViewer, explodeFactor) {
    var drawing_viewer = mapDrawingViewer[idDrawingViewer + "_DRAWINGVIEWER"];
    if (drawing_viewer != undefined) {
        drawing_viewer.SetExplodeFactor(parseInt(explodeFactor));
    }
}

// Activate the transparency selection in the main viewer
// @param viewerId the id of the main viewer
// @param transparencyLevel (optional) the transparency level (by default 20)
function setTransparencyON(viewerId, transparencyLevel) {
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        service.SetTransparencyMode(1);
        service.SetTransparencyLevel(transparencyLevel == undefined ? 20 : transparencyLevel);
    }
}

// Deactivate the transparency selection in the main viewer
// @param viewerId the id of the main viewer
function setTransparencyOFF(viewerId) {
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        service.SetTransparencyMode(0);
    }
}

// Reframe the main viewer on all its content
// @param viewerId the id of the main viewer
function reframe(viewerId) {
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        service.Reframe();
    }
}

// Reframe the main viewer on its selected elements
// @param viewerId the id of the main viewer
function reframeOnSelection(viewerId) {
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        service.ReframeOnSelection();
    }
}

// Change the document of the main viewer
// @param viewerId the id of the main viewer
// @param url the url of the new document
function setDocument(viewerId, url) {
    var viewer = document.getElementById(viewerId);
    if (viewer != undefined) {
        viewer.SetPLMProperty("DocumentFile", url);
    }
}

// Select an object in the main viewer
// @param viewerId the id of the main viewer
// @param objectId the id of the object to select
function select(viewerId, objectId) {
    if (mapEWVIA[viewerId] != undefined && mapSelection[viewerId] != undefined) {
        mapSelection[viewerId].Add(mapEWVIA[viewerId].CreateReferenceFromIDPath(objectId));
    }
}

// Unselect an object from the main viewer
// @param viewerId the id of the main viewer
// @param objectId the id of the object to unselect
function unselect(viewerId, objectId) {
    if (mapEWVIA[viewerId] != undefined && mapSelection[viewerId] != undefined) {
        mapSelection[viewerId].Remove(mapEWVIA[viewerId].CreateReferenceFromIDPath(objectId));
    }
}

// Show a specific part of the main viewer in a part viewer
// @param viewerId the id of the main viewer
// @param idPartViewer the id of container which contains the part viewer into which the part will be displayed
// @param partId the id of the part to display
function showPart(viewerId, idPartViewer, partId) {
    var part_viewer = mapPartViewer[idPartViewer + "_PARTVIEWER"];
    if (mapEWVIA[viewerId] != undefined && part_viewer != undefined) {
        part_viewer.Show(mapEWVIA[viewerId].CreateReferenceFromIDPath(partId));
    }
}

// Activate the labels in a drawing viewer of the main viewer
// @param idDrawingViewer the id of the container which contains the drawing viewer to be modified
function setLabelsON(idDrawingViewer) {
    var drawing_viewer = mapDrawingViewer[idDrawingViewer + "_DRAWINGVIEWER"];
    if (drawing_viewer != undefined) {
        drawing_viewer.SetLabelMode("StdLabel");
    }
}

// Deactivate the labels in a drawing viewer of the main viewer
// @param idDrawingViewer the id of the container which contains the drawing viewer to be modified
function setLabelsOFF(idDrawingViewer) {
    var drawing_viewer = mapDrawingViewer[idDrawingViewer + "_DRAWINGVIEWER"];
    if (drawing_viewer != undefined) {
        drawing_viewer.SetLabelMode("NoLabel");
    }
}

// Get the current language used in the main viewer
// @param viewerId the id of the main viewer
// @return the current language ("English", "French"...)
function getCurrentLanguage(viewerId) {
    var currentLanguage = "";
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        currentLanguage = service.GetCurrentLanguage();
    }
    return currentLanguage;
}

// Create a new custom toolbar
// @param viewerId the id of the main viewer
// @param nlsName the toolbar name (you must provide the NLS name, according to getCurrentLanguage)
// @return the toolbar id
function createToolbar(viewerId, nlsName) {
    var toolbarId = -1;
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        toolbarId = service.CreateToolbar(nlsName);
    }
    return toolbarId;
}

// Create a new custom command
// @param viewerId the id of the main viewer
// @param toolbarId the id of the toolbar (as returned by createToolbar)
// @param buttonType the type of the command (may be "push" or "check")
// @param nlsName the command name (you must provide the NLS name, according to getCurrentLanguage)
// @param nlsShortHelp the short help associated to the command (you must provide the NLS short help, according to getCurrentLanguage)
// @param nlsLongHelp the long help associated to the command (you must provide the NLS long help, according to getCurrentLanguage)
// @param iconUrl the full url to the icon to be displayed
// @return the command id
function createCommand(viewerId, toolbarId, buttonType, nlsName, nlsShortHelp, nlsLongHelp, iconUrl) {
    var commandId = -1;
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        commandId = service.CreateCommand(toolbarId, buttonType, nlsName, nlsShortHelp, nlsLongHelp, iconUrl);
    }
    return commandId;
}

// Remove a custom command
// @param viewerId the id of the main viewer
// @param commandId the id of the command to remove (as returned by createCommand) (it can also be the id of a toolbar, as returned by createToolbar)
function removeCommand(viewerId, commandId) {
    if (mapEWVIA[viewerId] != undefined) {
        var service = mapEWVIA[viewerId].GetSessionService("PlayerService");
        service.RemoveCommand(commandId);
    }
}

// Create a scriptable main viewer in the browser
// @param containerId the id of the container which will contains the viewer object
// @param viewerId the id of the viewer to be created
// @param serverUrl the url of the server which provides installation packages
// @param documentUrl the url of the document to load in the viewer
function createScriptableViewer(containerId, viewerId, serverUrl, documentUrl) {
    var params = ID2PARAMS[viewerId];
    if (params == undefined) {
        params = new Object();
        ID2PARAMS[viewerId] = params;
    }
    ID2PARAMS[viewerId]["USE_SCRIPTING"] = true;
    createViewer(containerId, viewerId, serverUrl, documentUrl);
}
