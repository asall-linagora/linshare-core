/*
 * LinShare is an open source filesharing software, part of the LinPKI software
 * suite, developed by Linagora.
 * 
 * Copyright (C) 2015 LINAGORA
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version, provided you comply with the Additional Terms applicable for
 * LinShare software by Linagora pursuant to Section 7 of the GNU Affero General
 * Public License, subsections (b), (c), and (e), pursuant to which you must
 * notably (i) retain the display of the “LinShare™” trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source
 * and free version of LinShare™, powered by Linagora © 2009–2015. Contribute to
 * Linshare R&D by subscribing to an Enterprise offer!” infobox and in the
 * e-mails sent with the Program, (ii) retain all hypertext links between
 * LinShare and linshare.org, between linagora.com and Linagora, and (iii)
 * refrain from infringing Linagora intellectual property rights over its
 * trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License and
 * its applicable Additional Terms for LinShare along with this program. If not,
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License
 * version 3 and <http://www.linagora.com/licenses/> for the Additional Terms
 * applicable to LinShare software.
 */
package org.linagora.linshare.core.service.impl;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.linagora.linshare.core.business.service.DocumentEntryBusinessService;
import org.linagora.linshare.core.domain.constants.AuditLogEntryType;
import org.linagora.linshare.core.domain.constants.LogAction;
import org.linagora.linshare.core.domain.constants.Role;
import org.linagora.linshare.core.domain.entities.Account;
import org.linagora.linshare.core.domain.entities.Functionality;
import org.linagora.linshare.core.domain.entities.Thread;
import org.linagora.linshare.core.domain.entities.ThreadLogEntry;
import org.linagora.linshare.core.domain.entities.ThreadMember;
import org.linagora.linshare.core.domain.entities.User;
import org.linagora.linshare.core.exception.BusinessErrorCode;
import org.linagora.linshare.core.exception.BusinessException;
import org.linagora.linshare.core.rac.ThreadMemberResourceAccessControl;
import org.linagora.linshare.core.rac.ThreadResourceAccessControl;
import org.linagora.linshare.core.repository.ThreadMemberRepository;
import org.linagora.linshare.core.repository.ThreadRepository;
import org.linagora.linshare.core.repository.UserRepository;
import org.linagora.linshare.core.service.FunctionalityReadOnlyService;
import org.linagora.linshare.core.service.LogEntryService;
import org.linagora.linshare.core.service.ThreadService;
import org.linagora.linshare.mongo.entities.logs.ThreadAuditLogEntry;
import org.linagora.linshare.mongo.entities.logs.ThreadMemberAuditLogEntry;
import org.linagora.linshare.mongo.entities.mto.ThreadMemberMto;
import org.linagora.linshare.mongo.entities.mto.ThreadMto;
import org.linagora.linshare.mongo.repository.AuditUserMongoRepository;

public class ThreadServiceImpl extends GenericServiceImpl<Account, Thread> implements ThreadService {

	private final ThreadRepository threadRepository;

	private final ThreadMemberRepository threadMemberRepository;

	private final DocumentEntryBusinessService documentEntryBusinessService;

	private final LogEntryService logEntryService;

	private final ThreadMemberResourceAccessControl threadMemberAC;

	private final UserRepository<User> userRepository;

	private final FunctionalityReadOnlyService functionalityReadOnlyService;

	private final AuditUserMongoRepository auditMongoRepository;

	public ThreadServiceImpl(
			ThreadRepository threadRepository,
			ThreadMemberRepository threadMemberRepository,
			DocumentEntryBusinessService documentEntryBusinessService,
			LogEntryService logEntryService,
			ThreadResourceAccessControl rac,
			ThreadMemberResourceAccessControl threadMemberResourceAccessControl,
			UserRepository<User> userRepository,
			FunctionalityReadOnlyService functionalityReadOnlyService,
			AuditUserMongoRepository auditMongoRepository) {
		super(rac);
		this.threadRepository = threadRepository;
		this.threadMemberRepository = threadMemberRepository;
		this.documentEntryBusinessService = documentEntryBusinessService;
		this.logEntryService = logEntryService;
		this.threadMemberAC = threadMemberResourceAccessControl;
		this.userRepository = userRepository;
		this.functionalityReadOnlyService = functionalityReadOnlyService;
		this.auditMongoRepository = auditMongoRepository;
	}

