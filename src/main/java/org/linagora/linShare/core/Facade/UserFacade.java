/*
 *    This file is part of Linshare.
 *
 *   Linshare is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of
 *   the License, or (at your option) any later version.
 *
 *   Linshare is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with Foobar.  If not, see
 *                                    <http://www.gnu.org/licenses/>.
 *
 *   (c) 2008 Groupe Linagora - http://linagora.org
 *
*/
package org.linagora.linShare.core.Facade;

import java.util.List;

import org.linagora.linShare.core.domain.entities.MailContainer;
import org.linagora.linShare.core.domain.entities.Role;
import org.linagora.linShare.core.domain.entities.UserType;
import org.linagora.linShare.core.domain.vo.DomainVo;
import org.linagora.linShare.core.domain.vo.UserVo;
import org.linagora.linShare.core.exception.BusinessException;


/**
 *  Facade entry for user management.
 */
public interface UserFacade {

    /** Create a guest.
     * @param mail user email (natural key).
     * @param firstName first name.
     * @param lastName last name.
     * @param canUpload if the user can upoad.
     * @param comment : the comment about the user
     * @param mailContainer informations needed to construct the email
     * @param owner user who create the guest.
     * @throws BusinessException if user already exist.
     */
    void createGuest(String mail, String firstName, String lastName, Boolean canUpload, Boolean canCreateGuest,String comment,
    		MailContainer mailContainer, UserVo owner) throws BusinessException;
    
    /**
     * update a guest (edit)
     * @param mail
     * @param firstName
     * @param lastName
     * @param canUpload
     * @param owner
     * @throws BusinessException
     */
    public void updateGuest(String mail, String firstName, String lastName, Boolean canUpload, Boolean canCreateGuest, UserVo owner) throws BusinessException;
    
    /**
     * update an user (only the role)
     * @param mail
     * @param role
     * @param owner
     * @throws BusinessException
     */
    public void updateUser(String mail,Role role, UserVo owner) throws BusinessException;
    

    /** Search a user using its mail.
     * @param mail user mail.
     * @return founded user.
     */
    UserVo findUser(String mail, String domain) throws BusinessException;
    UserVo findUser(String mail, String domain, UserVo actorVO) throws BusinessException;
    

    /** Search a user.
     * @param mail user email.
     * @param firstName user first name.
     * @param lastName user last name.
     * @return a list of matching users.
     */
    List<UserVo> searchUser(String mail, String firstName, String lastName, UserVo currentUser) throws BusinessException;
    List<UserVo> searchUser(String mail, String firstName, String lastName, UserVo currentUser, boolean multiDomain) throws BusinessException;

    
    /** Search a user.
     * @param mail user email.
     * @param firstName user first name.
     * @param lastName user last name.
     * @param userType the type of the user.
     * @return a list of matching users.
     */
    List<UserVo> searchUser(String mail, String firstName, String lastName,UserType userType, UserVo currentUser) throws BusinessException;

    /** Get all guests created by a user.
     * @param mail owner mail.
     * @return the list of guests created by their owner.
     */
    List<UserVo> searchGuest(String mail);

    /** Delete a guest, purge an internal user.
     * @param login login of the user to delete.
     * @param owner : the actor that has to be the guest creator or an admin of the application
     */
    void deleteUser(String login, UserVo actor);
    
    
    List<String> findMails(String beginWith);
    
    /**
     * delete temp admin account which is created by import.sql
     * key is linshare.admin.temp.mail in linshare.properties
     * @throws BusinessException
     */
	public void deleteTempAdminUser() throws  BusinessException;
	/**
	 * search temp admin account which is created by import.sql
	 * key is linshare.admin.temp.mail in linshare.properties
	 * @return
	 */
	public UserVo searchTempAdminUser() throws BusinessException;
	
	/**
	 * Update a user locale
	 * @param user
	 * @param locale
	 */
	public void updateUserLocale(UserVo user, String locale);
	
	
	/**
	 * temporary admin user (import.sql)
	 * edit this constant if you change default temp admin account in import.sql
	 */
    public static final String ADMIN_TEMP_MAIL= "root@localhost.localdomain";
	
    /** Load a User.
     * If the user doesn't exist in database, search informations in LDAP and create a user entry before returning it.
     * @param login user login.
     * @return user details or null if user is neither in database or LDAP.
     */
    UserVo loadUserDetails(String login, String domainId);

    /** Get user password.
     * @param login user login.
     * @return password or null if empty or null.
     */
    String getPassword(String login);
    
    /**
     * Change a user password
     * @param user
     * @param oldPassword
     * @param newPassword
     * @throws BusinessException  AUTHENTICATION_ERROR if the password supplied is wrong
     */
    void changePassword(UserVo user, String oldPassword, String newPassword) throws BusinessException;
    
    /**
     * Set a new password to a guest user
     * @param user
     * @param mailContainer informations needed to construct the email
     */
    void resetPassword(UserVo user, MailContainer mailContainer) throws BusinessException;
    
    /**
	 * Update a guest as restricted and set his list of contacts
	 * 
	 * @param login of the guest
	 * @param mailContacts
	 */
	void setGuestContactRestriction(String login, List<String> mailContacts) throws BusinessException;
	
    /**
	 * Set a guest as not restricted and remove his list of contacts
	 * 
	 * @param login
	 */
	public void removeGuestContactRestriction(String login) throws BusinessException;
	
	/**
	 * Add one contact to a restricted guest
	 * 
	 * @param ownerLogin
	 * @param contactLogin
	 */
	public void addGuestContactRestriction(String ownerLogin, String contactLogin) throws BusinessException;
    
	/**
	 * Retrieve the list of contacts of the guest
	 * 
	 * @param login
	 * @return
	 */
	List<UserVo> fetchGuestContacts(String login) throws BusinessException;

	void updateUserDomain(String mail, DomainVo selectedDomain,
			UserVo userLoggedIn) throws BusinessException;

	/**
	 * Search user that are internal and in the DB but not in domains (=removed from ldap).
	 * 
	 * @param userLoggedIn
	 * @return
	 */
	List<UserVo> searchAllBreakedUsers(UserVo userLoggedIn);
}