/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Defines managers and functional UI components
*/
function init() {
	//defining globally declared values
	controlManager = new PLMVisWeb.Control(params)
	viewerManager = controlManager.viewer;
	pmiManager = viewerManager.addExtension(PLMVisWeb.PMI);
	sectionManager = viewerManager.addExtension(PLMVisWeb.Section);
	measurementManager = viewerManager.addExtension(PLMVisWeb.Measurement);
	markupManager = viewerManager.addExtension(PLMVisWeb.Markup);
	dimensionManager = viewerManager.addExtension(PLMVisWeb.Dimension);
	navCub = addNavCube(viewerManager);	
	sectionHandle = addSectionHandle(viewerManager);
	markuppropertyCheck = 0;
	toggleedges = false;
	togglevertices = false;
	renderMode = PLMVisWeb.EdgeType.ShadedrMode1;
	sliderValue = 0;
	pmiIsLoaded = false;
	selectedModelView = null;
	selectedDesignGroup = null;
	psTree = new Map();
	pmiTree = new Map();
	modelLoaded = false;
	lassoMode = false;
	edgesVisible = false;
	currentModelRootPsId = null;

	viewerManager.registerProgressEvent( onProgress );
	
	initToolkitWorkspace ();
}


/**
* Initialize and defines values within the canvas and viewerManager 
* to prepare it for operation by the end user.
*/
function initToolkitWorkspace () {

    viewerManager.registerProgressEvent( onProgress );

	//registering initial measurements
	measurementManager.registerMeasurementEvent( function ( objects, vertexIndices, result ) {
		onMeasurementEvent( objects, vertexIndices, result, dataNameTag, measureModeTag );
	} );

	//adding event listener to product structure tree
	document.getElementById( 'viewer' ).addEventListener( 'click', function ( event ) {
		processClick( event, {
			treeType: 'product structure',
			treeMap: psTree
		} );
	} );
	// document.getElementById( 'viewer' ).addEventListener( 'touchstart', function ( event ) {
		// processClick( event, {
			// treeType: 'product structure',
			// treeMap: psTree
		// } );
	// } );	

	//adding event listener to PMI tree
	document.getElementById( 'pmi' ).addEventListener( 'click', function ( event ) {
		processClick( event, {
			treeType: 'pmi',
			treeMap: pmiTree
		} );
	} );
	// document.getElementById( 'pmi' ).addEventListener( 'touchstart', function ( event ) {
		// processClick( event, {
			// treeType: 'pmi',
			// treeMap: pmiTree
		// } );
	// } );	

	//adding event listener to model view
	document.getElementById( 'mv' ).addEventListener( 'click', function ( event ) {
		processClick( event, {
			treeType: 'model view',
			selectedItem: selectedModelView,
			setSelectedItem: function ( item ) {
				selectedModelView = item;
			}
		} );
	} );
	// document.getElementById( 'mv' ).addEventListener( 'touchstart', function ( event ) {
		// processClick( event, {
			// treeType: 'model view',
			// selectedItem: selectedModelView,
			// setSelectedItem: function ( item ) {
				// selectedModelView = item;
			// }
		// } );
	// } );	
	
	//adding event listener to selected design group
	document.getElementById( 'dg' ).addEventListener( 'click', function ( event ) {
		processClick( event, {
			treeType: 'design group',
			selectedItem: selectedDesignGroup,
			setSelectedItem: function ( item ) {
				selectedDesignGroup = item;
			}
		} );
	} );
	// document.getElementById( 'dg' ).addEventListener( 'touchstart', function ( event ) {
		// processClick( event, {
			// treeType: 'design group',
			// selectedItem: selectedDesignGroup,
			// setSelectedItem: function ( item ) {
				// selectedDesignGroup = item;
			// }
		// } );
	// } );	

	//preparing viewer
	resizeContent();
	registerPickingEventHandler( viewerManager );
	registerDimensionEventHandlers( dimensionManager );

	if ( viewerManager ) {
		changeTab( null, "plmviswebHost" ); 
	}

	if ( sectionManager ) {
		sectionManager.registerPlaneSelectionEvent( sectionPlaneSelectionHandler );
	}

	if ( viewerManager ) {
		viewerManager.setDrawWhileLoading( true );
		viewerManager.setSectionEdges( true );
	}
}

/**
* Handles logic in plane selection within the sectioning feature
* @param {Object} evt - contains the section in question (by Id) and the event type (ie: section change)
*/
function sectionPlaneSelectionHandler ( evt ) {
	selectedPlaneId = evt.selectedPlane;

	if ( selectedPlaneId >= 0 ) {
		var side = sectionManager.getSectionSide( selectedPlaneId );
		switch ( side ) {
			case PLMVisWeb.SectionSide.OFF:
				document.getElementById( "sectionSideRadio_off" ).checked = true;
				break;
			case PLMVisWeb.SectionSide.BOTH:
				document.getElementById( "sectionSideRadio_both" ).checked = true;
				break;
			case PLMVisWeb.SectionSide.POSITIVE:
				document.getElementById( "sectionSideRadio_pos" ).checked = true;
				break;
			case PLMVisWeb.SectionSide.NEGATIVE:
				document.getElementById( "sectionSideRadio_neg" ).checked = true;
				break;
			default:
				break;
		}

		var direction = sectionManager.getSectionDirection( selectedPlaneId );
		//direction[0]=X direcction[1]=Y direction[2]=Z
		if ( direction[ 0 ] === 1 && direction[ 1 ] === 0 && direction[ 2 ] === 0 ) {
			document.getElementById( "sectionAxisRadio_X" ).checked = true;
		}
		else if ( direction[ 0 ] === 0 && direction[ 1 ] === 1 && direction[ 2 ] === 0 ) {
			document.getElementById( "sectionAxisRadio_Y" ).checked = true;
		}
		if ( direction[ 0 ] === 0 && direction[ 1 ] === 0 && direction[ 2 ] === 1 ) {
			document.getElementById( "sectionAxisRadio_Z" ).checked = true;
		}
	}
}

function addNavCube ( viewer ) {
	var navCube = new NavCube();
	var sgoManager = viewer.addExtension(PLMVisWeb.SGOManager);
	sgoManager.addSGO(navCube);
	return navCube;
}

function addSectionHandle ( viewer ) {
	sectionHandle = new SectionHandle();
	sectionHandle.manager = sectionManager;
	var sgoManager = viewer.addExtension(PLMVisWeb.SGOManager);
	sgoManager.addSGO(sectionHandle);
	return sectionHandle;
}