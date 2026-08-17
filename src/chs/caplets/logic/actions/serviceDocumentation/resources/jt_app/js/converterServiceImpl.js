/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Handles logic for uploading a file through the fileSelection HTML element
*/
function uploadFile () {}

/*
function uploadFile () {
	var fileInput = document.getElementById( "fileSelector" );
	var file = fileInput.files[ 0 ];

	var callback = function ( xhr ) {
		document.getElementById( 'fileSelectorOverride' ).disabled = false;  // re-enable the button
		showSpinner( false );
		if ( showDebug ) {
			console.log( "Upload done." );
		}
		if ( xhr.responseText !== undefined && xhr.responseText !== "" && ( xhr.status === 200 || xhr.status === 0 ) ) {
			if ( showDebug ) {
				console.log( "Upload response:" + xhr.responseText );
			}
			setStatus( "Upload result: " + xhr.responseText );
			var det = eval( "(" + xhr.responseText + ")" );
			convertFile( det.input );
		}
		else {
			console.error( 'Failed to upload file.  Response:', xhr.responseText );
		}
	};

	document.getElementById( 'fileSelectorOverride' ).disabled = true;  // disable the button
	showSpinner( true );
	if ( showDebug ) {
		console.log( "Uploading Jt file(s)..." );
	}
	uploader( file, callback );
}
*/


/**
* Handles the logic for display of toolbox containing converter service settings and options 
*/
function showConverterService () {
	updateCameraMode();
	setActiveTool( document.getElementById( 'leftToolbox' ), document.getElementById( 'serviceToolBox' ) );
	resizeContent();

	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( leftToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is IN
		drawerPull( leftToolbox.children[ 0 ] );
	}
}

/**
* Handles the logic for the passing selected files ito the converter, which in turn converts a .JT file (Open CAD file) to a .BOD (Binary Object Data) file.
*/
function convertFile ( input, outputDir, deleteJt ) {
	var lodLevel = 0, logLevel = 1, xt = false, pmi = false, pool = false, nodenames = false, base64 = false;
	var returnBase64String = false;

	var lodObj = document.getElementById( 'lodLevel' );
	var logObj = document.getElementById( 'logLevel' );
	if ( lodObj && lodObj.value.length > 0 ) {
		lodLevel = parseInt( lodObj.value );
	}
	if ( logObj && logObj.value.length > 0 ) {
		logLevel = parseInt( logObj.value );
	}
	if ( document.getElementById( 'enableXT' ).checked ) {
		xt = true;
	}
	if ( document.getElementById( 'enablePMI' ).checked ) {
		pmi = true;
	}
	if ( document.getElementById( 'enablePooling' ).checked ) {
		pool = true;
	}
	if ( document.getElementById( 'enableNodeNames' ).checked ) {
		nodenames = true;
	}
	if ( document.getElementById( 'encodeBase64' ).checked ) {
		base64 = true;
	}

	if ( outputDir === undefined ) {
		var output_folder = document.getElementById( 'fileNames' ).value.split( "." );
		outputDir = output_folder[ 0 ];
	}
	var callback = function ( xhr ) {
		document.getElementById( 'fileSelectorOverride' ).disabled = false;  // re-enable the button
		showSpinner( false );
		if ( showDebug ) {
			console.log( "Conversion done." );
		}
		document.getElementById( "fileSelector" ).value = null;
		document.getElementById( 'fileNames' ).value = null;
		if ( xhr.response !== undefined && ( xhr.status === 200 || xhr.status === 0 ) ) {
			if ( returnBase64String === true && base64 ) {
				var det = JSON.parse( xhr.responseText );
				console.log( det );
			}
			if ( showDebug ) {
				console.log( "Conversion result: " + xhr.response );
			}
			setStatus( "Conversion result: " + xhr.response );

			if ( viewerManager && xhr.response !== "failed" ) {
				openModel( xhr.response );
			}
		}
		else {
			console.error( 'Failed to receive response from the converter service.' );
		}
	};

	document.getElementById( 'fileSelectorOverride' ).disabled = true;// disable the button
	showSpinner( true, document.getElementById( "jt" ) );
	if ( showDebug ) {
		console.log( "Converting..." );
	}
	converter( input, outputDir, callback, lodLevel, logLevel, xt, pmi, pool, nodenames, base64, false/*lateLoad*/, deleteJt );
}

/**
* Handles the logic for setting section direction 
* @param {boolean} isShowing - whether or function should initiate spinner animation
* @param {HTMLLIElement} move - element that appends spinner element onto itself 
*/
function showSpinner ( isShowing, move ) {
	if ( isShowing && move ) {
		if ( move === document.getElementById( "jt" ) ) {
			move.appendChild( document.getElementById( 'spinner' ) );
		}
		else {
			var li = document.createElement( 'li' );
			move.appendChild( li );
			li.appendChild( document.getElementById( 'spinner' ) );

		}

	}
	var spinner = document.getElementById( 'spinner' );
	spinner.style.visibility = ( isShowing ? "visible" : "hidden" );
}

/**
* Handles the logic for setting status of converstion 
* @param {String} msg - message indicating status
*/
function setStatus ( msg ) {
	if ( msg.indexOf( 'error' ) !== -1 || msg.indexOf( 'failed' ) !== -1 ) {
		var status = document.getElementById( 'statusText' );
		status.innerHTML = msg;
	}
}