$(document).ready(function () {
	var inputs = $("input[type='text']");
	if (inputs.length > 0) {
		for (var i = 0; i < inputs.length; i++) {
			var input = $(inputs[i]);
			if (input.attr("data-source")) {
				var hidden = $("<input type='hidden'/>");
				hidden.attr("name", input.attr("name"));
				hidden.attr("value", input.attr("data-value"));
				input.attr("name", "");
				
				input.parent().append(hidden);
								
				input.autocomplete({
					source: input.attr("data-source"),
					minLength: 2
				});
				input.bind("autocompleteselect", { elem : hidden }, function( event, ui ) {
					if (ui.item) {
						event.data.elem.attr("value", ui.item.id);
					}
				});
			}
		}
	}
});