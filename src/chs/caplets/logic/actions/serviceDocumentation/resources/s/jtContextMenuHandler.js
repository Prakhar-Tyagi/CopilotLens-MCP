var JTContextMenuHandler = function (jtViewerManager, smartClientDoc, currentDoc, pubNS)
{
    this.onclick = function (e)
    {
        var eventX = e.offsetX, eventY = e.offsetY;
        e.preventDefault();
        var cor = {
            x: eventX,
            y: eventY
        }
        var id = jtViewerManager.getPsIdAtViewCoordinate(cor);
        var modelPath, oIds;
        $(smartClientDoc).find('.panel_content > object').each(function ()
        {
            var doc = this.contentDocument, that = this;
            if (doc == currentDoc || $(doc).has(currentDoc).length > 0 || $(currentDoc).has(doc).length > 0) {
                modelPath = "./../" + $(this).attr("data-path");
                $.getJSON(modelPath + "/psidMap.json", function (data)
                {
                    if (id && id.psId) {
                        var firstIndex = id.psId.lastIndexOf(":");
                        var lastindex = id.psId.length;
                        var oPsid = id.psId.substr(firstIndex + 1, lastindex).replace(".svg", "");
                        oIds = data["psidMap"][oPsid];
                        if (oIds && oIds.length > 0) {
                            eventX = eventX + $(that).parent().offset().left;
                            eventY = eventY + $(that).parent().offset().top;
                            pubNS.eventDispatcher.dispatchEvent(pubNS.events.HIGHLIGHT_OBJECT_ACROSS_WINDOWS, {
                                objectId: oIds[0],
                                systemId: null
                            });
                            pubNS.eventDispatcher.dispatchEvent(pubNS.events.OPEN_OBJECT_POPUP,
                                    {
                                        id: oIds[0],
                                        x: eventX,
                                        y: eventY,
                                        schemUID: null,
                                        systemId: null
                                    });
                        }
                        else {
                            pubNS.eventDispatcher.dispatchEvent(pubNS.events.CLOSE_POPOVER, {});
                        }
                    }
                    else {
                        pubNS.eventDispatcher.dispatchEvent(pubNS.events.CLOSE_POPOVER, {});
                    }
                });
            }
        });
    };
};