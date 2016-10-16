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

```java
try {
    // Fields you want to read
    String[] fields = { "firstName", "lastName", "userImage" };
    // RMI call
    Object result = Context.getCurrent().invoke(
        "i5.las2peer.services.userInformationService.UserInformationService@0.1", "get",
        new Serializable[] { Context.getCurrent().getMainAgent().getId(), fields });
    if (result != null) {
        @SuppressWarnings({ "unchecked" })
        HashMap<String, Serializable> hashMap = (HashMap<String, Serializable>) result;
    } else {
        // Error handling
    }
} catch (Exception e) {
    // Exception handling
}
```

``public boolean set(Map<String, Serializable> values)``

```java
try {
    // Setting parameters 
    HashMap<String, Serializable> m = new HashMap<String, Serializable>();
    m.put("firstName", "first name");
    m.put("lastName", "last name");
    m.put("userImage", "file service id for user image");
    // RMI call
    Object result = Context.getCurrent().invoke(
        "i5.las2peer.services.userInformationService.UserInformationService@0.1", "set",
        new Serializable[] { m });
    if (result != null) {
        // Ok
    } else {
        // Error handling
    }
} catch (Exception e) {
    // Exception handling
}
```

``public Map<String, Boolean> getPermissions(String[] fields)``

```java
try {
    // Fields you want to read
    String[] fields = { "firstName", "lastName", "userImage" };
    // RMI call
    Object result = Context.getCurrent().invoke(
        "i5.las2peer.services.userInformationService.UserInformationService@0.1", "getPermissions",
        new Serializable[] { fields });
    if (result != null) {
        @SuppressWarnings({ "unchecked" })
        HashMap<String, Serializable> hashMap = (HashMap<String, Serializable>) result;
    } else {
        // Error handling
    }
} catch (Exception e) {
    // Exception handling
}

```

``public boolean setPermissions(Map<String, Boolean> permissions)``

```java
try {
    // Setting parameters 
    // true  - public
    // false - private
    HashMap<String, Boolean> m = new HashMap<String, Boolean>();
    m.put("firstName", true);
    m.put("lastName", true);
    m.put("userImage", true);
    // RMI call 
    Object result = Context.getCurrent().invoke(
        "i5.las2peer.services.userInformationService.UserInformationService@0.1", "setPermissions", m);
    if (result != null) {
        System.out.println("Setting permission: " + ((Boolean) result));
    } else {
        // Error handling
    }
} catch (Exception e) {
    // Exception handling
}
```

For detailed documentation read the Javadoc.
