// Global Variables
var currentLayer  = null;
var map = null;  

var mapCanvas;
var points;
var points_bounds;

//Variables under the context of Web GL 
var gl;
var pointProgram;
var pointArrayBuffer;

var pixelsToWebGLMatrix = new Float32Array(16);
var mapMatrix = new Float32Array(16);
var pi_180 = Math.PI / 180.0;
var pi_4 = Math.PI * 4;

var classes = { 1: '#FF0000',
				3: '#CC0000',
				7: '#9900FF',
				100: '#6500FF',
				200: '#3200FF',
				2000: '#0000FF' 
			 }; 

var nSElements = new Array();
var nSyntheses = new Array();
var totalElems = new Array();
var gridSizes =  new Array();
var reductionRates = new Array();
var zoomArray = new Array();
var processedTimes = new Array();

			 
var lineChartData = null;
var chart1 = null;
var chart2 = null;
var chart3 = null;
var currGridSize = null;
var testes = null;
var zoomMap = null;
var ischanged = false;

var isZoomChanged = false;


//var myip = "10.0.0.42";
var myip = "localhost";
var myport = "8080";
var dataset = ($("#dataset").val() != undefined) ? $("#dataset").val() : "accidents";

function init() {
	
	
	init_map();
	getInitialInfo();			
	updateZoom();
	zoomMap = map.getZoom();

	$("#refresh-button").click(function(){
		$.ajax({
			  url: "http://"+ myip + ":" + myport + "/status"
			})
			  .done(function( data ) {
				$("#status").html("<pre>"+data+"</pre>");
			  });
	});	
}

			 
function init_map() {
	console.log("Leaflet: " + L.version);

	if (map!= undefined) {
		map.remove();
		$("#map").empty();
		map = null;

		refresh_everything();
	} 

	map = new L.Map('map', {center: new L.LatLng(41, -95), zoom: 4});

	
	L.tileLayer('http://{s}.tile.cloudmade.com/d4fc77ea4a63471cab2423e66626cbb6/9/256/{z}/{x}/{y}.png', {
		attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery � <a href="http://cloudmade.com">CloudMade</a>',
		maxZoom: 18
	}).addTo(map);
	
	var googleLayer = new L.Google('ROADMAP');
    map.addLayer(googleLayer);
	
	map.on('move', drawPoints);
    
	createMapCanvas();
	
	/*map.on('zoomstart', startingChange);
    map.on('dragstart', startingChange);
	map.on('zoomend', onChangedZoom);
	map.on('dragend', onEndingPan);*/
    
//	loadCanvas();
}

function createMapCanvas() {
	
	 // Fill the path
	
	mapCanvas = document.createElement('canvas');
	mapCanvas.id = 'mapCanvas'
		mapCanvas.style.position = 'absolute';
	
	
	mapCanvas.height = map.getSize().y;
	mapCanvas.width = map.getSize().x;

	var mapDiv = map.getContainer();
	mapDiv.appendChild(mapCanvas);
}

/*function loadCanvas() {
	canvas = document.createElement('canvas');
	canvas.id = 'pointscanvas';		
    canvas.height = map.getSize().y;
    canvas.width = map.getSize().x;

    console.log(map);
    var mapDiv = map.getContainer();
    mapDiv.appendChild(canvas);    
}



function clearCanvas() {
    var context = canvas.getContext("2d");
    context.clearRect(0, 0, canvas.width, canvas.height);
}

function drawCanvas() {
    //var date1 = new Date().getTime();
	
    var context = canvas.getContext("2d");
    var bounds = map.getBounds();

    var maxLatitude = bounds.getNorth();
    var minLatitude = bounds.getSouth();
    var maxLongitude = bounds.getEast();
    var minLongitude = bounds.getWest();

    var imageData = context.createImageData(canvas.width, canvas.height);

    var pointsDrawn = 0;

    for (var i = 0; i < points.length; i++) {

        var point = points[i];
        var loc = point.coords;

        //discard coordinates outside the current map view
        if (loc.lat >= minLatitude && loc.lat <= maxLatitude && 
            loc.lng >= minLongitude && loc.lng <= maxLongitude) {
            pointsDrawn++;
        	var pixelCoordinate = map.latLngToContainerPoint( point.coords );
        	var a = (255*point.opacity) | 0;
        	var r = hexToR(point.color);
        	var g = hexToG(point.color);
        	var b = hexToB(point.color);
        	
            setPixel(imageData, pixelCoordinate.x, pixelCoordinate.y, r, g, b, a);
            
        } else {
        	console.log("fora: " + point);
        }
    }

    //var date2 = new Date().getTime();
    //addMessage(pointsDrawn + " Points Drawn", date2-date1);
    context.putImageData(imageData, 0, 0);
}

function setPixel(imageData, x, y, r, g, b, a) {
    //find the pixel index based on it's coordinates
    index = (x + y * imageData.width) * 4;

    imageData.data[index + 0] = r;
    imageData.data[index + 1] = g;
    imageData.data[index + 2] = b;
    imageData.data[index + 3] = a;
}

function hexToR(h) {return parseInt((cutHex(h)).substring(0,2),16)}
function hexToG(h) {return parseInt((cutHex(h)).substring(2,4),16)}
function hexToB(h) {return parseInt((cutHex(h)).substring(4,6),16)}
function cutHex(h) {return (h.charAt(0)=="#") ? h.substring(1,7):h}
*/


