package i5.las2peer.services.userInformationService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.security.Agent;

/**
 * las2peer User Information Service
 * 
 * This service provides methods to store public user related data (user image, profile, ...) for other services. It
 * does not provide a REST API on its own.
 * 
 * Supported fields:
 * <ul>
 * <li>firstName</li>
 * <li>lastName</li>
 * <li>userImage: Reference to file service file</li>
 * </ul>
 * 
 */
public class UserInformationService extends Service {

	private static final String PREFIX = "USER-INFORMATION_";

	public UserInformationService() {

	}

	// //////////////////////////////////////////////////////////////////////////////////////
	// RMI methods.
	// //////////////////////////////////////////////////////////////////////////////////////

	/**
	 * RMI method to fetch information for the user given by the agent id
	 * 
	 * Does not need any special permissions, information is available to everyone
	 * 
	 * @param agentId the user's agent id
	 * @param fields an array of requested fields
	 * @return a map of the requested user information fields, or null on error
	 */
	public Map<String, Serializable> get(String agentId, String[] fields) {
		Map<String, Serializable> result = new HashMap<>();
		for (String field : fields) {
			if (isValidField(field, null)) {
				try {
					Envelope env = Context.get().requestEnvelope(getEnvelopeId(agentId, field));
					result.put(field, env.getContent());
				} catch (EnvelopeNotFoundException e) {
					// value does not exist or is not readable, add default value
					result.put(field, "");
				} catch (EnvelopeException e) {
					// do nothing
				}
			}
		}

		return result;

	}

	/**
	 * Sets the information related to the calling agent
	 * 
	 * @param values key-value pairs of properties
	 * @return true on success, otherwise false
	 */
	public boolean set(Map<String, Serializable> values) {
		try {
			for (Map.Entry<String, Serializable> entry : values.entrySet()) {
				if (isValidField(entry.getKey(), entry.getValue())) {
					Agent owner = Context.get().getMainAgent();
					String envId = getEnvelopeId(owner.getIdentifier(), entry.getKey());
					Envelope env;
					try {
						env = Context.get().requestEnvelope(envId);
					} catch (EnvelopeNotFoundException e) {
						env = Context.get().createEnvelope(envId, owner);
					}
					env.setContent(entry.getValue());
					Context.get().storeEnvelope(env);
				}
			}

			return true;
		} catch (EnvelopeException e) {
			return false;
		}
	}

	/**
	 * Gets a map of permissions
	 * 
	 * @param fields list of fields
	 * @return map of permissions of all given fields: true for public, false for private; null on error
	 */
	public Map<String, Boolean> getPermissions(String[] fields) {
		try {
			Map<String, Boolean> result = new HashMap<>();

			for (String field : fields) {
				if (isValidField(field, null)) {
					try {
						Envelope env = Context.get()
								.requestEnvelope(getEnvelopeId(Context.get().getMainAgent().getIdentifier(), field));
						boolean isPublic = !env.isPrivate();
						result.put(field, isPublic);
					} catch (EnvelopeNotFoundException e) {
						// value does not exist or is not readable, add default value
						result.put(field, false);
					}
				}
			}

			return result;
		} catch (EnvelopeException | IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Sets permissions
	 * 
	 * @param permissions map of fields an their permissions
	 * @return true on success
	 */
	public boolean setPermissions(Map<String, Boolean> permissions) {
		try {
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
				if (!isValidField(entry.getKey(), null)) {
					return false;
				}
				Agent owner = Context.get().getMainAgent();
				String envId = getEnvelopeId(owner.getIdentifier(), entry.getKey());
				try {
					Envelope env = Context.get().requestEnvelope(envId);
					// check if the permission has changed
					if (entry.getValue() != !env.isPrivate()) {
						if (entry.getValue() == true) {
							env.setPublic();
						} else {
							env.addReader(owner);
						}
						Context.get().storeEnvelope(env);
					}
				} catch (EnvelopeNotFoundException e) {
					// no value means no permission to set
				}
			}

			return true;
		} catch (EnvelopeException e) {
			return false;
		}
	}

	// Helper methods

	private static String getEnvelopeId(String agentId, String field) {
		return PREFIX + agentId + "_" + field;
	}

	/**
	 * Checks if the field is valid
	 * 
	 * @param field field id
	 * @param content null, if content is unknown, in this case only the field id is checked
	 * @return true if field is valid
	 */
	private static boolean isValidField(String field, Object content) {
		if (content != null) {
			if (!(content instanceof String)) {
				return false;
			}
		}

		return field.equals("firstName") || field.equals("lastName") || field.equals("userImage");
	}

}
