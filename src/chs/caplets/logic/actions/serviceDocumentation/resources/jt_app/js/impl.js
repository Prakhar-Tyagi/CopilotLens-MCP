/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

// SET THIS TO YOUR LICENSE SERVER!
// example:  "http://yourLicenseServer:3000/api/license"
var licenseServerURL;

/************** Global Variables  ******************************/
var dataNameTag = "data-psid";
var measureModeTag = "measureMode";
var markupViewTag = "markup-view-svg";
var activeViewer = "plmviswebHost";
var defaultDimensionColors = { extensionLine: 0x783CB4, dimensionLine: 0x783CB4, text: 0x783CB4 };
//This variable defines && stores the directionality in several sectionManager API calls
var dir = [ 1, 0, 0 ], side = PLMVisWeb.SectionSide.POSITIVE, selectedPlaneId = "NONE";


//Defining parameters by element
var params = {
	name: "Sample1",
	host: document.getElementById( 'plmviswebHost' ),
	width: document.getElementById( 'content' ).offsetWidth + 'px',
	height: document.getElementById( 'content' ).offsetHeight + 'px',
	root: null
};

//Declares managers necessary to make API calls
var controlManager,
	viewerManager,
	pmiManager,
	sectionManager,
	measurementManager,
	markupManager,
	dimensionManager,
	markuppropertyCheck,
	toggleedges,
	togglevertices,
	renderMode,
	sliderValue,
	pmiIsLoaded,
	selectedModelView,
	selectedDesignGroup,
	psTree,
	pmiTree,
	modelLoaded,
	lassoMode;

/*************************************************************** */

// license check:
/* if ( licenseServerURL ) {
	PLMVisWeb.checkLicense( licenseServerURL, function ( success ) {
		if ( success ) {
			init();
		}
		else {
			console.log( "PLM Vis Web  CheckLicense FAILED!" );
		}
	} );
}
else {
	// try unlicensed:
	init();
} */

PLMVisWeb.setLicenseKey("RkqFL6Sbdvr03Rn2M5npf7qQu5dkGyJWOP/UQuVkytqgnmeHEfLiG5/kKVK6BBXNvHr6kIBWSbsW/9xT4wTW5vNkGgv/RrEZbgYjw8uSgyq2C0ZrgKH4nfMDVuOKJBlYiTb5spb5bcTWYifl17enKUZwyId1in2LkzCeaYQz+uA=");
init();


/**
* Selects which tool is active within the toolbox.
* @param {HTMLDivElement} toolbox - whether toolbox will open on either left or right side of UI
* @param {HTMLDivElement} tool- which tool is specifically being loaded
*/
function setActiveTool ( toolbox, tool ) {
	if ( tool && tool.parentElement && tool.parentElement !== toolbox ) {
		var toolboxToolbox = document.getElementById( 'toolboxToolbox' );
		if ( toolbox.children[ 1 ] ) {
			toolboxToolbox.appendChild( toolbox.removeChild( toolbox.children[ 1 ] ) );
		}
		if ( tool.parentElement === toolboxToolbox ) {
			toolbox.appendChild( toolboxToolbox.removeChild( tool ) );
		}
		else {
			toolbox.appendChild( tool );
		}
	}
}

/**
* Sets selection by PSID
* @param {String} psId - psId to set selection
* @param {boolean} isSelected- object selection
*/
function setSelection ( psId, isSelected ) {
	viewerManager.setSelectionByPsId( psId, isSelected );
}

/**
* Rebuilds the octree for after part translation
*/
function rebuild () {
	viewerManager.rebuild();
}

function setSelection ( psId, select ) {
	viewerManager.setSelectionByPsId( psId, select );
}

/**
* Sets selection of PMI by PSID
* @param {String} psId - psId of PMI to set selection
* @param {boolean} isSelected- PMI selection
*/
function setPmiSelection ( psId, isSelected ) {
	pmiManager.setSelectionByPsId( psId, isSelected );
}

function setPmiVisibility ( psId, visible ) {
	pmiManager.setVisibilityByPsId( psId, visible );
}

function setDesignGroupVisibility ( psId, visible ) {
	pmiManager.setDesignGroupActive( psId, visible );
}

function setModelViewVisibility ( psId, visible ) {
	pmiManager.setModelViewActive( psId, visible );
	updateCameraMode();
}

/**
* Sets picking mode
* @param {Enumerator} value - picking mode to be set
*/
function setPickingMode ( value ) {
	viewerManager.setPickingMode( value );
}

/**
* Handles logic for updating picking mode within measurement toolbox
* @param {HTMLInputElement} element - picking mode to be updated with check or uncheck
*/
function updatePickingMode ( element ) {
	element.checked = !element.checked;
	setPickingModes();
}

/**
* Sets picking mode within measurement toolbox
*/
function setPickingModes () {
	var mode = PLMVisWeb.PickingMode.FREE;
	if ( document.getElementById( 'Pick_face' ).checked ) {
		mode |= PLMVisWeb.PickingMode.FACE;
	}
	if ( document.getElementById( 'Pick_edge' ).checked ) {
		mode |= PLMVisWeb.PickingMode.EDGE;
	}
	if ( document.getElementById( 'Pick_vertex' ).checked ) {
		mode |= PLMVisWeb.PickingMode.VERTEX;
	}
	if ( document.getElementById( 'pickingModeRadio_part' ).checked ) {
		mode = PLMVisWeb.PickingMode.PART | PLMVisWeb.PickingMode.BODY;
	}
	if ( measurementManager.mode === PLMVisWeb.MeasurementMode.PointDistance ) {
		mode = PLMVisWeb.PickingMode.POINT;
	}

	// setPickingMode can take a bitmask:
	viewerManager.setPickingMode( mode );

	// enable/disable highlighting:
	if ( mode !== PLMVisWeb.PickingMode.FREE ) {
		viewerManager.setMouseHighlightEnabled( true );
	}
	else {
		viewerManager.setMouseHighlightEnabled( false );
		viewerManager.setDimensionHighlightEnabled( false );
	}
}
