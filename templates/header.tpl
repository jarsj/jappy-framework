<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Chatpat Movies</title>
    <!-- Le styles -->
    <link rel="stylesheet" href="${root}/resource/css/bootstrap.css">
    <link rel="stylesheet" href="${root}/resource/css/fileuploader.css">
    <link rel="stylesheet" href="${root}/resource/css/jquery-ui.css">
    <link rel="stylesheet" href="${root}/resource/css/datatable.css">
    
    <script src="${root}/resource/jquery.min.js"></script>
    <script src="${root}/resource/jquery-ui.min.js"></script>
    <script src="${root}/resource/bootstrap.js"></script>
    <script src="${root}/resource/fileuploader.js"></script>
    <script src="${root}/resource/image.js"></script>
    <script src="${root}/resource/reference.js"></script>
    <script src="${root}/resource/jquery.dataTables.js"></script>
    <script src="${root}/resource/datatable-bootstrap.js"></script>
    <style>
    body {
    	padding-top : 50px;
    }
    </style>
  </head>
  <body>
    	<div class="navbar navbar-fixed-top">
      		<div class="navbar-inner">
        		<div class="container">
		         	<a class="brand" href="#">Admin Panel</a>
           			<ul class="nav pull-right">
           				<li class="dropdown">
              				<a href="#" class="dropdown-toggle" data-toggle="dropdown">Tables <b class="caret"></b></a>
              				<ul class="dropdown-menu">
              					[#list tables as table]
              					<li><a href='${root}/dbadmin/${table.name}'>${table.name}</a></li>
              					[/#list]
				             </ul>
            			</li>
            		</ul>
	        	</div>
    	  	</div>
    	</div>