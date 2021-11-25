function changeAutoUpgradable(id, value) {
	if (value == true) {
		document.getElementById("field-overRide-" + id).style.display = "block"
	} else {
		document.getElementById("field-overRide-" + id).style.display = "none"
	}
}		
