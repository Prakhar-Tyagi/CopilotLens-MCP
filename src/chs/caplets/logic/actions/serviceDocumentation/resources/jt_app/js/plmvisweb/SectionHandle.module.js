//© 2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC

import { CameraMode, SectionSide, THREE } from "@com.siemens.plm.web/PLMVisWeb";
import ArrowHandle from "./ArrowHandle.module";

var _manager = null,
	_mouseDown = false,
	_savedCaps = false,
	_savedEdges = false,
	_intersectPoint = null,
	_sectionPlaneChangeHandle = null;

/**
 * @class SectionHandle
 * @classdesc Represents the arrow with handle.<br><br>
 */
function SectionHandle ( arrowColor, handleColor ) { //NOSONAR
	ArrowHandle.call( this, arrowColor, handleColor );
	this.type = "SectionHandle";
	this.name = "SectionHandle";
}

SectionHandle.prototype = Object.create( ArrowHandle.prototype );
SectionHandle.prototype.constructor = SectionHandle;

function _getViewCamera ( viewer ) {
	var camInfo = viewer.getCameraInfo();
	if ( viewer.getCameraMode() === CameraMode.PERSPECTIVE ) {
		var pCamera = new THREE.PerspectiveCamera( camInfo.perspective.fov, camInfo.perspective.aspect, camInfo.perspective.near, camInfo.perspective.far );
		pCamera.position.fromArray( camInfo.perspective.pos );
		pCamera.up.fromArray( camInfo.perspective.up );
		pCamera.lookAt( new THREE.Vector3().fromArray( camInfo.perspective.tgt ) );
		pCamera.updateMatrixWorld();
		return pCamera;
	}
	else {
		var oCamera = new THREE.OrthographicCamera( camInfo.orthographic.left, camInfo.orthographic.right, camInfo.orthographic.top,
			camInfo.orthographic.bottom, camInfo.orthographic.near, camInfo.orthographic.left );
		oCamera.position.fromArray( camInfo.orthographic.pos );
		oCamera.up.fromArray( camInfo.orthographic.up );
		oCamera.lookAt( new THREE.Vector3().fromArray( camInfo.orthographic.tgt ) );
		oCamera.updateMatrixWorld();
		return oCamera;
	}
}

function _setHandlePosition ( handle, planeId ) {
	if ( handle.viewer && planeId !== "NONE" ) {
		var sectionPosition = _manager.getSectionPosition( planeId );

		if ( sectionPosition ) {
			var planeVector = new THREE.Vector3( sectionPosition[ 0 ], sectionPosition[ 1 ], sectionPosition[ 2 ] );
			var camera = _getViewCamera( handle.viewer );
			var vector = planeVector.project( camera );
			var dim = handle.viewer.control.getSize();
			var halfWidth = dim.width / 2,
				halfHeight = dim.height / 2;

			handle.setPosition( Math.round( vector.x * halfWidth + halfWidth ), Math.round( -vector.y * halfHeight + halfHeight ) );
		}
	}
};

Object.defineProperties( SectionHandle.prototype, {
	manager: {
		set: function ( manager ) {
			_manager = manager;
			if ( _manager ) {
				_sectionPlaneChangeHandle = function ( obj ) {
					var sectionPlaneID = obj.selectedPlane;
					if ( sectionPlaneID !== "NONE" ) {
						var planeId = _manager.getSelectedPlane();
						_setHandlePosition( this, planeId );
						var sectionDirection = _manager.getSectionDirection( planeId );
						var sectionSide = _manager.getSectionSide( planeId );
						if ( sectionSide === SectionSide.NEGATIVE ) {
							sectionDirection[ 0 ] = -sectionDirection[ 0 ];
							sectionDirection[ 1 ] = -sectionDirection[ 1 ];
							sectionDirection[ 2 ] = -sectionDirection[ 2 ];
						}
						this.setDirection( sectionDirection[ 0 ], sectionDirection[ 1 ], sectionDirection[ 2 ] );
						this.visible = true;
					}
					else {
						this.visible = false;
					}
				}.bind( this );
				_manager.registerPlaneSelectionEvent( _sectionPlaneChangeHandle );
			}
		}
	}
} );

