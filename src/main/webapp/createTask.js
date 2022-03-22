function changeAutoUpgradable(id, value) {
	if (value == true) {
		document.getElementById("field-overRide-" + id).style.display = "block"
	} else {
		document.getElementById("field-overRide-" + id).style.display = "none"
	}
}		

function displayParameters(id, value) {
	if (value == "STANDARD") {
		document.getElementById("parameterdiv-" + id).style.display = "block"
	} else {
		document.getElementById("parameterdiv-" + id).style.display = "none"
	}
	document.getElementById("field-artifact-" + id).getElementsByTagName('input')[0].value = ""
}		

function displayRuntime(id, value) {
    if (value != "CLOUD") {
        document.getElementById("field-runtime-" + id).style.display = "block"
    } else {
        document.getElementById("field-runtime-" + id).style.display = "none"
    }
}       
