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
		"sAjaxSource": "/dbadmin/${table.name}/fetch",
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
		<a class="pull-right btn btn-success" href="/dbadmin/${table.name}/add"><i class="icon-plus-sign icon-white"></i>Add</a>
		<h1>${table.name}</h1>
	</div>
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
