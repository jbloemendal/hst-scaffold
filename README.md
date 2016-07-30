[![Build Status](https://travis-ci.org/jbloemendal/hst-scaffold.svg?branch=master)](https://travis-ci.org/jbloemendal/hst-scaffold)
![HST-Scaffold](https://raw.githubusercontent.com/jbloemendal/hst-scaffold/master/logo.png)
HST-Scaffold
============

Scaffold your projects Hippo Site Toolkit configuration from text file.

Build:
```
# build embedded project
cd myhippoproject
mvn clean verify

# build scaffold
cd .. 
mvn clean verify
```

Example Scaffold (myhippoproject/scaffold.hst):
```
#HST scaffold example

#URL                   CONTENTPATH                          COMPONENTS
/                       /home                                home(header,&main(banner, doc),footer) # home page
/contact                /contact                             text(header,*main,footer)              # text page
/simple                 /simple                              simple                                 # simple page
/news/:date/:id         /news/date:String/id:String          news(header,*main,footer)              # news page
/news                   /news                                newsoverview(header,list, footer)      # news overview page
/text/*path             /text/path:String                    text(header,*main,footer)
/text/content/*path     /text/content/path:String            test(header,*main,footer)
```

Conventions:
```
- template and component names are unique
- define abstract/common components at the top
...
```

Usage:
```
cd myhippoproject
java -jar sfd.jar [options] [args]

Options
-h     --help               Show help
-b     --build              Build configuration from scaffold.
-u     --updateForce        Update and overwrite configuration from scaffold (interactive).
-w     --dryrun             Dryrun
-c     --configuration      Custom configuration file.
-s     --scaffold           Build scaffold from existing project configuration (reverse).
-r     --rollback           Rollback Build / Update / Scaffold.
-d     --diagnose           Find outdated components/sitemapitems/templates
```