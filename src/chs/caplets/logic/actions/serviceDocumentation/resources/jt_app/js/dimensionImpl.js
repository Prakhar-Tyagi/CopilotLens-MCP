/* © 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC */

function _normalOfTriangle ( array, index1, index2, index3 ) {
	var p1 = new THREE.Vector3().fromArray( array, index1 );
	var p2 = new THREE.Vector3().fromArray( array, index2 );
	var p3 = new THREE.Vector3().fromArray( array, index3 );

	var v1 = new THREE.Vector3().subVectors( p2, p1 );
	var v2 = new THREE.Vector3().subVectors( p3, p1 );

	return v1.cross( v2 );
}

function _isZeroVector ( vector ) {
	return vector.length() < 0.0001;
}

function _getInitialScale ( origin ) {
	var cameraInfo = viewerManager.getCameraOrientationInfo();
	var cameraPosition = new THREE.Vector3().fromArray( cameraInfo.pos );
	var cameraDirection = new THREE.Vector3().fromArray( cameraInfo.tgt ).sub( cameraPosition );
	var distance = new THREE.Vector3().subVectors( origin, cameraPosition );
	return distance.projectOnVector( cameraDirection ).length();
}

function _getDirectionFromCameraInfo ( cameraInfo ) {
	var cameraPosition = new THREE.Vector3().fromArray( cameraInfo.pos );
	return new THREE.Vector3().fromArray( cameraInfo.tgt ).sub( cameraPosition );
}

function _getWorldMatrixByPsId ( psId ) {
	return new THREE.Matrix4().fromArray( viewerManager.getWorldMatrixByPsId( psId ) );
}

function _getHeightFromLength ( length ) {
	return Math.max( 5.0, length * 0.5 );
}

// TODO - Internal object should not be exposed. We *may* need to create a new API to get position/normal information
function _getPositionAndNormalArrayByPsId ( psId ) {
	// Fix after public APIs are available
	var geomObj = viewerManager._modelObject.getObjectByPsId( psId, true );
	if ( geomObj ) {
		return {
			position: geomObj.position,
			normal: geomObj.normal
		};
	}
}

// TODO - Internal object should not be exposed. We *may* need to create a new API to get positions/indices information
function _getFaceGeometryInfoByPsId ( psId ) {
	// Fix after public APIs are available
	var geomObj = viewerManager._modelObject.getObjectByPsId( psId, true );
	if ( geomObj && geomObj.geometry && geomObj.geometry.index ) {
		return {
			position: geomObj.geometry.attributes.position.array,
			index: geomObj.geometry.index.array,
			start: geomObj.start || 0
		};
	}
}

// TODO - Internal object should not be exposed. We *may* need to create a new API to get XTDescription information
function _getXTDescriptionByPsId ( psId ) {
	// Fix after public APIs are available
	var geomObj = viewerManager._modelObject.getObjectByPsId( psId, true );
	if ( geomObj ) {
		var description = geomObj.getDescription();
		if ( description ) {
			return {
				location: description.location,
				axis: description.axis
			};
		}
	}
}

// guard testing for multiple lines of text
var _generateRandomTextArray = function ( text ) {
	var len = Math.ceil( Math.random() * 5 );
	var result = [];
	if ( len === 5 ) {
		// a complex text example for testing
		result = [
			"Face Type: Planar",
			"Entity Id: ['Steps.prt','JQBj']",
			"Normal: {'x':0,'y':0,'z':-1}",
			"Position: {'x':-204.3805099140345,'y':192.55844728809552,'z':85.3197406661518}",
			"Radius: 0",
			"Radius or Angle: 0",
			"Edges: [['Steps.prt','JQCQ'],['Steps.prt','JQCW'],['Steps.prt','JQB2'],['Steps.prt','JQCD']]",
		];
	} else {
		for ( var i = 0; i < len; i++ ) {
			result.push( text );
		}
	}
	return result;
};

var dimensionTextDiv = document.createElement( 'div' );
dimensionTextDiv.style.zIndex = 99;
dimensionTextDiv.style.position = 'absolute';
dimensionTextDiv.style.left = 0;
dimensionTextDiv.style.top = 0;
var dimensionTextInput = document.createElement( 'textarea' );
dimensionTextInput.style.resize = 'none';
dimensionTextInput.style.overflow = 'hidden';
dimensionTextInput.style.zIndex = 100;
dimensionTextInput.style.position = 'absolute';
dimensionTextInput.oninput = function () {
	var text = dimensionTextInput.value.split( '\n' );
	var textRowNumber = text.length;
	var textLength = 0;
	for ( var i = 0; i < textRowNumber; i++ ) {
		if ( text[ i ].length > textLength ) {
			textLength = text[ i ].length;
		}
	}
	dimensionTextInput.cols = textLength;
	dimensionTextInput.rows = textRowNumber;
};
dimensionTextDiv.appendChild( dimensionTextInput );

function _showTextEditor ( dimId, position2d, text ) {
	// create text area for user input
	params.host.appendChild( dimensionTextDiv );
	dimensionTextDiv.style.height = params.height;
	dimensionTextDiv.style.width = params.width;
	var textValue = '';
	var textLength = 0;
	var textRowNumber = 1;
	if ( text instanceof Array ) {
		textRowNumber = text.length;
		for ( var i = 0; i < textRowNumber; i++ ) {
			textValue += text[ i ];
			if ( i < textRowNumber - 1 ) {
				textValue += '\n';
			}
			if ( text[ i ].length > textLength ) {
				textLength = text[ i ].length;
			}
		}
	} else {
		textValue = text;
		textLength = text.length;
	}
	dimensionTextInput.value = textValue;
	dimensionTextInput.cols = textLength;
	dimensionTextInput.rows = textRowNumber;
	var halfWidth = dimensionTextInput.clientWidth / 2;
	dimensionTextInput.style.left = position2d[ 0 ] - halfWidth + 'px';
	dimensionTextInput.style.top = position2d[ 1 ] + 'px';

	dimensionTextInput.onblur = function () {
		params.host.removeChild( dimensionTextDiv );
		dimensionManager.setDimensionText( dimId, dimensionTextInput.value.split( '\n' ) );
	};

	dimensionTextInput.select();
}

