package i5.las2peer.services.userInformationService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import i5.las2peer.api.Service;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;

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
	public Map<String, Serializable> get(long agentId, String[] fields) {
		Map<String, Serializable> result = new HashMap<>();
		for (String field : fields) {
			if (isValidField(field, null)) {
				try {
					Envelope env = getContext().fetchEnvelope(getEnvelopeId(agentId, field));
					result.put(field, env.getContent());
				} catch (ArtifactNotFoundException e) {
					// value does not exist or is not readable, add default value
					result.put(field, "");
				} catch (IllegalArgumentException | SerializationException | CryptoException | StorageException
						| L2pSecurityException e) {
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
				if (!isValidField(entry.getKey(), entry.getValue())) {
					return false;
				}
				Agent owner = getContext().getMainAgent();
				String envId = getEnvelopeId(owner.getId(), entry.getKey());
				Envelope newVersion;
				try {
					Envelope env = getContext().fetchEnvelope(envId);
					newVersion = getContext().createEnvelope(env, entry.getValue());
				} catch (ArtifactNotFoundException e) {
					newVersion = getContext().createEnvelope(envId, entry.getValue(), owner);
				}
				getContext().storeEnvelope(newVersion);
			}

			return true;
		} catch (StorageException | SerializationException | IllegalArgumentException | CryptoException e) {
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
						Envelope env = getContext()
								.fetchEnvelope(getEnvelopeId(getContext().getMainAgent().getId(), field));
						boolean isPublic = !env.isEncrypted();
						result.put(field, isPublic);
					} catch (ArtifactNotFoundException e) {
						// value does not exist or is not readable, add default value
						result.put(field, false);
					}
				}
			}

			return result;
		} catch (StorageException | IllegalArgumentException e) {
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
				Agent owner = getContext().getMainAgent();
				String envId = getEnvelopeId(owner.getId(), entry.getKey());
				try {
					Envelope env = getContext().fetchEnvelope(envId);
					// check if the permission has changed
					if (entry.getValue() != !env.isEncrypted()) {
						Envelope newVersion;
						if (entry.getValue() == true) {
							newVersion = getContext().createUnencryptedEnvelope(env, env.getContent());
						} else {
							newVersion = getContext().createEnvelope(env, env.getContent(),
									getContext().getMainAgent());
						}
						getContext().storeEnvelope(newVersion);
					}
				} catch (ArtifactNotFoundException e) {
					// no value means no permission to set
				}
			}

			return true;
		} catch (L2pSecurityException | StorageException | SerializationException | IllegalArgumentException
				| CryptoException e) {
			return false;
		}
	}

	// Helper methods

	private static String getEnvelopeId(long agentId, String field) {
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
