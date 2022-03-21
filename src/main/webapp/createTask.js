function changeAutoUpgradable(id, value) {
	if (value == true) {
		document.getElementById("field-overRide-" + id).style.display = "block"
	} else {
		document.getElementById("field-overRide-" + id).style.display = "none"
	}
}		

function displayParameters(id, value) {
	if (value == "JOB") {
		document.getElementById("parameterdiv-" + id).style.display = "block"
	} else {
		document.getElementById("parameterdiv-" + id).style.display = "none"
	}
}		

function displayRuntime(id, value) {
    if (value != "CLOUD") {
        document.getElementById("field-runtime-" + id).style.display = "block"
    } else {
        document.getElementById("field-runtime-" + id).style.display = "none"
    }
}       
