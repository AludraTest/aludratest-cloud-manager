/**
 * Configuration Help JavaScript
 * 
 * Implements inserting an additional table row when help button is clicked, and hiding it on second click.
 * 
 */

function toggleHelp(clientId, helpTextHtml) {
	
	// check if there already is a help text; remove it then
	var helpTextElement = $("*[id='" + clientId + "_helpText']");
	if (helpTextElement.length) {
		helpTextElement.remove();
	}
	else {
		var tr = $("*[id='" + clientId + "']").closest("tr");
		$('<tr class="config-helptext" id="' + clientId + '_helpText"><td colspan="3">' + helpTextHtml + "</td></tr>").insertAfter(tr);
	}
	
}