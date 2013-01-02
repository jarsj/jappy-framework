<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>Chatpat Movies</title>
    <!-- Le styles -->
    <link rel="stylesheet" href="/resource/class/css/bootstrap.css">
    <link rel="stylesheet" href="/resource/class/css/fileuploader.css">
    <link rel="stylesheet" href="/resource/class/css/jquery-ui.css">
    <link rel="stylesheet" href="/resource/class/css/datatable.css">
    
    <script src="/resource/class/jquery.min.js"></script>
    <script src="/resource/class/jquery-ui.min.js"></script>
    <script src="/resource/class/bootstrap.js"></script>
    <script src="/resource/class/fileuploader.js"></script>
    <script src="/resource/class/image.js"></script>
    <script src="/resource/class/reference.js"></script>
    <script src="/resource/class/jquery.dataTables.js"></script>
    <script src="/resource/class/datatable-bootstrap.js"></script>
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
              					<li><a href='/dbadmin/${table.name}'>${table.name}</a></li>
              					[/#list]
				             </ul>
            			</li>
            		</ul>
	        	</div>
    	  	</div>
    	</div>