/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
var $ = window.parent.$;
var handler = window.parent.mentor.publisher.rapidAuthorCatalogPanel;
var m = window.parent.mentor;
m.publisher.solo = Cortona3DSolo;

Cortona3DSolo.baseUrl = 'solo/';

Cortona3DSolo.uniview = {
    options: {
        TableBackgroundColorHL: '<#OHC#>',
        TableBackgroundColorSel: '<#OCC#>',
        '3DBackgroundColor': '#F4F4F4',
        DisableTextSelection: true,
        InitialSurfaceEdges: false,
        components: {
                // overrides the viewer component
                uiDplHeader: function (skin, options, solo) {
                    return skin.create('.header',
                            skin.create('h1', solo.uniview.metadata.TITLE || 'No title'),
                            skin.create('h2', 'Specification')
                    );
                }
            },
            keymap: {
                // z
                '0000:90' : doFit,
                // shift+ Z
                '0010:90' : doFit
            }
    }
};

Cortona3DSolo.use('skin', {
    baseUrl: 'uniview/'
});

function doFit() {
    var selectedItems = Cortona3DSolo.app.ipc.selectedItems;
    if (selectedItems.length === 1) {
        Cortona3DSolo.app.ipc.fitItem(Cortona3DSolo.uniview.ixml.getIndexByRow(selectedItems[0]), true);
    }
}

Cortona3DSolo.skin.create('app')
        .use('solo-uniview', {
            baseUrl: 'spec/',
            totalMemory: parseInt($("#ramodel").attr("data-memory")),
            src: $("#ramodel").attr("data-path")
        })
        .then(function () {
            var solo = Cortona3DSolo;

            solo.app.configureInstance(solo.app.DEFAULT_WHELL_ACTION_ZOOM_MAC);
            //single selection only in IPC
            solo.catalog.options.disableMultipleSelections = true;
            // instead of solo.skin.get('main').render(require('./components/mentor-fit-item'));
            solo.app.ipc.fitItem = (function (handler) {
                return function (index, animate, factor) {
                    handler.call(solo.app.ipc, index, animate, factor);
                    // dispatch custom event
                    solo.dispatch('app.ipc.didFitItem', index);
                };
            })(solo.app.ipc.fitItem);

            // change default action for 'Fit selection' button
            // instead of solo.skin.get('main').render(require('./components/mentor-fit-button'));
            document.querySelector('#btn-selection-fit').onclick = doFit;


            //off by default
            solo.uniview.settings.SurfaceEdges = false;
            //enable Full Table by default
            solo.uniview.settings.FullTable=true;
        })
        .catch(console.error.bind(console));

// General processing
Cortona3DSolo.on('app.didFinishLoadDocument', function (data) {
    switch (data.type) {
        case "ipc":
            handler.loadSheetViews(data.sheets);
            handler.layoutContentPanel();
            break;
        case "procedure":
            Cortona3DSolo.app.procedure.play();
            break;
    }
});

// 3D Model processing
Cortona3DSolo.on('app.ipc.didFitItem', function (index) {
    var itemInfo = getItemInfo(index);
    handler.zoomRelatedDiagrams(itemInfo);
});

Cortona3DSolo.on('touch.didObjectClick', function (handle, name, m_pageX, m_pageY, buttons) {
    if (buttons === 1) { // LMB only
        handler.selectMatchedItem(m_pageX, m_pageY, name);
    }
});

//Cortona3DSolo.on('app.ipc.didHoverItem', function (index) {
//  var itemInfo = getItemInfo(index);
//handler.doHoverItem(itemInfo);
//});

// DPL events processing
Cortona3DSolo.on('app.ipc.dpl.didSelectRow', function (index, keys) {
    var selectedRows = Cortona3DSolo.app.ipc.selectedItems;
    const ixml = Cortona3DSolo.app.ipc.interactivity;
    var row = ixml.getRowByIndex(index);
    if (!selectedRows.includes(row)) {
        var itemInfo = getItemInfo(index);
        handler.selectMatchedRow(m_pageX, m_pageY, itemInfo);
    }
});

Cortona3DSolo.on('app.ipc.dpl.didHoverRow', function (index) {
    var itemInfo = getItemInfo(index);
    handler.doHoverItem(itemInfo);
});

Cortona3DSolo.on('touch.didObjectEnter', function (handle,name) {
    handler.doHoverObject(name,handle);
});

function getItemInfo(index)
{
    const ixml = Cortona3DSolo.app.ipc.interactivity;
    var row = ixml.getRowByIndex(index);

    // get item's information object
    var info = ixml.getItemInfo(row);

    return info;
}

//for use in selectMatchedRow
let m_pageX, m_pageY;
window.addEventListener('click', function (event) {
    m_pageX = event.x;
    m_pageY = event.y;
    // ensure Popover closes on document level mouse click
    m.publisher.eventDispatcher.dispatchEvent(m.publisher.events.CLOSE_POPOVER, {});
    handler.clearIPCTable();
});