// Highlight Point in a jqplot
function doHighlight(plot, pos, indserie,chartind) {
		var seriesIndex = indserie; 
        var data = plot.series[seriesIndex].data;
        var pointIndex = pos;
        var x = plot.axes.xaxis.series_u2p(data[pointIndex][0]);
        var y = plot.axes.yaxis.series_u2p(data[pointIndex][1]);
        var r = 8;
        var drawingCanvas = $(".jqplot-highlight-canvas")[chartind]; //$(".jqplot-series-canvas")[0];
        var context = drawingCanvas.getContext('2d');
        context.clearRect(0, 0, drawingCanvas.width, drawingCanvas.height); //plot.replot();            
        context.strokeStyle = "#000000";
		context.globalAlpha = 0.5;
        context.fillStyle = "#999999";
        context.beginPath();
        context.arc(x, y, r, 0, Math.PI * 2, false);
        context.closePath();
        context.fill();
}

function highlightCharts() {
	zoom = map.getZoom();
	
	// Find Pos
	ind = 0;
	for(i = 0; i < zoomArray.length; i++) {
		if(zoom == zoomArray[i]) {
			currGridSize = gridSizes[i];
			ind = i;
			break;
		}
	}
	
	if(!ischanged) // para o caso de se alterar a grelha manualmente
		setGridSize(ind);
	else {
		setGridSize(document.getElementById('combo').selectedIndex)
		ind = document.getElementById('combo').selectedIndex;
	}
		
	doHighlight(chart2, ind, 0, 1);
	doHighlight(chart3, ind, 0, 2);
	
	for(i = 0; i < 3; i++) {
		doHighlight(chart1, ind, i, 0);
	}
}

/*function startingChange(e) {
	if (points !=  undefined) {
		var start_remove_layer = new Date();
		console.log("removing layer...");
 		clearCanvas();
		var finish_remove_layer = new Date();
		console.log("Removed Layer time: " + (finish_remove_layer - start_remove_layer));
	}
}

function onEndingPan(e) {
	onChangedAux(e);
}

function onChangedZoom(e) {
	isZoomChanged = true;
	onChangedAux(e);
	isZoomChanged = false;
}

function onChangedAux(e) {
	updateZoom();
		
	bounds = map.getBounds(); // AQUI TENHO A BOUNDING BOX DO MAPA, FALTA AGORA ENVIAR PARA O SERVIDOR. E DEPOIS O QUE FAZER?
	console.log(bounds);
	
	sw = bounds._southWest.lng + "," + bounds._southWest.lat;
	ne = bounds._northEast.lng + "," + bounds._northEast.lat;
	
	if(!ischanged) {
		zoom = map.getZoom();
		requesturl = "http://"+ myip + ":" + myport + "/data/" + dataset + "?zoom=" + zoom + "&sw=" + sw + "&ne=" + ne;
		console.log("url: " + requesturl);
	}
	else {
		gridsizeSelected = document.getElementById('combo').selectedIndex;
		
		console.log(map.getZoom() + "<" +  zoomMap)
		
		if(map.getZoom() > zoomMap && (gridsizeSelected -1) >= 0) {
			gridsize = gridSizes[gridsizeSelected - 1];
			setGridSize(gridsizeSelected - 1);
		}
		
		if(map.getZoom() < zoomMap && (gridsizeSelected +1) < gridSizes.length) {
			gridsize = gridSizes[gridsizeSelected + 1];
			setGridSize(gridsizeSelected + 1);
		}
		
		if(gridsize !=null)
			requesturl = "http://"+ myip + ":" + myport + "/data/" + dataset + "?zoom=" + zoom + "&gridSize=" + gridsize + "&sw=" + sw + "&ne=" + ne;
			
	}
	zoomMap = map.getZoom();
	(requesturl);
	highlightCharts();
}*/

