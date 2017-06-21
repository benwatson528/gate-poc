# gate-poc
A simple POC to demonstrate running [GATE](gate.ac.uk) as a Java app.

This application takes a list of paths to documents, annotates the documents with their Named Entities (Person, Location etc), and then outputs the annotated documents.

## Running Instructions
1. Checkout this project with - `git clone git@github.com:benwatson528/gate-poc.git`
2. Download the GATE installer from [Sourceforge](http://sourceforge.net/projects/gate/files/gate/8.4.1/gate-8.4.1-build5753-installer.jar/download)
3. Move the GATE installer JAR to `src/main/resources` in this project
4. Build the Docker container `docker build -t gate .`
5. Run the test case within Docker `docker run -it -v ~/programming/coursera/gate-pipeline/:/code gate mvn clean test -Dgate.home=/usr/local/GATE_Developer_8.4.1`
6. View the annotated output in the project's root directory: `StANNIE_1.HTML` and `StANNIE_toXML_1.HTML`

## Notes
1. All the code here is contained within the `ANNIETutorial` and `ANNIETutorialTest` classes. It's adapted from the first example in [https://gate.ac.uk/wiki/code-repository/](https://gate.ac.uk/wiki/code-repository/).
2. This application is not completely standalone - GATE must be installed on the machine from which this is running (hence the Docker requirement).
3. This example only tags Persons and Locations, but it would be trivial to pass a list of desired Named Entities as an argument (or to directly modify the parameters within the code).
