function JappyConvertFileInput(input) {
	if (input.attr("data-folder") || input.attr("data-bucket")) {
		var fileUploaderElem = $("<div id='file-uploader'/>");
		input.parent().append(fileUploaderElem);

		var hiddenInput = $("<input type='hidden'/>");
		hiddenInput.attr("name", input.attr("name"));
		hiddenInput.attr("value", input.attr("data-value"))
		input.parent().append(hiddenInput);

		input.detach();

		var params = {};
		if (input.attr("data-folder"))
			params["folder"] = input.attr("data-folder");
		else
			params["bucket"] = input.attr("data-bucket");

		var uploader = new qq.FileUploader({
			element : fileUploaderElem[0],
			action : "/resource",
			onComplete : function(id, fileName, responseJSON) {
				this.hiddenInput.attr("value", responseJSON.value);
			},
			debug : true,
			params : params,
			hiddenInput : hiddenInput
		});
	}
}

$(document).ready(function() {
	var inputs = $("input[type='file']");
	if (inputs.length > 0) {
		for ( var i = 0; i < inputs.length; i++) {
			var input = $(inputs[i]);
			JappyConvertFileInput(input);
		}
	}
});