/*function findClass(value) {
	var keys = Object.keys(classes);
	var oldvalue = keys[0];
	for(v in keys) {
		if(keys[v] >= value) {
			break;
		}
		else oldvalue = keys[v] 
	}
	return classes[oldvalue];
}*/

/*function giveColor(feature) {
	var color  = null;
	var values = feature.p.v;
	if (feature.p.s == "true") {
		var soma = 0;
		for(synthesis in values) {
			soma += parseInt(values[synthesis][1]);
		}
		color = findClass(soma);
	}
	else color = '#FF0000';
	return color;
}*/

function changeGridSize() {
	ischanged = true;
	
	if(currentLayer !=  undefined) {
		var start_remove_layer = new Date();
		console.log("removing layer...");
 		clearCanvas();
		var finish_remove_layer = new Date();
		console.log("Removed Layer time: " + (finish_remove_layer - start_remove_layer));
	}

	zoom = map.getZoom();
	bounds = map.getBounds();
	selectedgridsize = document.getElementById('combo').options[document.getElementById('combo').selectedIndex].value;
	
	sw = bounds._southWest.lng + "," + bounds._southWest.lat;
	ne = bounds._northEast.lng + "," + bounds._northEast.lat;

	requesturl = "http://"+ myip + ":" + myport + "/data/" + dataset + "?zoom=" + zoom + "&gridSize=" + selectedgridsize + "&sw=" + sw + "&ne=" + ne;
	
	console.log(requesturl);
	
	getGeoJSON(requesturl);
	highlightCharts();
	
	currGridSize = selectedgridsize; //Update curr gridsize
	ischanged = false;
}

function numberElementsVisible() {
	$("#numelements" ).val("...");
	
	bounds = map.getBounds();
	
	sw = bounds._southWest.lng + "," + bounds._southWest.lat;
	ne = bounds._northEast.lng + "," + bounds._northEast.lat;
	
	
	var requesturl = "http://"+ myip + ":" + myport + "/nrelements/" + dataset + "?gridSize=" + currGridSize + "&sw=" + sw + "&ne=" + ne;
	console.log(requesturl);
	
	$.getJSON(requesturl, function(data) {
		console.log(data)
		$("#numelements" ).val(data.nspatialobjects);
	});
}

/*function isCanvasValid() {
	console.log(isZoomChanged);
	if ((points_bounds!=undefined) && (map.getBounds().contains(points_bounds)) && !isZoomChanged && !ischanged) {
		return true;
	} else {
		return false;
	}
}*/

function getGeoJSON(url) {
	if(map!=null) {
		var start_time = new Date();
		console.log("request geodata: " + url);

		$.getJSON(url, function(data) {
			var getRequest_time = new Date();
			points = data;
			drawCanvas(data);

			var finishRequest_time = new Date();
			console.log("Apos a funcao de desenho: " + (finishRequest_time - start_time));

		});
	}
}

function drawCanvas(geojson) {
//	clearCanvas();
	points = geojson;
	console.log("HERE");
	
	// Draw using WebGL
	webGLStart(null);
	pixelsToWebGLMatrix.set([2/mapCanvas.width, 0, 0, 0, 0, -2/mapCanvas.height, 0, 0, 0, 0, 0, 0, -1, 1, 0, 1]);
	
	drawPoints();
	
	numberFeatures = 0;
	mapCanvas.style.zIndex='10';
}

function webGLStart() {
	var canvas = document.getElementById("mapCanvas");
	
	initGL(canvas);
	resize();
	initShaders();
	initBuffers();
}

function resize() {
	gl.viewport(0, 0, map.getContainer().offsetWidth, map.getContainer().offsetHeight);
}

var pi_180 = Math.PI / 180.0;
var pi_4 = Math.PI * 4;

