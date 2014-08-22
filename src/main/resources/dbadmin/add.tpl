<div class="container">
	<div class="page-header">
		<h1>${table.name}</h1>
	</div>
	<div>
		<form method="POST" action="/dbadmin/${table.name}/add">
			<fieldset>
				[#assign id = 0]
				[#list columns as column]
					[#if !column.auto]
					
					[#assign id = id + 1]
				<div class="control-group">
					<label class="control-label" for="input${id}">${column.name}</label>
					<div class="controls">
						[#if column.type == "TEXT"]
						<input type="text" class="input-xlarge" id="input${id}" name="${column.name}" value="">
						[#elseif column.type == "LONGTEXT"]
						<textarea class="input-xlarge" id="input${id}" rows="3"
							style="margin: 0px; width: 100%; height: 100px;" name="${column.name}"></textarea>
						[#elseif column.type == "INTEGER"]						
						<input type="text" class="input-min" id="input${id}" name="${column.name}" value="">
						[#elseif column.type == "DATE"]
						<input type="text" class="input-small" id="input${id}" name="${column.name}" value=""><span class="help-inline">YYYY-MM-DD</span>							
						[#elseif column.type == "FILE"]
						<input type="file" name="${column.name}" data-folder="${column.folder}" data-value="">
						[#elseif column.type == "S3"]
						<input type="file" name="${column.name}" data-bucket="${column.bucket}" data-value="">
						[#elseif column.type == "REFERENCE"]
						<input type="text" class="input-small" id="input${id}" name="${column.name}" 
							data-source="/dbadmin/${column.destTable}/lookup?c=${column.destColumn}" 
							data-value=""
							value="">
							<span class="help-inline">Start typing and select</span>							
						[/#if]
					</div>
				</div>
					[/#if]
				[/#list]
				<div class="form-actions">
           			<button type="submit" class="btn btn-primary">Save changes</button>
           			<button class="btn">Cancel</button>
         			</div>					
			</fieldset>
		</form>
	</div>
