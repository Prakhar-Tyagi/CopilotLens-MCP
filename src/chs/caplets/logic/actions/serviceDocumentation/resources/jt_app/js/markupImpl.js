/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Enables markup within toolkit
* @param {boolean} enable - whether markup is enabled
*/
function enableMarkup ( enable ) {
	if ( !markupManager ) {
		return;
	}

	if ( enable ) {

		document.getElementById( 'markupOn' ).checked = true;			// synch buttons
		document.getElementById( 'markupOff' ).checked = false;
		document.getElementById( 'markupSelect' ).checked = true;

		if ( markuppropertyCheck > 0 ) {
			document.getElementById( "markupProperty" + markuppropertyCheck ).checked = true;
		}
	}
	else {

		document.getElementById( 'markupOn' ).checked = false;			// synch buttons
		document.getElementById( 'markupOff' ).checked = true;
		document.getElementById( 'markupSelect' ).checked = false;
		document.getElementById( 'markupLine' ).checked = false;
		document.getElementById( 'markupFreehand' ).checked = false;
		document.getElementById( 'markupEllipse' ).checked = false;
		document.getElementById( 'markupRect' ).checked = false;
		document.getElementById( 'markupText' ).checked = false;
		document.getElementById( 'markupAnchoredText' ).checked = false;

		document.getElementById( 'markupProperty1' ).checked = false;	// sync buttons
		document.getElementById( 'markupProperty2' ).checked = false;
		document.getElementById( 'markupProperty3' ).checked = false;
	}

	markupManager.enable = enable;
}

/**
* Sets the markup mode within toolkit
* @param {int} mode - integer corresponding to markup mode enabled 
*/
function setMarkupMode ( mode ) {
	markupManager.mode = mode;

	document.getElementById( 'markupOn' ).checked = true;				// synch buttons
	document.getElementById( 'markupOff' ).checked = false;
	document.getElementById( 'markupSelect' ).checked = false;
	document.getElementById( 'markupLine' ).checked = false;
	document.getElementById( 'markupFreehand' ).checked = false;
	document.getElementById( 'markupEllipse' ).checked = false;
	document.getElementById( 'markupRect' ).checked = false;
	document.getElementById( 'markupText' ).checked = false;
	document.getElementById( 'markupAnchoredText' ).checked = false;

	if ( mode === PLMVisWeb.MarkupMode.Select )
		document.getElementById( 'markupSelect' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.Line )
		document.getElementById( 'markupLine' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.Freehand )
		document.getElementById( 'markupFreehand' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.Ellipse )
		document.getElementById( 'markupEllipse' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.Rect )
		document.getElementById( 'markupRect' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.Text )
		document.getElementById( 'markupText' ).checked = true;
	else if ( mode === PLMVisWeb.MarkupMode.AnchoredText )
		document.getElementById( 'markupAnchoredText' ).checked = true;

	if ( markuppropertyCheck > 0 ) {
		document.getElementById( "markupProperty" + markuppropertyCheck ).checked = true;
	}
}

