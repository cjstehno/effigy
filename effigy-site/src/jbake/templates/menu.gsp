	<!-- Fixed navbar -->
    <div class="navbar navbar-default navbar-fixed-top" role="navigation">
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
            <a class="navbar-brand" href="<% if (content.rootpath) { %>${content.rootpath}<% } else { %><% } %>">Effigy</a>
        </div>
        <div class="navbar-collapse collapse">
          <ul class="nav navbar-nav">
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>index.html">Home</a></li>
            <li><a href="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>about.html">About</a></li>
              <li><a href="<% if (content.rootpath) { %>${content.rootpath}<% } else { %><% } %>guide" target="_blank">User Guide</a></li>
            <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Reports <b class="caret"></b></a>
              <ul class="dropdown-menu">
                  <li><a href="tests" target="_blank">JUnit</a></li>
                  <li><a href="codenarc/main.html" target="_blank">CodeNarc (Main)</a></li>
                  <li><a href="codenarc/test.html" target="_blank">CodeNarc (Test)</a></li>
                  <li><a href="groovydoc" target="_blank">GroovyDoc</a></li>
              </ul>
            </li>
          </ul>
        </div><!--/.nav-collapse -->
      </div>
    </div>
    <div class="container">