function registerPickingEventHandler ( viewerManager ) {
	if ( !viewerManager ) {
		return;
	}

	viewerManager.registerPickingEvent( function ( pickArgs ) {
		if ( pickArgs.dimensionId ) {
			if ( pickArgs.dimensionComponentType === 'Text' ) {
				var controlPoints = dimensionManager.getControlPoints( pickArgs.dimensionId, [ { pointType: 'TextCenter' } ] );
				var textCenter = controlPoints[ 0 ].value;
				if ( textCenter ) {
					var textValue = dimensionManager.getDimensionText( pickArgs.dimensionId );
					var position2d = viewerManager.projectModelPointToViewCoordinate( textCenter[ 0 ], textCenter[ 1 ], textCenter[ 2 ] );
					// locate the edit box under text center for 12px offset
					position2d[ 1 ] += 12;
					_showTextEditor( pickArgs.dimensionId, position2d, textValue );
				}
			}
		}
	} );
}

/**
 * Measures and creates an angle dimension object according to the given parameters.
 * @function measureAndCreateAngleDimension
 *
 * @param {String} psID1 - psID of first geometry
 * @param {String} psID2 - psID of second geometry
 * @returns {Number} ID of the created dimension
 */
function measureAndCreateAngleDimension ( psID1, psID2 ) {
	var options = {
		"Text.Orientation": PLMVisWeb.TextOrientation.Over,
		EnableScreenRelativeSize: true
	};

	var retval = -1; // BAD dimension ID
	if ( measurementManager && psID1 && psID2 ) {
		var parameters = {
			psIds: [ psID1, psID2 ],
			mode: PLMVisWeb.MeasurementMode.Angle
		};
		parameters.measurementEvent = function ( objects, vertices, result ) {
			var matrix1 = _getWorldMatrixByPsId( objects[ 0 ] );
			var matrix2 = _getWorldMatrixByPsId( objects[ 1 ] );
			if ( matrix1 && matrix2 ) {
				var positions1 = result.cacheData.positions1;
				var positions2 = result.cacheData.positions2;
				var p1, p2, p3, p4;
				var getWorldVectorFromObject = function ( objNumber, index, matrix ) {
					var positions = objNumber === 1 ? positions1 : positions2;
					var matrixWorld = matrix || ( objNumber === 1 ? matrix1 : matrix2 );
					return new THREE.Vector3( positions[ index * 3 ], positions[ index * 3 + 1 ], positions[ index * 3 + 2 ] ).applyMatrix4( matrixWorld );
				};
				if ( positions1.length === 6 && positions2.length === 6 ) { // when from edge to edge
					p1 = getWorldVectorFromObject( 1, 0, matrix2 );
					p2 = getWorldVectorFromObject( 1, 1, matrix2 );
					p3 = getWorldVectorFromObject( 2, 0, matrix2 );
					p4 = getWorldVectorFromObject( 2, 1, matrix2 );
				} else if ( positions1.length === 6 && positions2.length === 9 ) { // when from edge to face
					p1 = getWorldVectorFromObject( 1, 0 );
					p2 = getWorldVectorFromObject( 1, 1 );
					p3 = getWorldVectorFromObject( 2, 0 );
					var p5 = getWorldVectorFromObject( 2, 1 ),
						p6 = getWorldVectorFromObject( 2, 2 ),
						p7 = new THREE.Vector3(),
						p8 = new THREE.Vector3(),
						plane = new THREE.Plane().setFromCoplanarPoints( p3, p5, p6 );
					plane.projectPoint( p1.clone(), p7 );
					plane.projectPoint( p2.clone(), p8 );
					// project the egde direction to the triangle plane and get the new direction as the plane direction
					var direction = p8.sub( p7 );
					if ( _isZeroVector( direction ) ) {
						p4 = p5;
					} else {
						p4 = p3.clone().add( direction );
					}
				} else if ( positions1.length === 9 && positions2.length === 6 ) { // when from face to edge
					p3 = getWorldVectorFromObject( 2, 0 );
					p4 = getWorldVectorFromObject( 2, 1 );
					p1 = getWorldVectorFromObject( 1, 0 );
					var p5 = getWorldVectorFromObject( 1, 1 ),
						p6 = getWorldVectorFromObject( 1, 2 ),
						p7 = new THREE.Vector3(),
						p8 = new THREE.Vector3();
					var plane = new THREE.Plane().setFromCoplanarPoints( p1, p5, p6 );
					plane.projectPoint( p3.clone(), p7 );
					plane.projectPoint( p4.clone(), p8 );
					var direction = p8.sub( p7 );
					if ( _isZeroVector( direction ) ) {
						p2 = p5;
					} else {
						p2 = p1.clone().add( direction );
					}
				} else if ( positions1.length === 9 && positions2.length === 9 ) { // when from face to face
					p1 = getWorldVectorFromObject( 1, 0 );
					p2 = new THREE.Vector3();
					p3 = getWorldVectorFromObject( 2, 0 );
					p4 = new THREE.Vector3();
					var p5 = new THREE.Vector3(),
						p6 = getWorldVectorFromObject( 1, 1 ),
						p7 = getWorldVectorFromObject( 1, 2 ),
						p8 = new THREE.Vector3(),
						p9 = getWorldVectorFromObject( 2, 1 ),
						p10 = getWorldVectorFromObject( 2, 2 ),
						p11 = new THREE.Vector3(),
						p12 = new THREE.Vector3();
					var n1 = _normalOfTriangle( positions1, 0, 3, 6 );
					var n2 = _normalOfTriangle( positions2, 0, 3, 6 );
					var plane1 = new THREE.Plane().setFromCoplanarPoints( p1, p6, p7 );
					var plane2 = new THREE.Plane().setFromCoplanarPoints( p3, p9, p10 );
					plane2.projectPoint( p1.clone(), p5 );
					plane2.projectPoint( p1.clone().add( n1 ), p11 );
					plane1.projectPoint( p3.clone(), p8 );
					plane1.projectPoint( p3.clone().add( n2 ), p12 );
					var direction1 = p12.sub( p8 ),
						direction2 = p11.sub( p5 );
					if ( _isZeroVector( direction1 ) || _isZeroVector( direction2 ) ) {
						// Similar logic as the measurement manager, consider two parallel planes as 0 degree
						if ( direction1.length() === 0 && direction2.length() === 0 ) {
							p2 = p1.clone().add( n1 );
							p4 = p3.clone().add( n2 );
						} else { // but consider two parallel curved surfaces as 180 degree
							p2 = p1.clone().add( n1 );
							p4 = p3.clone().add( n2.negate() );
						}
					} else {
						p2 = p1.clone().add( direction1 );
						p4 = p3.clone().add( direction2 );
					}
				} else { // Should not happen
					return retval;
				}

				var start1 = p1.toArray();
				var start2 = p3.toArray();
				var direction1 = p2.clone().sub( p1 ).normalize().toArray();
				var direction2 = p4.clone().sub( p3 ).normalize().toArray();
				var extension2;
				var cameraInfo = viewerManager.getCameraOrientationInfo();
				var cameraDirection = new THREE.Vector3().fromArray( cameraInfo.tgt ).sub( new THREE.Vector3().fromArray( cameraInfo.pos ) );
				/* comment out to test Dimension.getCoplanarPoint() function
				var adjustedStartTwo = dimensionManager.getCoplanarPoint( start1, direction1, start2, direction2, cameraDirection );
				if ( adjustedStartTwo !== start2 ) {
					extension2 = start2;
					start2 = adjustedStartTwo;
				}
				*/
				var angleText = result.content.Angle + '`';
				var scale = _getInitialScale( p1 );
				if ( result.content.Angle < 90 ) {
					options[ "Text.Orientation" ] = PLMVisWeb.TextOrientation.Aligned;
				}
				var params = {
					startOne: start1,
					extensionOne: p1.clone().add( cameraDirection.multiplyScalar( 0.01 ) ).toArray(),
					directionOne: direction1,
					startTwo: start2,
					extensionTwo: extension2,
					directionTwo: direction2,
					radius: Math.ceil( scale / 25 ),
					majorAngle: Number.parseFloat( result.content.Angle ) > 180,
					text: angleText,
					options: options
				};

				retval = dimensionManager.createAngularDimension( params );
			}
		};
		measurementManager.measure( parameters );
	}

	return retval;
}