function latLongToPixelXY(latitude, longitude) {

	var sinLatitude = Math.sin(latitude * pi_180);
	var pixelY = (0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) /(pi_4)) * 256;
	var pixelX = ((longitude + 180) / 360) * 256;

	var pixel =  { x: pixelX, y: pixelY};
	
	return pixel;
}



var pointArrayBuffer;
var colorsArrayBuffer;
var attributeLoc;
var vertexColorAttribute;
var colors;

function initBuffers(e) {
	//Custom Load
	var colrs = 0;
	
	points = points.points;
	points[1].p.v[3]
	
	var rawData = new Float32Array( (2 * points.length) );
	colors = new Float32Array( (4 * points.length) );
	
	for (var i = 0; i < points.length - 1; i++) {
		var loc = points[i].g.c;
		
		var pixelCoordinate = latLongToPixelXY(loc[1], loc[0] );
		
		rawData[(i * 2)] = pixelCoordinate.x;
		rawData[(i * 2) + 1] = pixelCoordinate.y;
		
		var value;
		if (points[i].p.s == "true") {
			value = points[i].p.v[[0]][1];
			
		}
		else {
			value = points[i].p.v[1]; //counts
		}
		
//		colors[(i * 4)] = (255/255);
//		colors[(i * 4) + 1] = (0/255);
//		colors[(i * 4) + 2] = (0/255);
//		colors[(i * 4) + 3] = 1;
//		
	
		if(value == 1) {
//			console.log("cores");
			colors[(i * 4)] = (255/255);
			colors[(i * 4) + 1] = (0/255);
			colors[(i * 4) + 2] = (0/255);
			colors[(i * 4) + 3] = 0.3;
			colrs++;
			
		}
		else if(value > 1 && value <= 2) {
//			console.log("cores menos de 10	");
			colors[(i * 4)] = (255/255);
			colors[(i * 4) + 1] = (0/255);
			colors[(i * 4) + 2] = (0/255);
			colors[(i * 4) + 3] = 0.5;
			colrs++;
		}
		else if (value > 2) { 
			colors[(i * 4)] = (255/255);
			colors[(i * 4) + 1] = (0/255);
			colors[(i * 4) + 2] = (0/255);
			colors[(i * 4) + 3] = 0.8;
			colrs++;
		}
		
//		if(value == 1) {
////			console.log("cores");
//			colors[(i * 4)] = (254/255);
//			colors[(i * 4) + 1] = (204/255);
//			colors[(i * 4) + 2] = (178/255);
//			colors[(i * 4) + 3] = 0.3;
//			colrs++;
//			
//		}
//		else if(value > 1 && value <= 3) {
////			console.log("cores menos de 10	");
//			colors[(i * 4)] = (253/255);
//			colors[(i * 4) + 1] = (141/255);
//			colors[(i * 4) + 2] = (60/255);
//			colors[(i * 4) + 3] = 1;
//			colrs++;
//		}
//		else if (value > 3) { 
//			colors[(i * 4)] = (189/255);
//			colors[(i * 4) + 1] = (0/255);
//			colors[(i * 4) + 2] = (38/255);
//			colors[(i * 4) + 3] = 1;
//			colrs++;
//		}
//		
	}
	console.log("Number of Colors: " + colrs);
	
	// tell webgl how buffer is laid out (pairs of x,y coords)
	
	// enable the 'worldCoord' attribute in the shader to receive buffer
	var attributeLoc = gl.getAttribLocation(pointProgram, 'worldCoord');
	gl.enableVertexAttribArray(attributeLoc);
	
	var vertexColorAttribute = gl.getAttribLocation(pointProgram, "aVertexColor");
    gl.enableVertexAttribArray(vertexColorAttribute);
    
	// create webgl buffer, bind it, and load rawData into it
	pointArrayBuffer = gl.createBuffer();
	gl.bindBuffer(gl.ARRAY_BUFFER, pointArrayBuffer);
	gl.bufferData(gl.ARRAY_BUFFER, rawData, gl.STATIC_DRAW);

	pointArrayBuffer.itemSize = 2;
	pointArrayBuffer.numItems = points.length;
	
	//colors
	colorsArrayBuffer = gl.createBuffer();
	gl.bindBuffer(gl.ARRAY_BUFFER, colorsArrayBuffer);
	gl.bufferData(gl.ARRAY_BUFFER, colors, gl.STATIC_DRAW);
//	
	colorsArrayBuffer.itemSize = 4;
	colorsArrayBuffer.numItems = points.length;
	
	gl.bindBuffer(gl.ARRAY_BUFFER, pointArrayBuffer);
    gl.vertexAttribPointer(attributeLoc, pointArrayBuffer.itemSize, gl.FLOAT, false, 0, 0);
  
    console.log(colorsArrayBuffer.itemSize);
  
    gl.bindBuffer(gl.ARRAY_BUFFER, colorsArrayBuffer);
    gl.vertexAttribPointer(vertexColorAttribute, colorsArrayBuffer.itemSize, gl.FLOAT, false, 0, 0);

	
}


