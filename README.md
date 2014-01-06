teiidSimpleDBQuickstart
=======================
Introduction
-----------------------
This is quickstart for SimpleDB connector and translator for Teiid - https://github.com/teiid/teiid
-------------------
<a id="systemrequirements"></a>
System Requirements 
-------------------
To run these quickstarts with the provided build scripts, you need the following:

1. Java 1.6 or better, to run JBoss AS and Maven. You can choose from the following:
    * OpenJDK
    * Oracle Java SE
    * Oracle JRockit

2. Maven 3.0.0 or newer, to build and deploy the examples
    * If you have not yet installed Maven, see the [Maven Getting Started Guide](http://maven.apache.org/guides/getting-started/index.html) for details.
    * If you have installed Maven, you can check the version by typing the following in a command line:

            mvn --version 

3. The JBoss Enterprise Application Platform (EAP) 6.1 (and higher) distribution ZIP or the JBoss AS 7.2 (or WildFly 8 or higher) distribution ZIP.
    * For information on how to install and run JBoss, refer to the server documentation.
    
4. Amazon AWS credentials
    * If you don't have one, go to aws.amazon.com and sing up for using Amazon SimpleDB
    * Your access credentials can be found on https://portal.aws.amazon.com/gp/aws/securityCredentials
    
------------------
<a id="installingTeiid"></a>
Installing Teiid
------------------
Project Teiid (version 8.6) is compatible with EAP 6.1 or WildFly 8.
1. Prepare local environment:
    1. Install application server (by uncompressing it to desired folder)
    2. Download latest Teiid Runtime from http://www.jboss.org/teiid/downloads and install it
        * Installing teiid is just matter of extracting into WildFly or EAP directory.
    3. Add connection definition to resource adapter
        * Open $AS_ROOT_DIRECTORY/standalone/configuration/standalone-teiid.xml in your favourite text editor
        * Add this snippet under simpledb resource adapter to look like so (do not forget to fill your access key id & access key):
        ```
        <resource-adapter id="simpledb">
            <module slot="main" id="org.jboss.teiid.resource-adapter.simpledb"/>
            <connection-definitions>
                <connection-definition class-name="org.teiid.resource.adapter.simpledb.SimpleDBManagedConnectionFactory" jndi-name="java:/simpledb" enabled="true" use-java-context="true" pool-name="fileDS">
                    <config-property name="AccessKey">
                        <YOUR_ACCESS_KEY_ID>
                    </config-property>
                    <config-property name="SecretAccessKey">
                        <YOUR_SECRET_ACCESS_KEY>
                    </config-property>
                </connection-definition>
            </connection-definitions>
        </resource-adapter>
        ```
    4. Create virtual database file (VDB) and deploy it:
        * Create file 'my-vdb.vdb' with content:
        ```
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <vdb name="Test" version="1">
        
            <description>Test</description>
            <model name="Test">
                <source name="simpledb-connector" translator-name="simpledb" connection-jndi-name="java:/simpledb"/>
            </model>
        </vdb>
        ```
        * Save this file to your AS deploy folder ($AS_ROOT_DIRECTORY/standalone/deployments/my-vdb.vdb)
        
2. Prepare data in SimpleDB database:
    1. Clone this git repository
    2. In root folder (the one with pom.xml in it) run `mvn verify -DkeyID=<yourKeyID> -DsecretKey=<yourSecretKey>`
        * This should create 3 domains in your SimpleDB database
            1. `movies` database of movies
            2. `people` database of actors and directors
            3. `people_movies` intersection table for m to n relationship between movies and people
3. Start Teiid
    * Start  your application server like `bin/standalone.sh -c standalone-teiid.xml`
TODO