/**
 * Takes a single XT Edge psId and determines a frame for it.
 * @function createDimensionForLength
 *
 * @param {String} psID - psID of geometry, only use this for creating a default frame.
 * @param {String} text - text value for the dimension.
 * @returns {Number} ID of the created dimension
 */
function createDimensionForLength ( psID, text ) {
	var retval = -1; // BAD dimension ID

	var options = {
		"Text.Orientation": PLMVisWeb.TextOrientation.Aligned,
		EnableScreenRelativeSize: true
	};

	var frameOrigin = new THREE.Vector3(),
		frameHeight = new THREE.Vector3(),
		frameLength = new THREE.Vector3(),
		geomStartNormal = new THREE.Vector3(),
		geomEndNormal = new THREE.Vector3();
	var matrix = _getWorldMatrixByPsId( psID );
	if ( !matrix ) {
		return;
	}
	// create frame based on Edge position and normal:
	var arrays = _getPositionAndNormalArrayByPsId( psID );
	if ( arrays && arrays.position && arrays.normal ) {
		var position_array = arrays.position;
		var pos_array_length = position_array ? position_array.length : 0;
		var normal_array = arrays.normal;
		var normal_array_length = normal_array ? normal_array.length : 0;
		geomStartNormal.fromArray( normal_array, 0 );
		geomEndNormal.fromArray( normal_array, normal_array_length - 3 );

		frameOrigin.fromArray( position_array, 0 );
		frameLength.fromArray( position_array, pos_array_length - 3 );
		var xLength = new THREE.Vector3().subVectors( frameLength, frameOrigin ).length();
		var direction = geomStartNormal;
		var heightValue = _getHeightFromLength( xLength );
		if ( geomStartNormal.equals( geomEndNormal ) ) {
			direction.setLength( heightValue );
		} else {
			direction.addVectors( geomStartNormal, geomEndNormal ).setLength( heightValue );
		}

		frameHeight.addVectors( frameOrigin, direction );

		frameOrigin.applyMatrix4( matrix );
		frameHeight.applyMatrix4( matrix );
		frameLength.applyMatrix4( matrix );

		var params = {
			length: frameLength.toArray(),
			height: frameHeight.toArray(),
			origin: frameOrigin.toArray(),
			text: text,
			options: options
		};

		// comment out the async dimension creation as the font file is temporary removed, turn on when CICD is setup
		// options.font = './font/Times New Roman_Regular.json';
		// var promise = new Promise( function ( resolve, reject ) {
		// 	dimensionManager.loadFont( options.font ).then( function () {
		// 		retval = dimensionManager.createLinearDimension( params );
		// 		resolve( retval );
		// 	} );
		// } );

		retval = dimensionManager.createLinearDimension( params );

		/* Testing performance
		var ids = [];
		for ( var i = 0; i < 1000; i++ ) {
			retval = dimensionManager.createLinearDimension( params );
			ids.push( retval );
		}

		frameHeight.sub( frameOrigin );
		frameLength.sub( frameOrigin ).normalize();
		var scale = 1.1;
		console.log( "Dimensions created" );

		setTimeout( function () {
			var startTime = performance.now();
			for ( var i = 0; i < 1000; i++ ) {
				var id = ids[ i ];
				var dir = frameHeight.applyMatrix4( new THREE.Matrix4().makeRotationAxis( frameLength, Math.PI / 90 ) );
				var newEnd = frameOrigin.clone().add( dir.clone().multiplyScalar( scale ) );
				dimensionManager.updateLinearDimension( id, { origin: null, height: newEnd.toArray() } );
			}
			var timeUsed = performance.now() - startTime;
			console.log( "updateLinearDimension used: " + ( timeUsed / 1000 ).toFixed( 10 ) + " seconds" );
		}, 1000 );

		setTimeout( function () {
			dimensionManager.clearDimensions();
			startTime = performance.now();
			for ( var i = 0; i < 1000; i++ ) {
				var dir = frameHeight.applyMatrix4( new THREE.Matrix4().makeRotationAxis( frameLength, Math.PI / 90 ) );
				var newEnd = frameOrigin.clone().add( dir.clone().multiplyScalar( scale ) );
				params.height = newEnd.toArray();
				dimensionManager.createLinearDimension( params );
			}
			timeUsed = performance.now() - startTime;
			console.log( "createLinearDimension used: " + ( timeUsed / 1000 ).toFixed( 10 ) + " seconds" );
		}, 2000 );*/

		/* Testing updating dimension
		dimIdEnableUpdate = retval;
		dimUpdateOrigin = frameOrigin;
		dimUpdateLengthEnd = frameLength.clone();
		dimUpdateVector = frameLength.sub( frameOrigin );
		var inputManager = viewerManager.getInputManager();
		if ( inputManager ) {
			inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Down, dimensionMouseDown, 0 );
			inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Up, dimensionMouseUp, 0 );
			inputManager.addEventListener( PLMVisWeb.InputManager.EventTypes.Move, dimensionMouseMove, 0 );
		}*/
	}

	return retval;
};

