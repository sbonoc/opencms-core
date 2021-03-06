#!/bin/bash

# The OpenCms Version number.
#
# If this script is used in the build process, 
# you need to change the version number only here and no where else.
# It can be accessed in OpenCms by OpenCms.getSystemInfo().getVersionNumber().
#
OPENCMS_VERSION_NUMBER="9.4.8"

#
# The type of build generated.
#
# Values used in the test cases are:
# "Release" for a release build
# "Nightly" for a nightly build
#
OPENCMS_BUILD_TYPE="Milestone"

#
# The build system that was used.
#
OPENCMS_BUILD_SYSTEM="Jenkins CI"

# The output path/filename where the properties are written.
# 
# The idea of this script is as follows:
# In the RCS there is a 'static' variation of the version properties.
# If this script is used, the 'static' file from the RCS will 
# be replaced by a dynamically generated version that contains
# more detailed information about the build.
# 
OUTPUT_FILE="$WORKSPACE/opencms/src/org/opencms/main/version.properties"

#
# Variables set by the CI/build system.
#
# These will be provided to OpenCms as list of variables that 
# can be accessed by OpenCms.getSystemInfo().getBuildInfo().
#
OPENCMS_BUILD_NUMBER=$JENKINS_BUILD_NUMBER
OPENCMS_BUILD_DATE=$(date +"%Y-%m-%d %H:%M")
OPENCMS_GIT_ID=${GIT_COMMIT:0:7}
OPENCMS_GIT_BRANCH=$GIT_BRANCH

#
# The OpenCms version ID.
#
# This is a condensed String from the variables set above.
# It can be accessed in OpenCms by OpenCms.getSystemInfo().getVersionId().
#
OPENCMS_VERSION_ID="$OPENCMS_BUILD_TYPE $OPENCMS_BUILD_NUMBER ($OPENCMS_GIT_BRANCH - $OPENCMS_GIT_ID) $OPENCMS_BUILD_DATE"


#
# Echo some info to the console.
#
echo "# "
echo "# OpenCms Version Information:"
echo "# "
echo "# Version Number: $OPENCMS_VERSION_NUMBER"
echo "# Version ID    : $OPENCMS_VERSION_ID"
echo "# Version File  : $OUTPUT_FILE"
echo "# "

#
# Generate the output file.
#
echo "# " > "$OUTPUT_FILE"
echo "# OpenCms version information." >> "$OUTPUT_FILE"
echo "# Automatically generated by Jenkins build." >> "$OUTPUT_FILE"
echo "# " >> "$OUTPUT_FILE"
#
echo "version.number=$OPENCMS_VERSION_NUMBER" >> "$OUTPUT_FILE"
echo "version.id=$OPENCMS_VERSION_ID" >> "$OUTPUT_FILE"
#
echo "build.number=$OPENCMS_BUILD_NUMBER" >> "$OUTPUT_FILE"
echo "build.date=$OPENCMS_BUILD_DATE" >> "$OUTPUT_FILE"
echo "build.type=$OPENCMS_BUILD_TYPE" >> "$OUTPUT_FILE"
echo "build.system=$OPENCMS_BUILD_SYSTEM" >> "$OUTPUT_FILE"
echo "build.gitid=$OPENCMS_GIT_ID" >> "$OUTPUT_FILE"
echo "build.gitbranch=$OPENCMS_GIT_BRANCH" >> "$OUTPUT_FILE"
#
# Nice names for the build information (optional).
#
echo "nicename.build.number=Build Number" >> "$OUTPUT_FILE"
echo "nicename.build.date=Build Date" >> "$OUTPUT_FILE"
echo "nicename.build.type=Build Type" >> "$OUTPUT_FILE"
echo "nicename.build.system=Build System" >> "$OUTPUT_FILE"
echo "nicename.build.gitid=Git Commit ID" >> "$OUTPUT_FILE"
echo "nicename.build.gitbranch=Git Branch" >> "$OUTPUT_FILE"
