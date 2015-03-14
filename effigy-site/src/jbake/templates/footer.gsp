		</div>
		<div id="push"></div>
    </div>
    
    <div id="footer">
        <div class="container" style="color:#000000;">
            <p class="credit" style="text-align: center;">
                <a href="https://drone.io/github.com/cjstehno/effigy/latest"><img alt="Build Status"
                                                                                  src="https://drone.io/github.com/cjstehno/effigy/status.png"/></a> ~
            Copyright &copy; 2015 Christopher J. Stehno ~
            Mixed with <a href="http://getbootstrap.com/">Bootstrap v3.1.1</a> ~
            Baked with <a href="http://jbake.org">JBake ${version}</a>
            </p>
      </div>
    </div>
    
    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>js/jquery-1.11.1.min.js"></script>
    <script src="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>js/bootstrap.min.js"></script>
    <script src="<%if (content.rootpath) {%>${content.rootpath}<% } else { %><% }%>js/prettify.js"></script>

        </body>
      </html>