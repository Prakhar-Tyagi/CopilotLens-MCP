/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Handles the logic for display of toolbox containing section settings 
*/
function showSectionToolbox () {
	if ( document.getElementById( 'rightToolbox' ).children.measurementToolbox !== undefined ) {
		resetMeasurement();
	}
	if ( document.getElementById( 'rightToolbox' ).children.markupToolbox !== undefined ) {
		resetMarkup();
	}
	updateCameraMode();
	setActiveTool( document.getElementById( 'rightToolbox' ), document.getElementById( 'sectionToolbox' ) );
	resizeContent();


	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( rightToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is IN
		drawerPull( rightToolbox.children[ 0 ] );
	}
}

/**
* Handles the logic for application of section 
*/
	function applySection () {
		if ( selectedPlaneId !== "NONE" ) {
			sectionManager.setSectionDirection( selectedPlaneId, dir[ 0 ], dir[ 1 ], dir[ 2 ] );
		}
}

/**
* Handles the logic for applying section direction 
*/
function applySectionHandleDirection () {
	if ( selectedPlaneId !== "NONE" ) {
		var sectionSide = sectionManager.getSectionSide( selectedPlaneId );
		var handleDir = [].concat( dir );
		if ( sectionSide === PLMVisWeb.SectionSide.NEGATIVE ) {
			handleDir[ 0 ] = -handleDir[ 0 ];
			handleDir[ 1 ] = -handleDir[ 1 ];
			handleDir[ 2 ] = -handleDir[ 2 ];
		}
		sectionHandle.setDirection( handleDir[ 0 ], handleDir[ 1 ], handleDir[ 2 ] );
	}
}

/**
* Handles the logic for setting section direction 
* @param {String} direction - Denotes direction of section by axis ("X", "Y", or "Z")
*/
function setSectionDirection ( direction ) {
	document.getElementById( 'sectionAxisRadio_' + direction ).checked = true;
	switch ( direction ) {
		case 'X':
			dir = [ 1, 0, 0 ];
			break;
		case 'Y':
			dir = [ 0, 1, 0 ];
			break;
		case 'Z':
			dir = [ 0, 0, 1 ];
			break;
		default:
			break;
	}
	applySectionHandleDirection();
	applySection();
}

/**
* Handles the logic for adding a section plane 
*/
 function addSectionPlane () {
	if ( selectedPlaneId === "NONE" ) {
		var center = viewerManager.getVisibleModelBoundingBoxCenter();
		sectionManager.addSectionPlane( center[ 0 ], center[ 1 ], center[ 2 ], dir[ 0 ], dir[ 1 ], dir[ 2 ], side );
	}
}


/**
* Handles the logic for removing section plane 
*/
function removeSectionPlane () {
	if ( selectedPlaneId !== "NONE" ) {
		sectionManager.removeSectionPlane( selectedPlaneId );
	}
}

/**
* Handles the logic for clearing section plane 
*/
function clearSectionPlanes () {
	sectionManager.clear();
}

/**
* Handles the logic for setting the visible section side.
* @param {String} flag - Denotes direction of section side ("+" or "-") 
*/
function setSectionSide ( flag ) {
	var oldSide = PLMVisWeb.SectionSide.POSITIVE;
	var inverse = false;
	if ( selectedPlaneId !== "NONE" ) {
		oldSide = sectionManager.getSectionSide( selectedPlaneId );
	}

	switch ( flag ) {
		case "*":
			side = PLMVisWeb.SectionSide.OFF;
			if ( oldSide === PLMVisWeb.SectionSide.NEGATIVE ) {
				inverse = true;
			}
			break;
		case '+':
			side = PLMVisWeb.SectionSide.POSITIVE;
			if ( oldSide === PLMVisWeb.SectionSide.NEGATIVE ) {
				inverse = true;
			}
			break;
		case '-':
			side = PLMVisWeb.SectionSide.NEGATIVE;
			if ( oldSide !== PLMVisWeb.SectionSide.NEGATIVE ) {
				inverse = true;
			}
			break;
		default:
			side = PLMVisWeb.SectionSide.BOTH;
			if ( oldSide === PLMVisWeb.SectionSide.NEGATIVE ) {
				inverse = true;
			}
			break;
	}

	if ( selectedPlaneId !== "NONE") {
		sectionManager.setSectionSide( selectedPlaneId, side );
		if ( inverse ) {
			applySectionHandleDirection();
		}
	}
}

function updateSection(direction)
{
	if (selectedPlaneId != 'NONE') {
		var updateDir = 5;
		if (direction === '-') {
			updateDir = -5;
		}

		var position = sectionManager.getSectionPosition(selectedPlaneId);
		if (document.getElementById('sectionAxisRadio_X').checked) {
			position[0] += updateDir;
		}
		else if (document.getElementById('sectionAxisRadio_Y').checked) {
			position[1] += updateDir;
		}
		else if (document.getElementById('sectionAxisRadio_Z').checked) {
			position[2] += updateDir;
		}
		;

		sectionManager.setSectionPosition(selectedPlaneId, position[0], position[1], position[2]);
	}
}