var dimIdEnableUpdate, dimUpdateVector, dimUpdateOrigin, dimUpdateLengthEnd, dimUpdateStartPoint, isDimUpdateOrigin;

function getEventTargetPosition ( event ) {
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
	return [ ptX, ptY ];
}

function dimensionMouseDown ( event ) {
	var point = getEventTargetPosition( event );
	var ptX = point[ 0 ], ptY = point[ 1 ];
	var intersectInfo = dimensionManager.getComponentInfoAtViewCoordinate( dimIdEnableUpdate, ptX, ptY );
	if ( dimIdEnableUpdate && dimensionManager.isDimensionVisible( dimIdEnableUpdate ) && intersectInfo && intersectInfo.type.indexOf( "ExtensionLine" ) > -1 ) {
		event.override = true;

		// get corresponding model point
		dimUpdateStartPoint = new THREE.Vector3().fromArray( dimensionManager.getPointOnDimFrameAtViewCoordinate( dimIdEnableUpdate, ptX, ptY ) );
		isDimUpdateOrigin = intersectInfo.side !== "Side2";
	}
}

function dimensionMouseMove ( event ) {
	if ( dimUpdateStartPoint && dimIdEnableUpdate && dimUpdateVector ) {
		event.override = true;
		var point = getEventTargetPosition( event );
		var ptX = point[ 0 ], ptY = point[ 1 ];
		var dimUpdateEndPoint = new THREE.Vector3().fromArray( dimensionManager.getPointOnDimFrameAtViewCoordinate( dimIdEnableUpdate, ptX, ptY ) );
		var dragVec = dimUpdateEndPoint.clone().sub( dimUpdateStartPoint ).projectOnVector( dimUpdateVector );

		// simulate SE face drag when user performs modeling
		// step1, compose input parameter,
		var dimensionLineLength = dimUpdateVector.add( dragVec.clone().multiplyScalar( isDimUpdateOrigin ? -1 : 1 ) ).length();
		var text = "" + Number( `${Math.round( `${dimensionLineLength}e${2}` )}e-${2}` );
		var origin = isDimUpdateOrigin ? dimUpdateOrigin.add( dragVec ) : dimUpdateOrigin;
		var length = isDimUpdateOrigin ? dimUpdateLengthEnd : dimUpdateLengthEnd.add( dragVec );

		// step2, call updateAPI
		dimensionManager.setDimensionText( dimIdEnableUpdate, text );
		dimensionManager.updateLinearDimension( dimIdEnableUpdate, { origin: origin.toArray(), length: length.toArray() } );
		viewerManager.draw();
		dimUpdateStartPoint = dimUpdateEndPoint;
	}
}

function dimensionMouseUp ( event ) {
	if ( dimUpdateStartPoint ) {
		event.override = true;
		dimUpdateStartPoint = null;
	}
}

/**
 * create area dimension for face
 * @function createNoteDimension
 *
 */
function createNoteDimension ( psIDs, text ) {
	if ( psIDs.length !== 1 || !text ) {
		return;
	}

	var faceInfo = _getFaceGeometryInfoByPsId( psIDs[ 0 ] );

	if ( !faceInfo || !faceInfo.position || !faceInfo.index ) {
		return;
	}

	var matrix = _getWorldMatrixByPsId( psIDs[ 0 ] );
	var v1 = faceInfo.index[ faceInfo.start ] * 3;
	var anchorPoint = new THREE.Vector3().fromArray( faceInfo.position, v1 );
	anchorPoint.applyMatrix4( matrix );

	var textList = _generateRandomTextArray( text );
	var scale = _getInitialScale( anchorPoint );
	var length1 = 0.07 * scale;
	var length2 = length1 / 4;
	var params = {
		origin: anchorPoint.toArray(),
		length1: length1,
		length2: length2,
		text: text,  // textList
		options: {
			EnableScreenRelativeSize: true
		}
	};

	return dimensionManager.createNoteDimension( params );
};

/**
 * create linear dimension for distance
 * @function createLinearDimensionForDistance
 *
 */
