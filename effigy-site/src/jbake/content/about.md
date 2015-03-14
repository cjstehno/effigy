title=About
date=2015-03-14
type=page
status=published
~~~~~~

Effigy started out as a Java-based project using generated proxies to provide a simpler means of working with the repetitive code required to develop Spring JDBC applications. The proxies would provide all the boilerplate code so that the developer just worked on SQL and functionality.

That project died under lack of time and loss of interest. The proxy approach was flawed and has been overdone anyway. Proxies are acceptable in a language like Java where you have no other option, but with a dynamic language like Groovy you have AST transforms which allow you to undercut proxies and just generate actual code that becomes part of the object. This approach appealed to me even then; I looked at Javassist, but like I said, time was also a factor.

More recently, I started to get more interested in AST Transformations and I was working on a Spring-JDBC-based project of my own, so it seemed a good time to delve deeper into the spirit of my earlier work. Effigy is the work in progress towards that goal.