function initGL(canvas) {
	try {
		gl = canvas.getContext('webgl');
		gl.viewport(0, 0, canvas.width, canvas.height);
		
		gl.disable(gl.DEPTH_TEST);
	} catch(e) {
	}
	if (!gl) {
		alert("Could not initialise WebGL, sorry :-( ");
	}
}

function initShaders() {
	var fragmentShader = getShader(gl, "pointFragmentShader");
	var vertexShader = getShader(gl, "pointVertexShader");
	
	// link shaders to create our program
	pointProgram = gl.createProgram();
	gl.attachShader(pointProgram, vertexShader);
	gl.attachShader(pointProgram, fragmentShader);
	gl.linkProgram(pointProgram);

	gl.useProgram(pointProgram);

	gl.aPointSize = gl.getAttribLocation(pointProgram, "aPointSize");
}


function getShader(gl, id) {
	var shaderScript = document.getElementById(id);
	if (!shaderScript) {
		return null;
	}

	var str = "";
	var k = shaderScript.firstChild;
	while (k) {
		if (k.nodeType == 3)
			str += k.textContent;
		k = k.nextSibling;
	}

	var shader;
	if (shaderScript.type == "x-shader/x-fragment") {
		shader = gl.createShader(gl.FRAGMENT_SHADER);
	} else if (shaderScript.type == "x-shader/x-vertex") {
		shader = gl.createShader(gl.VERTEX_SHADER);
	} else {
		return null;
	}

	gl.shaderSource(shader, str);
	gl.compileShader(shader);

	if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
		alert(gl.getShaderInfoLog(shader));
		return null;
	}

	return shader;
}  

function scaleMatrix(matrix, scaleX, scaleY) {
	// scaling x and y, which is just scaling first two columns of matrix
	matrix[0] *= scaleX;
	matrix[1] *= scaleX;
	matrix[2] *= scaleX;
	matrix[3] *= scaleX;

	matrix[4] *= scaleY;
	matrix[5] *= scaleY;
	matrix[6] *= scaleY;
	matrix[7] *= scaleY;
}

function translateMatrix(matrix, tx, ty) {
	// translation is in last column of matrix
	matrix[12] += matrix[0]*tx + matrix[4]*ty;
	matrix[13] += matrix[1]*tx + matrix[5]*ty;
	matrix[14] += matrix[2]*tx + matrix[6]*ty;
	matrix[15] += matrix[3]*tx + matrix[7]*ty;
}

function drawPoints(e) {
	if (points == null) return;
		
//	gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
	gl.enable(gl.BLEND);
	gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
	
	var currentZoom = map.getZoom();
	var pointSize = Math.max(currentZoom - 5.0, 1.0);
	gl.vertexAttrib1f(gl.aPointSize, pointSize);

	/**
	 * We need to create a transformation that takes world coordinate
	 * points in the pointArrayBuffer to the coodinates WebGL expects.
	 * 1. Start with second half in pixelsToWebGLMatrix, which takes pixel
	 *     coordinates to WebGL coordinates.
	 * 2. Scale and translate to take world coordinates to pixel coords
	 * see https://developers.google.com/maps/documentation/javascript/maptypes#MapCoordinate
	 */
	
	// copy pixel->webgl matrix
	mapMatrix.set(pixelsToWebGLMatrix);
	
	// Scale to current zoom (worldCoords * 2^zoom)
	var scale = Math.pow(2, currentZoom);
	scaleMatrix(mapMatrix, scale, scale);
	
	var offset = latLongToPixelXY(map.getBounds().getNorthWest().lat, map.getBounds().getNorthWest().lng);
	translateMatrix(mapMatrix, -offset.x, -offset.y);
	
	// attach matrix value to 'mapMatrix' uniform in shader
	var matrixLoc = gl.getUniformLocation(pointProgram, 'mapMatrix');
	gl.uniformMatrix4fv(matrixLoc, false, mapMatrix);
	
	
	// draw!
	gl.drawArrays(gl.POINTS, 0, points.length);
}

