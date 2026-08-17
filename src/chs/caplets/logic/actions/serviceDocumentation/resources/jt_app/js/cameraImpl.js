/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Sets camera to perspective view
*/
function setPerspectiveCamera () {
	viewerManager.setCameraMode( PLMVisWeb.CameraMode.PERSPECTIVE );
}

/**
* Sets camera to orthographic view
*/
function setOrthographicCamera () {
	viewerManager.setCameraMode( PLMVisWeb.CameraMode.ORTHOGRAPHIC );
}

/**
* Sets camera to fit everything visible
*/
function fitAll () {
	viewerManager.fitToVisible();
}

/**
* Handles the logic for display of toolbox containing camera settings 
*/
function showSettings () {
	if ( document.getElementById( 'rightToolbox' ).children.measurementToolbox !== undefined ) {
		resetMeasurement();
	}
	if ( document.getElementById( 'rightToolbox' ).children.markupToolbox !== undefined ) {
		resetMarkup();
	}
	updateCameraMode();
	setActiveTool( document.getElementById( 'rightToolbox' ), document.getElementById( 'settingsToolbox' ) );
	resizeContent();

	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( rightToolbox.children[ 0 ].children[ 0 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is IN
		drawerPull( rightToolbox.children[ 0 ] );
	}
}

/**
* Handles the logic for toggling camera mode between orthographic view and perspective view 
*/
function updateCameraMode () {
	var mode = viewerManager.getCameraMode();
	if ( mode === PLMVisWeb.CameraMode.PERSPECTIVE ) {
		document.getElementById( 'cameraModeRadio_perspective' ).checked = true;
	}
	else {
		document.getElementById( 'cameraModeRadio_orthographic' ).checked = true;
	}
}