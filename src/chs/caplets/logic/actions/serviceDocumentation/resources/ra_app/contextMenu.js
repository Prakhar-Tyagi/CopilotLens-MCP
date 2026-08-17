var psid, psids;
var materialsNew = {
	diffuse: 0xff5555,
	emissive: 0x0,
	specular: 0xffaaaa,
	shininess: 50,
	opacity: 0.5
};

function setCrystal()
{
    var path = "img/cubeTextures/";
	var format = '.png';
	var urls = [
		path + 'px' + format, path + 'nx' + format,
		path + 'py' + format, path + 'ny' + format,
		path + 'pz' + format, path + 'nz' + format
	];
	var obj = {
		diffuse: 0xffffff,
		emissive: 0,
		specular: 0xffffff,
		shininess: 0,
		opacity: 0.4,
		clipping: true,
		hasEnvMap: true,
		refractionRatio: 0.0,
	};
	function cb () {
		closeContextMenu();
	}
	var params = {
		overwrite: true,
		callback: cb
	};
    var psidStr = (psid && psid.psId) || "";
	viewerManager.setMaterialByPsId(psidStr, obj, params);
}
function closeContextMenu () {
	document.getElementById( "contextMenu" ).style.display = "none";
}

function contextToggleEdges () {
	toggleEdges(params.root, !viewerManager.getEdgesVisibilityByPsId( params.root ))
	closeContextMenu();
}

function contextToggleVertices () {
	toggleVertices( params.root );
	closeContextMenu();
}

function contextchangeRenderMode ( edgeType ) {
	changeRenderMode( edgeType );
	closeContextMenu();
}


function setCameraToStandardView ( standardView ) {
	viewerManager.setCameraToStandardView( standardView );
	closeContextMenu();
}
function AlignToPmi () {
	pmiManager.alignCameraToPmi( psid.psId );
}
function enableContextMenu () {
	function handleContextClick ( e ) {
		document.body.style.cursor = 'progress';
		e.preventDefault();
		psid = viewerManager.getPsIdAtViewCoordinate( e.offsetX, e.offsetY );
		psids = viewerManager.getSelectedParts();

		var x = e.offsetX,
			y = e.offsetY;

		var menu = document.getElementById( 'contextMenu' );
		document.body.style.cursor = 'default';
		menu.style.display = 'inline-block';
		menu.style.left = x + 'px';
		menu.style.top = y + 'px';
	}

	function onClick(e)
    {
        var pubNS = window.parent.mentor.publisher;
        var eventX = e.offsetX, eventY = e.offsetY;
        e.preventDefault();
        var cor = {
            x: eventX,
            y: eventY
        }
        var id = viewerManager.getPsIdAtViewCoordinate(cor.x,cor.y);
        var modelName, systemPaths;
        $(window.parent.document).find('.panel_content > object').each(function ()
        {
            var doc = this.contentDocument, that = this;
            if (doc == document || $(doc).has(document).length > 0 || $(document).has(doc).length > 0) {
                modelName = $(this).attr("data-path");
                if (id && id.psId) {
                    var partName = viewerManager.getNameByPsId(id.psId);
                    var firstIndex = id.psId.lastIndexOf(":");
                    var lastindex = id.psId.length;
                    var oPsid = id.psId.substr(firstIndex + 1, lastindex).replace(".svg", "");
					eventX = eventX + $(that).parent().offset().left;
                    eventY = eventY + $(that).parent().offset().top;
                    $(that).attr("data-id", oPsid);
                    window.parent.displayJTAttributes(eventX, eventY, oPsid, partName, modelName);
                }
                else {
                    pubNS.eventDispatcher.dispatchEvent(pubNS.events.CLOSE_POPOVER, {});
                }
            }
        });
    }

	document.getElementById( 'ramodel' ).addEventListener( 'contextmenu', handleContextClick, true );
	document.getElementById( 'ramodel' ).addEventListener( 'click', onClick, true );
	Cortona3DSolo.app.drawing.on

    /**    ###################################################
     *    Changes for iPad long-press for contextmenu - start
     *    ###################################################
     */

    var __longPressTimerId = null;

    function touchStartHandler(evt) {
        //evt.preventDefault();
        closeContextMenu();

        if (evt.touches.length === 1) {
            var rect = evt.target.getBoundingClientRect();
            var thisOffsetX = evt.changedTouches[0].pageX - rect.left;
            var thisOffsetY = evt.changedTouches[0].pageY - rect.top;
            var customEvt = new CustomEvent("generated::" + evt.type, {
                detail: {
                    offsetX: thisOffsetX,
                    offsetY: thisOffsetY
                }
            });
            customEvt.offsetX = thisOffsetX;
            customEvt.offsetY = thisOffsetY;

            __longPressTimerId = setTimeout(function () {
                handleContextClick(customEvt);
                __longPressTimerId = null;
            }, 1000);
        }
    }

    function touchMoveHandler(evt) {
        evt.preventDefault();
        closeContextMenu();
        clearTimeout(__longPressTimerId);
        __longPressTimerId = null
    }

    function touchEndHandler(evt) {
        //evt.preventDefault();
        if (__longPressTimerId) {
            clearTimeout(__longPressTimerId);
            __longPressTimerId = null;
        }
        else {
            evt && evt.preventDefault()
        }
    }

    function stopDefault(evt) {
        evt.preventDefault();
    }

    document.getElementById('ramodel').addEventListener('touchstart', touchStartHandler, true);
    document.getElementById('ramodel').addEventListener('touchend', touchEndHandler, true);
    document.getElementById('ramodel').addEventListener('touchmove', touchMoveHandler, true);

    // disable user-selection in the content area
    var userSelectionDisabled = '; user-select: none; -webkit-user-select: none;';
    var containerIds = ['ramodel', 'content', 'contextMenu'];
    for (var i = 0; i < containerIds.length; i++ ) {
        var cssText = document.getElementById(containerIds[i]).style.cssText;
        cssText += userSelectionDisabled;
        document.getElementById(containerIds[i]).style.cssText = cssText;
    }

    /**    ###################################################
     *    Changes for iPad long-press for contextmenu - end
     *    ###################################################
     */

	// Listens for and handles the mouse leaving the context menu.
	// NOTE: mouseleave vs. mouseout: mouseleave doens't bubble, while mouseout does. This can confuse the browser,
	// resulting in a state where a mouseout on the main div is never fired.
	document.getElementById( 'contextMenu' ).addEventListener( 'mouseleave', function ( e ) {
		closeContextMenu();
	}, false );
}

function disablePartContextMenuItems ( disabled ) {
	document.getElementById( "contextToggleEdges" ).disabled = disabled;
	document.getElementById( "contextToggleVertices" ).disabled = disabled;
	document.getElementById( "shadedEdges" ).disabled = disabled;
	document.getElementById( "outlineEdges" ).disabled = disabled;
	document.getElementById( "visibleEdges" ).disabled = disabled;
	document.getElementById( "hiddenEdges" ).disabled = disabled;

}
