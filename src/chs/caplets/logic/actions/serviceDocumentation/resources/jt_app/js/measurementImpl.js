/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Updates measurement toolbar
* @param {String} rootPSID - root PSID of object being measured
*/

function updateMeasurement ( rootPSID ) {
	document.getElementById( 'toolbar_measurement' ).className = 'toolbarButton';
	document.getElementById( 'toolbar_markup' ).className = 'toolbarButton'; 	// lets allow Markup here also
	if ( !viewerManager.modelHasXt( rootPSID ) ) {
		document.getElementById( 'XTmeasurement' ).style.display = 'none';
		document.getElementById( 'Picking_Modes' ).style.display = 'none';
		document.getElementById( 'Filter_pick' ).style.display = 'none';
	}
	else {
		document.getElementById( 'XTmeasurement' ).style.display = 'block';
		document.getElementById( 'Picking_Modes' ).style.display = 'block';
		document.getElementById( 'Filter_pick' ).style.display = 'block';
	}
}

/**
* Handles all logic in a measurement event.
* @param {String[]} objects - psIds of objects involved in the measurement operations.
* @param {int[]} vertexIndices - vertices involved in the measurement operations. Defined in callback.
* @param {Object} result - result of measurement operation
*/
function onMeasurementEvent ( objects, vertexIndices, result, dataNameTag, measureModeTag ) {
	//create measurement result on toolkit panel
	var measureText = '';
	var span = document.createElement( 'span' );
	span.className += "measurementListItem";
	for ( var key in result.content ) {
		if ( result.unit !== undefined ) {
			measureText = result.content[ key ] + result.unit;
			span.innerHTML += key + ": " + measureText + " ";
		}
		else {
			measureText = result.content[ key ];
			span.innerHTML += key + ": " + measureText + " ";
		}
	}

	var line = document.createElement( 'div' );
	line.className += "measurementListItemContainer";
	var type = null;
	for ( var i = 0; i < objects.length; i++ ) {
			line.setAttribute( dataNameTag + i.toString(), objects[ i ] );
			line.setAttribute( measureModeTag, measurementManager.mode );
			type = viewerManager.getObjectTypeByPsId( objects[ i ] );

			if ( i + 1 < objects.length && type !== undefined ) {
				span.innerHTML += type + "-";
			}
			else if ( span.innerHTML !== "" ) {
				span.innerHTML += type;
			}
			else if ( span.innerHTML !== "" ) {
				span.innerHTML += type;
			}

		line.addEventListener( "click", function () {
			onMeasurementListItemClick( line );
		} );

		line.appendChild( span );
		document.getElementById( "annotationMeasurementBox" ).getElementsByClassName( 'toolBoxContent' )[ 0 ].appendChild( line );
	}

	function onPickingEvent ( args ) {
		if ( args.psId ) {
			var eventType = document.createElement( 'span' );
			eventType.innerHTML = 'PICK EVENT:';
			eventType.style.borderBottom = "1px solid black";
			writeHtmlToConsole( eventType );
			//console.log( measurementManager.convertModelSpaceToU( args.psId, args.pickPt, args.pickIndex ), args.pickIndex );
			var type = viewerManager.getObjectTypeByPsId( args.psId );
			var span = document.createElement( 'span' );
			var spanOne = document.createElement( 'span' );

			span.innerHTML = 'The model intersection point: ['
				+ Math.round( args.pickPt[ 0 ] ) + ', '
				+ Math.round( args.pickPt[ 1 ] ) + ', '
				+ Math.round( args.pickPt[ 2 ] ) + ']';
			writeHtmlToConsole( span );

			spanOne.innerHTML = 'The following ' + type + ' was PICKED: ' + args.psId.toString();
			writeHtmlToConsole( spanOne );

			if ( args.ctrl || args.alt ) {
				var spanTwo = document.createElement( 'span' );
				spanTwo.innerHTML = 'The ' + ( ( args.ctrl && args.alt ) ? 'ctrl & alt' : ( args.ctrl ? 'ctrl' : 'alt' ) )
					+ ' is currently pressed.';
				writeHtmlToConsole( spanTwo );
			}
		}
	}
}
/**
* Handles logic in selecting measurement item list click
* @param {HTMLDivElement} element - specific measurement item clicked within list
*/
function onMeasurementListItemClick ( element ) {
	var count = 0;
	var dataName = dataNameTag + count.toString();

	viewerManager.selectNone();

	while ( element.getAttribute( dataName ) != null ) {
		viewerManager.setSelectionByPsId( element.getAttribute( dataName ), true );
		count += 1;
		dataName = dataNameTag + count.toString();
	}
}

/**
* Handles logic for completely resetting measurements (for instances such as  loading a new model, etc)
*/
function resetMeasurement () {
	if ( measurementManager ) {
		setMeasurementMode( PLMVisWeb.MeasurementMode.None );
		document.getElementById( "measurementModeRadio_none" ).checked = true;
	}
}