function createLinearDimensionForDistance ( psIDs, points, text ) {
	if ( psIDs.length !== 2 || !points || points.length !== 2 ) {
		return;
	}

	var options = {
		"Text.Orientation": PLMVisWeb.TextOrientation.Aligned,
		EnableScreenRelativeSize: true
	};

	var pt1 = points[ 0 ];
	var pt2 = points[ 1 ];

	var matrix = _getWorldMatrixByPsId( psIDs[ 0 ] );
	var startPoint = new THREE.Vector3( pt1.x, pt1.y, pt1.z );
	var endPoint = new THREE.Vector3( pt2.x, pt2.y, pt2.z );
	var xVector = endPoint.clone().sub( startPoint );

	var cameraInfo = viewerManager.getCameraInfo();
	var cameraUp = new THREE.Vector3().fromArray( cameraInfo.perspective.up );
	var cameraDir = _getDirectionFromCameraInfo( cameraInfo.perspective );
	var xLength = xVector.length();
	var oldXVector = xVector.clone();
	var projectOffset = xVector.clone().projectOnVector( cameraDir );
	if ( xLength < 0.001 ) {
		xVector.crossVectors( cameraUp, cameraDir );
	} else {
		xVector.sub( projectOffset ).normalize().multiplyScalar( xLength );
		projectOffset.subVectors( oldXVector, xVector );
	}

	//calculate origin, end and extension
	var origin, length, extension1, extension2;
	if ( projectOffset.length() < 0.001 || projectOffset.angleTo( cameraDir ) > Math.PI / 2 ) {
		origin = startPoint.clone().add( projectOffset );
		length = endPoint;
		extension1 = startPoint.clone().toArray();
	} else {
		origin = startPoint;
		length = endPoint.clone().sub( projectOffset );
		extension2 = endPoint.clone().toArray();
	}

	var lineOneStart, lineOneEnd;
	var yVector = new THREE.Vector3();
	var firstPsId = psIDs[ 0 ];
	var objectType = viewerManager.getObjectTypeByPsId( firstPsId );
	if ( objectType === 'Vertex' ) {
		//get dimension frame direction by camera direction
		yVector.copy( cameraDir );
	} else {
		//use the first edge to calculate frame direction
		if ( objectType === 'Edge' ) {
			var edgeInfo = _getPositionAndNormalArrayByPsId( firstPsId );
			var len = edgeInfo.position.length;
			var pos = edgeInfo.position;
			lineOneStart = new THREE.Vector3( pos[ 0 ], pos[ 1 ], pos[ 2 ] );
			lineOneEnd = new THREE.Vector3( pos[ len - 3 ], pos[ len - 2 ], pos[ len - 1 ] );
		} else if ( objectType === 'Face' ) {
			var faceInfo = _getFaceGeometryInfoByPsId( firstPsId );
			var pos = faceInfo.position;
			var indices = faceInfo.index;
			var start = faceInfo.start;
			var firstIndex = indices[ start ] * 3;
			var secondIndex = indices[ start + 1 ] * 3;

			lineOneStart = new THREE.Vector3( pos[ firstIndex ], pos[ firstIndex + 1 ], pos[ firstIndex + 2 ] );
			lineOneEnd = new THREE.Vector3( pos[ secondIndex ], pos[ secondIndex + 1 ], pos[ secondIndex + 2 ] );
		}
		yVector.subVectors( lineOneEnd, lineOneStart );
		yVector.applyMatrix4( matrix );
	}

	var direction = xVector.cross( yVector );
	var heightValue = _getHeightFromLength( xLength );
	var directionProj = direction.clone().projectOnVector( cameraDir );
	if ( _isZeroVector( direction.clone().sub( directionProj ) ) ) {
		direction.copy( cameraUp ).normalize().multiplyScalar( heightValue );
	} else {
		direction.sub( directionProj ).normalize().multiplyScalar( heightValue );
		//reverse direction to get better look
		if ( directionProj.angleTo( cameraDir ) < 0.001 ) {
			direction.negate();
		}
	}

	var height = origin.clone().add( direction );

	var params = {
		length: length.toArray(),
		height: height.toArray(),
		origin: origin.toArray(),
		extensionOne: extension1,
		extensionTwo: extension2,
		text: text,
		options: options
	};

	var dimId = dimensionManager.createLinearDimension( params );

	/* Testing rotation when updating dimension
	height.sub( origin );
	length.sub( origin ).normalize();
	var count = 0;
	var scale = 1.02;
	setInterval( function () {
		requestAnimationFrame( function () {
			count++;
			if ( count > 10 ) {
				count = 1;
				scale = scale > 1 ? 0.98 : 1.02;
			}
			var dir = height.applyMatrix4( new THREE.Matrix4().makeRotationAxis( length, Math.PI / 90 ) );
			var newEnd = origin.clone().add( dir.multiplyScalar( scale ) );
			dimensionManager.updateLinearDimension( dimId, { origin: null, height: newEnd.toArray() } );
		} );
	}, 30 );*/
	return dimId;
}

/**
 * create radial dimension for radius
 * @function createRadiusDimension
 *
 */
