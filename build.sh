if test ! -f src/main/resources/JiraClient.properties; then echo "Not configured yet. Check README.md for details"; exit 1; fi 
mvn clean assembly:assembly