/**
* Handles the logic for display of toolbox containing measurement settings
*/
function showMeasurementSettings () {
	if ( document.getElementById( 'toolbar_measurement' ).className === 'toolbarButton toolbarButton_inactive' ) {
		return;
	}
	if ( document.getElementById( 'rightToolbox' ).children.markupToolbox !== undefined ) {
		resetMarkup();
	}
	updateCameraMode();
	setActiveTool( document.getElementById( 'rightToolbox' ), document.getElementById( 'measurementToolbox' ) );
	resizeContent();

	/*
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( rightToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is IN
		drawerPull( rightToolbox.children[ 0 ] );
	}
	updatePickingModeOptions();
}

/**
 * * Handles logic for setting mode of measurement
* @param {int} value - selected measurement mode
* @param {HTMLInputElement} id - Id of model to be measured / highlighted
*/
function setMeasurementMode ( value, id ) {
	if ( measurementManager ) {
		if ( params.root ) {
			viewerManager.selectNone();
		}

		measurementManager.mode = value;
		updatePickingModeOptions();

		if ( id !== undefined && id !== null ) {
			id.checked = true;
			if ( value === 0 ) {
				// enable dimension highlight for testing dimension highlight feature
				viewerManager.setDimensionHighlightEnabled( true );
			}
		}
	}
}

/**
* Convenience function supplementing updatePickingModeOptions to clear selected picking mode options within measurement toolbox if necessary
*/
function clearSelection () {
	document.getElementById( 'Pick_face' ).checked = false;
	document.getElementById( 'Pick_edge' ).checked = false;
	document.getElementById( 'Pick_vertex' ).checked = false;
}

/**
* Updates selection of picking mode options within measurement toolbox as necessary
*/
function updatePickingModeOptions () {
	// Show available Selection Modes based on the current Measurement Mode
	var curSelMode = viewerManager.getPickingMode();

	var edgeVis = toggleedges,
		vertVis = togglevertices;

	clearSelection();
	resetPickFilters();

	if ( ( curSelMode & PLMVisWeb.PickingMode.PART ) === PLMVisWeb.PickingMode.PART ) {
		document.getElementById( 'pickingModeRadio_part' ).checked = true;
	}
	else {
		document.getElementById( 'pickingModeRadio_part' ).checked = false;
	}
	if ( ( curSelMode & PLMVisWeb.PickingMode.FACE ) === PLMVisWeb.PickingMode.FACE ) {
		document.getElementById( 'pickingModeRadio_face' ).checked = true;
		document.getElementById( "PickFaceDiv" ).className = "updatePadding";
		document.getElementById( "Pick_face" ).checked = true;
		edgeVis = true;
	}
	else {
		document.getElementById( 'pickingModeRadio_face' ).checked = false;
		if ( toggleedges && document.getElementById( 'measurementModeRadio_none' ).checked ) {
			edgeVis = toggleedges;
		}
		else {
			edgeVis = false;
		}

	}
	if ( ( curSelMode & PLMVisWeb.PickingMode.EDGE ) === PLMVisWeb.PickingMode.EDGE ) {
		document.getElementById( "PickEdgeDiv" ).className = "updatePadding";
		document.getElementById( "Pick_edge" ).checked = true;
		document.getElementById( 'pickingModeRadio_edge' ).checked = true;
		edgeVis = true;

	}
	else {
		document.getElementById( 'pickingModeRadio_edge' ).checked = false;
		// Don't turn edges off if faces are a selectable option but edges aren't. Shows the division of faces better.
		if ( ( curSelMode & PLMVisWeb.PickingMode.FACE ) !== PLMVisWeb.PickingMode.FACE ) {
			if ( toggleedges && document.getElementById( 'measurementModeRadio_none' ).checked ) {
				edgeVis = toggleedges;
			}
			else {
				edgeVis = false;
			}

		}
	}
	if ( ( curSelMode & PLMVisWeb.PickingMode.VERTEX ) === PLMVisWeb.PickingMode.VERTEX ) {
		document.getElementById( "PickVertexDiv" ).className = "updatePadding";
		document.getElementById( "Pick_vertex" ).checked = true;
		document.getElementById( 'pickingModeRadio_vertex' ).checked = true;
		vertVis = true;

	}
	else {
		document.getElementById( 'pickingModeRadio_vertex' ).checked = false;
		if ( togglevertices && document.getElementById( 'measurementModeRadio_none' ).checked ) {
			vertVis = togglevertices;
		}
		else {
			vertVis = false;
		}
	}

	if ( document.getElementById( 'measurementModeRadio_none' ).checked ) {
		vertVis = false;
		edgeVis = false;
	}

	setPickingModes();
	makeXtVisible( edgeVis, vertVis );
}
/**
* Convenience function supplementing updatePickingModeOptions to disable measurement filters if not available
*/
function resetPickFilters () {
	document.getElementById( "PickFaceDiv" ).className = "disableLabel";
	document.getElementById( "PickEdgeDiv" ).className = "disableLabel";
	document.getElementById( "PickVertexDiv" ).className = "disableLabel";
}

/**
* Convenience function supplementing updatePickingModeOptions to enable XT visibility
*/
function makeXtVisible ( makeEdgeVis, makeVertVis ) {
	if ( params.root !== null && params.root !== undefined ) {
		var callback = function () {
			viewerManager.setVerticesVisibilityByPsId( params.root, makeVertVis );
		};
		viewerManager.setEdgesVisibilityByPsId( params.root, makeEdgeVis, callback );
	}

}
/**
* Handles logic when "Clear Measurement" button in measurement toolbox is clicked
*/
function clearMeasurement () {
	dimensionManager.clearDimensions();
	hideEditHandlerSGO(); // hide all dimension sgo
	document.getElementById( 'annotationMeasurementBox' ).getElementsByClassName( 'toolBoxContent' )[ 0 ].innerHTML = '';
}