function createRadiusDimension ( psId, text ) {
	var objectType = viewerManager.getObjectTypeByPsId( psId );
	if ( objectType !== "Face" && objectType !== "Edge" ) {
		return;
	}

	// set text
	var showRadiusSymbol = true;
	if ( text === "" ) {
		text = "No Radius";
		showRadiusSymbol = false;
	} else if ( text.indexOf( "∞" ) !== -1 ) {
		text = text.replace( "∞", "Infinite " );
	}

	// get position, normal and matrix
	var arrayInfo = _getPositionAndNormalArrayByPsId( psId );
	var positionArray = arrayInfo.position;
	if ( objectType === "Face" ) {
		var faceInfo = _getFaceGeometryInfoByPsId( psId );
		var pos = faceInfo.position;
		var indices = faceInfo.index;
		var firstIndex = indices[ faceInfo.start ] * 3;
		positionArray = [ pos[ firstIndex ], pos[ firstIndex + 1 ], pos[ firstIndex + 2 ] ];
	}
	var matrix = _getWorldMatrixByPsId( psId );

	// set origin
	var origin = new THREE.Vector3().fromArray( positionArray ).applyMatrix4( matrix );

	// set direction and normal
	var direction, baseDirection, normal;
	var radius = parseFloat( text );
	var cameraInfo = viewerManager.getCameraInfo();
	var cameraDir = _getDirectionFromCameraInfo( cameraInfo.perspective );
	var normals = arrayInfo.normal;
	var description = _getXTDescriptionByPsId( psId );
	var location = description ? description.location : null;
	var axis = description ? description.axis : null;
	if ( location && axis && !isNaN( radius ) ) {
		var axisVector = new THREE.Vector3( axis.x, axis.y, axis.z ).applyMatrix4( matrix );
		var locationVector = new THREE.Vector3( location.x, location.y, location.z ).applyMatrix4( matrix );
		var ray = new THREE.Ray( locationVector, axisVector );
		var centerPt = new THREE.Vector3();
		ray.closestPointToPoint( origin, centerPt );
		direction = origin.clone().sub( centerPt ).normalize();
		baseDirection = cameraDir.clone().cross( axisVector ).normalize();
		normal = axisVector.toArray();
	} else {
		var cameraUp = new THREE.Vector3().fromArray( cameraInfo.perspective.up );
		var directionArray = normals.toArray ? normals.toArray() : [ normals[ 0 ], normals[ 1 ], normals[ 2 ] ];
		direction = new THREE.Vector3().fromArray( directionArray ).applyMatrix4( matrix ).normalize();
		baseDirection = new THREE.Vector3().crossVectors( cameraDir, cameraUp ).normalize();
		if ( baseDirection.angleTo( direction ) > Math.PI / 2 ) {
			baseDirection.negate();
		}
		normal = cameraDir.toArray();
	}

	// set other parameters
	var scale = _getInitialScale( origin );
	var leaderEnd = origin.clone().add( direction.clone().multiplyScalar( Math.ceil( scale / 15 ) ) );
	var baselineEnd = leaderEnd.clone().add( baseDirection.clone().multiplyScalar( Math.ceil( scale / 45 ) ) );
	var cCenter = origin.clone().sub( direction.clone().multiplyScalar( 5 ) ), cStart = origin.clone(), cEnd = origin.clone();
	if ( !isNaN( radius ) && radius > 0 ) {
		cCenter = origin.clone().sub( direction.clone().multiplyScalar( radius ) );
		cEnd = cCenter.clone().add( baselineEnd.clone().sub( cCenter ).normalize().multiplyScalar( radius ) );
	}
	var textInside = ( !isNaN( radius ) && radius > 5 );

	return dimensionManager.createRadialDimension( {
		origin: origin.toArray(),
		baselineDirection: baseDirection.toArray(),
		extensionLineLength: textInside ? 0 : Math.ceil( scale / 15 ),
		baselineLength: textInside ? 0 : Math.ceil( scale / 45 ),
		circleDef: {
			center: cCenter.toArray(),
			start: textInside && radius < 10 ? null : cStart.toArray(),
			end: textInside && radius < 10 ? null : cEnd.toArray(),
			normal: normal,
			isRadius: showRadiusSymbol && radius > 20,
			alwaysShow: radius < 10,
			showArrow: textInside,
			textInside: textInside
		},
		text: text,
		options: {
			EnableScreenRelativeSize: true
		}
	} );
}


function registerDimensionSelectionChangedHandler ( dimManager ) {
	if ( !dimManager ) {
		return;
	}

	dimManager.addEventListener( PLMVisWeb.Dimension.EventTypes.SelectionChanged, function ( eventData ) {
		//TODO add selected edit handle on dimension object
		if ( showDebug ) {
			console.log( 'Dimension ID:', eventData.dimensionId, eventData.selected ? 'selected' : 'deselected', ', Component Type:', eventData.dimensionComponentType, ', Side:', eventData.dimensionSide );
		}

		// set Extension Line Gap visibility
		dimManager.setComponentVisibility( eventData.dimensionId, eventData.selected, 'ExtensionLineGap' );

		if ( !dimensionManager.EditHandleSGOObjects ) {
			dimensionManager.EditHandleSGOObjects = {};
		}
		var pointTypes = [
			{ pointType: 'TextCenter' },

			{ pointType: 'ArrowHeadStart', side: 'Side1' },
			{ pointType: 'ArrowHeadStart', side: 'Side2' },
			{ pointType: 'ArrowLineStart', side: 'Side1' },
			{ pointType: 'ArrowLineStart', side: 'Side2' },

			{ pointType: 'ExtensionLineStart', side: 'Side1' },
			{ pointType: 'ExtensionLineStart', side: 'Side2' },
			{ pointType: 'ExtensionLineAnchor', side: 'Side1' },
			{ pointType: 'ExtensionLineAnchor', side: 'Side2' },

			{ pointType: 'StubLineStart' },
			{ pointType: 'StubLineEnd' }
		];
		var controlPoints = dimensionManager.getControlPoints( eventData.dimensionId, pointTypes );
		toggleEditHandler( eventData.dimensionId, controlPoints, eventData.selected );
	} );
}

function toggleEditHandler ( dimensionId, controlPoints, selected ) {
	if ( selected ) {
		showEditHandlerSGO( dimensionId, controlPoints );
	} else {
		hideEditHandlerSGO();
	}
};

function getSGOOptionsForControlPoint ( controlPoint, textOptions ) {
	var options = {
		color: 0x00ff00,
		enableDrag: true,
		visible: true,
	};

	var type = controlPoint.pointType;
	switch ( type ) {
		case 'ArrowHeadStart':
			options.color = 0x0000ff;
			break;
		case 'ArrowLineStart':
			if (
				textOptions.orientation === PLMVisWeb.TextOrientation.Over ||
				!textOptions.insideExtensionLine
			) {
				options.visible = false;
			}

			// except TextOrientation.Over, 			need to check on text position to dynamic show/hide ArrowLineStart sgo
			if ( textOptions.orientation !== PLMVisWeb.TextOrientation.Over ) {
				options.checkTextFn = function () {
					var val = getTextOptions( controlPoint.dimensionId );
					if ( val ) { return val.insideExtensionLine; }
					return false;
				}
			}
			break;
		case 'ExtensionLineAnchor':
			options.color = 0xff0000;
			options.enableDrag = false
			break;
		case 'TextCenter':
		case 'ExtensionLineStart':
			break;
	}

	return options;
};


/**
 *
 * @param {*} dimensionId
 * @returns {Object} \{
 * 		orientation: orientation,
 *      insideExtensionLine: true
 * \}
 */