	@Override
	public Thread find(Account actor, Account owner, String uuid) {
		preChecks(actor, owner);
		Validate.notEmpty(uuid, "Missing thread uuid");
		Thread thread = threadRepository.findByLsUuid(uuid);

		if (thread == null) {
			logger.error("Can't find thread  : " + uuid);
			logger.error("Current actor " + actor.getAccountRepresentation()
					+ " is looking for a misssing thread (" + uuid
					+ ") owned by : " + owner.getAccountRepresentation());
			String message = "Can not find thread with uuid : " + uuid;
			throw new BusinessException(
					BusinessErrorCode.THREAD_NOT_FOUND, message);
		}
		checkReadPermission(actor, owner, Thread.class,
				BusinessErrorCode.THREAD_FORBIDDEN, thread, owner);
//		ThreadAuditLogEntry log = new ThreadAuditLogEntry(actor, owner, LogAction.GET, AuditLogEntryType.THREAD,
//				new ThreadMto(thread, false));
//		auditMongoRepository.insert(log);
		return thread;
	}

	@Override
	public Thread findByLsUuidUnprotected(String uuid) {
		Thread thread = threadRepository.findByLsUuid(uuid);
		if (thread == null) {
			logger.error("Can't find thread  : " + uuid);
		}
		return thread;
	}

	@Override
	public List<Thread> findAll(Account actor, Account owner) {
		checkListPermission(actor, owner, Thread.class,
				BusinessErrorCode.THREAD_FORBIDDEN, null);
		return threadRepository.findAll();
	}

	@Override
	public Thread create(Account actor, Account owner, String name) throws BusinessException {
		Functionality threadFunc = functionalityReadOnlyService.getThreadTabFunctionality(owner.getDomain());
		Functionality threadCreation = functionalityReadOnlyService.getThreadCreationPermission(owner.getDomain());
		if (!threadFunc.getActivationPolicy().getStatus()
				|| !threadCreation.getActivationPolicy().getStatus()) {
			throw new BusinessException(BusinessErrorCode.THREAD_FORBIDDEN, "Functionality forbideen.");
		}
		checkCreatePermission(actor, owner, Thread.class,
				BusinessErrorCode.THREAD_FORBIDDEN, null);
		Thread thread = null;
		ThreadMember member = null;
		logger.debug("User " + owner.getAccountRepresentation() + " trying to create new thread named " + name);
		thread = new Thread(owner.getDomain(), owner, name);
		threadRepository.create(thread);
		logEntryService.create(new ThreadLogEntry(owner, thread, LogAction.THREAD_CREATE, "Creation of a new thread."));
		// creator = first member = default admin
		member = new ThreadMember(true, true, (User) owner, thread);
		thread.getMyMembers().add(member);
		thread = threadRepository.update(thread);
		logEntryService.create(new ThreadLogEntry(owner, member, LogAction.THREAD_ADD_MEMBER,
				"Creating the first member of the newly created thread."));
		ThreadAuditLogEntry log = new ThreadAuditLogEntry(actor, owner, LogAction.CREATE, AuditLogEntryType.THREAD,
				new ThreadMto(thread, false));
		auditMongoRepository.insert(log);
		return thread;
	}

	@Override
	public ThreadMember getThreadMemberById(long id) throws BusinessException {
		return threadMemberRepository.findById(id);
	}

	@Override
	public ThreadMember getMemberFromUser(Thread thread, User user) throws BusinessException {
		return threadMemberRepository.findUserThreadMember(thread, user);
	}

	@Override
	public List<ThreadMember> findAllThreadMembers(Account actor, User owner,
			Thread thread) throws BusinessException {
		threadMemberAC.checkListPermission(actor, owner, ThreadMember.class,
				BusinessErrorCode.THREAD_MEMBER_FORBIDDEN, null, thread);
		return threadMemberRepository.findAllThreadMembers(thread);
	}

	@Override
	public List<ThreadMember> findAllInconsistentMembers(Account actor, User owner,
			Thread thread) throws BusinessException {
		threadMemberAC.checkListPermission(actor, owner, ThreadMember.class,
				BusinessErrorCode.THREAD_MEMBER_FORBIDDEN, null, thread);
		return threadMemberRepository.findAllInconsistentThreadMembers(thread);
	}

