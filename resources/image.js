$(document).ready(
		function() {
			var inputs = $("input[type='file']");
			if (inputs.length > 0) {
				for ( var i = 0; i < inputs.length; i++) {
					var input = $(inputs[i]);

					if (input.attr("data-prefix")) {

						var fileUploaderElem = $("<div id='file-uploader'/>");
						input.parent().append(fileUploaderElem);

						var hiddenInput = $("<input type='hidden'/>");
						hiddenInput.attr("name", input.attr("name"));
						hiddenInput.attr("value", input.attr("data-value"))
						input.parent().append(hiddenInput);

						input.detach();

						var uploader = new qq.FileUploader({
							element : fileUploaderElem[0],
							action : "/images",
							onComplete : function(id, fileName, responseJSON) {
								hiddenInput.attr("value", input
										.attr("data-prefix")
										+ responseJSON.value);
							},
							debug : true
						});
					}
				}
			}
		});