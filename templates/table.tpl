<script type='text/javascript'>
$(document).ready(function() {
    $('#maintable').dataTable( {
        "sDom": "<'row-fluid'<'span6'l><'span6'f>r>t<'row-fluid'<'span6'i><'span6'p>>",
        "sPaginationType": "bootstrap",
		"oLanguage": {
			"sLengthMenu": "_MENU_ records per page"
		},
		"bProcessing": true,
		"bServerSide": true,
		"sAjaxSource": "${root}/dbadmin/${table.name}/fetch",
		"fnRowCallback": function( nRow, aData, iDisplayIndex ) {
              $('td:nth-child(2)', nRow).attr('nowrap','nowrap');
             return nRow;
         }
    } );
} );

</script>
<div class="container">
	[#if error??]
	<div class="alert alert-block">
  		<a class="close" data-dismiss="alert" href="#">x</a>
  		${error}
	</div>
	[/#if]
	<div class="page-header">
		<h1>${table.name}</h1>
	</div>
	<div class="row">
		<div class="span8">
			<table class="table-striped table-bordered table" id="maintable">
				<thead>
				<tr>
					[#list preview as column]
					<th>${column}</th>
					[/#list]
					<th>Action</th>
				</tr>
				</thead>
				<tbody>
				</tbody>
			</table>
		</div>
		<div class="span4">
			<form method="POST" action="../dbadmin/${table.name}/add">
				<fieldset>
					[#assign id = 0]
					[#list columns as column]
						[#assign id = id + 1]
					<div class="control-group">
						<label class="control-label" for="input${id}">${column.name}</label>
						<div class="controls">
							[#if column.type == "TEXT"]
							<input type="text" class="input-xlarge" id="input${id}" name="${column.name}">
							[#elseif column.type == "LONGTEXT"]
							<textarea class="input-xlarge" id="input${id}" rows="3"
								style="margin: 0px; width: 100%; height: 100px;" name="${column.name}"></textarea>
							[#elseif column.type == "INTEGER"]
							<input type="text" class="input-min" id="input${id}" name="${column.name}">
							[#elseif column.type == "DATE"]
							<input type="text" class="input-small" id="input${id}" name="${column.name}"><span class="help-inline">YYYY-MM-DD</span>							
							[#elseif column.type == "DATETIME"]
							<input type="text" class="input-small" id="input${id}" name="${column.name}"><span class="help-inline">YYYY-MM-DD HH:MM:SS</span>
							[#elseif column.type == "TIME"]
							<input type="text" class="input-small" id="input${id}" name="${column.name}"><span class="help-inline">HH:MM:SS</span>
							[#elseif column.type == "PHOTO"]
							<input type="file" name="${column.name}" data-prefix="/images/">
							[#elseif column.type == "REFERENCE"]
							<input type="text" class="input-small" id="input${id}" name="${column.name}" 
								data-source="${root}/dbadmin/${column.destTable}/lookup?&c=${column.destColumn}" data-value="">
								<span class="help-inline">Start typing and select</span>							
							[/#if]
						</div>
					</div>
					[/#list]
					<div class="form-actions">
            			<button type="submit" class="btn btn-primary">Save changes</button>
            			<button class="btn">Cancel</button>
          			</div>					
				</fieldset>
			</form>
		</div>
	</div>
</div>
