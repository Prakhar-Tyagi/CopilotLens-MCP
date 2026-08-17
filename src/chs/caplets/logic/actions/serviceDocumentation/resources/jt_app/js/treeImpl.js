/* © 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

var leafTemplate = document.getElementsByTagName( "template" )[ 0 ];
var nodeTemplate = document.getElementsByTagName( "template" )[ 1 ];
var firstLeaf = true;

function createTree ( params ) {

	var data;

	switch ( params.treeType ) {
		case "product structure":
			data = viewerManager.getProductStructureInfo( viewerManager.psId, [ PLMVisWeb.AttributeFlag.VISIBILITY ], 0 );
			if ( data.childrenIds ) {
				params.treeMap.set( viewerManager.psId, data.childrenIds );
			}
			break;
		case "pmi":
			firstLeaf = true;
			data = pmiManager.getPmiStructureInfo( pmiManager.psId, [ PLMVisWeb.AttributeFlag.VISIBILITY ], 0 );
			if ( data.childrenIds ) {
				params.treeMap.set( pmiManager.psId, data.childrenIds );
			}
			break;
		case "model view":
			data = pmiManager.getModelViewsStructureInfo();
			break;
		case "design group":
			data = pmiManager.getDesignGroupsStructureInfo();
			break;
	}
	createNode( params.treeContainer, data, params.treeType );
}

function getElementByIdFromClone ( clone, id ) {

	var node = undefined;

	if ( clone.getElementById ) {
		node = clone.getElementById( id );
	} else {
		node = clone.querySelector( '#' + id );
	}

	return node;
}

function createLeaf ( parent, data, treeType ) {
	var cloneLeaf = leafTemplate.content.cloneNode( true );

	var nameNode = getElementByIdFromClone( cloneLeaf, "name" );

	var treeLineEntry = getElementByIdFromClone( cloneLeaf, "treeLineEntry" );

	if ( ( !data.children && !data.childrenIds ) || ( data.childrenIds && data.childrenIds === [] ) ) {
		var expandCollapse = getElementByIdFromClone( cloneLeaf, "expandCollapse" );
		expandCollapse.parentNode.removeChild( expandCollapse );
	} else {
		var imgSpacer = getElementByIdFromClone( cloneLeaf, "imgSpacer" );
		imgSpacer.parentNode.removeChild( imgSpacer );
	}
	nameNode.setAttribute( "id", data.psId );
	nameNode.innerHTML = data.name;
	treeLineEntry.setAttribute( "psId", data.psId );

	var imgVisible = getElementByIdFromClone( cloneLeaf, "imgCheckbox" );
	if ( treeType === "product structure" || treeType === "pmi" ) {
		if ( data.visibility === PLMVisWeb.TriState.All ) {
			imgVisible.src = "img/allChecked.svg";
		}
		else if ( data.visibility === PLMVisWeb.TriState.Some ) {
			imgVisible.src = "img/someChecked.svg";
		}
		else {
			imgVisible.src = "img/noneChecked.svg";
		}
	}
	else {
		imgVisible.parentNode.removeChild( imgVisible );
	}

	if ( treeType === "product structure" ) {
		viewerManager.registerVisibilityChangeEventByPsId( data.psId, function ( tristate ) {
			// change image based on tristate of this psId
			switch ( tristate ) {
				case PLMVisWeb.TriState.None:
					imgVisible.src = "img/noneChecked.svg";
					break;
				case PLMVisWeb.TriState.Some:
					imgVisible.src = "img/someChecked.svg";
					break;
				case PLMVisWeb.TriState.All:
					imgVisible.src = "img/allChecked.svg";
					break;
			}
		} );

		viewerManager.registerSelectionChangeEventByPsId( data.psId, function ( selected ) {
			// change bg color based on selection state of this psId
			if ( selected ) {
				nameNode.style.backgroundColor = "rgb(175,215,225)";
			}
			else {
				nameNode.style.backgroundColor = "";
			}
		} );
	}
	else if ( treeType === "pmi" ) {
		pmiManager.registerVisibilityChangeEventByPsId( data.psId, function ( tristate ) {
			// change image based on tristate of this psId
			switch ( tristate ) {
				case PLMVisWeb.TriState.None:
					imgVisible.src = "img/noneChecked.svg";
					break;
				case PLMVisWeb.TriState.Some:
					imgVisible.src = "img/someChecked.svg";
					break;
				case PLMVisWeb.TriState.All:
					imgVisible.src = "img/allChecked.svg";
					break;
			}
		} );

		pmiManager.registerSelectionChangeEventByPsId( data.psId, function ( selected ) {
			// change bg color based on selection state of this psId
			if ( selected ) {
				nameNode.style.backgroundColor = "rgb(175,215,225)";
			}
			else {
				nameNode.style.backgroundColor = "";
			}
		} );
	}


	parent.appendChild( cloneLeaf );

	return treeLineEntry;
}

function createNode ( parent, data, treeType ) {

	//prevents creation of mulitple 'PMI' nodes within tree
	if ( firstLeaf && data.name === "PMI" ) {
		var leaf = createLeaf( parent, data, treeType );
		firstLeaf = false;
	}
	if ( data.name !== "PMI" ) {
		var leaf = createLeaf( parent, data, treeType );
	}

	if ( data.children ) {
		var cloneNode = nodeTemplate.content.cloneNode( true );
		var treeNode = getElementByIdFromClone( cloneNode, "treeNode" );

		for ( var i = 0, len = data.children.length; i < len; i++ ) {
			createNode( treeNode, data.children[ i ], treeType );
		}

		leaf.parentElement.appendChild( cloneNode );
	}
}

function treeMouseOver ( elem ) {
	elem.style.backgroundColor = "rgb(169, 169, 169)";
}

function treeMouseOut ( elem ) {
	elem.style.backgroundColor = "rgb(255, 255, 255)";
}

function handleExpandCollapse ( elem, params ) {
	var childList = elem.parentElement.parentElement.getElementsByClassName( "treeListStyle" );

	if ( childList.length === 0 ) {
		var parentPsId = elem.parentElement.getAttribute( "psId" );
		var childNodes = params.treeMap.get( parentPsId );

		var cloneNode = nodeTemplate.content.cloneNode( true );
		var treeNode = getElementByIdFromClone( cloneNode, "treeNode" );

		for ( var i = 0, l = childNodes.length; i < l; ++i ) {
			var newData;
			if ( params.treeType === "product structure" ) {
				newData = viewerManager.getProductStructureInfo( childNodes[ i ], [ PLMVisWeb.AttributeFlag.VISIBILITY ], 0 );
			}
			else if ( params.treeType === "pmi" ) {
				newData = pmiManager.getPmiStructureInfo( childNodes[ i ], [ PLMVisWeb.AttributeFlag.VISIBILITY ], 0 );
			}

			if ( newData ) {
				params.treeMap.set( childNodes[ i ], newData.childrenIds );
				params.treeMap.delete( parentPsId );
				createNode( treeNode, newData, params.treeType );
			}
			else {
				console.warn( "No data returned for child node " );
			}
		}

		elem.parentElement.parentElement.appendChild( cloneNode );
		childList = elem.parentElement.parentElement.getElementsByClassName( "treeListStyle" );
	}

	if ( childList[ 0 ].classList.contains( "hideChildren" ) ) {
		childList[ 0 ].classList.remove( "hideChildren" );
		elem.src = "img/collapse.svg";
	} else {
		childList[ 0 ].classList.add( "hideChildren" );
		elem.src = "img/expand.svg";
	}
}

function handleVisibility ( elem, params ) {
	// get the psId...
	var psId;
	if ( elem.parentElement instanceof HTMLDivElement ) {
		var parent = elem.parentElement;
		psId = parent.getAttribute( "psId" );
	}

	if ( psId ) {
		var bVisible = !elem.src.endsWith( "allChecked.svg" );
		if ( params.treeType === "product structure" ) {
			viewerManager.setVisibilityByPsId( psId, bVisible );
		}
		else if ( params.treeType === "pmi" ) {
			pmiManager.setVisibilityByPsId( psId, bVisible );
		}
	}
}

function handleSelection ( elem, params ) {
	// get the psId...
	var psId, parent;
	if ( elem.parentElement instanceof HTMLDivElement ) {
		parent = elem.parentElement;
		psId = parent.getAttribute( "psId" );
	}

	// TODO: might need to re-think how we handle selection-click
	if ( psId ) {
		// detect current selection state
		var bgcolor = elem.style.backgroundColor;//"rgb(175, 215, 225)"
		var bSelected = ( bgcolor !== "rgb(175, 215, 225)" );
		if ( params.treeType === "product structure" ) {
			viewerManager.setSelectionByPsId( psId, bSelected );
		}
		else if ( params.treeType === "pmi" ) {
			pmiManager.setSelectionByPsId( psId, bSelected );
		}
		viewerManager.fitToPsId(psId, false, function(){
			console.log('ok');
		});
	}
}

function handleActivation ( elem, params ) {
	// get the psId...
	var psId, parent;
	if ( elem.parentElement instanceof HTMLDivElement ) {
		parent = elem.parentElement;
		psId = parent.getAttribute( "psId" );
	}

	if ( psId ) {
		if ( params.selectedItem === null ) {
			elem.style.backgroundColor = "rgb(175,215,225)";
			params.setSelectedItem( elem );
			if ( params.treeType === "model view" ) {
				pmiManager.setModelViewActive( psId, true );
			}
			else {
				pmiManager.setDesignGroupActive( psId, true );
			}
		}
		else if ( params.selectedItem === elem ) {
			elem.style.backgroundColor = "";
			params.setSelectedItem( null );
			if ( params.treeType === "model view" ) {
				pmiManager.setModelViewActive( psId, false );
			}
			else {
				pmiManager.setDesignGroupActive( psId, false );
			}
		}
		else {
			params.selectedItem.style.backgroundColor = "";
			elem.style.backgroundColor = "rgb(175,215,225)";
			params.setSelectedItem( elem );
			if ( params.treeType === "model view" ) {
				pmiManager.setModelViewActive( psId, true );
			}
			else {
				pmiManager.setDesignGroupActive( psId, true );
			}
		}
	}
}

function processClick ( event, params ) {
	if ( event.target !== event.currentTarget ) {
		var attribute = event.target.getAttribute( "data-element" );
		if ( attribute === "expander" ) {
			handleExpandCollapse( event.target, params );
			event.stopPropagation();
		}
		else if ( attribute === "checkbox" ) {
			handleVisibility( event.target, params );
			event.stopPropagation();
		}
		else if ( attribute === "selection" ) {
			if ( params.treeType === "product structure" || params.treeType === "pmi" ) {
				handleSelection( event.target, params );
			}
			else if ( params.treeType === "model view" || params.treeType === "design group" ) {
				handleActivation( event.target, params );
			}
			event.stopPropagation();
		}
	}
}

/**
* Handles logic for reloading tree into UI
* @param {boolean} ps - whether product strucutre is loaded into tree
* @param {boolean} pmi - whether pmi is loaded into tree
*/
function reloadTree ( ps, pmi ) {
	if ( pmi ) {
		document.getElementById( 'toolbar_showPmiTree' ).className = 'toolbarButton';
	}

	if ( activeViewer === "plmviswebHost" ) {
		if ( ps === true ) {
			if ( document.getElementById( 'viewer' ).children.length === 0 ) {
				createTree( {
					treeType: 'product structure',
					treeMap: psTree,
					treeContainer: document.getElementById( 'viewer' )
				} );
			}
		}
		//This first checks if pmi exists and then evaluates it to avoid undefined errors
		else if ( pmi && pmi === true ) {
			if ( document.getElementById( 'pmi' ).children.length === 0 ) {
				createTree( {
					treeType: 'pmi',
					treeMap: pmiTree,
					treeContainer: document.getElementById( 'pmi' )
				} );
			}

			var modelViewTreeData = pmiManager.getModelViewsStructureInfo();
			if ( modelViewTreeData !== null && document.getElementById( 'mv' ).children.length === 0 ) {
				createTree( {
					treeType: 'model view',
					treeContainer: document.getElementById( 'mv' )
				} );
			}

			var designGroupTreeData = pmiManager.getDesignGroupsStructureInfo();
			if ( designGroupTreeData !== null && document.getElementById( 'dg' ).children.length === 0 ) {
				createTree( {
					treeType: 'design group',
					treeContainer: document.getElementById( 'dg' )
				} );
			}
		}
	}
}

