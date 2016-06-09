![las2peer](https://github.com/rwth-acis/LAS2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

# las2peer User Information Service

This service stores user related information for usage from other services.
The user can set up permissions for each field (currently public/private only).
This service does have an RMI API only.

## Fields

* firstName (String)
* lastName (String)
* userImage (String): Reference to an image stored in the file service.

## RMI methods

``public Map<String, Serializable> get(long agentId, String[] fields)``

``public boolean set(Map<String, Serializable> values)``

``public Map<String, Boolean> getPermissions(String[] fields)``

``public boolean setPermissions(Map<String, Boolean> permissions)``

For detailed documentation read the Javadoc.