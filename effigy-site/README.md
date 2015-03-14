# JBake Site - Groovy Template

Your jBake content can be found in `src/jbake` and you can build the site using:

    ./gradlew jbake
    
Your site will be generated in `build/jbake`.

You can start a local server with your generated content using:

  groovy serve.groovy [port]
  
> There is an upcoming release of the `gradle-jbake-plugin` which will have serving support built-in, this is just a stop gap solution until then.

When you are ready to publish the site to your GitHub-Pages, run:

  ./gradlew publish

    
