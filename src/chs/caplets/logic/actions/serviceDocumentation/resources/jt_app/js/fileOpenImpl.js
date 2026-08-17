/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Initiates the selection of a file
*/
function selectFile () {
	document.getElementById( 'fileSelector' ).click();
}

/**
* Handles the logic for opening file dialogue options and settings
*/
function openFileDialogTree () {
	var fileDialog = document.getElementById( 'fileToolbox' );
	var fileList = fileDialog.getElementsByClassName( 'toolBoxContent' )[ 0 ];

	var callback = function () {
		var leftToolbox = document.getElementById( 'leftToolbox' );
		setActiveTool( leftToolbox, fileDialog );

		/*
		* show the panel, if applicable.
		* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
		*/
		if ( leftToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is IN
			drawerPull( leftToolbox.children[ 0 ] );
		}
	};

	if ( fileList.children.length === 0 ) {
		// first time:  load sampleFiles.json:
		readJson( "sampleFiles.json", function ( text ) {
			var data = JSON.parse( text );
			var array = [];
			var clickHandler = function ( path, format ) {
				return function () {
					openModel( path );
				};
			};
			for ( key in data ) {
				array[ key ] = document.createElement( 'div' );
				text = document.createTextNode( data[ key ].title );
				array[ key ].appendChild( text );
				array[ key ].onclick = clickHandler( data[ key ].path, data[ key ].format );
				fileList.appendChild( array[ key ] );
				if ( parseInt( key ) + 1 < data.length ) { fileList.appendChild( document.createElement( 'br' ) ); }
			}
			callback();
		} );
	}
	else {
		callback();
	}
}

/**
* This function that handles the lofic of reading converted JSON files upon page load
* in order to allow models to be accessed in the toolbox. Contemporary security features
* do not enable browsers to natively access local user files through JavaScript, so the
* toolkit loads them in through JSON as an xhr request. This then uses the filepath as
* the URL and serializes the response (filetext) as JSON.
* @param {String} file - filename for file attempting to read into the toolkit
* @param {function} callback - callback function to asynchronously load in model
*/
function readJson ( file, callback ) {
	var xhr = new XMLHttpRequest();  // HTTP GET files as JSON
	xhr.overrideMimeType( "application/json" );
	xhr.open( "GET", file, true );
	xhr.onload = function () {
		if ( xhr.status === 200 || xhr.status === 0 ) {
			callback( xhr.responseText );
		}
	};
	xhr.onerror = function () {
		console.log( 'Failed to retrieve ' + file );
	};
	xhr.send( null );
}

/**
* Function that makes API calls necessary to open a model in PLMVisWeb
* @param {String} path - filepath for file attempting to read into the toolkit
* @param {function} openCallback - callback for opening the model
*/
function openModel ( path, openCallback ) {
	var openCallback = openCallback || function () { };
	modelLoaded = false;
	//Reset render modes
	changeRenderMode( PLMVisWeb.EdgeType.Shaded );
	var culler = viewerManager.addExtension( PLMVisWeb.Culling );

	var done = function ( success, rootID ) {
		resetMeasurement();
		clearMeasurement();
		resetMarkup();
		clearMarkupViews();
		sliderValue = 0;
		pmiIsLoaded = false;
		pmiManager.clearPmiData();
		updateSliderRange();
		explode( 0 );
		sectionManager.setSelectionAll( false );
		enableContextMenu();
		if ( success ) {
			updateMeasurement( rootID );
			updateExplode();
			if ( activeViewer === "plmviswebHost" ) {
				params.root = rootID;
				document.getElementById( 'viewer' ).innerHTML = '';
				document.getElementById( 'pmi' ).innerHTML = '';
				document.getElementById( 'mv' ).innerHTML = '';
				document.getElementById( 'dg' ).innerHTML = '';
				psTree.clear();
				pmiTree.clear();
			}

			if ( viewerManager.modelHasPmi( rootID ) ) {
				document.getElementById( 'toolbar_showPmiTree' ).className = 'toolbarButton';
			} else {
				document.getElementById( 'toolbar_showPmiTree' ).className = 'toolbarButton toolbarButton_inactive';
			}

			//showPsTree();
			viewerManager.setVisibilityByPsId( rootID, true, function () {
				modelLoaded = true;
				 console.log( "Enabling Surface Scanning" );
				 culler.surfaceScanner.scanningPositions.count = 6;
				 culler.surfaceScanner.delay = 2000;
				 culler.surfaceScanner.autoScan = true;
				 culler.surfaceScanner.showCulledObjects = true;
				 culler.surfaceScanner.detailLevel = 50;
				 culler.surfaceScanner.registerSurfaceScannerEvent(
					   function ( p ) {
							if ( p === PLMVisWeb.CullingScannerEvents.scanStart ) {
								// window.parent.LoadMask.addLoadMask('locationViewSVGLoadArea');
								// $("#LoadMask",window.parent.document).show();
								// console.log( "scanning" );
								// onProgress(-1);								
								console.log( "scanning" );
								// onProgress(.2);									
							}
							if ( p === PLMVisWeb.CullingScannerEvents.scanEnd ) {
								// window.parent.LoadMask.removeLoadMask();
								// $("#LoadMask",window.parent.document).hide();
								// console.log( "done" );	
								// onProgress(1);								
								console.log( "done" );				
								// onProgress(101);	
							}
					   } 
					);
				culler.surfaceScanner.performScan();
				viewerManager.setMouseNavigationModeSensitivity(PLMVisWeb.MouseMode.ROTATE, .02);
				openCallback(success, rootID);
			} );
		} else {
			console.log( "Failed to load model into PLMVisWeb!" );
		}
	};
     culler.setActive( true );
     culler.sizeCuller.setUseLoadingUnloading( true );
     culler.sizeCuller.useMovingFrameCulling = true;
     culler.sizeCuller.boundaryMovingFrame = 15;
     culler.sizeCuller.boundary1 = 8;
     culler.sizeCuller.boundary2 = 3;	
	 culler.sizeCuller.registerLayerChangeBatchEvent(
					   function ( p ) {
							// onProgress(-1);		
							console.log( "layer change");	
							// onProgress(.2);		
							});	 
	 culler.waitUntilNotBusy(					   
						function ( p ) {
								// onProgress(1);								
								console.log( "waitUntilNotBusy" );				
								// onProgress(101);	
							});					
	culler.registerBusyEvent((isBusy)=>{
		console.log("CullingIsBusy:" + isBusy);
		if(isBusy){
			$("#LoadMask",window.parent.document).show();
		}else{
			$("#LoadMask",window.parent.document).hide();
		}
	});							
	viewerManager.open( path, done );
}
