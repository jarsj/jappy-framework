$(document).ready(function () {
	var buttons = $("button");
	if (buttons.length > 0) {
		for (var i = 0; i < buttons.length; i++) {
			var button = $(buttons[i]);
			if (button.attr("data-action")) {
				var action = button.attr("data-action").split("-");
				if (action[0] == "submitform") {
					var formId = action[1];
				    button.click(formId, function (btnFrm) {
				    	$(this).hide();
				    	var frm = $("#" + btnFrm.data);
				    	var btn = this;
				        $.ajax({
				            type: frm.attr('method'),
				            url: frm.attr('action'),
				            data: frm.serialize(),
				            success: function (data) {
				                $(btn).show();
				            }
				        });
				        return false;
				    });
				}
			}
		}
	}
});