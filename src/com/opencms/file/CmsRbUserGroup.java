package com.opencms.file;

import java.util.*;
import javax.servlet.http.*;

import com.opencms.core.*;

/**
 * This class describes a resource broker for user and groups in the Cms.<BR/>
 * <B>All</B> Methods get a first parameter: A_CmsUser. It is the current user. This 
 * is for security-reasons, to check if this current user has the rights to call the
 * method.<BR/>
 * 
 * All methods have package-visibility for security-reasons.
 * 
 * @author Michael Emmerich
 * @version $Revision: 1.1 $ $Date: 1999/12/14 18:02:14 $
 */
 class CmsRbUserGroup extends A_CmsRbUserGroup{

	/**
	 * Determines, if the users current group is the admin-group.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @return true, if the users current group is the admin-group, 
	 * else it returns false.
	 */	
     boolean isAdmin(A_CmsUser callingUSer) {
      return true;
     }

	/**
	 * Determines, if the users current group is the projectleader-group.<BR/>
	 * All projectleaders can create new projects, or close their own projects.
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @return true, if the users current group is the projectleader-group, 
	 * else it returns false.
	 */	
     boolean isProjectLeader(A_CmsUser callingUSer){
         return true;
     }

	/**
	 * Returns the anonymous user object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @return the anonymous user object.
	 */
     A_CmsUser anonymousUser(A_CmsUser callingUSer) {
         return null;
     }
	
	/**
	 * Returns a user object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user that is to be read.
	 * @return User
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	 A_CmsUser readUser(A_CmsUser callingUSer, String username)
         throws CmsException {
         return null;
     }
	
	/**
	 * Returns a user object if the password for the user is correct.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The username of the user that is to be read.
	 * @param password The password of the user that is to be read.
	 * @return User
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful
	 */		
	 A_CmsUser readUser(A_CmsUser callingUser, String username, String password)
         throws CmsException {
         return null;
     }
	
	/**
	 * Authentificates a user to the CmsSystem. If the user exists in the system, 
	 * a CmsUser object is created and his session is used for identification. This
	 * operation fails, if the password is incorrect.</P>
	 * 
	 * <B>Security:</B>
	 * All users are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param session The HttpSession to store identification.
	 * @param username The Name of the user.
	 * @param password The password of the user.
	 * @return A CmsUser Object if authentification was succesful, otherwise null 
	 * will be returned.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful.
	 */
	 A_CmsUser loginUser(A_CmsUser callingUser, HttpSession session, 
						String username, String password)
         throws CmsException {
         return null;
     }

	/**
	 * Returns a list of groups of a user.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user.
	 * @return Vector of groups
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	 Vector getGroupsOfUser(A_CmsUser callingUser, String username)
         throws CmsException {
         return null;
     }
     

	/**
	 * Returns a group object.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param groupname The name of the group that is to be read.
	 * @return Group.
	 * 
	 * @exception CmsException  Throws CmsException if operation was not succesful
	 */
	 A_CmsGroup readGroup(A_CmsUser callingUser, String groupname)
         throws CmsException {
         return null;
     }

	/**
	 * Returns a list of users in a group.<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param groupname The name of the group to list users from.
	 * @return Vector of users.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */
	 Vector getUsersOfGroup(A_CmsUser callingUser, String groupname)
         throws CmsException {
         return null;
     }

	/**
	 * Checks if a user is member of a group.<P/>
	 *  
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param nameuser The name of the user to check.
	 * @param groupname The name of the group to check.
	 * @return True or False
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	 boolean userInGroup(A_CmsUser callingUser, String username, String groupname)
         throws CmsException {
         return true;
     }

	/** 
	 * Adds a user to the Cms.
	 * 
	 * Only a adminstrator can add users to the cms.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param name The new name for the user.
	 * @param password The new password for the user.
	 * @param group The default groupname for the user.
	 * @param description The description for the user.
	 * @param additionalInfos A Hashtable with additional infos for the user. These
	 * Infos may be stored into the Usertables (depending on the implementation).
	 * @param flags The flags for a user (e.g. C_FLAG_ENABLED)
	 * 
	 * @return user The added user will be returned.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 * @exception CmsDuplicateKeyException Throws CmsDuplicateKeyException if
	 * a user with the given username exists already.
	 */
	 A_CmsUser addUser(A_CmsUser callingUser, String name, String password, 
					  String group, String description, 
					  Hashtable additionalInfos, int flags)
         throws CmsException, CmsDuplicateKeyException {
         return null;
     }

	/** 
	 * Deletes a user from the Cms.
	 * 
	 * Only a adminstrator can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param name The name of the user to be deleted.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */
	 void deleteUser(A_CmsUser callingUser, String username)
         throws CmsException {
     }

	/**
	 * Updated the userinformation.<BR/>
	 * 
	 * Only the administrator can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user to be updated.
	 * @param additionalInfos A Hashtable with additional infos for the user. These
	 * @param flag The new user access flags.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesful
	 */
	 void updateUser(A_CmsUser callingUser, String username, 
					Hashtable additionalInfos, int flag)
         throws CmsException {
     }

	/**
	 * Add a new group to the Cms.<BR/>
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param name The name of the new group.
	 * @param description The description for the new group.
	 * @int flags The flags for the new group.
	 *
	 * @return Group
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 * @exception MhtDuplicateKeyException Throws MhtDuplicateKeyException if 
	 * same group already exists.
	 */	
	 A_CmsGroup addGroup(A_CmsUser callingUser, String name, String description, int flags)
         throws CmsException, CmsDuplicateKeyException {
         return null;
     }

	/**
	 * Delete a group from the Cms.<BR/>
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param delgroup The name of the group that is to be deleted.
	 * @exception CmsException  Throws CmsException if operation was not succesfull.
	 */	
	 void deleteGroup(A_CmsUser callingUser, String delgroup)
         throws CmsException {
     }

	/**
	 * Adds a user to a group.<BR/>
     *
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user that is to be added to the group.
	 * @param groupname The name of the group.
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */	
	 void addUserToGroup(A_CmsUser callingUser, String username, String groupname)
         throws CmsException {
     }

	/**
	 * Removes a user from a group.
	 * 
	 * Only the admin can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Only users, which are in the group "administrators" are granted.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user that is to be removed from the group.
	 * @param groupname The name of the group.
	 * @exception CmsException Throws CmsException if operation was not succesful.
	 */	
	 void removeUserFromGroup(A_CmsUser callingUser, String username, String groupname)
         throws CmsException {
     }

	/**
	 * Returns all users<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @return users A Vector of all existing users.
	 */
     Vector getUsers(A_CmsUser callingUser) {
         return null;
     }
	
	/**
	 * Returns all groups<P/>
	 * 
	 * <B>Security:</B>
	 * All users are granted, except the anonymous user.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @return users A Vector of all existing groups.
	 */
     Vector getGroups(A_CmsUser callingUser) {
         return null;
     }

	/** 
	 * Sets the password for a user.
	 * 
	 * Only a adminstrator or the curretuser can do this.<P/>
	 * 
	 * <B>Security:</B>
	 * Users, which are in the group "administrators" are granted.<BR/>
	 * Current users can change their own password.
	 * 
	 * @param callingUser The user who wants to use this method.
	 * @param username The name of the user.
	 * @param newPassword The new password.
	 * 
	 * @exception CmsException Throws CmsException if operation was not succesfull.
	 */
	 void setPassword(A_CmsUser callingUser, String username, String newPassword)
        throws CmsException{
                           }
 }
