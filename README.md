Public file download sample
===========================

This sample shows how to export files from system using public links.

Components:

* PublicFile entity - describes published file descriptor
* PublicFileDownloadController - downloads published file anonymously by provided link id
* web-dispatcher-spring.xml - enables Spring component scan for `com.company.demo.web.controllers` package
* PublicFileBrowse - shows list of published files with persistent web links

Demo:

* Open Application - Public Files menu
* Create published file
* Upload file and set Link id that will be used as a part of permanent link
* Save published file
* Click on `Download` link in the table to download file using permanent link