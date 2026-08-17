/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

/**
* Sets visibility by PSID
* @param {String} psId - psId to set visibility
* @param {boolean} isVisibile - object visibility
*/
function setVisibility ( psId, isVisible ) {
	viewerManager.setVisibilityByPsId( psId, isVisible );
}

/**
* Sets design group visibility by PSID
* @param {String} psId - psId of design group to set visibility
* @param {boolean} isVisible - design group visibility
*/
function setDesignGroupVisibility ( psId, isVisible ) {
	pmiManager.setDesignGroupActive( psId, isVisible );
}

/**
* Sets model view visibility by PSID
* @param {String} psId - psId of model view to set visibility
* @param {boolean} isVisible - model view visibility
*/
function setModelViewVisibility ( psId, isVisible ) {
	pmiManager.setModelViewActive( psId, isVisible );
	updateCameraMode();
}

/**
* Handles the logic toggling edge visiblity by psId
* @param {String} psId - psId of object in which the edge visiblity should be toggled
*/
function toggleEdges ( psId ) {
	var done = function () {
		toggleedges = viewerManager.getEdgesVisibilityByPsId( psId );
	};

	if ( psId === "-1" ) {
		psId = params.root;
	}

	if ( psId ) {
		viewerManager.setEdgesVisibilityByPsId( psId, !viewerManager.getEdgesVisibilityByPsId( psId ), done );
	}
	else {
		viewerManager.setEdgesVisibilityByPsId( params.root, !viewerManager.getEdgesVisibilityByPsId( params.root ), done );
	}
}

/*
* Sets the render type of the current model's edges
* @param {int} edgeType - number corresponding to edge type
*/
function changeRenderMode ( edgeType ) {
	if ( edgeType !== renderMode ) {
		renderMode = edgeType;
		if ( !params.root ) {
			params.root = viewerManager.psId;
		}
		if ( !viewerManager.getEdgesVisibilityByPsId( params.root ) ) {
			viewerManager.setEdgesVisibilityByPsId( params.root, true );
		}
		viewerManager.setRenderMode( edgeType );
	}
}

/**
* Handles the logic toggling vertices visiblity by psId
* @param {String} psId - psId of object in which the vertex visiblity should be toggled
*/
function toggleVertices ( psId ) {
	var done = function () {
		togglevertices1 = viewerManager.getVerticesVisibilityByPsId( psId );
	};
	if ( psId ) {
		viewerManager.setVerticesVisibilityByPsId( psId, !viewerManager.getVerticesVisibilityByPsId( psId ), done );
	}
	else {
		viewerManager.setVerticesVisibilityByPsId( params.root, !viewerManager.getVerticesVisibilityByPsId( psId ), done );
	}
}
