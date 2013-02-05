<script type='text/javascript'>
$(document).ready(function() {
} );

</script>
<div class="container">
	<div class="page-header">
		<h1><a href='/dbadmin/${table.name}'>${table.name}</a></h1>
	</div>
	<form class="form-horizontal" method="POST" action="/dbadmin/${table.name}/settings">
	[#list columns as column]
	[#if !column.auto]
	<div class="control-group">
		<label class="control-label">${column.name}</label>
		<div class="controls">
			<input type="checkbox" name="${column.name}" [#if column.admin]checked="true"[/#if]></input>
		</div>
	</div>
	[/#if]
	[/#list]
	    <div class="form-actions">
    		<button type="submit" class="btn btn-primary">Save changes</button>
    		<button type="button" class="btn">Cancel</button>
    	</div>
	</form>
</div>