/**
* Handles the logic for displaying product structure tree
* @param {String} psId - psId of object in which the vertex visiblity should be toggled
*/
function showPsTree () {
	reloadTree( true, false );
	setActiveTool( document.getElementById( 'leftToolbox' ), document.getElementById( 'productStructure' ) );
	resizeContent();

	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( leftToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is IN
		drawerPull( leftToolbox.children[ 0 ] );
	}

	if ( activeViewer === "plmviswebHost" ) {
		document.getElementById( 'viewer' ).classList.remove( 'displayNone' );
	}
}

/**
* Handles the logic for displaying Product Manufacturing Information (PMI) tree
*/
function showPmiTree ( showCallback ) {
	var showCallback = showCallback || function () { };

	if ( document.getElementById( 'toolbar_showPmiTree' ).className === 'toolbarButton toolbarButton_inactive' ) {
		return;
	}

	var callback = function () {
		reloadTree( false, true );
		pmiIsLoaded = true;
		showCallback();
	};

	if ( viewerManager.modelHasPmi( params.root ) && pmiIsLoaded !== true ) {
		pmiManager.loadPmiData( params.root, callback );
	}

	setActiveTool( document.getElementById( 'leftToolbox' ), document.getElementById( 'pmiStructure' ) );
	resizeContent();

	/* 
	* show the panel, if applicable.
	* 8250 and 8249 are special characters displayed in the panel ( 8250 === &rsaquo && 8249 === &lsaquo );
	*/
	if ( leftToolbox.children[ 0 ].childNodes[ 1 ].innerHTML.charCodeAt( 0 ) === 8250 ) { // drawer is IN
		drawerPull( leftToolbox.children[ 0 ] );
	}

	if ( activeViewer === "plmviswebHost" ) {
		document.getElementById( 'pmi' ).classList.remove( 'displayNone' );
		document.getElementById( 'mv' ).classList.remove( 'displayNone' );
		document.getElementById( 'dg' ).classList.remove( 'displayNone' );
	}
}