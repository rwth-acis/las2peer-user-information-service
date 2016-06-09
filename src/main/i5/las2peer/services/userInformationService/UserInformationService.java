package i5.las2peer.services.userInformationService;

import i5.las2peer.api.Service;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.ArtifactNotFoundException;
import i5.las2peer.p2p.StorageException;
import i5.las2peer.persistency.DecodingFailedException;
import i5.las2peer.persistency.EncodingFailedException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.EnvelopeException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.tools.SerializationException;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

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
 * 
 */

public class UserInformationService extends Service {

	// instantiate the logger class
	private final L2pLogger logger = L2pLogger.getInstance(UserInformationService.class.getName());

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
		try {
			Map<String, Serializable> result = new HashMap<>();

			for (String field : fields) {
				if (isValidField(field, null)) {
					Envelope env = load(agentId, field);
					result.put(field, env.getContentAsString());
				}
			}

			return result;

		} catch (L2pSecurityException | StorageException | EnvelopeException | UnsupportedEncodingException
				| SerializationException e) {
			e.printStackTrace();
			return null;
		}
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
				if (!isValidField(e.getKey(), e.getValue()))
					return false;

				Envelope env = load(getContext().getMainAgent().getId(), e.getKey());

				env.updateContent((String) e.getValue());

				store(env);
			}

			return true;
		} catch (L2pSecurityException | StorageException | EnvelopeException | UnsupportedEncodingException
				| SerializationException e) {
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
					Envelope env = load(getContext().getMainAgent().getId(), field);

					boolean isPublic = env.hasReader(getContext().getLocalNode().getAnonymous());

					result.put(field, isPublic);
				}
			}

			return result;
		} catch (L2pSecurityException | UnsupportedEncodingException | StorageException | EnvelopeException
				| SerializationException e) {
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
				if (!isValidField(e.getKey(), null))
					return false;

				Envelope env = load(getContext().getMainAgent().getId(), e.getKey());

				if (e.getValue() == true)
					env.addReader(getContext().getLocalNode().getAnonymous());
				else
					env.removeReader(getContext().getLocalNode().getAnonymous());

				store(env);
			}

			return true;
		} catch (L2pSecurityException | StorageException | EnvelopeException | UnsupportedEncodingException
				| SerializationException e) {
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
	 * @throws L2pSecurityException
	 * @throws StorageException
	 * @throws EnvelopeException
	 * @throws SerializationException
	 * @throws UnsupportedEncodingException
	 */
	private Envelope load(long agentId, String field) throws L2pSecurityException, StorageException, EnvelopeException,
			UnsupportedEncodingException, SerializationException {
		try {
			Envelope env = getContext().getStoredObject(String.class, getEnvelopeId(agentId, field));
			try {
				env.open();
			} catch (L2pSecurityException e) {
				env.open(getContext().getLocalNode().getAnonymous());
			}
			return env;

		} catch (ArtifactNotFoundException e) {
			Envelope env = Envelope.createClassIdEnvelope(new String(),
					getEnvelopeId(getContext().getMainAgent().getId(), field), getContext().getMainAgent());
			env.open();
			return env;
		}
	}

	/**
	 * stores an envelope
	 * 
	 * @param env
	 * @throws L2pSecurityException
	 * @throws UnsupportedEncodingException
	 * @throws EncodingFailedException
	 * @throws SerializationException
	 * @throws StorageException
	 * @throws DecodingFailedException
	 */
	private void store(Envelope env) throws L2pSecurityException, UnsupportedEncodingException,
			EncodingFailedException, SerializationException, StorageException, DecodingFailedException {

		if (getContext().getMainAgent().equals(getContext().getLocalNode().getAnonymous()))
			throw new L2pSecurityException("Data cannot be stored for anonymous!");

		// private by default:
		// env.addReader(getContext().getLocalNode().getAnonymous());
		env.addSignature(getContext().getMainAgent());
		env.store();
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
			if (!(content instanceof String))
				return false;
		}

		return field.equals("firstName") || field.equals("lastName") || field.equals("userImage");
	}

}