/**
* Enables or disables markup properties within toolkit
* @param {int} choice - integer corresponding to choice of selected property
*/
function setMarkupProperties ( choice ) {
	if ( !markupManager ) {
		return;
	}

	if ( choice === 1 ) {
		document.getElementById( 'markupProperty1' ).checked = true;							// sync buttons
		document.getElementById( 'markupProperty2' ).checked = false;
		document.getElementById( 'markupProperty3' ).checked = false;

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.SelectColor, '#ff0000' ); 	// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EditColor, '#000000' ); 		// black

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineColor, '#000000' );		// black
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineOpacity, '1.0' );			// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandColor, '#000000' );	// black
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandOpacity, '1.0' );		// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillColor, 'none' );  		// no fill
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillOpacity, '0.0' );			// transparent

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeColor, '#000000' );		// black
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeOpacity, '1.0' );		// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontColor, '#000000' );		// black
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontName, 'Calibri, sans-serif' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontSize, '20' );
	}

	else if ( choice === 2 ) {
		document.getElementById( 'markupProperty1' ).checked = false;							// sync buttons
		document.getElementById( 'markupProperty2' ).checked = true;
		document.getElementById( 'markupProperty3' ).checked = false;

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.SelectColor, '#ffffff' ); 	// white
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EditColor, '#ff0000' ); 		// red

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineColor, '#ff0000' );		// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineOpacity, '1.0' );			// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandColor, '#ff0000' );	// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandOpacity, '1.0' );		// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillColor, '#ffffff' );  		// white
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillOpacity, '0.5' );			// half

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeColor, '#ff0000' );		// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeWidth, '4' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeOpacity, '1.0' );		// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontColor, '#ff0000' );		// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontName, 'Calibri, sans-serif' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontSize, '20' );
	}

	else if ( choice === 3 ) {
		document.getElementById( 'markupProperty1' ).checked = false;							// sync buttons
		document.getElementById( 'markupProperty2' ).checked = false;
		document.getElementById( 'markupProperty3' ).checked = true;

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.SelectColor, '#ff0000' ); 	// red
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EditColor, '#0000ff' ); 		// blue

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineColor, '#0000ff' );		// blue
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineWidth, '8' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.LineOpacity, '1.0' );			// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandColor, '#00ff00' );	// green
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandWidth, '8' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FreehandOpacity, '1.0' );		// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillColor, '#00ff00' );  		// green
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FillOpacity, '0.5' );			// half

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeColor, '#0000ff' );		// blue
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeWidth, '8' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.EdgeOpacity, '1' );			// full

		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontColor, '#0000ff' );		// blue
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontName, 'Calibri, sans-serif' );
		markupManager.setMarkupProperty( PLMVisWeb.MarkupProperty.FontSize, '40' );
	}

	// save properties option
	markuppropertyCheck = choice;

}

/**
* Restores saved markup into canvas
* @param {String} viewData - "svg" tag in html constituting data of markup 
*/
function showMarkupViewByData ( viewData ) {
	setMarkupMode( PLMVisWeb.MarkupMode.Select );
	markupManager.showMarkupViewByData( viewData );
}

/**
* Handles the logic for saving a markup view
*/
function getMarkupViewData () {
	// get the view data ( markups and camera position ) and the view image
	var viewData = markupManager.getMarkupViewData();
	markupManager.getMarkupViewImage( cb );
	function cb ( data ) {
		if ( data ) {	// data can be null if an exception occurs ( IE browser )
			var img = new Image();
			img.style.height = '100px';
			img.style.width = '100px';
			img.max = width = '100%';
			img.max = height = '100%';

			// add the view data to the markup view list block
			var block = document.createElement( 'div' );
			block.className += "markupListItemBlock";
			block.setAttribute( markupViewTag, viewData );

			block.addEventListener( "click", function () {
				onMarkupViewListItemClick( block );
			} );

			document.getElementById( "markupViewBox" ).getElementsByClassName( 'toolBoxContent' )[ 0 ].appendChild( block );

			// add the view image to the markup view list block
			img.onload = function () {
				block.appendChild( img );
			};

			img.src = data;
		}
	}
}

/**
* Handles selection and deselection of markup view list items
*/
function onMarkupViewListItemClick ( element ) {
	var viewData = element.getAttribute( markupViewTag );
	showMarkupViewByData( viewData );
}


/**
* Clears markup view data from canvas
*/
function clearMarkupViews() {
	document.getElementById( 'markupViewBox' ).getElementsByClassName( 'toolBoxContent' )[ 0 ].innerHTML = '';
}

/**
* Disables markup from canvas
*/
function resetMarkup () {
	enableMarkup( false );
}

/**
* Handles the logic for display of toolbox containing markup settings 
*/
function showMarkupSettings () {
	if ( document.getElementById( 'toolbar_markup' ).className === 'toolbarButton toolbarButton_inactive' ) {
		return;
	}
	if ( document.getElementById( 'rightToolbox' ).children.measurementToolbox !== undefined ) {
		resetMeasurement();
	}
	updateCameraMode();
	setActiveTool( document.getElementById( 'rightToolbox' ), document.getElementById( 'markupToolbox' ) );
	resizeContent();

	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( rightToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is IN
		drawerPull( rightToolbox.children[ 0 ] );
	}
}