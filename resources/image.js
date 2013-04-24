function JappyConvertFileInput(input) {
	if (input.attr("data-folder") || input.attr("data-bucket")) {
		var fileUploaderElem = $("<div id='file-uploader'/>");
		
		var parent = input.parent();
		var name = input.attr("name");
		
		parent.append(fileUploaderElem);
		input.detach();

		var params = {};
		if (input.attr("data-folder"))
			params["folder"] = input.attr("data-folder");
		else
			params["bucket"] = input.attr("data-bucket");

		var multiple = false;
		if (input.attr("data-multiple")) {
			multiple = true;
		}
		var json = false;
		if (input.attr("data-json")) {
			json = true;
		}
		
		var uploader = new qq.FileUploader({
			element : fileUploaderElem[0],
			action : "/resource",
			onComplete : function(id, fileName, responseJSON) {
				var hiddenInput = $("<input type='hidden'/>");
				hiddenInput.attr("name", this.myname);
				if (json) {
					var o = {name : fileName, url : responseJSON.value };
					hiddenInput.attr("value", JSON.stringify(o));
				} else {
					hiddenInput.attr("value", responseJSON.value);
				}
				this.myparent.append(hiddenInput);
			},
			debug : true,
			params : params,
			multiple : multiple,
			myparent : parent,
			myname : name,
			myjson : json
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