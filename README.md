## Register for an API Key

For this project, you will be calling CHPL's RESTful API. You will be using our Staging system and you will need to register to use the API. Complete the registration form on the [CHPL API Registration Page](https://chpl.ahrqstg.org/#/resources/chpl_api) and make sure to hang on to your API Key.

## Instructions for Automated Test Engineers

We would like you to write 3-5 tests that can run in an automated way against the CHPL's RESTful API. API documentation can be found [here](https://chpl.ahrqstg.org/#/resources/chpl_api)

Please use whatever tool you are most comfortable with! We have provided a couple of samples in this Github project to help you get started. The sample unit tests are written in Java and you can find them in the file
>
	src/test/java/com/ainq/chpl/PassingUnitTests.java

You should expect all of the sample tests to pass. If you would like to run the samples, you'll need to follow the [Instructions for Java Developers](#developerInstructions) below to install required software on your machine, check out the code, and use your API Key.

You may write automated testing code to test the same API methods as the samples or use the API documentation to find additional methods to test. Some good candidates for testing are any of the /data/* methods or /search. You will not be able to test methods that require username and password authentication although a nice test might be to confirm that an unauthenticated user cannot access one of these protected methods. 

Be prepared to discuss your solution.

## <a name="developerInstructions"></a>Instructions for Java Developers

#### Prerequisites
* Git and an account on Github
* Java 1.8
* Maven (3.3+ recommended)

#### Check out the code
Fork this git project into your own Github account. Clone the forked copy to your development machine.

#### Insert your API Key
Put your API Key into the necessary properties file: 

>
	src/test/resources/environment.properties 

There is already a placeholder for it:
> 
	apiKey=yourApiKeyHere

#### Run the Tests

From the command line, you should be able to run the unit tests (some passing and some failing) with 
>
	mvn test
	
Using the command line is not required. You may also run them within your favorite IDE. 

#### Write Some Code!

A few of our unit tests have found bugs or incomplete code and are failing. You can see those tests in the file:
>
	src/test/java/com/ainq/chpl/FailingUnitTests.java
	
You'll need to update ChplApiWrapper.java in such a way that all of the failing unit tests pass. The existing failing unit tests *should not* need to be changed, and they should all pass once your ChplApiWrapper updates are complete. Feel free to add other unit tests or make any changes you feel would improve this code.

#### Extra Credit

There are some less-than best practices in the ChplApiWrapper. For example, we are using System.out.println instead of a proper logging framework. Convert our logging statements to use a logging framework of your choice. Feel free to add or remove log statements. Ideally, the logger will be set up to log to the console and to a file that rolls over daily. 

#### Conclusion

Commit and push whatever code you've written to your Github fork of this project. Be prepared to discuss your solution. Did you notice the use of a design pattern in ChplApiWrapper?