	@Override
	public List<Thread> findAllWhereMember(User user) {
		return threadRepository.findAllWhereMember(user);
	}

	@Override
	public List<Thread> findAllWhereAdmin(User user) {
		return threadRepository.findAllWhereAdmin(user);
	}

	@Override
	public List<Thread> findAllWhereCanUpload(User user) {
		return threadRepository.findAllWhereCanUpload(user);
	}

	@Override
	public boolean hasAnyWhereAdmin(User user) {
		return threadMemberRepository.isUserAdminOfAny(user);
	}

	@Override
	public boolean isUserAdmin(User user, Thread thread) {
		return threadMemberRepository.isUserAdmin(user, thread);
	}

	@Override
	public long countMembers(Thread thread) {
		return threadMemberRepository.count(thread);
	}

	@Override
	public long countEntries(Thread thread) {
		return documentEntryBusinessService.countThreadEntries(thread);
	}

	@Override
	public ThreadMember addMember(Account actor, Account owner, Thread thread,
			User user, boolean admin, boolean canUpload)
			throws BusinessException {
		ThreadMember member = new ThreadMember(canUpload, admin, user, thread);
		threadMemberAC.checkCreatePermission(actor, owner, ThreadMember.class,
				BusinessErrorCode.THREAD_MEMBER_FORBIDDEN, member, thread);
		if (getMemberFromUser(thread, user) != null) {
			logger.warn("The current " + user.getAccountRepresentation()
					+ " user is already member of the thread : "
					+ thread.getAccountRepresentation());
			throw new BusinessException(
					"You are not authorized to add member to this thread. Already exists.");
		}
		thread.getMyMembers().add(member);
		threadRepository.update(thread);
		logEntryService.create(new ThreadLogEntry(owner, member,
				LogAction.THREAD_ADD_MEMBER,
				"Adding a new member to a thread : "
						+ member.getUser().getAccountRepresentation()));
		ThreadMemberAuditLogEntry log = new ThreadMemberAuditLogEntry(actor, owner, LogAction.CREATE,
				AuditLogEntryType.THREAD_MEMBER, member);
		auditMongoRepository.insert(log);
		return member;
	}

	@Override
	public ThreadMember updateMember(Account actor, Account owner, String threadUuid, String userUuid,
			boolean admin, boolean canUpload)
			throws BusinessException {
		Thread thread = find(actor, owner, threadUuid);
		User user = getUserMember(userUuid);
		ThreadMember member = getMemberFromUser(thread, user);
		threadMemberAC.checkUpdatePermission(actor, owner, ThreadMember.class,
				BusinessErrorCode.THREAD_MEMBER_FORBIDDEN, member);
		ThreadMemberAuditLogEntry log = new ThreadMemberAuditLogEntry(actor, owner, LogAction.UPDATE,
				AuditLogEntryType.THREAD_MEMBER, member);
		member.setAdmin(admin);
		member.setCanUpload(canUpload);
		ThreadMember res = threadMemberRepository.update(member);
		log.setMemberUpdated(new ThreadMemberMto(res));
		auditMongoRepository.insert(log);
		return res;
	}

	@Override
	public ThreadMember deleteMember(Account actor, Account owner, String threadUuid,
			String userUuid) throws BusinessException {
		preChecks(actor, owner);
		Validate.notEmpty(userUuid);
		Validate.notEmpty(threadUuid);
		Thread thread = find(actor, owner, threadUuid);
		User user = getUserMember(userUuid);
		ThreadMember member = getMemberFromUser(thread,
				user);
		threadMemberAC.checkDeletePermission(actor, owner, ThreadMember.class,
				BusinessErrorCode.THREAD_MEMBER_FORBIDDEN, member);
		thread.getMyMembers().remove(member);
		threadRepository.update(thread);
		threadMemberRepository.delete(member);
		logEntryService.create(new ThreadLogEntry(owner, member,
				LogAction.THREAD_REMOVE_MEMBER,
				"Deleting a member in a thread."));
		ThreadMemberAuditLogEntry log = new ThreadMemberAuditLogEntry(actor, owner, LogAction.DELETE,
				AuditLogEntryType.THREAD_MEMBER, member);
		auditMongoRepository.insert(log);
		return member;
	}

