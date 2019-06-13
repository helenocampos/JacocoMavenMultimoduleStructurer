# JacocoMavenMultimoduleStructurer
Structures a multi module Maven project so that Jacoco can aggregate coverage metrics

How to use: 

- build from source (mvn install)
- execute the generated .jar passing the project path as argument

Be aware that by executing the .jar, an aggregator module will be created within your project and your pom.xml will be modified.

