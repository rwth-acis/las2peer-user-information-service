package i5.las2peer.services.userInformationService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import i5.las2peer.api.execution.ServiceInvocationException;
import i5.las2peer.api.p2p.ServiceNameVersion;
import i5.las2peer.api.security.AgentLockedException;
import i5.las2peer.p2p.LocalNode;
import i5.las2peer.p2p.LocalNodeManager;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.ServiceAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.testing.MockAgentFactory;

/**
 * Example Test Class demonstrating a basic JUnit test structure.
 *
 */
public class ServiceTest {

	private static LocalNode node;
	private static UserAgentImpl testAgent, testAgent2;
	private static final String testPass = "adamspass";
	private static final String testPass2 = "evespass";

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
		node = new LocalNodeManager().newNode();
		testAgent = MockAgentFactory.getAdam();
		testAgent.unlock(testPass); // agent must be unlocked in order to be stored
		node.storeAgent(testAgent);
		testAgent2 = MockAgentFactory.getEve();
		testAgent2.unlock(testPass2); // agent must be unlocked in order to be stored
		node.storeAgent(testAgent2);
		node.launch();

		// during testing, the specified service version does not matter
		ServiceAgentImpl testService = ServiceAgentImpl
				.createServiceAgent(new ServiceNameVersion(UserInformationService.class.getName(), "1.0"), "a pass");
		testService.unlock("a pass");

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
	}

	/**
	 * 
	 * Tests the validation method.
	 * @throws ServiceInvocationException 
	 * @throws AgentLockedException 
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test() throws AgentLockedException, ServiceInvocationException {

		// Create Profile
		Map<String, Serializable> values = new HashMap<>();
		values.put("firstName", "Bart");
		values.put("lastName", "Simpson");
		values.put("userImage", "asdf");
		Object result = node.invoke(testAgent, UserInformationService.class.getName(), "set",
				new Serializable[] { (Serializable) values });

		assertTrue(result.equals(true));

		// Access Profile
		String[] fields = new String[] { "firstName", "lastName", "userImage" };
		result = node.invoke(testAgent, UserInformationService.class.getName(), "get",
				new Serializable[] { testAgent.getIdentifier(), fields });
		assertTrue(((Map<String, Serializable>) result).get("firstName").equals("Bart"));
		assertTrue(((Map<String, Serializable>) result).get("lastName").equals("Simpson"));
		assertTrue(((Map<String, Serializable>) result).get("userImage").equals("asdf"));

		// Change Profile
		values = new HashMap<>();
		values.put("firstName", "Homer");
		result = node.invoke(testAgent, UserInformationService.class.getName(), "set",
				new Serializable[] { (Serializable) values });

		assertTrue(result.equals(true));

		// Access Profile
		fields = new String[] { "firstName", "lastName" };
		result = node.invoke(testAgent, UserInformationService.class.getName(), "get",
				new Serializable[] { testAgent.getIdentifier(), fields });
		assertTrue(((Map<String, Serializable>) result).get("firstName").equals("Homer"));
		assertTrue(((Map<String, Serializable>) result).get("lastName").equals("Simpson"));

		// Set Permissions
		Map<String, Boolean> perms = new HashMap<>();
		perms.put("firstName", true);
		result = node.invoke(testAgent, UserInformationService.class.getName(), "setPermissions",
				new Serializable[] { (Serializable) perms });

		assertTrue(result.equals(true));

		// Get Permissions
		fields = new String[] { "firstName", "lastName", "userImage" };
		result = node.invoke(testAgent, UserInformationService.class.getName(), "getPermissions",
				new Serializable[] { fields });
		assertTrue(((Map<String, Boolean>) result).get("firstName").equals(true));
		assertTrue(((Map<String, Boolean>) result).get("lastName").equals(false));
		assertTrue(((Map<String, Boolean>) result).get("userImage").equals(false));

		// other user Access
		fields = new String[] { "firstName" };
		result = node.invoke(testAgent2, UserInformationService.class.getName(), "get",
				new Serializable[] { testAgent.getIdentifier(), fields });
		assertTrue(((Map<String, Serializable>) result).get("firstName").equals("Homer"));

		// Anonymous Access
		fields = new String[] { "firstName" };
		result = node.invoke(AnonymousAgentImpl.getInstance(), UserInformationService.class.getName(), "get",
				new Serializable[] { testAgent.getIdentifier(), fields });
		assertTrue(((Map<String, Serializable>) result).get("firstName").equals("Homer"));

		// "malicious" anonymous access
		fields = new String[] { "lastName", "firstName" };
		result = node.invoke(AnonymousAgentImpl.getInstance(), UserInformationService.class.getName(), "get",
				new Serializable[] { testAgent.getIdentifier(), fields });
		assertTrue(((Map<String, Serializable>) result).get("firstName").equals("Homer"));
		assertFalse(((Map<String, Serializable>) result).containsKey("lastName"));

	}

}
