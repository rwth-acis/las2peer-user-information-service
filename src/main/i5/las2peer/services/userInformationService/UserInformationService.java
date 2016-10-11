package i5.las2peer.services.userInformationService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import i5.las2peer.api.Context;
import i5.las2peer.api.Service;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.persistency.Envelope;
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
					Envelope env = fetchOrCreateEnvelope(agentId, field);
					result.put(field, env.getContent());
				} catch (IllegalArgumentException | SerializationException | CryptoException | StorageException
						| L2pSecurityException e) {
					// do nothing
				}
			}
		}

		return result;

	}

	/**
	 * sets the information related to the calling agent
	 * 
	 * @param values key-value pairs of properties
	 * @return true on success, otherwise false
	 */
	public boolean set(Map<String, Serializable> values) {
		try {
			for (Map.Entry<String, Serializable> e : values.entrySet()) {
				if (!isValidField(e.getKey(), e.getValue())) {
					return false;
				}
				Envelope env = fetchOrCreateEnvelope(getContext().getMainAgent().getId(), e.getKey());
				Envelope newVersion = Context.getCurrent().createEnvelope(env, e.getValue());
				Context.getCurrent().storeEnvelope(newVersion);
			}

			return true;
		} catch (StorageException | SerializationException | IllegalArgumentException | CryptoException e) {
			return false;
		}
	}

	/**
	 * get a map of permissions
	 * 
	 * @param fields list of fields
	 * @return map of permissions of all given fiels: true for public, false for private; null on error
	 */
	public Map<String, Boolean> getPermissions(String[] fields) {
		try {
			Map<String, Boolean> result = new HashMap<>();

			for (String field : fields) {
				if (isValidField(field, null)) {
					Envelope env = fetchOrCreateEnvelope(getContext().getMainAgent().getId(), field);
					boolean isPublic = !env.isEncrypted();
					result.put(field, isPublic);
				}
			}

			return result;
		} catch (StorageException | SerializationException | IllegalArgumentException | CryptoException e) {
			return null;
		}
	}

	/**
	 * set permissions
	 * 
	 * @param permissions map of fields an their permissions
	 * @return true on success
	 */
	public boolean setPermissions(Map<String, Boolean> permissions) {
		try {
			for (Map.Entry<String, Boolean> e : permissions.entrySet()) {
				if (!isValidField(e.getKey(), null)) {
					return false;
				}
				Envelope env = fetchOrCreateEnvelope(getContext().getMainAgent().getId(), e.getKey());

				if (e.getValue() != !env.isEncrypted()) {
					Envelope newVersion;
					if (e.getValue() == true) {
						newVersion = Context.getCurrent().createUnencryptedEnvelope(env, env.getContent());
					} else {
						newVersion = Context.getCurrent().createEnvelope(env, env.getContent(),
								getContext().getMainAgent());
					}
					Context.getCurrent().storeEnvelope(newVersion);
				}
			}

			return true;
		} catch (L2pSecurityException | StorageException | SerializationException | IllegalArgumentException
				| CryptoException e) {
			return false;
		}
	}

	// Storage methods

	/**
	 * returns an open Envelope
	 * 
	 * creates a new one if none exists
	 * 
	 * @param agentId
	 * @param field
	 * @return
	 * @throws CryptoException
	 * @throws IllegalArgumentException
	 * @throws SerializationException
	 * @throws StorageException
	 */
	private Envelope fetchOrCreateEnvelope(long agentId, String field)
			throws IllegalArgumentException, SerializationException, CryptoException, StorageException {
		try {
			return Context.getCurrent().fetchEnvelope(getEnvelopeId(agentId, field));
		} catch (ArtifactNotFoundException e) {
			return Context.getCurrent().createEnvelope(getEnvelopeId(agentId, field), "", getContext().getMainAgent());
		}
	}

	private static String getEnvelopeId(long agentId, String field) {
		return PREFIX + agentId + "_" + field;
	}

	/**
	 * checks if the field is valid
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
