package i5.las2peer.services.userInformationService;

import i5.las2peer.p2p.LocalNode;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.userInformationService.UserInformationService;
import i5.las2peer.testing.MockAgentFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Example Test Class demonstrating a basic JUnit test structure.
 *
 */
public class ServiceTest {

	private static LocalNode node;

	private static UserAgent testAgent;
	private static final String testPass = "adamspass";

	/**
	 * Called before the tests start.
	 * 
	 * Sets up the node and initializes connector and users that can be used throughout the tests.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startServer() throws Exception {

		// start node
		node = LocalNode.newNode();
		testAgent = MockAgentFactory.getAdam();
		testAgent.unlockPrivateKey(testPass); // agent must be unlocked in order to be stored
		node.storeAgent(testAgent);
		node.launch();

		// during testing, the specified service version does not matter
		ServiceAgent testService = ServiceAgent.createServiceAgent(UserInformationService.class.getName(), "a pass");
		testService.unlockPrivateKey("a pass");

		node.registerReceiver(testService);

	}

	/**
	 * Called after the tests have finished. Shuts down the server and prints out the connector log file for reference.
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void shutDownServer() throws Exception {
		node.shutDown();
		node = null;

		LocalNode.reset();
	}

	/**
	 * 
	 * Tests the validation method.
	 * 
	 */
	@Test
	public void testGet() {

	}

}