/*function (url) {
	
	if(map!=null) {
		var start_time = new Date();
		console.log("start request");
	
		console.log(url);
		
		if ( isCanvasValid() )  {
			clearCanvas();
			drawCanvas();
		} else {
			$("#info").text("Loading ...");
			$.getJSON(url)			
				.done(function( data, status, jqxhr ) {
					var getRequest_time = new Date();
	
					console.log("Request process time: " + (getRequest_time - start_time) + " ms for " + (data.points.length - 1) + " features");
					
					points = new Array();
					points_bounds = undefined;
					
					for (var i=0;i<data.points.length-1;i++) {
						object = data.points[i].g.c;
						feature = data.points[i];
						
						var coord = new L.LatLng(object[1], object[0]);
						point = { "coords": coord, "color": giveColor(feature), "opacity": 0.7 }
						
						if (points_bounds!=undefined) { 
							points_bounds.extend(coord);
						} else {
							points_bounds = new L.LatLngBounds(coord, coord);
						}
						
						points.push(point);
					}	
					
					var finishRequest_time = new Date();
					console.log("Layer Create time: " + (finishRequest_time - getRequest_time));
					
					clearCanvas();
					drawCanvas();
					
					var ntime = new Date();
					console.log("Layer Render time: " + (ntime - finishRequest_time));
					
					$("#info").text("Get+Creat+Render: " + (ntime - start_time) + " ms for " + (data.points.length - 1) + " features");					
				  })
				 .fail(function( jqxhr, textStatus, error ) {
					var getRequest_time = new Date();
				    var err = textStatus + ", " + error;
				    console.log( "Request Failed: " + err + "in "+ (getRequest_time - start_time) + " ms");
				 });
		}
	}
}*/


function findGridSizeInPlot(plot, gridSize) {
	 var data = plot.series[seriesIndex].data;
}

function fillComboBox() {
	var combo = document.getElementById('combo');

	for(i = 0; i < gridSizes.length; i++) {
	
		var option = document.createElement("option");
		option.text = gridSizes[i];
		option.value = gridSizes[i];
		try {
			combo.add(option, null); //Standard 
		}catch(error) {
			combo.add(option); // IE only
		}
	}
	
}

function getInitialInfo() {
	var url = "http://"+ myip + ":" + myport + "/info/" + dataset;
	$.getJSON(url, function(data) {
		fillInitialStats(data);
		drawCharts();
		fillComboBox();
	});
}

function fillInitialStats(data) {
	console.log(data.length)
	for(i = 0; i < data.length; i++) {
		gridSizes[i] = data[i].gridSize + "";
		
		gridSize = gridSizes[i];
		expoent = (Math.log(gridSize))/(Math.log(2))
		
        nSElements[i] = new Array(expoent + "", data[i].NSingularElements);
		nSyntheses[i] = new Array(expoent + "", data[i].NSyntheses);
		totalElems[i] = new Array(expoent + "", data[i].totalElements);
		zoomArray[i] = data[i].zoom;
		reductionRates[i] = new Array(expoent+ "", data[i].reductionRate);
		processedTimes[i] = new Array(expoent+ "", data[i].processedTime);
    }
}

function writeData(data) {
	
	var url = "http://"+ myip + ":" + myport + "/info/" + dataset;
	$.getJSON(url, function(data) {
		for(i = 0; i < data.length; i++) {
			console.log(data[i].reductionRate);
		}
	});
	
	
}

