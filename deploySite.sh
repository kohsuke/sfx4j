#!/bin/sh
cd target/docs
cvs -d:pserver:kohsuke_agent@cvs.dev.java.net:/cvs import sfx4j/www/maven site-deployment t`date +%Y%m%d-%H%M%S`
cd ../..
cd ../www
date >> update.html
cvs commit -m "to work around a bug in java.net web updater" update.html