function getTextOptions ( dimensionId ) {
	// return TextOrientation and Text Relative position (inside/outside extension line)
	var options = {};

	var opt = dimensionManager.getDimensionOptions( dimensionId );
	options.orientation = opt[ "Text.Orientation" ];

	// if TextOrientation.Over, return directly
	if ( options.orientation === PLMVisWeb.TextOrientation.Over ) {
		return options;
	}

	// else need to check Text Relative position (inside/outside extension line)
	var points = [
		{ pointType: 'TextCenter' },
		{ pointType: 'ArrowLineEnd', side: 'Side1' },
		{ pointType: 'ArrowLineEnd', side: 'Side2' } ];
	dimensionManager.getControlPoints( dimensionId, points );
	options.insideExtensionLine = true;

	// if it's valid dimension type
	if ( points[ 0 ].value && points[ 1 ].value && points[ 2 ].value ) {
		var textCenter = new THREE.Vector3().fromArray( points[ 0 ].value );
		var startPoint = new THREE.Vector3().fromArray( points[ 1 ].value );
		var endPoint = new THREE.Vector3().fromArray( points[ 2 ].value );

		var dimensionLineLength = new THREE.Vector3().subVectors( startPoint, endPoint ).length();
		var centerToStartLength = new THREE.Vector3().subVectors( startPoint, textCenter ).length();
		var centerToEndLength = new THREE.Vector3().subVectors( endPoint, textCenter ).length();

		if ( centerToStartLength > dimensionLineLength || centerToEndLength > dimensionLineLength ) {
			options.insideExtensionLine = false;
		}
	}

	return options;
};

function showEditHandlerSGO ( dimensionId, controlPoints ) {
	if ( !controlPoints ) {
		return;
	}

	var textOptions = getTextOptions( dimensionId );

	controlPoints.forEach( item => {
		// construct controlPointInfo
		var controlPointInfo = {
			dimensionId: dimensionId,
			pointType: item.pointType,
			side: item.side
		};

		item.dimensionId = dimensionId;
		var sgoOptions = getSGOOptionsForControlPoint( item, textOptions );

		// show sgo
		var key = item.side ? item.pointType + "_" + item.side : item.pointType;
		var point = item.value;
		if ( !dimensionManager.EditHandleSGOObjects[ dimensionId ] ) {
			dimensionManager.EditHandleSGOObjects[ dimensionId ] = {};
		}
		var obj = dimensionManager.EditHandleSGOObjects[ dimensionId ];
		var sgo = obj[ key ];
		if ( point ) {
			if ( !sgo ) {
				sgo = obj[ key ] = createSGO( 'DimensionEditHandle', null, sgoOptions );
				sgo.manager = dimensionManager;
				sgo.controlPointInfo = controlPointInfo;
				var position2d = viewerManager.projectModelPointToViewCoordinate( point[ 0 ], point[ 1 ], point[ 2 ] );
				sgo.setPosition( position2d[ 0 ], position2d[ 1 ] );
			} else {
				sgo.visible = sgoOptions.visible;
			}
		} else if ( sgo ) {
			viewerManager.removeSGO( sgo );
			delete obj[ key ];
		}
	} );
};

function hideEditHandlerSGO () {
	if ( !dimensionManager.EditHandleSGOObjects ) {
		return;
	}
	var obj = dimensionManager.EditHandleSGOObjects;
	Object.keys( obj ).forEach( dimensionId => {
		if ( obj[ dimensionId ] ) {
			Object.keys( obj[ dimensionId ] ).forEach( key => {
				// removeSGO api will remove all event handlers
				viewerManager.removeSGO( obj[ dimensionId ][ key ] );
			} );
		}
	} );

	dimensionManager.EditHandleSGOObjects = null;
};


function registerDimensionHoverChangedHandler ( dimManager ) {
	if ( !dimManager ) {
		return;
	}

	dimManager.addEventListener( PLMVisWeb.Dimension.EventTypes.HoverChanged, function ( eventData ) {
		if ( !eventData ) { return };

		if ( !dimensionManager.SGOObjects ) {
			dimensionManager.SGOObjects = {};
		}

		var dimensionId = eventData.dimensionId;
		var pointTypes = [
			{ pointType: 'TextCenter' },
			{ pointType: 'ArrowLineEnd', side: 'Side1' },
			{ pointType: 'ArrowLineEnd', side: 'Side2' },
			{ pointType: 'ArrowHeadStart', side: 'Side1' },
			{ pointType: 'ArrowHeadStart', side: 'Side2' },
			{ pointType: 'ArrowLineStart', side: 'Side1' },
			{ pointType: 'TextStart' },
		];
		var controlPoints = dimensionManager.getControlPoints( dimensionId, pointTypes );
		if ( eventData.highlight && eventData.dimensionComponentType === 'Text' && controlPoints[ 0 ].value && controlPoints[ 1 ].value ) {
			var textSide = calculateTextSideByPoint( dimensionId, controlPoints, eventData.point );
			showHoverSGOByTextSide( dimensionId, controlPoints, textSide );
		} else {
			hideHoverSGO();
		}
	} );
};

function updateEditHandles ( dimId ) {
	if ( dimId instanceof Array ) {
		dimId.forEach( id => {
			_updateEditHandles( id );
		} );
	} else {
		_updateEditHandles( dimId );
	}
};

function _updateEditHandles ( dimId ) {
	if ( !dimensionManager.EditHandleSGOObjects || !dimensionManager.EditHandleSGOObjects[ dimId ] ) {
		return;
	}
	var pointTypes = [
		{ pointType: 'TextCenter' },

		{ pointType: 'ArrowHeadStart', side: 'Side1' },
		{ pointType: 'ArrowHeadStart', side: 'Side2' },
		{ pointType: 'ArrowLineStart', side: 'Side1' },
		{ pointType: 'ArrowLineStart', side: 'Side2' },

		{ pointType: 'ExtensionLineStart', side: 'Side1' },
		{ pointType: 'ExtensionLineStart', side: 'Side2' },
		{ pointType: 'ExtensionLineAnchor', side: 'Side1' },
		{ pointType: 'ExtensionLineAnchor', side: 'Side2' },

		{ pointType: 'StubLineStart' },
		{ pointType: 'StubLineEnd' }
	];
	var controlPoints = dimensionManager.getControlPoints( dimId, pointTypes );
	showEditHandlerSGO( dimId, controlPoints );
}

function registerDimensionPositionChangedHandler ( dimManager ) {
	if ( !dimManager ) {
		return;
	}

	dimManager.addEventListener( PLMVisWeb.Dimension.EventTypes.PositionChanged, function ( eventData ) {
		if ( !eventData ) { return };

		if ( !dimensionManager.SGOObjects ) {
			dimensionManager.SGOObjects = {};
		}
		hideHoverSGO();

		updateEditHandles( eventData.dimensionId );
	} );
};

