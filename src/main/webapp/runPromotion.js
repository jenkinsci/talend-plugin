function changeArtifactType(id, value) {
	console.log("id=" + id)
	console.log(value)
	if (value != "ENVIRONMENT" && value.length > 0) {
		document.getElementById("workspace-"+id).style.display = "block"
	} else {
		document.getElementById("workspace-"+id).style.display = "none"
	}		
	if (value == "PLAN") {
		document.getElementById("plan-"+id).style.display = "block"
	} else {
		document.getElementById("plan-"+id).style.display = "none"
	}		
	if (value == "FLOW") {
		document.getElementById("task-"+id).style.display = "block"
	} else {
		document.getElementById("task-"+id).style.display = "none"
	}		
	if (value == "ACTION") {
		document.getElementById("artifact-"+id).style.display = "block"
	} else {
		document.getElementById("artifact-"+id).style.display = "none"
	}
}    
