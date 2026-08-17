#
# run.sh: executes CIS web service client example
# environment: cygwin shell
# parameter: the name of the client class with no extension or package prefix
# example: run.sh ListDesignsClient
#

export JAVA_HOME=/cygdrive/c/jdk1.6.0_3
export PATH=$JAVA_HOME/bin:$PATH

java -jar wsclient.jar $*