function registerDimensionVisibilityChangedHandler ( dimManager ) {
	if ( !dimManager ) {
		return;
	}

	dimManager.addEventListener( PLMVisWeb.Dimension.EventTypes.VisibilityChanged, function ( eventData ) {
		if ( !eventData ) { return };

		updateEditHandles( eventData.dimensionIds || eventData.dimensionId );
	} );
};

function registerDimensionEventHandlers ( dimensionMgr ) {
	registerDimensionSelectionChangedHandler( dimensionMgr );
	registerDimensionHoverChangedHandler( dimensionMgr );
	registerDimensionPositionChangedHandler( dimensionMgr );
	registerDimensionVisibilityChangedHandler( dimensionMgr );
}


/**
 * calculateTextSide based on the current mouse point
 *
 * Support all types of TextOrientation, eg: Aligned, Perpendicular
 *
 * @param {Integer} dimensionId the dimensionId
 * @param {Array} controlPoints the controlPoints
 * @param {Array} point the current crosspoint between ray and text model
 */
function calculateTextSideByPoint ( dimensionId, controlPoints, point ) {
	if ( !controlPoints[ 2 ].value ) {
		return "SIDE1";
	}

	var textHeight = dimensionManager.getDimensionTextSize( dimensionId ).height;

	var tolerance = textHeight;
	var textStart = new THREE.Vector3().fromArray( controlPoints[ 6 ].value );
	var textCenter = new THREE.Vector3().fromArray( controlPoints[ 0 ].value );

	var centerToStart = Math.sqrt( textCenter.distanceToSquared( textStart ) );
	var pointToStart = Math.sqrt( point.distanceToSquared( textStart ) );
	var min = centerToStart - tolerance;
	var max = centerToStart + tolerance;
	if ( pointToStart < min ) {
		return "SIDE1";
	} else if ( pointToStart > max ) {
		return "SIDE2";
	} else {
		return "CENTER";
	}
};

var _map = {
	CENTER: [ 'Side1_Cone', 'Side2_Cone' ],
	SIDE1: [ 'Side1_Cone', 'Side2_Sphere' ],
	SIDE2: [ 'Side1_Sphere', 'Side2_Cone' ],
};

function showHoverSGOByTextSide ( dimensionId, controlPoints, textSide ) {
	hideHoverSGO();

	_map[ textSide ].forEach( item => {
		// Side2_Cone need apply flip
		showSGOObject( dimensionId, controlPoints, item, item === 'Side2_Cone' );
	} );
};

function hideHoverSGO () {
	if ( !dimensionManager.SGOObjects ) {
		return;
	}

	var obj = dimensionManager.SGOObjects;
	Object.keys( obj ).forEach( key => {
		if ( obj[ key ].visible ) {
			obj[ key ].visible = false;
		}
	} );
};

function showSGOObject ( dimensionId, controlPoints, key, flipWhenCreate ) {
	var startPoint = controlPoints[ 1 ];
	var endPoint = controlPoints[ 2 ].value ? controlPoints[ 2 ] : controlPoints[ 5 ];
	var startPointValue = startPoint.value;
	var endPointValue = endPoint.value;


	var splitData = key.split( /_/ );
	var side = splitData[ 0 ];
	var sgoType = splitData[ 1 ];

	var sgo = dimensionManager.SGOObjects[ key ];
	if ( !sgo ) {
		dimensionManager.SGOObjects[ key ] = sgo = createSGO( 'DimensionHoverHandle', flipWhenCreate, { type: sgoType } );
		sgo.manager = dimensionManager;
	} else {
		sgo.visible = true;
	}

	// update controlPointInfo
	var isStartPoint = side === 'Side1';
	var controlPointInfo = {
		dimensionId: dimensionId,
		pointType: isStartPoint ? startPoint.pointType : endPoint.pointType,
		side: isStartPoint ? startPoint.side : endPoint.side,
		value: isStartPoint ? startPoint.value : endPoint.value,
	};
	var modelPoint = sgo.controlPointInfo = controlPointInfo;
	var modelPointValue = modelPoint.value;

	var position2d = viewerManager.projectModelPointToViewCoordinate( modelPointValue[ 0 ], modelPointValue[ 1 ], modelPointValue[ 2 ] );
	sgo.setPosition( position2d[ 0 ], position2d[ 1 ] );

	// update direction to align with dimensionLine for Cone case.
	if ( sgoType === 'Cone' ) {
		var projectedStartPoint = viewerManager.projectModelPointToViewCoordinate( startPointValue[ 0 ], startPointValue[ 1 ], startPointValue[ 2 ] );
		var projectedEndPoint = viewerManager.projectModelPointToViewCoordinate( endPointValue[ 0 ], endPointValue[ 1 ], endPointValue[ 2 ] );
		var startArrowEnd = controlPoints[ 3 ].value;
		var endArrowEnd = controlPoints[ 4 ].value ? controlPoints[ 4 ].value : controlPoints[ 5 ].value;
		var projectedStartArrowEnd = viewerManager.projectModelPointToViewCoordinate( startArrowEnd[ 0 ], startArrowEnd[ 1 ], startArrowEnd[ 2 ] );
		var projectedEndArrowEnd = viewerManager.projectModelPointToViewCoordinate( endArrowEnd[ 0 ], endArrowEnd[ 1 ], endArrowEnd[ 2 ] );
		sgo.setDirection( flipWhenCreate ? projectedEndArrowEnd : projectedStartPoint, flipWhenCreate ? projectedEndPoint : projectedStartArrowEnd );
	}
};


function createSGO ( type, flip, options ) {
	var item = null;
	if ( type === 'DimensionHoverHandle' ) {
		if ( options.type === 'Sphere' ) { options.size = { width: 10, height: 10 } };
		item = new DimensionHoverHandle( options.type, options.color, options.size );
		if ( flip ) {
			item.flip = true;
		}
	} else if ( type === 'DimensionEditHandle' ) {
		item = new DimensionEditHandle( options.color, options.enableDrag, options.visible, options.size, options.checkTextFn );
	}
	viewerManager.addSGO( item );
	return item;
}
