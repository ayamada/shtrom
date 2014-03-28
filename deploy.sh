#!/bin/sh

if [ $# -ne 1 ]; then
    echo "Usage: $0 bucket-name"
    exit 1
fi

lein clean
lein with-profile provided uberjar

TARGET=`ls target/shtrom-*-standalone.jar`
BUCKET=$1

echo "Deploy $TARGET to $BUCKET with version $VERSION"

aws s3 cp $TARGET s3://$BUCKET/shtrom/

echo "Done"