SectionHandle.prototype.render = function ( renderer, camInfo ) {
	if ( _manager ) {
		_setHandlePosition( this, _manager.getSelectedPlane() );
	}

	ArrowHandle.prototype.render.apply( this, arguments );
};

/**
 * Removes attached events
 *
 * @function removeEvents
 * @memberof SectionHandle.prototype
 *
 */
SectionHandle.prototype.removeEvents = function () {
	if ( _manager ) {
		_manager.unregisterPlaneSelectionEvent( _sectionPlaneChangeHandle );
	}

	ArrowHandle.prototype.removeEvents.apply( this, arguments );
};

SectionHandle.prototype.mouseMove = function ( event ) {
	if ( _mouseDown && this.viewer && _manager ) {
		this.viewer.startRenderLoop();

		var camera = _getViewCamera( this.viewer );
		var viewerPlace = this.viewer.domElement;

		// Need to check event for touch or mouse.
		var ptX, ptY;
		if ( event.touches && event.touches.length > 0 ) {
			ptX = event.touches[ 0 ].pageX;
			ptY = event.touches[ 0 ].pageY;
		}
		else {
			ptX = event.offsetX;
			ptY = event.offsetY;
		}

		var pX = ( ptX / viewerPlace.width ) * 2 - 1;
		var pY = - ( ptY / viewerPlace.height ) * 2 + 1;

		var mouseVector = new THREE.Vector3( pX, pY, 1 );
		mouseVector.unproject( camera );
		var raycaster = new THREE.Raycaster( camera.position, mouseVector.sub( camera.position ).normalize() );
		var dragDist = new THREE.Vector3();
		raycaster.ray.closestPointToPoint( _intersectPoint, dragDist );
		dragDist.subVectors( dragDist, _intersectPoint );
		var target = this.viewer.getActiveCameraControl().target;
		var cameraVector = new THREE.Vector3().subVectors( target, camera.position );
		var selectedPlaneId = _manager.getSelectedPlane();
		var sectionD = _manager.getSectionDirection( selectedPlaneId );
		var sectionP = _manager.getSectionPosition( selectedPlaneId );
		var sectionDirection = new THREE.Vector3( sectionD[ 0 ], sectionD[ 1 ], sectionD[ 2 ] );
		var sectionPosition = new THREE.Vector3( sectionP[ 0 ], sectionP[ 1 ], sectionP[ 2 ] );
		var rads = cameraVector.angleTo( sectionDirection );
		if ( rads < Math.PI * 0.10 || rads > Math.PI * 0.90 ) {
			return; 	// Angle too severe, drag unpredictable
		}

		// Distance to section plane
		dragDist.projectOnVector( sectionDirection );
		var newPos = new THREE.Vector3().copy( sectionPosition ).add( dragDist );
		_manager.setSectionPosition( selectedPlaneId, newPos.x, newPos.y, newPos.z, false );

		_intersectPoint.add( dragDist );
	}
};

SectionHandle.prototype.mouseUp = function ( event ) {
	if ( _mouseDown ) {
		// restore saved cap/edge states:
		this.viewer.setSectionCaps( _savedCaps );
		this.viewer.setSectionEdges( _savedEdges );

		this.viewer.stopRenderLoop();
		_mouseDown = false;
	}
};

SectionHandle.prototype.mouseDown = function ( event ) {

	// Need to check event for touch or mouse.
	var ptX, ptY;
	if ( event.touches && event.touches.length > 0 ) {
		ptX = event.touches[ 0 ].pageX;
		ptY = event.touches[ 0 ].pageY;
	}
	else {
		ptX = event.offsetX;
		ptY = event.offsetY;
	}

	if ( _manager && this.intersect( ptX, ptY ) ) {
		event.override = true;
		_mouseDown = true;

		// save off cap/edge state and disable for drag:
		_savedCaps = this.viewer.getSectionCaps();
		_savedEdges = this.viewer.getSectionEdges();
		this.viewer.setSectionCaps( false );
		this.viewer.setSectionEdges( false );

		var dim = this.viewer.control.getSize();
		var pX = ( ptX / dim.width ) * 2 - 1;
		var pY = - ( ptY / dim.height ) * 2 + 1;
		_intersectPoint = new THREE.Vector3( pX, pY, 1 ).unproject( _getViewCamera( this.viewer ) );
	}
};

export default SectionHandle;
