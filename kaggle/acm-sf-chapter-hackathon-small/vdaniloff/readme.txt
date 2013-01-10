This solution is developed in Java, so one needs JDK 1.6 installed to compile and run it.
Both IntelliJ IDEA project and Apache Ant build files are provided, either of them can be used for running the project.

To make the project one needs to run "all" Ant target either by "ant all" command issued from command line or from IDEA.
Once the project is built, the artifact hackathon.jar shall appear in out/artifacts directory.

To run the application one needs to issue the following command:

java -jar hackathon.jar <xml data file> <training data file> <test data file> <output file>

When parameters are not provided the application prints the usage line.