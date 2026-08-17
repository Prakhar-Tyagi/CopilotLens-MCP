
/**
* Handles logic for updating explosion of the object relative to slider's position
*/
function updateExplode () {
	if ( viewerManager.getPartCount() < 2 ) {
		document.getElementById( 'toolbar_explodeSlider' ).style.display = 'none';
	}
	else {
		document.getElementById( 'toolbar_explodeSlider' ).style.display = 'block';
	}
}

function explode ( value ) {
	if ( document.getElementById( 'rightToolbox' ).children.markupToolbox !== undefined ) {
		resetMarkup();
	}
	dimensionManager.clearDimensions();
	if ( modelLoaded ) {
		viewerManager.autoExplode( parseFloat( value ) );
	}

	if ( value !== undefined ) {
		sliderValue = value;
	}
}

/**
* Handles logic for updating exploding toolbar's position relative to the explosion of the object
*/
function updateSliderRange () {
	var slider = document.getElementById( 'explodeSlider' );
	slider.value = sliderValue;
}