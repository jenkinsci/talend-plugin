function changeAutoUpgradable(id, value) {
	if (value == true) {
		document.getElementById("field-overRide-" + id).style.display = "block"
	} else {
		document.getElementById("field-overRide-" + id).style.display = "none"
	}
}		

function displayRuntime(id, value) {
	if (value == "CLOUD" || value == "CLOUD_EXCLUSIVE") {
		document.getElementById("field-runtime-" + id).style.display = "none"
	} else {
		document.getElementById("field-runtime-" + id).style.display = "block"
	}
}		
