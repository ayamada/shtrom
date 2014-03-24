#!/bin/sh

if [ $# -ne 1 ]; then
    echo "Usage: $0 bucket-name"
    exit 1
fi

lein clean
lein with-profile provided bin

TARGET=target/shtrom
TARGET_JAR=`ls target/shtrom-*-standalone.jar`
VERSION=`echo $TARGET_JAR | sed -e 's/^target\/shtrom-\(.*\)-standalone\.jar$/\1/'`
BUCKET=$1

echo "Deploy $TARGET to $BUCKET with version $VERSION"

aws s3 cp $TARGET s3://$BUCKET/shtrom/shtrom-$VERSION

echo "Done"
