/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

function resizeContent () {
	var toolbarHeight = document.getElementById( 'toolbar' ).offsetHeight;
	var footerHeight = document.getElementById( 'footer' ).offsetHeight;

	var contentHeight = ( window.innerHeight > ( toolbarHeight + footerHeight ) ) ? window.innerHeight - ( toolbarHeight + footerHeight ) : ( toolbarHeight + footerHeight ) - window.innerHeight;
	document.getElementById( 'content' ).style.height = contentHeight.toString() + 'px';

	var toolBoxes = document.getElementsByClassName( 'toolBox' );
	for ( var idx = 0, len = toolBoxes.length; idx < len; ++idx ) {
		toolBoxes[ idx ].style.height = ( contentHeight - 50 ).toString() + 'px';

	}

	for ( var idx = 0, len = toolBoxes.length; idx < len; ++idx ) {
		var headerHeight = toolBoxes[ idx ].querySelector( '.toolBoxTitle' ).offsetHeight;
		footerHeight = ( toolBoxes[ idx ].querySelector( '.toolBoxFooter' ) ) ? toolBoxes[ idx ].querySelector( '.toolBoxFooter' ).offsetHeight : 0;
		toolBoxes[ idx ].querySelector( '.toolBoxScrollContent' ).style.height = ( contentHeight - headerHeight - footerHeight - 50 ).toString() + 'px';
		var el = toolBoxes[ idx ].children[ 0 ];
		// Fix for defect D-41716 Resizing with arrows collapsed can cause them to disappear entirely
		if ( toolBoxes[ idx ].id === 'leftToolbox' ) {
			if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) {
				toolBoxes[ idx ].style.left = ( -toolBoxes[ idx ].offsetWidth ).toString() + 'px';
			}
			else {
				el.style.left = ( ( el.parentNode.offsetWidth ) - el.offsetWidth ).toString() + 'px';
			}

		}
		else if ( toolBoxes[ idx ].id === 'rightToolbox' ) {
			if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) {
				toolBoxes[ idx ].style.right = ( -toolBoxes[ idx ].offsetWidth ).toString() + 'px';
			}

			else {
				el.style.left = ( ( el.parentNode.offsetWidth ) - el.offsetWidth ).toString() + 'px';
			}
		}
	}
}

function onProgress ( percent ) {
	if ( percent === -1 ) {
		document.getElementById( 'globalProgressBox' ).style.display = 'block';
		document.getElementById( 'globalProgressBar' ).style.width = '0';
	}
	else if ( percent === 101 ) {
		document.getElementById( 'globalProgressBox' ).style.display = 'none';
		document.getElementById( 'globalProgressBar' ).style.width = '0';
	}
	else {
		document.getElementById( 'globalProgressBar' ).style.width = Math.floor( percent * 100.0 ).toString() + '%';
		document.getElementById( 'globalProgressText' ).innerHTML = document.getElementById( 'globalProgressBar' ).style.width;
	}
}

function drawerPull ( el ) {
	var toolbox = el.parentNode;
	// Fix for defect D-41610 Toolbox open / close arrows aren’t always correct for their state (IE)
	if ( el.style.transform || el.style.msTransform ) {
		el.style.transform = null;
		el.style.msTransform = null;
	}
	if ( toolbox.id === 'leftToolbox' ) {
		// 8250 === &rsaquo;
		// 8249 === &lsaquo;
		if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is IN
			el.style.backgroundColor = "transparent";
			el.childNodes[ 1 ].innerHTML = '&lsaquo;';
			el.style.backgroundImage = "url('./img/close.svg')";
			el.style.backgroundSize = "16px 16px";
			el.style.backgroundPositionY = "25px";
			el.style.transform = "rotate(180deg)";
			el.style.msTransform = "rotate(180deg)"; // for IE
			el.style.left = ( ( toolbox.offsetWidth ) - el.offsetWidth ).toString() + 'px';
			toolbox.style.left = '0';
		}
		else if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is OUT
			if ( el.style.left ) {
				el.style.removeProperty( 'left' );
			}
			el.childNodes[ 1 ].innerHTML = '&rsaquo;';
			el.style.backgroundImage = "url('./img/leftDrawpull.svg')";
			el.style.backgroundColor = "rgb(255, 255, 255)";
			el.style.backgroundSize = "16px 16px";
			el.style.backgroundPosition = "center";
			el.style.msTransform = "rotate(360deg)";
			toolbox.style.left = ( -toolbox.offsetWidth ).toString() + 'px';
		}
	}
	if ( toolbox.id === 'rightToolbox' ) {
		if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is OUT
			// Fix for Defect D-41611 Measurement is not toggled “off” when the measurement toolbox is closed.
			if ( typeof resetMeasurement !== "undefined" && document.getElementById( 'rightToolbox' ).children.measurementToolbox !== undefined ) {
				resetMeasurement();
			}
			if ( typeof resetMarkup !== "undefined" && document.getElementById( 'rightToolbox' ).children.markupToolbox !== undefined ) {
				resetMarkup();
			}
			if ( el.style.left ) {
				el.style.removeProperty( 'left' );
			}
			el.childNodes[ 1 ].innerHTML = '&lsaquo;';
			el.style.backgroundImage = "url('./img/leftDrawpull.svg')";
			el.style.backgroundColor = "rgb(255, 255, 255)";
			el.style.backgroundSize = "16px 16px";
			el.style.backgroundPosition = "center";
			el.style.transform = "rotate(180deg)";
			el.style.msTransform = "rotate(180deg)"; // for IE
			toolbox.style.right = ( -toolbox.offsetWidth ).toString() + 'px';
		}
		else if ( el.childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8249 ) { // drawer is IN

			el.style.backgroundImage = "url('./img/close.svg')";
			el.style.backgroundColor = "transparent";
			el.style.fill = "rgb(255, 255, 255)";
			el.style.backgroundSize = "16px 16px";
			el.style.backgroundPositionY = "6px";
			el.style.msTransform = "rotate(360deg)";
			el.childNodes[ 1 ].innerHTML = '&rsaquo;';
			el.style.left = ( ( el.parentNode.offsetWidth ) - el.offsetWidth ).toString() + 'px';
			toolbox.style.right = '0';

		}
	}
	resizeContent();
}

function changeTab ( evt, viewerTab ) {
	var i, tabContent;

	tabContent = document.getElementsByClassName( "tabContent" );
	for ( i = 0; i < tabContent.length; i++ ) {
		tabContent[ i ].style.display = "none";
	}

	//var tablinks;
	//tabLinks = document.getElementsByClassName("tablinks");
	//for (i = 0; i < tabLinks.length; i++) {
	//    tabLinks[i].className = tabLinks[i].className.replace(" active", "");
	//}

	document.getElementById( viewerTab ).style.display = "block";
	//if(evt == null){
	//	tabLinks[0].className += " active"; // set default to first viewer
	//
	//}else{
	//	evt.currentTarget.className += " active";
	//}

	//setViewer( viewerTab );
}

document.getElementById( 'fileSelector' ).addEventListener( 'change', function ( e ) {
	var files = e.target.files;
	if ( files.length > 0 ) {
		document.getElementById( 'fileNames' ).value = files[ 0 ].name;
		uploadFile();
	}
	else {
		document.getElementById( 'fileNames' ).value = e.target.nodeValue;
	}
} );
window.onresize = resizeContent;
resizeContent( null );
// close the drawers
drawerPull( document.getElementById( 'leftToolbox' ).children[ 0 ] );
drawerPull( document.getElementById( 'rightToolbox' ).children[ 0 ] );
