/*
 * LinShare is an open source filesharing software, part of the LinPKI software
 * suite, developed by Linagora.
 * 
 * Copyright (C) 2016 LINAGORA
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

package org.linagora.linshare.core.batches.impl;

import java.util.List;

import org.linagora.linshare.core.domain.constants.LogActionCause;
import org.linagora.linshare.core.domain.entities.AbstractDomain;
import org.linagora.linshare.core.domain.entities.Account;
import org.linagora.linshare.core.domain.entities.Entry;
import org.linagora.linshare.core.domain.entities.ShareEntry;
import org.linagora.linshare.core.domain.entities.SystemAccount;
import org.linagora.linshare.core.domain.objects.TimeUnitValueFunctionality;
import org.linagora.linshare.core.exception.BatchBusinessException;
import org.linagora.linshare.core.exception.BusinessException;
import org.linagora.linshare.core.job.quartz.Context;
import org.linagora.linshare.core.job.quartz.EntryBatchResultContext;
import org.linagora.linshare.core.repository.AccountRepository;
import org.linagora.linshare.core.service.DocumentEntryService;
import org.linagora.linshare.core.service.FunctionalityReadOnlyService;
import org.linagora.linshare.core.service.ShareEntryService;

public class DeleteExpiredShareEntryBatchImpl extends GenericBatchImpl {

	private final ShareEntryService service;

	private final FunctionalityReadOnlyService functionalityService;

	private final DocumentEntryService documentEntryService;

	public DeleteExpiredShareEntryBatchImpl(
			final AccountRepository<Account> accountRepository,
			final ShareEntryService shareEntryService,
			final FunctionalityReadOnlyService functionalityService,
			final DocumentEntryService documentEntryService) {
		super(accountRepository);
		this.service = shareEntryService;
		this.functionalityService = functionalityService;
		this.documentEntryService = documentEntryService;
	}

	@Override
	public List<String> getAll() {
		logger.info(getClass().toString() + " job starting ...");
		SystemAccount actor = getSystemAccount();
		List<String> allShares = service.findAllExpiredEntries(actor, actor);
		logger.info(allShares.size()
				+ " share(s) have been found to be deleted");
		return allShares;
	}

	@Override
	public Context execute(String identifier, long total, long position)
			throws BatchBusinessException, BusinessException {
		SystemAccount actor = getSystemAccount();
		ShareEntry resource = service.find(actor, actor, identifier);
		Context context = new EntryBatchResultContext(resource);
		try {
			logInfo(total, position,
					"processing share : " + resource.getRepresentation());
			AbstractDomain domain = resource.getEntryOwner().getDomain();
			TimeUnitValueFunctionality func = functionalityService
					.getDefaultShareExpiryTimeFunctionality(domain);
			// test if this functionality is enable for the current domain.
			if (func.getActivationPolicy().getStatus()) {
				service.delete(actor, actor, identifier, LogActionCause.EXPIRATION);
				documentEntryService.deleteOrComputeExpiryDate(actor, domain,
						resource.getDocumentEntry());
			} else {
				logger.warn(
						"Expiration date is set for the current share '%s' but functionnality is disabled for its domain '%s'",
						resource.getRepresentation(), domain.getUuid());
			}
			logger.info("Expired share was deleted : "
					+ resource.getRepresentation());
		} catch (BusinessException businessException) {
			logError(total, position,
					"Error while trying to delete expired share : " + resource.getRepresentation());
			logger.info("Error occured while cleaning expired shares",
					businessException);
			BatchBusinessException exception = new BatchBusinessException(
					context, "Error while trying to delete expired shares.");
			exception.setBusinessException(businessException);
			throw exception;
		}
		return context;
	}

	@Override
	public void notify(Context context, long total, long position) {
		EntryBatchResultContext shareContext = (EntryBatchResultContext) context;
		Entry entry = shareContext.getResource();
		logInfo(total, position, "The share " + entry.getRepresentation()
				+ " has been successfully deleted.");
	}

	@Override
	public void notifyError(BatchBusinessException exception,
			String identifier, long total, long position) {
		EntryBatchResultContext shareContext = (EntryBatchResultContext) exception
				.getContext();
		Entry entry = shareContext.getResource();
		logError(total, position,
				"cleaning share has failed : " + entry.getRepresentation());
		logger.error(
				"Error occured while cleaning expired share "
						+ entry.getRepresentation() + ". BatchBusinessException ",
				exception);
	}

	@Override
	public void terminate(List<String> all, long errors, long unhandled_errors,
			long total, long processed) {
		long success = total - errors - unhandled_errors;
		logger.info(success + " share(s) have been deleted.");
		if (errors > 0) {
			logger.error(errors + " share(s) failed to be deleted.");
		}
		if (unhandled_errors > 0) {
			logger.error(unhandled_errors
					+ " share(s) failed to be deleted (unhandled error).");
		}
		logger.info(getClass().toString() + " job terminated.");
	}

}