	private User getUserMember(String userUuid) {
		User user = userRepository.findByLsUuid(userUuid);
		if (user == null) {
			user = userRepository.findDeleted(userUuid);
			if (user == null) {
				throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "Can not find user with uuid : " + userUuid);
			} else {
				logger.info("The member with uuid " + userUuid
						+ " you are trying to delete is already deleted");
			}
		}
		return user;
	}

	@Override
	public void deleteAllMembers(Account actor, Thread thread) throws BusinessException {
		// permission check
		checkUserIsAdmin(actor, thread);
		Object[] myMembers = thread.getMyMembers().toArray();
		for (Object threadMember : myMembers) {
			thread.getMyMembers().remove(threadMember);
			threadRepository.update(thread);
			threadMemberRepository.delete((ThreadMember) threadMember);
		}
		logEntryService.create(new ThreadLogEntry(actor, thread, LogAction.THREAD_REMOVE_MEMBER, "Deleting all members in a thread."));
	}

	@Override
	public void deleteAllUserMemberships(Account actor, User user)
			throws BusinessException {
		List<ThreadMember> memberships = threadMemberRepository
				.findAllUserMemberships(user);
		for (ThreadMember threadMember : memberships) {
			deleteMember(actor, actor, threadMember.getThread().getLsUuid(),
					threadMember.getUser().getLsUuid());
		}
	}

	@Override
	public void deleteThread(User actor, Account owner, Thread thread)
			throws BusinessException {
		checkDeletePermission(actor, owner, Thread.class,
				BusinessErrorCode.THREAD_FORBIDDEN, thread);
		ThreadLogEntry log = new ThreadLogEntry(actor, thread,
				LogAction.THREAD_DELETE, "Deleting a thread.");
		// Delete all entries
		documentEntryBusinessService.deleteSetThreadEntry(thread.getEntries());
		thread.setEntries(null);
		threadRepository.update(thread);
		// Deleting members
		this.deleteAllMembers(actor, thread);
		// Deleting the thread
		threadRepository.delete(thread);
		ThreadAuditLogEntry threadAuditLog = new ThreadAuditLogEntry(actor, owner, LogAction.DELETE,
				AuditLogEntryType.THREAD, new ThreadMto(thread, true));
		auditMongoRepository.insert(threadAuditLog);
		logEntryService.create(log);
	}

	@Override
	public Thread update(User actor, Account owner, String threadUuid,
			String threadName) throws BusinessException {
		Thread thread = find(actor, owner, threadUuid);
		checkUpdatePermission(actor, owner, Thread.class,
				BusinessErrorCode.THREAD_FORBIDDEN, thread);
		ThreadAuditLogEntry log = new ThreadAuditLogEntry(actor, owner, LogAction.UPDATE, AuditLogEntryType.THREAD,
				new ThreadMto(thread, false));
		String oldname = thread.getName();
		thread.setName(threadName);
		Thread update = threadRepository.update(thread);
		logEntryService.create(new ThreadLogEntry(actor, thread,
				LogAction.THREAD_RENAME, "Renamed thread from " + oldname
						+ " to " + threadName));
		log.setResourceUpdated(new ThreadMto(update, false));
		auditMongoRepository.insert(log);
		return update;
	}

	@Override
	public List<Thread> findLatestWhereMember(User actor, int limit) {
		return threadRepository.findLatestWhereMember(actor, limit);
	}

	@Override
	public List<Thread> searchByName(User actor, String pattern) {
		return threadRepository.searchByName(actor, pattern);
	}

	@Override
	public List<Thread> searchByMembers(User actor, String pattern) {
		return threadRepository.searchAmongMembers(actor, pattern);
	}

    /* ***********************************************************
     *                   Helpers
     ************************************************************ */


	/**
	 * Check if actor is admin of the thread and so has the right to perform any action.
	 * Throw a BusinessException if the actor isn't authorized to modify the thread.
	 */
	private void checkUserIsAdmin(Account actor, Thread thread) throws BusinessException {
		if (actor.getRole().equals(Role.SUPERADMIN) || actor.getRole().equals(Role.SYSTEM)) {
			return; // superadmin or system accounts have all rights
		}
		if (!isUserAdmin((User) actor, thread)) {
			logger.error("Actor: " + actor.getAccountRepresentation() + " isn't admin of the Thread: "
					+ thread.getAccountRepresentation());
			throw new BusinessException(BusinessErrorCode.FORBIDDEN,
					"you are not authorized to perform this action on this thread.");
		}
	}
}