function drawCharts() {

	  chart1 = $.jqplot ('stats', [nSElements,nSyntheses,totalElems], {
		  seriesDefaults: {
			  rendererOptions: {
				  smooth: true
			  }
		  },
		  axes: {
			xaxis: {
			  label: "Grid Size",
			  pad:0,
			  renderer: $.jqplot.CategoryAxisRenderer,
			},
			yaxis: {
			 min: 0, 
			}
		  },  
		  series: [
					{	
						label: "Singular",
						color: '#6699cc',
					},
					{
						label: "Syntheses",
						color: '#ff6633',
					},
					{
						label: "All",
						color: 'green',
					},
				],
		  legend: {
				show: true,
				showLabels:true,
				showSwatch: true,
				placement: 'insideGrid',
				location: 'ne'
		  },
		  grid: {
			drawGridLines: true,
			gridLineColor: '#cccccc', 
			borderColor: '#000000',
			borderWidth: 0,
			 shadow: false	,
		  },
		  highlighter: {
			show: true,
			sizeAdjust: 7.5,
			tooltipLocation: 'ne',
			 tooltipAxes: 'y'
		  },
		  
		});
		
		 chart2 = $.jqplot ('stats2', [reductionRates], {
		  seriesDefaults: {
			  rendererOptions: {
				  smooth: true
			  }
		  },
		  axes: {
			xaxis: {
			  label: "Grid Size",
			  pad:0,
			  renderer: $.jqplot.CategoryAxisRenderer
			},
			yaxis: {
			 min: 0, 
			}
		  },  
		  series: [
					{
						label: "Reduction Percentage",
						color: '#cccc00',
					},
				],
		  legend: {
				show: true,
				showLabels:true,
				showSwatch: true,
				placement: 'insideGrid',
				location: 'ne',
		  },
		  grid: {
			drawGridLines: true,
			gridLineColor: '#cccccc', 
			borderColor: '#000000',
			borderWidth: 0,
			 shadow: false	,
		  },
		  highlighter: {
			show: true,
			sizeAdjust: 7.5,
			tooltipLocation: 'sw',
			 tooltipAxes: 'y'
		  },
		});

		 chart3 = $.jqplot ('stats3', [processedTimes], {
			  seriesDefaults: {
				  rendererOptions: {
					  smooth: true
				  }
			  },
			  axes: {
				xaxis: {
				  label: "Grid Size",
				  pad:0,
				  renderer: $.jqplot.CategoryAxisRenderer
				},
				yaxis: {
				 min: 0, 
				}
			  },  
			  series: [
						{
							label: "Processed Times",
							color: 'magenta',
						},
					],
			  legend: {
					show: true,
					showLabels:true,
					showSwatch: true,
					placement: 'insideGrid',
					location: 'ne',
			  },
			  grid: {
				drawGridLines: true,
				gridLineColor: '#cccccc', 
				borderColor: '#000000',
				borderWidth: 0,
				 shadow: false	,
			  },
			  highlighter: {
				show: true,
				sizeAdjust: 7.5,
				tooltipLocation: 'sw',
				 tooltipAxes: 'y'
			  },
			});
}

/*function getGeoJSON2(url) {
	if(map!=null) {
		zoom = map.getZoom();
		requesturl = url + "?request=data&zoom=" + zoom;
	
		console.log("start request");
	
		$.getJSON(requesturl, function(data) { 
			
			currentLayer = data;

			for (var i=0;i<currentLayer.features.length;i++) {
		
				object = currentLayer.features[i].geometry.coordinates;
				feature = currentLayer.features[i];
				
				point =  new L.LatLng(object[1], object[0]);
				
				var circle = new L.CircleMarker(point, {
						fill:true,
						fillColor: giveColor(feature),
						fillOpacity: 0.3,
						radius: 1,
						stroke:false
					});
					
				map.addLayer(circle);
			}	
		});
	}
}*/

function updateZoom() {
	$("#mapzoom").val( map.getZoom() );
}

function setGridSize(valueToSelect) {	
    var element = document.getElementById('combo');
    element.selectedIndex = valueToSelect;
}

function refresh_everything() {
	clearCanvas();
	nSElements = new Array();
	nSyntheses = new Array();
	totalElems = new Array();
	gridSizes =  new Array();
	reductionRates = new Array();
	zoomArray = new Array();
	processedTimes = new Array();
	lineChartData = null;
	chart1 = null;
	chart2 = null;
	chart3 = null;
	currGridSize = null;
	testes = null;
	zoomMap = null;
	ischanged = false;
	isZoomChanged = false;
	points = null;
	points_bounds = null;
	$("#stats").empty();
	$("#stats2").empty();
	$("#stats3").empty();
	$("#combo").empty();
	$("#info").empty();
}
